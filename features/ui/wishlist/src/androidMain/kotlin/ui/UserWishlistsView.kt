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
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    sections.forEach { section ->
                        item(key = "header-${section.wishlist.id.long}") {
                            Text(
                                section.wishlist.title,
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            HorizontalDivider()
                        }
                        items(section.items) { item ->
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
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(item.title)
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
                }
            }
        }
    }
}
