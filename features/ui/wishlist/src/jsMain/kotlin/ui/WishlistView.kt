package dev.inmo.wishlist.features.ui.wishlist.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dev.inmo.micro_utils.strings.translation
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.mvvm.compose.ComposeView
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.common.client.ui.components.CalmIcon
import dev.inmo.wishlist.features.common.client.ui.components.CalmIcons
import dev.inmo.wishlist.features.common.client.ui.components.Toaster
import dev.inmo.wishlist.features.ui.topBar.ui.TopBarTitleProvider
import dev.inmo.wishlist.features.ui.wishlist.WishlistStrings
import kotlinx.browser.window
import org.jetbrains.compose.web.attributes.disabled
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.H3
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Text
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

/**
 * JS Compose-HTML view for the wishlist detail screen (Calm Studio list view).
 *
 * Renders the list inside the standard `.content-inner` + `.pagehead` shell: a Share action (copies the
 * page link), an owner "Add item" / visitor "Copy to my profile" primary action, a `.toolbar` carrying
 * the sort + grid/list controls, and the items as a `.grid` of cards or `.rows` of list rows. Class
 * names mirror the design skill's `app.jsx` so the Calm Studio shell CSS styles the screen directly.
 */
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

    /** Copies the current page URL to the clipboard, the web client's "Share" behavior. */
    private fun shareLink() {
        runCatching { window.navigator.asDynamic().clipboard?.writeText(window.location.href) }
        Toaster.show(WishlistStrings.shareLinkCopiedToast.translation())
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
        val canCopy by viewModel.canCopyState.collectAsState()
        val copyRequested by viewModel.copyRequestedState.collectAsState()
        val copyFailed by viewModel.copyFailedState.collectAsState()
        val loading by viewModel.loadingState.collectAsState()
        val currencyEnabled by viewModel.currencyEnabledState.collectAsState()
        val currencies by viewModel.currenciesState.collectAsState()
        val selectedCurrency by viewModel.selectedCurrencyState.collectAsState()
        val rates by viewModel.ratesState.collectAsState()
        val costSortAvailable by viewModel.costSortAvailableState.collectAsState()
        val sortSelectorVisible by viewModel.sortSelectorVisibleState.collectAsState()

        Div({ classes("content-inner") }) {
            Div({ classes("pagehead") }) {
                Div {
                    H1 { Text(wishlist?.title ?: "") }
                }
                Div({ classes("acts") }) {
                    Button({
                        classes("btn")
                        onClick { shareLink() }
                    }) {
                        CalmIcon(CalmIcons.share)
                        Text(WishlistStrings.shareButton.translation())
                    }
                    if (isOwner) {
                        Button({
                            classes("btn")
                            onClick { viewModel.onEditWishlist() }
                        }) {
                            CalmIcon(CalmIcons.edit)
                            Text(WishlistStrings.editButton.translation())
                        }
                    }
                    when {
                        isOwner -> Button({
                            classes("btn", "primary")
                            onClick { viewModel.onAddItem() }
                        }) {
                            CalmIcon(CalmIcons.plus)
                            Text(WishlistStrings.addItemButton.translation())
                        }
                        canCopy -> Button({
                            classes("btn", "primary")
                            if (copyRequested) disabled()
                            onClick { viewModel.onCopyWishlist() }
                        }) {
                            Text(WishlistStrings.copyWishlistButton.translation())
                        }
                    }
                }
            }

            // Async copy result surfaces as a toast (queued / failed), keyed on the view-model state.
            LaunchedEffect(copyRequested) {
                if (copyRequested) Toaster.show(WishlistStrings.copyQueued.translation())
            }
            LaunchedEffect(copyFailed) {
                if (copyFailed) Toaster.show(WishlistStrings.copyFailed.translation())
            }

            when {
                loading -> P({ classes("subline") }) { Text(WishlistStrings.loading.translation()) }
                items.isEmpty() -> Div({ classes("empty") }) {
                    Div({ classes("ic") }) { CalmIcon(CalmIcons.gift) }
                    H3 { Text(WishlistStrings.emptyItems.translation()) }
                    if (isOwner) {
                        Button({
                            classes("btn", "primary")
                            onClick { viewModel.onAddItem() }
                        }) {
                            CalmIcon(CalmIcons.plus)
                            Text(WishlistStrings.addItemButton.translation())
                        }
                    }
                }
                else -> {
                    WishlistSelectorsRow(
                        sortMode = sortMode,
                        onSortModeSelected = viewModel::onSortModeSelected,
                        costSortAvailable = costSortAvailable,
                        showSortSelector = sortSelectorVisible,
                        noneLabel = WishlistStrings.sortDefault,
                        isCurrenciesFeatureEnabled = currencyEnabled,
                        currencies = currencies,
                        selectedCurrency = selectedCurrency,
                        onCurrencySelected = viewModel::onCurrencySelected,
                        viewMode = viewMode,
                        onViewModeSelected = viewModel::onViewModeSelected
                    )
                    if (viewMode == WishlistViewMode.Grid) {
                        Div({ classes("grid") }) {
                            sortedItems.forEach { item ->
                                WishlistItemCard(
                                    item = item,
                                    wishlistTitle = null,
                                    imageUrl = viewModel::imageUrl,
                                    onSelect = { viewModel.onViewItem(item.id) }
                                )
                            }
                        }
                    } else {
                        Div({ classes("rows") }) {
                            sortedItems.forEach { item ->
                                WishlistItemRow(
                                    item = item,
                                    secondaryTitle = null,
                                    selectedCurrency = selectedCurrency,
                                    rates = rates,
                                    imageUrl = viewModel::imageUrl,
                                    onSelect = { viewModel.onViewItem(item.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
