package dev.inmo.wishlist.features.email.server.services

import dev.inmo.micro_utils.repos.MapKeyValueRepo
import dev.inmo.wishlist.features.deeplinks.common.models.DeepLinkHandlerInfo
import dev.inmo.wishlist.features.deeplinks.common.models.DeepLinkId
import dev.inmo.wishlist.features.deeplinks.common.repo.DeepLinksRepo
import dev.inmo.wishlist.features.deeplinks.server.services.DeepLinksService
import dev.inmo.wishlist.features.email.common.EmailConstants
import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.email.common.models.EmailVerificationRequestResult
import dev.inmo.wishlist.features.users.common.models.RegisteredUser
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.users.common.models.Username
import dev.inmo.wishlist.features.users.common.repo.exceptions.DuplicateUserFieldException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Verifies [EmailFeatureService]: [EmailFeatureService.isFeatureEnabled] always returns `true` (this
 * class is only ever constructed with a real, non-null `EmailsService` — see `DisabledEmailFeatureTest`
 * for the SMTP-disabled no-op path); [EmailFeatureService.sendTestEmail] delegates the privilege check
 * to `RolesFeature.isFunctionalityAvailable(callerId, email.sendTest)` before delegating exactly one
 * `EmailsService.sendText` call (an unknown `UserId` takes the same "not available" branch as any other
 * non-privileged caller); [EmailFeatureService.setMyEmail] persists through the shared coordinator
 * for a found user, unaffected by the role check.
 */
class EmailFeatureServiceTest {

    /** In-memory deep-link storage used by the real registration invite sender. */
    private class FakeDeepLinksRepo : DeepLinksRepo,
        dev.inmo.micro_utils.repos.KeyValueRepo<DeepLinkId, DeepLinkHandlerInfo> by MapKeyValueRepo()

    /** Fixture user used by every `setMyEmail` assertion (superadmin status is irrelevant there). */
    private val plainUser = RegisteredUser(UserId(2L), Username("alice"))

    /** Shared test-email recipient used by every `sendTestEmail` assertion. */
    private val recipient = Email("recipient@example.com")

    /**
     * Builds the SMTP-enabled feature around one coordinator for the supplied user repository.
     *
     * @param emailsService Delivery double used by send-test behavior.
     * @param usersRepo User-state double owned by the coordinator.
     * @param rolesFeature Authorization double used by send-test behavior.
     * @return SMTP-enabled feature using the supplied doubles.
     */
    private fun createService(
        emailsService: FakeEmailsService = FakeEmailsService(),
        usersRepo: FakeUsersRepo = FakeUsersRepo(),
        rolesFeature: FakeRolesFeature = FakeRolesFeature(),
        inviteSender: EmailRegistrationInviteSender? = null,
    ): EmailFeatureService = EmailFeatureService(
        emailsService = emailsService,
        accountCoordinator = EmailVerificationAccountCoordinator(usersRepo, FakeRolesRepo()),
        rolesFeature = rolesFeature,
        inviteSender = inviteSender,
    )

    /** Builds the real link-minting sender without requiring an external SMTP server. */
    private fun createInviteSender(emailsService: FakeEmailsService?): EmailRegistrationInviteSender =
        EmailRegistrationInviteSender(
            emailsService = emailsService,
            deepLinksService = DeepLinksService(FakeDeepLinksRepo(), emptyList()),
            publicHttpOrigin = "https://wishlist.example",
        )

    /** `isFeatureEnabled` unconditionally returns `true` — `emailsService` is a non-nullable constructor parameter, so this class is only ever constructed with a real transport. */
    @Test
    fun isFeatureEnabledAlwaysReturnsTrue() = runTest {
        val service = createService()
        assertTrue(service.isFeatureEnabled())
    }

    /** Superadmin caller + present `emailsService` → exactly one `sendText` call with the fixed subject/text; result is `sendText`'s own `true`. */
    @Test
    fun sendTestEmailDelegatesToSendTextForSuperAdminCallerAndReturnsTrueResult() = runTest {
        val emailsService = FakeEmailsService(result = true)
        val rolesFeature = FakeRolesFeature(result = true)
        val service = createService(emailsService, FakeUsersRepo(), rolesFeature)

        val result = service.sendTestEmail(plainUser.id, recipient)

        assertTrue(result)
        assertEquals(listOf(plainUser.id to EmailConstants.sendTestFunctionalityId), rolesFeature.calls)
        assertEquals(1, emailsService.sendTextCalls.size)
        val call = emailsService.sendTextCalls.single()
        assertEquals(recipient, call.recipient)
        assertEquals("Test email from WishlistApp", call.subject)
        assertEquals(
            "This is a test email sent from WishlistApp to verify SMTP configuration.",
            call.text
        )
    }

    /** Same as above but `sendText` fails — the `false` result must propagate through unchanged. */
    @Test
    fun sendTestEmailDelegatesToSendTextForSuperAdminCallerAndReturnsFalseResult() = runTest {
        val emailsService = FakeEmailsService(result = false)
        val service = createService(emailsService, FakeUsersRepo(), FakeRolesFeature(result = true))

        val result = service.sendTestEmail(plainUser.id, recipient)

        assertFalse(result)
        assertEquals(1, emailsService.sendTextCalls.size)
    }

