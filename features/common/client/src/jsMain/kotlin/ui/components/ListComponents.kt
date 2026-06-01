package dev.inmo.wishlist.features.common.client.ui.components

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.Li
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

/**
 * Screen heading rendered as a Bootstrap `h3` headline.
 *
 * @param text Already-translated heading text.
 * @param bottomMarginClass Bootstrap margin-bottom utility class controlling spacing under the title.
 * @param extraClasses Additional Bootstrap utility classes (e.g. `flex-grow-1`) applied to the headline.
 */
@Composable
fun ScreenTitle(text: String, bottomMarginClass: String = "mb-0", vararg extraClasses: String) {
    H1({ classes("h3", bottomMarginClass, *extraClasses) }) {
        Text(text)
    }
}

/**
 * Back-navigation button styled as a Bootstrap outline-secondary button.
 *
 * @param text Already-translated button label.
 * @param onClick Invoked when the user activates the button.
 */
@Composable
fun BackButton(text: String, onClick: () -> Unit) {
    Button({
        classes("btn", "btn-outline-secondary")
        onClick { onClick() }
    }) {
        Text(text)
    }
}

/**
 * Single Bootstrap `list-group-item` row carrying a primary label and optional leading/trailing content.
 *
 * Must be rendered inside a `Ul` with the `list-group` class.
 *
 * @param text Already-translated primary label of the row.
 * @param onSelect Invoked when the user clicks the row label; `null` makes the label non-interactive.
 * @param leading Optional leading content (e.g. an avatar/thumbnail) rendered at the row's start.
 * @param trailing Optional trailing content (e.g. action buttons) rendered at the row's end.
 */
@Composable
fun ListRow(
    text: String,
    onSelect: (() -> Unit)? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    ListRow(
        onSelect = onSelect,
        leading = leading,
        trailing = trailing,
        content = { Span { Text(text) } },
    )
}

/**
 * Single Bootstrap `list-group-item` row with a caller-provided primary content slot and optional leading/trailing content.
 *
 * Must be rendered inside a `Ul` with the `list-group` class. Use this overload when the primary cell needs more
 * than a single label (badges, secondary text, etc.).
 *
 * @param onSelect Invoked when the user clicks the primary content; `null` makes the content non-interactive.
 * @param leading Optional leading content (e.g. an avatar/thumbnail) rendered at the row's start.
 * @param trailing Optional trailing content (e.g. action buttons) rendered at the row's end.
 * @param content Primary content of the row.
 */
@Composable
fun ListRow(
    onSelect: (() -> Unit)? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Li({
        classes("list-group-item", "list-group-item-action", "d-flex", "justify-content-between", "align-items-center")
        if (leading == null && trailing == null && onSelect != null) {
            style { property("cursor", "pointer") }
            onClick { onSelect() }
        }
    }) {
        if (leading == null && trailing == null) {
            content()
        } else {
            Div({
                classes("d-flex", "align-items-center", "gap-3", "flex-grow-1")
                if (onSelect != null) {
                    style { property("cursor", "pointer") }
                    onClick { onSelect() }
                }
            }) {
                leading?.invoke()
                Div({ classes("flex-grow-1") }) { content() }
            }
            trailing?.invoke()
        }
    }
}
