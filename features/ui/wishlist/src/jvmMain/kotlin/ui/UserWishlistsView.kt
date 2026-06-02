package dev.inmo.wishlist.features.ui.wishlist.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import dev.inmo.micro_utils.strings.translation
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.mvvm.compose.ComposeView
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.common.client.ui.components.BackButton
import dev.inmo.wishlist.features.common.client.ui.components.ListRow
import dev.inmo.wishlist.features.ui.topBar.ui.TopBarTitleProvider
import dev.inmo.wishlist.features.ui.wishlist.WishlistStrings
import dev.inmo.wishlist.features.ui.wishlist.labelResource
import dev.inmo.wishlist.features.wishlist.common.models.RegisteredWishlistItem
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

/** JVM Compose-Desktop list of every item across a user's wishlists. Uses the shared [ListRow]. */
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
                Button(onClick = { viewModel.onOpenProfile() }) {
                    Text(WishlistStrings.profileButton.translation())
                }
            }

            if (loading) {
                Text(WishlistStrings.loading.translation())
            } else if (sections.isEmpty()) {
                Text(WishlistStrings.emptyItems.translation(), style = MaterialTheme.typography.caption)
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(WishlistStrings.sortLabel.translation(), style = MaterialTheme.typography.caption)
                    WishlistSortMode.entries.forEach { mode ->
                        val active = mode == sortMode
                        Button(
                            onClick = { viewModel.onSortModeSelected(mode) },
                            colors = if (active) {
                                ButtonDefaults.buttonColors()
                            } else {
                                ButtonDefaults.outlinedButtonColors()
                            }
                        ) {
                            Text(mode.labelResource().translation())
                        }
                    }
                }

                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (sortMode == WishlistSortMode.None) {
                        sections.forEach { section ->
                            item(key = "header-${section.wishlist.id.long}") {
                                Text(
                                    section.wishlist.title,
                                    modifier = Modifier.fillMaxWidth()
                                        .clickable { viewModel.onWishlistSelected(section.wishlist) }
                                        .padding(top = 8.dp),
                                    style = MaterialTheme.typography.subtitle2,
                                    color = MaterialTheme.colors.primary
                                )
                                Divider()
                            }
                            items(section.items, key = { it.id.long }) { item ->
                                ItemRow(item, null)
                            }
                        }
                    } else {
                        items(sortedItems, key = { it.item.id.long }) { sorted ->
                            ItemRow(sorted.item, sorted.wishlistTitle)
                        }
                    }
                }
            }
        }
    }

    /**
     * Renders a single item row reusing the shared [ListRow].
     *
     * @param item Item to display.
     * @param wishlistTitle When non-null (custom sorting active), appended after the item title in
     * brackets so the originating wishlist stays visible without the grouping headers.
     */
    @Composable
    private fun ItemRow(item: RegisteredWishlistItem, wishlistTitle: String?) {
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
                    Box(
                        modifier = avatarModifier.background(
                            MaterialTheme.colors.onSurface.copy(alpha = 0.12f)
                        )
                    )
                }
            }
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(wishlistTitle?.let { "${item.title} ($it)" } ?: item.title)
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
