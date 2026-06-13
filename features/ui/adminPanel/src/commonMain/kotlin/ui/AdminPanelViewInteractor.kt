package dev.inmo.wishlist.features.ui.adminPanel.ui

import dev.inmo.navigation.core.NavigationNode
import dev.inmo.wishlist.features.common.client.models.ViewConfig

/**
 * Interactor for [AdminPanelViewModel].
 *
 * Exposes navigation side-effects from the admin dashboard. Implementation registered in `client/ClientPlugin`.
 */
interface AdminPanelViewInteractor {
    /** Called when the user navigates to the users section. */
    suspend fun onOpenUsers(node: NavigationNode<AdminPanelViewConfig, ViewConfig>)

    /** Called when the user navigates to the wishlists section. */
    suspend fun onOpenWishlists(node: NavigationNode<AdminPanelViewConfig, ViewConfig>)
}
