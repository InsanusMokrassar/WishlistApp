package dev.inmo.wishlist.features.deeplinks.common.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Serializable record stored as JSON for each deeplink.
 *
 * [type] is a caller-chosen discriminator naming the owning handler; [payload] is that handler's
 * own data class already encoded to a [JsonElement]. This is the exact object decoded at dispatch
 * time and passed as `handlerInfo: Any` to every
 * [dev.inmo.wishlist.features.deeplinks.common.DeepLinkHandler]. No `Any` value is ever serialized:
 * only this fully-serializable record is persisted.
 *
 * @property type Logical handler key; a handler claims a deeplink only when this equals its own
 *   constant. Keys must be globally unique per handler (dispatch is first-true-wins, unordered).
 * @property payload Opaque per-handler JSON body; the owning handler decodes it with its own
 *   serializer after matching [type].
 */
@Serializable
data class DeepLinkHandlerInfo(
    val type: String,
    val payload: JsonElement
)
