package dev.inmo.wishlist.features.roles.server

import dev.inmo.micro_utils.repos.MapCRUDRepo
import dev.inmo.micro_utils.coroutines.withWriteLock
import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.email.common.models.EmailProfile
import dev.inmo.wishlist.features.users.common.models.NewUser
import dev.inmo.wishlist.features.users.common.models.RegisteredUser
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.users.common.models.Username
import dev.inmo.wishlist.features.users.common.repo.UsersRepo

/**
 * In-memory [UsersRepo] test double backed by [MapCRUDRepo] — same composition shape as
 * `email/server`'s `FakeUsersRepo` (a library base class supplies the CRUD surface, including the
 * real `WriteCRUDRepo.newObjectsFlow` this module's reactive-subscription tests exercise directly).
 * No duplicate-username/email enforcement — not exercised by any test in this module.
 *
 * @param initialUsers Users the repo is pre-seeded with, keyed by their [UserId].
 */
internal class FakeUsersRepo(
    initialUsers: Map<UserId, RegisteredUser> = emptyMap()
) : UsersRepo, MapCRUDRepo<RegisteredUser, UserId, NewUser>(initialUsers.toMutableMap()) {
    private var nextId: Long = (initialUsers.keys.maxOfOrNull { it.long } ?: 0L) + 1L

    override suspend fun updateObject(newValue: NewUser, id: UserId, old: RegisteredUser): RegisteredUser =
        old.copy(
            username = newValue.username,
            email = newValue.email,
            emailApproved = newValue.email != null && old.email == newValue.email && old.emailApproved,
        )

    override suspend fun createObject(newValue: NewUser): Pair<UserId, RegisteredUser> {
        val id = UserId(nextId++)
        return id to RegisteredUser(id, newValue.username, newValue.email)
    }

    override suspend fun getUserByUsername(username: Username): RegisteredUser? =
        getAll().values.firstOrNull { it.username == username }

    /** Rejects email lifecycle reads outside this roles-focused fixture's supported surface. */
    override suspend fun getEmailProfileFresh(id: UserId): EmailProfile? =
        error("Email profile reads are not exercised by this roles fake")

    override suspend fun setEmail(id: UserId, email: Email?): RegisteredUser? = locker.withWriteLock {
        val current = map[id] ?: return@withWriteLock null
        when {
            email == current.email || email == current.pendingEmail -> current
            email == null -> current.copy(email = null, emailApproved = false, pendingEmail = null, emailChangeAllowedAt = null)
            current.emailApproved && current.email != null -> current.copy(pendingEmail = email)
            else -> current.copy(email = email, emailApproved = false, pendingEmail = null)
        }.also { map[id] = it }
    }?.also { _updatedObjectsFlow.emit(it) }

    override suspend fun updateUsername(id: UserId, username: Username): RegisteredUser? = locker.withWriteLock {
        map[id]?.copy(username = username)?.also { map[id] = it }
    }?.also { _updatedObjectsFlow.emit(it) }

    override suspend fun approveEmail(id: UserId, expectedEmail: Email, cooldownMillis: Long): RegisteredUser? =
        locker.withWriteLock {
            val current = map[id] ?: return@withWriteLock null
            when {
                current.pendingEmail == expectedEmail -> current.copy(
                    email = expectedEmail,
                    emailApproved = true,
                    pendingEmail = null,
                    emailChangeAllowedAt = cooldownMillis.takeIf { it > 0L },
                )
                current.email == expectedEmail && !current.emailApproved && current.pendingEmail == null -> current.copy(
                    emailApproved = true,
                    emailChangeAllowedAt = cooldownMillis.takeIf { it > 0L },
                )
                current.email == expectedEmail && current.emailApproved && current.pendingEmail == null -> current
                else -> return@withWriteLock null
            }.also { map[id] = it }
        }?.also { _updatedObjectsFlow.emit(it) }
}
