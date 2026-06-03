package dev.inmo.wishlist.features.ui.wishlist.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.OutlinedButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.inmo.wishlist.features.currency.common.utils.PriceUnitsResolver

/**
 * Compose-Desktop currency/units input: a free-text field (custom currency) paired with a dropdown of
 * preset currencies taken from [PriceUnitsResolver]. Typing a currency code (or symbol) into the field
 * filters the dropdown via [PriceUnitsResolver.search] — e.g. typing `USD` surfaces `$` — and picking
 * an entry overwrites the text with its symbol; the user may also type any custom value.
 *
 * @param label Localized caption for the input.
 * @param value Current units string.
 * @param enabled Disables both controls while a request is in flight.
 * @param onValueChange Invoked with the new units string (preset pick or manual edit).
 * @param modifier Layout modifier applied to the row.
 */
@Composable
fun PriceUnitsSelector(
    label: String,
    value: String,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val results = remember(value) { PriceUnitsResolver.search(value).ifEmpty { PriceUnitsResolver.entries } }
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            singleLine = true,
            enabled = enabled,
            modifier = Modifier.weight(1f)
        )
        Box {
            OutlinedButton(onClick = { expanded = true }, enabled = enabled) { Text("▾") }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                results.forEach { (code, symbol) ->
                    DropdownMenuItem(onClick = {
                        expanded = false
                        onValueChange(symbol)
                    }) { Text("${code.code}  $symbol") }
                }
            }
        }
    }
}
