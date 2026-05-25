package project_group.project_name.features.ui.auth.ui

import project_group.project_name.features.auth.common.models.Password
import project_group.project_name.features.users.common.models.Username

interface AuthModel {
    suspend fun isAlreadyLoggedIn(): Boolean
    suspend fun getServerAddress(): String?
    suspend fun saveServerAddress(address: String?)
    suspend fun login(username: Username, password: Password): Boolean
}
