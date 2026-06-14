package dev.inmo.wishlist.features.ui.wishlist.ui

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.dom.AttrBuilderContext
import org.jetbrains.compose.web.dom.Img
import org.w3c.dom.HTMLImageElement

/**
 * Inline SVG markup (URL-encoded for a `data:` URI) of the default wishlist look: three small item
 * cards stacked behind each other, each offset by a few units in x and y so the stack reads as
 * "several wishlist items, one behind another". Drawn on a 100x100 viewBox so it scales to any size.
 */
private val stackedItemsSvg: String =
    (
        "<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 100 100'>" +
            "<rect width='100' height='100' fill='%23eef1f4'/>" +
            "<rect x='34' y='22' width='44' height='44' rx='5' fill='%23c7cdd6'/>" +
            "<rect x='27' y='29' width='44' height='44' rx='5' fill='%23aeb6c1'/>" +
            "<rect x='20' y='36' width='44' height='44' rx='5' fill='%238b94a1'/>" +
            "</svg>"
        )

/** `data:` URI of [stackedItemsSvg] usable directly as an `<img src>`. */
private val stackedItemsDataUri: String = "data:image/svg+xml;charset=UTF-8,$stackedItemsSvg"

/**
 * Renders the default wishlist placeholder (stacked item cards) shown for a wishlist, which never
 * carries its own image.
 *
 * @param alt Already-translated accessibility text for the image.
 * @param attrs Extra attribute builder applied to the `<img>` (classes, sizing).
 */
@Composable
fun WishlistImagePlaceholder(alt: String, attrs: AttrBuilderContext<HTMLImageElement> = {}) {
    Img(src = stackedItemsDataUri, alt = alt, attrs = attrs)
}
