package dev.inmo.wishlist.features.email.server.services

import dev.inmo.kroles.repos.BaseRoleSubject
import dev.inmo.kroles.repos.RolesRepo
import dev.inmo.kroles.roles.BaseRole
import dev.inmo.wishlist.features.deeplinks.common.DeepLinkHandler
import dev.inmo.wishlist.features.deeplinks.common.models.DeepLinkId
import dev.inmo.wishlist.features.deeplinks.common.models.HandleResult
import dev.inmo.wishlist.features.email.common.EmailConstants
import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.email.common.models.EmailProfile
import dev.inmo.wishlist.features.email.server.EmailFeature
import dev.inmo.wishlist.features.email.server.Plugin
import dev.inmo.wishlist.features.email.server.models.EmailVerificationPayload
import dev.inmo.wishlist.features.roles.common.models.NewUserRole
import dev.inmo.wishlist.features.roles.common.models.UserRole
import dev.inmo.wishlist.features.roles.server.RolesFeature
import dev.inmo.wishlist.features.users.common.models.RegisteredUser
import dev.inmo.wishlist.features.users.common.models.NewUser
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.users.common.models.Username
import dev.inmo.wishlist.features.users.common.repo.UsersRepo
import dev.inmo.wishlist.features.users.common.repo.exceptions.DuplicateUserFieldException
import dev.inmo.wishlist.features.users.common.repo.exceptions.EmailChangeCooldownException
import dev.inmo.wishlist.features.users.common.repo.ExposedUsersRepo
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import org.koin.core.KoinApplication
import org.koin.dsl.module
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Role repository wrapper that pauses a pending-to-approved transition at its first role mutation.
 *
 * The pause creates a deterministic observation point after invited-email equality has succeeded
 * while the account coordinator still owns its mutex. The wrapper also records the stored address
 * observed when [UserRole] is granted.
 *
 * @param usersRepo User-state fixture inspected at the approval grant.
 * @param userId Account whose address is recorded at the approval grant.
 * @param delegate In-memory role repository receiving all operations.
 */
private class BlockingPromotionRolesRepo(
    private val usersRepo: FakeUsersRepo,
    private val userId: UserId,
    private val delegate: FakeRolesRepo = FakeRolesRepo(),
) : RolesRepo by delegate {
    /** Signals that promotion reached exclusion of the pending role. */
    val promotionEntered = CompletableDeferred<Unit>()

    /** Gate that keeps promotion suspended while the coordinator mutex is observed. */
    val releasePromotion = CompletableDeferred<Unit>()

    /** Address stored when approval grants [UserRole], or `null` before that grant. */
    var emailAtUserRoleGrant: Email? = null
        private set

    /**
     * Suspends the pending-role exclusion until [releasePromotion] is completed.
     *
     * @param subject Role subject being changed.
     * @param role Role being removed.
     * @return Whether the delegated repository changed.
     */
    override suspend fun excludeDirect(subject: BaseRoleSubject, role: BaseRole): Boolean {
        if (role == NewUserRole) {
            promotionEntered.complete(Unit)
            releasePromotion.await()
        }
        return delegate.excludeDirect(subject, role)
    }

    /**
     * Records current user state immediately before delegating an approved-role grant.
     *
     * @param subject Role subject being changed.
     * @param role Role being granted.
     * @return Whether the delegated repository changed.
     */
    override suspend fun includeDirect(subject: BaseRoleSubject, role: BaseRole): Boolean {
        if (role == UserRole) {
            emailAtUserRoleGrant = usersRepo.getById(userId)?.email
        }
        return delegate.includeDirect(subject, role)
    }
}

/** Verifies serial, forced-ordering, and real Koin-graph coordinator contracts. */
@OptIn(ExperimentalCoroutinesApi::class)
class EmailVerificationAccountCoordinatorTest {
    /** Address that received the verification link. */
    private val invitedEmail = Email("alice@example.com")

    /** Address written by the competing self-service update. */
    private val changedEmail = Email("changed@example.com")

    /** Pending account used by every coordinator-ordering test. */
    private val user = RegisteredUser(UserId(7L), Username("alice"), invitedEmail)

    /** Rejects cached-user reads so the test proves the coordinator uses only the fresh profile capability. */
    private class FreshProfileOnlyUsersRepo(
        private val delegate: FakeUsersRepo,
    ) : UsersRepo by delegate {
        override suspend fun getByIdFresh(id: UserId): RegisteredUser? =
            error("Coordinator must not read a user projection for email state")
    }

