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
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.inmo.micro_utils.strings.translation
import dev.inmo.navigation.compose.InjectNavigationChain
import dev.inmo.navigation.compose.InjectNavigationNode
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.mvvm.compose.ComposeView
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.common.client.ui.components.BackButton
import dev.inmo.wishlist.features.currency.common.utils.formatItemPrice
import dev.inmo.wishlist.features.ui.topBar.ui.TopBarTitleProvider
import dev.inmo.wishlist.features.ui.wishlist.WishlistStrings
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

/** JVM Compose-Desktop read-only view for a single wishlist item. */
class WishlistItemView(
    chain: NavigationChain<ViewConfig>,
    config: WishlistItemViewConfig,
) : ComposeView<WishlistItemViewConfig, ViewConfig, WishlistItemViewModel>(config, chain), TopBarTitleProvider {
    override val viewModel: WishlistItemViewModel by inject(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
        parametersOf(this@WishlistItemView)
    }

    override val title: String
        @Composable get() {
            val item by viewModel.itemState.collectAsState()
            return item?.title ?: WishlistStrings.viewItemTitle.translation()
        }

    @Composable
    override fun onDraw() {
        super.onDraw()
        val item by viewModel.itemState.collectAsState()
        val loading by viewModel.loadingState.collectAsState()
        val isOwner by viewModel.isOwnerState.collectAsState()
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
                BackButton(WishlistStrings.backButton.translation()) { viewModel.onBack() }
                if (isOwner) {
                    Button(onClick = { viewModel.onEditItem() }) {
                        Text(WishlistStrings.editButton.translation())
                    }
                }
            }

            if (loading) {
                Text(WishlistStrings.loading.translation())
            } else if (item != null) {
                val it = item!!

                if (it.description.isNotBlank()) {
                    Text(WishlistStrings.descriptionLabel.translation(), style = MaterialTheme.typography.subtitle2)
                    Text(it.description, style = MaterialTheme.typography.body1)
                }

                if (it.amount != 1u) {
                    Text(WishlistStrings.amountLabel.translation(), style = MaterialTheme.typography.subtitle2)
                    Text("×${it.amount}", style = MaterialTheme.typography.body1)
                }

                if (currencyEnabled && currencies.isNotEmpty()) {
                    CurrencySelector(
                        currencies = currencies,
                        selected = selectedCurrency,
                        onCurrencySelected = viewModel::onCurrencySelected
                    )
                }

                Text(WishlistStrings.priceLabel.translation(), style = MaterialTheme.typography.subtitle2)
                if (it.approximatePrice != null) {
                    Text(
                        formatItemPrice(it.approximatePrice, it.priceUnits, selectedCurrency, rates),
                        style = MaterialTheme.typography.body1
                    )
                } else {
                    Text(WishlistStrings.noPrice.translation(), style = MaterialTheme.typography.caption)
                }

                Text(WishlistStrings.priorityLabel.translation(), style = MaterialTheme.typography.subtitle2)
                PriorityBadge(it.priority)

                // Each registered WishlistAdditionalConfigsProvider (e.g. booking) is drawn INLINE
                // here, where the hard-coded booking button used to be, by injecting a dedicated
                // navigation chain + node per provider. The provider's own compact view renders in
                // place; no button pushes a separate screen.
                viewModel.additionalConfigsProviders.forEach { provider ->
                    InjectNavigationChain<ViewConfig>(id = provider.chainId) {
                        InjectNavigationNode(provider.createConfig(it))
                    }
                }

                Text(WishlistStrings.linksLabel.translation(), style = MaterialTheme.typography.subtitle2)
                if (it.links.isEmpty()) {
                    Text(WishlistStrings.noLinks.translation(), style = MaterialTheme.typography.caption)
                } else {
                    it.links.forEach { link ->
                        Text(link, style = MaterialTheme.typography.body2)
                    }
                }

                Text(WishlistStrings.imagesLabel.translation(), style = MaterialTheme.typography.subtitle2)
                if (it.imageIds.isEmpty()) {
                    Text(WishlistStrings.noImages.translation(), style = MaterialTheme.typography.caption)
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
