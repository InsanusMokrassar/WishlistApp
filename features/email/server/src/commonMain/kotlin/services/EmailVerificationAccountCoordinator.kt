package dev.inmo.wishlist.features.email.server.services

import dev.inmo.kroles.repos.RolesRepo
import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.roles.server.promoteNewUserToUser
import dev.inmo.wishlist.features.users.common.models.NewUser
import dev.inmo.wishlist.features.users.common.models.RegisteredUser
import dev.inmo.wishlist.features.users.common.models.Username
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.users.common.repo.UsersRepo
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Serializes self-service email mutation with verification approval for every account in one
 * server process.
 *
 * The process-local mutex covers each complete state-dependent operation: mutation keeps the user
 * lookup and update together, while verification keeps the nullable invited-address check, current
 * user lookup, exact address comparison, and pending-to-approved role transition together.
 * Repository failures propagate unchanged, and [Mutex.withLock] releases the critical section on
 * normal return, failure, or cancellation.
 *
 * @param usersRepo User repository read and updated by coordinated account operations.
 * @param rolesRepo Role repository used for the coordinated verification promotion.
 */
class EmailVerificationAccountCoordinator(
    private val usersRepo: UsersRepo,
    private val rolesRepo: RolesRepo,
) {
    /** Process-local serialization boundary shared by mutation and verification. */
    private val mutex = Mutex()

    /**
     * Updates or clears one user's stored email while excluding concurrent verification.
     *
     * @param userId User whose stored address is changed.
     * @param email New address, or `null` to clear the current address.
     * @return `true` when the update produced a stored record; `false` when the user is missing.
     * @throws dev.inmo.wishlist.features.users.common.repo.exceptions.DuplicateUserFieldException
     * when [email] belongs to another user; the repository exception propagates unchanged.
     */
    suspend fun updateStoredEmail(userId: UserId, email: Email?): Boolean = mutex.withLock {
        val user = usersRepo.getById(userId) ?: return@withLock false
        usersRepo.update(userId, NewUser(user.username, email)) != null
    }

    /**
     * Replaces all ordinary editable user fields under the same process-local account mutex.
     *
     * Admin full replacements must share the coordinator with self-service updates and deeplink
     * verification so an email replacement cannot interleave between verification checks.
     *
     * @param userId User to replace.
     * @param user Replacement username/address values.
     * @return `true` when persisted, `false` on an unexpected failed update, or `null` when absent.
     */
    suspend fun updateUser(userId: UserId, user: NewUser): Boolean? = mutex.withLock {
        if (usersRepo.getById(userId) == null) return@withLock null
        usersRepo.update(userId, user) != null
    }

    /**
     * Replaces only a user's username while preserving the latest stored email and its approval state.
     *
     * @param userId User whose username changes.
     * @param username New username.
     * @return `true` when persisted, `false` on an unexpected failed update, or `null` when absent.
     */
    suspend fun updateUsername(userId: UserId, username: Username): Boolean? = mutex.withLock {
        val user = usersRepo.getById(userId) ?: return@withLock null
        usersRepo.update(userId, NewUser(username, user.email)) != null
    }

    /**
     * Returns the current private record for [userId] under the shared account mutex.
     *
     * The caller must still compare the returned record after asynchronous delivery because SMTP is
     * intentionally outside this lock.
     *
     * @param userId Authenticated account to inspect.
     * @return The current private user record, or `null` when no account remains.
     */
    suspend fun getCurrentUser(userId: UserId): RegisteredUser? = mutex.withLock {
        usersRepo.getById(userId)
    }

    /**
     * Promotes a pending account only while its current stored email equals the invited address.
     *
     * The full nullable-address check, user lookup, equality check, and role transition execute
     * under the same mutex used by [updateStoredEmail]. Legacy null-email payloads, missing users,
     * cleared addresses, and mismatches therefore fail without changing roles.
     *
     * @param userId Pending account referenced by the verification payload.
     * @param invitedEmail Address that received the verification link, or `null` for a legacy
     * payload that must fail closed.
     * @return `true` after a matching account is promoted; `false` for a stale or invalid payload.
     */
    suspend fun verifyInvitedEmailAndPromote(userId: UserId, invitedEmail: Email?): Boolean =
        mutex.withLock {
            val expectedEmail = invitedEmail ?: return@withLock false
            val user = usersRepo.getById(userId) ?: return@withLock false
            if (user.email != expectedEmail) return@withLock false
            if (usersRepo.approveEmail(userId, expectedEmail) == null) return@withLock false
            promoteNewUserToUser(rolesRepo, userId)
        }
}
