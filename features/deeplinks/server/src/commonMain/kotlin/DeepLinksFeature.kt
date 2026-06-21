package dev.inmo.wishlist.features.deeplinks.server

import dev.inmo.wishlist.features.deeplinks.common.models.DeepLinkId

/**
 * Server-only capability contract for the deeplinks feature.
 * Not exposed to client modules — consumed only within the server-side DI graph.
 */
interface DeepLinksFeature {
    /**
     * Persists a new deep link with the given handler info and returns its identifier.
     *
     * The [handlerInfo] object must have its type registered in the shared
     * [kotlinx.serialization.json.Json]'s `SerializersModule` via polymorphic registration
     * before calling this method; otherwise a `SerializationException` is thrown at runtime.
     *
     * @param handlerInfo Serializable handler-info object identifying the handler type and data.
     * @return Newly created [DeepLinkId] wrapping a UUID string.
     */
    suspend fun createDeepLink(handlerInfo: Any): DeepLinkId

    /**
     * Resolves a deep link by invoking registered [DeepLinkHandler]s in order until one returns `true`.
     *
     * @param deeplinkId Identifier of the deep link to resolve.
     * @return `true` if any handler reported success; `false` if not found or no handler matched.
     */
    suspend fun resolveDeepLink(deeplinkId: DeepLinkId): Boolean
}
