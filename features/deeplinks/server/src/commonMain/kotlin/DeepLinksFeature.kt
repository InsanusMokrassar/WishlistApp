package dev.inmo.wishlist.features.deeplinks.server

import dev.inmo.wishlist.features.deeplinks.common.models.DeepLinkId

/**
 * Server-only capability for declaring and resolving deeplinks.
 *
 * This interface is intentionally not mirrored on the client: deeplink creation is a server-side
 * operation performed by other server features that also register a [DeepLinkHandler] to react when
 * the deeplink is invoked. Resolution is reached through the public `links/<deeplink_uuid>` route.
 */
interface DeepLinksFeature {
    /**
     * Declares a new deeplink with [handlerInfo] attached.
     *
     * The handler-info object is encoded with the shared polymorphic serializer and persisted; its
     * concrete type must be registered in the aggregated [kotlinx.serialization.modules.SerializersModule]
     * by the owning feature (the same mechanism as `ViewConfig` registration).
     *
     * @param handlerInfo Serializable object describing how the deeplink should be handled.
     * @return The generated [DeepLinkId], usable to build the `links/<deeplink_uuid>` URL.
     */
    suspend fun createDeepLink(handlerInfo: Any): DeepLinkId

    /**
     * Resolves the deeplink identified by [deeplinkId] by offering its handler-info to every
     * registered [DeepLinkHandler] until one reports it processed the deeplink.
     *
     * @param deeplinkId Identifier extracted from the invoked `links/<deeplink_uuid>` path.
     * @return `true` when the deeplink exists and a handler processed it; `false` when the deeplink
     * is unknown or no handler recognized its handler-info type.
     */
    suspend fun resolveDeepLink(deeplinkId: DeepLinkId): Boolean
}
