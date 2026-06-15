package dev.inmo.wishlist.features.email.common.models

import kotlinx.serialization.Serializable

/**
 * Request body for the authenticated "set my e-mail" endpoint.
 *
 * The caller is identified by the bearer token, so no user identifier is carried here.
 *
 * @property email New e-mail address for the caller, or `null` to clear a previously stored address.
 */
@Serializable
data class SetEmailRequest(
    val email: Email?
)
