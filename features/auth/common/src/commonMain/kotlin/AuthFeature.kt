package dev.inmo.wishlist.features.auth.common

import dev.inmo.wishlist.features.auth.common.models.AuthCredentials
import dev.inmo.wishlist.features.auth.common.models.Password
import dev.inmo.wishlist.features.auth.common.models.RefreshToken
import dev.inmo.wishlist.features.users.common.models.Username

interface AuthFeature {
    suspend fun login(username: Username, password: Password): AuthCredentials?
    suspend fun refresh(refreshToken: RefreshToken): AuthCredentials?

    /**
     * Creates a new account and returns credentials on success, or `null` when
     * registration is disabled or the username is already taken.
     */
    suspend fun register(username: Username, password: Password): AuthCredentials?

    /**
     * Returns `true` when the server allows self-service account registration.
     */
    suspend fun isRegistrationAvailable(): Boolean
}
