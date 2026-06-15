package dev.inmo.wishlist.features.users.common.models

import dev.inmo.wishlist.features.email.common.models.Email
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable
@JvmInline
value class UserId(val long: Long)

@Serializable
sealed interface User {
    val username: Username

    /** Optional e-mail address of the user; `null` when not set. */
    val email: Email?
}

@Serializable
data class NewUser(
    override val username: Username,
    override val email: Email? = null
) : User

@Serializable
data class RegisteredUser(
    val id: UserId,
    override val username: Username,
    override val email: Email? = null
) : User
