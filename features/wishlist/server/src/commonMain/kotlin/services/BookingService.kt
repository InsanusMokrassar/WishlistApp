package dev.inmo.wishlist.features.wishlist.server.services

import dev.inmo.micro_utils.repos.create
import dev.inmo.micro_utils.repos.deleteById
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.wishlist.common.models.BookingState
import dev.inmo.wishlist.features.wishlist.common.models.NewBooking
import dev.inmo.wishlist.features.wishlist.common.models.WishlistItemId
import dev.inmo.wishlist.features.wishlist.common.repo.BookingRepo
import dev.inmo.wishlist.features.wishlist.common.repo.WishlistItemRepo
import dev.inmo.wishlist.features.wishlist.common.repo.WishlistRepo

/**
 * Server-side service that owns all booking (gift reservation) business rules.
 *
 * Enforces every visibility/authorization rule of issue #29 on the server side:
 * 1. Only authorized users reach this service (the routing layer wraps all booking routes in
 *    Ktor `authenticate { }`; this service is never invoked for anonymous callers).
 * 2. Other users can observe only whether an item is booked, never WHO booked it: results carry
 *    no booker identity, only the boolean [BookingState].
 * 3. The item OWNER is fully cut off from booking state: every operation returns
 *    [BookingResult.OwnerForbidden] / [BookResult.OwnerForbidden] / [CancelResult.OwnerForbidden]
 *    when the caller owns the parent wishlist, so an owner never learns whether an item is booked.
 * 4. An item can be booked by at most one user: [book] rejects when a booking already exists, and
 *    the underlying repository's unique item-id index rejects concurrent duplicate inserts.
 *
 * @param bookingRepo Repository storing the single active booking per item.
 * @param wishlistItemRepo Repository used to resolve an item's parent wishlist.
 * @param wishlistRepo Repository used to resolve the parent wishlist owner.
 */
class BookingService(
    private val bookingRepo: BookingRepo,
    private val wishlistItemRepo: WishlistItemRepo,
    private val wishlistRepo: WishlistRepo
) {
    /**
     * Resolves the [UserId] that owns the wishlist containing [itemId].
     *
     * @param itemId Item whose owner to resolve.
     * @return Owner id, or `null` when the item or its parent wishlist does not exist.
     */
    private suspend fun ownerOf(itemId: WishlistItemId): UserId? {
        val item = wishlistItemRepo.getById(itemId) ?: return null
        return wishlistRepo.getById(item.wishlistId)?.userId
    }

    /**
     * Computes the booking state visible to [callerId] for [itemId].
     *
     * @param itemId Item whose booking state to read.
     * @param callerId Authenticated caller resolved from the request context.
     * @return [BookingResult.ItemNotFound] when the item/parent is missing,
     *   [BookingResult.OwnerForbidden] when the caller owns the item (rule 3 — owner hidden),
     *   otherwise [BookingResult.State] carrying a booker-anonymous [BookingState].
     */
    suspend fun getState(itemId: WishlistItemId, callerId: UserId): BookingResult {
        val ownerId = ownerOf(itemId) ?: return BookingResult.ItemNotFound
        if (ownerId == callerId) return BookingResult.OwnerForbidden
        val booking = bookingRepo.getByItemId(itemId)
        return BookingResult.State(
            BookingState(
                booked = booking != null,
                bookedByMe = booking?.userId == callerId
            )
        )
    }

    /**
     * Reserves [itemId] for [callerId] if permitted and not already booked.
     *
     * @param itemId Item to reserve.
     * @param callerId Authenticated caller placing the booking.
     * @return [BookResult.ItemNotFound] when the item/parent is missing,
     *   [BookResult.OwnerForbidden] when the caller owns the item (rule 3),
     *   [BookResult.AlreadyBooked] when another booking already exists (rule 4),
     *   [BookResult.Ok] on success.
     */
    suspend fun book(itemId: WishlistItemId, callerId: UserId): BookResult {
        val ownerId = ownerOf(itemId) ?: return BookResult.ItemNotFound
        if (ownerId == callerId) return BookResult.OwnerForbidden
        if (bookingRepo.getByItemId(itemId) != null) return BookResult.AlreadyBooked
        return try {
            bookingRepo.create(NewBooking(itemId, callerId))
            BookResult.Ok
        } catch (e: Throwable) {
            // Unique item-id index rejects a concurrent second booking; surface it as a conflict.
            BookResult.AlreadyBooked
        }
    }

    /**
     * Cancels [callerId]'s own booking of [itemId].
     *
     * @param itemId Item whose booking to cancel.
     * @param callerId Authenticated caller cancelling the booking.
     * @return [CancelResult.ItemNotFound] when the item/parent is missing,
     *   [CancelResult.OwnerForbidden] when the caller owns the item (rule 3),
     *   [CancelResult.NotBooker] when an existing booking belongs to a different user,
     *   [CancelResult.Ok] when the caller's booking was removed or no booking existed (idempotent).
     */
    suspend fun cancel(itemId: WishlistItemId, callerId: UserId): CancelResult {
        val ownerId = ownerOf(itemId) ?: return CancelResult.ItemNotFound
        if (ownerId == callerId) return CancelResult.OwnerForbidden
        val booking = bookingRepo.getByItemId(itemId) ?: return CancelResult.Ok
        if (booking.userId != callerId) return CancelResult.NotBooker
        bookingRepo.deleteById(booking.id)
        return CancelResult.Ok
    }
}

/** Outcome of [BookingService.getState]. */
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

/** Outcome of [BookingService.book]. */
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

/** Outcome of [BookingService.cancel]. */
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
