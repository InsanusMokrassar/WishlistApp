package dev.inmo.wishlist.features.roles.server.services

import dev.inmo.kroles.repos.ReadRolesRepo
import dev.inmo.wishlist.features.roles.common.FeatureRolesRegistry
import dev.inmo.wishlist.features.roles.common.FunctionalityId
import dev.inmo.wishlist.features.roles.server.RolesFeature
import dev.inmo.wishlist.features.roles.server.utils.isRoleRequirementSatisfied
import dev.inmo.wishlist.features.users.common.models.UserId

/**
 * Server-side [RolesFeature] implementation delegating to the same pure decision the route guard uses
 * ([isRoleRequirementSatisfied]): resolve [functionalityId]'s required role from [registry], then test
 * [rolesRepo]. Fails closed for an unregistered functionality.
 *
 * @param registry Resolves a [FunctionalityId] to its required role.
 * @param rolesRepo Read-only view of the role graph.
 */
class RolesFeatureService(
    private val registry: FeatureRolesRegistry,
    private val rolesRepo: ReadRolesRepo
) : RolesFeature {
    override suspend fun isFunctionalityAvailable(userId: UserId, functionalityId: FunctionalityId): Boolean =
        isRoleRequirementSatisfied(registry, functionalityId, userId, rolesRepo)
}
