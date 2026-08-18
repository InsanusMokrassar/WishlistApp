package dev.inmo.wishlist.features.roles.server

import dev.inmo.kroles.repos.RolesRepo
import dev.inmo.wishlist.features.auth.server.UserRoleAuthorization
import dev.inmo.wishlist.features.users.common.models.UserId

/**
 * Roles-owned implementation of Auth's narrow direct approved-user authorization port.
 *
 * @param rolesRepo Role graph used for synchronized direct membership transitions and checks.
 */
class RolesUserRoleAuthorization(
    private val rolesRepo: RolesRepo,
) : UserRoleAuthorization {
    /** Grants optional-registration membership and confirms the final direct role state. */
    override suspend fun ensureUserRole(userId: UserId): Boolean = ensureDirectUserRole(rolesRepo, userId)

    /** Reads current direct approved-user membership without accepting inherited or other roles. */
    override suspend fun hasUserRole(userId: UserId): Boolean = hasDirectUserRole(rolesRepo, userId)
}
