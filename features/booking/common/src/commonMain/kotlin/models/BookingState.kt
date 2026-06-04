package dev.inmo.wishlist.features.booking.common.models

import kotlinx.serialization.Serializable

/**
 * Wire-format view of a wishlist item's booking status exposed to non-owner authorized users.
 *
 * Deliberately carries no booker identity: other users may learn only whether the item is
 * reserved for gifting and whether the reservation is their own, never WHO reserved it.
 * The item owner never receives this DTO (the server answers `403 Forbidden` for owners),
 * so an owner cannot infer whether their item is booked.
 *
 * @property booked `true` when some authorized user currently has the item reserved for gifting.
 * @property bookedByMe `true` when the booking belongs to the requesting caller; always `false`
 *   when [booked] is `false`.
 */
@Serializable
data class BookingState(
    val booked: Boolean,
    val bookedByMe: Boolean
)
