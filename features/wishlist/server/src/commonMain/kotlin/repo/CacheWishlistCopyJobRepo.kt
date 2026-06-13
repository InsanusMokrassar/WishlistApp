package dev.inmo.wishlist.features.wishlist.server.repo

import dev.inmo.micro_utils.coroutines.SmartRWLocker
import dev.inmo.micro_utils.repos.KeyValueRepo
import dev.inmo.micro_utils.repos.MapKeyValueRepo
import dev.inmo.micro_utils.repos.cache.full.FullCRUDCacheRepo
import dev.inmo.wishlist.features.wishlist.common.models.NewWishlistCopyJob
import dev.inmo.wishlist.features.wishlist.common.models.RegisteredWishlistCopyJob
import dev.inmo.wishlist.features.wishlist.common.models.WishlistCopyJobId
import kotlinx.coroutines.CoroutineScope

/**
 * In-memory caching layer over a [WishlistCopyJobRepo] delegate.
 *
 * Wraps [FullCRUDCacheRepo] to keep a local map of all jobs synced with the underlying
 * [originalRepo]. [getUnfinished] always delegates to [originalRepo] because the flat cache does not
 * index by status.
 *
 * @param originalRepo Backing persistent repository.
 * @param scope Coroutine scope used for cache synchronisation flows.
 * @param kvCache Key-value store for the in-memory cache; defaults to [MapKeyValueRepo].
 * @param locker Read-write locker used to guard concurrent cache access.
 */
class CacheWishlistCopyJobRepo(
    private val originalRepo: WishlistCopyJobRepo,
    scope: CoroutineScope,
    kvCache: KeyValueRepo<WishlistCopyJobId, RegisteredWishlistCopyJob> = MapKeyValueRepo(),
    locker: SmartRWLocker = SmartRWLocker()
) : WishlistCopyJobRepo, FullCRUDCacheRepo<RegisteredWishlistCopyJob, WishlistCopyJobId, NewWishlistCopyJob>(
    crudRepo = originalRepo,
    kvCache = kvCache,
    scope = scope,
    skipStartInvalidate = false,
    locker = locker,
    idGetter = RegisteredWishlistCopyJob::id
) {
    /**
     * Delegates directly to [originalRepo] because the flat cache cannot filter by status.
     *
     * @return Unfinished jobs from the persistent store.
     */
    override suspend fun getUnfinished(): List<RegisteredWishlistCopyJob> =
        originalRepo.getUnfinished()
}
