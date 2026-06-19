package dev.inmo.wishlist.features.ui.adminPanel.ui

import dev.inmo.wishlist.features.common.client.ui.CalmStudioStyleSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dev.inmo.micro_utils.strings.translation
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.mvvm.compose.ComposeView
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.common.client.ui.components.BackButton
import dev.inmo.wishlist.features.common.client.ui.components.CalmButton
import dev.inmo.wishlist.features.common.client.ui.components.CalmButtonVariant
import dev.inmo.wishlist.features.common.client.ui.components.ContentColumn
import dev.inmo.wishlist.features.common.client.ui.components.ListRow
import dev.inmo.wishlist.features.common.client.ui.components.PageHead
import dev.inmo.wishlist.features.common.client.ui.components.RowsList
import dev.inmo.wishlist.features.common.client.ui.components.Subline
import dev.inmo.wishlist.features.ui.topBar.ui.TopBarTitleProvider
import dev.inmo.wishlist.features.ui.adminPanel.AdminPanelStrings
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H2
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

        ContentColumn {
            PageHead(
                title = user?.username?.string ?: "#${config.userId.long}",
                actions = {
                    BackButton(AdminPanelStrings.backButton.translation()) { viewModel.onBack() }
                    CalmButton(
                        text = AdminPanelStrings.editButton.translation(),
                        onClick = { viewModel.onEditUser() },
                        variant = CalmButtonVariant.Primary,
                    )
                },
            )

            if (loading) {
                Subline(AdminPanelStrings.loading.translation())
            } else {
                Div({ classes(CalmStudioStyleSheet.sectionhead) }) {
                    H2 { Text(AdminPanelStrings.userWishlistsSection.translation()) }
                    CalmButton(
                        text = AdminPanelStrings.addWishlistForUserButton.translation(),
                        onClick = { viewModel.onAddWishlist() },
                    )
                }
                if (wishlists.isEmpty()) {
                    Subline(AdminPanelStrings.noWishlistsForUser.translation())
                } else {
                    RowsList {
                        wishlists.forEach { wishlist ->
                            ListRow(onSelect = { viewModel.onOpenWishlist(wishlist.id) }) {
                                Span { Text(wishlist.title) }
                                Span({ classes(CalmStudioStyleSheet.pill) }) { Text("#${wishlist.id.long}") }
                            }
                        }
                    }
                }
            }
        }
    }
}
