package dev.inmo.wishlist.features.email.common

import dev.inmo.wishlist.features.email.common.models.Email

/**
 * Shared capability surface for the email feature, implemented by both the server-side
 * [dev.inmo.wishlist.features.email.server.services.SmtpEmailService] and the client-side
 * [dev.inmo.wishlist.features.email.client.KtorEmailFeature].
 *
 * When the email feature is disabled (no SMTP configuration provided), [isFeatureEnabled] returns
 * `false`, [sendTestEmail] returns `false` without attempting any delivery, and [setMyEmail]
 * is still available for persisting the address regardless of SMTP state (storage vs sending are
 * independent concerns).
 */
interface EmailFeature {

    /**
     * Returns whether SMTP delivery is configured and operational.
     *
     * @return `true` when an SMTP host is configured server-side; `false` otherwise.
     */
    suspend fun isFeatureEnabled(): Boolean

    /**
     * Sends a test email to [recipient] using the server's configured SMTP settings.
     *
     * Root-only on the server side — clients must supply a valid bearer token belonging to the
     * root user; the server enforces this via [requireRoot]. The client implementation forwards
     * the request to `POST /email/sendTest`.
     *
     * @param recipient Target email address to deliver the test message to.
     * @return `true` when the message was accepted by the SMTP server; `false` when the feature
     *   is disabled, the request was rejected, or an error occurred.
     */
    suspend fun sendTestEmail(recipient: Email): Boolean

    /**
     * Updates the authenticated caller's own email address to [email], or clears it when
     * [email] is `null`.
     *
     * Self-service — no elevated privilege required. The client implementation forwards the
     * request to `PUT /email/myEmail`.
     *
     * @param email New address to store, or `null` to remove the current address.
     * @return `true` when the update was persisted successfully; `false` otherwise.
     */
    suspend fun setMyEmail(email: Email?): Boolean
}
