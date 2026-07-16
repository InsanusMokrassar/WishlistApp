package dev.inmo.wishlist.features.admin.common.models

import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.users.common.models.RegisteredUser
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.users.common.models.Username
import kotlinx.serialization.Serializable

/**
 * Feature model returned by the admin users-management surface
 * ([dev.inmo.wishlist.features.admin.client.UsersManagementFeature.getAll]/`getById`/`create`, and the
 * server-side [dev.inmo.wishlist.features.admin.server.UsersManagementFeature] equivalents).
 *
 * Root-only surface (`features/admin/README.md` Operator Notes: only the `root` user may reach the
 * admin panel/features), so [email] is kept deliberately — an unprivileged caller never reaches this
 * model.
 *
 * @property id Database-assigned identifier of the user.
 * @property username Unique login name of the user.
 * @property email Stored email of the user, or `null` when unset. Kept intentionally — see class KDoc.
 */
@Serializable
data class AdminUser(
    val id: UserId,
    val username: Username,
    val email: Email?
)

/**
 * Projects this [RegisteredUser] onto [AdminUser], carrying every field through unchanged.
 *
 * @return An [AdminUser] mirroring this user's [RegisteredUser.id], [RegisteredUser.username] and
 *   [RegisteredUser.email].
 */
fun RegisteredUser.asAdminUser(): AdminUser = AdminUser(
    id = id,
    username = username,
    email = email
)

/**
 * Projects this [AdminUser] back onto the persistence-layer [RegisteredUser], carrying every field
 * through unchanged (including [AdminUser.email] — this feature model mirrors the base verbatim, so
 * no extra arguments are required).
 *
 * @return A [RegisteredUser] mirroring this model's [AdminUser.id], [AdminUser.username] and
 *   [AdminUser.email].
 */
fun AdminUser.asRegisteredUser(): RegisteredUser = RegisteredUser(
    id = id,
    username = username,
    email = email
)
