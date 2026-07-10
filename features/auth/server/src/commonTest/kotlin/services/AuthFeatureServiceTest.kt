package dev.inmo.wishlist.features.auth.server.services

import dev.inmo.micro_utils.repos.MapCRUDRepo
import dev.inmo.micro_utils.repos.MapKeyValueRepo
import dev.inmo.wishlist.features.auth.common.models.AuthFeatureUser
import dev.inmo.wishlist.features.auth.common.models.Password
import dev.inmo.wishlist.features.auth.common.models.Token
import dev.inmo.wishlist.features.auth.server.repo.PasswordsRepo
import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.users.common.models.NewUser
import dev.inmo.wishlist.features.users.common.models.RegisteredUser
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.users.common.models.Username
import dev.inmo.wishlist.features.users.common.repo.UsersRepo
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * In-memory [UsersRepo] test double, mirroring the write-capable pattern used by
 * `features/email/server/src/commonTest/kotlin/services/FakeUsersRepo.kt` (this module needs write
 * access too, since [AuthFeatureService.register] and the fixtures below create users).
 *
 * @param initialUsers Users the repo is pre-seeded with, keyed by their [UserId].
 */
internal class FakeUsersRepo(
    initialUsers: Map<UserId, RegisteredUser> = emptyMap()
) : UsersRepo, MapCRUDRepo<RegisteredUser, UserId, NewUser>(initialUsers.toMutableMap()) {

    private var nextId: Long = (initialUsers.keys.maxOfOrNull { it.long } ?: 0L) + 1L

    override suspend fun updateObject(newValue: NewUser, id: UserId, old: RegisteredUser): RegisteredUser =
        old.copy(username = newValue.username, email = newValue.email)

    override suspend fun createObject(newValue: NewUser): Pair<UserId, RegisteredUser> {
        val id = UserId(nextId++)
        return id to RegisteredUser(id, newValue.username, newValue.email)
    }

    override suspend fun getUserByUsername(username: Username): RegisteredUser? =
        getAll().values.firstOrNull { it.username == username }
}

/**
 * In-memory [PasswordsRepo] test double delegating entirely to [MapKeyValueRepo].
 */
internal class FakePasswordsRepo : PasswordsRepo, dev.inmo.micro_utils.repos.KeyValueRepo<UserId, Password> by MapKeyValueRepo()

/**
 * Verifies [AuthFeatureService.getUser]: a valid, unexpired token resolves to an [AuthFeatureUser]
 * that preserves [RegisteredUser.email] — a regression check that B-V1's own-record surface does
 * NOT drop email, unlike Commit A's `UsersFeatureUser`.
 */
class AuthFeatureServiceTest {

    private val userWithEmail = RegisteredUser(UserId(7L), Username("bob"), Email("bob@example.com"))
    private val plainPassword = Password("s3cret-pw")

    private fun buildService(
        usersRepo: FakeUsersRepo,
        passwordsRepo: FakePasswordsRepo = FakePasswordsRepo(),
        tokenTtl: Duration = 15.minutes
    ) = AuthFeatureService(
        usersRepo = usersRepo,
        writeUsersRepo = usersRepo,
        passwordsRepo = passwordsRepo,
        tokenTtl = tokenTtl
    )

    /** A valid, unexpired token resolves to an [AuthFeatureUser] carrying the seeded user's email. */
    @Test
    fun getUserReturnsFeatureUserWithEmailForValidToken() = runTest {
        val usersRepo = FakeUsersRepo(mapOf(userWithEmail.id to userWithEmail))
        val service = buildService(usersRepo)
        service.setPassword(userWithEmail.id, plainPassword)
        val credentials = service.login(userWithEmail.username, plainPassword)
        checkNotNull(credentials) { "login must succeed with the just-set password" }

        val result = service.getUser(credentials.token)

        assertEquals(
            AuthFeatureUser(userWithEmail.id, userWithEmail.username, userWithEmail.email),
            result
        )
    }

    /** A token issued by a service configured with a zero TTL is treated as already expired. */
    @Test
    fun getUserReturnsNullForExpiredToken() = runTest {
        val usersRepo = FakeUsersRepo(mapOf(userWithEmail.id to userWithEmail))
        val service = buildService(usersRepo, tokenTtl = Duration.ZERO)
        service.setPassword(userWithEmail.id, plainPassword)
        val credentials = service.login(userWithEmail.username, plainPassword)
        checkNotNull(credentials) { "login must succeed with the just-set password" }

        assertNull(service.getUser(credentials.token))
    }

    /** A token that was never issued resolves to `null`. */
    @Test
    fun getUserReturnsNullForUnknownToken() = runTest {
        val service = buildService(FakeUsersRepo())

        assertNull(service.getUser(Token("unknown-token")))
    }
}
