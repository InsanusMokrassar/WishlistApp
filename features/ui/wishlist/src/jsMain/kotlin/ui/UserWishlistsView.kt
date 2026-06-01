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
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H6
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.dom.Ul
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

/** JS Compose-HTML list of every item across a user's wishlists. Uses the shared Bootstrap [ListRow]. */
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
        val sections by viewModel.sectionsState.collectAsState()
        val userName by viewModel.userNameState.collectAsState()
        val loading by viewModel.loadingState.collectAsState()

        Div({ classes("container", "py-3") }) {
            Div({ classes("d-flex", "align-items-center", "mb-3", "gap-2") }) {
                BackButton(WishlistStrings.backButton.translation()) { viewModel.onBack() }
                ScreenTitle(
                    userName?.let {
                        WishlistStrings.userWishesTitleFormat.translation().replace("{name}", it)
                    } ?: WishlistStrings.allItemsTitle.translation(),
                    "mb-0", "flex-grow-1"
                )
            }

            if (loading) {
                P { Text(WishlistStrings.loading.translation()) }
            } else if (sections.isEmpty()) {
                P({ classes("text-muted") }) { Text(WishlistStrings.emptyItems.translation()) }
            } else {
                sections.forEach { section ->
                    H6({ classes("mt-3", "mb-1", "text-muted", "border-bottom", "pb-1") }) {
                        Text(section.wishlist.title)
                    }
                    Ul({ classes("list-group") }) {
                        section.items.forEach { item ->
                            ListRow(onSelect = { viewModel.onItemSelected(item) }) {
                                Div({ classes("flex-grow-1") }) {
                                    Div({ classes("d-flex", "justify-content-between", "align-items-center") }) {
                                        Span { Text(item.title) }
                                        item.approximatePrice?.let { price ->
                                            Span({ classes("text-muted", "small") }) {
                                                Text("$price ${item.priceUnits}")
                                            }
                                        }
                                    }
                                    if (item.description.isNotBlank()) {
                                        P({ classes("mb-0", "text-muted", "small", "mt-1") }) {
                                            Text(item.description)
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
