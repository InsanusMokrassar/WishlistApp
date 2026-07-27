package dev.inmo.wishlist.features.roles.server

import dev.inmo.kroles.repos.BaseRoleSubject
import dev.inmo.kroles.repos.RolesRepo
import dev.inmo.wishlist.features.roles.common.models.SuperAdminRole
import dev.inmo.wishlist.features.roles.common.models.NewUserRole
import dev.inmo.wishlist.features.roles.common.models.UserRole
import dev.inmo.wishlist.features.users.common.models.RegisteredUser
import dev.inmo.wishlist.features.users.common.repo.ReadUsersRepo

/** Username of the single, hardcoded SuperAdmin recipient (issue #68 point 5). */
internal const val rootUsername = "root"

/**
 * Grants the configured default role to [user] and, when [user] is the `root` account, additionally
 * grants the SuperAdmin role. A non-root account receives [NewUserRole] while required-email
 * registration is enabled, otherwise [UserRole]. Idempotent — kroles' `RolesRepo.includeDirect` is a no-op (returns `false`, no
 * error) when the subject already holds the role — so this is safe to call more than once for the
 * same user (see `roles/README.md` Architecture Notes on the subscribe-then-backfill overlap window).
 *
 * Shared by [JVMPlugin]'s reactive `newObjectsFlow` subscription (point 6's "going forward" half) and
 * [backfillDefaultRoles] (point 6's one-time migration half), so the exact same rule governs both.
 *
 * @param rolesRepo Repo roles are granted through.
 * @param user User to grant default roles to.
 * @param requireEmailForRegistration Whether new non-root accounts await email verification.
 */
internal suspend fun grantDefaultRoles(
    rolesRepo: RolesRepo,
    user: RegisteredUser,
    requireEmailForRegistration: Boolean = false,
) {
    val subject = BaseRoleSubject.Direct(user.id.long.toString())
    if (user.username.string == rootUsername) {
        rolesRepo.includeDirect(subject, UserRole)
        rolesRepo.includeDirect(subject, SuperAdminRole)
    } else {
        rolesRepo.includeDirect(
            subject,
            if (requireEmailForRegistration) NewUserRole else UserRole
        )
    }
}

/**
 * Promotes a verified account to the normal user role.
 *
 * Exclusion and inclusion are both idempotent, so repeated verification-link opens converge on
 * exactly the approved role state.
 *
 * @param rolesRepo Repo roles are updated through.
 * @param userId Account being approved.
 */
suspend fun promoteNewUserToUser(rolesRepo: RolesRepo, userId: dev.inmo.wishlist.features.users.common.models.UserId) {
    val subject = BaseRoleSubject.Direct(userId.long.toString())
    rolesRepo.excludeDirect(subject, NewUserRole)
    rolesRepo.includeDirect(subject, UserRole)
}

/**
 * One-time migration body (issue #68 point 6's "small migration"): applies [grantDefaultRoles] to
 * every currently-existing user. Extracted as a standalone, Koin/`VersionsRepo`-free function so it is
 * directly unit-testable — calling it twice in a row must be a no-op the second time (verifies
 * [grantDefaultRoles]'s `includeDirect` idempotency at the migration-body level, independent of
 * whatever gates how many times production actually invokes it, i.e. `VersionsRepo.setTableVersion`).
 *
 * @param usersRepo Source of all currently-existing users.
 * @param rolesRepo Repo roles are granted through.
 * @param requireEmailForRegistration Ignored for existing accounts; existing accounts are always
 *   backfilled with [UserRole] and are never downgraded to [NewUserRole].
 */
internal suspend fun backfillDefaultRoles(
    usersRepo: ReadUsersRepo,
    rolesRepo: RolesRepo,
    requireEmailForRegistration: Boolean = false,
) {
    usersRepo.getAll().values.forEach { user ->
        grantDefaultRoles(rolesRepo, user, requireEmailForRegistration = false)
    }
}
