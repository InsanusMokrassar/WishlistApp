package dev.inmo.wishlist.features.ui.wishlist.ui

import androidx.compose.runtime.Composable
import dev.inmo.wishlist.features.currency.common.utils.PriceUnitsResolver
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.disabled
import org.jetbrains.compose.web.attributes.placeholder
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Input
import org.jetbrains.compose.web.dom.Label
import org.jetbrains.compose.web.dom.Option
import org.jetbrains.compose.web.dom.Select
import org.jetbrains.compose.web.dom.Text

/**
 * Bootstrap currency/units input: a free-text field (custom currency) next to a `<select>` of preset
 * currency symbols taken from [PriceUnitsResolver]. Picking a preset overwrites the text; the user may
 * also type any custom value.
 *
 * @param label Localized caption for the input.
 * @param value Current units string.
 * @param enabled Disables both controls while a request is in flight.
 * @param onValueChange Invoked with the new units string (preset pick or manual edit).
 * @param id DOM id linking the [Label] to the text input.
 */
@Composable
fun PriceUnitsSelector(
    label: String,
    value: String,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
    id: String = "price-units",
) {
    val presets = PriceUnitsResolver.symbolToCode.keys.toList()
    Label(id) { Text(label) }
    Div({ classes("input-group") }) {
        Input(InputType.Text) {
            id(id)
            classes("form-control")
            value(value)
            placeholder("$, €, USD...")
            onInput { onValueChange(it.value) }
            if (!enabled) disabled()
        }
        Select({
            classes("form-select", "flex-grow-0", "w-auto")
            onChange { event ->
                val picked = event.value
                if (!picked.isNullOrEmpty()) onValueChange(picked)
            }
            if (!enabled) disabled()
        }) {
            Option("") { Text("▾") }
            presets.forEach { preset ->
                Option(preset) { Text(preset) }
            }
        }
    }
}
