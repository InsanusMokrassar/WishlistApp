package dev.inmo.wishlist.features.ui.wishlist.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import dev.inmo.wishlist.features.common.client.ui.components.ListRow
import dev.inmo.wishlist.features.common.client.ui.components.ScreenTitle
import dev.inmo.wishlist.features.ui.wishlist.WishlistStrings
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

/** JVM Compose-Desktop list of every item across a user's wishlists. Uses the shared [ListRow]. */
class UserWishlistsView(
    chain: NavigationChain<ViewConfig>,
    config: UserWishlistsViewConfig,
) : ComposeView<UserWishlistsViewConfig, ViewConfig, UserWishlistsViewModel>(config, chain) {
    override val viewModel: UserWishlistsViewModel by inject(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
        parametersOf(this@UserWishlistsView)
    }

    @Composable
    override fun onDraw() {
        super.onDraw()
        val items by viewModel.itemsState.collectAsState()
        val loading by viewModel.loadingState.collectAsState()

        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BackButton(WishlistStrings.backButton.translation()) { viewModel.onBack() }
                ScreenTitle(
                    WishlistStrings.allItemsTitle.translation(),
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                )
            }

            if (loading) {
                Text(WishlistStrings.loading.translation())
            } else if (items.isEmpty()) {
                Text(WishlistStrings.emptyItems.translation(), style = MaterialTheme.typography.caption)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(items) { item ->
                        ListRow(onSelect = { viewModel.onItemSelected(item) }) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(item.title)
                                    item.approximatePrice?.let { price ->
                                        Text(
                                            "$price ${item.priceUnits}",
                                            style = MaterialTheme.typography.caption
                                        )
                                    }
                                }
                                if (item.description.isNotBlank()) {
                                    Text(item.description, style = MaterialTheme.typography.caption)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
