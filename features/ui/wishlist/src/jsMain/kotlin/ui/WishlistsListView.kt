package dev.inmo.wishlist.features.ui.wishlist.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dev.inmo.micro_utils.strings.translation
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.mvvm.compose.ComposeView
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.common.client.ui.components.CalmButton
import dev.inmo.wishlist.features.common.client.ui.components.CalmButtonVariant
import dev.inmo.wishlist.features.common.client.ui.components.CalmIcons
import dev.inmo.wishlist.features.common.client.ui.components.ContentColumn
import dev.inmo.wishlist.features.common.client.ui.components.EmptyState
import dev.inmo.wishlist.features.common.client.ui.components.ListCard
import dev.inmo.wishlist.features.common.client.ui.components.ListCardsGrid
import dev.inmo.wishlist.features.common.client.ui.components.NewListCard
import dev.inmo.wishlist.features.common.client.ui.components.PageHead
import dev.inmo.wishlist.features.common.client.ui.components.Subline
import dev.inmo.wishlist.features.common.client.ui.components.tintClass
import dev.inmo.wishlist.features.ui.topBar.ui.TopBarTitleProvider
import dev.inmo.wishlist.features.ui.wishlist.WishlistStrings
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

/**
 * JS Compose-HTML view for the wishlists list screen (Calm Studio "My Lists" / a user's profile).
 *
 * Composed from the shared Calm Studio components ([ContentColumn] + [PageHead] shell, a [ListCardsGrid]
 * of [ListCard]s with a trailing owner-only [NewListCard], and [EmptyState] when there are no lists).
 * Owners get a primary "New Wishlist" action; visitors get the "All items" and "Profile" affordances.
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

        ContentColumn {
            PageHead(
                title = userName?.let { WishlistStrings.userWishlistsTitleFormat.translation().replace("{name}", it) }
                    ?: WishlistStrings.wishlistsTitle.translation(),
                actions = {
                    if (viewModel.targetUserId != null) {
                        CalmButton(
                            text = WishlistStrings.allItemsButton.translation(),
                            onClick = { viewModel.onShowUserWishlists() },
                        )
                    }
                    if (profileUserId != null) {
                        CalmButton(
                            text = WishlistStrings.profileButton.translation(),
                            onClick = { viewModel.onShowProfile() },
                        )
                    }
                    CreateWishlistButton(isOwner) { viewModel.onCreateWishlist() }
                },
            )

            when {
                loading -> Subline(WishlistStrings.loading.translation())
                wishlists.isEmpty() -> EmptyState(
                    icon = CalmIcons.gift,
                    title = WishlistStrings.emptyWishlists.translation(),
                    action = {
                        if (isOwner) {
                            CalmButton(
                                text = WishlistStrings.createWishlistButton.translation(),
                                onClick = { viewModel.onCreateWishlist() },
                                variant = CalmButtonVariant.Primary,
                                leadingIcon = CalmIcons.plus,
                            )
                        }
                    },
                )
                else -> ListCardsGrid {
                    wishlists.forEach { wishlist ->
                        ListCard(
                            title = wishlist.title,
                            tintClass = tintClass(wishlist.id.long),
                            onOpen = { viewModel.onWishlistSelected(wishlist.id) },
                        )
                    }
                    if (isOwner) {
                        NewListCard(WishlistStrings.createWishlistButton.translation()) { viewModel.onCreateWishlist() }
                    }
                }
            }
        }
    }
}
