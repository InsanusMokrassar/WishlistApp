package dev.inmo.wishlist.features.auth.server.services

import com.benasher44.uuid.uuid4
import dev.inmo.micro_utils.coroutines.SmartRWLocker
import dev.inmo.micro_utils.coroutines.withReadAcquire
import dev.inmo.micro_utils.coroutines.withWriteLock
import dev.inmo.micro_utils.repos.MapKeyValueRepo
import dev.inmo.micro_utils.repos.create
import dev.inmo.micro_utils.repos.set
import dev.inmo.micro_utils.repos.unset
import korlibs.time.DateTime
import org.mindrot.jbcrypt.BCrypt
import dev.inmo.wishlist.features.auth.server.ServerAuthFeature
import dev.inmo.wishlist.features.auth.server.models.AuthConfig
import dev.inmo.wishlist.features.auth.common.models.AuthCredentials
import dev.inmo.wishlist.features.auth.common.models.AuthFeatureUser
import dev.inmo.wishlist.features.auth.common.models.Password
import dev.inmo.wishlist.features.auth.common.models.RefreshToken
import dev.inmo.wishlist.features.auth.common.models.Token
import dev.inmo.wishlist.features.auth.server.repo.PasswordsRepo
import dev.inmo.wishlist.features.auth.common.models.asAuthFeatureUser
import dev.inmo.wishlist.features.users.common.models.NewUser
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.users.common.models.Username
import dev.inmo.wishlist.features.users.common.repo.ReadUsersRepo
import dev.inmo.wishlist.features.users.common.repo.WriteUsersRepo
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

    override suspend fun register(username: Username, password: Password): AuthCredentials? {
        if (enableRegistration == false) return null
        if (!isAcceptablePassword(password)) return null

        locker.withWriteLock {
            if (usersRepo.getUserByUsername(username) != null) return null
            val created = writeUsersRepo.create(listOf(NewUser(username))).firstOrNull() ?: return null
            val hashed = BCrypt.hashpw(password.string, BCrypt.gensalt())
            passwordsRepo.set(created.id to Password(hashed))
            return issueCredentialsFor(created.id)
        }
    }

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
