package dev.inmo.wishlist.features.users.common.repo

import dev.inmo.micro_utils.repos.ReadCRUDRepo
import dev.inmo.wishlist.features.users.common.models.RegisteredUser
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.users.common.models.Username

interface ReadUsersRepo : ReadCRUDRepo<RegisteredUser, UserId> {
    suspend fun getUserByUsername(username: Username): RegisteredUser?

    /** Reads the current backing value, bypassing any cache when an implementation has one. */
    suspend fun getByIdFresh(id: UserId): RegisteredUser? = getById(id)
}
