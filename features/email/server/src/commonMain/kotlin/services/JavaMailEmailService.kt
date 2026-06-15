package dev.inmo.wishlist.features.email.server.services

import dev.inmo.kslog.common.logger
import dev.inmo.kslog.common.e
import dev.inmo.kslog.common.w
import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.email.server.EmailFeature
import dev.inmo.wishlist.features.email.server.SmtpConfig
import jakarta.mail.Authenticator
import jakarta.mail.Message
import jakarta.mail.PasswordAuthentication
import jakarta.mail.Session
import jakarta.mail.Transport
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Properties

/**
 * [EmailFeature] implementation backed by Jakarta Mail (Angus Mail runtime).
 *
 * When [smtp] is `null` the feature is disabled: [sendEmail] logs a warning and returns `false` without
 * touching the network. Sending runs on [Dispatchers.IO] because Jakarta Mail performs blocking socket
 * I/O.
 *
 * @param smtp SMTP connection/credentials, or `null` to disable sending.
 */
class JavaMailEmailService(
    private val smtp: SmtpConfig?
) : EmailFeature {

    /**
     * Lazily-built Jakarta Mail [Session] derived from [smtp]; `null` while the feature is disabled.
     */
    private val session: Session? by lazy {
        val config = smtp?.takeIf { it.host.isNotBlank() } ?: return@lazy null
        val properties = Properties().apply {
            put("mail.smtp.host", config.host)
            put("mail.smtp.port", config.port.toString())
            put("mail.smtp.auth", (config.username != null && config.username.isNotBlank()).toString())
            put("mail.smtp.starttls.enable", config.useTls.toString())
            put("mail.smtp.ssl.enable", config.useSsl.toString())
        }
        val authenticator = when {
            config.username != null && config.username.isNotBlank() -> object : Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication =
                    PasswordAuthentication(config.username, config.password.orEmpty())
            }
            else -> null
        }
        Session.getInstance(properties, authenticator)
    }

    override suspend fun sendEmail(to: Email, subject: String, body: String): Boolean {
        val config = smtp?.takeIf { it.host.isNotBlank() }
        val currentSession = session
        if (config == null || currentSession == null) {
            logger.w("Email feature is disabled (no SMTP config); dropping message to ${to.raw}")
            return false
        }
        return withContext(Dispatchers.IO) {
            runCatching {
                val message = MimeMessage(currentSession).apply {
                    setFrom(InternetAddress(config.from.raw))
                    setRecipient(Message.RecipientType.TO, InternetAddress(to.raw))
                    setSubject(subject)
                    setText(body)
                }
                Transport.send(message)
                true
            }.getOrElse { throwable ->
                logger.e("Failed to send email to ${to.raw}", throwable)
                false
            }
        }
    }
}
