package dev.inmo.wishlist.features.booking.common.models

import kotlinx.serialization.Serializable

/**
 * Wire-format view of a wishlist item's booking status exposed to non-owner authorized users.
 *
 * Deliberately carries no booker identity: other users may learn only whether the item is reserved
 * for gifting and whether the reservation is their own, never WHO reserved it. The item owner never
 * receives this value (the server answers `403 Forbidden` for owners), so an owner cannot infer
 * whether their item is booked.
 *
 * Sealed so the three mutually-exclusive states are exhaustive at every consumer:
 * - [Free] — not booked by anyone.
 * - [Booked] — booked by some other (anonymous) user.
 * - [BookedByMe] — booked by the requesting caller.
 */
@Serializable
sealed interface BookingState {
    /** The item is not reserved for gifting by anyone. */
    @Serializable
    data object Free : BookingState

    /**
     * The item is reserved for gifting by some other authorized user. The booker's identity is
     * deliberately omitted (rule 2 — booker anonymity).
     */
    @Serializable
    data object Booked : BookingState

    /** The item is reserved for gifting by the requesting caller. */
    @Serializable
    data object BookedByMe : BookingState
}
