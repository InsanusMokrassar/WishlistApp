package dev.inmo.wishlist.features.auth.common.models

import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.users.common.models.RegisteredUser
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.users.common.models.Username
import kotlinx.serialization.Serializable

/**
 * Feature model returned by the auth "me" surface —
 * [dev.inmo.wishlist.features.auth.client.ClientAuthFeature.getMe],
 * [dev.inmo.wishlist.features.auth.server.ServerAuthFeature.getUser], and the
 * [dev.inmo.wishlist.features.auth.client.meStateFlow] state — the authenticated caller's own record.
 *
 * Deliberately keeps [email]: this is a self-service, own-record surface (the caller reading their own
 * profile), not the public listing that leaked email in issue #67 point 1. A future "show/edit my
 * email" screen can read it directly from this model without a further "me" projection. Do not
 * interpret the presence of [email] here as a regression of the point-1 fix — [UsersFeatureUser] (the
 * public, unauthenticated listing) is the surface that must never carry it.
 *
 * @property id Database-assigned identifier of the authenticated user.
 * @property username Unique login name of the authenticated user.
 * @property email Stored email of the authenticated user, or `null` when unset. Kept intentionally —
 *   see class KDoc.
 */
@Serializable
data class AuthFeatureUser(
    val id: UserId,
    val username: Username,
    val email: Email?
)

/**
 * Projects this [RegisteredUser] onto [AuthFeatureUser], carrying every field through unchanged
 * (including [RegisteredUser.email] — see [AuthFeatureUser] KDoc for why this surface keeps it).
 *
 * @return An [AuthFeatureUser] mirroring this user's [RegisteredUser.id], [RegisteredUser.username]
 *   and [RegisteredUser.email].
 */
fun RegisteredUser.asAuthFeatureUser(): AuthFeatureUser = AuthFeatureUser(
    id = id,
    username = username,
    email = email
)

/**
 * Projects this [AuthFeatureUser] back onto the persistence-layer [RegisteredUser], carrying every
 * field through unchanged (including [AuthFeatureUser.email] — this feature model mirrors the base
 * verbatim, so no extra arguments are required).
 *
 * @return A [RegisteredUser] mirroring this model's [AuthFeatureUser.id], [AuthFeatureUser.username]
 *   and [AuthFeatureUser.email].
 */
fun AuthFeatureUser.asRegisteredUser(): RegisteredUser = RegisteredUser(
    id = id,
    username = username,
    email = email
)
