package dev.inmo.wishlist.features.email.server.services

import dev.inmo.wishlist.features.deeplinks.common.DeepLinkHandler
import dev.inmo.wishlist.features.deeplinks.common.models.DeepLinkHandlerId
import dev.inmo.wishlist.features.deeplinks.common.models.DeepLinkId
import dev.inmo.wishlist.features.deeplinks.common.models.HandleResult
import dev.inmo.wishlist.features.email.common.EmailConstants
import dev.inmo.wishlist.features.email.server.EmailsService
import dev.inmo.wishlist.features.email.server.models.EmailVerification
import dev.inmo.wishlist.features.email.server.models.EmailVerificationPayload
import kotlinx.coroutines.CancellationException

/**
 * Deeplink boundary that delegates account-state validation and approval as one coordinated
 * operation.
 *
 * @param accountCoordinator Shared coordinator that checks the invited address against current
 *   user state and performs the pending-to-approved role transition atomically with self-service
 *   email mutation.
 * @param emailsService Optional SMTP transport used for the post-approval confirmation attempt.
 */
class EmailVerificationDeepLinkHandler(
    private val accountCoordinator: EmailVerificationAccountCoordinator,
    private val emailsService: EmailsService? = null,
) : DeepLinkHandler {
    /** Stable handler id persisted in email verification deeplinks. */
    override val id: DeepLinkHandlerId = EmailVerification.handlerId

    /** Subject used for the post-approval confirmation message. */
    private val approvalSubject = "Your WishlistApp account is approved"

    /** Fixed plain-text body for the post-approval confirmation message. */
    private val approvalText = "Your email has been approved. You can now sign in to WishlistApp."

    /**
     * Promotes the payload's account only while the stored address still matches the invited one.
     *
     * @param deeplinkId Opened deeplink identifier; retained for the handler contract.
     * @param value Decoded polymorphic payload.
     * @return A root-page redirect after successful approval, or `null` for invalid or stale links.
    */
    override suspend fun tryHandle(deeplinkId: DeepLinkId, value: Any): HandleResult.Handled? {
        val payload = value as? EmailVerificationPayload ?: return null
        val recipient = payload.email ?: return null
        if (!accountCoordinator.verifyInvitedEmailAndPromote(payload.userId, recipient)) return null
        try {
            emailsService?.sendText(recipient, approvalSubject, approvalText)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            // Approval has committed; an ordinary delivery failure must not change the HTTP outcome.
        }
        return HandleResult.Handled.Redirect(EmailConstants.approvalRedirectPath)
    }
}
