package dev.inmo.wishlist.features.ui.wishlist.ui

import dev.inmo.navigation.core.NavigationNode
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.wishlist.common.models.WishlistId

/**
 * Interactor for [WishlistsListViewModel].
 *
 * Exposes navigation side-effects that [WishlistsListViewModel] delegates to the surrounding
 * application layer. The implementation is registered in `client/ClientPlugin`.
 */
interface WishlistsListViewInteractor {
    /**
     * Called when the user selects a wishlist from the list.
     *
     * @param node Current navigation node.
     * @param wishlistId Identifier of the selected wishlist.
     */
    suspend fun onWishlistSelected(node: NavigationNode<WishlistsListViewConfig, ViewConfig>, wishlistId: WishlistId)

    /**
     * Called when the user requests creation of a new wishlist.
     *
     * @param node Current navigation node.
     */
    suspend fun onCreateWishlist(node: NavigationNode<WishlistsListViewConfig, ViewConfig>)

    /**
     * Called when the user taps the back button.
     *
     * @param node Current navigation node.
     */
    suspend fun onBack(node: NavigationNode<WishlistsListViewConfig, ViewConfig>)
}
