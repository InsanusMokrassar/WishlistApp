package dev.inmo.wishlist.features.ui.wishlist.ui

import dev.inmo.navigation.core.NavigationNode
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.wishlist.common.models.WishlistItemId

/**
 * Interactor for [WishlistViewModel].
 *
 * Exposes navigation side-effects that [WishlistViewModel] delegates to the surrounding
 * application layer. The implementation is registered in `client/ClientPlugin`.
 */
interface WishlistViewInteractor {
    /**
     * Called when the user navigates back from the wishlist detail screen.
     *
     * Replaces the current node with the wishlist owner's all-items screen
     * ([UserWishlistsViewConfig]) so Back leads to the logical parent. When [ownerUserId] is
     * `null` (the wishlist has not loaded yet) the implementation falls back to a plain pop.
     *
     * @param node Current navigation node.
     * @param ownerUserId Identifier of the wishlist owner; the all-items screen to navigate to.
     */
    suspend fun onBack(
        node: NavigationNode<WishlistViewConfig, ViewConfig>,
        ownerUserId: UserId?
    )

    /**
     * Called when the user requests editing the wishlist metadata.
     *
     * @param node Current navigation node.
     */
    suspend fun onEditWishlist(node: NavigationNode<WishlistViewConfig, ViewConfig>)

    /**
     * Called when the user taps an item to view it.
     *
     * @param node Current navigation node.
     * @param itemId Identifier of the item to open.
     */
    suspend fun onViewItem(node: NavigationNode<WishlistViewConfig, ViewConfig>, itemId: WishlistItemId)

    /**
     * Called when the user requests adding a new item to the wishlist.
     *
     * @param node Current navigation node.
     */
    suspend fun onAddItem(node: NavigationNode<WishlistViewConfig, ViewConfig>)
}
