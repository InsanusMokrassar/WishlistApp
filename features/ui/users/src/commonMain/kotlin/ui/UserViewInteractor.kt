package dev.inmo.wishlist.features.ui.users.ui

import dev.inmo.navigation.core.NavigationNode
import dev.inmo.wishlist.features.common.client.models.ViewConfig

/**
 * Side-effecting capability for the user profile detail screen.
 *
 * Implementation owned by the top-level `client/` module (intra-feature push/pop).
 */
interface UserViewInteractor {
    /**
     * Invoked when the user taps Back (or when the displayed user no longer exists).
     *
     * @param node Navigation node hosting the user profile view.
     */
    suspend fun onBack(node: NavigationNode<UserViewConfig, ViewConfig>)

    /**
     * Invoked when an owner / `root` taps Edit — pushes the edit screen for the same user.
     *
     * @param node Navigation node hosting the user profile view.
     */
    suspend fun onEditUser(node: NavigationNode<UserViewConfig, ViewConfig>)
}
