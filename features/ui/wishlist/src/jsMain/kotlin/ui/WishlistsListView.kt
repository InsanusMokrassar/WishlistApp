package dev.inmo.wishlist.features.ui.wishlist.ui

import dev.inmo.wishlist.features.common.client.ui.CalmStudioStyleSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dev.inmo.micro_utils.strings.translation
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.mvvm.compose.ComposeView
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.common.client.ui.components.CalmIcon
import dev.inmo.wishlist.features.common.client.ui.components.CalmIcons
import dev.inmo.wishlist.features.common.client.ui.components.tintClass
import dev.inmo.wishlist.features.ui.topBar.ui.TopBarTitleProvider
import dev.inmo.wishlist.features.ui.wishlist.WishlistStrings
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.H3
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Text
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

/**
 * JS Compose-HTML view for the wishlists list screen (Calm Studio "My Lists" / a user's profile).
 *
 * Renders a `.listgrid` of `.listcard`s — each a deterministic gradient cover over the list title —
 * inside the standard `.content-inner` + `.pagehead` shell. Owners get a primary "New Wishlist" action
 * and a trailing dashed "new" card; visitors get the "All items" and "Profile" affordances. Class names
 * mirror the design skill's `app.jsx` so the Calm Studio shell CSS styles the screen directly.
 */
class WishlistsListView(
    chain: NavigationChain<ViewConfig>,
    config: WishlistsListViewConfig,
) : ComposeView<WishlistsListViewConfig, ViewConfig, WishlistsListViewModel>(config, chain), TopBarTitleProvider {
    override val viewModel: WishlistsListViewModel by inject(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
        parametersOf(this@WishlistsListView)
    }

    override val title: String
        @Composable get() {
            val userName by viewModel.userNameState.collectAsState()
            return userName?.let {
                WishlistStrings.userWishlistsTitleFormat.translation().replace("{name}", it)
            } ?: WishlistStrings.wishlistsTitle.translation()
        }

    @Composable
    override fun onDraw() {
        super.onDraw()
        val wishlists by viewModel.wishlistsState.collectAsState()
        val loading by viewModel.loadingState.collectAsState()
        val userName by viewModel.userNameState.collectAsState()
        val profileUserId by viewModel.profileUserIdState.collectAsState()
        val isOwner by viewModel.isOwnerState.collectAsState()

        Div({ classes(CalmStudioStyleSheet.`content-inner`) }) {
            Div({ classes(CalmStudioStyleSheet.pagehead) }) {
                Div {
                    H1 {
                        Text(
                            userName?.let { WishlistStrings.userWishlistsTitleFormat.translation().replace("{name}", it) }
                                ?: WishlistStrings.wishlistsTitle.translation()
                        )
                    }
                }
                Div({ classes(CalmStudioStyleSheet.acts) }) {
                    if (viewModel.targetUserId != null) {
                        Button({
                            classes(CalmStudioStyleSheet.btn)
                            onClick { viewModel.onShowUserWishlists() }
                        }) { Text(WishlistStrings.allItemsButton.translation()) }
                    }
                    if (profileUserId != null) {
                        Button({
                            classes(CalmStudioStyleSheet.btn)
                            onClick { viewModel.onShowProfile() }
                        }) { Text(WishlistStrings.profileButton.translation()) }
                    }
                    CreateWishlistButton(isOwner) { viewModel.onCreateWishlist() }
                }
            }

            when {
                loading -> P({ classes(CalmStudioStyleSheet.subline) }) { Text(WishlistStrings.loading.translation()) }
                wishlists.isEmpty() -> Div({ classes("empty") }) {
                    Div({ classes(CalmStudioStyleSheet.ic) }) { CalmIcon(CalmIcons.gift) }
                    H3 { Text(WishlistStrings.emptyWishlists.translation()) }
                    if (isOwner) {
                        Button({
                            classes(CalmStudioStyleSheet.btn, CalmStudioStyleSheet.primary)
                            onClick { viewModel.onCreateWishlist() }
                        }) {
                            CalmIcon(CalmIcons.plus)
                            Text(WishlistStrings.createWishlistButton.translation())
                        }
                    }
                }
                else -> Div({ classes(CalmStudioStyleSheet.listgrid) }) {
                    wishlists.forEach { wishlist ->
                        Div({
                            classes(CalmStudioStyleSheet.listcard)
                            onClick { viewModel.onWishlistSelected(wishlist.id) }
                        }) {
                            Div({ classes(CalmStudioStyleSheet.cover, tintClass(wishlist.id.long)) })
                            Div({ classes(CalmStudioStyleSheet.c) }) {
                                H3 { Text(wishlist.title) }
                            }
                        }
                    }
                    if (isOwner) {
                        Div({
                            classes(CalmStudioStyleSheet.listcard, CalmStudioStyleSheet.new)
                            onClick { viewModel.onCreateWishlist() }
                        }) {
                            CalmIcon(CalmIcons.plus)
                            Text(WishlistStrings.createWishlistButton.translation())
                        }
                    }
                }
            }
        }
    }
}
