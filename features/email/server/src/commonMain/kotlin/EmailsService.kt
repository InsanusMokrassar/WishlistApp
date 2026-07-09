package dev.inmo.wishlist.features.email.server

import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.email.server.models.EmailAttachment

/**
 * Server-only email-sending surface of the email feature.
 *
 * Declares direct message-sending operations for other server-side components, resolved from
 * Koin. Deliberately NOT exposed through [EmailFeature], the client `KtorEmailFeature`, or any
 * HTTP route — this interface never crosses the server boundary.
 *
 * All methods share one result contract: `true` when the SMTP server accepted the message;
 * `false` when SMTP is not configured (disabled no-op mode) or an error occurred. Errors are
 * logged at warn level and never thrown to the caller.
 */
interface EmailsService {

    /**
     * Sends an email with a plain-text body.
     *
     * @param recipient Target email address.
     * @param subject Subject header of the message.
     * @param text Plain-text body (sent as `text/plain`, UTF-8).
     * @return `true` when the SMTP server accepted the message; `false` when SMTP is disabled
     *   or an error occurred (logged at warn level).
     */
    suspend fun sendText(recipient: Email, subject: String, text: String): Boolean

    /**
     * Sends an email with a plain-text body and file attachments.
     *
     * Attachment content is streamed to the SMTP connection during send: each
     * [EmailAttachment.content] provider may be invoked more than once and must return a fresh
     * stream on every call; attachment bytes are never buffered wholesale in memory. An empty
     * [attachments] list produces a valid multipart message containing only the text part.
     *
     * @param recipient Target email address.
     * @param subject Subject header of the message.
     * @param text Plain-text body part (sent as `text/plain`, UTF-8).
     * @param attachments Attachments appended after the text part, in list order.
     * @return `true` when the SMTP server accepted the message; `false` when SMTP is disabled
     *   or an error occurred (logged at warn level).
     */
    suspend fun sendTextWithAttachments(
        recipient: Email,
        subject: String,
        text: String,
        attachments: List<EmailAttachment>
    ): Boolean

    /**
     * Sends an email with an HTML body (`text/html; charset=utf-8`).
     *
     * @param recipient Target email address.
     * @param subject Subject header of the message.
     * @param html HTML markup used as the message body.
     * @return `true` when the SMTP server accepted the message; `false` when SMTP is disabled
     *   or an error occurred (logged at warn level).
     */
    suspend fun sendHtml(recipient: Email, subject: String, html: String): Boolean
}
