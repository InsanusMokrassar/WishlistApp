package dev.inmo.wishlist.features.users.server

import dev.inmo.wishlist.features.users.common.models.UsersFeatureUser

/**
 * Public-facing read-only feature exposing the registered users list.
 *
 * Used by the main page on the client to render the global list of users.
 * No auth required at the route layer.
 */
interface UsersFeature {
    /**
     * Returns all users registered in the application, projected onto the public-facing
     * [UsersFeatureUser] model — not the persistence entity — so no field beyond `id`/`username` is
     * ever exposed to this unauthenticated route.
     *
     * @return Full list of [UsersFeatureUser]; empty when no user has been registered.
     */
    suspend fun getAll(): List<UsersFeatureUser>
}
