package dev.inmo.wishlist.features.auth.server

import dev.inmo.wishlist.features.users.common.models.RegisteredUser

/**
 * Server-side hook used by auth registration to deliver an approval message for a newly created
 * account.
 */
fun interface RegistrationEmailSender {
    /**
     * Sends the verification invite for [user].
     *
     * @param user Newly persisted account, including the submitted email address.
     * @return `true` when the invite was accepted for delivery; `false` when delivery is unavailable
     *   or failed.
     */
    suspend fun sendRegistrationEmail(user: RegisteredUser): Boolean
}

/**
 * Request-local compensation for an invite artifact delivered during required-email registration.
 */
fun interface RegistrationEmailDeliveryHandle {
    /** Removes the exact delivery artifact owned by the originating registration request. */
    suspend fun rollback()
}

/**
 * Optional extension of [RegistrationEmailSender] that returns request-local rollback ownership
 * after successful delivery while preserving the legacy Boolean sender contract.
 */
interface CompensableRegistrationEmailSender : RegistrationEmailSender {
    /**
     * Sends an invite and returns its exact rollback handle only after successful delivery.
     *
     * @param user Newly persisted account receiving the verification invite.
     * @return Request-local delivery handle, or `null` when delivery is unavailable or fails.
     */
    suspend fun sendRegistrationEmailWithCompensation(
        user: RegisteredUser,
    ): RegistrationEmailDeliveryHandle?
}
