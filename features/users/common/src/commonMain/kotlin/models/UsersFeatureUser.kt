package dev.inmo.wishlist.features.users.common.models

import dev.inmo.wishlist.features.email.common.models.Email
import kotlinx.serialization.Serializable

/**
 * Public-facing projection of a [RegisteredUser] returned by
 * [dev.inmo.wishlist.features.users.server.UsersFeature.getAll] and its client mirror
 * [dev.inmo.wishlist.features.users.client.UsersFeature.getAll].
 *
 * Deliberately omits [RegisteredUser.email] and [RegisteredUser.emailApproved]: the `getAll` route
 * requires no authentication (per that interface's own KDoc), so any field carried by this model is
 * visible to anonymous callers — private email data must never be exposed there. This is the model
 * that fixes the data leak in issue #67.
 *
 * Does not implement the sealed [User] interface: [User] requires a nullable [User.email], which would
 * reintroduce the leaked field through a shared supertype.
 *
 * @property id Database-assigned identifier of the user.
 * @property username Unique login name of the user.
 */
@Serializable
data class UsersFeatureUser(
    val id: UserId,
    val username: Username
)

/**
 * Projects this [RegisteredUser] onto the public-facing [UsersFeatureUser], dropping
 * [RegisteredUser.email].
 *
 * @return A [UsersFeatureUser] carrying only this user's [RegisteredUser.id] and [RegisteredUser.username].
 */
fun RegisteredUser.asUsersFeatureUser(): UsersFeatureUser = UsersFeatureUser(
    id = id,
    username = username
)

/**
 * Projects this [UsersFeatureUser] back onto the persistence-layer [RegisteredUser].
 *
 * [UsersFeatureUser] deliberately drops [RegisteredUser.email] and [RegisteredUser.emailApproved]
 * (see this model's class KDoc), so the reverse conversion cannot recover either value from the
 * receiver: the caller MUST supply both fields explicitly. The parameters have NO default values on
 * purpose — a silent default would let a caller accidentally reconstruct a user with private data
 * erased, the same data-integrity trap (in the opposite direction) that issue #67 fixed.
 *
 * @param email Email address to restore onto the rebuilt [RegisteredUser] (typically taken from the
 *   stored record being reconstructed), or `null` to consciously record "no email".
 * @param emailApproved Approval evidence to restore for the supplied current email.
 * @return A [RegisteredUser] carrying this model's [UsersFeatureUser.id] and
 *   [UsersFeatureUser.username] plus the supplied private fields.
 */
fun UsersFeatureUser.asRegisteredUser(email: Email?, emailApproved: Boolean): RegisteredUser = RegisteredUser(
    id = id,
    username = username,
    email = email,
    emailApproved = emailApproved
)
