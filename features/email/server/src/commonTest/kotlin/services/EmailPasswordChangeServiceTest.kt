package dev.inmo.wishlist.features.email.server.services

import dev.inmo.micro_utils.repos.KeyValueRepo
import dev.inmo.micro_utils.repos.MapKeyValueRepo
import dev.inmo.wishlist.features.auth.common.models.CompletePasswordChangeRequest
import dev.inmo.wishlist.features.auth.common.models.Password
import dev.inmo.wishlist.features.auth.common.models.PasswordChangeEmailRequestResult
import dev.inmo.wishlist.features.auth.common.models.PasswordChangeResult
import dev.inmo.wishlist.features.auth.server.UserRoleAuthorization
import dev.inmo.wishlist.features.auth.server.repo.PasswordsRepo
import dev.inmo.wishlist.features.auth.server.services.AuthFeatureService
import dev.inmo.wishlist.features.deeplinks.common.models.DeepLinkHandlerInfo
import dev.inmo.wishlist.features.deeplinks.common.models.DeepLinkId
import dev.inmo.wishlist.features.deeplinks.common.models.HandleResult
import dev.inmo.wishlist.features.deeplinks.common.repo.DeepLinksRepo
import dev.inmo.wishlist.features.deeplinks.server.services.DeepLinksService
import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.email.server.EmailsService
import dev.inmo.wishlist.features.email.server.models.EmailPasswordChange
import dev.inmo.wishlist.features.email.server.models.EmailPasswordChangePayload
import dev.inmo.wishlist.features.users.common.models.RegisteredUser
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.users.common.models.Username
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** In-memory deeplink persistence used by password-approval integration tests. */
private class PasswordChangeDeepLinksRepo : DeepLinksRepo,
    KeyValueRepo<DeepLinkId, DeepLinkHandlerInfo> by MapKeyValueRepo()

/** In-memory BCrypt storage used only by the Auth service exercised through Email orchestration. */
private class PasswordChangePasswordsRepo : PasswordsRepo,
    KeyValueRepo<UserId, Password> by MapKeyValueRepo()

/** Mutable direct-role authorization fixture for stale-role completion tests. */
private class PasswordChangeRoleAuthorization(
    /** Current direct User-role decision. */
    var directRolePresent: Boolean = true,
) : UserRoleAuthorization {
    /** Registration is outside these approval tests and never creates a role. */
    override suspend fun ensureUserRole(userId: UserId): Boolean = directRolePresent

    /** Returns the current direct-role decision. */
    override suspend fun hasUserRole(userId: UserId): Boolean = directRolePresent
}

/** Email transport that changes credentials during delivery to exercise post-delivery revalidation. */
private class CredentialReplacingEmailsService(
    private val beforeSuccess: suspend () -> Unit,
) : EmailsService by FakeEmailsService() {
    /** Applies the configured concurrent credential mutation before reporting successful delivery. */
    override suspend fun sendHtml(recipient: Email, subject: String, html: String): Boolean {
        beforeSuccess()
        return true
    }
}

/** Fully wired in-memory Email/Auth/deeplink fixture for one approval-bound account. */
private class PasswordChangeFixture(
    val user: RegisteredUser,
    val users: FakeUsersRepo,
    val roles: PasswordChangeRoleAuthorization,
    val passwords: PasswordChangePasswordsRepo,
    val auth: AuthFeatureService,
    val coordinator: EmailVerificationAccountCoordinator,
    val linksRepo: PasswordChangeDeepLinksRepo,
    val links: DeepLinksService,
    val service: EmailPasswordChangeService,
)

/** Exercises server-owned approval persistence, delivery compensation, and final Auth commit boundaries. */
class EmailPasswordChangeServiceTest {
    /** Approved account shared by every password-approval test. */
    private val user = RegisteredUser(
        id = UserId(7L),
        username = Username("owner"),
        email = Email("owner@example.com"),
        emailApproved = true,
    )

    /** Original credential used to prove completion changes only the approval subject. */
    private val oldPassword = Password("old-password")

