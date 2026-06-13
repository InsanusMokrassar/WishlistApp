package dev.inmo.wishlist.features.auth.common.models

import kotlinx.serialization.Serializable

@Serializable
data class RefreshRequest(
    val refreshToken: RefreshToken
)
