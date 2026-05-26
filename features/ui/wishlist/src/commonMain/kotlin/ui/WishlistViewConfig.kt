package dev.inmo.wishlist.features.ui.wishlist.ui

import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.wishlist.common.models.WishlistId
import kotlinx.serialization.Serializable

/**
 * Navigation config for the wishlist detail screen.
 *
 * @property wishlistId Identifier of the wishlist to display.
 */
@Serializable
data class WishlistViewConfig(val wishlistId: WishlistId) : ViewConfig
