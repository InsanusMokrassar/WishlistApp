package dev.inmo.wishlist.features.wishlist.common.repo

import dev.inmo.micro_utils.repos.ReadCRUDRepo
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.wishlist.common.models.RegisteredWishlist
import dev.inmo.wishlist.features.wishlist.common.models.WishlistId

/**
 * Read-only repository for [RegisteredWishlist] entities.
 *
 * Extends standard CRUD read operations with [getByUserId] to support
 * the primary access pattern of listing a user's wishlists.
 */
interface ReadWishlistRepo : ReadCRUDRepo<RegisteredWishlist, WishlistId> {
    /**
     * Returns all wishlists owned by [userId].
     *
     * @param userId Owner to filter by.
     * @return Matching wishlists; empty list when none found.
     */
    suspend fun getByUserId(userId: UserId): List<RegisteredWishlist>
}
