package dev.inmo.wishlist.features.ui.booking.ui

import dev.inmo.wishlist.features.booking.client.BookingFeature
import dev.inmo.wishlist.features.booking.common.models.BookingFeatureItem
import dev.inmo.wishlist.features.booking.common.models.BookingState
import dev.inmo.wishlist.features.wishlist.common.models.WishlistItemId

/**
 * Default booking model that forwards UI requests to the booking client feature.

 * @param bookingFeature Remote booking capability.
 */
class DefaultBookingModel(
    private val bookingFeature: BookingFeature,
) : BookingModel {
    /** @return Booking state visible for [itemId], or `null` when hidden. */
    override suspend fun getBookingState(itemId: WishlistItemId): BookingState? =
        bookingFeature.getState(itemId)

    /** @return `true` when [itemId] is reserved for the caller. */
    override suspend fun bookItem(itemId: WishlistItemId): Boolean =
        bookingFeature.tryBook(itemId)

    /** @return `true` when the caller's reservation of [itemId] is cancelled. */
    override suspend fun cancelBooking(itemId: WishlistItemId): Boolean =
        bookingFeature.cancelBooking(itemId)

    /** @return Items currently reserved by the caller. */
    override suspend fun myPresentsBooks(): List<BookingFeatureItem> =
        bookingFeature.myPresentsBooks()
}
