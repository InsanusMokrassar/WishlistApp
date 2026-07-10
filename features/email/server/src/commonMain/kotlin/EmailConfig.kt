package dev.inmo.wishlist.features.email.server

import dev.inmo.wishlist.features.email.common.models.Email
import kotlinx.serialization.Serializable

/**
 * Email-feature config slice, decoded from the nested `"email"` object in the server config JSON
 * (`config["email"]`) — NOT the whole root config object (unlike the root-flat-key pattern used by
 * e.g. [dev.inmo.wishlist.features.currency.server.CurrencyConfig]).
 *
 * [Plugin] only registers this class's Koin `single` (and, together with it,
 * [dev.inmo.wishlist.features.email.server.services.SmtpEmailService] and the [EmailsService]
 * binding) when the `"email"` key is present and non-null in the root config. When absent, none of
 * the three exist in the DI graph and [EmailFeature] resolves to
 * [dev.inmo.wishlist.features.email.server.services.DisabledEmailFeature] instead — "disabled" is a
 * DI-graph-shape fact, not a value carried on this class.
 *
 * @property smtp SMTP delivery settings. Always present whenever an [EmailConfig] instance exists.
 */
@Serializable
data class EmailConfig(
    val smtp: SmtpConfig
)

/**
 * SMTP connection and authentication settings.
 *
 * Used by [dev.inmo.wishlist.features.email.server.services.SmtpEmailService] to build a
 * [jakarta.mail.Session] and deliver messages.
 *
 * @property host SMTP server hostname. A blank value still disables delivery via the guard in
 *   [dev.inmo.wishlist.features.email.server.services.SmtpEmailService]'s private send skeleton.
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
