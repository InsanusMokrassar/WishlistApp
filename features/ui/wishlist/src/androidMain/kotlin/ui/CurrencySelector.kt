package dev.inmo.wishlist.features.ui.wishlist.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.unit.dp
import dev.inmo.micro_utils.strings.translation
import dev.inmo.wishlist.features.currency.common.models.CurrencyCode
import dev.inmo.wishlist.features.currency.common.models.CurrencyInfo
import dev.inmo.wishlist.features.ui.wishlist.WishlistStrings

/**
 * Material3 currency-conversion selector: a "Currency" caption followed by a dropdown button showing
 * the current selection. The menu lists the "Original" no-conversion option plus every available
 * currency.
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
    val resources = LocalResources.current
    var expanded by remember { mutableStateOf(false) }
    val buttonLabel = selected?.code ?: WishlistStrings.currencyOriginal.translation(resources)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(WishlistStrings.currencyLabel.translation(resources), style = MaterialTheme.typography.bodySmall)
        Box {
            OutlinedButton(onClick = { expanded = true }) { Text(buttonLabel) }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(
                    text = { Text(WishlistStrings.currencyOriginal.translation(resources)) },
                    onClick = {
                        expanded = false
                        onCurrencySelected(null)
                    }
                )
                currencies.forEach { info ->
                    DropdownMenuItem(
                        text = { Text("${info.code.code} — ${info.name}") },
                        onClick = {
                            expanded = false
                            onCurrencySelected(info.code)
                        }
                    )
                }
            }
        }
    }
}
