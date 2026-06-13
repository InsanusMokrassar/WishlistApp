package dev.inmo.wishlist.features.ui.adminPanel.ui

import dev.inmo.navigation.core.NavigationNode
import dev.inmo.wishlist.features.common.client.models.ViewConfig

/**
 * Interactor for [AdminUserEditViewModel].
 *
 * Exposes navigation side-effects from the user create/edit screen. Implementation registered in `client/ClientPlugin`.
 */
interface AdminUserEditViewInteractor {
    /** Called when the admin cancels or navigates back without saving. */
    suspend fun onNavigateBack(node: NavigationNode<AdminUserEditViewConfig, ViewConfig>)

    /** Called after a user has been successfully saved (created or updated). */
    suspend fun onSaved(node: NavigationNode<AdminUserEditViewConfig, ViewConfig>)
}
