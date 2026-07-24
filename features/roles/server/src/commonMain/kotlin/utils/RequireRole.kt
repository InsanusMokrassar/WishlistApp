package dev.inmo.wishlist.features.roles.server.utils

import dev.inmo.kroles.repos.BaseRoleSubject
import dev.inmo.kroles.repos.ReadRolesRepo
import dev.inmo.kroles.roles.BaseRole
import dev.inmo.wishlist.features.auth.server.utils.getCallerUserIdOrAnswerUnauthorized
import dev.inmo.wishlist.features.roles.common.FeatureRolesRegistry
import dev.inmo.wishlist.features.roles.common.FunctionalityId
import dev.inmo.wishlist.features.users.common.models.UserId
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.RoutingContext

/**
 * Pure allow/deny decision behind [requireRole], extracted from any Ktor coupling so it is directly
 * unit-testable (this repo has no precedent for constructing a fake [RoutingContext] — see
 * `roles/README.md` Architecture Notes). Denies (`false`) when [functionalityId] has no registered
 * requirement (fail-closed on a typo) or when [callerId] lacks the registered role.
 *
 * @param registry Registry resolving [functionalityId] to its required role.
 * @param functionalityId Gated capability looked up in [registry].
 * @param callerId Identity being checked.
 * @param rolesRepo Source of truth for role grants.
 * @return `true` when [functionalityId] is registered and [callerId] holds the required role.
 */
internal suspend fun isRoleRequirementSatisfied(
    registry: FeatureRolesRegistry,
    functionalityId: FunctionalityId,
    callerId: UserId,
    rolesRepo: ReadRolesRepo
): Boolean {
    val requiredRole: BaseRole = registry.requiredRole(functionalityId) ?: return false
    return rolesRepo.contains(BaseRoleSubject.Direct(callerId.long.toString()), requiredRole)
}

/**
 * Route-guard suspend function analogous to [getCallerUserIdOrAnswerUnauthorized]: resolves the
 * caller, then delegates the allow/deny decision to [isRoleRequirementSatisfied]. Responds `403
 * Forbidden` and returns `null` when denied; otherwise returns the caller's [UserId] so callers can
 * chain further logic, mirroring `AdminRoutingsConfigurator.requireAdmin()`'s existing `UserId?`
 * return shape.
 *
 * No production route calls this yet in issue #68's scope — point 8's three replacements call
 * `SimpleRolesFeature.isSuperAdmin` directly instead (see `roles/README.md` Architecture Notes for
 * why); this establishes a tested, ready-to-use guard for the next role-gated route.
 *
 * @param functionalityId Gated capability looked up in [registry].
 * @param registry Registry resolving [functionalityId] to its required role.
 * @param rolesRepo Source of truth for role grants.
 * @return Caller's [UserId] when allowed; `null` after responding 401/403.
 */
suspend fun RoutingContext.requireRole(
    functionalityId: FunctionalityId,
    registry: FeatureRolesRegistry,
    rolesRepo: ReadRolesRepo
): UserId? {
    val callerId = getCallerUserIdOrAnswerUnauthorized() ?: return null
    if (!isRoleRequirementSatisfied(registry, functionalityId, callerId, rolesRepo)) {
        call.respond(HttpStatusCode.Forbidden)
        return null
    }
    return callerId
}
