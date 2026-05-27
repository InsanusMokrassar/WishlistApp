package dev.inmo.wishlist.features.admin.client

import dev.inmo.wishlist.features.admin.common.models.NewUserWithPassword
import dev.inmo.wishlist.features.users.common.models.NewUser
import dev.inmo.wishlist.features.users.common.models.RegisteredUser
import dev.inmo.wishlist.features.users.common.models.UserId

interface UsersManagementFeature {
    suspend fun getAll(): List<RegisteredUser>
    suspend fun getById(id: UserId): RegisteredUser?
    suspend fun create(newUser: NewUserWithPassword): RegisteredUser?
    suspend fun update(id: UserId, newUser: NewUser): Boolean
    suspend fun delete(id: UserId): Boolean
}
