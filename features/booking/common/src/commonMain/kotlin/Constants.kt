package dev.inmo.wishlist.features.booking.common

/**
 * URL path segment constants shared between booking server routing and client HTTP calls.
 *
 * All booking routes live under [bookingPrefixPathPart] and require authentication.
 */
object Constants {
    /** Root path segment for all wishlist item booking routes: `/wishlistItemBooking/...`. All booking routes require authentication. */
    const val bookingPrefixPathPart = "wishlistItemBooking"

    /** Path segment for the booking state route: `.../state/{itemId}`. */
    const val bookingStatePathPart = "state"

    /** Path segment for the book route: `.../book/{itemId}`. */
    const val bookingBookPathPart = "book"

    /** Path segment for the cancel-booking route: `.../cancel/{itemId}`. */
    const val bookingCancelPathPart = "cancel"

    /** Path segment for the my-presents route: `.../myPresentsBooks`. Returns items the caller has booked. */
    const val bookingMyPresentsBooksPathPart = "myPresentsBooks"
}