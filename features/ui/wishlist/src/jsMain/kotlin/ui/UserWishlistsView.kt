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
import dev.inmo.wishlist.features.currency.common.models.CurrencyCode
import dev.inmo.wishlist.features.currency.common.models.CurrencyRates
import dev.inmo.wishlist.features.currency.common.utils.formatItemPrice
import dev.inmo.wishlist.features.ui.topBar.ui.TopBarTitleProvider
import dev.inmo.wishlist.features.ui.wishlist.WishlistStrings
import org.jetbrains.compose.web.css.height
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H6
import org.jetbrains.compose.web.dom.Img
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.dom.Ul
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

/** JS Compose-HTML list of every item across a user's wishlists. Uses the shared Bootstrap [ListRow]. */
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
        val sections by viewModel.sectionsState.collectAsState()
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

        Div({ classes("container", "py-3") }) {
            Div({ classes("d-flex", "align-items-center", "mb-3", "gap-2") }) {
                BackButton(WishlistStrings.backButton.translation()) { viewModel.onBack() }
                Div({ classes("d-flex", "align-items-center", "gap-2", "ms-auto") }) {
                    CreateWishlistButton(isOwner) { viewModel.onCreateWishlist() }
                    Button({
                        classes("btn", "btn-outline-primary")
                        onClick { viewModel.onOpenProfile() }
                    }) {
                        Text(WishlistStrings.profileButton.translation())
                    }
                }
            }

            when {
                loading -> {
                    P { Text(WishlistStrings.loading.translation()) }
                }
                sections.isEmpty() -> {
                    P({ classes("text-muted") }) { Text(WishlistStrings.emptyItems.translation()) }
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
                            Div({
                                classes(
                                    "d-flex",
                                    "align-items-center",
                                    "justify-content-between",
                                    "mt-3",
                                    "mb-1",
                                    "border-bottom",
                                    "pb-1"
                                )
                            }) {
                                H6({ classes("mb-0", "text-muted") }) { Text(section.wishlist.title) }
                                Div({ classes("d-flex", "align-items-center", "gap-2") }) {
                                    if (isOwner) {
                                        Button({
                                            classes("btn", "btn-sm", "btn-primary")
                                            onClick { viewModel.onCreateItem(section.wishlist) }
                                        }) {
                                            Text(WishlistStrings.addItemButton.translation())
                                        }
                                    }
                                    Button({
                                        classes("btn", "btn-sm", "btn-outline-primary")
                                        onClick { viewModel.onWishlistSelected(section.wishlist) }
                                    }) {
                                        Text(WishlistStrings.openWishlistButton.translation())
                                    }
                                }
                            }
                            when {
                                section.items.isEmpty() -> P({ classes("text-muted") }) { Text(WishlistStrings.emptyItems.translation()) }
                                viewMode == WishlistViewMode.Grid -> ItemsGrid(section.items.map { it to section.wishlist.title })
                                else -> Ul({ classes("list-group") }) {
                                    section.items.forEach { item -> ItemRow(item, null, selectedCurrency, rates) }
                                }
                            }
                        }
                    } else {
                        if (viewMode == WishlistViewMode.Grid) {
                            ItemsGrid(sortedItems.map { it.item to it.wishlistTitle })
                        } else {
                            Ul({ classes("list-group") }) {
                                sortedItems.forEach { sorted ->
                                    ItemRow(
                                        sorted.item,
                                        sorted.wishlistTitle,
                                        selectedCurrency,
                                        rates
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Renders a single item row reusing the shared Bootstrap [ListRow].
     *
     * @param item Item to display.
     * @param wishlistTitle When non-null (custom sorting active), appended after the item title in
     * brackets so the originating wishlist stays visible without the grouping headers.
     * @param selectedCurrency Shared conversion target, or `null` for original prices.
     * @param rates Latest rates snapshot used to convert the price, or `null` when unavailable.
     */
    @Composable
    private fun ItemRow(
        item: dev.inmo.wishlist.features.wishlist.common.models.RegisteredWishlistItem,
        wishlistTitle: String?,
        selectedCurrency: CurrencyCode?,
        rates: CurrencyRates?
    ) {
        ListRow(
            onSelect = { viewModel.onItemSelected(item) },
            leading = {
                val firstImage = item.imageIds.firstOrNull()
                if (firstImage != null) {
                    Img(src = viewModel.imageUrl(firstImage), alt = "") {
                        classes("rounded", "flex-shrink-0")
                        style {
                            width(48.px)
                            height(48.px)
                            property("object-fit", "cover")
                        }
                    }
                } else {
                    WishlistItemImagePlaceholder(
                        alt = WishlistStrings.itemImagePlaceholderAlt.translation()
                    ) {
                        classes("rounded", "flex-shrink-0")
                        style {
                            width(48.px)
                            height(48.px)
                        }
                    }
                }
            }
        ) {
            Div({ classes("flex-grow-1") }) {
                Div({ classes("d-flex", "justify-content-between", "align-items-center") }) {
                    Div({ classes("d-flex", "align-items-center", "gap-2") }) {
                        Span {
                            Text(wishlistTitle?.let { "${item.title} ($it)" } ?: item.title)
                        }
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

    /**
     * Renders a responsive Bootstrap card grid of items.
     *
     * @param entries Items paired with the title of the wishlist each one belongs to (used as the
     * card subtitle).
     */
    @Composable
    private fun ItemsGrid(
        entries: List<Pair<dev.inmo.wishlist.features.wishlist.common.models.RegisteredWishlistItem, String?>>
    ) {
        Div({ classes("row", "row-cols-1", "row-cols-sm-2", "row-cols-md-3", "g-3", "mt-1") }) {
            entries.forEach { (item, wishlistTitle) ->
                Div({ classes("col") }) {
                    WishlistItemCard(
                        item = item,
                        wishlistTitle = wishlistTitle,
                        imageUrl = viewModel::imageUrl,
                        onSelect = { viewModel.onItemSelected(item) }
                    )
                }
            }
        }
    }
}