    /** Builds an approval fixture with a deferred handler provider, matching the production DI cycle break. */
    private suspend fun fixture(
        emails: EmailsService? = FakeEmailsService(),
        nowEpochMillis: () -> Long = { 1_000L },
    ): PasswordChangeFixture {
        val users = FakeUsersRepo(mapOf(user.id to user))
        val roles = PasswordChangeRoleAuthorization()
        val passwords = PasswordChangePasswordsRepo()
        val auth = AuthFeatureService(
            usersRepo = users,
            writeUsersRepo = users,
            passwordsRepo = passwords,
            userRoleAuthorization = roles,
        )
        auth.setPassword(user.id, oldPassword)
        val coordinator = EmailVerificationAccountCoordinator(users, FakeRolesRepo())
        val linksRepo = PasswordChangeDeepLinksRepo()
        lateinit var service: EmailPasswordChangeService
        val links = DeepLinksService(
            linksRepo,
            listOf(EmailPasswordChangeDeepLinkHandler { service }),
        )
        service = EmailPasswordChangeService(
            emailsService = emails,
            deepLinksService = links,
            accountCoordinator = coordinator,
            authFeatureService = auth,
            publicHttpOrigin = "https://wishlist.example",
            nowEpochMillis = nowEpochMillis,
        )
        return PasswordChangeFixture(user, users, roles, passwords, auth, coordinator, linksRepo, links, service)
    }

    /** Requests one approval, proves GET is read-only, then consumes the exact id once. */
    @Test
    fun deliveredApprovalRedirectsReadOnlyAndChangesOnlyItsSubjectOnce() = runTest {
        val fixture = fixture()

        assertEquals(
            PasswordChangeEmailRequestResult.Sent,
            fixture.service.requestPasswordChangeEmail(fixture.user.id, fixture.user.email!!),
        )
        val approvalId = fixture.linksRepo.getAll().keys.single()
        val payload = fixture.linksRepo.get(approvalId)?.value as EmailPasswordChangePayload
        assertEquals(EmailPasswordChange.handlerId, fixture.linksRepo.get(approvalId)?.handlerId)
        assertTrue(payload.toString().contains("redacted"))

        assertEquals(
            HandleResult.Handled.Redirect("/password-change/${fixture.user.id.long}/${approvalId.string}"),
            fixture.links.handle(approvalId),
        )
        assertTrue(fixture.linksRepo.get(approvalId) != null)

        val request = CompletePasswordChangeRequest(fixture.user.id, approvalId, Password("new-password"))
        assertEquals(PasswordChangeResult.Changed, fixture.service.completePasswordChange(request))
        assertNull(fixture.linksRepo.get(approvalId))
        assertNull(fixture.auth.login(fixture.user.username, oldPassword))
        assertTrue(fixture.auth.login(fixture.user.username, Password("new-password")) != null)
        assertEquals(PasswordChangeResult.InvalidApproval, fixture.service.completePasswordChange(request))
    }

    /** Expiry, stale email, revoked direct role, and subject mismatch all reject without password mutation. */
    @Test
    fun invalidApprovalStatesNeverReplaceThePassword() = runTest {
        var now = 1_000L
        val fixture = fixture(nowEpochMillis = { now })
        assertEquals(
            PasswordChangeEmailRequestResult.Sent,
            fixture.service.requestPasswordChangeEmail(fixture.user.id, fixture.user.email!!),
        )
        val approvalId = fixture.linksRepo.getAll().keys.single()
        val validRequest = CompletePasswordChangeRequest(fixture.user.id, approvalId, Password("new-password"))

        now += 15L * 60L * 1000L
        assertEquals(PasswordChangeResult.InvalidApproval, fixture.service.completePasswordChange(validRequest))
        assertTrue(fixture.auth.login(fixture.user.username, oldPassword) != null)

        now = 1_000L
        fixture.coordinator.updateStoredEmail(fixture.user.id, Email("changed@example.com"))
        assertEquals(PasswordChangeResult.InvalidApproval, fixture.service.completePasswordChange(validRequest))
        assertTrue(fixture.auth.login(fixture.user.username, oldPassword) != null)

        fixture.roles.directRolePresent = false
        assertEquals(PasswordChangeResult.InvalidApproval, fixture.service.completePasswordChange(validRequest))
        assertTrue(fixture.auth.login(fixture.user.username, oldPassword) == null)
    }

