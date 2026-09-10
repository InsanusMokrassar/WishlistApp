package dev.inmo.wishlist.features.email.server.services

import dev.inmo.wishlist.features.deeplinks.common.DeepLinkHandler
import dev.inmo.wishlist.features.deeplinks.common.models.DeepLinkHandlerId
import dev.inmo.wishlist.features.deeplinks.common.models.DeepLinkId
import dev.inmo.wishlist.features.deeplinks.common.models.HandleResult
import dev.inmo.wishlist.features.email.server.models.EmailPasswordChange
import dev.inmo.wishlist.features.email.server.models.EmailPasswordChangePayload

/**
 * Read-only deeplink handler for email-authorized password changes.
 *
 * The service provider is deferred to prevent DeepLinksService construction from recursively
 * resolving the service that depends on it.
 *
 * @param serviceProvider Deferred Email password-change service resolver.
 */
class EmailPasswordChangeDeepLinkHandler(
    private val serviceProvider: () -> EmailPasswordChangeService,
) : DeepLinkHandler {
    /** Stable handler identifier persisted with this approval purpose. */
    override val id: DeepLinkHandlerId = EmailPasswordChange.handlerId

    /**
     * Rereads and validates the stored approval before emitting a fixed redirect.
     *
     * @param deeplinkId Opened approval identifier.
     * @param value Initially dispatched value; used only for purpose-type isolation.
     * @return A fixed pending-page redirect, or `null` without mutation for invalid approvals.
     */
    override suspend fun tryHandle(deeplinkId: DeepLinkId, value: Any): HandleResult.Handled? {
        if (value !is EmailPasswordChangePayload) return null
        return serviceProvider().pendingPasswordChangePath(deeplinkId)
            ?.let(HandleResult.Handled::Redirect)
    }
}
