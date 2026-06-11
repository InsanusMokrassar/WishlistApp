package dev.inmo.wishlist.features.booking.client

import dev.inmo.wishlist.features.booking.common.Constants
import dev.inmo.wishlist.features.booking.common.models.BookingState
import dev.inmo.wishlist.features.wishlist.common.models.RegisteredWishlistItem
import dev.inmo.wishlist.features.wishlist.common.models.WishlistItemId
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.http.isSuccess

/**
 * Ktor HTTP client implementation of [BookingFeature].
 *
 * Builds requests against the booking paths in [Constants]. The underlying [client] is expected
 * to carry bearer auth and base-URL configuration from the common client `HttpClientConfigurator`
 * chain. This class performs HTTP calls only — all booking rules are enforced server-side.
 *
 * @param client Preconfigured Ktor [HttpClient] injected from Koin.
 */
class KtorBookingFeature(
    private val client: HttpClient
) : BookingFeature {
    /**
     * Requests the booking state and deserialises it on success.
     *
     * @param itemId Item whose state to read.
     * @return Parsed [BookingState], or `null` on any non-2xx response (owner `403`, unauthorized,
     *   or missing item).
     */
    override suspend fun getState(itemId: WishlistItemId): BookingState? {
        val response = client.get(
            "${Constants.bookingPrefixPathPart}/${Constants.bookingStatePathPart}/${itemId.long}"
        )
        return if (response.status.isSuccess()) response.body() else null
    }

    /**
     * Posts a booking request and reports whether the server acknowledged success.
     *
     * @param itemId Item to reserve.
     * @return `true` on a 2xx response; `false` on `403`/`404`/`409`/`401`.
     */
    override suspend fun tryBook(itemId: WishlistItemId): Boolean {
        val response = client.post(
            "${Constants.bookingPrefixPathPart}/${Constants.bookingBookPathPart}/${itemId.long}"
        )
        return response.status.isSuccess()
    }

    /**
     * Posts a cancel request and reports whether the server acknowledged success.
     *
     * @param itemId Item whose reservation to cancel.
     * @return `true` on a 2xx response; `false` on `403`/`404`/`401`.
     */
    override suspend fun cancelBooking(itemId: WishlistItemId): Boolean {
        val response = client.post(
            "${Constants.bookingPrefixPathPart}/${Constants.bookingCancelPathPart}/${itemId.long}"
        )
        return response.status.isSuccess()
    }

    /**
     * Requests the caller's booked items and deserialises them on success.
     *
     * @return Parsed item list, or an empty list on any non-2xx response.
     */
    override suspend fun myPresentsBooks(): List<RegisteredWishlistItem> {
        val response = client.get(
            "${Constants.bookingPrefixPathPart}/${Constants.bookingMyPresentsBooksPathPart}"
        )
        return if (response.status.isSuccess()) response.body() else emptyList()
    }
}
