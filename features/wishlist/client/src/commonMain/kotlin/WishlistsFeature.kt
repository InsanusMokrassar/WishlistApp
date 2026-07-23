package dev.inmo.wishlist.features.wishlist.client

import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.wishlist.common.models.NewWishlistInFeature
import dev.inmo.wishlist.features.wishlist.common.models.WishlistId
import dev.inmo.wishlist.features.wishlist.common.models.WishlistsFeatureWishlist

/**
 * Feature contract for wishlist CRUD operations.
 *
 * The [create] and [update] operations accept [NewWishlistInFeature] — a payload that
 * carries no [UserId]. The server resolves the caller identity from the authenticated
 * request context; the client must not include a user identifier in mutation payloads.
 *
 * Implemented on client by [dev.inmo.wishlist.features.wishlist.client.KtorWishlistFeature].
 */
interface WishlistsFeature {
    /**
     * Returns a single wishlist by [id] without requiring authentication.
     *
     * @param id Wishlist primary key.
     * @return Matching [WishlistsFeatureWishlist], or `null` when not found.
     */
    suspend fun getById(id: WishlistId): WishlistsFeatureWishlist?

    /**
     * Returns all wishlists owned by the given user.
     *
     * @param userId Owner to filter by.
     * @return List of matching registered wishlists; empty when none found.
     */
    suspend fun getByUserId(userId: UserId): List<WishlistsFeatureWishlist>

    /**
     * Returns all wishlists owned by the authenticated caller.
     *
     * No [UserId] parameter is required — the server extracts the caller identity from
     * the bearer token. The client implementation calls the `getMy` endpoint directly.
     *
     * @return List of wishlists owned by the caller; empty when none found.
     */
    suspend fun getMyWishlists(): List<WishlistsFeatureWishlist>

    /**
     * Creates a new wishlist for the authenticated caller.
     *
     * @param newWishlist Data for the wishlist to create; owner resolved server-side.
     * @return The persisted [WishlistsFeatureWishlist], or `null` on failure.
     */
    suspend fun create(newWishlist: NewWishlistInFeature): WishlistsFeatureWishlist?

    /**
     * Replaces data of an existing wishlist identified by [id].
     *
     * The server enforces that the caller owns the wishlist.
     *
     * @param id Identifier of the wishlist to update.
     * @param newWishlist Replacement data.
     * @return `true` if updated, `false` if not found or not owned by the caller.
     */
    suspend fun update(id: WishlistId, newWishlist: NewWishlistInFeature): Boolean

    /**
     * Deletes a wishlist by [id].
     *
     * The server enforces that the caller owns the wishlist.
     *
     * @param id Identifier of the wishlist to remove.
     * @return `true` on successful deletion, `false` if not found or not owned by the caller.
     */
    suspend fun delete(id: WishlistId): Boolean
}
