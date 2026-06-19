package dev.inmo.wishlist.features.common.client.ui.components

import dev.inmo.wishlist.features.common.client.ui.CalmStudioStyleSheet
import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.disabled
import org.jetbrains.compose.web.attributes.placeholder
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Input
import org.jetbrains.compose.web.dom.Label
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.dom.TextArea

/**
 * Calm Studio form container (`.form`) — the max-width column wrapping a screen's fieldsets.
 *
 * @param content The form's fieldsets and action rows.
 */
@Composable
fun CalmForm(content: @Composable () -> Unit) {
    Div({ classes(CalmStudioStyleSheet.form) }) { content() }
}

/**
 * Calm Studio field block (`.fieldset`) — an optional bold [label] above caller-provided field content.
 *
 * Use this directly when a field needs a non-text control (a custom select, a priority picker, etc.);
 * for plain text fields prefer [CalmTextField] / [CalmTextArea].
 *
 * @param label Optional already-translated field label.
 * @param forId Optional id of the control inside [content]; when set, ties [label] to that control
 *   (`<label for=…>`) for accessibility. Omit when [content] has no single associable control.
 * @param content The field control(s).
 */
@Composable
fun FieldSet(label: String? = null, forId: String? = null, content: @Composable () -> Unit) {
    Div({ classes(CalmStudioStyleSheet.fieldset) }) {
        label?.let { Label(forId = forId) { Text(it) } }
        content()
    }
}

/**
 * Calm Studio labeled text field — a `.fieldset` wrapping a `.input`, with optional helper [hint].
 *
 * Mirrors the design's `Input` component. Restricted to string-valued input types ([InputType.Text],
 * [InputType.Password], [InputType.Email], …); numeric fields can drop to a raw [FieldSet] + [Input].
 *
 * @param value Current field value.
 * @param onValueChange Invoked with the new value on every input event.
 * @param label Optional already-translated field label.
 * @param placeholder Optional already-translated placeholder.
 * @param type Input type; defaults to [InputType.Text].
 * @param disabled Whether the field is non-interactive.
 * @param hint Optional already-translated helper text under the field.
 * @param id Optional element id; ties [label] to the input when set.
 */
@Composable
fun CalmTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String? = null,
    placeholder: String? = null,
    type: InputType<String> = InputType.Text,
    disabled: Boolean = false,
    hint: String? = null,
    id: String? = null,
) {
    Div({ classes(CalmStudioStyleSheet.fieldset) }) {
        label?.let { Label(forId = id) { Text(it) } }
        Input(type) {
            id?.let { id(it) }
            classes(CalmStudioStyleSheet.input)
            value(value)
            placeholder?.let { placeholder(it) }
            onInput { onValueChange(it.value) }
            if (disabled) disabled()
        }
        hint?.let { FormHint(it) }
    }
}

/**
 * Calm Studio labeled multi-line field — a `.fieldset` wrapping a `.textarea`.
 *
 * @param value Current field value.
 * @param onValueChange Invoked with the new value on every input event.
 * @param label Optional already-translated field label.
 * @param placeholder Optional already-translated placeholder.
 * @param disabled Whether the field is non-interactive.
 * @param hint Optional already-translated helper text under the field.
 * @param id Optional element id; ties [label] to the textarea when set.
 */
@Composable
fun CalmTextArea(
    value: String,
    onValueChange: (String) -> Unit,
    label: String? = null,
    placeholder: String? = null,
    disabled: Boolean = false,
    hint: String? = null,
    id: String? = null,
) {
    Div({ classes(CalmStudioStyleSheet.fieldset) }) {
        label?.let { Label(forId = id) { Text(it) } }
        TextArea {
            id?.let { id(it) }
            classes(CalmStudioStyleSheet.textarea)
            value(value)
            placeholder?.let { placeholder(it) }
            onInput { onValueChange(it.value) }
            if (disabled) disabled()
        }
        hint?.let { FormHint(it) }
    }
}

/**
 * Calm Studio form row (`.form-row`) — lays its children side by side with equal width (e.g. price +
 * amount). Place [FieldSet]s / [CalmTextField]s inside.
 *
 * @param content The side-by-side fields.
 */
@Composable
fun FormRow(content: @Composable () -> Unit) {
    Div({ classes(CalmStudioStyleSheet.`form-row`) }) { content() }
}

/**
 * Calm Studio form hint (`.hint`) — the muted helper line shown under a field; the [error] variant tints
 * it with the danger color (`.hint.danger`) for inline validation messages.
 *
 * @param text Already-translated helper text.
 * @param error When `true`, renders the hint in the destructive/error color.
 */
@Composable
fun FormHint(text: String, error: Boolean = false) {
    Div({
        classes(CalmStudioStyleSheet.hint)
        if (error) classes(CalmStudioStyleSheet.danger)
    }) { Text(text) }
}

/**
 * Calm Studio priority options (`.priopts` of `.priopt`) — a segmented single-choice control. The
 * selected option carries the `on` highlight. Generic over the option type [T].
 *
 * @param options All selectable options, in display order.
 * @param selected The currently active option.
 * @param label Maps an option to its already-translated label.
 * @param enabled When `false`, clicks are ignored (e.g. while a request is in flight).
 * @param onSelect Invoked with the option the user picked.
 */
@Composable
fun <T> PriorityOptions(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    enabled: Boolean = true,
    onSelect: (T) -> Unit,
) {
    Div({ classes(CalmStudioStyleSheet.priopts) }) {
        options.forEach { option ->
            val classNames = if (option == selected) arrayOf("priopt", "on") else arrayOf("priopt")
            Div({
                classes(*classNames)
                if (enabled) onClick { onSelect(option) }
            }) { Text(label(option)) }
        }
    }
}
