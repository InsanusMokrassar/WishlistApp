package dev.inmo.wishlist.features.deeplinks.server.models

import dev.inmo.wishlist.features.deeplinks.common.models.DeepLinkId
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Persisted record of a declared deeplink.
 *
 * The attached handler-info is kept as an already-encoded polymorphic [JsonElement] rather than a
 * typed field, so the deeplinks feature never needs to know the concrete handler-info classes
 * declared by other features. The element carries its own type discriminator (array-polymorphism of
 * the shared [kotlinx.serialization.json.Json]) and is decoded back to its concrete type only at
 * resolution time.
 *
 * @property id Identifier of the deeplink (the UUID used in `links/<deeplink_uuid>`).
 * @property handlerInfo Polymorphic JSON encoding of the handler-info object attached at creation.
 */
@Serializable
data class StoredDeepLink(
    val id: DeepLinkId,
    val handlerInfo: JsonElement
)
