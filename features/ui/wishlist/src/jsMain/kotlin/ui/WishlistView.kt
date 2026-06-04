package dev.inmo.wishlist.features.ui.wishlist.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dev.inmo.micro_utils.strings.translation
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.mvvm.compose.ComposeView
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.common.client.ui.components.BackButton
import dev.inmo.wishlist.features.common.client.ui.components.ListRow
import dev.inmo.wishlist.features.currency.common.utils.formatItemPrice
import dev.inmo.wishlist.features.ui.topBar.ui.TopBarTitleProvider
import dev.inmo.wishlist.features.ui.wishlist.WishlistStrings
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.dom.Ul
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

/** JS Compose-HTML view for the wishlist detail screen. Uses Bootstrap classes. */
class WishlistView(
    chain: NavigationChain<ViewConfig>,
    config: WishlistViewConfig,
) : ComposeView<WishlistViewConfig, ViewConfig, WishlistViewModel>(config, chain), TopBarTitleProvider {
    override val viewModel: WishlistViewModel by inject(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
        parametersOf(this@WishlistView)
    }

    override val title: String
        @Composable get() {
            val wishlist by viewModel.wishlistState.collectAsState()
            return wishlist?.title ?: ""
        }

    @Composable
    override fun onDraw() {
        super.onDraw()
        val wishlist by viewModel.wishlistState.collectAsState()
        val items by viewModel.itemsState.collectAsState()
        val sortMode by viewModel.sortModeState.collectAsState()
        val sortedItems by viewModel.sortedItemsState.collectAsState()
        val viewMode by viewModel.viewModeState.collectAsState()
        val isOwner by viewModel.isOwnerState.collectAsState()
        val loading by viewModel.loadingState.collectAsState()
        val currencyEnabled by viewModel.currencyEnabledState.collectAsState()
        val currencies by viewModel.currenciesState.collectAsState()
        val selectedCurrency by viewModel.selectedCurrencyState.collectAsState()
        val rates by viewModel.ratesState.collectAsState()
        val costSortAvailable by viewModel.costSortAvailableState.collectAsState()

        Div({ classes("container", "py-3") }) {
            Div({ classes("d-flex", "align-items-center", "mb-3", "gap-2") }) {
                BackButton(WishlistStrings.backButton.translation()) { viewModel.onBack() }
                Div({ classes("flex-grow-1") }) {}
                if (isOwner) {
                    Button({
                        classes("btn", "btn-outline-primary")
                        onClick { viewModel.onEditWishlist() }
                    }) {
                        Text(WishlistStrings.editButton.translation())
                    }
                }
            }

            if (loading) {
                P { Text(WishlistStrings.loading.translation()) }
            } else if (items.isEmpty()) {
                P({ classes("text-muted") }) {
                    Text(WishlistStrings.emptyItems.translation())
                }
            } else {
                WishlistSelectorsRow(
                    sortMode = sortMode,
                    onSortModeSelected = viewModel::onSortModeSelected,
                    costSortAvailable = costSortAvailable,
                    noneLabel = WishlistStrings.sortDefault,
                    isCurrenciesFeatureEnabled = currencyEnabled,
                    currencies = currencies,
                    selectedCurrency = selectedCurrency,
                    onCurrencySelected = viewModel::onCurrencySelected,
                    viewMode = viewMode,
                    onViewModeSelected = viewModel::onViewModeSelected
                )
                if (viewMode == WishlistViewMode.Grid) {
                    Div({ classes("row", "row-cols-1", "row-cols-sm-2", "row-cols-md-3", "g-3", "mb-3") }) {
                        sortedItems.forEach { item ->
                            Div({ classes("col") }) {
                                WishlistItemCard(
                                    item = item,
                                    wishlistTitle = wishlist?.title,
                                    imageUrl = viewModel::imageUrl,
                                    onSelect = { viewModel.onViewItem(item.id) }
                                )
                            }
                        }
                    }
                } else {
                    Ul({ classes("list-group", "mb-3") }) {
                        sortedItems.forEach { item ->
                            ListRow(onSelect = { viewModel.onViewItem(item.id) }) {
                                Div({ classes("flex-grow-1") }) {
                                    Div({ classes("d-flex", "justify-content-between", "align-items-center") }) {
                                        Div({ classes("d-flex", "align-items-center", "gap-2") }) {
                                            Span { Text(item.title) }
                                            PriorityBadge(item.priority)
                                        }
                                        if (item.approximatePrice != null) {
                                            Span({ classes("text-muted", "small") }) {
                                                Text(
                                                    formatItemPrice(
                                                        item.approximatePrice,
                                                        item.priceUnits,
                                                        selectedCurrency,
                                                        rates
                                                    )
                                                )
                                            }
                                        }
                                    }
                                    if (item.description.isNotBlank()) {
                                        P({ classes("mb-0", "text-muted", "small", "mt-1") }) {
                                            Text(item.description)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (isOwner) {
                Button({
                    classes("btn", "btn-success")
                    onClick { viewModel.onAddItem() }
                }) {
                    Text(WishlistStrings.addItemButton.translation())
                }
            }
        }
    }
}
