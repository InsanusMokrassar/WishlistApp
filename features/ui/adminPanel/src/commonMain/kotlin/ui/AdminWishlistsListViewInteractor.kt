package dev.inmo.wishlist.features.ui.adminPanel.ui

import dev.inmo.navigation.core.NavigationNode
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.wishlist.common.models.WishlistId

/**
 * Interactor for [AdminWishlistsListViewModel].
 *
 * Exposes navigation side-effects from the wishlists list screen. Implementation registered in `client/ClientPlugin`.
 */
interface AdminWishlistsListViewInteractor {
    /**
     * Called when the admin selects a wishlist from the list.
     *
     * @param node Current navigation node.
     * @param wishlistId Identifier of the selected wishlist.
     */
    suspend fun onWishlistSelected(node: NavigationNode<AdminWishlistsListViewConfig, ViewConfig>, wishlistId: WishlistId)

    /**
     * Called when the admin requests creation of a new wishlist.
     *
     * @param node Current navigation node.
     */
    suspend fun onCreateWishlist(node: NavigationNode<AdminWishlistsListViewConfig, ViewConfig>)
}
