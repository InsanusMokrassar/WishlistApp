package dev.inmo.wishlist.features.roles.common

import dev.inmo.kroles.roles.BaseRole

/**
 * Aggregator of role-gated functionalities and the [BaseRole] each one requires (issue #68 point 4).
 * Every [Requirement] contributed into Koin (via [singleRequirement]) is folded into one inspectable
 * place, consulted by the route-guard helper
 * ([dev.inmo.wishlist.features.roles.server.utils.requireRole]).
 *
 * The realization ([MapFeatureRolesRegistry]) is built from the DI-collected requirements rather than
 * from a mutable process-wide singleton: a feature declares its own gate requirement from its own
 * `setupDI`, and the registry is assembled from all of them at resolution time. See `roles/README.md`
 * Architecture Notes.
 */
interface FeatureRolesRegistry {
    /**
     * One functionality→role requirement contributed into the registry. Registered into Koin with
     * [singleRequirement] and collected by [MapFeatureRolesRegistry] via `getAllDistinct`.
     *
     * @property functionalityId Gated capability (see [RoleGatedFeatureIds]).
     * @property role [BaseRole] a caller must hold to access [functionalityId].
     */
    data class Requirement(
        val functionalityId: FunctionalityId,
        val role: BaseRole
    )

    /**
     * Looks up the role required to access [functionalityId].
     *
     * @param functionalityId Gated capability to resolve.
     * @return Required [BaseRole], or `null` when [functionalityId] has no registered requirement —
     *   treated as "deny" by [dev.inmo.wishlist.features.roles.server.utils.requireRole]
     *   (fail-closed on an unregistered id / typo).
     */
    fun requiredRole(functionalityId: FunctionalityId): BaseRole?
}

/**
 * In-memory [FeatureRolesRegistry] built from the [requirements] collected out of Koin (via
 * `getAllDistinct<FeatureRolesRegistry.Requirement>()`). Folds the contributions into a single
 * `FunctionalityId -> BaseRole` map at construction, failing fast if two requirements disagree on the
 * role for one [FunctionalityId]. Exact-duplicate requirements are harmless — already collapsed by
 * `getAllDistinct` — so re-declaring the same requirement in two places is safe.
 *
 * @param requirements All functionality→role requirements contributed across the app.
 * @throws IllegalStateException when two requirements assign different roles to the same
 *   [FunctionalityId].
 */
class MapFeatureRolesRegistry(
    requirements: List<FeatureRolesRegistry.Requirement>
) : FeatureRolesRegistry {
    private val rolesByFunctionalityId: Map<FunctionalityId, BaseRole> = buildMap {
        requirements.forEach { requirement ->
            val existing = put(requirement.functionalityId, requirement.role)
            check(existing == null || existing == requirement.role) {
                "Functionality '${requirement.functionalityId.string}' already required role " +
                    "'${existing?.plain}', cannot also require '${requirement.role.plain}'"
            }
        }
    }

    override fun requiredRole(functionalityId: FunctionalityId): BaseRole? =
        rolesByFunctionalityId[functionalityId]
}
