package dev.inmo.wishlist.features.roles.client

import dev.inmo.wishlist.features.roles.common.FunctionalityId

/**
 * Client-side roles capability: whether the currently authenticated caller may access a role-gated
 * functionality. No `UserId` parameter — the server resolves the caller from the bearer token,
 * mirroring every other authenticated client call in this app. Generic replacement for the removed
 * `simpleRoles` feature's client `isSuperAdmin` check.
 */
interface RolesFeature {
    /**
     * Returns whether the authenticated caller may access [functionalityId].
     *
     * @param functionalityId Gated capability being checked.
     * @return `true` when available; `false` otherwise, including when anonymous or on any request
     *   failure (the realization fails closed — see [KtorRolesFeature]).
     */
    suspend fun isFunctionalityAvailable(functionalityId: FunctionalityId): Boolean
}
