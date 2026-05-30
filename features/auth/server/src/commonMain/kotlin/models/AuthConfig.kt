package dev.inmo.wishlist.features.auth.server.models

import kotlinx.serialization.Serializable

/**
 * Wire DTO returned by `GET /auth/config`.
 *
 * @param enableRegistration `true` when self-service registration is open.
 */
@Serializable
data class AuthConfig(
    val enableRegistration: Boolean,
)
