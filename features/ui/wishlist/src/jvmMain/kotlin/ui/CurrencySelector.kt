package dev.inmo.wishlist.features.ui.wishlist.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.inmo.micro_utils.strings.translation
import dev.inmo.wishlist.features.currency.common.models.CurrencyCode
import dev.inmo.wishlist.features.currency.common.models.CurrencyInfo
import dev.inmo.wishlist.features.ui.wishlist.WishlistStrings

/**
 * Compose-Desktop currency-conversion selector: a "Currency" caption followed by a dropdown button
 * showing the current selection. The menu lists the "Original" no-conversion option plus every
 * available currency.
 *
 * @param currencies Available currencies to offer.
 * @param selected Currently selected target, or `null` for "Original".
 * @param onCurrencySelected Invoked with the picked currency, or `null` for "Original".
 */
@Composable
fun CurrencySelector(
    currencies: List<CurrencyInfo>,
    selected: CurrencyCode?,
    onCurrencySelected: (CurrencyCode?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val buttonLabel = selected?.code ?: WishlistStrings.currencyOriginal.translation()
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(WishlistStrings.currencyLabel.translation(), style = MaterialTheme.typography.caption)
        Box {
            OutlinedButton(onClick = { expanded = true }) { Text(buttonLabel) }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(onClick = {
                    expanded = false
                    onCurrencySelected(null)
                }) {
                    Text(WishlistStrings.currencyOriginal.translation())
                }
                currencies.forEach { info ->
                    DropdownMenuItem(onClick = {
                        expanded = false
                        onCurrencySelected(info.code)
                    }) {
                        Text("${info.code.code} — ${info.name}")
                    }
                }
            }
        }
    }
}
