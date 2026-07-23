package dev.inmo.wishlist.features.auth.server

import dev.inmo.wishlist.features.auth.common.AuthFeature
import dev.inmo.wishlist.features.auth.common.models.AuthFeatureUser
import dev.inmo.wishlist.features.auth.common.models.Token

/**
 * Server-side auth capability: the shared [AuthFeature] plus session-only operations
 * ([logout]) and the token owner's own record ([getUser]).
 */
interface ServerAuthFeature : AuthFeature {
    /** Invalidates [token] and any refresh token linked to it. */
    suspend fun logout(token: Token)

    /**
     * Resolves the caller identified by [token].
     *
     * @param token Bearer access token presented by the caller.
     * @return An [AuthFeatureUser] describing the token's owner, or `null` when [token] is
     *   unknown or expired.
     */
    suspend fun getUser(token: Token): AuthFeatureUser?
}
