package dev.inmo.wishlist.features.ui.wishlist.ui

import dev.inmo.navigation.core.NavigationNode
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.wishlist.common.models.WishlistId

/**
 * Interactor for [UserWishlistsViewModel].
 *
 * Exposes navigation side-effects of the grid wishlists screen; the implementation
 * is registered in `client/ClientPlugin`.
 */
interface UserWishlistsViewInteractor {
    /**
     * Called when the user selects a wishlist card.
     *
     * @param node Current navigation node.
     * @param wishlistId Identifier of the selected wishlist.
     */
    suspend fun onWishlistSelected(node: NavigationNode<UserWishlistsViewConfig, ViewConfig>, wishlistId: WishlistId)

    /**
     * Called when the user taps the back button.
     *
     * @param node Current navigation node.
     */
    suspend fun onBack(node: NavigationNode<UserWishlistsViewConfig, ViewConfig>)
}
