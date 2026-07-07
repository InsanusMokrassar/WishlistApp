package dev.inmo.wishlist.features.email.server.services

import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.w
import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.email.server.EmailConfig
import dev.inmo.wishlist.features.email.server.SmtpConfig
import jakarta.mail.Message
import jakarta.mail.Session
import jakarta.mail.Transport
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Properties

/**
 * SMTP delivery service backed by Jakarta Mail (Angus Mail).
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
class SmtpEmailService(private val config: EmailConfig) {

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
     * Runs the blocking [Transport.send] call on [Dispatchers.IO].
     *
     * @param recipient Target address for the test message.
     * @return `true` when the message was accepted by the SMTP server; `false` when the feature
     *   is disabled or an error occurs (errors are logged at warn level).
     */
    suspend fun sendTestEmail(recipient: Email): Boolean {
        val smtp = config.smtp
        if (smtp == null || smtp.host.isBlank()) {
            logger.w { "sendTestEmail called but SMTP is not configured — skipping." }
            return false
        }
        return runCatching {
            withContext(Dispatchers.IO) {
                val session = buildSession(smtp)
                val message = MimeMessage(session).apply {
                    setFrom(InternetAddress(smtp.from.string))
                    setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipient.string))
                    subject = "Test email from WishlistApp"
                    setText("This is a test email sent from WishlistApp to verify SMTP configuration.")
                }
                Transport.send(message)
            }
            true
        }.getOrElse { e ->
            logger.w(e) { "Failed to send test email to ${recipient.string}" }
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
