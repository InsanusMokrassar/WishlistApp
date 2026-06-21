package dev.inmo.wishlist.features.deeplinks.server.services

import com.benasher44.uuid.uuid4
import dev.inmo.micro_utils.repos.set
import dev.inmo.wishlist.features.deeplinks.common.models.DeepLinkId
import dev.inmo.wishlist.features.deeplinks.server.DeepLinkHandler
import dev.inmo.wishlist.features.deeplinks.server.DeepLinksFeature
import dev.inmo.wishlist.features.deeplinks.server.models.StoredDeepLink
import dev.inmo.wishlist.features.deeplinks.server.repo.DeepLinksRepo
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.json.Json

/**
 * Default implementation of [DeepLinksFeature].
 *
 * Encodes handler info using [PolymorphicSerializer] with `Any::class`, delegating type
 * discrimination to the consuming feature's `SerializersModule` registered in the shared [Json].
 * Uses `useArrayPolymorphism=true` format: `["dev.foo.MyHandlerInfo", {...}]`.
 *
 * @param json Shared [Json] instance from `features/common/common`; must include serializers
 *   for all handler-info types registered by consuming features.
 * @param repo Persistence layer for stored deep links.
 * @param handlers All registered [DeepLinkHandler] instances collected via `getAllDistinct()`.
 */
class DeepLinksService(
    private val json: Json,
    private val repo: DeepLinksRepo,
    private val handlers: List<DeepLinkHandler>
) : DeepLinksFeature {

    /**
     * Creates a new deep link: generates a random [DeepLinkId], polymorphically encodes [handlerInfo]
     * to JSON, persists the pair, and returns the id.
     *
     * @param handlerInfo Serializable handler payload; its type must be registered in the shared [json].
     * @return The freshly generated [DeepLinkId] addressing the stored handler info.
     */
    override suspend fun createDeepLink(handlerInfo: Any): DeepLinkId {
        val id = DeepLinkId(uuid4().toString())
        val encoded = json.encodeToJsonElement(PolymorphicSerializer(Any::class), handlerInfo)
        repo.set(id, StoredDeepLink(id, encoded))
        return id
    }

    /**
     * Resolves a deep link: loads the stored handler info, decodes it polymorphically, and offers it
     * to each registered [DeepLinkHandler] until one reports it handled the link.
     *
     * @param deeplinkId Identifier to resolve.
     * @return `true` if the link exists and a handler processed it; `false` if unknown or unhandled.
     */
    override suspend fun resolveDeepLink(deeplinkId: DeepLinkId): Boolean {
        val stored = repo.get(deeplinkId) ?: return false
        val handlerInfo = json.decodeFromJsonElement(PolymorphicSerializer(Any::class), stored.handlerInfo)
        return handlers.any { it.tryHandle(deeplinkId, handlerInfo) }
    }
}
