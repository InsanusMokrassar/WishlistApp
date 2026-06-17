package dev.inmo.wishlist.features.ui.adminPanel.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dev.inmo.micro_utils.strings.translation
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.mvvm.compose.ComposeView
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.common.client.ui.components.ListRow
import dev.inmo.wishlist.features.ui.topBar.ui.TopBarTitleProvider
import dev.inmo.wishlist.features.ui.adminPanel.AdminPanelStrings
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.P
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

        Div({ classes("content-inner") }) {
            Div({ classes("pagehead") }) {
                Div {
                    H1 { Text(AdminPanelStrings.wishlistsListTitle.translation()) }
                }
                Div({ classes("acts") }) {
                    Button({
                        classes("btn", "primary")
                        onClick { viewModel.onCreateWishlist() }
                    }) {
                        Text(AdminPanelStrings.addWishlistButton.translation())
                    }
                }
            }
            when {
                loading -> P({ classes("subline") }) { Text(AdminPanelStrings.loading.translation()) }
                wishlists.isEmpty() -> P({ classes("subline") }) { Text(AdminPanelStrings.emptyWishlists.translation()) }
                else -> Div({ classes("rows") }) {
                    wishlists.forEach { wishlist ->
                        ListRow(onSelect = { viewModel.onWishlistSelected(wishlist.id) }) {
                            Span { Text(wishlist.title) }
                            Span({ classes("pill") }) {
                                Text("user #${wishlist.userId.long}")
                            }
                        }
                    }
                }
            }
        }
    }
}
