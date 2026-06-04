package dev.inmo.wishlist.features.ui.booking.ui

import dev.inmo.wishlist.features.booking.common.models.BookingState
import dev.inmo.wishlist.features.wishlist.common.models.RegisteredWishlistItem
import dev.inmo.wishlist.features.wishlist.common.models.WishlistItemId

/**
 * Model interface for the booking UI scenario.
 *
 * Wraps [dev.inmo.wishlist.features.booking.client.BookingFeature]. The server hides booking state
 * from the item owner and from anonymous callers, so [getBookingState] returns `null` in those
 * cases and the booking view renders nothing.
 */
interface BookingModel {
    /**
     * Reads the booking state of [itemId] visible to the current caller.
     *
     * @param itemId Item whose booking state to read.
     * @return Booker-anonymous [BookingState], or `null` when booking state is not visible.
     */
    suspend fun getBookingState(itemId: WishlistItemId): BookingState?

    /**
     * Reserves [itemId] for gifting by the authenticated caller.
     *
     * @param itemId Item to reserve.
     * @return `true` on success; `false` when not allowed.
     */
    suspend fun bookItem(itemId: WishlistItemId): Boolean

    /**
     * Cancels the caller's own reservation of [itemId].
     *
     * @param itemId Item whose reservation to cancel.
     * @return `true` on success; `false` when not allowed.
     */
    suspend fun cancelBooking(itemId: WishlistItemId): Boolean

    /**
     * Lists every item the caller has booked (the presents the caller plans to make).
     *
     * @return Booked items; empty when nothing booked.
     */
    suspend fun myPresents(): List<RegisteredWishlistItem>
}
