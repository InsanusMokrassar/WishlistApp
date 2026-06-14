package dev.inmo.wishlist.features.wishlist.common.models

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive

/**
 * A single external link attached to a [WishlistItem].
 *
 * Carries the target [url] and an optional human-readable [title]. When a [title] is present it is
 * shown as the clickable text of the link; otherwise the bare [url] is shown instead (see [displayText]).
 *
 * Serialized via [WishlistItemLinkSerializer]: a title-less link is written as a plain JSON string
 * (the [url]) so it stays wire-compatible with the legacy `List<String>` link format; a titled link is
 * written as a `{ "url": ..., "title": ... }` object.
 *
 * @property url Target URL of the link (e.g. a product page).
 * @property title Optional display text for the link; `null` (or blank) means the [url] itself is shown.
 */
@Serializable(with = WishlistItemLinkSerializer::class)
data class WishlistItemLink(
    val url: String,
    val title: String? = null,
)

/**
 * Object surrogate used to encode/decode the structured (titled) form of a [WishlistItemLink].
 *
 * @property url Target URL of the link.
 * @property title Optional display text for the link.
 */
@Serializable
private data class WishlistItemLinkSurrogate(
    val url: String,
    val title: String? = null,
)

/**
 * Custom serializer that keeps title-less links wire-compatible with the legacy bare-string link format.
 *
 * Encoding:
 * - [WishlistItemLink.title] `== null` → encoded as a plain string equal to [WishlistItemLink.url].
 * - otherwise → encoded as a [WishlistItemLinkSurrogate] object (`{ "url", "title" }`).
 *
 * Decoding (JSON):
 * - a JSON string → `WishlistItemLink(url = <string>, title = null)`.
 * - a JSON object → parsed through [WishlistItemLinkSurrogate].
 *
 * For non-JSON decoders the structured surrogate form is always used.
 */
object WishlistItemLinkSerializer : KSerializer<WishlistItemLink> {
    override val descriptor: SerialDescriptor = WishlistItemLinkSurrogate.serializer().descriptor

    override fun serialize(encoder: Encoder, value: WishlistItemLink) {
        when (value.title) {
            null -> encoder.encodeString(value.url)
            else -> encoder.encodeSerializableValue(
                WishlistItemLinkSurrogate.serializer(),
                WishlistItemLinkSurrogate(value.url, value.title),
            )
        }
    }

    override fun deserialize(decoder: Decoder): WishlistItemLink {
        val jsonDecoder = decoder as? JsonDecoder
            ?: return decoder.decodeSerializableValue(WishlistItemLinkSurrogate.serializer())
                .let { WishlistItemLink(it.url, it.title) }
        val element = jsonDecoder.decodeJsonElement()
        return when (element) {
            is JsonPrimitive -> WishlistItemLink(element.content, null)
            else -> jsonDecoder.json
                .decodeFromJsonElement(WishlistItemLinkSurrogate.serializer(), element)
                .let { WishlistItemLink(it.url, it.title) }
        }
    }
}

/**
 * Text that should be rendered as the clickable label of this link.
 *
 * Returns [WishlistItemLink.title] when it is non-blank, otherwise falls back to [WishlistItemLink.url].
 * Centralizes the "title-as-link or bare-link" display rule so every platform view stays consistent.
 */
val WishlistItemLink.displayText: String
    get() = title?.takeIf { it.isNotBlank() } ?: url
