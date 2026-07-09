package dev.inmo.wishlist.features.email.server.services

import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.w
import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.email.server.EmailConfig
import dev.inmo.wishlist.features.email.server.EmailsService
import dev.inmo.wishlist.features.email.server.SmtpConfig
import dev.inmo.wishlist.features.email.server.models.EmailAttachment
import jakarta.activation.DataHandler
import jakarta.activation.DataSource
import jakarta.mail.Message
import jakarta.mail.Part
import jakarta.mail.Session
import jakarta.mail.Transport
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeBodyPart
import jakarta.mail.internet.MimeMessage
import jakarta.mail.internet.MimeMultipart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.util.Properties

/**
 * SMTP delivery service backed by Jakarta Mail (Angus Mail); the server-side [EmailsService]
 * implementation.
 *
 * Performs email delivery on [Dispatchers.IO] to keep blocking socket I/O off the event loop.
 * When [EmailConfig.smtp] is `null` or the host is blank, all delivery methods are no-ops and
 * [isFeatureEnabled] returns `false`.
 *
 * This class is an SMTP transport only — caller-identity checks and user-email persistence are
 * handled by [EmailFeatureService], which wraps this class.
 *
 * @param config Full email config slice decoded from the server config JSON.
 */
class SmtpEmailService(private val config: EmailConfig) : EmailsService {

    private val logger = KSLog("SmtpEmailService")

    /**
     * Returns `true` when an SMTP host is configured and non-blank.
     *
     * @return Whether SMTP delivery is available.
     */
    suspend fun isFeatureEnabled(): Boolean =
        config.smtp != null && config.smtp.host.isNotBlank()

    /**
     * Sends a test email to [recipient] using the configured SMTP settings.
     *
     * Routed through the shared [send] skeleton; runs the blocking [Transport.send] call on
     * [Dispatchers.IO].
     *
     * @param recipient Target address for the test message.
     * @return `true` when the message was accepted by the SMTP server; `false` when the feature
     *   is disabled or an error occurs (errors are logged at warn level).
     */
    suspend fun sendTestEmail(recipient: Email): Boolean = send(
        recipient = recipient,
        subject = "Test email from WishlistApp",
        logLabel = "sendTestEmail"
    ) {
        setText("This is a test email sent from WishlistApp to verify SMTP configuration.")
    }

    /**
     * Sends an email with a plain-text body.
     *
     * @param recipient Target email address.
     * @param subject Subject header of the message.
     * @param text Plain-text body (sent as `text/plain`, UTF-8).
     * @return `true` when the SMTP server accepted the message; `false` when SMTP is disabled
     *   or an error occurred (logged at warn level).
     */
    override suspend fun sendText(recipient: Email, subject: String, text: String): Boolean = send(
        recipient = recipient,
        subject = subject,
        logLabel = "sendText"
    ) {
        setText(text, "UTF-8")
    }

    /**
     * Sends an email with a plain-text body and file attachments.
     *
     * The message is assembled as a [MimeMultipart]: one text part followed by one part per
     * attachment in list order. Each attachment part streams its bytes through
     * [EmailAttachmentDataSource], so content is encoded on the fly during [Transport.send]
     * without whole-payload buffering.
     *
     * @param recipient Target email address.
     * @param subject Subject header of the message.
     * @param text Plain-text body part (sent as `text/plain`, UTF-8).
     * @param attachments Attachments appended after the text part, in list order.
     * @return `true` when the SMTP server accepted the message; `false` when SMTP is disabled
     *   or an error occurred (logged at warn level).
     */
    override suspend fun sendTextWithAttachments(
        recipient: Email,
        subject: String,
        text: String,
        attachments: List<EmailAttachment>
    ): Boolean = send(
        recipient = recipient,
        subject = subject,
        logLabel = "sendTextWithAttachments"
    ) {
        val multipart = MimeMultipart()
        multipart.addBodyPart(
            MimeBodyPart().apply {
                setText(text, "UTF-8")
            }
        )
        attachments.forEach { attachment ->
            multipart.addBodyPart(
                MimeBodyPart().apply {
                    dataHandler = DataHandler(EmailAttachmentDataSource(attachment))
                    fileName = attachment.fileName
                    disposition = Part.ATTACHMENT
                }
            )
        }
        setContent(multipart)
    }

    /**
     * Sends an email with an HTML body (`text/html; charset=utf-8`).
     *
     * @param recipient Target email address.
     * @param subject Subject header of the message.
     * @param html HTML markup used as the message body.
     * @return `true` when the SMTP server accepted the message; `false` when SMTP is disabled
     *   or an error occurred (logged at warn level).
     */
    override suspend fun sendHtml(recipient: Email, subject: String, html: String): Boolean = send(
        recipient = recipient,
        subject = subject,
        logLabel = "sendHtml"
    ) {
        setContent(html, "text/html; charset=utf-8")
    }

