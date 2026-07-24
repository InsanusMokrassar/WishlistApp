package dev.inmo.wishlist.features.roles.common

/**
 * Shared URL path segment constants for the `roles` feature's HTTP surface, consumed by both the
 * server routing configurator and the client Ktor realization so path strings never drift.
 */
object RolesConstants {
    /** Root path segment for all `roles` routes: `/roles` (served under the global `/api` prefix). */
    const val prefixPathPart = "roles"

    /** Path segment for the functionality-availability probe: `/roles/isFunctionalityAvailable/{functionalityId}`. */
    const val isFunctionalityAvailablePathPart = "isFunctionalityAvailable"

    /** Name of the path parameter carrying the [FunctionalityId] string. */
    const val functionalityIdParameter = "functionalityId"
}
