package dev.inmo.wishlist.features.ui.adminPanel.ui

import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.wishlist.common.models.WishlistId
import dev.inmo.wishlist.features.wishlist.common.models.WishlistItemId
import kotlinx.serialization.Serializable

/**
 * Navigation config for the admin wishlist item create/edit screen.
 *
 * @property itemId `null` in create mode; non-null to edit the existing item.
 * @property wishlistId Parent wishlist identifier (used when creating a new item).
 */
@Serializable
data class AdminWishlistItemEditViewConfig(
    val itemId: WishlistItemId?,
    val wishlistId: WishlistId
) : ViewConfig
