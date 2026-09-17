package dev.inmo.wishlist.features.users.common.repo

import dev.inmo.micro_utils.coroutines.SmartRWLocker
import dev.inmo.micro_utils.repos.KeyValueRepo
import dev.inmo.micro_utils.repos.MapKeyValueRepo
import dev.inmo.micro_utils.repos.cache.CRUDCacheRepo
import dev.inmo.micro_utils.repos.cache.cache.KVCache
import dev.inmo.micro_utils.repos.cache.full.FullCRUDCacheRepo
import dev.inmo.micro_utils.repos.cache.full.FullKeyValueCacheRepo
import dev.inmo.micro_utils.coroutines.withWriteLock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.users.common.models.NewUser
import dev.inmo.wishlist.features.users.common.models.RegisteredUser
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.users.common.models.Username

/**
 * [FullCRUDCacheRepo]-backed [UsersRepo] wrapper around [originalRepo].
 *
 * Reads are served from an in-memory cache (pre-filled from [originalRepo] on startup since
 * `skipStartInvalidate = false`); writes delegate to [originalRepo] and update the cache only on
 * a successful, non-throwing result. In particular, [FullCRUDCacheRepo]'s write wrapper does
 * **not** catch exceptions from [originalRepo] — a
 * [dev.inmo.wishlist.features.users.common.repo.exceptions.DuplicateUserFieldException] thrown by
 * the JVM `ExposedUsersRepo` on a unique-constraint violation propagates through this class
 * unchanged, and the cache is left untouched (no partial/incorrect write is ever cached).
 *
 * @param originalRepo Backing [UsersRepo], normally the JVM-only Exposed implementation.
 * @param scope Coroutine scope the cache uses for its background invalidation subscriptions.
 * @param kvCache In-memory key-value store backing the cache; defaults to a plain [MapKeyValueRepo].
 * @param locker Read/write lock guarding concurrent cache access.
 */
class CacheUsersRepo(
    private val originalRepo: UsersRepo,
    scope: CoroutineScope,
    kvCache: KeyValueRepo<UserId, RegisteredUser> = MapKeyValueRepo<UserId, RegisteredUser>(),
    locker: SmartRWLocker = SmartRWLocker()
) : UsersRepo, FullCRUDCacheRepo<RegisteredUser, UserId, NewUser>(
    crudRepo = originalRepo,
    kvCache = kvCache,
    scope = scope,
    skipStartInvalidate = false,
    locker = locker,
    idGetter = RegisteredUser::id
) {
    /**
     * Looks up a user by [username] directly against [originalRepo] — bypassing the id-keyed
     * cache, which has no username index.
     *
     * @param username Login name to search for.
     * @return Matching [RegisteredUser], or `null` when not found.
     */
    override suspend fun getUserByUsername(username: Username): RegisteredUser? =
        originalRepo.getUserByUsername(username)

    /**
     * Delegates conditional address approval to [originalRepo] and synchronously mirrors a successful
     * result into the id-keyed cache.
     *
     * The backing call happens before taking [locker] because it emits its repository update event;
     * taking the cache lock first could deadlock the cache subscription. Failed conditional writes do
     * not alter cached state.
     *
     * @param id User whose current address is being approved.
     * @param expectedEmail Exact address required by the backing repository.
     * @return The approved user returned by the backing repository, or `null` on a failed condition.
     */
    override suspend fun approveEmail(id: UserId, expectedEmail: Email): RegisteredUser? {
        val approved = originalRepo.approveEmail(id, expectedEmail) ?: return null
        locker.withWriteLock {
            kvCache.set(mapOf(approved.id to approved))
        }
        return approved
    }
}
