package project_group.project_name.features.users.common.models

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable
@JvmInline
value class UserId(val long: Long)

@Serializable
sealed interface User {
    val username: Username
}

@Serializable
data class NewUser(
    override val username: Username
) : User

@Serializable
data class RegisteredUser(
    val id: UserId,
    override val username: Username
) : User
