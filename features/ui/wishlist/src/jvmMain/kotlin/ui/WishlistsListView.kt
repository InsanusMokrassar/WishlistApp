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
import dev.inmo.wishlist.features.ui.wishlist.ui.WishlistsListViewConfig
import dev.inmo.wishlist.features.ui.wishlist.ui.WishlistsListViewModel
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

/** JVM Compose-Desktop view for the wishlists list screen. */
class WishlistsListView(
    chain: NavigationChain<ViewConfig>,
    config: WishlistsListViewConfig,
) : ComposeView<WishlistsListViewConfig, ViewConfig, WishlistsListViewModel>(config, chain) {
    override val viewModel: WishlistsListViewModel by inject(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
        parametersOf(this@WishlistsListView)
    }

    @Composable
    override fun onDraw() {
        super.onDraw()
        val wishlists by viewModel.wishlistsState.collectAsState()
        val loading by viewModel.loadingState.collectAsState()

        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(WishlistStrings.wishlistsTitle.translation(), style = MaterialTheme.typography.h5)
                Button(onClick = { viewModel.onCreateWishlist() }) {
                    Text(WishlistStrings.createWishlistButton.translation())
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (loading) {
                CircularProgressIndicator()
            } else if (wishlists.isEmpty()) {
                Text(WishlistStrings.emptyWishlists.translation())
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(wishlists) { wishlist ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = wishlist.title,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.onWishlistSelected(wishlist.id) }
                                    .padding(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
