package dev.inmo.wishlist.features.ui.users.ui

import dev.inmo.navigation.core.NavigationNode
import dev.inmo.wishlist.features.common.client.models.ViewConfig

/**
 * Side-effecting capability for the user profile edit screen.
 *
 * Implementation owned by the top-level `client/` module (intra-feature pop navigation).
 */
interface UserEditViewInteractor {
    /** Called when the user cancels or navigates back without saving. */
    suspend fun onNavigateBack(node: NavigationNode<UserEditViewConfig, ViewConfig>)

    /** Called after the user has been successfully saved. */
    suspend fun onSaved(node: NavigationNode<UserEditViewConfig, ViewConfig>)

    /**
     * Called after `root` deletes the user. Pops the edit screen; the underlying profile view
     * auto-pops when it reloads and finds the user gone.
     */
    suspend fun onDeleted(node: NavigationNode<UserEditViewConfig, ViewConfig>)
}
