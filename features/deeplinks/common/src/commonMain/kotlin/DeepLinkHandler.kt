package dev.inmo.wishlist.features.deeplinks.common

import dev.inmo.wishlist.features.deeplinks.common.models.DeepLinkHandlerId
import dev.inmo.wishlist.features.deeplinks.common.models.DeepLinkId

/**
 * Contract a feature implements to react when one of ITS deeplinks is opened.
 *
 * The deeplinks feature ships ZERO implementations; it only declares this interface and the
 * dispatch infrastructure. It lives in `common` so a handler-providing feature depends only on
 * `deeplinks/common`, not on the server module. A handler registers itself via
 * `singleWithRandomQualifier<DeepLinkHandler> { ... }` in its own server plugin; the dispatcher
 * collects every registered handler with `getAllDistinct<DeepLinkHandler>()` and indexes them by
 * [id]. Selection is by id (map lookup) — NOT a first-true-wins list scan — so each [id] MUST be
 * globally unique.
 */
interface DeepLinkHandler {
    /**
     * Identity of this handler; the dispatcher maps every stored
     * [dev.inmo.wishlist.features.deeplinks.common.models.DeepLinkHandlerInfo.handlerId] to the
     * handler declaring the matching [id]. MUST be globally unique across all registered handlers.
     */
    val id: DeepLinkHandlerId

    /**
     * Process the opened deeplink that was already routed to THIS handler by its [id].
     *
     * [value] is this handler's own already-decoded polymorphic payload (the
     * [dev.inmo.wishlist.features.deeplinks.common.models.DeepLinkHandlerInfo.value], without the id).
     * The handler casts it to its concrete type (`value as? T`), performs its side-effect, and returns
     * `true` if processed; it returns `false` if it cannot process the value (e.g. the cast fails or
     * the current state is invalid), in which case the service reports the deeplink as unhandled.
     *
     * @param deeplinkId Identifier of the opened deeplink.
     * @param value This handler's own decoded payload, passed as `Any`.
     * @return `true` if this handler processed the deeplink, `false` otherwise.
     */
    suspend fun tryHandle(deeplinkId: DeepLinkId, value: Any): Boolean
}
