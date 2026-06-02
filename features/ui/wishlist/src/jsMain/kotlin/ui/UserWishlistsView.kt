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
import dev.inmo.wishlist.features.ui.topBar.ui.TopBarTitleProvider
import dev.inmo.wishlist.features.ui.wishlist.WishlistStrings
import dev.inmo.wishlist.features.ui.wishlist.labelResource
import org.jetbrains.compose.web.css.height
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H6
import org.jetbrains.compose.web.dom.Img
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
) : ComposeView<UserWishlistsViewConfig, ViewConfig, UserWishlistsViewModel>(config, chain), TopBarTitleProvider {
    override val viewModel: UserWishlistsViewModel by inject(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
        parametersOf(this@UserWishlistsView)
    }

    override val title: String
        @Composable get() {
            val userName by viewModel.userNameState.collectAsState()
            return userName?.let {
                WishlistStrings.userWishesTitleFormat.translation().replace("{name}", it)
            } ?: WishlistStrings.allItemsTitle.translation()
        }

    @Composable
    override fun onDraw() {
        super.onDraw()
        val sections by viewModel.sectionsState.collectAsState()
        val sortMode by viewModel.sortModeState.collectAsState()
        val sortedItems by viewModel.sortedItemsState.collectAsState()
        val loading by viewModel.loadingState.collectAsState()

        Div({ classes("container", "py-3") }) {
            Div({ classes("d-flex", "align-items-center", "mb-3", "gap-2") }) {
                BackButton(WishlistStrings.backButton.translation()) { viewModel.onBack() }
                Button({
                    classes("btn", "btn-outline-primary", "ms-auto")
                    onClick { viewModel.onOpenProfile() }
                }) {
                    Text(WishlistStrings.profileButton.translation())
                }
            }

            if (loading) {
                P { Text(WishlistStrings.loading.translation()) }
            } else if (sections.isEmpty()) {
                P({ classes("text-muted") }) { Text(WishlistStrings.emptyItems.translation()) }
            } else {
                Div({ classes("d-flex", "align-items-center", "mb-3", "gap-2", "flex-wrap") }) {
                    Span({ classes("text-muted", "small") }) { Text(WishlistStrings.sortLabel.translation()) }
                    Div({ classes("btn-group", "btn-group-sm") }) {
                        WishlistSortMode.entries.forEach { mode ->
                            val active = mode == sortMode
                            Button({
                                classes("btn", if (active) "btn-primary" else "btn-outline-primary")
                                onClick { viewModel.onSortModeSelected(mode) }
                            }) {
                                Text(mode.labelResource().translation())
                            }
                        }
                    }
                }

                if (sortMode == WishlistSortMode.None) {
                    sections.forEach { section ->
                        Div({
                            classes("d-flex", "align-items-center", "justify-content-between", "mt-3", "mb-1", "border-bottom", "pb-1")
                        }) {
                            H6({ classes("mb-0", "text-muted") }) { Text(section.wishlist.title) }
                            Button({
                                classes("btn", "btn-sm", "btn-outline-primary")
                                onClick { viewModel.onWishlistSelected(section.wishlist) }
                            }) {
                                Text(WishlistStrings.openWishlistButton.translation())
                            }
                        }
                        Ul({ classes("list-group") }) {
                            section.items.forEach { item -> ItemRow(item, null) }
                        }
                    }
                } else {
                    Ul({ classes("list-group") }) {
                        sortedItems.forEach { sorted -> ItemRow(sorted.item, sorted.wishlistTitle) }
                    }
                }
            }
        }
    }

    /**
     * Renders a single item row reusing the shared Bootstrap [ListRow].
     *
     * @param item Item to display.
     * @param wishlistTitle When non-null (custom sorting active), appended after the item title in
     * brackets so the originating wishlist stays visible without the grouping headers.
     */
    @Composable
    private fun ItemRow(item: dev.inmo.wishlist.features.wishlist.common.models.RegisteredWishlistItem, wishlistTitle: String?) {
        ListRow(
            onSelect = { viewModel.onItemSelected(item) },
            leading = {
                val firstImage = item.imageIds.firstOrNull()
                if (firstImage != null) {
                    Img(src = viewModel.imageUrl(firstImage), alt = "") {
                        classes("rounded", "flex-shrink-0")
                        style {
                            width(48.px)
                            height(48.px)
                            property("object-fit", "cover")
                        }
                    }
                } else {
                    Div({
                        classes("rounded", "bg-secondary-subtle", "flex-shrink-0")
                        style {
                            width(48.px)
                            height(48.px)
                        }
                    })
                }
            }
        ) {
            Div({ classes("flex-grow-1") }) {
                Div({ classes("d-flex", "justify-content-between", "align-items-center") }) {
                    Div({ classes("d-flex", "align-items-center", "gap-2") }) {
                        Span {
                            Text(wishlistTitle?.let { "${item.title} ($it)" } ?: item.title)
                        }
                        PriorityBadge(item.priority)
                    }
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
