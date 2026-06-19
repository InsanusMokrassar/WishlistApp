package dev.inmo.wishlist.features.ui.wishlist.ui

import androidx.compose.runtime.Composable
import dev.inmo.micro_utils.coroutines.compose.StyleSheetsAggregator
import org.jetbrains.compose.web.css.StyleSheet
import org.jetbrains.compose.web.dom.AttrBuilderContext
import org.jetbrains.compose.web.dom.Img
import org.w3c.dom.HTMLImageElement

/**
 * Inline SVG markup (URL-encoded for a `data:` URI) of the default wishlist-item gift box: a box body,
 * a lid, a vertical ribbon and a bow, on a neutral background. Drawn on a 100x100 viewBox so it scales
 * to any requested size.
 */
private val giftBoxSvg: String =
    (
        "<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 100 100'>" +
            "<rect width='100' height='100' fill='%23eef1f4'/>" +
            "<rect x='24' y='44' width='52' height='38' rx='3' fill='%23c0c6cf'/>" +
            "<rect x='20' y='36' width='60' height='14' rx='3' fill='%23a8b0bb'/>" +
            "<rect x='46' y='36' width='8' height='46' fill='%238b94a1'/>" +
            "<path d='M50 36 C40 24 26 30 50 36 C74 30 60 24 50 36 Z' fill='%238b94a1'/>" +
            "</svg>"
        )

/** `data:` URI of [giftBoxSvg] usable directly as an `<img src>`. */
private val giftBoxDataUri: String = "data:image/svg+xml;charset=UTF-8,$giftBoxSvg"

/**
 * Per-view stylesheet for [WishlistItemImagePlaceholder]: the gift glyph stays fully visible
 * (`object-fit: contain`) on its neutral fill instead of being cropped. Self-registers into the
 * [StyleSheetsAggregator] so referencing [placeholder] renders it without a per-call `Style(...)`.
 */
object WishlistItemImagePlaceholderStylesheet : StyleSheet() {
    /** Default placeholder image styling: contain the glyph over the gift's neutral background. */
    val placeholder by style {
        property("object-fit", "contain")
        property("background-color", "#eef1f4")
    }

    init { StyleSheetsAggregator.addStyleSheet(this) }
}

/**
 * Renders the default gift-box placeholder shown whenever a wishlist item has no attached image.
 *
 * @param alt Already-translated accessibility text for the image.
 * @param attrs Extra attribute builder applied to the `<img>` (classes, sizing). Lets callers reuse
 * the same placeholder both as fixed-size leading thumbnails and as full-width card media.
 *
 * Defaults to `object-fit: contain` plus the gift's neutral background so the whole gift icon stays
 * visible (never cropped or stretched) whatever the container's aspect ratio is; the background fills
 * any letterbox area. Callers may override via [attrs] (applied last).
 */
@Composable
fun WishlistItemImagePlaceholder(alt: String, attrs: AttrBuilderContext<HTMLImageElement> = {}) {
    Img(src = giftBoxDataUri, alt = alt) {
        classes(WishlistItemImagePlaceholderStylesheet.placeholder)
        attrs()
    }
}
