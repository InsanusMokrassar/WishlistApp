package dev.inmo.wishlist.features.wishlist.server.services

import dev.inmo.micro_utils.repos.create
import dev.inmo.micro_utils.repos.deleteById
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.wishlist.common.models.NewWishlistItem
import dev.inmo.wishlist.features.wishlist.common.models.RegisteredWishlistItem
import dev.inmo.wishlist.features.wishlist.common.models.WishlistId
import dev.inmo.wishlist.features.wishlist.common.models.WishlistItemId
import dev.inmo.wishlist.features.wishlist.common.repo.WishlistItemRepo
import dev.inmo.wishlist.features.wishlist.common.repo.WishlistRepo

/**
 * Server-side wishlist item service that enforces caller ownership on mutable operations.
 *
 * Each mutating operation verifies that [callerId] owns the parent wishlist before
 * proceeding. [create] resolves the parent wishlist from [NewWishlistItem.wishlistId];
 * [update] and [delete] resolve the parent wishlist from the stored item.
 *
 * Return semantics for [update] and [delete]:
 * - `null` — item or parent wishlist not found
 * - `false` — caller is not the owner of the parent wishlist
 * - `true` — operation succeeded
 *
 * @param wishlistItemRepo Repository supplying persistent wishlist item storage.
 * @param wishlistRepo Repository used to resolve parent wishlist ownership.
 */
class WishlistItemService(
    private val wishlistItemRepo: WishlistItemRepo,
    private val wishlistRepo: WishlistRepo
) {
    /**
     * Returns all items belonging to [wishlistId].
     *
     * @param wishlistId Parent wishlist to filter by.
     * @return Matching items; empty list when none found.
     */
    suspend fun getByWishlistId(wishlistId: WishlistId): List<RegisteredWishlistItem> =
        wishlistItemRepo.getByWishlistId(wishlistId)

    /**
     * Creates an item if [callerId] owns the parent wishlist specified in [newWishlistItem].
     *
     * Returns `null` when the parent wishlist is not found, the caller is not the owner,
     * or the repository returns no result.
     *
     * @param newWishlistItem Data for the item to persist.
     * @param callerId Authenticated caller identity resolved from the request context.
     * @return Persisted [RegisteredWishlistItem], or `null` on failure or authorization error.
     */
    suspend fun create(newWishlistItem: NewWishlistItem, callerId: UserId): RegisteredWishlistItem? {
        val wishlist = wishlistRepo.getById(newWishlistItem.wishlistId) ?: return null
        if (wishlist.userId != callerId) return null
        return wishlistItemRepo.create(newWishlistItem).firstOrNull()
    }

    /**
     * Replaces data of the item identified by [id] if [callerId] owns its parent wishlist.
     *
     * @param id Target item identifier.
     * @param newWishlistItem Replacement data.
     * @param callerId Authenticated caller identity resolved from the request context.
     * @return `true` on success, `false` when [callerId] is not the owner, `null` when not found.
     */
    suspend fun update(id: WishlistItemId, newWishlistItem: NewWishlistItem, callerId: UserId): Boolean? {
        val item = wishlistItemRepo.getById(id) ?: return null
        val wishlist = wishlistRepo.getById(item.wishlistId) ?: return null
        if (wishlist.userId != callerId) return false
        return wishlistItemRepo.update(id, newWishlistItem) != null
    }

    /**
     * Deletes the item identified by [id] if [callerId] owns its parent wishlist.
     *
     * @param id Identifier of the item to remove.
     * @param callerId Authenticated caller identity resolved from the request context.
     * @return `true` on success, `false` when [callerId] is not the owner, `null` when not found.
     */
    suspend fun delete(id: WishlistItemId, callerId: UserId): Boolean? {
        val item = wishlistItemRepo.getById(id) ?: return null
        val wishlist = wishlistRepo.getById(item.wishlistId) ?: return null
        if (wishlist.userId != callerId) return false
        wishlistItemRepo.deleteById(id)
        return true
    }
}
