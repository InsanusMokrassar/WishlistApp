package dev.inmo.wishlist.features.ui.wishlist.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
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
import dev.inmo.wishlist.features.ui.wishlist.WishlistStrings
import dev.inmo.wishlist.features.ui.wishlist.ui.WishlistViewConfig
import dev.inmo.wishlist.features.ui.wishlist.ui.WishlistViewModel
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

/** JVM Compose-Desktop view for the wishlist detail screen. */
class WishlistView(
    chain: NavigationChain<ViewConfig>,
    config: WishlistViewConfig,
) : ComposeView<WishlistViewConfig, ViewConfig, WishlistViewModel>(config, chain) {
    override val viewModel: WishlistViewModel by inject(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
        parametersOf(this@WishlistView)
    }

    @Composable
    override fun onDraw() {
        super.onDraw()
        val wishlist by viewModel.wishlistState.collectAsState()
        val items by viewModel.itemsState.collectAsState()
        val isOwner by viewModel.isOwnerState.collectAsState()
        val loading by viewModel.loadingState.collectAsState()

        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(onClick = { viewModel.onBack() }) {
                    Text(WishlistStrings.backButton.translation())
                }
                Text(wishlist?.title ?: "", style = MaterialTheme.typography.h5, modifier = Modifier.weight(1f).padding(horizontal = 8.dp))
                if (isOwner) {
                    Button(onClick = { viewModel.onEditWishlist() }) {
                        Text(WishlistStrings.editButton.translation())
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            if (loading) {
                CircularProgressIndicator()
            } else if (items.isEmpty()) {
                Text(WishlistStrings.emptyItems.translation())
            } else {
                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(items) { item ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .then(if (isOwner) Modifier.clickable { viewModel.onEditItem(item.id) } else Modifier)
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(item.title, style = MaterialTheme.typography.subtitle1)
                                    item.approximatePrice?.let { price ->
                                        Text("${price} ${item.priceUnits}", style = MaterialTheme.typography.caption)
                                    }
                                }
                                if (item.description.isNotBlank()) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(item.description, style = MaterialTheme.typography.caption)
                                }
                            }
                        }
                    }
                }
            }

            if (isOwner) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { viewModel.onAddItem() }, modifier = Modifier.fillMaxWidth()) {
                    Text(WishlistStrings.addItemButton.translation())
                }
            }
        }
    }
}
