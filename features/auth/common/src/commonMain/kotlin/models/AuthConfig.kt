package dev.inmo.wishlist.features.auth.common.models

import kotlinx.serialization.Serializable

/**
 * Public authentication configuration consumed by clients before rendering registration UI.
 *
 * @property enableRegistration Whether self-service registration is available.
 * @property requireEmailForRegistration Whether registration requires a validated email address and
 *   successful delivery of the verification invite.
 */
@Serializable
data class AuthConfig(
    val enableRegistration: Boolean = false,
    val requireEmailForRegistration: Boolean = false,
)
