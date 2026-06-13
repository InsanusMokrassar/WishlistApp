package dev.inmo.wishlist.features.booking.common.repo

import dev.inmo.micro_utils.repos.ReadCRUDRepo
import dev.inmo.wishlist.features.booking.common.models.BookingId
import dev.inmo.wishlist.features.booking.common.models.RegisteredBooking
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.wishlist.common.models.WishlistItemId

/**
 * Read-only repository for [RegisteredBooking] entities.
 *
 * Extends standard CRUD read operations with [getByItemId] (resolve the single active booking
 * of a wishlist item) and [getByUserId] (list all bookings a user has placed).
 */
interface ReadBookingRepo : ReadCRUDRepo<RegisteredBooking, BookingId> {
    /**
     * Returns the active booking of [itemId], or `null` when the item is not booked.
     *
     * At most one booking exists per item (enforced by a unique index on the item id column).
     *
     * @param itemId Item whose booking to resolve.
     * @return The active [RegisteredBooking], or `null` when the item is not reserved.
     */
    suspend fun getByItemId(itemId: WishlistItemId): RegisteredBooking?

    /**
     * Returns every booking placed by [userId] (the items the user plans to gift).
     *
     * @param userId Booker whose bookings to list.
     * @return Bookings owned by [userId]; empty list when the user has booked nothing.
     */
    suspend fun getByUserId(userId: UserId): List<RegisteredBooking>
}
