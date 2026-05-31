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
import dev.inmo.wishlist.features.common.client.ui.components.ScreenTitle
import dev.inmo.wishlist.features.ui.adminPanel.AdminPanelStrings
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H2
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Small
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.dom.Ul
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

/** JS Compose-HTML view for the admin wishlist detail screen. Shows items inline. Uses Bootstrap classes. */
class AdminWishlistView(
    chain: NavigationChain<ViewConfig>,
    config: AdminWishlistViewConfig,
) : ComposeView<AdminWishlistViewConfig, ViewConfig, AdminWishlistViewModel>(config, chain) {
    override val viewModel: AdminWishlistViewModel by inject(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
        parametersOf(this@AdminWishlistView)
    }

    @Composable
    override fun onDraw() {
        super.onDraw()
        val wishlist by viewModel.wishlistState.collectAsState()
        val items by viewModel.itemsState.collectAsState()
        val loading by viewModel.loadingState.collectAsState()

        Div({ classes("container", "py-3") }) {
            Div({ classes("d-flex", "align-items-center", "mb-3", "gap-2") }) {
                BackButton(AdminPanelStrings.backButton.translation()) { viewModel.onBack() }
                ScreenTitle(wishlist?.title ?: "#${config.wishlistId.long}")
                if (wishlist != null) {
                    Small({ classes("text-muted", "ms-2") }) {
                        Text("user #${wishlist!!.userId.long}")
                    }
                }
                Button({
                    classes("btn", "btn-outline-primary", "ms-auto")
                    onClick { viewModel.onEditWishlist() }
                }) { Text(AdminPanelStrings.editButton.translation()) }
            }

            if (loading) {
                P { Text(AdminPanelStrings.loading.translation()) }
            } else {
                Div {
                    Div({ classes("d-flex", "justify-content-between", "align-items-center", "mb-2") }) {
                        H2({ classes("h5", "mb-0") }) {
                            Text(AdminPanelStrings.itemsSection.translation())
                        }
                        Button({
                            classes("btn", "btn-sm", "btn-primary")
                            onClick { viewModel.onAddItem() }
                        }) { Text(AdminPanelStrings.addItemButton.translation()) }
                    }
                    if (items.isEmpty()) {
                        P({ classes("text-muted") }) { Text(AdminPanelStrings.emptyItems.translation()) }
                    } else {
                        Ul({ classes("list-group") }) {
                            items.forEach { item ->
                                ListRow(
                                    trailing = {
                                        Div({ classes("d-flex", "gap-2") }) {
                                            Button({
                                                classes("btn", "btn-sm", "btn-outline-secondary")
                                                onClick { viewModel.onEditItem(item.id) }
                                            }) { Text(AdminPanelStrings.editButton.translation()) }
                                            Button({
                                                classes("btn", "btn-sm", "btn-outline-danger")
                                                onClick { viewModel.onDeleteItem(item.id) }
                                            }) { Text(AdminPanelStrings.deleteButton.translation()) }
                                        }
                                    }
                                ) {
                                    Span { Text(item.title) }
                                    if (item.approximatePrice != null) {
                                        Small({ classes("text-muted", "ms-2") }) {
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
}
