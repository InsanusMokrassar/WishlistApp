package dev.inmo.wishlist.features.ui.adminPanel.ui

import dev.inmo.wishlist.features.admin.common.models.NewUserWithPassword
import dev.inmo.wishlist.features.users.common.models.NewUser
import dev.inmo.wishlist.features.users.common.models.RegisteredUser
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.wishlist.common.models.NewWishlist
import dev.inmo.wishlist.features.wishlist.common.models.NewWishlistItem
import dev.inmo.wishlist.features.wishlist.common.models.RegisteredWishlist
import dev.inmo.wishlist.features.wishlist.common.models.RegisteredWishlistItem
import dev.inmo.wishlist.features.wishlist.common.models.WishlistId
import dev.inmo.wishlist.features.wishlist.common.models.WishlistItemId
import kotlinx.coroutines.flow.StateFlow

/**
 * Model interface for all admin panel UI screens.
 *
 * Wraps [dev.inmo.wishlist.features.admin.client.AdminFeature] into a single surface
 * consumed by all admin ViewModels.
 */
interface AdminPanelModel {
    /**
     * Reactive login-state flow; emits `true` while a user is authenticated and `false` after logout.
     * Mirrors `AuthCredentialsStorage.userAuthorised`, exposed here so edit ViewModels route their
     * logout-exit through the model instead of touching auth storage directly (MVVM boundary).
     */
    val userAuthorisedState: StateFlow<Boolean>

    /** Returns all registered users. */
    suspend fun getAllUsers(): List<RegisteredUser>

    /**
     * Returns a single user by [id].
     *
     * @param id User identifier.
     * @return Matching user, or `null` when not found.
     */
    suspend fun getUserById(id: UserId): RegisteredUser?

    /**
     * Creates a new user with password.
     *
     * @param newUser Username and password for the new user.
     * @return Created user, or `null` on failure.
     */
    suspend fun createUser(newUser: NewUserWithPassword): RegisteredUser?

    /**
     * Updates an existing user's username.
     *
     * @param id User to update.
     * @param newUser Replacement data.
     * @return `true` on success.
     */
    suspend fun updateUser(id: UserId, newUser: NewUser): Boolean

    /**
     * Deletes user [id].
     *
     * @param id User to delete.
     * @return `true` on success.
     */
    suspend fun deleteUser(id: UserId): Boolean

    /** Returns all wishlists across all users. */
    suspend fun getAllWishlists(): List<RegisteredWishlist>

    /**
     * Returns wishlists owned by [userId].
     *
     * @param userId Owner identifier.
     * @return List of wishlists; empty when none found.
     */
    suspend fun getWishlistsByUser(userId: UserId): List<RegisteredWishlist>

    /**
     * Returns a single wishlist by [id].
     *
     * @param id Wishlist identifier.
     * @return Matching wishlist, or `null` when not found.
     */
    suspend fun getWishlistById(id: WishlistId): RegisteredWishlist?

    /**
     * Creates a new wishlist.
     *
     * @param newWishlist Owner and title for the new wishlist.
     * @return Created wishlist, or `null` on failure.
     */
    suspend fun createWishlist(newWishlist: NewWishlist): RegisteredWishlist?

    /**
     * Updates an existing wishlist's owner and title.
     *
     * @param id Wishlist to update.
     * @param userId New owner.
     * @param title New title.
     * @return `true` on success.
     */
    suspend fun updateWishlist(id: WishlistId, userId: UserId, title: String): Boolean

    /**
     * Deletes wishlist [id].
     *
     * @param id Wishlist to delete.
     * @return `true` on success.
     */
    suspend fun deleteWishlist(id: WishlistId): Boolean

    /**
     * Returns all items belonging to [wishlistId].
     *
     * @param wishlistId Parent wishlist identifier.
     * @return List of items; empty when none found.
     */
    suspend fun getItemsByWishlist(wishlistId: WishlistId): List<RegisteredWishlistItem>

    /**
     * Creates a new wishlist item.
     *
     * @param item Item data including parent [WishlistId].
     * @return Created item, or `null` on failure.
     */
    suspend fun createWishlistItem(item: NewWishlistItem): RegisteredWishlistItem?

    /**
     * Replaces item [id] data with [item].
     *
     * @param id Item to update.
     * @param item Replacement data.
     * @return `true` on success.
     */
    suspend fun updateWishlistItem(id: WishlistItemId, item: NewWishlistItem): Boolean

    /**
     * Deletes item [id].
     *
     * @param id Item to delete.
     * @return `true` on success.
     */
    suspend fun deleteWishlistItem(id: WishlistItemId): Boolean
}
