package dev.inmo.wishlist.features.email.server

import dev.inmo.wishlist.features.email.common.models.Email

/**
 * Server-side capability for sending e-mail messages over SMTP.
 *
 * Implementations are expected to be a no-op (returning `false`) when no SMTP configuration is supplied,
 * so the feature can be safely registered even on deployments without mail enabled.
 */
interface EmailFeature {
    /**
     * Sends a plain-text e-mail.
     *
     * @param to Recipient address.
     * @param subject Message subject line.
     * @param body Plain-text message body.
     * @return `true` when the message was handed off to the SMTP server successfully; `false` when the
     * feature is disabled (no SMTP config) or sending failed.
     */
    suspend fun sendEmail(to: Email, subject: String, body: String): Boolean
}
