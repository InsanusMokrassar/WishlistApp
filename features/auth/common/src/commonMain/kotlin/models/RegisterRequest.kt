package dev.inmo.wishlist.features.auth.common.models

import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.users.common.models.Username
import kotlinx.serialization.Serializable

/**
 * Wire DTO for the `POST /auth/register` endpoint.
 *
 * @param username Desired username.
 * @param password Plaintext password; hashed server-side before storage.
 * @param email Optional validated email address used for verification when configured.
 */
@Serializable
data class RegisterRequest(
    val username: Username,
    val password: Password,
    val email: Email? = null,
)
