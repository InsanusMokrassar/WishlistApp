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
