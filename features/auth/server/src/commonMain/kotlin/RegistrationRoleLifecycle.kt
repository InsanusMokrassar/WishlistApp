package dev.inmo.wishlist.features.auth.server

import dev.inmo.wishlist.features.users.common.models.UserId

/**
 * Registration-specific role transition boundary implemented by the feature that owns role data.
 */
interface RegistrationRoleLifecycle {
    /**
     * Moves a provisional self-registration into the pending-verification role state.
     *
     * @param userId Provisional account being reserved.
     * @return `true` when the pending state is established; `false` when registration must roll back.
     */
    suspend fun markPending(userId: UserId): Boolean

    /**
     * Removes every direct role left by a failed provisional registration.
     *
     * @param userId Account whose role state must be compensated.
     */
    suspend fun removeRoles(userId: UserId)
}
