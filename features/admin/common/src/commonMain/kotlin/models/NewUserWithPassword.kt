package dev.inmo.wishlist.features.admin.common.models

import dev.inmo.wishlist.features.auth.common.models.Password
import dev.inmo.wishlist.features.users.common.models.Username
import kotlinx.serialization.Serializable

@Serializable
data class NewUserWithPassword(
    val username: Username,
    val password: Password
)
