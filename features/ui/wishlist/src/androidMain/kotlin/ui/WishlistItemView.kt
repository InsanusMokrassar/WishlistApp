package dev.inmo.wishlist.features.ui.wishlist.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
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
import dev.inmo.wishlist.features.currency.common.utils.formatItemPrice
import dev.inmo.wishlist.features.ui.topBar.ui.TopBarTitleProvider
import dev.inmo.wishlist.features.ui.wishlist.WishlistStrings
import dev.inmo.wishlist.features.ui.wishlist.ui.WishlistItemViewConfig
import dev.inmo.wishlist.features.ui.wishlist.ui.WishlistItemViewModel
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

/** Android Compose-Material3 read-only view for a single wishlist item. */
class WishlistItemView(
    chain: NavigationChain<ViewConfig>,
    config: WishlistItemViewConfig,
) : ComposeView<WishlistItemViewConfig, ViewConfig, WishlistItemViewModel>(config, chain), TopBarTitleProvider {
    override val viewModel: WishlistItemViewModel by inject(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
        parametersOf(this@WishlistItemView)
    }

    override val title: String
        @Composable get() {
            val resources = LocalResources.current
            val item by viewModel.itemState.collectAsState()
            return item?.title ?: WishlistStrings.viewItemTitle.translation(resources)
        }

    @Composable
    override fun onDraw() {
        super.onDraw()
        val resources = LocalResources.current
        val item by viewModel.itemState.collectAsState()
        val loading by viewModel.loadingState.collectAsState()
        val isOwner by viewModel.isOwnerState.collectAsState()
        val canCopy by viewModel.canCopyState.collectAsState()
        val currencyEnabled by viewModel.currencyEnabledState.collectAsState()
        val currencies by viewModel.currenciesState.collectAsState()
        val selectedCurrency by viewModel.selectedCurrencyState.collectAsState()
        val rates by viewModel.ratesState.collectAsState()

        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BackButton(WishlistStrings.backButton.translation(resources)) { viewModel.onBack() }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (canCopy) {
                        Button(onClick = { viewModel.onCopyItem() }) {
                            Text(WishlistStrings.copyItemButton.translation(resources))
                        }
                    }
                    if (isOwner) {
                        Button(onClick = { viewModel.onEditItem() }) {
                            Text(WishlistStrings.editButton.translation(resources))
                        }
                    }
                }
            }

            if (loading) {
                Text(WishlistStrings.loading.translation(resources))
            } else if (item != null) {
                val it = item!!

                if (it.description.isNotBlank()) {
                    Text(WishlistStrings.descriptionLabel.translation(resources), style = MaterialTheme.typography.titleSmall)
                    Text(it.description, style = MaterialTheme.typography.bodyMedium)
                }

                if (it.amount != 1u) {
                    Text(WishlistStrings.amountLabel.translation(resources), style = MaterialTheme.typography.titleSmall)
                    Text("×${it.amount}", style = MaterialTheme.typography.bodyMedium)
                }

                if (currencyEnabled && currencies.isNotEmpty()) {
                    CurrencySelector(
                        currencies = currencies,
                        selected = selectedCurrency,
                        onCurrencySelected = viewModel::onCurrencySelected
                    )
                }

                Text(WishlistStrings.priceLabel.translation(resources), style = MaterialTheme.typography.titleSmall)
                if (it.approximatePrice != null) {
                    Text(
                        formatItemPrice(it.approximatePrice, it.priceUnits, selectedCurrency, rates),
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    Text(WishlistStrings.noPrice.translation(resources), style = MaterialTheme.typography.bodySmall)
                }

                Text(WishlistStrings.priorityLabel.translation(resources), style = MaterialTheme.typography.titleSmall)
                PriorityBadge(it.priority)

                Text(WishlistStrings.linksLabel.translation(resources), style = MaterialTheme.typography.titleSmall)
                if (it.links.isEmpty()) {
                    Text(WishlistStrings.noLinks.translation(resources), style = MaterialTheme.typography.bodySmall)
                } else {
                    it.links.forEach { link ->
                        Text(link, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                Text(WishlistStrings.imagesLabel.translation(resources), style = MaterialTheme.typography.titleSmall)
                if (it.imageIds.isEmpty()) {
                    Text(WishlistStrings.noImages.translation(resources), style = MaterialTheme.typography.bodySmall)
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        it.imageIds.forEach { id ->
                            RemoteImage(
                                key = id.string,
                                loader = { viewModel.loadImageBytes(id) },
                                contentDescription = null,
                                modifier = Modifier.size(160.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
