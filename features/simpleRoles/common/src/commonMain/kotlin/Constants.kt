package dev.inmo.wishlist.features.simpleRoles.common

/**
 * Shared URL path segment constants for the `simpleRoles` feature.
 *
 * Consumed by both `simpleRoles/server`'s routing configurator and the client Ktor implementation to
 * keep path strings defined in a single place.
 */
object Constants {
    /** Root path segment for all `simpleRoles` routes: `/simpleRoles`. */
    const val prefixPathPart = "simpleRoles"

    /** Path segment for the superadmin-status probe: `/simpleRoles/isSuperAdmin`. */
    const val isSuperAdminPathPart = "isSuperAdmin"
}