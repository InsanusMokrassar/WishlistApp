package dev.inmo.wishlist.features.users.common

/**
 * Shared URL path constants for the public users feature.
 *
 * Reused by both [dev.inmo.wishlist.features.users.server.configurators.UsersRoutingsConfigurator]
 * (server side) and the Ktor client implementation (client side) to avoid out-of-sync strings.
 */
object Constants {
    /** Top-level route prefix for the users feature. */
    const val usersPrefixPathPart = "users"

    /** Subpath returning all registered users. */
    const val usersGetAllPathPart = "getAll"
}
