package dev.inmo.wishlist.features.ui.adminPanel.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dev.inmo.micro_utils.strings.translation
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.mvvm.compose.ComposeView
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.ui.adminPanel.AdminPanelStrings
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.H2
import org.jetbrains.compose.web.dom.Li
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.dom.Ul
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

/** JS Compose-HTML view for the admin user detail screen. Displays user info and wishlists inline. */
class AdminUserView(
    chain: NavigationChain<ViewConfig>,
    config: AdminUserViewConfig,
) : ComposeView<AdminUserViewConfig, ViewConfig, AdminUserViewModel>(config, chain) {
    override val viewModel: AdminUserViewModel by inject(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
        parametersOf(this@AdminUserView)
    }

    @Composable
    override fun onDraw() {
        super.onDraw()
        val user by viewModel.userState.collectAsState()
        val wishlists by viewModel.wishlistsState.collectAsState()
        val loading by viewModel.loadingState.collectAsState()

        Div({ classes("container", "py-3") }) {
            Div({ classes("d-flex", "align-items-center", "mb-3", "gap-2") }) {
                Button({
                    classes("btn", "btn-outline-secondary")
                    onClick { viewModel.onBack() }
                }) { Text(AdminPanelStrings.backButton.translation()) }
                H1({ classes("h3", "mb-0") }) {
                    Text(user?.username?.string ?: "#${config.userId.long}")
                }
                Button({
                    classes("btn", "btn-outline-primary", "ms-auto")
                    onClick { viewModel.onEditUser() }
                }) { Text(AdminPanelStrings.editButton.translation()) }
            }

            if (loading) {
                P { Text(AdminPanelStrings.loading.translation()) }
            } else {
                Div({ classes("mb-4") }) {
                    Div({ classes("d-flex", "justify-content-between", "align-items-center", "mb-2") }) {
                        H2({ classes("h5", "mb-0") }) {
                            Text(AdminPanelStrings.userWishlistsSection.translation())
                        }
                        Button({
                            classes("btn", "btn-sm", "btn-primary")
                            onClick { viewModel.onAddWishlist() }
                        }) { Text(AdminPanelStrings.addWishlistForUserButton.translation()) }
                    }
                    if (wishlists.isEmpty()) {
                        P({ classes("text-muted") }) {
                            Text(AdminPanelStrings.noWishlistsForUser.translation())
                        }
                    } else {
                        Ul({ classes("list-group") }) {
                            wishlists.forEach { wishlist ->
                                Li({
                                    classes("list-group-item", "list-group-item-action", "d-flex", "justify-content-between", "align-items-center")
                                    style { property("cursor", "pointer") }
                                    onClick { viewModel.onOpenWishlist(wishlist.id) }
                                }) {
                                    Span { Text(wishlist.title) }
                                    Span({ classes("badge", "bg-secondary") }) { Text("#${wishlist.id.long}") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
