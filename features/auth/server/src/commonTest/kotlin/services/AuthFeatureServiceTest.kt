package dev.inmo.wishlist.features.auth.server.services

import dev.inmo.micro_utils.repos.MapCRUDRepo
import dev.inmo.micro_utils.repos.MapKeyValueRepo
import dev.inmo.wishlist.features.auth.common.models.AuthFeatureUser
import dev.inmo.wishlist.features.auth.common.models.AuthConfig
import dev.inmo.wishlist.features.auth.common.models.Password
import dev.inmo.wishlist.features.auth.common.models.Token
import dev.inmo.wishlist.features.auth.server.RegistrationEmailSender
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
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
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

    /** Next identifier assigned to a newly created fixture user. */
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
 * Records required-email registration deliveries in auth service tests.
 *
 * @param results Delivery outcomes returned in attempt order.
 */
private class FakeRegistrationEmailSender(
    private vararg val results: Boolean,
) : RegistrationEmailSender {
    /** Number of delivery attempts already consumed from [results]. */
    private var attemptCount = 0

    /** Accounts passed to the sender, in registration order. */
    val users = mutableListOf<RegisteredUser>()

    /** Records the account and returns the next configured delivery result. */
    override suspend fun sendRegistrationEmail(user: RegisteredUser): Boolean {
        users += user
        val result = results.getOrNull(attemptCount) ?: results.lastOrNull() ?: false
        attemptCount++
        return result
    }
}

/**
 * Verifies [AuthFeatureService.getUser]: a valid, unexpired token resolves to an [AuthFeatureUser]
 * that preserves [RegisteredUser.email] — a regression check that B-V1's own-record surface does
 * NOT drop email, unlike Commit A's `UsersFeatureUser`.
 */
class AuthFeatureServiceTest {

    /** Seeded account fixture used by token-to-user tests. */
    private val userWithEmail = RegisteredUser(UserId(7L), Username("bob"), Email("bob@example.com"))

    /** Password fixture long enough to satisfy self-service registration policy. */
    private val plainPassword = Password("s3cret-pw")

    /** Builds an auth service with in-memory repositories and the requested registration policy. */
    private fun buildService(
        usersRepo: FakeUsersRepo,
        passwordsRepo: FakePasswordsRepo = FakePasswordsRepo(),
        tokenTtl: Duration = 15.minutes,
        enableRegistration: Boolean = false,
        requireEmailForRegistration: Boolean = false,
        registrationEmailSender: RegistrationEmailSender? = null,
    ) = AuthFeatureService(
        usersRepo = usersRepo,
        writeUsersRepo = usersRepo,
        passwordsRepo = passwordsRepo,
        tokenTtl = tokenTtl,
        enableRegistration = enableRegistration,
        requireEmailForRegistration = requireEmailForRegistration,
        registrationEmailSender = registrationEmailSender,
    )

    /** Auth config exposes both registration flags without server-only types. */
    @Test
    fun getConfigReturnsConfiguredRegistrationFlags() = runTest {
        val service = buildService(
            usersRepo = FakeUsersRepo(),
            enableRegistration = true,
            requireEmailForRegistration = true,
        )

        assertEquals(AuthConfig(true, true), service.getConfig())
    }

    /** Required-email registration rejects a missing address before creating a user. */
    @Test
    fun requiredEmailRegistrationRejectsMissingEmail() = runTest {
        val usersRepo = FakeUsersRepo()
        val service = buildService(
            usersRepo = usersRepo,
            enableRegistration = true,
            requireEmailForRegistration = true,
            registrationEmailSender = FakeRegistrationEmailSender(true),
        )

        assertNull(service.register(Username("alice"), plainPassword))
        assertNull(usersRepo.getUserByUsername(Username("alice")))
    }

    /** Optional-email registration accepts and persists a supplied address. */
    @Test
    fun optionalEmailRegistrationPersistsSuppliedEmail() = runTest {
        val usersRepo = FakeUsersRepo()
        val email = Email("alice@example.com")
        val service = buildService(usersRepo, enableRegistration = true)

        assertNotNull(service.register(Username("alice"), plainPassword, email))
        assertEquals(email, usersRepo.getUserByUsername(Username("alice"))?.email)
    }

    /** Required-email registration returns credentials only after the sender accepts the invite. */
    @Test
    fun requiredEmailRegistrationSendsInviteBeforeReturningCredentials() = runTest {
        val usersRepo = FakeUsersRepo()
        val sender = FakeRegistrationEmailSender(true)
        val email = Email("alice@example.com")
        val service = buildService(
            usersRepo = usersRepo,
            enableRegistration = true,
            requireEmailForRegistration = true,
            registrationEmailSender = sender,
        )

        assertNotNull(service.register(Username("alice"), plainPassword, email))
        assertEquals(
            listOfNotNull(usersRepo.getUserByUsername(Username("alice"))),
            sender.users
        )
    }

    /** Required-email registration fails closed when invite delivery reports failure. */
    @Test
    fun requiredEmailRegistrationHidesCredentialsWhenInviteFails() = runTest {
        val sender = FakeRegistrationEmailSender(false)
        val service = buildService(
            usersRepo = FakeUsersRepo(),
            enableRegistration = true,
            requireEmailForRegistration = true,
            registrationEmailSender = sender,
        )

        assertNull(service.register(Username("alice"), plainPassword, Email("alice@example.com")))
        assertTrue(sender.users.isNotEmpty())
    }

    /** A failed invite removes the account and password so a same-username retry can succeed. */
    @Test
    fun requiredEmailRegistrationCleansUpFailedInviteAndAllowsRetry() = runTest {
        val usersRepo = FakeUsersRepo()
        val passwordsRepo = FakePasswordsRepo()
        val sender = FakeRegistrationEmailSender(false, true)
        val service = buildService(
            usersRepo = usersRepo,
            passwordsRepo = passwordsRepo,
            enableRegistration = true,
            requireEmailForRegistration = true,
            registrationEmailSender = sender,
        )
        val username = Username("alice")
        val email = Email("alice@example.com")

        assertNull(service.register(username, plainPassword, email))
        assertNull(usersRepo.getUserByUsername(username))
        assertTrue(passwordsRepo.getAll().isEmpty())

        assertNotNull(service.register(username, plainPassword, email))
        assertNotNull(usersRepo.getUserByUsername(username))
        assertEquals(1, passwordsRepo.getAll().size)
    }

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
