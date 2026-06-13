package dev.inmo.wishlist.features.ui.wishlist.ui

import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.wishlist.common.models.WishlistId
import kotlinx.serialization.Serializable

/**
 * Navigation config for the wishlist edit/create screen.
 *
 * @property wishlistId `null` for create mode, non-null for edit mode.
 */
@Serializable
data class WishlistEditViewConfig(val wishlistId: WishlistId?) : ViewConfig
