package dev.inmo.wishlist.features.email.server.services

import dev.inmo.micro_utils.repos.MapCRUDRepo
import dev.inmo.wishlist.features.users.common.models.NewUser
import dev.inmo.wishlist.features.users.common.models.RegisteredUser
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.users.common.models.Username
import dev.inmo.wishlist.features.users.common.repo.UsersRepo

/**
 * In-memory [UsersRepo] test double backed by [MapCRUDRepo] (`dev.inmo:micro_utils.repos.inmemory`,
 * already on this module's test classpath transitively via `features/common/common`'s
 * `api libs.microutils.repos.cache` dependency — no new `build.gradle` entry needed). Mirrors the
 * composition shape of the production [dev.inmo.wishlist.features.users.common.repo.CacheUsersRepo]:
 * a library base class supplies the [dev.inmo.micro_utils.repos.CRUDRepo] surface, [getUserByUsername]
 * is added by hand.
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
     * @param newValue New username/email pair to apply.
     * @param id Id of the record being updated (unused — [old] already carries it).
     * @param old Current stored record.
     * @return Updated record.
     */
    override suspend fun updateObject(newValue: NewUser, id: UserId, old: RegisteredUser): RegisteredUser =
        old.copy(username = newValue.username, email = newValue.email)

    /**
     * Assigns the next sequential [UserId] and builds a [RegisteredUser] from [newValue].
     *
     * @param newValue Username/email pair to persist.
     * @return The assigned id paired with the newly registered user.
     */
    override suspend fun createObject(newValue: NewUser): Pair<UserId, RegisteredUser> {
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
