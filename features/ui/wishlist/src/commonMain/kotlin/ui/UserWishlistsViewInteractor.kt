package dev.inmo.wishlist.features.ui.wishlist.ui

import dev.inmo.navigation.core.NavigationNode
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.users.common.models.UserId
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
     * Called when the user selects a wishlist (e.g. taps a wishlist group header).
     *
     * @param node Current navigation node.
     * @param wishlistId Identifier of the selected wishlist; its detail screen should be opened.
     */
    suspend fun onWishlistSelected(
        node: NavigationNode<UserWishlistsViewConfig, ViewConfig>,
        wishlistId: WishlistId
    )

    /**
     * Called when the user taps the back button.
     *
     * @param node Current navigation node.
     */
    suspend fun onBack(node: NavigationNode<UserWishlistsViewConfig, ViewConfig>)

    /**
     * Called when the user opens the target user's public profile.
     *
     * @param node Current navigation node.
     * @param userId Identifier of the user whose profile should be opened.
     */
    suspend fun onOpenProfile(
        node: NavigationNode<UserWishlistsViewConfig, ViewConfig>,
        userId: UserId
    )
}
