package dev.inmo.wishlist.features.simpleRoles.client

/**
 * Client-side capability exposing exactly one narrow check (issue #68 point 7): whether the currently
 * authenticated caller holds the SuperAdmin role. No `UserId` parameter — the server resolves the
 * caller from the bearer token, mirroring every other authenticated client call in this app.
 */
interface SimpleRolesFeature {
    /**
     * Returns whether the currently authenticated caller holds the SuperAdmin role.
     *
     * @return `true` when authenticated as SuperAdmin; `false` otherwise, including when anonymous or
     *   on any request failure (both realizations fail closed — see [KtorSimpleRolesFeature] and
     *   [CacheSimpleRolesFeature]).
     */
    suspend fun isSuperAdmin(): Boolean
}
