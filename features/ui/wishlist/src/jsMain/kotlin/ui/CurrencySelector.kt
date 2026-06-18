package dev.inmo.wishlist.features.ui.wishlist.ui

import dev.inmo.wishlist.features.common.client.ui.CalmStudioStyleSheet
import androidx.compose.runtime.Composable
import dev.inmo.micro_utils.strings.translation
import dev.inmo.wishlist.features.currency.common.models.CurrencyCode
import dev.inmo.wishlist.features.currency.common.models.CurrencyInfo
import dev.inmo.wishlist.features.ui.wishlist.WishlistStrings
import org.jetbrains.compose.web.attributes.selected
import org.jetbrains.compose.web.dom.Option
import org.jetbrains.compose.web.dom.Select
import org.jetbrains.compose.web.dom.Text

/**
 * Calm Studio currency-conversion selector rendered as a native `.select`: the "Original"
 * no-conversion option followed by every available currency. Selecting an entry forwards the chosen
 * [CurrencyCode] (or `null` for "Original") to [onCurrencySelected]. Meant to sit in a screen toolbar;
 * carries no caption of its own and is rendered only when the feature is enabled.
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
    Select({
        classes(CalmStudioStyleSheet.select)
        onChange { event ->
            val value = event.value
            onCurrencySelected(if (value.isNullOrEmpty()) null else CurrencyCode(value))
        }
    }) {
        Option("", { if (selected == null) selected() }) {
            Text(WishlistStrings.currencyOriginal.translation())
        }
        currencies.forEach { info ->
            Option(info.code.code, { if (selected == info.code) selected() }) {
                Text("${info.code.code} — ${info.name}")
            }
        }
    }
}
