package dev.inmo.wishlist.features.email.common.models

import kotlinx.serialization.Serializable

/**
 * Request body for `POST /email/sendTest`.
 *
 * Carries the recipient address to which the server will attempt to deliver a test message via
 * the configured SMTP settings. The [Email] type validates the address on construction and
 * serializes to/from a plain JSON string.
 *
 * @property recipient Validated target email address.
 */
@Serializable
data class TestEmailRequest(val recipient: Email)
