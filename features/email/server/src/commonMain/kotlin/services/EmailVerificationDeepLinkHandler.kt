package dev.inmo.wishlist.features.email.server.services

import dev.inmo.wishlist.features.deeplinks.common.DeepLinkHandler
import dev.inmo.wishlist.features.deeplinks.common.models.DeepLinkHandlerId
import dev.inmo.wishlist.features.deeplinks.common.models.DeepLinkId
import dev.inmo.wishlist.features.email.server.models.EmailVerification
import dev.inmo.wishlist.features.email.server.models.EmailVerificationPayload

/**
 * Deeplink boundary that delegates account-state validation and approval as one coordinated
 * operation.
 *
 * @param accountCoordinator Shared coordinator that checks the invited address against current
 *   user state and performs the pending-to-approved role transition atomically with self-service
 *   email mutation.
 */
class EmailVerificationDeepLinkHandler(
    private val accountCoordinator: EmailVerificationAccountCoordinator,
) : DeepLinkHandler {
    /** Stable handler id persisted in email verification deeplinks. */
    override val id: DeepLinkHandlerId = EmailVerification.handlerId

    /**
     * Promotes the payload's account only while the stored address still matches the invited one.
     *
     * @param deeplinkId Opened deeplink identifier; retained for the handler contract.
     * @param value Decoded polymorphic payload.
     * @return `true` when an existing account was approved; `false` for invalid or stale links.
     */
    override suspend fun tryHandle(deeplinkId: DeepLinkId, value: Any): Boolean {
        val payload = value as? EmailVerificationPayload ?: return false
        return accountCoordinator.verifyInvitedEmailAndPromote(payload.userId, payload.email)
    }
}
