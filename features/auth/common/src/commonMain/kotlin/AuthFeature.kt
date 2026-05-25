package project_group.project_name.features.auth.common

import project_group.project_name.features.auth.common.models.AuthCredentials
import project_group.project_name.features.auth.common.models.Password
import project_group.project_name.features.auth.common.models.RefreshToken
import project_group.project_name.features.users.common.models.Username

interface AuthFeature {
    suspend fun login(username: Username, password: Password): AuthCredentials?
    suspend fun refresh(refreshToken: RefreshToken): AuthCredentials?
}