    /** Direct role subject corresponding to [user]. */
    private val subject = BaseRoleSubject.Direct(user.id.long.toString())

    /** Deeplink id required by the handler contract. */
    private val deeplinkId = DeepLinkId("verification-7")

    /** Full fixture updates clear explicit null addresses before nullable same-slot no-op checks. */
    @Test
    fun fakeFullUpdateClearsLifecycleAndRetainsSameAddressNoOps() = runTest {
        val unapproved = RegisteredUser(UserId(71L), Username("fake-unapproved"), Email("fake-unapproved@example.com"))
        val approved = RegisteredUser(UserId(72L), Username("fake-approved"), Email("fake-approved@example.com"), true)
        val unapprovedProfile = EmailProfile(
            userId = unapproved.id.long,
            email = unapproved.email,
            emailChangeRequestedAt = 10L,
        )
        val approvedProfile = EmailProfile(
            userId = approved.id.long,
            email = approved.email,
            emailApproved = true,
            emailChangeAllowedAt = 20L,
        )
        val usersRepo = FakeUsersRepo(
            initialUsers = mapOf(unapproved.id to unapproved, approved.id to approved),
            initialEmailProfiles = mapOf(unapproved.id to unapprovedProfile, approved.id to approvedProfile),
        )

        assertEquals(listOf(unapproved), usersRepo.update(listOf(unapproved.id to NewUser(unapproved.username, unapproved.email))))
        assertEquals(unapprovedProfile, usersRepo.getEmailProfileFresh(unapproved.id))
        assertEquals(listOf(approved), usersRepo.update(listOf(approved.id to NewUser(approved.username, approved.email))))
        assertEquals(approvedProfile, usersRepo.getEmailProfileFresh(approved.id))

        assertEquals(
            listOf(RegisteredUser(unapproved.id, unapproved.username)),
            usersRepo.update(listOf(unapproved.id to NewUser(unapproved.username, null))),
        )
        assertEquals(EmailProfile(userId = unapproved.id.long), usersRepo.getEmailProfileFresh(unapproved.id))
        assertEquals(
            listOf(RegisteredUser(approved.id, approved.username)),
            usersRepo.update(listOf(approved.id to NewUser(approved.username, null))),
        )
        assertEquals(EmailProfile(userId = approved.id.long), usersRepo.getEmailProfileFresh(approved.id))
    }

    /**
     * Builds a real Email plugin Koin application with controlled repository dependencies.
     *
     * @param config Root server config selecting the SMTP-enabled or disabled graph.
     * @param usersRepo User repository supplied to the real plugin module.
     * @param rolesRepo Role repository supplied to the real plugin module.
     * @return Isolated Koin application containing the real Email plugin definitions.
     */
    private fun createKoinApplication(
        config: JsonObject,
        usersRepo: UsersRepo,
        rolesRepo: RolesRepo,
    ): KoinApplication {
        val application = KoinApplication.init()
        application.modules(
            module {
                single<UsersRepo> { usersRepo }
                single<RolesRepo> { rolesRepo }
                single<RolesFeature> { FakeRolesFeature() }
                single<Json> { Json { ignoreUnknownKeys = true } }
                with(Plugin) { setupDI(config) }
            },
        )
        return application
    }

    /**
     * Returns a root config without an Email block.
     *
     * @return Config selecting [DisabledEmailFeature].
     */
    private fun smtpDisabledConfig(): JsonObject = buildJsonObject {}

    /**
     * Returns a root config with a valid Email block.
     *
     * @return Config selecting [EmailFeatureService].
     */
    private fun smtpEnabledConfig(): JsonObject = buildJsonObject {
        putJsonObject("email") {
            putJsonObject("smtp") {
                put("host", "smtp.example.com")
                put("from", "noreply@example.com")
            }
        }
    }

    /** Returns a valid graph configuration whose root policy issues a ten-millisecond restriction. */
    private fun positivePolicyConfig(smtpEnabled: Boolean): JsonObject = buildJsonObject {
        put("emailChangeCooldown", "PT0.01S")
        if (smtpEnabled) {
            putJsonObject("email") {
                putJsonObject("smtp") {
                    put("host", "smtp.example.com")
                    put("from", "noreply@example.com")
                }
            }
        }
    }

