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
 * @property emailApproved Whether the current stored email has been approved. This private root-only
 *   field mirrors current identity state. Pending verification lifecycle state is owned by
 *   [dev.inmo.wishlist.features.email.common.models.EmailProfile] and is never returned by admin APIs.
 */
@Serializable
data class AdminUser(
    val id: UserId,
    val username: Username,
    val email: Email?,
    val emailApproved: Boolean = false,
)

/**
 * Projects this [RegisteredUser] onto [AdminUser], retaining identity, current email, and approval.
 * Email-owned pending/request/deadline state is never accepted by this mapper or returned through
 * admin APIs.
 *
 * @return An [AdminUser] mirroring this user's [RegisteredUser.id], [RegisteredUser.username] and
 *   [RegisteredUser.email].
 */
fun RegisteredUser.asAdminUser(): AdminUser = AdminUser(
    id = id,
    username = username,
    email = email,
    emailApproved = emailApproved,
)

/**
 * Projects this [AdminUser] back onto the reduced persistence-layer [RegisteredUser], retaining
 * identity, current email, and approval only. Email-owned pending/request/deadline state is not
 * accepted as an argument and is never reconstructed through admin models.
 *
 * @return A [RegisteredUser] mirroring this model's [AdminUser.id], [AdminUser.username] and
 *   [AdminUser.email].
 */
fun AdminUser.asRegisteredUser(): RegisteredUser = RegisteredUser(
    id = id,
    username = username,
    email = email,
    emailApproved = emailApproved,
)
