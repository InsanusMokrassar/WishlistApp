package dev.inmo.wishlist.features.ui.wishlist.ui

import dev.inmo.micro_utils.coroutines.compose.StyleSheetsAggregator
import org.jetbrains.compose.web.css.StyleSheet

/**
 * Per-view stylesheet for [WishlistItemView] — the few layout rules specific to the item-detail screen
 * that the shared design system
 * [dev.inmo.wishlist.features.common.client.ui.CalmStudioStyleSheet] does not provide: the title's
 * priority-pill row and the extra-images thumbnail strip under the main image.
 *
 * Self-registers into the [StyleSheetsAggregator] from [init], so the single
 * `StyleSheetsAggregator.draw()` in `ClientJSPlugin` renders it reactively the first time the view reads
 * one of its class names — no per-call `Style(...)` needed. `var(--cs-*)` tokens resolve against the
 * `:root` block emitted by `CalmStudioStyleSheet` (same document).
 */
object WishlistItemViewStylesheet : StyleSheet() {
    /** Inline row pairing the item title's priority pill with its surrounding spacing. */
    val priorityRow by style {
        property("display", "flex")
        property("align-items", "center")
        property("gap", "10px")
        property("margin-bottom", "18px")
    }

    /** Wrapping flex strip of the item's additional image thumbnails under the main image. */
    val thumbStrip by style {
        property("display", "flex")
        property("flex-wrap", "wrap")
        property("gap", "8px")
        property("margin-top", "12px")
    }

    /** One additional-image thumbnail in the [thumbStrip] (fixed square, cropped to cover). */
    val thumbImg by style {
        property("width", "72px")
        property("height", "72px")
        property("object-fit", "cover")
        property("border-radius", "10px")
    }

    init {
        StyleSheetsAggregator.addStyleSheet(this)
    }
}
