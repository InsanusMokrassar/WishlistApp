package dev.inmo.wishlist.features.auth.server.services

import dev.inmo.micro_utils.repos.MapCRUDRepo
import dev.inmo.micro_utils.repos.MapKeyValueRepo
import dev.inmo.wishlist.features.auth.common.models.AuthFeatureUser
import dev.inmo.wishlist.features.auth.common.models.AuthConfig
import dev.inmo.wishlist.features.auth.common.models.Password
import dev.inmo.wishlist.features.auth.common.models.Token
import dev.inmo.wishlist.features.auth.server.RegistrationEmailSender
import dev.inmo.wishlist.features.auth.server.RegistrationRoleLifecycle
import dev.inmo.wishlist.features.auth.server.repo.PasswordsRepo
import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.users.common.models.NewUser
import dev.inmo.wishlist.features.users.common.models.RegisteredUser
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.users.common.models.Username
import dev.inmo.wishlist.features.users.common.repo.UsersRepo
import dev.inmo.wishlist.features.users.common.repo.exceptions.DuplicateUserFieldException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
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
        if (map.values.any { it.username == newValue.username }) {
            throw DuplicateUserFieldException()
        }
        if (newValue.email != null && map.values.any { it.email == newValue.email }) {
            throw DuplicateUserFieldException()
        }
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

/** Password storage that exposes a known finalization failure without retaining a password. */
private class ThrowingPasswordsRepo(
    /** Failure raised when required-email finalization attempts to persist a password. */
    private val failure: Throwable,
) : PasswordsRepo, dev.inmo.micro_utils.repos.KeyValueRepo<UserId, Password> by MapKeyValueRepo() {
    override suspend fun set(toSet: Map<UserId, Password>) {
        throw failure
    }
}

/** Password storage that records whether compensation purged authentication state. */
private class TrackingPasswordsRepo(
    /** Storage that handles operations other than the intercepted purge. */
    private val delegate: PasswordsRepo = FakePasswordsRepo(),
) : PasswordsRepo by delegate {
    /** Number of batch password-removal calls issued by compensation. */
    var unsetCalls = 0
        private set

    override suspend fun unset(toUnset: List<UserId>) {
        unsetCalls++
        delegate.unset(toUnset)
    }
}

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

/** Sender that suspends until a test explicitly finishes or cancels delivery. */
private class SuspendingRegistrationEmailSender : RegistrationEmailSender {
    /** Completes after the provisional account reaches the delivery phase. */
    val started = CompletableDeferred<Unit>()

    /** Outcome supplied by the test to resume delivery. */
    val result = CompletableDeferred<Boolean>()

    /** Announces delivery and waits for [result]. */
    override suspend fun sendRegistrationEmail(user: RegisteredUser): Boolean {
        started.complete(Unit)
        return result.await()
    }
}

/** Sender that fails with an ordinary delivery exception. */
private object ThrowingRegistrationEmailSender : RegistrationEmailSender {
    /** Throws to exercise the normal failed-delivery compensation path. */
    override suspend fun sendRegistrationEmail(user: RegisteredUser): Boolean =
        error("SMTP failure")
}

/** Sender that propagates a caller-provided delivery cancellation. */
private class CancelingRegistrationEmailSender(
    /** Cancellation raised while the provisional account waits for delivery. */
    private val cancellation: CancellationException,
) : RegistrationEmailSender {
    override suspend fun sendRegistrationEmail(user: RegisteredUser): Boolean = throw cancellation
}

/** Records registration-specific pending and compensation transitions. */
private class FakeRegistrationRoleLifecycle(
    private val markResult: Boolean = true,
) : RegistrationRoleLifecycle {
    /** Accounts currently represented as holding direct registration roles. */
    val directRoleUserIds = mutableSetOf<UserId>()

    /** Number of pending transition attempts. */
    var markCalls: Int = 0
        private set

    /** Number of compensation cleanup attempts. */
    var removeCalls: Int = 0
        private set

    /** Records a pending role when [markResult] permits registration to continue. */
    override suspend fun markPending(userId: UserId): Boolean {
        markCalls++
        if (markResult) directRoleUserIds += userId
        return markResult
    }

    /** Removes every simulated direct role for [userId]. */
    override suspend fun removeRoles(userId: UserId) {
        removeCalls++
        directRoleUserIds -= userId
    }
}

