package dev.inmo.wishlist.features.ui.wishlist.ui

import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.wishlist.common.models.WishlistId
import dev.inmo.wishlist.features.wishlist.common.models.WishlistItemId
import kotlinx.serialization.Serializable

/**
 * Navigation config for the wishlist item create/edit screen.
 *
 * @property wishlistItemId `null` for create mode, non-null for edit mode.
 * @property wishlistId Parent wishlist — required to create the item and for breadcrumb context.
 */
@Serializable
data class WishlistItemEditViewConfig(
    val wishlistItemId: WishlistItemId?,
    val wishlistId: WishlistId
) : ViewConfig
