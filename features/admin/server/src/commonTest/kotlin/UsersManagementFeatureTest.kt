package dev.inmo.wishlist.features.admin.server

import dev.inmo.micro_utils.repos.KeyValueRepo
import dev.inmo.micro_utils.repos.MapCRUDRepo
import dev.inmo.micro_utils.coroutines.withWriteLock
import dev.inmo.micro_utils.repos.MapKeyValueRepo
import dev.inmo.micro_utils.pagination.Pagination
import dev.inmo.micro_utils.pagination.PaginationResult
import dev.inmo.micro_utils.pagination.createPaginationResult
import dev.inmo.kroles.repos.BaseRoleSubject
import dev.inmo.kroles.repos.RolesRepo
import dev.inmo.kroles.roles.BaseRole
import dev.inmo.wishlist.features.admin.common.models.AdminUser
import dev.inmo.wishlist.features.admin.common.models.NewUserWithPassword
import dev.inmo.wishlist.features.admin.common.models.asAdminUser
import dev.inmo.wishlist.features.auth.common.models.Password
import dev.inmo.wishlist.features.auth.server.repo.PasswordsRepo
import dev.inmo.wishlist.features.auth.server.services.AuthFeatureService
import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.email.common.models.EmailProfile
import dev.inmo.wishlist.features.email.server.services.EmailVerificationAccountCoordinator
import dev.inmo.wishlist.features.users.common.models.NewUser
import dev.inmo.wishlist.features.users.common.models.RegisteredUser
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.users.common.models.Username
import dev.inmo.wishlist.features.users.common.repo.UsersRepo
import dev.inmo.wishlist.features.users.common.repo.ExposedUsersRepo
import dev.inmo.wishlist.features.users.common.repo.exceptions.EmailChangeCooldownException
import dev.inmo.wishlist.features.wishlist.common.models.NewWishlist
import dev.inmo.wishlist.features.wishlist.common.models.NewWishlistItem
import dev.inmo.wishlist.features.wishlist.common.models.RegisteredWishlist
import dev.inmo.wishlist.features.wishlist.common.models.RegisteredWishlistItem
import dev.inmo.wishlist.features.wishlist.common.models.WishlistId
import dev.inmo.wishlist.features.wishlist.common.models.WishlistItemId
import dev.inmo.wishlist.features.wishlist.common.repo.WishlistItemRepo
import dev.inmo.wishlist.features.wishlist.common.repo.WishlistRepo
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import java.nio.file.Files

/**
 * In-memory [UsersRepo] test double seeded with reduced user fixtures and independent email profiles.
 *
 * @param initialUsers Users the repository is pre-seeded with, keyed by [UserId].
 * @param initialEmailProfiles Email-owned state seeded independently from reduced user fixtures.
 */
