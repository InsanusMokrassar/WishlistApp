package dev.inmo.wishlist.features.wishlist.common.repo

import dev.inmo.micro_utils.coroutines.SmartRWLocker
import dev.inmo.micro_utils.repos.KeyValueRepo
import dev.inmo.micro_utils.repos.MapKeyValueRepo
import dev.inmo.micro_utils.repos.cache.full.FullCRUDCacheRepo
import dev.inmo.wishlist.features.wishlist.common.models.NewWishlistItem
import dev.inmo.wishlist.features.wishlist.common.models.RegisteredWishlistItem
import dev.inmo.wishlist.features.wishlist.common.models.WishlistId
import dev.inmo.wishlist.features.wishlist.common.models.WishlistItemId
import kotlinx.coroutines.CoroutineScope

/**
 * In-memory caching layer over a [WishlistItemRepo] delegate.
 *
 * Wraps [FullCRUDCacheRepo] to keep a local map of all items synced with the
 * underlying [originalRepo]. [getByWishlistId] always delegates to [originalRepo] because
 * the cache does not index by parent wishlist.
 *
 * @param originalRepo Backing persistent repository.
 * @param scope Coroutine scope used for cache synchronisation flows.
 * @param kvCache Key-value store for the in-memory cache; defaults to [MapKeyValueRepo].
 * @param locker Read-write locker used to guard concurrent cache access.
 */
class CacheWishlistItemRepo(
    private val originalRepo: WishlistItemRepo,
    scope: CoroutineScope,
    kvCache: KeyValueRepo<WishlistItemId, RegisteredWishlistItem> = MapKeyValueRepo(),
    locker: SmartRWLocker = SmartRWLocker()
) : WishlistItemRepo, FullCRUDCacheRepo<RegisteredWishlistItem, WishlistItemId, NewWishlistItem>(
    crudRepo = originalRepo,
    kvCache = kvCache,
    scope = scope,
    skipStartInvalidate = false,
    locker = locker,
    idGetter = RegisteredWishlistItem::id
) {
    /** Same in-memory cache handed to [FullCRUDCacheRepo], kept for the cache-first [getByIds]. */
    private val cache: KeyValueRepo<WishlistItemId, RegisteredWishlistItem> = kvCache

    /**
     * Delegates directly to [originalRepo] because the flat cache cannot filter by parent wishlist.
     *
     * @param wishlistId Parent wishlist to filter by.
     * @return Matching items from the persistent store.
     */
    override suspend fun getByWishlistId(wishlistId: WishlistId): List<RegisteredWishlistItem> =
        originalRepo.getByWishlistId(wishlistId)

    /**
     * Serves ids from the in-memory [cache] first; any ids missing from the cache (e.g. not yet
     * synced) are resolved together in a single [originalRepo] batch call rather than one-by-one.
     * Result is in [ids] order with duplicates and unknown ids removed (PR #31 F6).
     *
     * @param ids Item ids to resolve; an empty list short-circuits without touching storage.
     * @return Matching items in [ids] order, missing ids omitted.
     */
    override suspend fun getByIds(ids: List<WishlistItemId>): List<RegisteredWishlistItem> {
        if (ids.isEmpty()) return emptyList()
        val distinct = ids.distinct()
        val resolved = HashMap<WishlistItemId, RegisteredWishlistItem>()
        val missing = mutableListOf<WishlistItemId>()
        distinct.forEach { id ->
            val cached = cache.get(id)
            if (cached != null) resolved[id] = cached else missing.add(id)
        }
        if (missing.isNotEmpty()) {
            originalRepo.getByIds(missing).forEach { resolved[it.id] = it }
        }
        return distinct.mapNotNull { resolved[it] }
    }
}
