package dev.inmo.wishlist.features.ui.wishlist.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dev.inmo.micro_utils.strings.translation
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.mvvm.compose.ComposeView
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.common.client.ui.components.CalmButton
import dev.inmo.wishlist.features.common.client.ui.components.CalmButtonVariant
import dev.inmo.wishlist.features.common.client.ui.components.CalmIcons
import dev.inmo.wishlist.features.common.client.ui.components.ContentColumn
import dev.inmo.wishlist.features.common.client.ui.components.EmptyState
import dev.inmo.wishlist.features.common.client.ui.components.ItemGrid
import dev.inmo.wishlist.features.common.client.ui.components.PageHead
import dev.inmo.wishlist.features.common.client.ui.components.RowsList
import dev.inmo.wishlist.features.common.client.ui.components.Subline
import dev.inmo.wishlist.features.common.client.ui.components.Toaster
import dev.inmo.wishlist.features.ui.topBar.ui.TopBarTitleProvider
import dev.inmo.wishlist.features.ui.wishlist.WishlistStrings
import kotlinx.browser.window
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

/**
 * JS Compose-HTML view for the wishlist detail screen (Calm Studio list view).
 *
 * Composed from the shared Calm Studio components ([ContentColumn] + [PageHead] shell, [EmptyState], and
 * an [ItemGrid] of cards or [RowsList] of rows). The header carries a Share action (copies the page
 * link), an owner "Add item" / visitor "Copy to my profile" primary action, and the sort + view controls.
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

        ContentColumn {
            PageHead(
                title = wishlist?.title ?: "",
                actions = {
                    CalmButton(
                        text = WishlistStrings.shareButton.translation(),
                        onClick = { shareLink() },
                        leadingIcon = CalmIcons.share,
                    )
                    if (isOwner) {
                        CalmButton(
                            text = WishlistStrings.editButton.translation(),
                            onClick = { viewModel.onEditWishlist() },
                            leadingIcon = CalmIcons.edit,
                        )
                    }
                    when {
                        isOwner -> CalmButton(
                            text = WishlistStrings.addItemButton.translation(),
                            onClick = { viewModel.onAddItem() },
                            variant = CalmButtonVariant.Primary,
                            leadingIcon = CalmIcons.plus,
                        )
                        canCopy -> CalmButton(
                            text = WishlistStrings.copyWishlistButton.translation(),
                            onClick = { viewModel.onCopyWishlist() },
                            variant = CalmButtonVariant.Primary,
                            disabled = copyRequested,
                        )
                    }
                },
            )

            // Async copy result surfaces as a toast (queued / failed), keyed on the view-model state.
            LaunchedEffect(copyRequested) {
                if (copyRequested) Toaster.show(WishlistStrings.copyQueued.translation())
            }
            LaunchedEffect(copyFailed) {
                if (copyFailed) Toaster.show(WishlistStrings.copyFailed.translation())
            }

            when {
                loading -> Subline(WishlistStrings.loading.translation())
                items.isEmpty() -> EmptyState(
                    icon = CalmIcons.gift,
                    title = WishlistStrings.emptyItems.translation(),
                    action = {
                        if (isOwner) {
                            CalmButton(
                                text = WishlistStrings.addItemButton.translation(),
                                onClick = { viewModel.onAddItem() },
                                variant = CalmButtonVariant.Primary,
                                leadingIcon = CalmIcons.plus,
                            )
                        }
                    },
                )
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
                        ItemGrid {
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
                        RowsList {
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
