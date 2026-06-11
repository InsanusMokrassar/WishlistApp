package dev.inmo.wishlist.features.booking.common.models

import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.wishlist.common.models.WishlistItemId
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/** Type-safe identifier for a [Booking]. Backed by a [Long] primary key. */
@Serializable
@JvmInline
value class BookingId(val long: Long)

/**
 * Common interface for all booking (gift reservation) variants.
 *
 * A booking represents an authorized user's intent to gift the referenced wishlist item.
 * Sealed to allow exhaustive handling of [NewBooking] and [RegisteredBooking].
 */
@Serializable
sealed interface Booking {
    /** Wishlist item this booking reserves for gifting. */
    val itemId: WishlistItemId

    /** Authorized user who placed the booking (the prospective gifter). */
    val userId: UserId
}

/**
 * Booking not yet persisted — used as input to create operations.
 *
 * @property itemId Item being reserved for gifting.
 * @property userId User placing the booking.
 */
@Serializable
data class NewBooking(
    override val itemId: WishlistItemId,
    override val userId: UserId
) : Booking

/**
 * Booking already persisted in the database.
 *
 * @property id Unique persistent identifier assigned by the server.
 * @property itemId Item being reserved for gifting.
 * @property userId User who placed the booking.
 */
@Serializable
data class RegisteredBooking(
    val id: BookingId,
    override val itemId: WishlistItemId,
    override val userId: UserId
) : Booking
