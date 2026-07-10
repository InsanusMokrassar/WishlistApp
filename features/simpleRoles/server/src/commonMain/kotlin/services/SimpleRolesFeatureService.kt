package dev.inmo.wishlist.features.simpleRoles.server.services

import dev.inmo.kroles.repos.BaseRoleSubject
import dev.inmo.kroles.repos.ReadRolesRepo
import dev.inmo.wishlist.features.roles.common.SuperAdminRole
import dev.inmo.wishlist.features.simpleRoles.server.SimpleRolesFeature
import dev.inmo.wishlist.features.users.common.models.UserId

/**
 * Server-side [SimpleRolesFeature] implementation delegating to `features/roles`' [ReadRolesRepo].
 *
 * @param rolesRepo Read-only view of the role graph; only [ReadRolesRepo.contains] is used, so this
 *   class depends on the narrowest interface that can satisfy it.
 */
class SimpleRolesFeatureService(
    private val rolesRepo: ReadRolesRepo
) : SimpleRolesFeature {
    /**
     * Checks [userId] against the SuperAdmin role directly.
     *
     * @param userId Identity being checked.
     * @return `true` when [userId] holds SuperAdmin; `false` otherwise, including when [userId] is
     *   unknown to [rolesRepo].
     */
    override suspend fun isSuperAdmin(userId: UserId): Boolean =
        rolesRepo.contains(BaseRoleSubject.Direct(userId.long.toString()), SuperAdminRole)
}
