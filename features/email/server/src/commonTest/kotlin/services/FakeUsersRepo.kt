package dev.inmo.wishlist.features.email.server.services

import dev.inmo.micro_utils.repos.MapCRUDRepo
import dev.inmo.wishlist.features.users.common.models.NewUser
import dev.inmo.wishlist.features.users.common.models.RegisteredUser
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.users.common.models.Username
import dev.inmo.wishlist.features.users.common.repo.UsersRepo
import dev.inmo.wishlist.features.users.common.repo.exceptions.DuplicateUserFieldException

/**
 * In-memory [UsersRepo] test double backed by [MapCRUDRepo] (`dev.inmo:micro_utils.repos.inmemory`,
 * already on this module's test classpath transitively via `features/common/common`'s
 * `api libs.microutils.repos.cache` dependency — no new `build.gradle` entry needed). Mirrors the
 * composition shape of the production [dev.inmo.wishlist.features.users.common.repo.CacheUsersRepo]:
 * a library base class supplies the [dev.inmo.micro_utils.repos.CRUDRepo] surface, [getUserByUsername]
 * is added by hand.
 *
 * Enforces the same uniqueness rule the production
 * [dev.inmo.wishlist.features.users.common.repo.ExposedUsersRepo] enforces at the database level:
 * [updateObject] and [createObject] throw [DuplicateUserFieldException] when [NewUser.username] or a
 * non-null [NewUser.email] already belongs to a different stored user — a record never collides with
 * itself. This makes the fake a faithful double for exercising the duplicate-key-to-409 propagation
 * contract at the service layer without a live database.
 *
 * IDs are assigned sequentially starting one past the highest seeded [UserId]. [getUserByUsername]
 * performs a linear scan via `getAll()` — acceptable for the small fixtures used in these tests.
 *
 * @param initialUsers Users the repo is pre-seeded with, keyed by their [UserId].
 */
internal class FakeUsersRepo(
    initialUsers: Map<UserId, RegisteredUser> = emptyMap()
) : UsersRepo, MapCRUDRepo<RegisteredUser, UserId, NewUser>(initialUsers.toMutableMap()) {

    /** Next id assigned by [createObject], one past the highest id in the seeded map. */
    private var nextId: Long = (initialUsers.keys.maxOfOrNull { it.long } ?: 0L) + 1L

    /**
     * Applies [newValue] on top of [old], keeping [old]'s id.
     *
     * Reads [map] directly rather than through [getAll] — this is invoked from inside
     * [MapCRUDRepo]'s own `locker.withWriteLock { }` block (see `WriteMapCRUDRepo.update`), and
     * [getAll] acquires the same non-reentrant lock's read side, which would deadlock (the write
     * lock is already held by the very coroutine calling this method).
     *
     * @param newValue New username/email pair to apply.
     * @param id Id of the record being updated (used to exclude self from the uniqueness check).
     * @param old Current stored record.
     * @return Updated record.
     * @throws DuplicateUserFieldException when [newValue]'s username or non-null email already
     *   belongs to a different stored user.
     */
    override suspend fun updateObject(newValue: NewUser, id: UserId, old: RegisteredUser): RegisteredUser {
        if (map.values.any { it.id != id && it.username == newValue.username }) {
            throw DuplicateUserFieldException()
        }
        if (newValue.email != null && map.values.any { it.id != id && it.email == newValue.email }) {
            throw DuplicateUserFieldException()
        }
        return old.copy(username = newValue.username, email = newValue.email)
    }

    /**
     * Assigns the next sequential [UserId] and builds a [RegisteredUser] from [newValue].
     *
     * Reads [map] directly rather than through [getAll], for the same deadlock-avoidance reason
     * documented on [updateObject] — this runs inside `WriteMapCRUDRepo.create`'s write lock.
     *
     * @param newValue Username/email pair to persist.
     * @return The assigned id paired with the newly registered user.
     * @throws DuplicateUserFieldException when [newValue]'s username or non-null email already
     *   belongs to a stored user.
     */
    override suspend fun createObject(newValue: NewUser): Pair<UserId, RegisteredUser> {
        if (map.values.any { it.username == newValue.username }) {
            throw DuplicateUserFieldException()
        }
        if (newValue.email != null && map.values.any { it.email == newValue.email }) {
            throw DuplicateUserFieldException()
        }
        val id = UserId(nextId++)
        return id to RegisteredUser(id, newValue.username, newValue.email)
    }

    /**
     * Linear scan over all stored users for one matching [username].
     *
     * @param username Username to look up.
     * @return Matching user, or `null` when none is stored.
     */
    override suspend fun getUserByUsername(username: Username): RegisteredUser? =
        getAll().values.firstOrNull { it.username == username }
}
