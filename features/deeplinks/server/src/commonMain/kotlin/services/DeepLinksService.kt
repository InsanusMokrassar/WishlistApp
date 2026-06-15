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
 * Server-side implementation of [DeepLinksFeature].
 *
 * Creation encodes the handler-info object with the shared polymorphic serializer and persists it
 * under a freshly generated UUID. Resolution decodes the stored handler-info back to its concrete
 * type and offers it to each registered [DeepLinkHandler] until one reports it handled the deeplink.
 *
 * This is a server-only service (not bound to any client-facing interface), matching the
 * convention used by `FilesService`.
 *
 * @param json Shared serializer whose aggregated [kotlinx.serialization.modules.SerializersModule]
 * knows the concrete handler-info subclasses (registered by their owning features).
 * @param repo Persistent store of declared deeplinks.
 * @param handlers All registered handlers, collected from Koin via `getAllDistinct()`.
 */
class DeepLinksService(
    private val json: Json,
    private val repo: DeepLinksRepo,
    private val handlers: List<DeepLinkHandler>
) : DeepLinksFeature {
    /** Polymorphic serializer used to encode/decode arbitrary handler-info as `Any`. */
    private val anySerializer = PolymorphicSerializer(Any::class)

    override suspend fun createDeepLink(handlerInfo: Any): DeepLinkId {
        val id = DeepLinkId(uuid4().toString())
        val encoded = json.encodeToJsonElement(anySerializer, handlerInfo)
        repo.set(id, StoredDeepLink(id = id, handlerInfo = encoded))
        return id
    }

    override suspend fun resolveDeepLink(deeplinkId: DeepLinkId): Boolean {
        val stored = repo.get(deeplinkId) ?: return false
        val handlerInfo = json.decodeFromJsonElement(anySerializer, stored.handlerInfo)
        return handlers.any { it.tryHandle(deeplinkId, handlerInfo) }
    }
}
