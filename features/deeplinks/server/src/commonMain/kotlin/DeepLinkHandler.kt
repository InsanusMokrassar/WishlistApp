package dev.inmo.wishlist.features.deeplinks.server

import dev.inmo.wishlist.features.deeplinks.common.models.DeepLinkId

/**
 * Handler that may process a declared deeplink when it is invoked.
 *
 * On resolution of `links/<deeplink_uuid>` the deeplinks feature decodes the stored handler-info
 * back to its concrete type and offers it to every registered [DeepLinkHandler] in turn. A handler
 * inspects the runtime type of [handlerInfo]; if it recognizes the type it performs its action and
 * returns `true`, stopping the iteration. A handler that does not recognize the type returns `false`
 * so the next handler gets a chance.
 *
 * Implementations are contributed by other server features via
 * `singleWithRandomQualifier<DeepLinkHandler> { ... }`; the deeplinks feature itself ships none.
 */
fun interface DeepLinkHandler {
    /**
     * Attempts to process the deeplink identified by [deeplinkId] using the supplied [handlerInfo].
     *
     * @param deeplinkId Identifier of the invoked deeplink.
     * @param handlerInfo Handler-info object originally attached at creation time, decoded back to
     * its concrete serializable type via the shared polymorphic [kotlinx.serialization.json.Json].
     * @return `true` when this handler recognized the [handlerInfo] type and tried to process it
     * (resolution stops); `false` to defer to the next handler.
     */
    suspend fun tryHandle(deeplinkId: DeepLinkId, handlerInfo: Any): Boolean
}