    /** Runs a real repository over a fixture-owned SQLite file and always unregisters its database. */
    private suspend fun withSqliteUsersRepo(
        nowMillis: () -> Long,
        block: suspend (ExposedUsersRepo) -> Unit,
    ) {
        val file = Files.createTempFile("wishlist-email-coordinator", ".sqlite")
        val database = Database.connect(url = "jdbc:sqlite:${file.toAbsolutePath()}", driver = "org.sqlite.JDBC")
        try {
            block(ExposedUsersRepo(database, nowMillis))
        } finally {
            try {
                TransactionManager.closeAndUnregister(database)
            } finally {
                Files.deleteIfExists(file)
            }
        }
    }

    /**
     * Forces verification to own the coordinator before a self-service update starts.
     *
     * @param feature Real enabled or disabled feature using the shared coordinator.
     * @param handler Real verification handler using the shared coordinator.
     * @param usersRepo User-state fixture seeded with invited address A.
     * @param rolesRepo Blocking role fixture that exposes the promotion critical section.
     */
    private suspend fun TestScope.assertVerificationFirst(
        feature: EmailFeature,
        handler: EmailVerificationDeepLinkHandler,
        usersRepo: FakeUsersRepo,
        rolesRepo: BlockingPromotionRolesRepo,
    ) {
        rolesRepo.includeDirect(subject, NewUserRole)
        val verification = async {
            handler.tryHandle(deeplinkId, EmailVerificationPayload(user.id, invitedEmail))
        }
        rolesRepo.promotionEntered.await()

        val update = async { feature.setMyEmail(user.id, changedEmail) }
        runCurrent()

        assertFalse(update.isCompleted)
        assertEquals(invitedEmail, usersRepo.getById(user.id)?.email)

        rolesRepo.releasePromotion.complete(Unit)
        assertEquals(
            HandleResult.Handled.Redirect(EmailConstants.approvalRedirectPath),
            verification.await(),
        )
        assertEquals(invitedEmail, rolesRepo.emailAtUserRoleGrant)
        assertTrue(update.await())
        assertEquals(invitedEmail, usersRepo.getById(user.id)?.email)
        assertEquals(changedEmail, usersRepo.getEmailProfileFresh(user.id)?.pendingEmail)
        assertTrue(checkNotNull(usersRepo.getById(user.id)).emailApproved)
        assertEquals(setOf(UserRole), rolesRepo.getDirectRoles(subject).toSet())
    }

    /**
     * Verifies one real Koin graph contains one coordinator shared by feature and handler behavior.
     *
     * @param config Root config selecting one Email feature realization.
     * @param expectEnabled Whether [EmailFeatureService] must be selected.
     */
    private suspend fun TestScope.assertKoinGraphSharesCoordinator(
        config: JsonObject,
        expectEnabled: Boolean,
    ) {
        val profile = EmailProfile(
            userId = user.id.long,
            email = invitedEmail,
            emailChangeRequestedAt = 100L,
        )
        val usersRepo = FakeUsersRepo(
            initialUsers = mapOf(user.id to user),
            initialEmailProfiles = mapOf(user.id to profile),
        )
        val rolesRepo = BlockingPromotionRolesRepo(usersRepo, user.id)
        val application = createKoinApplication(config, usersRepo, rolesRepo)
        try {
            val coordinators = application.koin.getAll<EmailVerificationAccountCoordinator>()
            assertEquals(1, coordinators.size)
            assertSame(coordinators.single(), application.koin.get<EmailVerificationAccountCoordinator>())
            assertSame(
                application.koin.get<EmailVerificationAccountCoordinator>(),
                application.koin.get<EmailVerificationAccountCoordinator>(),
            )

            val feature = application.koin.get<EmailFeature>()
            when {
                expectEnabled -> assertIs<EmailFeatureService>(feature)
                else -> assertIs<DisabledEmailFeature>(feature)
            }
            assertEquals(profile, feature.getMyEmail(user.id))
            assertEquals(listOf(user.id), usersRepo.emailProfileReadCalls)
            val handler = application.koin.getAll<DeepLinkHandler>()
                .filterIsInstance<EmailVerificationDeepLinkHandler>()
                .single()

            assertVerificationFirst(feature, handler, usersRepo, rolesRepo)
        } finally {
            application.close()
        }
    }

