package project_group.project_name.features.auth.server.services

import com.benasher44.uuid.uuid4
import dev.inmo.micro_utils.coroutines.SmartRWLocker
import dev.inmo.micro_utils.coroutines.withReadAcquire
import dev.inmo.micro_utils.coroutines.withWriteLock
import dev.inmo.micro_utils.repos.MapKeyValueRepo
import dev.inmo.micro_utils.repos.set
import dev.inmo.micro_utils.repos.unset
import korlibs.time.DateTime
import org.mindrot.jbcrypt.BCrypt
import project_group.project_name.features.auth.server.ServerAuthFeature
import project_group.project_name.features.auth.common.models.AuthCredentials
import project_group.project_name.features.auth.common.models.Password
import project_group.project_name.features.auth.common.models.RefreshToken
import project_group.project_name.features.auth.common.models.Token
import project_group.project_name.features.auth.server.repo.PasswordsRepo
import project_group.project_name.features.users.common.models.RegisteredUser
import project_group.project_name.features.users.common.models.UserId
import project_group.project_name.features.users.common.models.Username
import project_group.project_name.features.users.common.repo.ReadUsersRepo
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.TimeMark
import kotlin.time.TimeSource

class AuthFeatureService(
    private val usersRepo: ReadUsersRepo,
    private val passwordsRepo: PasswordsRepo,
    private val tokenTtl: Duration = 15.minutes,
    private val refreshTokenTtl: Duration = 7.days
) : ServerAuthFeature {
    private data class Entry(val id: UserId, val issued: DateTime)
    private val locker = SmartRWLocker()

    private val tokens = MapKeyValueRepo<Token, Entry>()
    private val refreshTokens = MapKeyValueRepo<RefreshToken, Entry>()
    private val tokenToRefreshToken = MapKeyValueRepo<Token, RefreshToken>()

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

    override suspend fun getUser(token: Token): RegisteredUser? {
        locker.withReadAcquire {
            val entry = tokens.get(token) ?: return null
            if (entry.issued + tokenTtl > DateTime.now()) {
                return usersRepo.getById(entry.id)
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

    suspend fun setPassword(userId: UserId, rawPassword: Password) {
        locker.withWriteLock {
            val hashed = BCrypt.hashpw(rawPassword.string, BCrypt.gensalt())
            passwordsRepo.set(userId to Password(hashed))
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
