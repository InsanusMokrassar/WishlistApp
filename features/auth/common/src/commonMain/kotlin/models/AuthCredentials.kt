package dev.inmo.wishlist.features.auth.common.models

import kotlinx.serialization.Serializable

@Serializable
data class AuthCredentials(
    val token: Token,
    val refreshToken: RefreshToken
)
