package dev.inmo.wishlist.features.auth.server

import dev.inmo.wishlist.features.users.common.models.UserId

/**
 * Auth-owned port for current direct `User`-role checks and optional-registration approval.
 *
 * The interface deliberately exposes no Roles implementation types, preserving the one-way
 * dependency from Roles to Auth.
 */
interface UserRoleAuthorization {
    /**
     * Ensures an optionally registered account has direct approved-user membership.
     *
     * @return `true` only when direct approved-user membership is confirmed.
     */
    suspend fun ensureUserRole(userId: UserId): Boolean

    /**
     * Checks current direct approved-user membership.
     *
     * @return `true` only when direct approved-user membership is present now.
     */
    suspend fun hasUserRole(userId: UserId): Boolean
}
