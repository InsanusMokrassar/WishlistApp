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
 * item screen: tapping the booking button pushes this config to open the gift-booking view.
 *
 * @property itemId Item to (un)book for gifting.
 * @property wishlistId Parent wishlist of [itemId]; kept for symmetry with other item-scoped configs.
 */
@Serializable
data class BookingViewConfig(
    val itemId: WishlistItemId,
    val wishlistId: WishlistId
) : ViewConfig
