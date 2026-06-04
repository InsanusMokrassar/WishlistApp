package dev.inmo.wishlist.features.wishlist.common.repo

import dev.inmo.micro_utils.repos.ReadCRUDRepo
import dev.inmo.wishlist.features.wishlist.common.models.BookingId
import dev.inmo.wishlist.features.wishlist.common.models.RegisteredBooking
import dev.inmo.wishlist.features.wishlist.common.models.WishlistItemId

/**
 * Read-only repository for [RegisteredBooking] entities.
 *
 * Extends standard CRUD read operations with [getByItemId] to support the primary
 * access pattern of resolving the single active booking of a wishlist item.
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
}
