package dev.inmo.wishlist.features.ui.users.ui

import dev.inmo.wishlist.features.users.common.models.RegisteredUser
import dev.inmo.wishlist.features.users.common.models.UserId

/**
 * Model facade consumed by [UsersListViewModel].
 *
 * Hides the underlying `features/users/client`, `features/auth/client` and
 * `features/admin/client` feature surfaces, exposing only the read, root-check and
 * delete operations the main page needs.
 */
interface UsersListModel {
    /**
     * Returns the full list of registered users.
     *
     * @return All [RegisteredUser]s; empty when none registered.
     */
    suspend fun getAllUsers(): List<RegisteredUser>

    /**
     * Reports whether the authenticated caller is the `root` user — the only identity
     * permitted to delete users.
     *
     * @return `true` when logged in as `root`; `false` otherwise (including anonymous).
     */
    suspend fun isCurrentUserRoot(): Boolean

    /**
     * Deletes the user [id] together with all data owned by the user (root-only on the server).
     *
     * @param id Identifier of the user to remove.
     * @return `true` when the server confirmed deletion; `false` otherwise.
     */
    suspend fun deleteUser(id: UserId): Boolean
}
