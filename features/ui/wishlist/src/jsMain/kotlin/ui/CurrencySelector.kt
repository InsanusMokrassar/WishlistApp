package dev.inmo.wishlist.features.ui.wishlist.ui

import androidx.compose.runtime.Composable
import dev.inmo.micro_utils.strings.translation
import dev.inmo.wishlist.features.currency.common.models.CurrencyCode
import dev.inmo.wishlist.features.currency.common.models.CurrencyInfo
import dev.inmo.wishlist.features.ui.wishlist.WishlistStrings
import org.jetbrains.compose.web.attributes.selected
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Option
import org.jetbrains.compose.web.dom.Select
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

/**
 * Bootstrap currency-conversion selector: a "Currency" caption plus a `<select>` listing the
 * "Original" no-conversion option followed by every available currency.
 *
 * Selecting an entry forwards the chosen [CurrencyCode] (or `null` for "Original") to
 * [onCurrencySelected]. The component is meant to be rendered only when the feature is enabled.
 *
 * @param currencies Available currencies to offer.
 * @param selected Currently selected target, or `null` for the "Original" option.
 * @param onCurrencySelected Invoked with the picked currency, or `null` for "Original".
 */
@Composable
fun CurrencySelector(
    currencies: List<CurrencyInfo>,
    selected: CurrencyCode?,
    onCurrencySelected: (CurrencyCode?) -> Unit,
) {
    val originalValue = ""
    Div({ classes("d-flex", "align-items-center", "mb-3", "gap-2", "flex-wrap") }) {
        Span({ classes("text-muted", "small") }) { Text(WishlistStrings.currencyLabel.translation()) }
        Select({
            classes("form-select", "form-select-sm", "w-auto")
            onChange { event ->
                val value = event.value
                onCurrencySelected(if (value.isNullOrEmpty()) null else CurrencyCode(value))
            }
        }) {
            Option(originalValue, {
                if (selected == null) selected()
            }) {
                Text(WishlistStrings.currencyOriginal.translation())
            }
            currencies.forEach { info ->
                Option(info.code.code, {
                    if (selected == info.code) selected()
                }) {
                    Text("${info.code.code} — ${info.name}")
                }
            }
        }
    }
}