    /** Mutation stores and clears addresses, preserves username, and reports a missing user. */
    @Test
    fun updateStoredEmailPreservesSerialContract() = runTest {
        val usersRepo = FakeUsersRepo(mapOf(user.id to user))
        val coordinator = EmailVerificationAccountCoordinator(usersRepo, FakeRolesRepo())

        assertTrue(coordinator.updateStoredEmail(user.id, changedEmail))
        assertEquals(user.username, usersRepo.getById(user.id)?.username)
        assertEquals(changedEmail, usersRepo.getById(user.id)?.email)
        assertTrue(coordinator.updateStoredEmail(user.id, null))
        assertEquals(null, usersRepo.getById(user.id)?.email)
        assertFalse(coordinator.updateStoredEmail(UserId(999L), changedEmail))
    }

    /** Fresh reads stay under the shared mutex, bypass user projections, and propagate repository failures. */
    @Test
    fun getCurrentEmailProfileUsesMandatoryFreshProfileReadAndPropagatesErrors() = runTest {
        val profile = EmailProfile(
            userId = user.id.long,
            email = invitedEmail,
            emailApproved = true,
            pendingEmail = changedEmail,
            emailChangeRequestedAt = 100L,
            emailChangeAllowedAt = 200L,
        )
        val fake = FakeUsersRepo(
            initialUsers = mapOf(user.id to user),
            initialEmailProfiles = mapOf(user.id to profile),
        )
        val coordinator = EmailVerificationAccountCoordinator(FreshProfileOnlyUsersRepo(fake), FakeRolesRepo())

        assertEquals(profile, coordinator.getCurrentEmailProfile(user.id))
        fake.emailProfileReadFailure = IllegalStateException("fresh profile failed")
        assertFailsWith<IllegalStateException> { coordinator.getCurrentEmailProfile(user.id) }
        assertEquals(listOf(user.id, user.id), fake.emailProfileReadCalls)
    }

    /** Duplicate propagation releases the mutex for a subsequent successful operation. */
    @Test
    fun duplicateUpdatePropagatesAndReleasesCoordinator() = runTest {
        val owner = RegisteredUser(UserId(1L), Username("owner"), changedEmail)
        val usersRepo = FakeUsersRepo(mapOf(owner.id to owner, user.id to user))
        val coordinator = EmailVerificationAccountCoordinator(usersRepo, FakeRolesRepo())

        assertFailsWith<DuplicateUserFieldException> {
            coordinator.updateStoredEmail(user.id, changedEmail)
        }

        assertTrue(coordinator.updateStoredEmail(user.id, Email("after-failure@example.com")))
    }

    /** Matching verification promotes idempotently, while legacy null payloads fail closed. */
    @Test
    fun verificationPreservesSerialMatchAndLegacyContracts() = runTest {
        val usersRepo = FakeUsersRepo(mapOf(user.id to user))
        val rolesRepo = FakeRolesRepo()
        rolesRepo.includeDirect(subject, NewUserRole)
        val coordinator = EmailVerificationAccountCoordinator(usersRepo, rolesRepo)

        assertFalse(coordinator.verifyInvitedEmailAndPromote(user.id, null))
        assertEquals(setOf(NewUserRole), rolesRepo.getDirectRoles(subject).toSet())
        assertTrue(coordinator.verifyInvitedEmailAndPromote(user.id, invitedEmail))
        assertTrue(coordinator.verifyInvitedEmailAndPromote(user.id, invitedEmail))
        assertEquals(setOf(UserRole), rolesRepo.getDirectRoles(subject).toSet())
    }

    /** Verification-first ordering blocks an SMTP-disabled update until approval with address A. */
    @Test
    fun verificationFirstBlocksDisabledFeatureUpdateUntilPromotion() = runTest {
        val usersRepo = FakeUsersRepo(mapOf(user.id to user))
        val rolesRepo = BlockingPromotionRolesRepo(usersRepo, user.id)
        val coordinator = EmailVerificationAccountCoordinator(usersRepo, rolesRepo)

        assertVerificationFirst(
            feature = DisabledEmailFeature(coordinator),
            handler = EmailVerificationDeepLinkHandler(coordinator),
            usersRepo = usersRepo,
            rolesRepo = rolesRepo,
        )
    }

