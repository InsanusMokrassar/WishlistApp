package project_group.project_name.features.auth.client

import dev.inmo.micro_utils.coroutines.MutableRedeliverStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import project_group.project_name.features.auth.common.models.AuthCredentials

interface AuthCredentialsStorage {
    val userAuthorised: StateFlow<Boolean>
    suspend fun get(): AuthCredentials?
    suspend fun save(credentials: AuthCredentials?)
}
