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
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.dom.Ul
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

/** JS Compose-HTML view for the admin wishlists list screen. Uses Bootstrap classes. */
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

        Div({ classes("container", "py-3") }) {
            Div({ classes("d-flex", "justify-content-between", "align-items-center", "mb-3") }) {
                Button({
                    classes("btn", "btn-primary")
                    onClick { viewModel.onCreateWishlist() }
                }) {
                    Text(AdminPanelStrings.addWishlistButton.translation())
                }
            }
            when {
                loading -> P { Text(AdminPanelStrings.loading.translation()) }
                wishlists.isEmpty() -> P({ classes("text-muted") }) { Text(AdminPanelStrings.emptyWishlists.translation()) }
                else -> Ul({ classes("list-group") }) {
                    wishlists.forEach { wishlist ->
                        ListRow(onSelect = { viewModel.onWishlistSelected(wishlist.id) }) {
                            Span { Text(wishlist.title) }
                            Span({ classes("badge", "bg-secondary") }) {
                                Text("user #${wishlist.userId.long}")
                            }
                        }
                    }
                }
            }
        }
    }
}
