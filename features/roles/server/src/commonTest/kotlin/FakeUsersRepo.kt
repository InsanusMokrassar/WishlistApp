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

    /** Email verification state independent from the reduced user fixture. */
    private val emailProfiles = initialUsers.mapValues { (_, user) -> user.asEmailProfile() }.toMutableMap()

    override suspend fun updateObject(newValue: NewUser, id: UserId, old: RegisteredUser): RegisteredUser {
        val currentProfile = emailProfiles[id] ?: old.asEmailProfile()
        val profile = when {
            newValue.email == currentProfile.email || newValue.email == currentProfile.pendingEmail -> currentProfile
            newValue.email == null -> EmailProfile(userId = id.long)
            currentProfile.emailApproved && currentProfile.email != null -> currentProfile.copy(pendingEmail = newValue.email)
            else -> EmailProfile(userId = id.long, email = newValue.email)
        }
        emailProfiles[id] = profile
        return old.copy(
            username = newValue.username,
            email = profile.email,
            emailApproved = profile.emailApproved,
        )
    }

    override suspend fun createObject(newValue: NewUser): Pair<UserId, RegisteredUser> {
        val id = UserId(nextId++)
        val user = RegisteredUser(id, newValue.username, newValue.email)
        emailProfiles[id] = user.asEmailProfile()
        return id to user
    }

    override suspend fun getUserByUsername(username: Username): RegisteredUser? =
        getAll().values.firstOrNull { it.username == username }

    /** Returns independent email-owned lifecycle state. */
    override suspend fun getEmailProfileFresh(id: UserId): EmailProfile? =
        map[id]?.let { emailProfiles[id] ?: it.asEmailProfile() }

    override suspend fun setEmail(id: UserId, email: Email?): RegisteredUser? = locker.withWriteLock {
        val current = map[id] ?: return@withWriteLock null
        val profile = emailProfiles[id] ?: current.asEmailProfile()
        val updatedProfile = when {
            email == profile.email || email == profile.pendingEmail -> profile
            email == null -> EmailProfile(userId = id.long)
            profile.emailApproved && profile.email != null -> profile.copy(pendingEmail = email)
            else -> EmailProfile(userId = id.long, email = email)
        }
        emailProfiles[id] = updatedProfile
        current.withEmailProfile(updatedProfile).also { map[id] = it }
    }?.also { _updatedObjectsFlow.emit(it) }

    override suspend fun updateUsername(id: UserId, username: Username): RegisteredUser? = locker.withWriteLock {
        map[id]?.copy(username = username)?.also { map[id] = it }
    }?.also { _updatedObjectsFlow.emit(it) }

    override suspend fun approveEmail(id: UserId, expectedEmail: Email, cooldownMillis: Long): RegisteredUser? =
        locker.withWriteLock {
            val current = map[id] ?: return@withWriteLock null
            val profile = emailProfiles[id] ?: current.asEmailProfile()
            val approvedProfile = when {
                profile.pendingEmail == expectedEmail -> profile.copy(
                    email = expectedEmail,
                    emailApproved = true,
                    pendingEmail = null,
                    emailChangeAllowedAt = cooldownMillis.takeIf { it > 0L },
                )
                profile.email == expectedEmail && !profile.emailApproved && profile.pendingEmail == null -> profile.copy(
                    emailApproved = true,
                    emailChangeAllowedAt = cooldownMillis.takeIf { it > 0L },
                )
                profile.email == expectedEmail && profile.emailApproved && profile.pendingEmail == null -> profile
                else -> return@withWriteLock null
            }
            emailProfiles[id] = approvedProfile
            current.withEmailProfile(approvedProfile).also { map[id] = it }
        }?.also { _updatedObjectsFlow.emit(it) }

    /** Maps reduced fixture identity to email-owned state. */
    private fun RegisteredUser.asEmailProfile(): EmailProfile = EmailProfile(
        userId = id.long,
        email = email,
        emailApproved = emailApproved,
    )

    /** Synchronizes only current email identity from an email-owned profile. */
    private fun RegisteredUser.withEmailProfile(profile: EmailProfile): RegisteredUser = copy(
        email = profile.email,
        emailApproved = profile.emailApproved,
    )
}
