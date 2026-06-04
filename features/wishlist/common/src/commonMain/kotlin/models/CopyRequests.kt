package dev.inmo.wishlist.features.wishlist.common.models

import kotlinx.serialization.Serializable

/**
 * Wire-format request body for copying a single wishlist item into one of the caller's own wishlists.
 *
 * The server resolves the caller from the bearer token and enforces that [targetWishlistId] is owned
 * by the caller before creating the deep copy.
 *
 * @property sourceItemId Item to copy (may belong to any user).
 * @property sourceWishlistId Wishlist the source item currently belongs to.
 * @property targetWishlistId Caller-owned wishlist that receives the new item.
 */
@Serializable
data class CopyItemRequest(
    val sourceItemId: WishlistItemId,
    val sourceWishlistId: WishlistId,
    val targetWishlistId: WishlistId
)

/**
 * Wire-format request body for enqueuing a whole-wishlist deep copy into the caller's profile.
 *
 * The recipient is always the authenticated caller — never supplied by the client.
 *
 * @property sourceWishlistId Wishlist to deep-copy (may belong to any user).
 */
@Serializable
data class CopyWishlistRequest(
    val sourceWishlistId: WishlistId
)
