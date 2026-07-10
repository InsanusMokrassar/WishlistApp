package dev.inmo.wishlist.features.email.client

import dev.inmo.wishlist.features.email.common.models.Email

/**
 * Client-side capability surface for the email feature.
 *
 * Implemented by [KtorEmailFeature], which forwards each call to the server over the shared
 * authenticated [io.ktor.client.HttpClient]. Authentication context is embedded in the client,
 * so methods here carry no explicit caller identity — the server resolves it from the bearer token.
 *
 * The feature reports its enabled state via [isFeatureEnabled] so callers can decide whether to
 * surface email-related UI. [sendTestEmail] is root-only on the server — the server enforces
 * the privilege check; the client only forwards the request. [setMyEmail] is self-service and
 * available to every authenticated user.
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
     * The server enforces root-only access. The client implementation forwards the request to
     * `POST /email/sendTest`.
     *
     * @param recipient Target email address to deliver the test message to.
     * @return `true` when the message was accepted by the SMTP server; `false` otherwise.
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
