package dev.inmo.wishlist.features.ui.wishlist.ui

import dev.inmo.micro_utils.coroutines.compose.StyleSheetsAggregator
import org.jetbrains.compose.web.css.StyleSheet

/**
 * Per-view stylesheet for [WishlistItemEditView] — holds the raw layout rules specific to the item-edit
 * form (the links editor) that do not belong in the shared design system
 * [dev.inmo.wishlist.features.common.client.ui.CalmStudioStyleSheet].
 *
 * Self-registers into the [StyleSheetsAggregator] from [init], so the single
 * `StyleSheetsAggregator.draw()` in `ClientJSPlugin` renders it reactively the first time the view
 * reads one of its class names — no per-call `Style(...)` needed. `var(--cs-*)` tokens resolve against
 * the `:root` block emitted by `CalmStudioStyleSheet` (same document).
 */
object WishlistItemEditStyleSheet : StyleSheet() {
    /** One already-added link row: the link text on the left, the remove button on the right. */
    val linkRow by style {
        property("display", "flex")
        property("align-items", "center")
        property("gap", "8px")
        property("margin-bottom", "6px")
    }

    /** Link display text inside a [linkRow]; takes the remaining width and stays shrinkable. */
    val linkText by style {
        property("flex", "1")
        property("min-width", "0")
    }

    /** The add-link input row holding the title input, the url input and the add button. */
    val addLinkRow by style {
        property("display", "flex")
        property("gap", "8px")
    }

    /** Inline error shown under the links when two or more links share the same url. */
    val dupError by style {
        property("color", "var(--cs-danger)")
        property("font-size", "12px")
        property("margin-top", "6px")
    }

    init {
        StyleSheetsAggregator.addStyleSheet(this)
    }
}
