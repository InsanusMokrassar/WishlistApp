package dev.inmo.wishlist.features.roles.common

import dev.inmo.kroles.roles.BaseRole
import kotlinx.serialization.Serializable

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
     * `@Serializable`, and registered polymorphic-to-`Any` in `roles/common` `Plugin.setupDI`.
     *
     * @property functionalityId Gated capability (declared in its owning feature's `Constants`).
     * @property role [BaseRole] a caller must hold to access [functionalityId].
     */
    @Serializable
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
