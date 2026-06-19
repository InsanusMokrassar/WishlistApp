package dev.inmo.wishlist.features.ui.adminPanel.ui

import dev.inmo.wishlist.features.common.client.ui.CalmStudioStyleSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dev.inmo.micro_utils.strings.translation
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.mvvm.compose.ComposeView
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.common.client.ui.components.CalmButton
import dev.inmo.wishlist.features.common.client.ui.components.CalmButtonVariant
import dev.inmo.wishlist.features.common.client.ui.components.ContentColumn
import dev.inmo.wishlist.features.common.client.ui.components.ListRow
import dev.inmo.wishlist.features.common.client.ui.components.PageHead
import dev.inmo.wishlist.features.common.client.ui.components.RowsList
import dev.inmo.wishlist.features.common.client.ui.components.Subline
import dev.inmo.wishlist.features.ui.topBar.ui.TopBarTitleProvider
import dev.inmo.wishlist.features.ui.adminPanel.AdminPanelStrings
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

/** JS Compose-HTML view for the admin wishlists list screen (Calm Studio). */
class AdminWishlistsListView(
    chain: NavigationChain<ViewConfig>,
    config: AdminWishlistsListViewConfig,
) : ComposeView<AdminWishlistsListViewConfig, ViewConfig, AdminWishlistsListViewModel>(config, chain), TopBarTitleProvider {
    override val viewModel: AdminWishlistsListViewModel by inject(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
        parametersOf(this@AdminWishlistsListView)
    }

    override val title: String
        @Composable get() = AdminPanelStrings.wishlistsListTitle.translation()

    @Composable
    override fun onDraw() {
        super.onDraw()
        val wishlists by viewModel.wishlistsState.collectAsState()
        val loading by viewModel.loadingState.collectAsState()

        ContentColumn {
            PageHead(
                title = AdminPanelStrings.wishlistsListTitle.translation(),
                actions = {
                    CalmButton(
                        text = AdminPanelStrings.addWishlistButton.translation(),
                        onClick = { viewModel.onCreateWishlist() },
                        variant = CalmButtonVariant.Primary,
                    )
                },
            )
            when {
                loading -> Subline(AdminPanelStrings.loading.translation())
                wishlists.isEmpty() -> Subline(AdminPanelStrings.emptyWishlists.translation())
                else -> RowsList {
                    wishlists.forEach { wishlist ->
                        ListRow(onSelect = { viewModel.onWishlistSelected(wishlist.id) }) {
                            Span { Text(wishlist.title) }
                            Span({ classes(CalmStudioStyleSheet.pill) }) {
                                Text("user #${wishlist.userId.long}")
                            }
                        }
                    }
                }
            }
        }
    }
}
