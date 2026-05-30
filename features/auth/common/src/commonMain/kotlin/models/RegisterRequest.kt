package dev.inmo.wishlist.features.auth.common.models

import dev.inmo.wishlist.features.users.common.models.Username
import kotlinx.serialization.Serializable

/**
 * Wire DTO for the `POST /auth/register` endpoint.
 *
 * @param username Desired username.
 * @param password Plaintext password; hashed server-side before storage.
 */
@Serializable
data class RegisterRequest(
    val username: Username,
    val password: Password,
)
