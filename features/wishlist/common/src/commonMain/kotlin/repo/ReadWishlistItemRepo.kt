package dev.inmo.wishlist.features.wishlist.common.repo

import dev.inmo.micro_utils.repos.ReadCRUDRepo
import dev.inmo.wishlist.features.wishlist.common.models.RegisteredWishlistItem
import dev.inmo.wishlist.features.wishlist.common.models.WishlistId
import dev.inmo.wishlist.features.wishlist.common.models.WishlistItemId

/**
 * Read-only repository for [RegisteredWishlistItem] entities.
 *
 * Extends standard CRUD read operations with [getByWishlistId] and [getByIds] to support
 * the primary access patterns of listing all items in a wishlist and batch-resolving items by id.
 */
interface ReadWishlistItemRepo : ReadCRUDRepo<RegisteredWishlistItem, WishlistItemId> {
    /**
     * Returns all items belonging to [wishlistId].
     *
     * @param wishlistId Parent wishlist to filter by.
     * @return Matching items; empty list when none found.
     */
    suspend fun getByWishlistId(wishlistId: WishlistId): List<RegisteredWishlistItem>

    /**
     * Batch lookup by id, collapsing the per-id round-trips of calling [get] in a loop into a single
     * query (PR #31 F6 — the `myPresentsBooks` N+1).
     *
     * Ids with no matching item are silently omitted, so the result may be shorter than [ids]. The
     * result preserves the order of [ids]; duplicate ids collapse to a single entry.
     *
     * @param ids Item ids to resolve; an empty list returns an empty list without touching storage.
     * @return Matching items in [ids] order, missing ids omitted.
     */
    suspend fun getByIds(ids: List<WishlistItemId>): List<RegisteredWishlistItem>
}
