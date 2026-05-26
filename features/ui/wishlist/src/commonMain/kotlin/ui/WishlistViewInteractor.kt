package dev.inmo.wishlist.features.ui.wishlist.ui

import dev.inmo.navigation.core.NavigationNode
import dev.inmo.wishlist.features.common.client.models.ViewConfig
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
     * @param node Current navigation node.
     */
    suspend fun onBack(node: NavigationNode<WishlistViewConfig, ViewConfig>)

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
