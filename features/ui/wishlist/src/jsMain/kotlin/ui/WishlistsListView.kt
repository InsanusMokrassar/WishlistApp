package dev.inmo.wishlist.features.ui.wishlist.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dev.inmo.micro_utils.strings.translation
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.mvvm.compose.ComposeView
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.ui.wishlist.WishlistStrings
import dev.inmo.wishlist.features.ui.wishlist.ui.WishlistsListViewConfig
import dev.inmo.wishlist.features.ui.wishlist.ui.WishlistsListViewModel
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.Li
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.dom.Ul
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

/** JS Compose-HTML view for the wishlists list screen. Uses Bootstrap classes. */
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

        Div({ classes("container", "py-3") }) {
            Div({ classes("d-flex", "justify-content-between", "align-items-center", "mb-3") }) {
                H1({ classes("h3", "mb-0") }) {
                    Text(WishlistStrings.wishlistsTitle.translation())
                }
                Button({
                    classes("btn", "btn-primary")
                    onClick { viewModel.onCreateWishlist() }
                }) {
                    Text(WishlistStrings.createWishlistButton.translation())
                }
            }
            if (loading) {
                P { Text(WishlistStrings.loading.translation()) }
            } else if (wishlists.isEmpty()) {
                P({ classes("text-muted") }) {
                    Text(WishlistStrings.emptyWishlists.translation())
                }
            } else {
                Ul({ classes("list-group") }) {
                    wishlists.forEach { wishlist ->
                        Li({
                            classes("list-group-item", "list-group-item-action", "d-flex", "justify-content-between", "align-items-center")
                            style { property("cursor", "pointer") }
                            onClick { viewModel.onWishlistSelected(wishlist.id) }
                        }) {
                            Span { Text(wishlist.title) }
                        }
                    }
                }
            }
        }
    }
}
