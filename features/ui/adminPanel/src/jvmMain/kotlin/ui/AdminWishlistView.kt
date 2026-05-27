package dev.inmo.wishlist.features.ui.adminPanel.ui

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
import dev.inmo.wishlist.features.ui.adminPanel.AdminPanelStrings
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

/** JVM Compose-Desktop view for the admin wishlist detail screen. Shows items inline. */
class AdminWishlistView(
    chain: NavigationChain<ViewConfig>,
    config: AdminWishlistViewConfig,
) : ComposeView<AdminWishlistViewConfig, ViewConfig, AdminWishlistViewModel>(config, chain) {
    override val viewModel: AdminWishlistViewModel by inject(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
        parametersOf(this@AdminWishlistView)
    }

    @Composable
    override fun onDraw() {
        super.onDraw()
        val wishlist by viewModel.wishlistState.collectAsState()
        val items by viewModel.itemsState.collectAsState()
        val loading by viewModel.loadingState.collectAsState()

        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { viewModel.onBack() }) { Text(AdminPanelStrings.backButton.translation()) }
                    Column {
                        Text(wishlist?.title ?: "#${config.wishlistId.long}", style = MaterialTheme.typography.h5)
                        if (wishlist != null) {
                            Text("user #${wishlist!!.userId.long}", style = MaterialTheme.typography.caption)
                        }
                    }
                }
                Button(onClick = { viewModel.onEditWishlist() }) { Text(AdminPanelStrings.editButton.translation()) }
            }
            Spacer(modifier = Modifier.height(16.dp))
            if (loading) {
                CircularProgressIndicator()
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(AdminPanelStrings.itemsSection.translation(), style = MaterialTheme.typography.h6)
                    Button(onClick = { viewModel.onAddItem() }) { Text(AdminPanelStrings.addItemButton.translation()) }
                }
                Spacer(modifier = Modifier.height(8.dp))
                if (items.isEmpty()) {
                    Text(AdminPanelStrings.emptyItems.translation())
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(items) { item ->
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.title)
                                        if (item.approximatePrice != null) {
                                            Text(
                                                "${item.approximatePrice} ${item.priceUnits}".trim(),
                                                style = MaterialTheme.typography.caption
                                            )
                                        }
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Button(onClick = { viewModel.onEditItem(item.id) }) {
                                            Text(AdminPanelStrings.editButton.translation())
                                        }
                                        Button(onClick = { viewModel.onDeleteItem(item.id) }) {
                                            Text(AdminPanelStrings.deleteButton.translation())
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