    /** Missing infrastructure, a false send, and delivery cancellation retain no exact newly minted link. */
    @Test
    fun unavailableAndCancelledDeliveryFailClosedWithExactLinkCleanup() = runTest {
        val unavailable = fixture(emails = null)
        assertEquals(
            PasswordChangeEmailRequestResult.Unavailable,
            unavailable.service.requestPasswordChangeEmail(unavailable.user.id, unavailable.user.email!!),
        )
        assertTrue(unavailable.linksRepo.getAll().isEmpty())

        val failedDelivery = fixture(emails = FakeEmailsService(result = false))
        assertEquals(
            PasswordChangeEmailRequestResult.DeliveryFailed,
            failedDelivery.service.requestPasswordChangeEmail(failedDelivery.user.id, failedDelivery.user.email!!),
        )
        assertTrue(failedDelivery.linksRepo.getAll().isEmpty())

        val cancelledDelivery = fixture(emails = FakeEmailsService(failure = CancellationException("cancelled")))
        assertFailsWith<CancellationException> {
            cancelledDelivery.service.requestPasswordChangeEmail(cancelledDelivery.user.id, cancelledDelivery.user.email!!)
        }
        assertTrue(cancelledDelivery.linksRepo.getAll().isEmpty())

        val thrownDelivery = fixture(emails = FakeEmailsService(failure = IllegalStateException("smtp failed")))
        assertEquals(
            PasswordChangeEmailRequestResult.DeliveryFailed,
            thrownDelivery.service.requestPasswordChangeEmail(thrownDelivery.user.id, thrownDelivery.user.email!!),
        )
        assertTrue(thrownDelivery.linksRepo.getAll().isEmpty())
    }

    /** SMTP receives the approved address and one fixed URL containing the persisted approval id. */
    @Test
    fun sentApprovalUsesExactApprovedRecipientAndPersistedLink() = runTest {
        val emails = FakeEmailsService()
        val fixture = fixture(emails)
        assertEquals(PasswordChangeEmailRequestResult.Sent, fixture.service.requestPasswordChangeEmail(fixture.user.id, fixture.user.email!!))
        val approvalId = fixture.linksRepo.getAll().keys.single()
        assertEquals(fixture.user.email, emails.sendHtmlCalls.single().recipient)
        assertTrue(emails.sendHtmlCalls.single().html.contains("https://wishlist.example/api/links/${approvalId.string}"))
        assertTrue(fixture.linksRepo.get(approvalId) != null)
    }

    /** Credential replacement during SMTP causes post-delivery validation to remove the new approval. */
    @Test
    fun postDeliveryCredentialChangeInvalidatesAndCleansTheApproval() = runTest {
        lateinit var createdFixture: PasswordChangeFixture
        val emails = CredentialReplacingEmailsService {
            createdFixture.auth.setPassword(createdFixture.user.id, Password("replacement-password"))
        }
        createdFixture = fixture(emails = emails)

        assertEquals(
            PasswordChangeEmailRequestResult.Ineligible,
            createdFixture.service.requestPasswordChangeEmail(createdFixture.user.id, createdFixture.user.email!!),
        )
        assertTrue(createdFixture.linksRepo.getAll().isEmpty())
        assertTrue(createdFixture.auth.login(createdFixture.user.username, Password("replacement-password")) != null)
    }

    /** Concurrent completion attempts with one persisted UUID can accept at most one password write. */
    @Test
    fun concurrentCompletionConsumesAtMostOneApproval() = runTest {
        val fixture = fixture()
        assertEquals(
            PasswordChangeEmailRequestResult.Sent,
            fixture.service.requestPasswordChangeEmail(fixture.user.id, fixture.user.email!!),
        )
        val approvalId = fixture.linksRepo.getAll().keys.single()
        val request = CompletePasswordChangeRequest(fixture.user.id, approvalId, Password("new-password"))

        val outcomes = listOf(
            async { fixture.service.completePasswordChange(request) },
            async { fixture.service.completePasswordChange(request) },
        ).awaitAll()

        assertEquals(1, outcomes.count { it == PasswordChangeResult.Changed })
        assertEquals(1, outcomes.count { it == PasswordChangeResult.InvalidApproval })
        assertNull(fixture.linksRepo.get(approvalId))
        assertFalse(fixture.auth.login(fixture.user.username, oldPassword) != null)
    }
}
