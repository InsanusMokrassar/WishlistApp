package dev.inmo.wishlist.features.auth.common.models

import kotlinx.serialization.Serializable
import dev.inmo.wishlist.features.users.common.models.Username

@Serializable
data class LoginRequest(
    val username: Username,
    val password: Password
)
