package dev.inmo.wishlist.features.auth.client

import dev.inmo.wishlist.features.auth.common.AuthFeature
import dev.inmo.wishlist.features.users.common.models.RegisteredUser
import dev.inmo.wishlist.features.users.common.models.User

interface ClientAuthFeature : AuthFeature {
    suspend fun logout()
    suspend fun getMe(): RegisteredUser?
}
