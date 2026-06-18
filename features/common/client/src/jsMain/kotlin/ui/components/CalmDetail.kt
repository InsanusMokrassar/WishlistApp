package dev.inmo.wishlist.features.common.client.ui.components

import dev.inmo.wishlist.features.common.client.ui.CalmStudioStyleSheet
import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.dom.A
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text

/**
 * Calm Studio item-detail layout (`.detail`) — a two-column grid pairing a [gallery] (collapses below
 * the body on narrow viewports) with the item [body].
 *
 * @param gallery Left column content; typically a [DetailMedia].
 * @param body Right column content: title, pills, [ActionBar], and [DetailField]s.
 */
@Composable
fun DetailLayout(
    gallery: @Composable () -> Unit,
    body: @Composable () -> Unit,
) {
    Div({ classes(CalmStudioStyleSheet.detail) }) {
        Div({ classes(CalmStudioStyleSheet.gallery) }) { gallery() }
        Div { body() }
    }
}

/**
 * Calm Studio detail media (`.gallery .main-img`) — the square, tinted placeholder for an item's main
 * image. Render inside [DetailLayout]'s gallery slot.
 *
 * @param tintClass Deterministic media tint class (see [tintClass]).
 */
@Composable
fun DetailMedia(tintClass: String) {
    Div({ classes(CalmStudioStyleSheet.`main-img`, tintClass) })
}

/**
 * Calm Studio detail field (`.field`) — an uppercase [label] (`.lbl`) above caller-provided content.
 *
 * Use this overload when the value is richer than a single line (a [PriceTag], a list of [LinkRow]s,
 * etc.); for a plain value prefer the `DetailField(label, value)` overload.
 *
 * @param label Already-translated field label.
 * @param content The field value content.
 */
@Composable
fun DetailField(label: String, content: @Composable () -> Unit) {
    Div({ classes(CalmStudioStyleSheet.field) }) {
        Div({ classes(CalmStudioStyleSheet.lbl) }) { Text(label) }
        content()
    }
}

/**
 * Calm Studio detail field (`.field`) with a plain text value rendered as `.val`.
 *
 * @param label Already-translated field label.
 * @param value Already-translated field value.
 */
@Composable
fun DetailField(label: String, value: String) {
    DetailField(label) {
        Div({ classes(CalmStudioStyleSheet.`val`) }) { Text(value) }
    }
}

/**
 * Calm Studio price tag (`.pricetag`) — the large emphasized price on the item detail screen.
 *
 * @param text Pre-formatted price string.
 */
@Composable
fun PriceTag(text: String) {
    Div({ classes(CalmStudioStyleSheet.pricetag) }) { Text(text) }
}

/**
 * Calm Studio link row (`.linkrow`) — an accented external link with a trailing "open" glyph.
 *
 * @param text Already-translated link label.
 * @param href Optional URL; when set the link opens in a new tab (`target=_blank`, `rel=noreferrer`).
 * @param onSelect Optional click handler (use when navigation is handled in code rather than via [href]).
 */
@Composable
fun LinkRow(
    text: String,
    href: String? = null,
    onSelect: (() -> Unit)? = null,
) {
    A(href = href, attrs = {
        classes(CalmStudioStyleSheet.linkrow)
        if (href != null) {
            attr("target", "_blank")
            attr("rel", "noreferrer")
        }
        onSelect?.let { handler -> onClick { handler() } }
    }) {
        Text(text)
        CalmIcon(CalmIcons.ext)
    }
}

/**
 * Calm Studio action bar (`.actbar`) — the horizontal cluster of primary item actions (Reserve, Copy,
 * Edit) under the item title on the detail screen.
 *
 * @param content The action buttons.
 */
@Composable
fun ActionBar(content: @Composable () -> Unit) {
    Div({ classes(CalmStudioStyleSheet.actbar) }) { content() }
}
