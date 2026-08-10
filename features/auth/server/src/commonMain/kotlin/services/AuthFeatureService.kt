package dev.inmo.wishlist.features.auth.server.services

import com.benasher44.uuid.uuid4
import dev.inmo.micro_utils.coroutines.SmartRWLocker
import dev.inmo.micro_utils.coroutines.withReadAcquire
import dev.inmo.micro_utils.coroutines.withWriteLock
import dev.inmo.micro_utils.repos.MapKeyValueRepo
import dev.inmo.micro_utils.repos.create
import dev.inmo.micro_utils.repos.deleteById
import dev.inmo.micro_utils.repos.set
import dev.inmo.micro_utils.repos.unset
import korlibs.time.DateTime
import org.mindrot.jbcrypt.BCrypt
import dev.inmo.wishlist.features.auth.server.ServerAuthFeature
import dev.inmo.wishlist.features.auth.server.RegistrationEmailSender
import dev.inmo.wishlist.features.auth.server.RegistrationRoleLifecycle
import dev.inmo.wishlist.features.auth.common.models.AuthConfig
import dev.inmo.wishlist.features.auth.common.models.AuthCredentials
import dev.inmo.wishlist.features.auth.common.models.AuthFeatureUser
import dev.inmo.wishlist.features.auth.common.models.Password
import dev.inmo.wishlist.features.auth.common.models.RefreshToken
import dev.inmo.wishlist.features.auth.common.models.Token
import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.auth.server.repo.PasswordsRepo
import dev.inmo.wishlist.features.auth.common.models.asAuthFeatureUser
import dev.inmo.wishlist.features.users.common.models.NewUser
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.users.common.models.Username
import dev.inmo.wishlist.features.users.common.repo.ReadUsersRepo
import dev.inmo.wishlist.features.users.common.repo.WriteUsersRepo
import dev.inmo.wishlist.features.users.common.repo.exceptions.DuplicateUserFieldException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.TimeMark
import kotlin.time.TimeSource

