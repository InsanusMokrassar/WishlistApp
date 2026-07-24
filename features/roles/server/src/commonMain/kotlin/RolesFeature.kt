package dev.inmo.wishlist.features.roles.server

import dev.inmo.wishlist.features.roles.common.FunctionalityId
import dev.inmo.wishlist.features.users.common.models.UserId

/**
 * Server-side roles capability: whether a given user may access a role-gated functionality. Generic
 * replacement for the removed `simpleRoles` `isSuperAdmin` check — resolves the role required by
 * [functionalityId] through the `FeatureRolesRegistry` and tests the user against it.
 */
interface RolesFeature {
    /**
     * Returns whether [userId] may access [functionalityId] — i.e. holds the role that
     * [functionalityId]'s registered requirement demands.
     *
     * @param userId Identity being checked.
     * @param functionalityId Gated capability being checked.
     * @return `true` when [functionalityId] is registered and [userId] holds the required role;
     *   `false` otherwise (unregistered functionality, or the role is not held).
     */
    suspend fun isFunctionalityAvailable(userId: UserId, functionalityId: FunctionalityId): Boolean
}