/** Role lifecycle whose compensation phase fails after auth cleanup has started. */
private class CleanupFailingRegistrationRoleLifecycle(
    /** Failure raised when compensation tries to remove direct roles. */
    private val cleanupFailure: Throwable,
) : RegistrationRoleLifecycle {
    /** Number of successful provisional pending-role transitions. */
    var markCalls = 0
        private set

    /** Number of attempted direct-role compensations. */
    var removeCalls = 0
        private set

    override suspend fun markPending(userId: UserId): Boolean {
        markCalls++
        return true
    }

    override suspend fun removeRoles(userId: UserId) {
        removeCalls++
        throw cleanupFailure
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
        passwordsRepo: PasswordsRepo = FakePasswordsRepo(),
        tokenTtl: Duration = 15.minutes,
        enableRegistration: Boolean = false,
        requireEmailForRegistration: Boolean = false,
        registrationEmailSender: RegistrationEmailSender? = null,
        registrationRoleLifecycle: RegistrationRoleLifecycle? = null,
    ) = AuthFeatureService(
        usersRepo = usersRepo,
        writeUsersRepo = usersRepo,
        passwordsRepo = passwordsRepo,
        tokenTtl = tokenTtl,
        enableRegistration = enableRegistration,
        requireEmailForRegistration = requireEmailForRegistration,
        registrationEmailSender = registrationEmailSender,
        registrationRoleLifecycle = registrationRoleLifecycle,
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
            registrationRoleLifecycle = FakeRegistrationRoleLifecycle(),
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
        val lifecycle = FakeRegistrationRoleLifecycle()
        val email = Email("alice@example.com")
        val service = buildService(
            usersRepo = usersRepo,
            enableRegistration = true,
            requireEmailForRegistration = true,
            registrationEmailSender = sender,
            registrationRoleLifecycle = lifecycle,
        )

        assertNotNull(service.register(Username("alice"), plainPassword, email))
        assertEquals(
            listOfNotNull(usersRepo.getUserByUsername(Username("alice"))),
            sender.users
        )
        assertEquals(sender.users.map { it.id }.toSet(), lifecycle.directRoleUserIds)
    }

    /** Required-email registration fails closed when invite delivery reports failure. */
    @Test
    fun requiredEmailRegistrationHidesCredentialsWhenInviteFails() = runTest {
        val sender = FakeRegistrationEmailSender(false)
        val usersRepo = FakeUsersRepo()
        val lifecycle = FakeRegistrationRoleLifecycle()
        val service = buildService(
            usersRepo = usersRepo,
            enableRegistration = true,
            requireEmailForRegistration = true,
            registrationEmailSender = sender,
            registrationRoleLifecycle = lifecycle,
        )

        assertNull(service.register(Username("alice"), plainPassword, Email("alice@example.com")))
        assertTrue(sender.users.isNotEmpty())
        assertTrue(usersRepo.getAll().isEmpty())
        assertTrue(lifecycle.directRoleUserIds.isEmpty())
    }

    /** A failed invite removes the account and password so a same-username retry can succeed. */
    @Test
    fun requiredEmailRegistrationCleansUpFailedInviteAndAllowsRetry() = runTest {
        val usersRepo = FakeUsersRepo()
        val passwordsRepo = FakePasswordsRepo()
        val sender = FakeRegistrationEmailSender(false, true)
        val lifecycle = FakeRegistrationRoleLifecycle()
        val service = buildService(
            usersRepo = usersRepo,
            passwordsRepo = passwordsRepo,
            enableRegistration = true,
            requireEmailForRegistration = true,
            registrationEmailSender = sender,
            registrationRoleLifecycle = lifecycle,
        )
        val username = Username("alice")
        val email = Email("alice@example.com")

        assertNull(service.register(username, plainPassword, email))
        assertNull(usersRepo.getUserByUsername(username))
        assertTrue(passwordsRepo.getAll().isEmpty())
        assertTrue(lifecycle.directRoleUserIds.isEmpty())

        assertNotNull(service.register(username, plainPassword, email))
        assertNotNull(usersRepo.getUserByUsername(username))
        assertEquals(1, passwordsRepo.getAll().size)
        assertEquals(1, lifecycle.directRoleUserIds.size)
    }

    /** Required mode fails before persistence when the sender or role lifecycle is unavailable. */
    @Test
    fun requiredEmailRegistrationRequiresBothInfrastructureHooks() = runTest {
        val withoutSenderUsers = FakeUsersRepo()
        val withoutLifecycleUsers = FakeUsersRepo()
        val sender = FakeRegistrationEmailSender(true)

        assertNull(
            buildService(
                usersRepo = withoutSenderUsers,
                enableRegistration = true,
                requireEmailForRegistration = true,
                registrationRoleLifecycle = FakeRegistrationRoleLifecycle(),
            ).register(Username("alice"), plainPassword, Email("alice@example.com"))
        )
        assertNull(
            buildService(
                usersRepo = withoutLifecycleUsers,
                enableRegistration = true,
                requireEmailForRegistration = true,
                registrationEmailSender = sender,
            ).register(Username("bob"), plainPassword, Email("bob@example.com"))
        )

        assertTrue(withoutSenderUsers.getAll().isEmpty())
        assertTrue(withoutLifecycleUsers.getAll().isEmpty())
        assertTrue(sender.users.isEmpty())
    }

    /** A failed pending-role transition removes the provisional user before delivery. */
    @Test
    fun requiredEmailRegistrationCompensatesFailedPendingTransition() = runTest {
        val usersRepo = FakeUsersRepo()
        val sender = FakeRegistrationEmailSender(true)
        val lifecycle = FakeRegistrationRoleLifecycle(markResult = false)
        val service = buildService(
            usersRepo = usersRepo,
            enableRegistration = true,
            requireEmailForRegistration = true,
            registrationEmailSender = sender,
            registrationRoleLifecycle = lifecycle,
        )

        assertNull(service.register(Username("alice"), plainPassword, Email("alice@example.com")))
        assertTrue(usersRepo.getAll().isEmpty())
        assertTrue(sender.users.isEmpty())
        assertEquals(1, lifecycle.markCalls)
        assertEquals(1, lifecycle.removeCalls)
    }

    /** An ordinary sender exception is a compensated registration failure. */
    @Test
    fun requiredEmailRegistrationCompensatesSenderException() = runTest {
        val usersRepo = FakeUsersRepo()
        val passwordsRepo = FakePasswordsRepo()
        val lifecycle = FakeRegistrationRoleLifecycle()
        val service = buildService(
            usersRepo = usersRepo,
            passwordsRepo = passwordsRepo,
            enableRegistration = true,
            requireEmailForRegistration = true,
            registrationEmailSender = ThrowingRegistrationEmailSender,
            registrationRoleLifecycle = lifecycle,
        )

        assertNull(service.register(Username("alice"), plainPassword, Email("alice@example.com")))
        assertTrue(usersRepo.getAll().isEmpty())
        assertTrue(passwordsRepo.getAll().isEmpty())
        assertTrue(lifecycle.directRoleUserIds.isEmpty())
    }

    /** A final password-store failure propagates only after provisional state is compensated. */
    @Test
    fun requiredEmailRegistrationPropagatesUnexpectedFinalizationFailureAfterCompensation() = runTest {
        val failure = IllegalStateException("password storage unavailable")
        val usersRepo = FakeUsersRepo()
        val passwordsRepo = ThrowingPasswordsRepo(failure)
        val lifecycle = FakeRegistrationRoleLifecycle()
        val service = buildService(
            usersRepo = usersRepo,
            passwordsRepo = passwordsRepo,
            enableRegistration = true,
            requireEmailForRegistration = true,
            registrationEmailSender = FakeRegistrationEmailSender(true),
            registrationRoleLifecycle = lifecycle,
        )

        val thrown = assertFailsWith<IllegalStateException> {
            service.register(Username("alice"), plainPassword, Email("alice@example.com"))
        }

        assertSame(failure, thrown)
        assertTrue(usersRepo.getAll().isEmpty())
        assertTrue(passwordsRepo.getAll().isEmpty())
        assertTrue(lifecycle.directRoleUserIds.isEmpty())
    }

    /** External delivery does not block unrelated auth and leaves the provisional account unusable. */
    @Test
    fun requiredEmailDeliveryRunsOutsideGlobalAuthLock() = runTest {
        val existing = RegisteredUser(UserId(42L), Username("existing"), Email("existing@example.com"))
        val usersRepo = FakeUsersRepo(mapOf(existing.id to existing))
        val passwordsRepo = FakePasswordsRepo()
        val sender = SuspendingRegistrationEmailSender()
        val lifecycle = FakeRegistrationRoleLifecycle()
        val service = buildService(
            usersRepo = usersRepo,
            passwordsRepo = passwordsRepo,
            enableRegistration = true,
            requireEmailForRegistration = true,
            registrationEmailSender = sender,
            registrationRoleLifecycle = lifecycle,
        )
        service.setPassword(existing.id, plainPassword)

        val registration = async {
            service.register(Username("alice"), plainPassword, Email("alice@example.com"))
        }
        sender.started.await()
        val provisional = usersRepo.getUserByUsername(Username("alice"))!!

        assertNotNull(withTimeout(1_000) { service.login(existing.username, plainPassword) })
        assertNull(withTimeout(1_000) { service.login(provisional.username, plainPassword) })
        assertNull(passwordsRepo.get(provisional.id))

        sender.result.complete(true)
        assertNotNull(registration.await())
        assertNotNull(passwordsRepo.get(provisional.id))
        assertTrue(provisional.id in lifecycle.directRoleUserIds)
    }

    /** Delivery cancellation propagates after non-cancellable user, password, and role cleanup. */
    @Test
    fun requiredEmailDeliveryCancellationPropagatesAfterCompensation() = runTest {
        val usersRepo = FakeUsersRepo()
        val passwordsRepo = FakePasswordsRepo()
        val sender = SuspendingRegistrationEmailSender()
        val lifecycle = FakeRegistrationRoleLifecycle()
        val service = buildService(
            usersRepo = usersRepo,
            passwordsRepo = passwordsRepo,
            enableRegistration = true,
            requireEmailForRegistration = true,
            registrationEmailSender = sender,
            registrationRoleLifecycle = lifecycle,
        )
        val registration = async {
            service.register(Username("alice"), plainPassword, Email("alice@example.com"))
        }
        sender.started.await()

        registration.cancel()
        assertFailsWith<CancellationException> { registration.await() }

        assertTrue(usersRepo.getAll().isEmpty())
        assertTrue(passwordsRepo.getAll().isEmpty())
        assertTrue(lifecycle.directRoleUserIds.isEmpty())
    }

    /** Delivery cancellation retains a cleanup failure after user and auth-state compensation runs. */
    @Test
    fun requiredEmailRegistrationCancellationRetainsCleanupFailure() = runTest {
        val cancellation = CancellationException("delivery cancelled")
        val cleanupFailure = IllegalStateException("role cleanup unavailable")
        val usersRepo = FakeUsersRepo()
        val passwordsRepo = TrackingPasswordsRepo()
        val lifecycle = CleanupFailingRegistrationRoleLifecycle(cleanupFailure)
        val service = buildService(
            usersRepo = usersRepo,
            passwordsRepo = passwordsRepo,
            enableRegistration = true,
            requireEmailForRegistration = true,
            registrationEmailSender = CancelingRegistrationEmailSender(cancellation),
            registrationRoleLifecycle = lifecycle,
        )

        val thrown = assertFailsWith<CancellationException> {
            service.register(Username("alice"), plainPassword, Email("alice@example.com"))
        }

        assertSame(cancellation, thrown)
        assertTrue(thrown.suppressed.single() is IllegalStateException)
        assertEquals(cleanupFailure.message, thrown.suppressed.single().message)
        assertTrue(usersRepo.getAll().isEmpty())
        assertEquals(1, passwordsRepo.unsetCalls)
        assertEquals(1, lifecycle.markCalls)
        assertEquals(1, lifecycle.removeCalls)
    }

    /** A duplicate email maps to null without invoking delivery or role transitions. */
    @Test
    fun duplicateEmailRegistrationUsesExistingFailureContract() = runTest {
        val existing = RegisteredUser(UserId(7L), Username("existing"), Email("shared@example.com"))
        val usersRepo = FakeUsersRepo(mapOf(existing.id to existing))
        val sender = FakeRegistrationEmailSender(true)
        val lifecycle = FakeRegistrationRoleLifecycle()
        val service = buildService(
            usersRepo = usersRepo,
            enableRegistration = true,
            requireEmailForRegistration = true,
            registrationEmailSender = sender,
            registrationRoleLifecycle = lifecycle,
        )

        assertNull(service.register(Username("distinct"), plainPassword, existing.email))
        assertEquals(setOf(existing.id), usersRepo.getAll().keys)
        assertTrue(sender.users.isEmpty())
        assertEquals(0, lifecycle.markCalls)
        assertFalse(existing.id in lifecycle.directRoleUserIds)
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
