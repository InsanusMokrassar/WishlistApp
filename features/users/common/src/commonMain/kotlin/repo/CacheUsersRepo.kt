package dev.inmo.wishlist.features.users.common.repo

import dev.inmo.micro_utils.coroutines.SmartRWLocker
import dev.inmo.micro_utils.repos.KeyValueRepo
import dev.inmo.micro_utils.repos.MapKeyValueRepo
import dev.inmo.micro_utils.repos.cache.CRUDCacheRepo
import dev.inmo.micro_utils.repos.cache.cache.KVCache
import dev.inmo.micro_utils.repos.cache.full.FullCRUDCacheRepo
import dev.inmo.micro_utils.repos.cache.full.FullKeyValueCacheRepo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import dev.inmo.wishlist.features.users.common.models.NewUser
import dev.inmo.wishlist.features.users.common.models.RegisteredUser
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.users.common.models.Username

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
    override suspend fun getUserByUsername(username: Username): RegisteredUser? =
        originalRepo.getUserByUsername(username)
}
