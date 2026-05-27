package dev.inmo.wishlist.features.ui.adminPanel.ui

import dev.inmo.navigation.core.NavigationNode
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.users.common.models.UserId

/**
 * Interactor for [AdminUsersListViewModel].
 *
 * Exposes navigation side-effects from the users list screen. Implementation registered in `client/ClientPlugin`.
 */
interface AdminUsersListViewInteractor {
    /**
     * Called when the user selects a user from the list.
     *
     * @param node Current navigation node.
     * @param userId Identifier of the selected user.
     */
    suspend fun onUserSelected(node: NavigationNode<AdminUsersListViewConfig, ViewConfig>, userId: UserId)

    /**
     * Called when the user requests creation of a new user.
     *
     * @param node Current navigation node.
     */
    suspend fun onCreateUser(node: NavigationNode<AdminUsersListViewConfig, ViewConfig>)
}
