package dev.inmo.wishlist.features.ui.wishlist.ui

import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.wishlist.common.models.WishlistId
import dev.inmo.wishlist.features.wishlist.common.models.WishlistItemId
import kotlinx.serialization.Serializable

/**
 * Navigation config for the wishlist item read-only view screen.
 *
 * @property wishlistItemId Identifier of the item to display.
 * @property wishlistId Parent wishlist — used to load items when navigated without auth.
 */
@Serializable
data class WishlistItemViewConfig(
    val wishlistItemId: WishlistItemId,
    val wishlistId: WishlistId
) : ViewConfig
