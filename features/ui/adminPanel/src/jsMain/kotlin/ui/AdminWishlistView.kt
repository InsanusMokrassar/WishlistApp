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
import dev.inmo.wishlist.features.common.client.ui.components.CalmButtonSize
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

/** JS Compose-HTML view for the admin wishlist detail screen (Calm Studio). Shows items inline. */
class AdminWishlistView(
    chain: NavigationChain<ViewConfig>,
    config: AdminWishlistViewConfig,
) : ComposeView<AdminWishlistViewConfig, ViewConfig, AdminWishlistViewModel>(config, chain), TopBarTitleProvider {
    override val viewModel: AdminWishlistViewModel by inject(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
        parametersOf(this@AdminWishlistView)
    }

    override val title: String
        @Composable get() {
            val wishlist by viewModel.wishlistState.collectAsState()
            return wishlist?.title ?: "#${config.wishlistId.long}"
        }

    @Composable
    override fun onDraw() {
        super.onDraw()
        val wishlist by viewModel.wishlistState.collectAsState()
        val items by viewModel.itemsState.collectAsState()
        val loading by viewModel.loadingState.collectAsState()

        ContentColumn {
            PageHead(
                title = wishlist?.title ?: "#${config.wishlistId.long}",
                subline = wishlist?.let { "user #${it.userId.long}" },
                actions = {
                    BackButton(AdminPanelStrings.backButton.translation()) { viewModel.onBack() }
                    CalmButton(
                        text = AdminPanelStrings.editButton.translation(),
                        onClick = { viewModel.onEditWishlist() },
                        variant = CalmButtonVariant.Primary,
                    )
                },
            )

            if (loading) {
                Subline(AdminPanelStrings.loading.translation())
            } else {
                Div({ classes(CalmStudioStyleSheet.sectionhead) }) {
                    H2 { Text(AdminPanelStrings.itemsSection.translation()) }
                    CalmButton(
                        text = AdminPanelStrings.addItemButton.translation(),
                        onClick = { viewModel.onAddItem() },
                    )
                }
                if (items.isEmpty()) {
                    Subline(AdminPanelStrings.emptyItems.translation())
                } else {
                    RowsList {
                        items.forEach { item ->
                            ListRow(
                                trailing = {
                                    Div({ classes(CalmStudioStyleSheet.hstack) }) {
                                        CalmButton(
                                            text = AdminPanelStrings.editButton.translation(),
                                            onClick = { viewModel.onEditItem(item.id) },
                                            size = CalmButtonSize.Small,
                                        )
                                        CalmButton(
                                            text = AdminPanelStrings.deleteButton.translation(),
                                            onClick = { viewModel.onDeleteItem(item.id) },
                                            variant = CalmButtonVariant.Danger,
                                            size = CalmButtonSize.Small,
                                        )
                                    }
                                }
                            ) {
                                Span { Text(item.title) }
                                if (item.approximatePrice != null) {
                                    Span({ classes(CalmStudioStyleSheet.pill) }) {
                                        Text("${item.approximatePrice} ${item.priceUnits}".trim())
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
