package dev.inmo.wishlist.features.roles.server

import dev.inmo.kroles.repos.RolesRepo
import dev.inmo.wishlist.features.auth.server.RegistrationRoleLifecycle
import dev.inmo.wishlist.features.users.common.models.UserId

/**
 * Roles-owned implementation of provisional registration transitions.
 *
 * @param rolesRepo Role graph mutated by registration and compensation.
 */
class RolesRegistrationRoleLifecycle(
    private val rolesRepo: RolesRepo,
) : RegistrationRoleLifecycle {
    /** Establishes the synchronized pending-role state for a provisional account. */
    override suspend fun markPending(userId: UserId): Boolean =
        markNewUserPending(rolesRepo, userId)

    /** Removes all direct roles under the same synchronization used by creation and promotion. */
    override suspend fun removeRoles(userId: UserId) {
        removeDirectUserRoles(rolesRepo, userId)
    }
}
