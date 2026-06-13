package dev.inmo.wishlist.features.users.server

import dev.inmo.wishlist.features.users.common.models.RegisteredUser

/**
 * Public-facing read-only feature exposing the registered users list.
 *
 * Used by the main page on the client to render the global list of users.
 * No auth required at the route layer.
 */
interface UsersFeature {
    /**
     * Returns all users registered in the application.
     *
     * @return Full list of [RegisteredUser]; empty when no user has been registered.
     */
    suspend fun getAll(): List<RegisteredUser>
}
