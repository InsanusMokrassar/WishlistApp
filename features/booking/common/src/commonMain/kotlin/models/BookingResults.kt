package dev.inmo.wishlist.features.booking.common.models

/**
 * Outcome of reading an item's booking state on the server.
 *
 * Extracted from the server `BookingService` so the service and its routing configurator share a
 * single declaration. Semantics are unchanged from the pre-extraction service-local interface.
 */
sealed interface BookingResult {
    /** Item or its parent wishlist does not exist. */
    data object ItemNotFound : BookingResult

    /** Caller owns the item; booking state is hidden from owners (rule 3). */
    data object OwnerForbidden : BookingResult

    /**
     * Booking state visible to a non-owner authorized caller.
     *
     * @property state Booker-anonymous booking status.
     */
    data class State(val state: BookingState) : BookingResult
}

/**
 * Outcome of attempting to book an item on the server.
 *
 * Extracted from the server `BookingService`; semantics unchanged.
 */
sealed interface BookResult {
    /** Item or its parent wishlist does not exist. */
    data object ItemNotFound : BookResult

    /** Caller owns the item; owners cannot book their own items and never see booking state (rule 3). */
    data object OwnerForbidden : BookResult

    /** Item is already reserved by some user; only one active booking is allowed (rule 4). */
    data object AlreadyBooked : BookResult

    /** Booking succeeded. */
    data object Ok : BookResult
}

/**
 * Outcome of attempting to cancel an item's booking on the server.
 *
 * Extracted from the server `BookingService`; semantics unchanged.
 */
sealed interface CancelResult {
    /** Item or its parent wishlist does not exist. */
    data object ItemNotFound : CancelResult

    /** Caller owns the item; owners have no access to booking operations (rule 3). */
    data object OwnerForbidden : CancelResult

    /** An existing booking belongs to a different user; only the booker may cancel it. */
    data object NotBooker : CancelResult

    /** Booking cancelled, or there was no booking to cancel. */
    data object Ok : CancelResult
}