    /**
     * Shared send skeleton used by every delivery method.
     *
     * Centralizes the disabled-mode check (warn-log + `false` when [EmailConfig.smtp] is `null`
     * or its host is blank), session construction via [buildSession], the message envelope
     * (`From`, `To`, subject), the blocking [Transport.send] on [Dispatchers.IO], and the
     * `runCatching`/warn-log failure handling.
     *
     * @param recipient Target email address placed into the `To` header.
     * @param subject Subject header, encoded as UTF-8.
     * @param logLabel Method name used to prefix the disabled-mode and failure log messages.
     * @param fillContent Body-filling block applied to the message after the envelope is set.
     * @return `true` when the SMTP server accepted the message; `false` when SMTP is disabled
     *   or an error occurred (logged at warn level).
     */
    private suspend fun send(
        recipient: Email,
        subject: String,
        logLabel: String,
        fillContent: MimeMessage.() -> Unit
    ): Boolean {
        val smtp = config.smtp
        if (smtp == null || smtp.host.isBlank()) {
            logger.w { "$logLabel called but SMTP is not configured — skipping." }
            return false
        }
        return runCatching {
            withContext(Dispatchers.IO) {
                val session = buildSession(smtp)
                val message = MimeMessage(session).apply {
                    setFrom(InternetAddress(smtp.from.string))
                    setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipient.string))
                    setSubject(subject, "UTF-8")
                    fillContent()
                }
                Transport.send(message)
            }
            true
        }.getOrElse { e ->
            logger.w(e) { "$logLabel failed to send email to ${recipient.string}" }
            false
        }
    }

    /**
     * Builds a [Session] from [smtp] settings, configuring STARTTLS and/or SSL as requested.
     *
     * @param smtp Validated SMTP settings.
     * @return Ready-to-use [Session].
     */
    private fun buildSession(smtp: SmtpConfig): Session {
        val props = Properties().apply {
            put("mail.smtp.host", smtp.host)
            put("mail.smtp.port", smtp.port.toString())
            if (smtp.useTls) {
                put("mail.smtp.starttls.enable", "true")
            }
            if (smtp.useSsl) {
                put("mail.smtp.ssl.enable", "true")
            }
            if (smtp.username != null) {
                put("mail.smtp.auth", "true")
            }
        }
        return when {
            smtp.username != null && smtp.password != null -> {
                Session.getInstance(props, object : jakarta.mail.Authenticator() {
                    override fun getPasswordAuthentication() =
                        jakarta.mail.PasswordAuthentication(smtp.username, smtp.password)
                })
            }
            else -> Session.getInstance(props)
        }
    }
}

/**
 * Read-only [DataSource] bridge between the suspend streaming provider of an [EmailAttachment]
 * and Jakarta Mail's blocking attachment API.
 *
 * Jakarta Mail may call [getInputStream] more than once while encoding a message; per the
 * [DataSource] contract every call returns a fresh stream positioned at the beginning, obtained
 * by re-invoking [EmailAttachment.content]. Content therefore streams through the mail encoder
 * without being buffered wholesale.
 *
 * `internal` (not `private`) so the module's own unit tests can exercise the bridge directly;
 * the class is not part of the module's public API.
 *
 * @param attachment Attachment whose metadata and content provider back this data source.
 */
internal class EmailAttachmentDataSource(
    private val attachment: EmailAttachment
) : DataSource {

    /**
     * Returns a fresh stream over the full attachment content.
     *
     * Invokes the suspend provider via a bare [runBlocking] (no outer context is passed in, so
     * there is no dispatcher re-entry). Blocking is safe here: Jakarta Mail calls this method on
     * the [Dispatchers.IO] worker thread inside `Transport.send`, which is already a blocking
     * context.
     *
     * @return New [InputStream] positioned at the beginning of the content.
     */
    override fun getInputStream(): InputStream = runBlocking { attachment.content() }

    /**
     * Always throws: this data source is read-only.
     *
     * @return Never returns.
     * @throws UnsupportedOperationException on every call.
     */
    override fun getOutputStream(): OutputStream =
        throw UnsupportedOperationException("EmailAttachmentDataSource is read-only")

    /**
     * Returns the attachment's MIME type.
     *
     * @return Value of [EmailAttachment.mimeType].
     */
    override fun getContentType(): String = attachment.mimeType

    /**
     * Returns the attachment's file name.
     *
     * @return Value of [EmailAttachment.fileName].
     */
    override fun getName(): String = attachment.fileName
}
