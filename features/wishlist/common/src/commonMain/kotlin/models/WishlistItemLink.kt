package dev.inmo.wishlist.features.wishlist.common.models

import kotlinx.serialization.Serializable

/**
 * A single external link attached to a [WishlistItem].
 *
 * Carries the target [url] and an optional human-readable [title]. When a [title] is present it is
 * shown as the clickable text of the link; otherwise the bare [url] is shown instead (see [displayText]).
 *
 * @property url Target URL of the link (e.g. a product page).
 * @property title Optional display text for the link; `null` (or blank) means the [url] itself is shown.
 */
@Serializable
data class WishlistItemLink(
    val url: String,
    val title: String? = null,
)

/**
 * Text that should be rendered as the clickable label of this link.
 *
 * Returns [WishlistItemLink.title] when it is non-blank, otherwise falls back to [WishlistItemLink.url].
 * Centralizes the "title-as-link or bare-link" display rule so every platform view stays consistent.
 */
val WishlistItemLink.displayText: String
    get() = title?.takeIf { it.isNotBlank() } ?: url
