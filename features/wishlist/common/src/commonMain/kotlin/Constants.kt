package dev.inmo.wishlist.features.wishlist.common

/** URL path segment constants shared between server routing and client HTTP calls. */
object Constants {
    /** Root path segment for all wishlist routes: `/wishlist/...`. */
    const val wishlistPrefixPathPart = "wishlist"

    /** Path segment for the getByUserId route: `.../getByUserId/{userId}`. */
    const val wishlistGetByUserIdPathPart = "getByUserId"

    /** Path segment for the getMy route: `.../getMy`. Returns wishlists owned by the authenticated caller. */
    const val wishlistGetMyPathPart = "getMy"

    /** Path segment for the create route: `.../create`. */
    const val wishlistCreatePathPart = "create"

    /** Path segment for the update route: `.../update/{id}`. */
    const val wishlistUpdatePathPart = "update"

    /** Path segment for the delete route: `.../delete/{id}`. */
    const val wishlistDeletePathPart = "delete"

    /** Path segment for the public getById route: `.../getById/{id}`. No auth required. */
    const val wishlistGetByIdPathPart = "getById"

    /** Root path segment for all wishlist item routes: `/wishlistItem/...`. */
    const val wishlistItemPrefixPathPart = "wishlistItem"

    /** Path segment for the getByWishlistId route: `.../getByWishlistId/{wishlistId}`. */
    const val wishlistItemGetByWishlistIdPathPart = "getByWishlistId"

    /** Path segment for the item create route: `.../create`. */
    const val wishlistItemCreatePathPart = "create"

    /** Path segment for the item update route: `.../update/{id}`. */
    const val wishlistItemUpdatePathPart = "update"

    /** Path segment for the item delete route: `.../delete/{id}`. */
    const val wishlistItemDeletePathPart = "delete"

    /** Root path segment for all wishlist item booking routes: `/wishlistItemBooking/...`. All booking routes require authentication. */
    const val bookingPrefixPathPart = "wishlistItemBooking"

    /** Path segment for the booking state route: `.../state/{itemId}`. */
    const val bookingStatePathPart = "state"

    /** Path segment for the book route: `.../book/{itemId}`. */
    const val bookingBookPathPart = "book"

    /** Path segment for the cancel-booking route: `.../cancel/{itemId}`. */
    const val bookingCancelPathPart = "cancel"
}
