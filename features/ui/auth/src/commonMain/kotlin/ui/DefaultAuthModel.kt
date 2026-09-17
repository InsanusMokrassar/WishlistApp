package dev.inmo.wishlist.features.ui.auth.ui

import dev.inmo.micro_utils.coroutines.runCatchingLogging
import dev.inmo.wishlist.features.auth.client.AuthCredentialsStorage
import dev.inmo.wishlist.features.auth.client.ClientAuthFeature
import dev.inmo.wishlist.features.auth.common.models.AuthConfig
import dev.inmo.wishlist.features.auth.common.models.Password
import dev.inmo.wishlist.features.auth.common.models.RegistrationResult
import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.users.common.models.Username

/**
 * Default authentication model that delegates session operations to the auth client feature.

 * @param authFeature Remote authentication capability.
 * @param credentialsStorage Persistent authentication state shared with the UI.
 */
class DefaultAuthModel(
    private val authFeature: ClientAuthFeature,
    private val credentialsStorage: AuthCredentialsStorage,
) : AuthModel {
    /** Authentication state observed by the navigation widget. */
    override val userAuthorisedState = credentialsStorage.userAuthorised

    /** @return `true` when stored credentials exist and the server confirms the caller. */
    override suspend fun isAlreadyLoggedIn(): Boolean {
        if (credentialsStorage.userAuthorised.value) {
            return authFeature.getMe() != null
        }
        return false
    }

    /** @return `true` when [username] and [password] produce credentials. */
    override suspend fun login(username: Username, password: Password): Boolean =
        authFeature.login(username, password) != null

    /** Ends the active session through the authentication feature. */
    override suspend fun logout() {
        authFeature.logout()
    }

    /** @return `true` when server configuration enables registration. */
    override suspend fun isRegistrationEnabled(): Boolean =
        getConfig().enableRegistration

    /** @return Server configuration, or default configuration when loading fails. */
    override suspend fun getConfig(): AuthConfig =
        runCatchingLogging { authFeature.getConfig() }.getOrDefault(AuthConfig())

    /** @return The registration result for [username], [password], and optional [email]. */
    override suspend fun register(
        username: Username,
        password: Password,
        email: Email?,
    ): RegistrationResult? = authFeature.register(username, password, email)
}
