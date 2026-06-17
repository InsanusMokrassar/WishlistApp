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
 * Calm Studio currency/units input rendered as a `.fieldset`: a free-text `.input` (custom currency)
 * next to a `.select` of preset currencies from [PriceUnitsResolver]. Each option is labelled
 * `CODE symbol` and ordered by code, so the native type-ahead lets the user find a symbol by typing
 * its code (e.g. `USD` → `$`); picking an option overwrites the text with its symbol. The user may also
 * type any custom value into the field.
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
    val entries = PriceUnitsResolver.entries
    Div({ classes("fieldset") }) {
        Label(id) { Text(label) }
        Div({
            style {
                property("display", "flex")
                property("gap", "8px")
            }
        }) {
            Input(InputType.Text) {
                id(id)
                classes("input")
                value(value)
                placeholder("$, €, USD...")
                onInput { onValueChange(it.value) }
                if (!enabled) disabled()
            }
            Select({
                classes("select")
                onChange { event ->
                    val picked = event.value
                    if (!picked.isNullOrEmpty()) onValueChange(picked)
                }
                if (!enabled) disabled()
            }) {
                Option("") { Text("▾") }
                entries.forEach { (code, symbol) ->
                    Option(symbol) { Text("${code.code}  $symbol") }
                }
            }
        }
    }
}
