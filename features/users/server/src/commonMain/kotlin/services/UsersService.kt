package dev.inmo.wishlist.features.users.server.services

import dev.inmo.wishlist.features.users.common.models.UsersFeatureUser
import dev.inmo.wishlist.features.users.common.models.asUsersFeatureUser
import dev.inmo.wishlist.features.users.common.repo.ReadUsersRepo
import dev.inmo.wishlist.features.users.server.UsersFeature

/**
 * Default [UsersFeature] implementation backed by [ReadUsersRepo].
 *
 * @param usersRepo Source of registered users.
 */
class UsersService(
    private val usersRepo: ReadUsersRepo
) : UsersFeature {
    override suspend fun getAll(): List<UsersFeatureUser> =
        usersRepo.getAll().values.map { it.asUsersFeatureUser() }
}
