package dev.inmo.wishlist.features.admin.server

import dev.inmo.micro_utils.repos.create
import dev.inmo.micro_utils.repos.deleteById
import dev.inmo.wishlist.features.admin.common.models.NewUserWithPassword
import dev.inmo.wishlist.features.auth.server.services.AuthFeatureService
import dev.inmo.wishlist.features.users.common.models.NewUser
import dev.inmo.wishlist.features.users.common.models.RegisteredUser
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.users.common.repo.UsersRepo

class UsersManagementFeature(
    private val usersRepo: UsersRepo,
    private val authService: AuthFeatureService
) {
    suspend fun getAll(): List<RegisteredUser> =
        usersRepo.getAll().values.toList()

    suspend fun create(newUserWithPassword: NewUserWithPassword): RegisteredUser? {
        val user = usersRepo.create(NewUser(newUserWithPassword.username)).firstOrNull() ?: return null
        authService.setPassword(user.id, newUserWithPassword.password)
        return user
    }

    suspend fun update(id: UserId, newUser: NewUser): Boolean? {
        if (!usersRepo.contains(id)) return null
        return usersRepo.update(id, newUser) != null
    }

    suspend fun delete(id: UserId): Boolean? {
        if (!usersRepo.contains(id)) return null
        usersRepo.deleteById(id)
        return true
    }
}
