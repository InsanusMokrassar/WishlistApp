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
 * given username or non-null email already belongs to a different user. See that exception's
 * KDoc for the full propagation path.
 */
interface WriteUsersRepo : WriteCRUDRepo<RegisteredUser, UserId, NewUser> {
    /** Applies the lifecycle-aware email mutation for [id].
     * @param id User whose email lifecycle is changed.
     * @param email Replacement address, or `null` for explicit clearing.
     * @return Updated record, or `null` when [id] is absent.
     */
    suspend fun setEmail(id: UserId, email: Email?): RegisteredUser?

    /** Changes only the login name, preserving all email lifecycle state.
     * @param id User whose username is changed.
     * @param username Replacement login name.
     * @return Updated record, or `null` when [id] is absent.
     */
    suspend fun updateUsername(id: UserId, username: dev.inmo.wishlist.features.users.common.models.Username): RegisteredUser?
    /**
     * Marks the current address of [id] approved only when it still exactly equals [expectedEmail].
     *
     * Approval is deliberately a repository-owned mutation rather than a caller-writable field on
     * [NewUser], preventing stale verification links or ordinary user edits from approving another
     * address.
     *
     * @param id User whose address is being approved.
     * @param expectedEmail Exact address bound into the verification link.
     * @return The current approved user, or `null` when the user is absent or the address changed.
     */
    suspend fun approveEmail(id: UserId, expectedEmail: Email, cooldownMillis: Long = 0): RegisteredUser?
}