internal class FakeUsersRepo(
    initialUsers: Map<UserId, RegisteredUser> = emptyMap(),
    initialEmailProfiles: Map<UserId, EmailProfile> = emptyMap(),
) : UsersRepo, MapCRUDRepo<RegisteredUser, UserId, NewUser>(initialUsers.toMutableMap()) {

    private var nextId: Long = (initialUsers.keys.maxOfOrNull { it.long } ?: 0L) + 1L

    /** Email verification state independent from the reduced user fixture. */
    private val emailProfiles = initialUsers.mapValues { (_, user) -> user.asEmailProfile() }
        .toMutableMap()
        .also { it.putAll(initialEmailProfiles) }

    override suspend fun updateObject(newValue: NewUser, id: UserId, old: RegisteredUser): RegisteredUser {
        val currentProfile = emailProfiles[id] ?: old.asEmailProfile()
        val profile = when {
            newValue.email == null -> EmailProfile(userId = id.long)
            newValue.email == currentProfile.email || newValue.email == currentProfile.pendingEmail -> currentProfile
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
            email == null -> EmailProfile(userId = id.long)
            email == profile.email || email == profile.pendingEmail -> profile
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

/** In-memory [PasswordsRepo] test double delegating entirely to [MapKeyValueRepo]. */
internal class FakePasswordsRepo : PasswordsRepo, KeyValueRepo<UserId, Password> by MapKeyValueRepo()

/** Minimal role-store fixture; user update tests never invoke its role operations. */
internal object NoopRolesRepo : RolesRepo {
    override val roleIncluded: Flow<Pair<BaseRoleSubject, BaseRole>> = emptyFlow()
    override val roleExcluded: Flow<Pair<BaseRoleSubject, BaseRole>> = emptyFlow()
    override val roleCreated: Flow<BaseRole> = emptyFlow()
    override val roleRemoved: Flow<BaseRole> = emptyFlow()

    override suspend fun getDirectSubjects(role: BaseRole): List<BaseRoleSubject> = emptyList()
    override suspend fun getDirectRoles(subject: BaseRoleSubject): List<BaseRole> = emptyList()
    override suspend fun getAll(): Map<BaseRoleSubject, List<BaseRole>> = emptyMap()
    override suspend fun getAllRolesByPagination(
        pagination: Pagination,
        reversed: Boolean,
    ): PaginationResult<BaseRole> = emptyList<BaseRole>().createPaginationResult(pagination, 0)
    override suspend fun getAllSubjectsByPagination(
        pagination: Pagination,
        reversed: Boolean,
    ): PaginationResult<BaseRoleSubject> = emptyList<BaseRoleSubject>().createPaginationResult(pagination, 0)
    override suspend fun contains(subject: BaseRoleSubject, role: BaseRole): Boolean = false
    override suspend fun containsAny(subject: BaseRoleSubject, roles: List<BaseRole>): Boolean = false
    override suspend fun includeDirect(subject: BaseRoleSubject, role: BaseRole): Boolean = false
    override suspend fun excludeDirect(subject: BaseRoleSubject, role: BaseRole): Boolean = false
    override suspend fun createRole(newRole: BaseRole): Boolean = false
    override suspend fun removeRole(role: BaseRole): Boolean = false
}

/** In-memory [WishlistRepo] test double. Empty by default — [UsersManagementFeature.getAll]/`create` never read it. */
internal class FakeWishlistRepo(
    initialWishlists: Map<WishlistId, RegisteredWishlist> = emptyMap()
) : WishlistRepo, MapCRUDRepo<RegisteredWishlist, WishlistId, NewWishlist>(initialWishlists.toMutableMap()) {

    override suspend fun updateObject(newValue: NewWishlist, id: WishlistId, old: RegisteredWishlist): RegisteredWishlist =
        old.copy(userId = newValue.userId, title = newValue.title, defaultPriceUnits = newValue.defaultPriceUnits)

    override suspend fun createObject(newValue: NewWishlist): Pair<WishlistId, RegisteredWishlist> =
        WishlistId(1L) to RegisteredWishlist(WishlistId(1L), newValue.userId, newValue.title, newValue.defaultPriceUnits)

    override suspend fun getByUserId(userId: UserId): List<RegisteredWishlist> =
        getAll().values.filter { it.userId == userId }
}

/** In-memory [WishlistItemRepo] test double. Empty by default — [UsersManagementFeature.getAll]/`create` never read it. */
internal class FakeWishlistItemRepo(
    initialItems: Map<WishlistItemId, RegisteredWishlistItem> = emptyMap()
) : WishlistItemRepo, MapCRUDRepo<RegisteredWishlistItem, WishlistItemId, NewWishlistItem>(initialItems.toMutableMap()) {

    override suspend fun updateObject(newValue: NewWishlistItem, id: WishlistItemId, old: RegisteredWishlistItem): RegisteredWishlistItem = old

    override suspend fun createObject(newValue: NewWishlistItem): Pair<WishlistItemId, RegisteredWishlistItem> =
        WishlistItemId(1L) to RegisteredWishlistItem(WishlistItemId(1L), newValue.wishlistId, newValue.title)

    override suspend fun getByWishlistId(wishlistId: WishlistId): List<RegisteredWishlistItem> =
        getAll().values.filter { it.wishlistId == wishlistId }

    override suspend fun getByIds(ids: List<WishlistItemId>): List<RegisteredWishlistItem> {
        val all = getAll()
        return ids.distinct().mapNotNull { all[it] }
    }
}

/**
 * Verifies [UsersManagementFeature.getAll]/[UsersManagementFeature.create] return [AdminUser],
 * preserving [RegisteredUser.email] — this is a root-only surface, unlike Commit A's
 * `UsersFeatureUser`, which must drop email.
 */
class UsersManagementFeatureTest {

    private val userWithEmail = RegisteredUser(UserId(1L), Username("alice"), Email("alice@example.com"))
    private val userWithoutEmail = RegisteredUser(UserId(2L), Username("bob"))

    private fun buildFeature(usersRepo: UsersRepo, cooldownMillis: Long = 0L): UsersManagementFeature {
        val authService = AuthFeatureService(usersRepo, usersRepo, FakePasswordsRepo())
        return UsersManagementFeature(
            usersRepo,
            authService,
            FakeWishlistRepo(),
            FakeWishlistItemRepo(),
            EmailVerificationAccountCoordinator(usersRepo, NoopRolesRepo, cooldownMillis),
        )
    }

    /** Full fixture updates clear explicit null addresses before nullable same-slot no-op checks. */
    @Test
    fun fakeFullUpdateClearsLifecycleAndRetainsSameAddressNoOps() = runTest {
        val unapproved = RegisteredUser(UserId(71L), Username("fake-unapproved"), Email("fake-unapproved@example.com"))
        val approved = RegisteredUser(UserId(72L), Username("fake-approved"), Email("fake-approved@example.com"), true)
        val unapprovedProfile = EmailProfile(
            userId = unapproved.id.long,
            email = unapproved.email,
            emailChangeRequestedAt = 10L,
        )
        val approvedProfile = EmailProfile(
            userId = approved.id.long,
            email = approved.email,
            emailApproved = true,
            emailChangeAllowedAt = 20L,
        )
        val usersRepo = FakeUsersRepo(
            initialUsers = mapOf(unapproved.id to unapproved, approved.id to approved),
            initialEmailProfiles = mapOf(unapproved.id to unapprovedProfile, approved.id to approvedProfile),
        )

        assertEquals(listOf(unapproved), usersRepo.update(listOf(unapproved.id to NewUser(unapproved.username, unapproved.email))))
        assertEquals(unapprovedProfile, usersRepo.getEmailProfileFresh(unapproved.id))
        assertEquals(listOf(approved), usersRepo.update(listOf(approved.id to NewUser(approved.username, approved.email))))
        assertEquals(approvedProfile, usersRepo.getEmailProfileFresh(approved.id))

        assertEquals(
            listOf(RegisteredUser(unapproved.id, unapproved.username)),
            usersRepo.update(listOf(unapproved.id to NewUser(unapproved.username, null))),
        )
        assertEquals(EmailProfile(userId = unapproved.id.long), usersRepo.getEmailProfileFresh(unapproved.id))
        assertEquals(
            listOf(RegisteredUser(approved.id, approved.username)),
            usersRepo.update(listOf(approved.id to NewUser(approved.username, null))),
        )
        assertEquals(EmailProfile(userId = approved.id.long), usersRepo.getEmailProfileFresh(approved.id))
    }

    /** Runs an admin feature test against a fixture-owned Exposed SQLite repository. */
    private suspend fun withSqliteUsersRepo(
        nowMillis: () -> Long,
        block: suspend (ExposedUsersRepo) -> Unit,
    ) {
        val file = Files.createTempFile("wishlist-admin-users", ".sqlite")
        val database = Database.connect(url = "jdbc:sqlite:${file.toAbsolutePath()}", driver = "org.sqlite.JDBC")
        try {
            block(ExposedUsersRepo(database, nowMillis))
        } finally {
            try {
                TransactionManager.closeAndUnregister(database)
            } finally {
                Files.deleteIfExists(file)
            }
        }
    }

    /** [UsersManagementFeature.getAll] maps every stored user to [AdminUser], keeping email. */
    @Test
    fun getAllMapsEveryStoredUserToAdminUserWithEmailPreserved() = runTest {
        val usersRepo = FakeUsersRepo(mapOf(userWithEmail.id to userWithEmail, userWithoutEmail.id to userWithoutEmail))
        val feature = buildFeature(usersRepo)

        val result = feature.getAll()

        assertEquals(
            setOf(userWithEmail.asAdminUser(), userWithoutEmail.asAdminUser()),
            result.toSet()
        )
    }

    /** [UsersManagementFeature.create] returns the persisted user as an [AdminUser]. */
    @Test
    fun createReturnsPersistedUserAsAdminUser() = runTest {
        val usersRepo = FakeUsersRepo()
        val feature = buildFeature(usersRepo)

        val created = feature.create(NewUserWithPassword(Username("carol"), Password("s3cret-pw")))

        checkNotNull(created)
        assertEquals(Username("carol"), created.username)
        assertEquals(null, created.email)
    }

    /** Username-only admin changes keep the stored email and its approval state intact. */
    @Test
    fun updateUsernamePreservesEmailAndApproval() = runTest {
        val approvedUser = userWithEmail.copy(emailApproved = true)
        val usersRepo = FakeUsersRepo(mapOf(approvedUser.id to approvedUser))
        val feature = buildFeature(usersRepo)

        assertEquals(true, feature.updateUsername(approvedUser.id, Username("alice-renamed")))

        assertEquals(
            approvedUser.copy(username = Username("alice-renamed")),
            usersRepo.getById(approvedUser.id),
        )
    }

    /** Full updates share the real lifecycle gate, while the dedicated rename remains independently allowed. */
    @Test
    fun realRepositoryRejectsFullUpdateWithoutPartialRename() = runTest {
        var now = 1_000L
        withSqliteUsersRepo(nowMillis = { now }) { usersRepo ->
            val addressA = Email("admin-real-a@example.com")
            val addressB = Email("admin-real-b@example.com")
            val created = usersRepo.create(listOf(NewUser(Username("admin-real"), addressA))).single()
            checkNotNull(usersRepo.approveEmail(created.id, addressA, cooldownMillis = 10L))
            val feature = buildFeature(usersRepo, cooldownMillis = 10L)
            val approved = checkNotNull(usersRepo.getById(created.id))

            val rejection = assertFailsWith<EmailChangeCooldownException> {
                feature.update(created.id, NewUser(Username("must-not-rename"), addressB))
            }
            assertEquals(1_010L, rejection.emailChangeAllowedAt)
            assertEquals(approved, usersRepo.getById(created.id))
            assertFailsWith<EmailChangeCooldownException> {
                feature.update(created.id, NewUser(Username("must-not-clear"), null))
            }
            assertEquals(approved, usersRepo.getById(created.id))

            assertEquals(true, feature.updateUsername(created.id, Username("admin-real-renamed")))
            assertEquals(approved.copy(username = Username("admin-real-renamed")), usersRepo.getById(created.id))

            now = 1_010L
            assertEquals(true, feature.update(created.id, NewUser(Username("admin-real-expired"), addressB)))
            assertEquals(
                approved.copy(username = Username("admin-real-expired")),
                usersRepo.getById(created.id),
            )
            assertEquals(addressB, usersRepo.getEmailProfileFresh(created.id)?.pendingEmail)
        }
    }
}
