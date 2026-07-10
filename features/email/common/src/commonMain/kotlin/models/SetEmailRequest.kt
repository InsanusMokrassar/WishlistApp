package dev.inmo.wishlist.features.email.common.models

import kotlinx.serialization.Serializable

/**
 * Request body for `PUT /email/myEmail`.
 *
 * When [email] is `null` (or the field is absent from the JSON payload), the server clears the
 * caller's stored email address. Providing a validated [Email] replaces any existing address.
 *
 * @property email New email address to store, or `null` to clear the current address.
 */
@Serializable
data class SetEmailRequest(val email: Email? = null)
