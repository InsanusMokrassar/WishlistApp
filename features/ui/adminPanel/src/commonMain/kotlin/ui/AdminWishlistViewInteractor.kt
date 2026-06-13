package dev.inmo.wishlist.features.ui.adminPanel.ui

import dev.inmo.navigation.core.NavigationNode
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.wishlist.common.models.WishlistId
import dev.inmo.wishlist.features.wishlist.common.models.WishlistItemId

/**
 * Interactor for [AdminWishlistViewModel].
 *
 * Exposes navigation side-effects from the wishlist detail screen. Implementation registered in `client/ClientPlugin`.
 */
interface AdminWishlistViewInteractor {
    /** Called when the admin taps Back. */
    suspend fun onBack(node: NavigationNode<AdminWishlistViewConfig, ViewConfig>)

    /** Called when the admin taps Edit on the wishlist. */
    suspend fun onEditWishlist(node: NavigationNode<AdminWishlistViewConfig, ViewConfig>)

    /**
     * Called when the admin taps Add Item.
     *
     * @param node Current navigation node.
     * @param wishlistId Parent wishlist identifier.
     */
    suspend fun onAddItem(node: NavigationNode<AdminWishlistViewConfig, ViewConfig>, wishlistId: WishlistId)

    /**
     * Called when the admin taps Edit on a specific item.
     *
     * @param node Current navigation node.
     * @param itemId Identifier of the item to edit.
     * @param wishlistId Parent wishlist identifier.
     */
    suspend fun onEditItem(node: NavigationNode<AdminWishlistViewConfig, ViewConfig>, itemId: WishlistItemId, wishlistId: WishlistId)
}
