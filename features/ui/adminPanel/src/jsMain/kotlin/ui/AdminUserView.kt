package dev.inmo.wishlist.features.ui.adminPanel.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dev.inmo.micro_utils.strings.translation
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.mvvm.compose.ComposeView
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.common.client.ui.components.BackButton
import dev.inmo.wishlist.features.common.client.ui.components.ListRow
import dev.inmo.wishlist.features.ui.topBar.ui.TopBarTitleProvider
import dev.inmo.wishlist.features.ui.adminPanel.AdminPanelStrings
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.H2
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

/** JS Compose-HTML view for the admin user detail screen (Calm Studio). Displays user info and wishlists inline. */
class AdminUserView(
    chain: NavigationChain<ViewConfig>,
    config: AdminUserViewConfig,
) : ComposeView<AdminUserViewConfig, ViewConfig, AdminUserViewModel>(config, chain), TopBarTitleProvider {
    override val viewModel: AdminUserViewModel by inject(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
        parametersOf(this@AdminUserView)
    }

    override val title: String
        @Composable get() {
            val user by viewModel.userState.collectAsState()
            return user?.username?.string ?: "#${config.userId.long}"
        }

    @Composable
    override fun onDraw() {
        super.onDraw()
        val user by viewModel.userState.collectAsState()
        val wishlists by viewModel.wishlistsState.collectAsState()
        val loading by viewModel.loadingState.collectAsState()

        Div({ classes("content-inner") }) {
            Div({ classes("pagehead") }) {
                Div {
                    H1 { Text(user?.username?.string ?: "#${config.userId.long}") }
                }
                Div({ classes("acts") }) {
                    BackButton(AdminPanelStrings.backButton.translation()) { viewModel.onBack() }
                    Button({
                        classes("btn", "primary")
                        onClick { viewModel.onEditUser() }
                    }) { Text(AdminPanelStrings.editButton.translation()) }
                }
            }

            if (loading) {
                P({ classes("subline") }) { Text(AdminPanelStrings.loading.translation()) }
            } else {
                Div({
                    style {
                        property("display", "flex")
                        property("justify-content", "space-between")
                        property("align-items", "center")
                        property("margin", "18px 0 12px")
                    }
                }) {
                    H2 { Text(AdminPanelStrings.userWishlistsSection.translation()) }
                    Button({
                        classes("btn")
                        onClick { viewModel.onAddWishlist() }
                    }) { Text(AdminPanelStrings.addWishlistForUserButton.translation()) }
                }
                if (wishlists.isEmpty()) {
                    P({ classes("subline") }) {
                        Text(AdminPanelStrings.noWishlistsForUser.translation())
                    }
                } else {
                    Div({ classes("rows") }) {
                        wishlists.forEach { wishlist ->
                            ListRow(onSelect = { viewModel.onOpenWishlist(wishlist.id) }) {
                                Span { Text(wishlist.title) }
                                Span({ classes("pill") }) { Text("#${wishlist.id.long}") }
                            }
                        }
                    }
                }
            }
        }
    }
}
