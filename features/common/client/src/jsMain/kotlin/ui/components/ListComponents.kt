package dev.inmo.wishlist.features.common.client.ui.components

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

/**
 * Screen heading rendered as the Calm Studio page title (`<h1>`, styled by `.content-inner h1` /
 * `.pagehead h1`).
 *
 * @param text Already-translated heading text.
 * @param extraClasses Optional extra classes applied to the headline (e.g. layout helpers).
 */
@Composable
fun ScreenTitle(text: String, vararg extraClasses: String) {
    H1({ if (extraClasses.isNotEmpty()) classes(*extraClasses) }) {
        Text(text)
    }
}

/**
 * Back-navigation button styled as a Calm Studio ghost button (`.btn.ghost`).
 *
 * @param text Already-translated button label.
 * @param onClick Invoked when the user activates the button.
 */
@Composable
fun BackButton(text: String, onClick: () -> Unit) {
    Button({
        classes("btn", "ghost")
        onClick { onClick() }
    }) {
        Text(text)
    }
}

/**
 * Single Calm Studio list row (`.row`) carrying a primary label and optional leading/trailing content.
 *
 * Must be rendered inside a `Div` with the `rows` class.
 *
 * @param text Already-translated primary label of the row.
 * @param onSelect Invoked when the user clicks the row; `null` makes the row non-interactive.
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
 * Single Calm Studio list row (`.row`) with a caller-provided primary content slot and optional
 * leading/trailing content.
 *
 * Must be rendered inside a `Div` with the `rows` class. Use this overload when the primary cell needs
 * more than a single label (badges, secondary text, etc.).
 *
 * @param onSelect Invoked when the user clicks the primary content; `null` makes the row non-interactive.
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
    Div({
        classes("row")
        if (onSelect != null) {
            onClick { onSelect() }
        } else {
            style { property("cursor", "default") }
        }
    }) {
        leading?.invoke()
        Div({ classes("rmain") }) { content() }
        trailing?.invoke()
    }
}
