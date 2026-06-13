package dev.inmo.wishlist.features.ui.adminPanel.ui

import dev.inmo.navigation.core.NavigationNode
import dev.inmo.wishlist.features.common.client.models.ViewConfig

/**
 * Interactor for [AdminWishlistItemEditViewModel].
 *
 * Exposes navigation side-effects from the wishlist item create/edit screen. Implementation registered in `client/ClientPlugin`.
 */
interface AdminWishlistItemEditViewInteractor {
    /** Called when the admin cancels or navigates back without saving. */
    suspend fun onNavigateBack(node: NavigationNode<AdminWishlistItemEditViewConfig, ViewConfig>)

    /** Called after a wishlist item has been successfully saved. */
    suspend fun onSaved(node: NavigationNode<AdminWishlistItemEditViewConfig, ViewConfig>)
}
