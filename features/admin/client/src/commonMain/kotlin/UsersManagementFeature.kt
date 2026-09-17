package dev.inmo.wishlist.features.admin.client

import dev.inmo.wishlist.features.admin.common.models.AdminUser
import dev.inmo.wishlist.features.admin.common.models.NewUserWithPassword
import dev.inmo.wishlist.features.auth.common.models.Password
import dev.inmo.wishlist.features.users.common.models.NewUser
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.users.common.models.Username

interface UsersManagementFeature {
    suspend fun getAll(): List<AdminUser>
    suspend fun getById(id: UserId): AdminUser?
    suspend fun create(newUser: NewUserWithPassword): AdminUser?
    suspend fun update(id: UserId, newUser: NewUser): Boolean

    /**
     * Replaces only a user's username without writing an email field.
     *
     * @param id User to rename.
     * @param username Validated replacement username.
     * @return `true` on a successful root-authorized server response.
     */
    suspend fun updateUsername(id: UserId, username: Username): Boolean

    /**
     * Replaces the password of user [id] with [password] (root only on the server).
     *
     * @param id User whose password to change.
     * @param password New plaintext password; hashed server-side.
     * @return `true` on a 2xx response; `false` otherwise.
     */
    suspend fun setPassword(id: UserId, password: Password): Boolean

    suspend fun delete(id: UserId): Boolean
}
