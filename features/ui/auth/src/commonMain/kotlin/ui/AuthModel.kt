package dev.inmo.wishlist.features.ui.auth.ui

import dev.inmo.wishlist.features.auth.common.models.Password
import dev.inmo.wishlist.features.users.common.models.Username

interface AuthModel {
    suspend fun isAlreadyLoggedIn(): Boolean
    suspend fun getServerAddress(): String?
    suspend fun saveServerAddress(address: String?)
    suspend fun login(username: Username, password: Password): Boolean
}
