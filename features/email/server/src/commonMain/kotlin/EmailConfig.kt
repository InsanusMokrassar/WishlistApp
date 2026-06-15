package dev.inmo.wishlist.features.email.server

import dev.inmo.wishlist.features.email.common.models.Email
import kotlinx.serialization.Serializable

/**
 * Email-feature slice of the server config JSON, decoded from the same root config object the rest of the
 * server config is decoded from (mirrors `CurrencyConfig`), so adding this feature needs no change to any
 * shared `Config` type.
 *
 * @property smtp SMTP connection/credentials block. `null` (the default) disables the feature — every
 * send becomes a logged no-op returning `false`.
 */
@Serializable
data class EmailConfig(
    val smtp: SmtpConfig? = null
)

/**
 * SMTP transport configuration for outgoing mail.
 *
 * @property host SMTP server host. Blank host disables the feature.
 * @property port SMTP server port (commonly 587 for STARTTLS, 465 for implicit SSL, 25 plain).
 * @property username Username for SMTP authentication, or `null`/blank for an unauthenticated relay.
 * @property password Password for SMTP authentication, or `null` when [username] is absent.
 * @property from Sender ("From") address; validated as an [Email].
 * @property useTls Enable STARTTLS upgrade on a plain connection (`mail.smtp.starttls.enable`).
 * @property useSsl Use an implicit SSL/TLS socket from the start (`mail.smtp.ssl.enable`).
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