    /** Verification-first ordering blocks an SMTP-enabled update until approval with address A. */
    @Test
    fun verificationFirstBlocksEnabledFeatureUpdateUntilPromotion() = runTest {
        val usersRepo = FakeUsersRepo(mapOf(user.id to user))
        val rolesRepo = BlockingPromotionRolesRepo(usersRepo, user.id)
        val coordinator = EmailVerificationAccountCoordinator(usersRepo, rolesRepo)

        assertVerificationFirst(
            feature = EmailFeatureService(FakeEmailsService(), coordinator, FakeRolesFeature()),
            handler = EmailVerificationDeepLinkHandler(coordinator),
            usersRepo = usersRepo,
            rolesRepo = rolesRepo,
        )
    }

    /** Update-first ordering makes an address-A link stale without entering role promotion. */
    @Test
    fun updateFirstRejectsStaleInviteAndLeavesPendingRole() = runTest {
        val usersRepo = FakeUsersRepo(mapOf(user.id to user))
        val rolesRepo = BlockingPromotionRolesRepo(usersRepo, user.id)
        rolesRepo.includeDirect(subject, NewUserRole)
        val coordinator = EmailVerificationAccountCoordinator(usersRepo, rolesRepo)
        val feature = DisabledEmailFeature(coordinator)
        val handler = EmailVerificationDeepLinkHandler(coordinator)

        assertTrue(feature.setMyEmail(user.id, changedEmail))
        assertEquals(null, handler.tryHandle(deeplinkId, EmailVerificationPayload(user.id, invitedEmail)))

        assertEquals(changedEmail, usersRepo.getById(user.id)?.email)
        assertEquals(setOf(NewUserRole), rolesRepo.getDirectRoles(subject).toSet())
        assertFalse(rolesRepo.promotionEntered.isCompleted)
    }

    /** SMTP-disabled real graph contains one coordinator shared by mutation and verification. */
    @Test
    fun smtpDisabledKoinGraphSharesOneCoordinator() = runTest {
        assertKoinGraphSharesCoordinator(smtpDisabledConfig(), expectEnabled = false)
    }

    /** SMTP-enabled real graph contains one coordinator shared by mutation and verification. */
    @Test
    fun smtpEnabledKoinGraphSharesOneCoordinator() = runTest {
        assertKoinGraphSharesCoordinator(smtpEnabledConfig(), expectEnabled = true)
    }

    /** Actual Plugin graphs enforce the same positive policy with real durable storage in both SMTP shapes. */
    @Test
    fun positivePolicyFromPluginGuardsEnabledAndDisabledRealRepositories() = runTest {
        listOf(false, true).forEach { smtpEnabled ->
            var now = 1_000L
            withSqliteUsersRepo(nowMillis = { now }) { usersRepo ->
                val addressA = Email("plugin-${smtpEnabled}-a@example.com")
                val addressB = Email("plugin-${smtpEnabled}-b@example.com")
                val created = usersRepo.create(listOf(NewUser(Username("plugin-$smtpEnabled"), addressA))).single()
                val rolesRepo = FakeRolesRepo()
                rolesRepo.includeDirect(BaseRoleSubject.Direct(created.id.long.toString()), NewUserRole)
                val application = createKoinApplication(positivePolicyConfig(smtpEnabled), usersRepo, rolesRepo)
                try {
                    val coordinator = application.koin.get<EmailVerificationAccountCoordinator>()
                    val feature = application.koin.get<EmailFeature>()
                    assertTrue(coordinator.verifyInvitedEmailAndPromote(created.id, addressA))
                    val approved = checkNotNull(usersRepo.getById(created.id))
                    assertEquals(1_010L, usersRepo.getEmailProfileFresh(created.id)?.emailChangeAllowedAt)

                    assertFailsWith<EmailChangeCooldownException> { feature.setMyEmail(created.id, addressB) }
                    assertFailsWith<EmailChangeCooldownException> { feature.setMyEmail(created.id, null) }
                    assertEquals(approved, usersRepo.getById(created.id))

                    assertTrue(feature.setMyEmail(created.id, addressA))
                    assertEquals(true, coordinator.updateUsername(created.id, Username("plugin-renamed-$smtpEnabled")))
                    assertEquals(approved.copy(username = Username("plugin-renamed-$smtpEnabled")), usersRepo.getById(created.id))

                    now = 1_010L
                    assertTrue(feature.setMyEmail(created.id, addressB))
                    assertEquals(
                        approved.copy(username = Username("plugin-renamed-$smtpEnabled")),
                        usersRepo.getById(created.id),
                    )
                    assertEquals(addressB, usersRepo.getEmailProfileFresh(created.id)?.pendingEmail)
                } finally {
                    application.close()
                }
            }
        }
    }
}
