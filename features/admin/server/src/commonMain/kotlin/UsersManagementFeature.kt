package dev.inmo.wishlist.features.admin.server

import dev.inmo.micro_utils.repos.create
import dev.inmo.micro_utils.repos.deleteById
import dev.inmo.wishlist.features.admin.common.models.NewUserWithPassword
import dev.inmo.wishlist.features.auth.server.services.AuthFeatureService
import dev.inmo.wishlist.features.users.common.models.NewUser
import dev.inmo.wishlist.features.users.common.models.RegisteredUser
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.users.common.repo.UsersRepo
import dev.inmo.wishlist.features.wishlist.common.repo.WishlistItemRepo
import dev.inmo.wishlist.features.wishlist.common.repo.WishlistRepo

/**
 * Root-only user management capability backing the `/admin/users/...` routes.
 *
 * Wraps [UsersRepo] for identity CRUD and [AuthFeatureService] for password provisioning.
 * Deletion cascades through every piece of data owned by the user — wishlists, wishlist
 * items, the password hash and all active sessions — using existing repositories and the
 * auth service ([WishlistRepo], [WishlistItemRepo], [AuthFeatureService.purgeUser]).
 *
 * @param usersRepo User identity storage.
 * @param authService Auth service used to hash passwords and purge credentials/sessions.
 * @param wishlistRepo Wishlist storage, used to enumerate and delete a user's wishlists.
 * @param wishlistItemRepo Wishlist item storage, used to delete items of deleted wishlists.
 */
class UsersManagementFeature(
    private val usersRepo: UsersRepo,
    private val authService: AuthFeatureService,
    private val wishlistRepo: WishlistRepo,
    private val wishlistItemRepo: WishlistItemRepo
) {
    suspend fun getAll(): List<RegisteredUser> =
        usersRepo.getAll().values.toList()

    suspend fun create(newUserWithPassword: NewUserWithPassword): RegisteredUser? {
        val user = usersRepo.create(NewUser(newUserWithPassword.username)).firstOrNull() ?: return null
        authService.setPassword(user.id, newUserWithPassword.password)
        return user
    }

    suspend fun update(id: UserId, newUser: NewUser): Boolean? {
        if (!usersRepo.contains(id)) return null
        return usersRepo.update(id, newUser) != null
    }

    /**
     * Deletes user [id] together with all data owned by the user.
     *
     * Cascade order: delete every item of each owned wishlist, delete the wishlists, purge the
     * password hash and active sessions, then remove the user record.
     *
     * @param id Identity to remove.
     * @return `true` when the user existed and was removed; `null` when no such user.
     */
    suspend fun delete(id: UserId): Boolean? {
        if (!usersRepo.contains(id)) return null
        wishlistRepo.getByUserId(id).forEach { wishlist ->
            wishlistItemRepo.getByWishlistId(wishlist.id).forEach { item ->
                wishlistItemRepo.deleteById(item.id)
            }
            wishlistRepo.deleteById(wishlist.id)
        }
        authService.purgeUser(id)
        usersRepo.deleteById(id)
        return true
    }
}
