package dev.inmo.wishlist.features.ui.wishlist.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.unit.dp
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
import dev.inmo.wishlist.features.wishlist.common.models.RegisteredWishlistItem
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

/** Android Compose-Material3 list of every item across a user's wishlists. Uses the shared [ListRow]. */
class UserWishlistsView(
    chain: NavigationChain<ViewConfig>,
    config: UserWishlistsViewConfig,
) : ComposeView<UserWishlistsViewConfig, ViewConfig, UserWishlistsViewModel>(config, chain), TopBarTitleProvider {
    override val viewModel: UserWishlistsViewModel by inject(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
        parametersOf(this@UserWishlistsView)
    }

    override val title: String
        @Composable get() {
            val resources = LocalResources.current
            val userName by viewModel.userNameState.collectAsState()
            return userName?.let {
                WishlistStrings.userWishesTitleFormat.translation(resources).replace("{name}", it)
            } ?: WishlistStrings.allItemsTitle.translation(resources)
        }

    @Composable
    override fun onDraw() {
        super.onDraw()
        val resources = LocalResources.current
        val sections by viewModel.sectionsState.collectAsState()
        val backLabel by viewModel.backLabelState.collectAsState()
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

        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BackButton(backLabel ?: WishlistStrings.usersListBackLabel.translation(resources)) { viewModel.onBack() }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CreateWishlistButton(isOwner) { viewModel.onCreateWishlist() }
                    Button(onClick = { viewModel.onOpenProfile() }) {
                        Text(WishlistStrings.profileButton.translation(resources))
                    }
                }
            }

            when {
                loading -> Text(WishlistStrings.loading.translation(resources))
                sections.isEmpty() -> Text(WishlistStrings.emptyItems.translation(resources), style = MaterialTheme.typography.bodySmall)
                else -> {
                if (sortSelectorVisible) {
                    WishlistSortSelector(
                        selected = sortMode,
                        onSortModeSelected = viewModel::onSortModeSelected,
                        availableModes = sortModesFor(costSortAvailable)
                    )
                }
                if (currencyEnabled && currencies.isNotEmpty()) {
                    CurrencySelector(
                        currencies = currencies,
                        selected = selectedCurrency,
                        onCurrencySelected = viewModel::onCurrencySelected
                    )
                }
                ViewModeSelector(
                    selected = viewMode,
                    onViewModeSelected = viewModel::onViewModeSelected
                )

                if (viewMode == WishlistViewMode.Grid) {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (sortMode == WishlistSortMode.None) {
                            sections.forEach { section ->
                                SectionHeader(section, isOwner)
                                if (section.items.isEmpty()) {
                                    Text(WishlistStrings.emptyItems.translation(resources), style = MaterialTheme.typography.bodySmall)
                                } else {
                                    ItemCardsGrid(section.items.map { it to section.wishlist.title })
                                }
                            }
                        } else {
                            ItemCardsGrid(sortedItems.map { it.item to it.wishlistTitle })
                        }
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (sortMode == WishlistSortMode.None) {
                            sections.forEach { section ->
                                item(key = "header-${section.wishlist.id.long}") {
                                    SectionHeader(section, isOwner)
                                }
                                if (section.items.isEmpty()) {
                                    item(key = "empty-${section.wishlist.id.long}") {
                                        Text(WishlistStrings.emptyItems.translation(resources), style = MaterialTheme.typography.bodySmall)
                                    }
                                } else {
                                    items(section.items, key = { it.id.long }) { item ->
                                        ItemRow(item, null, selectedCurrency, rates)
                                    }
                                }
                            }
                        } else {
                            items(sortedItems, key = { it.item.id.long }) { sorted ->
                                ItemRow(sorted.item, sorted.wishlistTitle, selectedCurrency, rates)
                            }
                        }
                    }
                }
            }
            }
        }
    }

    /**
     * Header row of one wishlist section: the wishlist title, an owner-only "Add Item" button and
     * the "Open" button, followed by a divider.
     *
     * @param section Section whose wishlist this header represents.
     * @param isOwner Whether the caller owns the displayed wishlists; gates the "Add Item" button.
     */
    @Composable
    private fun SectionHeader(section: UserWishlistsSection, isOwner: Boolean) {
        val resources = LocalResources.current
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                section.wishlist.title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isOwner) {
                    Button(onClick = { viewModel.onCreateItem(section.wishlist) }) {
                        Text(WishlistStrings.addItemButton.translation(resources))
                    }
                }
                Button(onClick = { viewModel.onWishlistSelected(section.wishlist) }) {
                    Text(WishlistStrings.openWishlistButton.translation(resources))
                }
            }
        }
        HorizontalDivider()
    }

    /**
     * Renders a single item row reusing the shared [ListRow].
     *
     * @param item Item to display.
     * @param wishlistTitle When non-null (custom sorting active), appended after the item title in
     * brackets so the originating wishlist stays visible without the grouping headers.
     * @param selectedCurrency Shared conversion target, or `null` for original prices.
     * @param rates Latest rates snapshot used to convert the price, or `null` when unavailable.
     */
    @Composable
    private fun ItemRow(
        item: RegisteredWishlistItem,
        wishlistTitle: String?,
        selectedCurrency: CurrencyCode?,
        rates: CurrencyRates?
    ) {
        ListRow(
            onSelect = { viewModel.onItemSelected(item) },
            leading = {
                val avatarModifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                val firstImage = item.imageIds.firstOrNull()
                if (firstImage != null) {
                    RemoteImage(
                        key = firstImage.string,
                        loader = { viewModel.loadImageBytes(firstImage) },
                        contentDescription = null,
                        modifier = avatarModifier
                    )
                } else {
                    WishlistItemImagePlaceholder(modifier = avatarModifier)
                }
            }
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(wishlistTitle?.let { "${item.title} ($it)" } ?: item.title)
                        PriorityBadge(item.priority)
                    }
                    if (item.approximatePrice != null) {
                        Text(
                            formatItemPrice(item.approximatePrice, item.priceUnits, selectedCurrency, rates),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                if (item.description.isNotBlank()) {
                    Text(item.description, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }

    /**
     * Renders a 2-column card grid of items as chunked rows so it can be embedded inside the
     * vertically-scrolling section column without nesting same-axis lazy scrollers.
     *
     * @param entries Items paired with the title of the wishlist each one belongs to (used as the
     * card subtitle).
     */
    @Composable
    private fun ItemCardsGrid(entries: List<Pair<RegisteredWishlistItem, String?>>) {
        val columns = 2
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            entries.chunked(columns).forEach { rowEntries ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowEntries.forEach { (item, wishlistTitle) ->
                        Box(modifier = Modifier.weight(1f)) {
                            WishlistItemCard(
                                item = item,
                                wishlistTitle = wishlistTitle,
                                loadImageBytes = { viewModel.loadImageBytes(it) },
                                onSelect = { viewModel.onItemSelected(item) }
                            )
                        }
                    }
                    repeat(columns - rowEntries.size) {
                        Box(modifier = Modifier.weight(1f)) {}
                    }
                }
            }
        }
    }
}
