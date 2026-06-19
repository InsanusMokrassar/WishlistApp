package dev.inmo.wishlist.features.common.client.ui.components

import dev.inmo.wishlist.features.common.client.ui.CalmStudioStyleSheet
import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.attributes.ButtonType
import org.jetbrains.compose.web.attributes.type
import org.jetbrains.compose.web.dom.A
import org.jetbrains.compose.web.dom.B
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

/**
 * Calm Studio centered content column (`.content-inner`) — the max-width wrapper every content screen
 * places its body inside.
 *
 * @param content The screen body.
 */
@Composable
fun ContentColumn(content: @Composable () -> Unit) {
    Div({ classes(CalmStudioStyleSheet.`content-inner`) }) { content() }
}

/**
 * Calm Studio page header (`.pagehead`) — the screen title (`<h1>`) with an optional [subline] on the
 * left and an optional [actions] cluster (`.acts`) on the right.
 *
 * @param title Already-translated page title.
 * @param subline Optional already-translated secondary line under the title.
 * @param actions Optional right-aligned actions (typically [CalmButton]s).
 */
@Composable
fun PageHead(
    title: String,
    subline: String? = null,
    actions: (@Composable () -> Unit)? = null,
) {
    Div({ classes(CalmStudioStyleSheet.pagehead) }) {
        Div {
            H1 { Text(title) }
            subline?.let { Subline(it) }
        }
        actions?.let { Div({ classes(CalmStudioStyleSheet.acts) }) { it() } }
    }
}

/**
 * Calm Studio subline (`.subline`) — the muted secondary line used under page titles.
 *
 * @param text Already-translated subline text.
 */
@Composable
fun Subline(text: String) {
    P({ classes(CalmStudioStyleSheet.subline) }) { Text(text) }
}

/**
 * One [Breadcrumb] segment.
 *
 * @property label Already-translated segment label.
 * @property onClick Navigation handler; `null` renders the segment as a static (non-clickable) label.
 */
data class CrumbItem(val label: String, val onClick: (() -> Unit)? = null)

/**
 * Calm Studio breadcrumb (`.crumb`) — the ancestor [items] (each rendered as a link, joined by `/`
 * separators) followed by the bold [current] page.
 *
 * @param items Ancestor segments, outermost first; may be empty.
 * @param current Already-translated current-page label (rendered bold, non-clickable).
 */
@Composable
fun Breadcrumb(items: List<CrumbItem>, current: String) {
    Div({ classes(CalmStudioStyleSheet.crumb) }) {
        items.forEach { item ->
            A(attrs = {
                item.onClick?.let { handler -> onClick { handler() } }
            }) { Text(item.label) }
            Span(attrs = { classes(CalmStudioStyleSheet.sep) }) { Text("/") }
        }
        B { Text(current) }
    }
}

/**
 * Calm Studio toolbar (`.toolbar`) — a space-between row that pairs a left cluster (filters / segmented
 * controls) with an optional [right] cluster (`.toolbar .right`: sort select, view-mode switch).
 *
 * Both slots are always rendered (empty when omitted) so the space-between layout stays stable.
 *
 * @param left Optional left cluster content.
 * @param right Optional right cluster content.
 */
@Composable
fun Toolbar(
    left: (@Composable () -> Unit)? = null,
    right: (@Composable () -> Unit)? = null,
) {
    Div({ classes(CalmStudioStyleSheet.toolbar) }) {
        Div { left?.invoke() }
        Div({ classes("right") }) { right?.invoke() }
    }
}

/**
 * Calm Studio segmented control (`.seg`) — a row of mutually exclusive options where the active one
 * carries the `on` highlight. Generic over the option type [T].
 *
 * @param options All selectable options, in display order.
 * @param selected The currently active option.
 * @param label Maps an option to its already-translated label.
 * @param onSelect Invoked with the option the user picked.
 */
@Composable
fun <T> SegmentedControl(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
) {
    Div({ classes(CalmStudioStyleSheet.seg) }) {
        options.forEach { option ->
            Button(attrs = {
                type(ButtonType.Button)
                if (option == selected) classes(CalmStudioStyleSheet.on)
                onClick { onSelect(option) }
            }) { Text(label(option)) }
        }
    }
}