class AuthFeatureService(
    private val usersRepo: ReadUsersRepo,
    private val writeUsersRepo: WriteUsersRepo,
    private val passwordsRepo: PasswordsRepo,
    private val tokenTtl: Duration = 15.minutes,
    private val refreshTokenTtl: Duration = 7.days,
    private val enableRegistration: Boolean = false,
    private val requireEmailForRegistration: Boolean = false,
    private val registrationEmailSender: RegistrationEmailSender? = null,
    private val registrationRoleLifecycle: RegistrationRoleLifecycle? = null,
) : ServerAuthFeature {
    private data class Entry(val id: UserId, val issued: DateTime)
    private val locker = SmartRWLocker()

    private val tokens = MapKeyValueRepo<Token, Entry>()
    private val refreshTokens = MapKeyValueRepo<RefreshToken, Entry>()
    private val tokenToRefreshToken = MapKeyValueRepo<Token, RefreshToken>()

    /** Minimum accepted length of a self-service registration password. */
    private val minPasswordLength = 8

    /** Maximum accepted password length; BCrypt ignores any input past 72 bytes. */
    private val maxPasswordLength = 72

    /**
     * Returns `true` when [password] satisfies the self-service registration policy — its length is
     * within [minPasswordLength]..[maxPasswordLength]. Guards against empty/trivial passwords and
     * against silent BCrypt truncation of over-long input.
     *
     * @param password Candidate plaintext password.
     */
    private fun isAcceptablePassword(password: Password): Boolean =
        password.string.length in minPasswordLength..maxPasswordLength

    override suspend fun login(username: Username, password: Password): AuthCredentials? {
        locker.withWriteLock {
            val userInfo = usersRepo.getUserByUsername(username) ?: return null
            val storedHash = passwordsRepo.get(userInfo.id) ?: return null
            if (!BCrypt.checkpw(password.string, storedHash.string)) return null
            return issueCredentialsFor(userInfo.id)
        }
    }

    override suspend fun refresh(refreshToken: RefreshToken): AuthCredentials? {
        locker.withWriteLock {
            val entry = refreshTokens.get(refreshToken) ?: return null
            refreshTokens.unset(refreshToken)
            if (entry.issued + refreshTokenTtl < DateTime.now()) return null
            return issueCredentialsFor(entry.id)
        }
    }

    override suspend fun logout(token: Token) {
        locker.withWriteLock {
            val linkedRefreshToken = tokenToRefreshToken.get(token)
            tokens.unset(token)
            tokenToRefreshToken.unset(token)
            linkedRefreshToken?.let { refreshTokens.unset(it) }
        }
    }

    override suspend fun getUser(token: Token): AuthFeatureUser? {
        locker.withReadAcquire {
            val entry = tokens.get(token) ?: return null
            if (entry.issued + tokenTtl > DateTime.now()) {
                return usersRepo.getById(entry.id)?.asAuthFeatureUser()
            }
            return null
        }
    }

    suspend fun authenticate(token: Token): UserId? {
        locker.withReadAcquire {
            val entry = tokens.get(token) ?: return null
            if (entry.issued + tokenTtl < DateTime.now()) {
                tokens.unset(token)
                tokenToRefreshToken.unset(token)
                return null
            }
            return entry.id
        }
    }

    /** Delegates the legacy registration surface to the email-aware implementation. */
    override suspend fun register(username: Username, password: Password): AuthCredentials? =
        register(username, password, null)

    override suspend fun register(
        username: Username,
        password: Password,
        email: Email?
    ): AuthCredentials? {
        if (enableRegistration == false) return null
        if (!isAcceptablePassword(password)) return null
        return when {
            requireEmailForRegistration -> {
                val requiredEmail = email ?: return null
                val sender = registrationEmailSender ?: return null
                val roleLifecycle = registrationRoleLifecycle ?: return null
                registerWithRequiredEmail(username, password, requiredEmail, sender, roleLifecycle)
            }
            else -> registerWithoutRequiredEmail(username, password, email)
        }
    }

    /**
     * Preserves the original single-lock registration transaction when no external invite is needed.
     */
    private suspend fun registerWithoutRequiredEmail(
        username: Username,
        password: Password,
        email: Email?,
    ): AuthCredentials? = locker.withWriteLock {
        if (usersRepo.getUserByUsername(username) != null) return null
        val created = createUserOrNull(username, email) ?: return null
        val hashed = BCrypt.hashpw(password.string, BCrypt.gensalt())
        passwordsRepo.set(created.id to Password(hashed))
        issueCredentialsFor(created.id)
    }

    /**
     * Reserves a non-authenticating account, performs invite delivery without the global auth lock,
     * and installs credentials only after successful delivery.
     */
    private suspend fun registerWithRequiredEmail(
        username: Username,
        password: Password,
        email: Email,
        sender: RegistrationEmailSender,
        roleLifecycle: RegistrationRoleLifecycle,
    ): AuthCredentials? {
        var reservedUserId: UserId? = null
        val provisionalUser = try {
            locker.withWriteLock {
                if (usersRepo.getUserByUsername(username) != null) return null
                val created = createUserOrNull(username, email) ?: return null
                reservedUserId = created.id
                val pending = try {
                    roleLifecycle.markPending(created.id)
                } catch (error: CancellationException) {
                    throw error
                } catch (_: Exception) {
                    false
                }
                created.takeIf { pending }
            }
        } catch (error: CancellationException) {
            compensateAfterCancellation(reservedUserId, roleLifecycle, error)
        }

        if (provisionalUser == null) {
            reservedUserId?.let { compensateRequiredRegistration(it, roleLifecycle) }
            return null
        }

        val hashedPassword = try {
            Password(BCrypt.hashpw(password.string, BCrypt.gensalt()))
        } catch (error: CancellationException) {
            compensateAfterCancellation(provisionalUser.id, roleLifecycle, error)
        } catch (error: Exception) {
            compensateRequiredRegistration(provisionalUser.id, roleLifecycle)
            throw error
        }

        val delivered = try {
            sender.sendRegistrationEmail(provisionalUser)
        } catch (error: CancellationException) {
            compensateAfterCancellation(provisionalUser.id, roleLifecycle, error)
        } catch (_: Exception) {
            false
        }
        if (!delivered) {
            compensateRequiredRegistration(provisionalUser.id, roleLifecycle)
            return null
        }

        val credentials = try {
            locker.withWriteLock {
                val storedUser = usersRepo.getById(provisionalUser.id)
                if (storedUser?.email != email) return@withWriteLock null
                passwordsRepo.set(provisionalUser.id to hashedPassword)
                issueCredentialsFor(provisionalUser.id)
            }
        } catch (error: CancellationException) {
            compensateAfterCancellation(provisionalUser.id, roleLifecycle, error)
        } catch (error: Exception) {
            compensateRequiredRegistration(provisionalUser.id, roleLifecycle)
            throw error
        }
        if (credentials == null) {
            compensateRequiredRegistration(provisionalUser.id, roleLifecycle)
        }
        return credentials
    }

    /**
     * Creates one account while translating only the repository's expected uniqueness conflict.
     */
    private suspend fun createUserOrNull(username: Username, email: Email?): dev.inmo.wishlist.features.users.common.models.RegisteredUser? =
        try {
            writeUsersRepo.create(listOf(NewUser(username, email))).firstOrNull()
        } catch (_: DuplicateUserFieldException) {
            null
        }

    /**
     * Removes a provisional account before its direct roles, and defensively removes auth state.
     */
    private suspend fun compensateRequiredRegistration(
        userId: UserId,
        roleLifecycle: RegistrationRoleLifecycle,
    ) = withContext(NonCancellable) {
        locker.withWriteLock {
            writeUsersRepo.deleteById(userId)
            purgeUserWhileLocked(userId)
            roleLifecycle.removeRoles(userId)
        }
    }

    /**
     * Completes non-cancellable compensation and then propagates the initiating cancellation.
     */
    private suspend fun compensateAfterCancellation(
        userId: UserId?,
        roleLifecycle: RegistrationRoleLifecycle,
        error: CancellationException,
    ): Nothing {
        if (userId != null) {
            try {
                compensateRequiredRegistration(userId, roleLifecycle)
            } catch (cleanupError: Exception) {
                error.addSuppressed(cleanupError)
            }
        }
        throw error
    }

    /**
     * Returns the configured public auth flags.
     *
     * @return Registration configuration exposed by the auth config route.
     */
    override suspend fun getConfig(): AuthConfig = AuthConfig(
        enableRegistration = enableRegistration,
        requireEmailForRegistration = requireEmailForRegistration,
    )

    override suspend fun isRegistrationAvailable(): Boolean = enableRegistration

    suspend fun setPassword(userId: UserId, rawPassword: Password) {
        locker.withWriteLock {
            val hashed = BCrypt.hashpw(rawPassword.string, BCrypt.gensalt())
            passwordsRepo.set(userId to Password(hashed))
        }
    }

    /**
     * Removes all authentication data tied to [userId]: the stored password hash and every
     * active access/refresh session. Used by the admin cascade when a user is deleted so no
     * orphaned credentials or live tokens remain.
     *
     * @param userId Identity whose password and sessions must be purged.
     */
    suspend fun purgeUser(userId: UserId) {
        locker.withWriteLock {
            purgeUserWhileLocked(userId)
        }
    }

    /** Removes password and session state while the caller holds [locker]'s write lock. */
    private suspend fun purgeUserWhileLocked(userId: UserId) {
        passwordsRepo.unset(userId)
        tokens.getAll().filterValues { it.id == userId }.keys.forEach { token ->
            tokens.unset(token)
            tokenToRefreshToken.get(token)?.let { refreshTokens.unset(it) }
            tokenToRefreshToken.unset(token)
        }
        refreshTokens.getAll().filterValues { it.id == userId }.keys.forEach { refreshToken ->
            refreshTokens.unset(refreshToken)
        }
    }

    private suspend fun issueCredentialsFor(id: UserId): AuthCredentials {
        val now = DateTime.now()
        val token = Token(uuid4().toString())
        val refreshToken = RefreshToken(uuid4().toString())
        tokens.set(token to Entry(id, now))
        refreshTokens.set(refreshToken to Entry(id, now))
        tokenToRefreshToken.set(token to refreshToken)
        return AuthCredentials(token, refreshToken)
    }
}
