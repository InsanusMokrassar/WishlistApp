package dev.inmo.wishlist.features.ui.adminPanel.ui

import dev.inmo.navigation.core.NavigationNode
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.wishlist.common.models.WishlistId

/**
 * Interactor for [AdminUserViewModel].
 *
 * Exposes navigation side-effects from the user detail screen. Implementation registered in `client/ClientPlugin`.
 */
interface AdminUserViewInteractor {
    /** Called when the admin taps Back. */
    suspend fun onBack(node: NavigationNode<AdminUserViewConfig, ViewConfig>)

    /** Called when the admin taps Edit on the user. */
    suspend fun onEditUser(node: NavigationNode<AdminUserViewConfig, ViewConfig>)

    /**
     * Called when the admin taps on a wishlist belonging to this user.
     *
     * @param node Current navigation node.
     * @param wishlistId Identifier of the selected wishlist.
     */
    suspend fun onOpenWishlist(node: NavigationNode<AdminUserViewConfig, ViewConfig>, wishlistId: WishlistId)

    /**
     * Called when the admin taps Add Wishlist for this user.
     *
     * @param node Current navigation node.
     * @param userId Owner to pre-select in the new wishlist form.
     */
    suspend fun onAddWishlist(node: NavigationNode<AdminUserViewConfig, ViewConfig>, userId: UserId)
}
