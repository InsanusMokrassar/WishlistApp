package dev.inmo.wishlist.features.email.server.services

import dev.inmo.wishlist.features.email.common.EmailConstants
import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.email.common.models.EmailVerificationRequestResult
import dev.inmo.wishlist.features.email.server.EmailFeature
import dev.inmo.wishlist.features.email.server.EmailsService
import dev.inmo.wishlist.features.auth.server.RegistrationEmailDeliveryHandle
import dev.inmo.wishlist.features.roles.server.RolesFeature
import dev.inmo.wishlist.features.users.common.models.UserId
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

/**
 * Server-side [EmailFeature] implementation that unifies SMTP delivery and user-email persistence.
 *
 * [emailsService] is always a real, non-null [EmailsService] — [dev.inmo.wishlist.features.email.server.Plugin]
 * only ever constructs this class when one is registered (SMTP configured). When no [EmailsService] is
 * registered (SMTP not configured), [DisabledEmailFeature] is substituted for the whole [EmailFeature]
 * binding instead. Because of that, [isFeatureEnabled] always returns `true`.
 *
 * - [sendTestEmail] verifies [callerId] may access the `email.sendTest` functionality (via
 *   [rolesFeature]) before delegating SMTP delivery via [emailsService].
 * - [setMyEmail] updates the caller's stored email address through [accountCoordinator], sharing one
 *   atomicity boundary with verification approval regardless of SMTP configuration.
 *
 * @param emailsService SMTP delivery service used for sends. Always non-null — see class doc.
 * @param accountCoordinator Shared coordinator used for email-address persistence and verification
 *   atomicity.
 * @param rolesFeature Functionality-availability check used to gate [sendTestEmail]; see
 *   `features/roles` (issue #68).
 * @param inviteSender Existing exact-recipient deeplink sender reused for owner verification; `null`
 *   only in narrowly constructed legacy/test graphs where verification delivery is unavailable.
 */
class EmailFeatureService(
    private val emailsService: EmailsService,
    private val accountCoordinator: EmailVerificationAccountCoordinator,
    private val rolesFeature: RolesFeature,
    private val inviteSender: EmailRegistrationInviteSender? = null,
) : EmailFeature {

    /**
     * Returns whether an SMTP delivery service is available.
     *
     * @return Always `true` — this class is only ever constructed with a real [emailsService]; see
     *   [DisabledEmailFeature] for the SMTP-disabled no-op path.
     */
    override suspend fun isFeatureEnabled(): Boolean = true

    /**
     * Sends a test email to [recipient] if [callerId] may access the `email.sendTest` functionality.
     *
     * @param callerId Caller checked against [rolesFeature].
     * @param recipient Target address for the test message.
     * @return `true` when delivery succeeded; `false` when the caller lacks privilege or SMTP
     *   delivery fails.
     */
    override suspend fun sendTestEmail(callerId: UserId, recipient: Email): Boolean {
        if (!rolesFeature.isFunctionalityAvailable(callerId, EmailConstants.sendTestFunctionalityId)) return false
        return emailsService.sendText(
            recipient = recipient,
            subject = "Test email from WishlistApp",
            text = "This is a test email sent from WishlistApp to verify SMTP configuration."
        )
    }

    /**
     * Updates or clears the stored email address for [callerId].
     *
     * Delegates to [EmailVerificationAccountCoordinator.updateStoredEmail] — identical to
     * [DisabledEmailFeature.setMyEmail].
     *
     * @param callerId User whose record is updated.
     * @param email New address to store, or `null` to clear the current address.
     * @return `true` when the update was persisted; `false` when the user was not found.
     * @throws dev.inmo.wishlist.features.users.common.repo.exceptions.DuplicateUserFieldException
     *   when [email] is already stored for a different user; propagates unchanged from
     *   [EmailVerificationAccountCoordinator.updateStoredEmail] — this method does not catch it.
     */
    override suspend fun setMyEmail(callerId: UserId, email: Email?): Boolean =
        accountCoordinator.updateStoredEmail(callerId, email)

    /**
     * Delivers a verification link only for the caller's exact, pending current address.
     *
     * SMTP and deeplink work intentionally occur outside the coordinator mutex. The record is checked
     * again afterward; if a concurrent write made the delivered link stale, only the request-local
     * link is removed and the newer address is left intact.
     *
     * @param callerId Authenticated owner whose address is considered.
     * @param expectedEmail Address the owner saw before submitting.
     * @return Delivery or current-address result.
     */
    override suspend fun requestMyEmailVerification(
        callerId: UserId,
        expectedEmail: Email,
    ): EmailVerificationRequestResult {
        val initial = accountCoordinator.getCurrentUser(callerId)
            ?: return EmailVerificationRequestResult.NoEmail
        val currentEmail = initial.email ?: return EmailVerificationRequestResult.NoEmail
        if (currentEmail != expectedEmail) return EmailVerificationRequestResult.EmailChanged
        if (initial.emailApproved) return EmailVerificationRequestResult.AlreadyApproved

        val sender = inviteSender ?: return EmailVerificationRequestResult.Unavailable
        val delivery = sender.sendRegistrationEmailWithCompensation(initial)
            ?: return EmailVerificationRequestResult.DeliveryFailed
        val current = accountCoordinator.getCurrentUser(callerId)
        return when {
            current == null || current.email == null -> {
                rollbackDelivery(delivery)
                EmailVerificationRequestResult.NoEmail
            }
            current.email != expectedEmail -> {
                rollbackDelivery(delivery)
                EmailVerificationRequestResult.EmailChanged
            }
            current.emailApproved -> {
                rollbackDelivery(delivery)
                EmailVerificationRequestResult.AlreadyApproved
            }
            else -> EmailVerificationRequestResult.Sent
        }
    }

    /** Removes the one link owned by a stale request even if parent work was cancelled. */
    private suspend fun rollbackDelivery(delivery: RegistrationEmailDeliveryHandle) {
        withContext(NonCancellable) {
            delivery.rollback()
        }
    }
}
