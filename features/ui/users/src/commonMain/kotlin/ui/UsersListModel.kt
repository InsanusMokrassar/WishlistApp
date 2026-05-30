package dev.inmo.wishlist.features.ui.users.ui

import dev.inmo.wishlist.features.users.common.models.RegisteredUser

/**
 * Model facade consumed by [UsersListViewModel].
 *
 * Hides the underlying `features/users/client` feature surface and exposes only
 * the read operation needed by the main page.
 */
interface UsersListModel {
    /**
     * Returns the full list of registered users.
     *
     * @return All [RegisteredUser]s; empty when none registered.
     */
    suspend fun getAllUsers(): List<RegisteredUser>
}
