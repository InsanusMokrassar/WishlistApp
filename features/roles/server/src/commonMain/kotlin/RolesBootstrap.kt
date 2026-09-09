package dev.inmo.wishlist.features.roles.server

import dev.inmo.kroles.repos.BaseRoleSubject
import dev.inmo.kroles.repos.RolesRepo
import dev.inmo.wishlist.features.roles.common.models.SuperAdminRole
import dev.inmo.wishlist.features.roles.common.models.NewUserRole
import dev.inmo.wishlist.features.roles.common.models.UserRole
import dev.inmo.wishlist.features.users.common.models.RegisteredUser
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.users.common.repo.ReadUsersRepo
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Username of the single, hardcoded SuperAdmin recipient (issue #68 point 5). */
internal const val rootUsername = "root"

/** Serializes role transitions that must observe and update one account as a single operation. */
private val roleTransitionMutex = Mutex()

/**
 * Grants the generic default role to [user] and, when [user] is the `root` account, additionally
 * grants the SuperAdmin role. An explicitly pending account keeps [NewUserRole] without receiving
 * [UserRole]; required self-registration establishes that state through [markNewUserPending].
 *
 * Shared by [JVMPlugin]'s reactive `newObjectsFlow` subscription (point 6's "going forward" half) and
 * [backfillDefaultRoles] (point 6's one-time migration half), so the exact same rule governs both.
 *
 * @param rolesRepo Repo roles are granted through.
 * @param user User to grant default roles to.
 */
internal suspend fun grantDefaultRoles(
    rolesRepo: RolesRepo,
    user: RegisteredUser,
) {
    roleTransitionMutex.withLock {
        grantDefaultRolesWhileLocked(rolesRepo, user)
    }
}

/**
 * Grants generic roles only when the account still exists, closing delayed creation-callback races.
 *
 * @param usersRepo Authoritative account lookup.
 * @param rolesRepo Repo roles are granted through.
 * @param user User emitted by the creation flow.
 */
internal suspend fun grantDefaultRolesIfUserExists(
    usersRepo: ReadUsersRepo,
    rolesRepo: RolesRepo,
    user: RegisteredUser,
) {
    roleTransitionMutex.withLock {
        val storedUser = usersRepo.getById(user.id) ?: return@withLock
        grantDefaultRolesWhileLocked(rolesRepo, storedUser)
    }
}

/** Applies the generic grant rule while [roleTransitionMutex] is already held. */
private suspend fun grantDefaultRolesWhileLocked(rolesRepo: RolesRepo, user: RegisteredUser) {
    val subject = roleSubject(user.id)
    when {
        user.username.string == rootUsername -> {
            rolesRepo.excludeDirect(subject, NewUserRole)
            rolesRepo.includeDirect(subject, UserRole)
            rolesRepo.includeDirect(subject, SuperAdminRole)
        }
        rolesRepo.contains(subject, NewUserRole) -> Unit
        else -> rolesRepo.includeDirect(subject, UserRole)
    }
}

/**
 * Replaces the approved role with the pending role for required self-registration.
 *
 * @param rolesRepo Repo roles are updated through.
 * @param userId Provisional account awaiting verification.
 * @return `true` when the final direct state contains only the pending user role.
 */
internal suspend fun markNewUserPending(rolesRepo: RolesRepo, userId: UserId): Boolean {
    return roleTransitionMutex.withLock {
        val subject = roleSubject(userId)
        rolesRepo.excludeDirect(subject, UserRole)
        rolesRepo.includeDirect(subject, NewUserRole)
        rolesRepo.contains(subject, NewUserRole) && !rolesRepo.contains(subject, UserRole)
    }
}

/**
 * Synchronously grants and confirms direct approved-user membership for optional registration.
 *
 * A pending account remains pending and is never upgraded by this helper. The shared transition
 * mutex makes this mutually exclusive with the required-email pending transition.
 */
internal suspend fun ensureDirectUserRole(rolesRepo: RolesRepo, userId: UserId): Boolean {
    return roleTransitionMutex.withLock {
        val subject = roleSubject(userId)
        if (rolesRepo.contains(subject, NewUserRole)) return@withLock false
        rolesRepo.includeDirect(subject, UserRole)
        UserRole in rolesRepo.getDirectRoles(subject)
    }
}

/** Returns whether [userId] currently has direct approved-user membership. */
internal suspend fun hasDirectUserRole(rolesRepo: RolesRepo, userId: UserId): Boolean =
    roleTransitionMutex.withLock {
        UserRole in rolesRepo.getDirectRoles(roleSubject(userId))
    }

/**
 * Removes every direct role for one user under the shared transition lock.
 *
 * @param rolesRepo Repo roles are removed through.
 * @param userId Deleted or compensated account.
 */
internal suspend fun removeDirectUserRoles(rolesRepo: RolesRepo, userId: UserId) {
    roleTransitionMutex.withLock {
        val subject = roleSubject(userId)
        rolesRepo.getDirectRoles(subject).forEach { role ->
            rolesRepo.excludeDirect(subject, role)
        }
    }
}

/**
 * Promotes a verified account to the normal user role.
 *
 * The complete inclusion/confirmation/exclusion sequence shares a lock with [grantDefaultRoles], so
 * a delayed required-email callback cannot add [NewUserRole] after approval. A pending marker remains
 * in place until direct [UserRole] membership is observable: an ordinary failed grant can therefore
 * be retried through the same verification link without granting access to a revoked subject.
 * Repeated verification-link opens converge on exactly the approved role state.
 *
 * @param rolesRepo Repo roles are updated through.
 * @param userId Account being approved.
 * @return `true` when the account was already approved or was pending and became approved; `false`
 *   when no direct pending/approved role exists, which prevents a revoked account from regaining access.
 */
suspend fun promoteNewUserToUser(rolesRepo: RolesRepo, userId: UserId): Boolean =
    roleTransitionMutex.withLock {
        val subject = roleSubject(userId)
        val roles = rolesRepo.getDirectRoles(subject)
        when {
            NewUserRole in roles -> {
                if (UserRole !in roles) {
                    rolesRepo.includeDirect(subject, UserRole)
                    if (UserRole !in rolesRepo.getDirectRoles(subject)) return@withLock false
                }
                rolesRepo.excludeDirect(subject, NewUserRole)
                val resultingRoles = rolesRepo.getDirectRoles(subject)
                UserRole in resultingRoles && NewUserRole !in resultingRoles
            }
            UserRole in roles || SuperAdminRole in roles -> true
            else -> false
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
 */
internal suspend fun backfillDefaultRoles(
    usersRepo: ReadUsersRepo,
    rolesRepo: RolesRepo,
) {
    usersRepo.getAll().values.forEach { user ->
        grantDefaultRoles(rolesRepo, user)
    }
}

/** Converts an application user id into the direct role-subject representation. */
private fun roleSubject(userId: UserId): BaseRoleSubject.Direct =
    BaseRoleSubject.Direct(userId.long.toString())
