package dev.inmo.wishlist.features.ui.users.ui

import dev.inmo.navigation.core.NavigationNode
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.users.common.models.UserId

/**
 * Side-effecting capability for the users list screen.
 *
 * Implementation owned by the top-level `client/` module — pushes
 * `WishlistsListViewConfig(userId)` onto the current node's chain.
 */
interface UsersListViewInteractor {
    /**
     * Invoked when the user taps a row in the users list.
     *
     * @param node Navigation node hosting the users list view.
     * @param userId Identifier of the selected user.
     */
    suspend fun onUserSelected(
        node: NavigationNode<UsersListViewConfig, ViewConfig>,
        userId: UserId
    )
}
