package dev.inmo.wishlist.features.ui.wishlist.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.unit.dp
import dev.inmo.micro_utils.strings.translation
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.mvvm.compose.ComposeView
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.common.client.ui.components.BackButton
import dev.inmo.wishlist.features.common.client.ui.components.ListRow
import dev.inmo.wishlist.features.currency.common.utils.formatItemPrice
import dev.inmo.wishlist.features.ui.topBar.ui.TopBarTitleProvider
import dev.inmo.wishlist.features.ui.wishlist.WishlistStrings
import dev.inmo.wishlist.features.ui.wishlist.ui.WishlistViewConfig
import dev.inmo.wishlist.features.ui.wishlist.ui.WishlistViewModel
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

/** Android Compose-Material3 view for the wishlist detail screen. */
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
        val resources = LocalResources.current
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

        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BackButton(WishlistStrings.backButton.translation(resources)) { viewModel.onBack() }
                Spacer(modifier = Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (canCopy) {
                        Button(onClick = { viewModel.onCopyWishlist() }, enabled = !copyRequested) {
                            Text(WishlistStrings.copyWishlistButton.translation(resources))
                        }
                    }
                    if (isOwner) {
                        Button(onClick = { viewModel.onEditWishlist() }) {
                            Text(WishlistStrings.editButton.translation(resources))
                        }
                    }
                }
            }
            if (copyRequested) {
                Text(WishlistStrings.copyQueued.translation(resources), style = MaterialTheme.typography.bodySmall)
            }
            if (copyFailed) {
                Text(WishlistStrings.copyFailed.translation(resources), style = MaterialTheme.typography.bodySmall)
            }
            Spacer(modifier = Modifier.height(8.dp))

            when {
                loading -> CircularProgressIndicator()
                items.isEmpty() -> Text(WishlistStrings.emptyItems.translation(resources))
                else -> {
                if (sortSelectorVisible) {
                    WishlistSortSelector(
                        selected = sortMode,
                        onSortModeSelected = viewModel::onSortModeSelected,
                        noneLabel = WishlistStrings.sortDefault,
                        availableModes = sortModesFor(costSortAvailable)
                    )
                }
                if (currencyEnabled && currencies.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
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
                Spacer(modifier = Modifier.height(8.dp))
                if (viewMode == WishlistViewMode.Grid) {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 160.dp),
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        gridItems(sortedItems, key = { it.id.long }) { item ->
                            WishlistItemCard(
                                item = item,
                                wishlistTitle = wishlist?.title,
                                loadImageBytes = { viewModel.loadImageBytes(it) },
                                onSelect = { viewModel.onViewItem(item.id) }
                            )
                        }
                    }
                } else {
                    LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(sortedItems) { item ->
                            ListRow(onSelect = { viewModel.onViewItem(item.id) }) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(item.title, style = MaterialTheme.typography.bodyLarge)
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
                                        Spacer(Modifier.height(4.dp))
                                        Text(item.description, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                }
                }
            }

            if (isOwner) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { viewModel.onAddItem() }, modifier = Modifier.fillMaxWidth()) {
                    Text(WishlistStrings.addItemButton.translation(resources))
                }
            }
        }
    }
}
