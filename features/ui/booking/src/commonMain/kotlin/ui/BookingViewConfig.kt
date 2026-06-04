package dev.inmo.wishlist.features.ui.booking.ui

import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.wishlist.common.models.WishlistId
import dev.inmo.wishlist.features.wishlist.common.models.WishlistItemId
import kotlinx.serialization.Serializable

/**
 * Navigation config for the booking ("book this item for gifting") view — scenario view A.
 *
 * This is the config returned by the booking
 * [dev.inmo.wishlist.features.ui.wishlist.ui.WishlistAdditionalConfigsProvider] from the wishlist
 * item screen: the item screen draws the compact gift-booking view INLINE for this config via its
 * own `InjectNavigationChain` / `InjectNavigationNode` (no separate screen is pushed).
 *
 * @property itemId Item to (un)book for gifting.
 * @property wishlistId Parent wishlist of [itemId]; kept for symmetry with other item-scoped configs.
 */
@Serializable
data class BookingViewConfig(
    val itemId: WishlistItemId,
    val wishlistId: WishlistId
) : ViewConfig
