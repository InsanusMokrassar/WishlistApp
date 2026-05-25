package dev.inmo.wishlist.features.wishlist.common.repo

import dev.inmo.micro_utils.repos.ReadCRUDRepo
import dev.inmo.wishlist.features.wishlist.common.models.RegisteredWishlistItem
import dev.inmo.wishlist.features.wishlist.common.models.WishlistId
import dev.inmo.wishlist.features.wishlist.common.models.WishlistItemId

/**
 * Read-only repository for [RegisteredWishlistItem] entities.
 *
 * Extends standard CRUD read operations with [getByWishlistId] to support
 * the primary access pattern of listing all items in a wishlist.
 */
interface ReadWishlistItemRepo : ReadCRUDRepo<RegisteredWishlistItem, WishlistItemId> {
    /**
     * Returns all items belonging to [wishlistId].
     *
     * @param wishlistId Parent wishlist to filter by.
     * @return Matching items; empty list when none found.
     */
    suspend fun getByWishlistId(wishlistId: WishlistId): List<RegisteredWishlistItem>
}
