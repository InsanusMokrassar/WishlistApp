package dev.inmo.wishlist.features.ui.wishlist.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dev.inmo.micro_utils.strings.translation
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.mvvm.compose.ComposeView
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.common.client.ui.components.BackButton
import dev.inmo.wishlist.features.common.client.ui.components.ListRow
import dev.inmo.wishlist.features.common.client.ui.components.ScreenTitle
import dev.inmo.wishlist.features.ui.wishlist.WishlistStrings
import dev.inmo.wishlist.features.ui.wishlist.ui.WishlistsListViewConfig
import dev.inmo.wishlist.features.ui.wishlist.ui.WishlistsListViewModel
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.P
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
        val profileUserId by viewModel.profileUserIdState.collectAsState()
        val stack by chain.stackFlow.collectAsState()

        Div({ classes("container", "py-3") }) {
            Div({ classes("d-flex", "justify-content-between", "align-items-center", "mb-3") }) {
                Div({ classes("d-flex", "align-items-center", "gap-2") }) {
                    if (stack.size > 1) {
                        BackButton(WishlistStrings.backButton.translation()) { viewModel.onBack() }
                    }
                    ScreenTitle(
                        (if (viewModel.targetUserId == null) WishlistStrings.wishlistsTitle
                        else WishlistStrings.userWishlistsTitle).translation()
                    )
                }
                Div({ classes("d-flex", "gap-2") }) {
                    if (profileUserId != null) {
                        Button({
                            classes("btn", "btn-outline-secondary")
                            onClick { viewModel.onShowProfile() }
                        }) {
                            Text(WishlistStrings.profileButton.translation())
                        }
                    }
                    if (viewModel.targetUserId != null) {
                        Button({
                            classes("btn", "btn-outline-secondary")
                            onClick { viewModel.onShowGrid() }
                        }) {
                            Text(WishlistStrings.gridViewButton.translation())
                        }
                    }
                    Button({
                        classes("btn", "btn-primary")
                        onClick { viewModel.onCreateWishlist() }
                    }) {
                        Text(WishlistStrings.createWishlistButton.translation())
                    }
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
                        ListRow(
                            text = wishlist.title,
                            onSelect = { viewModel.onWishlistSelected(wishlist.id) }
                        )
                    }
                }
            }
        }
    }
}
