package dev.inmo.wishlist.features.wishlist.common.repo

import dev.inmo.micro_utils.coroutines.SmartRWLocker
import dev.inmo.micro_utils.repos.KeyValueRepo
import dev.inmo.micro_utils.repos.MapKeyValueRepo
import dev.inmo.micro_utils.repos.cache.full.FullCRUDCacheRepo
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.wishlist.common.models.NewWishlist
import dev.inmo.wishlist.features.wishlist.common.models.RegisteredWishlist
import dev.inmo.wishlist.features.wishlist.common.models.WishlistId
import kotlinx.coroutines.CoroutineScope

/**
 * In-memory caching layer over a [WishlistRepo] delegate.
 *
 * Wraps [FullCRUDCacheRepo] to keep a local map of all wishlists synced with the
 * underlying [originalRepo]. [getByUserId] always delegates to [originalRepo] because
 * the cache does not index by owner.
 *
 * @param originalRepo Backing persistent repository.
 * @param scope Coroutine scope used for cache synchronisation flows.
 * @param kvCache Key-value store for the in-memory cache; defaults to [MapKeyValueRepo].
 * @param locker Read-write locker used to guard concurrent cache access.
 */
class CacheWishlistRepo(
    private val originalRepo: WishlistRepo,
    scope: CoroutineScope,
    kvCache: KeyValueRepo<WishlistId, RegisteredWishlist> = MapKeyValueRepo(),
    locker: SmartRWLocker = SmartRWLocker()
) : WishlistRepo, FullCRUDCacheRepo<RegisteredWishlist, WishlistId, NewWishlist>(
    crudRepo = originalRepo,
    kvCache = kvCache,
    scope = scope,
    skipStartInvalidate = false,
    locker = locker,
    idGetter = RegisteredWishlist::id
) {
    /**
     * Delegates directly to [originalRepo] because the flat cache cannot filter by owner.
     *
     * @param userId Owner to filter by.
     * @return Matching wishlists from the persistent store.
     */
    override suspend fun getByUserId(userId: UserId): List<RegisteredWishlist> =
        originalRepo.getByUserId(userId)
}
