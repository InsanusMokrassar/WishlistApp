package dev.inmo.wishlist.features.wishlist.client

import dev.inmo.wishlist.features.wishlist.common.models.NewWishlistItem
import dev.inmo.wishlist.features.wishlist.common.models.RegisteredWishlistItem
import dev.inmo.wishlist.features.wishlist.common.models.WishlistId
import dev.inmo.wishlist.features.wishlist.common.models.WishlistItemId

/**
 * Feature contract for wishlist item CRUD operations.
 *
 * Mutation operations ([create], [update], [delete]) are restricted to the owner of the
 * parent wishlist. The server resolves the caller identity from the bearer token and
 * enforces ownership; clients must not supply a caller identifier in request payloads.
 *
 * Implemented on server by [dev.inmo.wishlist.features.wishlist.server.services.WishlistItemService]
 * and on client by [dev.inmo.wishlist.features.wishlist.client.KtorWishlistItemFeature].
 */
interface WishlistsItemsFeature {
    /**
     * Returns all items belonging to the given wishlist.
     *
     * @param wishlistId Parent wishlist to filter by.
     * @return List of matching registered items; empty when none found.
     */
    suspend fun getByWishlistId(wishlistId: WishlistId): List<RegisteredWishlistItem>

    /**
     * Creates a new wishlist item from [newWishlistItem].
     *
     * The caller must own the parent wishlist identified by [NewWishlistItem.wishlistId];
     * the server enforces this constraint.
     *
     * @param newWishlistItem Data for the item to create.
     * @return The persisted [RegisteredWishlistItem], or `null` on failure or authorization error.
     */
    suspend fun create(newWishlistItem: NewWishlistItem): RegisteredWishlistItem?

    /**
     * Replaces data of an existing item identified by [id].
     *
     * The caller must own the parent wishlist of the item; the server enforces this constraint.
     *
     * @param id Identifier of the item to update.
     * @param newWishlistItem Replacement data.
     * @return `true` if updated, `false` if not found or not owned by the caller.
     */
    suspend fun update(id: WishlistItemId, newWishlistItem: NewWishlistItem): Boolean

    /**
     * Deletes a wishlist item by [id].
     *
     * The caller must own the parent wishlist of the item; the server enforces this constraint.
     *
     * @param id Identifier of the item to remove.
     * @return `true` on successful deletion, `false` if not found or not owned by the caller.
     */
    suspend fun delete(id: WishlistItemId): Boolean
}
