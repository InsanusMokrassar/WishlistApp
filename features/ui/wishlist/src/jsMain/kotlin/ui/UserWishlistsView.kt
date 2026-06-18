package dev.inmo.wishlist.features.ui.wishlist.ui

import dev.inmo.wishlist.features.common.client.ui.CalmStudioStyleSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dev.inmo.micro_utils.strings.translation
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.mvvm.compose.ComposeView
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.currency.common.models.CurrencyCode
import dev.inmo.wishlist.features.currency.common.models.CurrencyRates
import dev.inmo.wishlist.features.ui.topBar.ui.TopBarTitleProvider
import dev.inmo.wishlist.features.ui.wishlist.WishlistStrings
import dev.inmo.wishlist.features.wishlist.common.models.RegisteredWishlistItem
import org.jetbrains.compose.web.css.Style
import org.jetbrains.compose.web.css.StyleSheet
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.H3
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Text
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

/**
 * Dedicated stylesheet for [UserWishlistsView] — the only custom CSS the Calm Studio shell does not
 * already provide: the grouped-section header row (`Grouped` sort) carrying a wishlist title beside
 * its Open / Add actions.
 */
object UserWishlistsViewStylesheet : StyleSheet() {
    /** Flex header row above each grouped section: title on the left, actions on the right. */
    val sectionHead by style {
        property("display", "flex")
        property("align-items", "center")
        property("justify-content", "space-between")
        property("gap", "12px")
        property("margin", "26px 0 12px")
        property("padding-bottom", "8px")
        property("border-bottom", "1px solid var(--cs-line, #ECECEF)")
    }
}

/**
 * JS Compose-HTML list of every item across a user's wishlists (Calm Studio all-items view).
 *
 * Renders the items inside the standard `.content-inner` + `.pagehead` shell with a `.toolbar` of sort
 * + grid/list controls, then either grouped sections (each with its own header) under the `Grouped`
 * sort or a single flat `.grid` / `.rows`. Reuses [WishlistItemCard] and [WishlistItemRow]; class names
 * mirror the design skill's `app.jsx` so the Calm Studio shell CSS styles the screen directly.
 */
