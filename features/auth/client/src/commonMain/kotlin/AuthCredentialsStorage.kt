package dev.inmo.wishlist.features.auth.client

import dev.inmo.micro_utils.coroutines.MutableRedeliverStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import dev.inmo.wishlist.features.auth.common.models.AuthCredentials

interface AuthCredentialsStorage {
    val userAuthorised: StateFlow<Boolean>
    suspend fun get(): AuthCredentials?
    suspend fun save(credentials: AuthCredentials?)
}
