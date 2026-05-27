package dev.inmo.wishlist.features.ui.adminPanel.ui

import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.wishlist.common.models.WishlistId
import kotlinx.serialization.Serializable

/**
 * Navigation config for the admin wishlist detail screen.
 *
 * @property wishlistId Identifier of the wishlist to display.
 */
@Serializable
data class AdminWishlistViewConfig(val wishlistId: WishlistId) : ViewConfig
