package dev.inmo.wishlist.features.deeplinks.server.models

import dev.inmo.wishlist.features.deeplinks.common.models.DeepLinkId
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Persisted record for a deep link. Stores handler info as an opaque [JsonElement] so the
 * server module has no compile-time dependency on concrete handler-info types from consuming features.
 *
 * The [handlerInfo] is produced by
 * `Json.encodeToJsonElement(PolymorphicSerializer(Any::class), handlerInfo)` and consumed by
 * `Json.decodeFromJsonElement(PolymorphicSerializer(Any::class), storedDeepLink.handlerInfo)`.
 *
 * @param id Unique identifier of this deep link.
 * @param handlerInfo Polymorphic JSON payload carrying type discriminator and data.
 */
@Serializable
data class StoredDeepLink(
    val id: DeepLinkId,
    val handlerInfo: JsonElement
)
