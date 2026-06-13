package dev.inmo.wishlist.features.users.client

import dev.inmo.wishlist.features.users.common.models.RegisteredUser

/**
 * Client-side mirror of the server [dev.inmo.wishlist.features.users.server.UsersFeature].
 *
 * Public read-only view of the registered users list. Implemented by [KtorUsersFeature].
 */
interface UsersFeature {
    /**
     * Fetches the full list of registered users from the server.
     *
     * @return All [RegisteredUser]s; empty when none registered or on network error handled upstream.
     */
    suspend fun getAll(): List<RegisteredUser>
}
