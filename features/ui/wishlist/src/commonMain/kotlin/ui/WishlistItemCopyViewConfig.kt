package dev.inmo.wishlist.features.ui.wishlist.ui

import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.wishlist.common.models.WishlistId
import dev.inmo.wishlist.features.wishlist.common.models.WishlistItemId
import kotlinx.serialization.Serializable

/**
 * Navigation config for the "copy item to my wishlist" target-picker screen.
 *
 * Lists the caller's own wishlists and copies the source item into the chosen one.
 *
 * @property sourceItemId Item to copy (belongs to another user's wishlist).
 * @property sourceWishlistId Wishlist the source item belongs to.
 */
@Serializable
data class WishlistItemCopyViewConfig(
    val sourceItemId: WishlistItemId,
    val sourceWishlistId: WishlistId
) : ViewConfig
