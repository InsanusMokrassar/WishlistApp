package dev.inmo.wishlist.features.ui.wishlist.ui

import dev.inmo.navigation.core.NavigationNode
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.wishlist.common.models.WishlistId
import dev.inmo.wishlist.features.wishlist.common.models.WishlistItemId

/**
 * Interactor for [UserWishlistsViewModel].
 *
 * Exposes navigation side-effects of the all-items screen; the implementation
 * is registered in `client/ClientPlugin`.
 */
interface UserWishlistsViewInteractor {
    /**
     * Called when the user selects an item row.
     *
     * @param node Current navigation node.
     * @param itemId Identifier of the selected item.
     * @param wishlistId Parent wishlist of the selected item, required to open its detail screen.
     */
    suspend fun onItemSelected(
        node: NavigationNode<UserWishlistsViewConfig, ViewConfig>,
        itemId: WishlistItemId,
        wishlistId: WishlistId
    )

    /**
     * Called when the user taps the back button.
     *
     * @param node Current navigation node.
     */
    suspend fun onBack(node: NavigationNode<UserWishlistsViewConfig, ViewConfig>)
}
