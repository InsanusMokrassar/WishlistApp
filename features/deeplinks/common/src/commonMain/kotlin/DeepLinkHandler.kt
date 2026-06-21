package dev.inmo.wishlist.features.deeplinks.common

import dev.inmo.wishlist.features.deeplinks.common.models.DeepLinkId

/**
 * Contract a feature implements to react when one of ITS deeplinks is opened.
 *
 * The deeplinks feature ships ZERO implementations; it only declares this interface and the
 * dispatch infrastructure. It lives in `common` so a handler-providing feature depends only on
 * `deeplinks/common`, not on the server module. A handler registers itself via
 * `singleWithRandomQualifier<DeepLinkHandler> { ... }` in its own server plugin; the dispatcher
 * collects every registered handler with `getAllDistinct<DeepLinkHandler>()`.
 */
interface DeepLinkHandler {
    /**
     * Attempt to process the opened deeplink.
     *
     * [handlerInfo] is the decoded
     * [dev.inmo.wishlist.features.deeplinks.common.models.DeepLinkHandlerInfo]; cast it with
     * `(handlerInfo as? DeepLinkHandlerInfo)`. A handler MUST: (1) return `false` immediately if
     * `info.type` is not its own key; (2) otherwise decode `info.payload` with its own serializer,
     * perform its side-effect, and return `true`.
     *
     * Returning `true` means "this handler owned and processed the deeplink"; the service then stops
     * and reports it handled. Returning `false` means "not mine / could not process", and the
     * service tries the next handler.
     *
     * The `Any` parameter type is the EXACT signature mandated by issue #45 and must not change.
     *
     * @param deeplinkId Identifier of the opened deeplink.
     * @param handlerInfo The decoded `DeepLinkHandlerInfo` for this deeplink, passed as `Any`.
     * @return `true` if this handler owned and processed the deeplink, `false` otherwise.
     */
    suspend fun tryHandle(deeplinkId: DeepLinkId, handlerInfo: Any): Boolean
}
