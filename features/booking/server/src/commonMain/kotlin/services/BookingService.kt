package dev.inmo.wishlist.features.booking.server.services

import dev.inmo.micro_utils.coroutines.SmartRWLocker
import dev.inmo.micro_utils.coroutines.withReadAcquire
import dev.inmo.micro_utils.coroutines.withWriteLock
import dev.inmo.micro_utils.repos.create
import dev.inmo.micro_utils.repos.deleteById
import dev.inmo.wishlist.features.booking.common.models.BookResult
import dev.inmo.wishlist.features.booking.common.models.BookingResult
import dev.inmo.wishlist.features.booking.common.models.BookingState
import dev.inmo.wishlist.features.booking.common.models.CancelResult
import dev.inmo.wishlist.features.booking.common.models.NewBooking
import dev.inmo.wishlist.features.booking.common.repo.BookingRepo
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.wishlist.common.models.RegisteredWishlistItem
import dev.inmo.wishlist.features.wishlist.common.models.WishlistItemId
import dev.inmo.wishlist.features.wishlist.common.repo.WishlistItemRepo
import dev.inmo.wishlist.features.wishlist.common.repo.WishlistRepo

/**
 * Server-side service that owns all booking (gift reservation) business rules.
 *
 * Enforces every visibility/authorization rule of issue #29 on the server side:
 * 1. Only authorized users reach this service (the routing layer wraps all booking routes in
 *    Ktor `authenticate { }`; this service is never invoked for anonymous callers).
 * 2. Other users can observe only whether an item is booked, never WHO booked it: results carry
 *    no booker identity, only the sealed [BookingState]. [myPresents] returns the CALLER's own
 *    booked items (the caller is the booker), so no other booker's identity is leaked.
 * 3. The item OWNER is fully cut off from booking state: every per-item operation returns
 *    [BookingResult.OwnerForbidden] / [BookResult.OwnerForbidden] / [CancelResult.OwnerForbidden]
 *    when the caller owns the parent wishlist, so an owner never learns whether an item is booked.
 * 4. An item can be booked by at most one user: [tryBook] rejects when a booking already exists.
 *    Defense-in-depth: an in-process [SmartRWLocker] serialises all mutations (so the check-then-create
 *    of [tryBook] is atomic against concurrent in-process callers) AND the underlying repository's
 *    unique item-id index rejects any duplicate that races past the locker (e.g. multi-instance).
 *
 * Concurrency: [getState] runs under the locker's read acquire; [tryBook] and [cancel] run under the
 * write lock, so the single-active-booking invariant holds in-process, not only at the DB index.
 *
 * @param bookingRepo Repository storing the single active booking per item.
 * @param wishlistItemRepo Repository used to resolve an item's parent wishlist and to materialize
 *   booked items for [myPresents].
 * @param wishlistRepo Repository used to resolve the parent wishlist owner.
 */
class BookingService(
    private val bookingRepo: BookingRepo,
    private val wishlistItemRepo: WishlistItemRepo,
    private val wishlistRepo: WishlistRepo
) {
    /** Guards the single-active-booking invariant in-process: reads acquire, mutations write-lock. */
    private val locker = SmartRWLocker()

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
     * Runs under the locker's read acquire so the observed state is consistent with concurrent
     * mutations.
     *
     * @param itemId Item whose booking state to read.
     * @param callerId Authenticated caller resolved from the request context.
     * @return [BookingResult.ItemNotFound] when the item/parent is missing,
     *   [BookingResult.OwnerForbidden] when the caller owns the item (rule 3 — owner hidden),
     *   otherwise [BookingResult.State] carrying a booker-anonymous [BookingState]
     *   ([BookingState.Free] / [BookingState.Booked] / [BookingState.BookedByMe]).
     */
    suspend fun getState(itemId: WishlistItemId, callerId: UserId): BookingResult = locker.withReadAcquire {
        val ownerId = ownerOf(itemId) ?: return@withReadAcquire BookingResult.ItemNotFound
        if (ownerId == callerId) return@withReadAcquire BookingResult.OwnerForbidden
        val booking = bookingRepo.getByItemId(itemId)
        val state = when {
            booking == null -> BookingState.Free
            booking.userId == callerId -> BookingState.BookedByMe
            else -> BookingState.Booked
        }
        BookingResult.State(state)
    }

    /**
     * Reserves [itemId] for [callerId] if permitted and not already booked.
     *
     * Runs under the locker's write lock so the existence check and the insert are atomic against
     * other in-process callers (rule 4).
     *
     * @param itemId Item to reserve.
     * @param callerId Authenticated caller placing the booking.
     * @return [BookResult.ItemNotFound] when the item/parent is missing,
     *   [BookResult.OwnerForbidden] when the caller owns the item (rule 3),
     *   [BookResult.AlreadyBooked] when another booking already exists (rule 4),
     *   [BookResult.Ok] on success.
     */
    suspend fun tryBook(itemId: WishlistItemId, callerId: UserId): BookResult = locker.withWriteLock {
        val ownerId = ownerOf(itemId) ?: return@withWriteLock BookResult.ItemNotFound
        if (ownerId == callerId) return@withWriteLock BookResult.OwnerForbidden
        if (bookingRepo.getByItemId(itemId) != null) return@withWriteLock BookResult.AlreadyBooked
        try {
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
     * Runs under the locker's write lock so the booking lookup and deletion are atomic against other
     * in-process callers.
     *
     * @param itemId Item whose booking to cancel.
     * @param callerId Authenticated caller cancelling the booking.
     * @return [CancelResult.ItemNotFound] when the item/parent is missing,
     *   [CancelResult.OwnerForbidden] when the caller owns the item (rule 3),
     *   [CancelResult.NotBooker] when an existing booking belongs to a different user,
     *   [CancelResult.Ok] when the caller's booking was removed or no booking existed (idempotent).
     */
    suspend fun cancel(itemId: WishlistItemId, callerId: UserId): CancelResult = locker.withWriteLock {
        val ownerId = ownerOf(itemId) ?: return@withWriteLock CancelResult.ItemNotFound
        if (ownerId == callerId) return@withWriteLock CancelResult.OwnerForbidden
        val booking = bookingRepo.getByItemId(itemId) ?: return@withWriteLock CancelResult.Ok
        if (booking.userId != callerId) return@withWriteLock CancelResult.NotBooker
        bookingRepo.deleteById(booking.id)
        CancelResult.Ok
    }

    /**
     * Lists all wishlist items [callerId] has booked — the presents the caller plans to make.
     *
     * Returns only the caller's own bookings, so no other booker's identity is exposed (rule 2).
     * Items whose backing wishlist item no longer exists are skipped. Read under the locker's read
     * acquire for consistency with concurrent mutations.
     *
     * @param callerId Authenticated caller whose booked items to list.
     * @return Items the caller has reserved for gifting; empty when the caller booked nothing.
     */
    suspend fun myPresents(callerId: UserId): List<RegisteredWishlistItem> = locker.withReadAcquire {
        bookingRepo.getByUserId(callerId).mapNotNull { wishlistItemRepo.getById(it.itemId) }
    }
}
