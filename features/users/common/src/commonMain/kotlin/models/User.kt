package dev.inmo.wishlist.features.users.common.models

import dev.inmo.wishlist.features.email.common.models.Email
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/** Opaque identifier wrapping a database-assigned long primary key. */
@Serializable
@JvmInline
value class UserId(val long: Long)

/**
 * Shared base for user variants.
 *
 * All user types share [username] and an optional [email] address.
 */
@Serializable
sealed interface User {
    /** Unique login name for this user. */
    val username: Username

    /** Optional email address stored for this user; `null` when not set. */
    val email: Email?
}

/**
 * Data sent when creating a new user.
 *
 * [email] defaults to `null` so all existing call sites remain valid without change.
 *
 * @property username Desired login name.
 * @property email Optional email address; `null` to leave unset.
 */
@Serializable
data class NewUser(
    override val username: Username,
    override val email: Email? = null
) : User

/**
 * Stored user entity returned after creation or lookup.
 *
 * [email] defaults to `null` for back-compatibility with existing serialized payloads that omit the field.
 *
 * @property id Database-assigned identifier.
 * @property username Unique login name.
 * @property email Stored email address, or `null` when not set.
 */
@Serializable
data class RegisteredUser(
    val id: UserId,
    override val username: Username,
    override val email: Email? = null
) : User
