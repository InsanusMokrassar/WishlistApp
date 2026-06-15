package dev.inmo.wishlist.features.email.client

import dev.inmo.wishlist.features.email.common.models.Email

/**
 * Client-side e-mail capability backed by the server email endpoints.
 */
interface EmailFeature {
    /**
     * Asks the server to send a test e-mail (root-only on the server side).
     *
     * @param to Destination address.
     * @return `true` when the server reports the message was sent.
     */
    suspend fun sendTestEmail(to: Email): Boolean

    /**
     * Sets or clears the caller's own stored e-mail address.
     *
     * @param email New address, or `null` to clear it.
     * @return `true` when the server accepted the change.
     */
    suspend fun setMyEmail(email: Email?): Boolean
}
