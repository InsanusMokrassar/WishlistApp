package project_group.project_name.features.auth.common.models

import kotlinx.serialization.Serializable

@Serializable
data class AuthCredentials(
    val token: Token,
    val refreshToken: RefreshToken
)
