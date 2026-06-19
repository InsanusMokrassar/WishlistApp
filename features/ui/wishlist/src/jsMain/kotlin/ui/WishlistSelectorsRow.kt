package dev.inmo.wishlist.features.ui.wishlist.ui

import androidx.compose.runtime.Composable
import dev.inmo.micro_utils.strings.StringResource
import dev.inmo.wishlist.features.common.client.ui.components.Toolbar
import dev.inmo.wishlist.features.currency.common.models.CurrencyCode
import dev.inmo.wishlist.features.currency.common.models.CurrencyInfo
import dev.inmo.wishlist.features.ui.wishlist.WishlistStrings

/**
 * Calm Studio screen [Toolbar] for the wishlist item collections — the sort selector, the optional
 * currency selector and the grid/list view-mode toggle, grouped on the right. The sort selector is
 * rendered only when [showSortSelector] is `true` (hidden while fewer than two items are loaded — PR #31
 * T1, operator decision); the currency selector only when [isCurrenciesFeatureEnabled] is `true` and
 * [currencies] is not empty. Shared by [WishlistView] and [UserWishlistsView].
 *
 * The left cell is intentionally empty (the app exposes no item filter backed by the data layer);
 * keeping it preserves the toolbar's space-between layout so the controls sit flush right.
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
    Toolbar(
        right = {
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
        },
    )
}
