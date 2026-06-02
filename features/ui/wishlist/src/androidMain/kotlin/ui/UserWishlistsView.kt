package dev.inmo.wishlist.features.ui.wishlist.ui

import androidx.compose.foundation.background
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
                BackButton(WishlistStrings.backButton.translation(resources)) { viewModel.onBack() }
                Button(onClick = { viewModel.onOpenProfile() }) {
                    Text(WishlistStrings.profileButton.translation(resources))
                }
            }

            if (loading) {
                Text(WishlistStrings.loading.translation(resources))
            } else if (sections.isEmpty()) {
                Text(WishlistStrings.emptyItems.translation(resources), style = MaterialTheme.typography.bodySmall)
            } else {
                WishlistSortSelector(
                    selected = sortMode,
                    onSortModeSelected = viewModel::onSortModeSelected
                )

                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (sortMode == WishlistSortMode.None) {
                        sections.forEach { section ->
                            item(key = "header-${section.wishlist.id.long}") {
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
                                    Button(onClick = { viewModel.onWishlistSelected(section.wishlist) }) {
                                        Text(WishlistStrings.openWishlistButton.translation(resources))
                                    }
                                }
                                HorizontalDivider()
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
                            MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
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
                    item.approximatePrice?.let { price ->
                        Text(
                            "$price ${item.priceUnits}",
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
}
