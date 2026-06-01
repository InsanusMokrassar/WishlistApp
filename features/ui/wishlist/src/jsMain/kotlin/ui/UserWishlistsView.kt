package dev.inmo.wishlist.features.ui.wishlist.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dev.inmo.micro_utils.strings.translation
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.mvvm.compose.ComposeView
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.common.client.ui.components.BackButton
import dev.inmo.wishlist.features.common.client.ui.components.ScreenTitle
import dev.inmo.wishlist.features.ui.wishlist.WishlistStrings
import org.jetbrains.compose.web.css.Style
import org.jetbrains.compose.web.css.StyleSheet
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H5
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Text
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

/** Custom CSS for [UserWishlistsView] — only the clickable-card affordance. */
object UserWishlistsViewStylesheet : StyleSheet() {
    /** Pointer cursor applied to each wishlist card to signal it is clickable. */
    val clickableCard by style {
        property("cursor", "pointer")
    }
}

/** JS Compose-HTML grid presentation of a user's wishlists. Uses Bootstrap card grid. */
class UserWishlistsView(
    chain: NavigationChain<ViewConfig>,
    config: UserWishlistsViewConfig,
) : ComposeView<UserWishlistsViewConfig, ViewConfig, UserWishlistsViewModel>(config, chain) {
    override val viewModel: UserWishlistsViewModel by inject(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
        parametersOf(this@UserWishlistsView)
    }

    @Composable
    override fun onDraw() {
        super.onDraw()
        Style(UserWishlistsViewStylesheet)
        val wishlists by viewModel.wishlistsState.collectAsState()
        val loading by viewModel.loadingState.collectAsState()

        Div({ classes("container", "py-3") }) {
            Div({ classes("d-flex", "align-items-center", "mb-3", "gap-2") }) {
                BackButton(WishlistStrings.backButton.translation()) { viewModel.onBack() }
                ScreenTitle(WishlistStrings.userWishlistsTitle.translation(), "mb-0", "flex-grow-1")
            }

            if (loading) {
                P { Text(WishlistStrings.loading.translation()) }
            } else if (wishlists.isEmpty()) {
                P({ classes("text-muted") }) { Text(WishlistStrings.emptyWishlists.translation()) }
            } else {
                Div({ classes("row", "row-cols-1", "row-cols-md-2", "row-cols-lg-3", "g-3") }) {
                    wishlists.forEach { wishlist ->
                        Div({ classes("col") }) {
                            Div({
                                classes("card", "h-100", "shadow-sm", UserWishlistsViewStylesheet.clickableCard)
                                onClick { viewModel.onWishlistSelected(wishlist.id) }
                            }) {
                                Div({ classes("card-body") }) {
                                    H5({ classes("card-title", "mb-0") }) { Text(wishlist.title) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
