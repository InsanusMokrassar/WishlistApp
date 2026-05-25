package project_group.project_name.features.auth.client

import project_group.project_name.features.auth.common.AuthFeature
import project_group.project_name.features.users.common.models.RegisteredUser
import project_group.project_name.features.users.common.models.User

interface ClientAuthFeature : AuthFeature {
    suspend fun logout()
    suspend fun getMe(): RegisteredUser?
}
