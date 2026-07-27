package dev.inmo.wishlist.features.roles.server

import dev.inmo.kroles.repos.BaseRoleSubject
import dev.inmo.kroles.repos.RolesRepo
import dev.inmo.wishlist.features.roles.common.models.SuperAdminRole
import dev.inmo.wishlist.features.roles.common.models.NewUserRole
import dev.inmo.wishlist.features.roles.common.models.UserRole
import dev.inmo.wishlist.features.users.common.models.RegisteredUser
import dev.inmo.wishlist.features.users.common.repo.ReadUsersRepo
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Username of the single, hardcoded SuperAdmin recipient (issue #68 point 5). */
internal const val rootUsername = "root"

/** Serializes role transitions that must observe and update one account as a single operation. */
private val roleTransitionMutex = Mutex()

/**
 * Grants the configured default role to [user] and, when [user] is the `root` account, additionally
 * grants the SuperAdmin role. A non-root account receives [NewUserRole] while required-email
 * registration is enabled, otherwise [UserRole]. Required-email registration checks for an existing
 * [UserRole] while holding the same transition lock used by [promoteNewUserToUser], preventing a
 * delayed default-role callback from re-adding [NewUserRole] after verification.
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
    roleTransitionMutex.withLock {
        val subject = BaseRoleSubject.Direct(user.id.long.toString())
        when {
            user.username.string == rootUsername -> {
                rolesRepo.includeDirect(subject, UserRole)
                rolesRepo.includeDirect(subject, SuperAdminRole)
            }
            requireEmailForRegistration && !rolesRepo.contains(subject, UserRole) -> {
                rolesRepo.includeDirect(subject, NewUserRole)
            }
            else -> rolesRepo.includeDirect(subject, UserRole)
        }
    }
}

/**
 * Promotes a verified account to the normal user role.
 *
 * The complete exclusion/inclusion sequence shares a lock with [grantDefaultRoles], so a delayed
 * required-email callback cannot add [NewUserRole] after approval. Repeated verification-link opens
 * converge on exactly the approved role state.
 *
 * @param rolesRepo Repo roles are updated through.
 * @param userId Account being approved.
 */
suspend fun promoteNewUserToUser(rolesRepo: RolesRepo, userId: dev.inmo.wishlist.features.users.common.models.UserId) {
    roleTransitionMutex.withLock {
        val subject = BaseRoleSubject.Direct(userId.long.toString())
        rolesRepo.excludeDirect(subject, NewUserRole)
        rolesRepo.includeDirect(subject, UserRole)
    }
}

/**
 * One-time migration body (issue #68 point 6's "small migration"): applies [grantDefaultRoles] to
 * every currently-existing user. Extracted as a standalone, Koin/`VersionsRepo`-free function so it is
 * directly unit-testable — calling it twice in a row must preserve the same role state (verifies the
 * transition helper's idempotency at the migration-body level, independent of whatever gates how
 * many times production actually invokes it, i.e. `VersionsRepo.setTableVersion`).
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
