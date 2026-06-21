package dev.inmo.wishlist.features.email.server

import dev.inmo.wishlist.features.email.common.models.Email
import kotlinx.serialization.Serializable

/**
 * Top-level email-feature config slice decoded from the server config JSON.
 *
 * Follows the same config-slice pattern as [dev.inmo.wishlist.features.currency.server.CurrencyConfig]:
 * decoded via `get<Json>().decodeFromJsonElement(EmailConfig.serializer(), config)` in [Plugin].
 *
 * When [smtp] is `null` (default, or the `"smtp"` key is absent/null in the config), the feature
 * operates in disabled/no-op mode — [dev.inmo.wishlist.features.email.server.services.SmtpEmailService]
 * will report `isFeatureEnabled() == false` and skip all delivery attempts.
 *
 * @property smtp SMTP delivery settings, or `null` to disable email delivery.
 */
@Serializable
data class EmailConfig(
    val smtp: SmtpConfig? = null
)

/**
 * SMTP connection and authentication settings.
 *
 * Used by [dev.inmo.wishlist.features.email.server.services.SmtpEmailService] to build a
 * [jakarta.mail.Session] and deliver messages.
 *
 * @property host SMTP server hostname. A blank value has the same effect as `null` on
 *   the parent [EmailConfig.smtp]: the feature is treated as disabled.
 * @property port SMTP server port. Defaults to 587 (STARTTLS submission).
 * @property username Optional SMTP authentication username.
 * @property password Optional SMTP authentication password.
 * @property from Sender address shown in the `From:` header of outgoing messages.
 * @property useTls Whether to enable STARTTLS negotiation (`mail.smtp.starttls.enable`).
 *   Defaults to `true`.
 * @property useSsl Whether to use SSL/TLS on connect (`mail.smtp.ssl.enable`).
 *   Defaults to `false`. Set to `true` for port 465.
 */
@Serializable
data class SmtpConfig(
    val host: String,
    val port: Int = 587,
    val username: String? = null,
    val password: String? = null,
    val from: Email,
    val useTls: Boolean = true,
    val useSsl: Boolean = false
)
