package dev.inmo.wishlist.features.wishlist.server.services

import dev.inmo.micro_utils.repos.create
import dev.inmo.micro_utils.repos.deleteById
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.wishlist.common.models.NewWishlist
import dev.inmo.wishlist.features.wishlist.common.models.NewWishlistInFeature
import dev.inmo.wishlist.features.wishlist.common.models.RegisteredWishlist
import dev.inmo.wishlist.features.wishlist.common.models.WishlistId
import dev.inmo.wishlist.features.wishlist.common.repo.WishlistRepo

/**
 * Server-side wishlist service that enforces caller ownership on mutable operations.
 *
 * Create converts [NewWishlistInFeature] to [NewWishlist] using the supplied [UserId].
 * Update and delete verify that [callerId] matches the stored owner before proceeding;
 * they return `null` when the wishlist is absent and `false` when the caller is not the owner.
 *
 * @param wishlistRepo Repository supplying persistent wishlist storage.
 */
class WishlistService(
    private val wishlistRepo: WishlistRepo
) {
    /**
     * Returns the wishlist identified by [id], or `null` when not found.
     *
     * Accessible without caller identity — used by the public read endpoint.
     *
     * @param id Wishlist primary key.
     * @return Matching [RegisteredWishlist], or `null` when absent.
     */
    suspend fun getById(id: WishlistId): RegisteredWishlist? =
        wishlistRepo.getById(id)

    /**
     * Returns all wishlists owned by [userId].
     *
     * @param userId Owner to filter by.
     * @return Matching wishlists; empty list when none found.
     */
    suspend fun getByUserId(userId: UserId): List<RegisteredWishlist> =
        wishlistRepo.getByUserId(userId)

    /**
     * Returns all wishlists owned by the authenticated [callerId].
     *
     * Delegates to [getByUserId] using the caller identity extracted from the request context.
     *
     * @param callerId Authenticated caller identity resolved from the bearer token.
     * @return Matching wishlists; empty list when none found.
     */
    suspend fun getMyWishlists(callerId: UserId): List<RegisteredWishlist> =
        wishlistRepo.getByUserId(callerId)

    /**
     * Creates a wishlist owned by [userId] from the client-supplied [newWishlist] payload.
     *
     * @param newWishlist Client-supplied data (title only, no user identifier).
     * @param userId Authenticated caller identity resolved from the request context.
     * @return Persisted [RegisteredWishlist], or `null` if the repo returned no result.
     */
    suspend fun create(newWishlist: NewWishlistInFeature, userId: UserId): RegisteredWishlist? =
        wishlistRepo.create(NewWishlist(userId, newWishlist.title)).firstOrNull()

    /**
     * Replaces data of the wishlist identified by [id] if [callerId] owns it.
     *
     * @param id Target wishlist identifier.
     * @param newWishlist Replacement data (title only).
     * @param callerId Authenticated caller identity resolved from the request context.
     * @return `true` on success, `false` when [callerId] is not the owner, `null` when not found.
     */
    suspend fun update(id: WishlistId, newWishlist: NewWishlistInFeature, callerId: UserId): Boolean? {
        val existing = wishlistRepo.getById(id) ?: return null
        if (existing.userId != callerId) return false
        return wishlistRepo.update(id, NewWishlist(callerId, newWishlist.title)) != null
    }

    /**
     * Deletes the wishlist identified by [id] if [callerId] owns it.
     *
     * @param id Identifier of the wishlist to remove.
     * @param callerId Authenticated caller identity resolved from the request context.
     * @return `true` on success, `false` when [callerId] is not the owner, `null` when not found.
     */
    suspend fun delete(id: WishlistId, callerId: UserId): Boolean? {
        val existing = wishlistRepo.getById(id) ?: return null
        if (existing.userId != callerId) return false
        wishlistRepo.deleteById(id)
        return true
    }
}