    /** Non-superadmin caller (including an id unknown to `usersRepo`, now indistinguishable from any other non-superadmin caller) → `false`, and `sendText` must never be invoked. */
    @Test
    fun sendTestEmailReturnsFalseWhenCallerIsNotSuperAdminAndDoesNotCallSendText() = runTest {
        val emailsService = FakeEmailsService()
        val rolesFeature = FakeRolesFeature(result = false)
        val service = createService(emailsService, FakeUsersRepo(), rolesFeature)

        val result = service.sendTestEmail(UserId(999L), recipient)

        assertFalse(result)
        assertEquals(listOf(UserId(999L) to EmailConstants.sendTestFunctionalityId), rolesFeature.calls)
        assertEquals(0, emailsService.sendTextCalls.size)
    }

    /** A found user's stored email is updated and persisted via `UsersRepo`, exercising `EmailFeatureService.setMyEmail` directly (the SMTP-disabled path is covered separately by `DisabledEmailFeatureTest`); superadmin status is irrelevant to this method. */
    @Test
    fun setMyEmailPersistsViaUsersRepoForFoundUser() = runTest {
        val repo = FakeUsersRepo(mapOf(plainUser.id to plainUser))
        val service = createService(usersRepo = repo)
        val newEmail = Email("alice@example.com")

        val result = service.setMyEmail(plainUser.id, newEmail)

        assertTrue(result)
        assertEquals(newEmail, repo.getById(plainUser.id)?.email)
    }

    /** Caller id resolves to no user → `setMyEmail` returns `false`. */
    @Test
    fun setMyEmailReturnsFalseWhenUserNotFound() = runTest {
        val service = createService()

        assertFalse(service.setMyEmail(UserId(999L), Email("alice@example.com")))
    }

    /** setMyEmail propagates DuplicateUserFieldException, unmodified, when the target email is already stored for a different user. */
    @Test
    fun setMyEmailPropagatesDuplicateUserFieldExceptionWhenEmailAlreadyTaken() = runTest {
        val takenEmail = Email("taken@example.com")
        val ownerUser = plainUser.copy(id = UserId(1L), email = takenEmail)
        val repo = FakeUsersRepo(mapOf(ownerUser.id to ownerUser, plainUser.id to plainUser))
        val service = createService(usersRepo = repo)

        assertFailsWith<DuplicateUserFieldException> {
            service.setMyEmail(plainUser.id, takenEmail)
        }
    }

    /** Requesting verification returns the current-state results without invoking SMTP unnecessarily. */
    @Test
    fun requestVerificationReturnsNoEmailChangedAlreadyApprovedAndUnavailable() = runTest {
        val email = Email("verify@example.com")
        val pending = RegisteredUser(UserId(3L), Username("pending"), email)
        val approved = RegisteredUser(UserId(4L), Username("approved"), email, emailApproved = true)
        val noEmail = RegisteredUser(UserId(5L), Username("no-email"))
        val emails = FakeEmailsService()
        val service = createService(
            emailsService = emails,
            usersRepo = FakeUsersRepo(mapOf(pending.id to pending, approved.id to approved, noEmail.id to noEmail)),
        )

        assertEquals(
            EmailVerificationRequestResult.NoEmail,
            service.requestMyEmailVerification(UserId(999L), email),
        )
        assertEquals(EmailVerificationRequestResult.NoEmail, service.requestMyEmailVerification(noEmail.id, email))
        assertEquals(
            EmailVerificationRequestResult.EmailChanged,
            service.requestMyEmailVerification(pending.id, Email("other@example.com")),
        )
        assertEquals(
            EmailVerificationRequestResult.AlreadyApproved,
            service.requestMyEmailVerification(approved.id, email),
        )
        assertEquals(EmailVerificationRequestResult.Unavailable, service.requestMyEmailVerification(pending.id, email))
        assertEquals(emptyList(), emails.sendHtmlCalls)
    }

    /** The real invite sender distinguishes successful delivery from a failed or unavailable transport. */
    @Test
    fun requestVerificationReturnsSentOrDeliveryFailedThroughRealInviteSender() = runTest {
        val email = Email("delivery@example.com")
        val pending = RegisteredUser(UserId(6L), Username("delivery"), email)
        val successfulEmails = FakeEmailsService(result = true)
        val sentService = createService(
            emailsService = successfulEmails,
            usersRepo = FakeUsersRepo(mapOf(pending.id to pending)),
            inviteSender = createInviteSender(successfulEmails),
        )

        assertEquals(EmailVerificationRequestResult.Sent, sentService.requestMyEmailVerification(pending.id, email))
        assertEquals(listOf(email), successfulEmails.sendHtmlCalls.map { it.recipient })

        val failedEmails = FakeEmailsService(result = false)
        val failedService = createService(
            emailsService = failedEmails,
            usersRepo = FakeUsersRepo(mapOf(pending.id to pending)),
            inviteSender = createInviteSender(failedEmails),
        )
        assertEquals(
            EmailVerificationRequestResult.DeliveryFailed,
            failedService.requestMyEmailVerification(pending.id, email),
        )

        val unavailableService = createService(
            usersRepo = FakeUsersRepo(mapOf(pending.id to pending)),
            inviteSender = createInviteSender(null),
        )
        assertEquals(
            EmailVerificationRequestResult.DeliveryFailed,
            unavailableService.requestMyEmailVerification(pending.id, email),
        )
    }
}
