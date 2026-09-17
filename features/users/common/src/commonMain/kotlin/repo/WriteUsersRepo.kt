package dev.inmo.wishlist.features.users.common.repo

import dev.inmo.micro_utils.repos.WriteCRUDRepo
import dev.inmo.wishlist.features.users.common.models.NewUser
import dev.inmo.wishlist.features.users.common.models.RegisteredUser
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.email.common.models.Email

/**
 * Write-only surface of the users CRUD repository.
 *
 * The only production implementation whose writes can fail on a constraint collision is the
 * JVM-only [dev.inmo.wishlist.features.users.common.repo.ExposedUsersRepo] (reached through
 * [CacheUsersRepo]): its `update`/`create` throw
 * [dev.inmo.wishlist.features.users.common.repo.exceptions.DuplicateUserFieldException] when the
 * given username or non-null email already belongs to a different user. Email lifecycle writes
 * retain the latest approved address in `email`, use `pendingEmail` for a replacement until exact
 * verification approval, and enforce a persisted post-approval deadline. See that exception's
 * KDoc for the full propagation path.
 */
interface WriteUsersRepo : WriteCRUDRepo<RegisteredUser, UserId, NewUser> {
    /** Applies the lifecycle-aware email mutation for [id].
     *
     * A replacement after approval is stored as a pending candidate while the approved current
     * address remains unchanged. Clearing is also a state-changing lifecycle mutation and is
     * rejected while the persisted cooldown deadline is active; an exact deadline permits the
     * mutation. Same-slot no-ops do not cancel a pending candidate.
     * @param id User whose email lifecycle is changed.
     * @param email Replacement address, or `null` for explicit clearing.
     * @return Updated record, or `null` when [id] is absent.
     * @throws dev.inmo.wishlist.features.users.common.repo.exceptions.EmailChangeCooldownException
     *   when the requested lifecycle mutation is blocked until its persisted deadline.
     */
    suspend fun setEmail(id: UserId, email: Email?): RegisteredUser?

    /** Changes only the login name, preserving all email lifecycle state.
     * @param id User whose username is changed.
     * @param username Replacement login name.
     * @return Updated record, or `null` when [id] is absent.
     */
    suspend fun updateUsername(id: UserId, username: dev.inmo.wishlist.features.users.common.models.Username): RegisteredUser?
    /**
     * Marks the exact verification candidate of [id] approved only when it still equals [expectedEmail].
     *
     * Approval is deliberately a repository-owned mutation rather than a caller-writable field on
     * [NewUser], preventing stale verification links or ordinary user edits from approving another
     * address. For a replacement, approval promotes `pendingEmail` to `email`, clears the pending
     * slot, and may issue a deadline from the approval clock. Replaying the already-approved exact
     * current address is a no-op.
     *
     * @param id User whose address is being approved.
     * @param expectedEmail Exact address bound into the verification link.
     * @param cooldownMillis Non-negative cooldown duration in milliseconds; zero disables a newly
     *   issued deadline.
     * @return The current approved user, or `null` when the user is absent or the candidate changed.
     */
    suspend fun approveEmail(id: UserId, expectedEmail: Email, cooldownMillis: Long = 0): RegisteredUser?
}
