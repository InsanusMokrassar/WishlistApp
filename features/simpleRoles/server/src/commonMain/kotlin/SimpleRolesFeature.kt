package dev.inmo.wishlist.features.simpleRoles.server

import dev.inmo.wishlist.features.users.common.models.UserId

/**
 * Server-side capability exposing exactly one narrow check (issue #68 point 7): whether a given user
 * currently holds the SuperAdmin role. Deliberately does not expose any broader role-graph read/write
 * surface — see `roles/README.md` Architecture Notes for why the general
 * [dev.inmo.kroles.repos.RolesRepo] stays internal to `features/roles`.
 */
interface SimpleRolesFeature {
    /**
     * Returns whether [userId] currently holds the SuperAdmin role.
     *
     * @param userId Identity being checked.
     * @return `true` when [userId] holds SuperAdmin; `false` otherwise, including when [userId] is
     *   unknown to the roles repo.
     */
    suspend fun isSuperAdmin(userId: UserId): Boolean
}
