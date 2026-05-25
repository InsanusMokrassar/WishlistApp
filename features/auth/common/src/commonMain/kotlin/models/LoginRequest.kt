package project_group.project_name.features.auth.common.models

import kotlinx.serialization.Serializable
import project_group.project_name.features.users.common.models.Username

@Serializable
data class LoginRequest(
    val username: Username,
    val password: Password
)
