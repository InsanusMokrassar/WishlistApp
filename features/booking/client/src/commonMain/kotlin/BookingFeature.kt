package dev.inmo.wishlist.features.booking.client

import dev.inmo.wishlist.features.booking.common.models.BookingState
import dev.inmo.wishlist.features.wishlist.common.models.RegisteredWishlistItem
import dev.inmo.wishlist.features.wishlist.common.models.WishlistItemId

/**
 * Client-side contract for the wishlist-item booking (gift reservation) feature.
 *
 * All operations require the user to be authenticated; the shared [io.ktor.client.HttpClient]
 * attaches the bearer token automatically. The server hides booking state from the item owner
 * and from anonymous callers, so [getState] returns `null` whenever the caller is the owner,
 * is unauthorized, or the item does not exist.
 *
 * Implemented on the client by [KtorBookingFeature]; the server side is
 * [dev.inmo.wishlist.features.booking.server.services.BookingService].
 */
interface BookingFeature {
    /**
     * Returns the booking state visible to the caller for [itemId].
     *
     * @param itemId Item whose booking state to read.
     * @return Booker-anonymous [BookingState], or `null` when the caller is the item owner,
     *   not authorized, or the item is missing (any non-2xx response).
     */
    suspend fun getState(itemId: WishlistItemId): BookingState?

    /**
     * Reserves [itemId] for the authenticated caller.
     *
     * @param itemId Item to reserve.
     * @return `true` on success; `false` when the caller owns the item, the item is already
     *   booked by someone else, the item is missing, or the caller is not authorized.
     */
    suspend fun book(itemId: WishlistItemId): Boolean

    /**
     * Cancels the authenticated caller's own reservation of [itemId].
     *
     * @param itemId Item whose reservation to cancel.
     * @return `true` on success; `false` when the caller owns the item, the booking belongs to
     *   another user, the item is missing, or the caller is not authorized.
     */
    suspend fun cancel(itemId: WishlistItemId): Boolean

    /**
     * Lists every wishlist item the authenticated caller has booked (the presents the caller
     * plans to make).
     *
     * @return Items the caller has reserved; empty when nothing booked or on any non-2xx response.
     */
    suspend fun myPresents(): List<RegisteredWishlistItem>
}
