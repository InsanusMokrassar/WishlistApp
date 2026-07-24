package dev.inmo.wishlist.features.roles.common

import dev.inmo.kroles.roles.BaseRole

/**
 * In-memory [FeatureRolesRegistry] built from the [requirements] collected out of Koin (via
 * `getAllDistinct<FeatureRolesRegistry.Requirement>()`). Folds the contributions into a single
 * `FunctionalityId -> Requirement` map at construction, failing fast if two requirements disagree on
 * the role for one [FunctionalityId]. Exact-duplicate requirements are harmless — already collapsed by
 * `getAllDistinct` — so re-declaring the same requirement in two places is safe.
 *
 * @param requirements All functionality→role requirements contributed across the app.
 * @throws IllegalStateException when two requirements assign different roles to the same
 *   [FunctionalityId].
 */
class MapFeatureRolesRegistry(
    requirements: List<FeatureRolesRegistry.Requirement>
) : FeatureRolesRegistry {
    private val requirementsByFunctionalityId: Map<FunctionalityId, FeatureRolesRegistry.Requirement> =
        buildMap {
            requirements.forEach { requirement ->
                val existing = put(requirement.functionalityId, requirement)
                check(existing == null || existing.role == requirement.role) {
                    "Functionality '${requirement.functionalityId.string}' already required role " +
                        "'${existing?.role?.plain}', cannot also require '${requirement.role.plain}'"
                }
            }
        }

    override fun requiredRole(functionalityId: FunctionalityId): BaseRole? =
        requirementsByFunctionalityId[functionalityId]?.role
}
