package dev.inmo.wishlist.features.common.client.ui.components

import dev.inmo.wishlist.features.common.client.ui.CalmStudioStyleSheet
import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.attributes.ButtonType
import org.jetbrains.compose.web.attributes.type
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H2
import org.jetbrains.compose.web.dom.H3
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Text

/**
 * Calm Studio empty state (`.empty`) — a centered glyph, heading, optional body line, and optional
 * call-to-action used when a screen has nothing to show.
 *
 * @param icon Inner SVG markup (one of [CalmIcons]) for the glyph.
 * @param title Already-translated heading.
 * @param text Optional already-translated explanatory line.
 * @param action Optional call-to-action (typically a primary [CalmButton]).
 */
@Composable
fun EmptyState(
    icon: String,
    title: String,
    text: String? = null,
    action: (@Composable () -> Unit)? = null,
) {
    Div({ classes("empty") }) {
        Div({ classes(CalmStudioStyleSheet.ic) }) { CalmIcon(icon) }
        H3 { Text(title) }
        text?.let { P { Text(it) } }
        action?.invoke()
    }
}

/**
 * Calm Studio modal (`.scrim` + `.modal`) — a centered dialog over a blurred backdrop. Clicking the
 * scrim invokes [onDismiss]; clicks inside the dialog are stopped from dismissing it.
 *
 * Compose the body from [ModalHeader], [ModalBody], [ModalFooter] (and [ModalTabs] inside the body).
 *
 * @param onDismiss Invoked when the backdrop is clicked; `null` makes the modal non-dismissible by click.
 * @param content The dialog content.
 */
@Composable
fun CalmModal(
    onDismiss: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Div({
        classes(CalmStudioStyleSheet.scrim)
        onDismiss?.let { handler -> onClick { handler() } }
    }) {
        Div({
            classes(CalmStudioStyleSheet.modal)
            onClick { it.stopPropagation() }
        }) {
            content()
        }
    }
}

/**
 * Calm Studio modal header (`.mhead`) — a bold [title] with an optional [subtitle] line.
 *
 * @param title Already-translated modal title.
 * @param subtitle Optional already-translated supporting line.
 */
@Composable
fun ModalHeader(title: String, subtitle: String? = null) {
    Div({ classes(CalmStudioStyleSheet.mhead) }) {
        H2 { Text(title) }
        subtitle?.let { P { Text(it) } }
    }
}

/**
 * Calm Studio modal body (`.mbody`) — the padded content area of a [CalmModal].
 *
 * @param content The body content.
 */
@Composable
fun ModalBody(content: @Composable () -> Unit) {
    Div({ classes(CalmStudioStyleSheet.mbody) }) { content() }
}

/**
 * Calm Studio modal footer (`.mfoot`) — the right-aligned action row of a [CalmModal].
 *
 * @param content The footer buttons.
 */
@Composable
fun ModalFooter(content: @Composable () -> Unit) {
    Div({ classes(CalmStudioStyleSheet.mfoot) }) { content() }
}

/**
 * Calm Studio modal tabs (`.modal .tabs`) — a two-or-more-way switch (e.g. Log in / Register) inside a
 * [ModalBody]. The active tab carries the `on` highlight. Generic over the tab type [T].
 *
 * @param tabs All tabs, in display order.
 * @param selected The currently active tab.
 * @param label Maps a tab to its already-translated label.
 * @param onSelect Invoked with the tab the user picked.
 */
@Composable
fun <T> ModalTabs(
    tabs: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
) {
    Div({ classes(CalmStudioStyleSheet.tabs) }) {
        tabs.forEach { tab ->
            Button(attrs = {
                type(ButtonType.Button)
                if (tab == selected) classes(CalmStudioStyleSheet.on)
                onClick { onSelect(tab) }
            }) { Text(label(tab)) }
        }
    }
}

/**
 * Calm Studio confirmation modal — a [CalmModal] with a question [title], a consequence [body], and a
 * Cancel / Confirm footer. The app's pattern for destructive confirmations (Delete item?, Discard
 * changes?).
 *
 * @param title Already-translated question-style heading.
 * @param body Already-translated consequence line.
 * @param confirmLabel Already-translated label for the confirm button.
 * @param cancelLabel Already-translated label for the cancel button.
 * @param danger When `true`, the confirm button uses the destructive [CalmButtonVariant.Danger] style.
 * @param onCancel Invoked when the user dismisses the modal.
 * @param onConfirm Invoked when the user confirms the action.
 */
@Composable
fun ConfirmModal(
    title: String,
    body: String,
    confirmLabel: String,
    cancelLabel: String,
    danger: Boolean = false,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    CalmModal(onDismiss = onCancel) {
        ModalHeader(title, body)
        ModalFooter {
            CalmButton(text = cancelLabel, onClick = onCancel, variant = CalmButtonVariant.Ghost)
            val confirmVariant = if (danger) CalmButtonVariant.Danger else CalmButtonVariant.Primary
            CalmButton(text = confirmLabel, onClick = onConfirm, variant = confirmVariant)
        }
    }
}
