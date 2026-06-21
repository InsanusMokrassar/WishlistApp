package dev.inmo.wishlist.features.deeplinks.server

import dev.inmo.wishlist.features.deeplinks.common.models.DeepLinkId

/**
 * Handler for a specific deep link type. Consuming features register implementations via
 * `singleWithRandomQualifier<DeepLinkHandler>` in their Koin plugin.
 *
 * Each handler is called in sequence when a deep link is resolved. Return `true` to signal
 * the link was handled (stops iteration); return `false` to defer to the next handler.
 */
fun interface DeepLinkHandler {
    /**
     * Attempts to handle the deep link identified by [deeplinkId].
     *
     * @param deeplinkId Identifier of the resolved deep link.
     * @param handlerInfo Deserialized handler-info object; the handler should check the type
     *   before processing and return `false` immediately for unknown types.
     * @return `true` if handled and processing should stop; `false` to pass to the next handler.
     */
    suspend fun tryHandle(deeplinkId: DeepLinkId, handlerInfo: Any): Boolean
}
