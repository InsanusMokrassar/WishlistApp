package dev.inmo.wishlist.features.booking.common.repo

import dev.inmo.micro_utils.coroutines.SmartRWLocker
import dev.inmo.micro_utils.repos.KeyValueRepo
import dev.inmo.micro_utils.repos.MapKeyValueRepo
import dev.inmo.micro_utils.repos.cache.full.FullCRUDCacheRepo
import dev.inmo.wishlist.features.booking.common.models.BookingId
import dev.inmo.wishlist.features.booking.common.models.NewBooking
import dev.inmo.wishlist.features.booking.common.models.RegisteredBooking
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.wishlist.common.models.WishlistItemId
import kotlinx.coroutines.CoroutineScope

/**
 * In-memory caching layer over a [BookingRepo] delegate.
 *
 * Wraps [FullCRUDCacheRepo] to keep a local map of all bookings synced with the underlying
 * [originalRepo]. [getByItemId] and [getByUserId] always delegate to [originalRepo] because the
 * cache is keyed by [BookingId] and does not index by item id or user id.
 *
 * @param originalRepo Backing persistent repository.
 * @param scope Coroutine scope used for cache synchronisation flows.
 * @param kvCache Key-value store for the in-memory cache; defaults to [MapKeyValueRepo].
 * @param locker Read-write locker used to guard concurrent cache access.
 */
class CacheBookingRepo(
    private val originalRepo: BookingRepo,
    scope: CoroutineScope,
    kvCache: KeyValueRepo<BookingId, RegisteredBooking> = MapKeyValueRepo(),
    locker: SmartRWLocker = SmartRWLocker()
) : BookingRepo, FullCRUDCacheRepo<RegisteredBooking, BookingId, NewBooking>(
    crudRepo = originalRepo,
    kvCache = kvCache,
    scope = scope,
    skipStartInvalidate = false,
    locker = locker,
    idGetter = RegisteredBooking::id
) {
    /**
     * Delegates directly to [originalRepo] because the flat cache cannot filter by item id.
     *
     * @param itemId Item whose booking to resolve.
     * @return The active booking from the persistent store, or `null` when the item is not booked.
     */
    override suspend fun getByItemId(itemId: WishlistItemId): RegisteredBooking? =
        originalRepo.getByItemId(itemId)

    /**
     * Delegates directly to [originalRepo] because the flat cache cannot filter by user id.
     *
     * @param userId Booker whose bookings to list.
     * @return Bookings owned by [userId] from the persistent store.
     */
    override suspend fun getByUserId(userId: UserId): List<RegisteredBooking> =
        originalRepo.getByUserId(userId)
}
