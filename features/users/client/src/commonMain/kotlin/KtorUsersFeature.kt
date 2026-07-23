package dev.inmo.wishlist.features.users.client

import dev.inmo.wishlist.features.users.common.Constants
import dev.inmo.wishlist.features.users.common.models.UsersFeatureUser
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

/**
 * Ktor HTTP client implementation of [UsersFeature].
 *
 * @param client Preconfigured [HttpClient] from the common client plugin.
 */
class KtorUsersFeature(
    private val client: HttpClient
) : UsersFeature {
    override suspend fun getAll(): List<UsersFeatureUser> =
        client.get("${Constants.usersPrefixPathPart}/${Constants.usersGetAllPathPart}").body()
}
