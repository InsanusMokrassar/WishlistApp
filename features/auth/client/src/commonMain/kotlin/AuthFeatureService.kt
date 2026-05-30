package dev.inmo.wishlist.features.auth.client

import dev.inmo.wishlist.features.auth.common.models.AuthCredentials
import dev.inmo.wishlist.features.auth.common.models.Password
import dev.inmo.wishlist.features.auth.common.models.RefreshToken
import dev.inmo.wishlist.features.users.common.models.RegisteredUser
import dev.inmo.wishlist.features.users.common.models.Username

class AuthFeatureService(
    private val storage: AuthCredentialsStorage,
    private val feature: ClientAuthFeature
) : ClientAuthFeature {
    override suspend fun login(username: Username, password: Password): AuthCredentials? {
        val credentials = feature.login(username, password) ?: return null
        storage.save(credentials)
        return credentials
    }

    override suspend fun refresh(refreshToken: RefreshToken): AuthCredentials? {
        val credentials = feature.refresh(refreshToken) ?: return null
        storage.save(credentials)
        return credentials
    }

    override suspend fun logout() {
        feature.logout()
        storage.save(null)
    }

    override suspend fun register(username: Username, password: Password): AuthCredentials? {
        val credentials = feature.register(username, password) ?: return null
        storage.save(credentials)
        return credentials
    }

    override suspend fun isRegistrationAvailable(): Boolean = feature.isRegistrationAvailable()

    override suspend fun getMe(): RegisteredUser? {
        if (storage.userAuthorised.value == false) {
            return null
        }
        return feature.getMe()
    }
}