class UserWishlistsView(
    chain: NavigationChain<ViewConfig>,
    config: UserWishlistsViewConfig,
) : ComposeView<UserWishlistsViewConfig, ViewConfig, UserWishlistsViewModel>(config, chain), TopBarTitleProvider {
    override val viewModel: UserWishlistsViewModel by inject(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
        parametersOf(this@UserWishlistsView)
    }

    override val title: String
        @Composable get() {
            val userName by viewModel.userNameState.collectAsState()
            return userName?.let {
                WishlistStrings.userWishesTitleFormat.translation().replace("{name}", it)
            } ?: WishlistStrings.allItemsTitle.translation()
        }

    @Composable
    override fun onDraw() {
        super.onDraw()
        Style(UserWishlistsViewStylesheet)
        val sections by viewModel.sectionsState.collectAsState()
        val userName by viewModel.userNameState.collectAsState()
        val sortMode by viewModel.sortModeState.collectAsState()
        val sortedItems by viewModel.sortedItemsState.collectAsState()
        val viewMode by viewModel.viewModeState.collectAsState()
        val loading by viewModel.loadingState.collectAsState()
        val currencyEnabled by viewModel.currencyEnabledState.collectAsState()
        val currencies by viewModel.currenciesState.collectAsState()
        val selectedCurrency by viewModel.selectedCurrencyState.collectAsState()
        val rates by viewModel.ratesState.collectAsState()
        val costSortAvailable by viewModel.costSortAvailableState.collectAsState()
        val isOwner by viewModel.isOwnerState.collectAsState()
        val sortSelectorVisible by viewModel.sortSelectorVisibleState.collectAsState()

        Div({ classes(CalmStudioStyleSheet.`content-inner`) }) {
            Div({ classes(CalmStudioStyleSheet.pagehead) }) {
                Div {
                    H1 {
                        Text(
                            userName?.let { WishlistStrings.userWishesTitleFormat.translation().replace("{name}", it) }
                                ?: WishlistStrings.allItemsTitle.translation()
                        )
                    }
                }
                Div({ classes(CalmStudioStyleSheet.acts) }) {
                    CreateWishlistButton(isOwner) { viewModel.onCreateWishlist() }
                    Button({
                        classes(CalmStudioStyleSheet.btn)
                        onClick { viewModel.onOpenProfile() }
                    }) { Text(WishlistStrings.profileButton.translation()) }
                }
            }

            when {
                loading -> P({ classes(CalmStudioStyleSheet.subline) }) { Text(WishlistStrings.loading.translation()) }
                sections.isEmpty() -> Div({ classes("empty") }) {
                    H3 { Text(WishlistStrings.emptyItems.translation()) }
                }
                else -> {
                    WishlistSelectorsRow(
                        sortMode = sortMode,
                        onSortModeSelected = viewModel::onSortModeSelected,
                        costSortAvailable = costSortAvailable,
                        showSortSelector = sortSelectorVisible,
                        isCurrenciesFeatureEnabled = currencyEnabled,
                        currencies = currencies,
                        selectedCurrency = selectedCurrency,
                        onCurrencySelected = viewModel::onCurrencySelected,
                        viewMode = viewMode,
                        onViewModeSelected = viewModel::onViewModeSelected
                    )

                    if (sortMode == WishlistSortMode.None) {
                        sections.forEach { section ->
                            Div({ classes(UserWishlistsViewStylesheet.sectionHead) }) {
                                H3 { Text(section.wishlist.title) }
                                Div({ classes(CalmStudioStyleSheet.acts) }) {
                                    if (isOwner) {
                                        Button({
                                            classes(CalmStudioStyleSheet.btn, CalmStudioStyleSheet.primary, CalmStudioStyleSheet.sm)
                                            onClick { viewModel.onCreateItem(section.wishlist) }
                                        }) { Text(WishlistStrings.addItemButton.translation()) }
                                    }
                                    Button({
                                        classes(CalmStudioStyleSheet.btn, CalmStudioStyleSheet.sm)
                                        onClick { viewModel.onWishlistSelected(section.wishlist) }
                                    }) { Text(WishlistStrings.openWishlistButton.translation()) }
                                }
                            }
                            when {
                                section.items.isEmpty() -> P({ classes(CalmStudioStyleSheet.subline) }) { Text(WishlistStrings.emptyItems.translation()) }
                                viewMode == WishlistViewMode.Grid -> ItemsGrid(section.items.map { it to null })
                                else -> ItemRows(section.items.map { it to null }, selectedCurrency, rates)
                            }
                        }
                    } else {
                        if (viewMode == WishlistViewMode.Grid) {
                            ItemsGrid(sortedItems.map { it.item to it.wishlistTitle })
                        } else {
                            ItemRows(sortedItems.map { it.item to it.wishlistTitle }, selectedCurrency, rates)
                        }
                    }
                }
            }
        }
    }

    /**
     * Renders a Calm Studio `.grid` of item cards.
     *
     * @param entries Items paired with the originating wishlist title (used as the card's secondary
     * line when sorting across lists); `null` shows the item alone.
     */
    @Composable
    private fun ItemsGrid(entries: List<Pair<RegisteredWishlistItem, String?>>) {
        Div({ classes(CalmStudioStyleSheet.grid) }) {
            entries.forEach { (item, wishlistTitle) ->
                WishlistItemCard(
                    item = item,
                    wishlistTitle = wishlistTitle,
                    imageUrl = viewModel::imageUrl,
                    onSelect = { viewModel.onItemSelected(item) }
                )
            }
        }
    }

    /**
     * Renders a Calm Studio `.rows` list of items.
     *
     * @param entries Items paired with the originating wishlist title (appended after the title when
     * sorting across lists); `null` shows the item alone.
     * @param selectedCurrency Shared conversion target, or `null` for original prices.
     * @param rates Latest rates snapshot used to convert prices, or `null` when unavailable.
     */
    @Composable
    private fun ItemRows(
        entries: List<Pair<RegisteredWishlistItem, String?>>,
        selectedCurrency: CurrencyCode?,
        rates: CurrencyRates?,
    ) {
        Div({ classes(CalmStudioStyleSheet.rows) }) {
            entries.forEach { (item, wishlistTitle) ->
                WishlistItemRow(
                    item = item,
                    secondaryTitle = wishlistTitle,
                    selectedCurrency = selectedCurrency,
                    rates = rates,
                    imageUrl = viewModel::imageUrl,
                    onSelect = { viewModel.onItemSelected(item) }
                )
            }
        }
    }
}
