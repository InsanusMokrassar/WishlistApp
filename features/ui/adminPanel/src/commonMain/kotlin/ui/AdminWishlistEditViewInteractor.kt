package dev.inmo.wishlist.features.ui.adminPanel.ui

import dev.inmo.navigation.core.NavigationNode
import dev.inmo.wishlist.features.common.client.models.ViewConfig

/**
 * Interactor for [AdminWishlistEditViewModel].
 *
 * Exposes navigation side-effects from the wishlist create/edit screen. Implementation registered in `client/ClientPlugin`.
 */
interface AdminWishlistEditViewInteractor {
    /** Called when the admin cancels or navigates back without saving. */
    suspend fun onNavigateBack(node: NavigationNode<AdminWishlistEditViewConfig, ViewConfig>)

    /** Called after a wishlist has been successfully saved (created or updated). */
    suspend fun onSaved(node: NavigationNode<AdminWishlistEditViewConfig, ViewConfig>)
}
