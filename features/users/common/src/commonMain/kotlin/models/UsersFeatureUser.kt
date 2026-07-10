package dev.inmo.wishlist.features.users.common.models

import kotlinx.serialization.Serializable

/**
 * Public-facing projection of a [RegisteredUser] returned by
 * [dev.inmo.wishlist.features.users.server.UsersFeature.getAll] and its client mirror
 * [dev.inmo.wishlist.features.users.client.UsersFeature.getAll].
 *
 * Deliberately omits [RegisteredUser.email]: the `getAll` route requires no authentication (per that
 * interface's own KDoc), so any field carried by this model is visible to anonymous callers — email
 * must never be exposed there. This is the model that fixes the data leak in issue #67.
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
