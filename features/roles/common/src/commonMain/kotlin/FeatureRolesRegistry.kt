package dev.inmo.wishlist.features.roles.common

import dev.inmo.kroles.roles.BaseRole

/**
 * Central registry mapping a symbolic feature/capability id to the [BaseRole] required to access it
 * (issue #68 point 4's "aggregator of features and required role for them"). Every role-gated
 * capability's requirement is recorded here — currently populated from [Plugin.setupDI] — so "what
 * requires what" lives in one inspectable place, consulted by the route-guard helper
 * ([dev.inmo.wishlist.features.roles.server.utils.requireRole]). See `roles/README.md` Architecture
 * Notes for why this registry currently has real data but the guard helper has no production caller
 * yet — the three concrete privilege-check replacements this issue makes (`admin`, `email`, `files`)
 * call `SimpleRolesFeature.isSuperAdmin` directly instead, per the issue's own literal text.
 */
object FeatureRolesRegistry {
    private val requirements = mutableMapOf<String, BaseRole>()

    /**
     * Registers that [featureId] requires [role]. Idempotent for re-registration with the *same*
     * role (safe to call more than once, e.g. from a `setupDI` that could run more than once in
     * tests); throws on a conflicting re-registration — two features must never silently disagree on
     * one id's required role.
     *
     * @param featureId Symbolic id of the gated capability (see [RoleGatedFeatureIds]).
     * @param role Role required to access [featureId].
     * @throws IllegalStateException when [featureId] is already registered with a different role.
     */
    fun register(featureId: String, role: BaseRole) {
        val existing = requirements[featureId]
        check(existing == null || existing == role) {
            "Feature '$featureId' already registered with role '${existing?.plain}', " +
                "cannot re-register with '${role.plain}'"
        }
        requirements[featureId] = role
    }

    /**
     * Looks up the role required to access [featureId].
     *
     * @param featureId Symbolic id previously passed to [register].
     * @return The required [BaseRole], or `null` when [featureId] was never registered — treated as
     *   "deny" by [dev.inmo.wishlist.features.roles.server.utils.requireRole] (fail-closed on a typo).
     */
    fun requiredRole(featureId: String): BaseRole? = requirements[featureId]
}

/**
 * Symbolic feature ids registered against [FeatureRolesRegistry]. One `const val` per gated
 * capability, named after the capability rather than the file/class that happens to enforce it today,
 * so the id survives future refactors of the enforcing code.
 */
object RoleGatedFeatureIds {
    /** The whole `/admin/...` route surface (`AdminRoutingsConfigurator`). */
    const val adminPanel = "admin.panel"

    /** Changing another user's avatar via `PUT /files/avatar/{userId}` (`FilesRoutingsConfigurator`). */
    const val filesAvatarChangeForOthers = "files.avatarChangeForOthers"

    /** Sending a test email via `POST /email/sendTest` (`EmailFeatureService.sendTestEmail`). */
    const val emailSendTest = "email.sendTest"
}
