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

        Div({ classes("content-inner") }) {
            Div({ classes("pagehead") }) {
                Div {
                    H1 { Text(wishlist?.title ?: "#${config.wishlistId.long}") }
                    if (wishlist != null) {
                        P({ classes("subline") }) { Text("user #${wishlist!!.userId.long}") }
                    }
                }
                Div({ classes("acts") }) {
                    BackButton(AdminPanelStrings.backButton.translation()) { viewModel.onBack() }
                    Button({
                        classes("btn", "primary")
                        onClick { viewModel.onEditWishlist() }
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
                    H2 { Text(AdminPanelStrings.itemsSection.translation()) }
                    Button({
                        classes("btn")
                        onClick { viewModel.onAddItem() }
                    }) { Text(AdminPanelStrings.addItemButton.translation()) }
                }
                if (items.isEmpty()) {
                    P({ classes("subline") }) { Text(AdminPanelStrings.emptyItems.translation()) }
                } else {
                    Div({ classes("rows") }) {
                        items.forEach { item ->
                            ListRow(
                                trailing = {
                                    Div({
                                        style {
                                            property("display", "flex")
                                            property("gap", "8px")
                                        }
                                    }) {
                                        Button({
                                            classes("btn", "sm")
                                            onClick { viewModel.onEditItem(item.id) }
                                        }) { Text(AdminPanelStrings.editButton.translation()) }
                                        Button({
                                            classes("btn", "danger", "sm")
                                            onClick { viewModel.onDeleteItem(item.id) }
                                        }) { Text(AdminPanelStrings.deleteButton.translation()) }
                                    }
                                }
                            ) {
                                Span { Text(item.title) }
                                if (item.approximatePrice != null) {
                                    Span({ classes("pill") }) {
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
