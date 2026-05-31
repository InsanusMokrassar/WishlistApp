package dev.inmo.wishlist.features.ui.wishlist.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.mvvm.compose.ComposeView
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.common.client.ui.components.BackButton
import dev.inmo.wishlist.features.common.client.ui.components.ScreenTitle
import dev.inmo.wishlist.features.ui.wishlist.WishlistStrings
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

/** JVM Compose-Desktop read-only view for a single wishlist item. */
class WishlistItemView(
    chain: NavigationChain<ViewConfig>,
    config: WishlistItemViewConfig,
) : ComposeView<WishlistItemViewConfig, ViewConfig, WishlistItemViewModel>(config, chain) {
    override val viewModel: WishlistItemViewModel by inject(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
        parametersOf(this@WishlistItemView)
    }

    @Composable
    override fun onDraw() {
        super.onDraw()
        val item by viewModel.itemState.collectAsState()
        val loading by viewModel.loadingState.collectAsState()
        val isOwner by viewModel.isOwnerState.collectAsState()

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
                ScreenTitle(
                    item?.title ?: WishlistStrings.viewItemTitle.translation(),
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                )
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

                Text(WishlistStrings.priceLabel.translation(), style = MaterialTheme.typography.subtitle2)
                if (it.approximatePrice != null) {
                    Text("${it.approximatePrice} ${it.priceUnits}", style = MaterialTheme.typography.body1)
                } else {
                    Text(WishlistStrings.noPrice.translation(), style = MaterialTheme.typography.caption)
                }

                Text(WishlistStrings.linksLabel.translation(), style = MaterialTheme.typography.subtitle2)
                if (it.links.isEmpty()) {
                    Text(WishlistStrings.noLinks.translation(), style = MaterialTheme.typography.caption)
                } else {
                    it.links.forEach { link ->
                        Text(link, style = MaterialTheme.typography.body2)
                    }
                }
            }
        }
    }
}
