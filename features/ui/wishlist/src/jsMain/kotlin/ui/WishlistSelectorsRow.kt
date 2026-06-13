package dev.inmo.wishlist.features.ui.wishlist.ui

import androidx.compose.runtime.Composable
import dev.inmo.micro_utils.strings.StringResource
import dev.inmo.wishlist.features.currency.common.models.CurrencyCode
import dev.inmo.wishlist.features.currency.common.models.CurrencyInfo
import dev.inmo.wishlist.features.ui.wishlist.WishlistStrings
import org.jetbrains.compose.web.dom.Div

/**
 * Reusable row of the wishlist items controls — the sort selector, the optional currency selector and
 * the view-mode selector — laid out together. The sort selector is conditionally rendered based on
 * [showSortSelector]; it is hidden while fewer than two items are loaded (PR #31 T1, operator decision).
 *
 * The three selectors sit in a row on wider screens (`flex-md-row` + `flex-wrap`) and gracefully fall
 * back to a vertical column on narrow ones (`flex-column`). The currency selector is only rendered when
 * [isCurrenciesFeatureEnabled] is `true` and [currencies] is not empty. Shared by [WishlistView] and
 * [UserWishlistsView].
 *
 * @param sortMode Currently selected sort mode.
 * @param onSortModeSelected Invoked with the sort mode the user picked.
 * @param costSortAvailable Whether cost-based sort modes are offered (forwarded to [sortModesFor]).
 * @param showSortSelector Whether the sort selector is rendered; pass the screen's ViewModel
 *   `sortSelectorVisibleState` — hidden while fewer than two items are shown.
 * @param isCurrenciesFeatureEnabled Whether the currency feature is enabled for the current user.
 * @param currencies Currencies available for selection.
 * @param selectedCurrency Currently selected currency, or `null` for original prices.
 * @param onCurrencySelected Invoked with the currency the user picked (`null` resets to original).
 * @param viewMode Currently selected items view mode.
 * @param onViewModeSelected Invoked with the view mode the user picked.
 * @param noneLabel Label for the "no sort" option; defaults to [WishlistStrings.sortNone].
 */
@Composable
fun WishlistSelectorsRow(
    sortMode: WishlistSortMode,
    onSortModeSelected: (WishlistSortMode) -> Unit,
    costSortAvailable: Boolean,
    showSortSelector: Boolean,
    isCurrenciesFeatureEnabled: Boolean,
    currencies: List<CurrencyInfo>,
    selectedCurrency: CurrencyCode?,
    onCurrencySelected: (CurrencyCode?) -> Unit,
    viewMode: WishlistViewMode,
    onViewModeSelected: (WishlistViewMode) -> Unit,
    noneLabel: StringResource = WishlistStrings.sortNone,
) {
    Div({
        classes(
            "d-flex", "flex-column", "flex-md-row", "flex-wrap",
            "gap-md-3", "align-items-md-start", "mb-3"
        )
    }) {
        if (showSortSelector) {
            WishlistSortSelector(
                selected = sortMode,
                onSortModeSelected = onSortModeSelected,
                noneLabel = noneLabel,
                availableModes = sortModesFor(costSortAvailable)
            )
        }
        if (isCurrenciesFeatureEnabled && currencies.isNotEmpty()) {
            CurrencySelector(
                currencies = currencies,
                selected = selectedCurrency,
                onCurrencySelected = onCurrencySelected
            )
        }
        ViewModeSelector(
            selected = viewMode,
            onViewModeSelected = onViewModeSelected
        )
    }
}
