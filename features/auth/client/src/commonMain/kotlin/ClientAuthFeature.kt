package dev.inmo.wishlist.features.auth.client

import dev.inmo.wishlist.features.auth.common.AuthFeature
import dev.inmo.wishlist.features.auth.common.models.AuthFeatureUser

/**
 * Client-side auth capability: the shared [AuthFeature] plus session-only operations
 * ([logout]) and the authenticated caller's own record ([getMe]).
 */
interface ClientAuthFeature : AuthFeature {
    /** Logs the current session out, invalidating its access/refresh tokens server-side. */
    suspend fun logout()

    /**
     * Returns the authenticated caller's own record.
     *
     * @return An [AuthFeatureUser] describing the current caller, or `null` when nobody is
     *   authenticated (no/invalid session).
     */
    suspend fun getMe(): AuthFeatureUser?
}
