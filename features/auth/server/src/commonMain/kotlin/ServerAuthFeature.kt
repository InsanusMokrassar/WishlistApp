package project_group.project_name.features.auth.server

import project_group.project_name.features.auth.common.AuthFeature
import project_group.project_name.features.auth.common.models.Token
import project_group.project_name.features.users.common.models.RegisteredUser

interface ServerAuthFeature : AuthFeature {
    suspend fun logout(token: Token)
    suspend fun getUser(token: Token): RegisteredUser?
}
