package dev.inmo.wishlist.features.auth.server

import dev.inmo.wishlist.features.auth.common.AuthFeature
import dev.inmo.wishlist.features.auth.common.models.Token
import dev.inmo.wishlist.features.users.common.models.RegisteredUser

interface ServerAuthFeature : AuthFeature {
    suspend fun logout(token: Token)
    suspend fun getUser(token: Token): RegisteredUser?
}
