package dev.inmo.wishlist.features.admin.server

import dev.inmo.micro_utils.repos.KeyValueRepo
import dev.inmo.micro_utils.repos.MapCRUDRepo
import dev.inmo.micro_utils.repos.MapKeyValueRepo
import dev.inmo.wishlist.features.admin.common.models.AdminUser
import dev.inmo.wishlist.features.admin.common.models.NewUserWithPassword
import dev.inmo.wishlist.features.admin.common.models.asAdminUser
import dev.inmo.wishlist.features.auth.common.models.Password
import dev.inmo.wishlist.features.auth.server.repo.PasswordsRepo
import dev.inmo.wishlist.features.auth.server.services.AuthFeatureService
import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.users.common.models.NewUser
import dev.inmo.wishlist.features.users.common.models.RegisteredUser
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.users.common.models.Username
import dev.inmo.wishlist.features.users.common.repo.UsersRepo
import dev.inmo.wishlist.features.wishlist.common.models.NewWishlist
import dev.inmo.wishlist.features.wishlist.common.models.NewWishlistItem
import dev.inmo.wishlist.features.wishlist.common.models.RegisteredWishlist
import dev.inmo.wishlist.features.wishlist.common.models.RegisteredWishlistItem
import dev.inmo.wishlist.features.wishlist.common.models.WishlistId
import dev.inmo.wishlist.features.wishlist.common.models.WishlistItemId
import dev.inmo.wishlist.features.wishlist.common.repo.WishlistItemRepo
import dev.inmo.wishlist.features.wishlist.common.repo.WishlistRepo
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/** In-memory [UsersRepo] test double, seeded via the constructor map. */
internal class FakeUsersRepo(
    initialUsers: Map<UserId, RegisteredUser> = emptyMap()
) : UsersRepo, MapCRUDRepo<RegisteredUser, UserId, NewUser>(initialUsers.toMutableMap()) {

    private var nextId: Long = (initialUsers.keys.maxOfOrNull { it.long } ?: 0L) + 1L

    override suspend fun updateObject(newValue: NewUser, id: UserId, old: RegisteredUser): RegisteredUser =
        old.copy(username = newValue.username, email = newValue.email)

    override suspend fun createObject(newValue: NewUser): Pair<UserId, RegisteredUser> {
        val id = UserId(nextId++)
        return id to RegisteredUser(id, newValue.username, newValue.email)
    }

    override suspend fun getUserByUsername(username: Username): RegisteredUser? =
        getAll().values.firstOrNull { it.username == username }
}

/** In-memory [PasswordsRepo] test double delegating entirely to [MapKeyValueRepo]. */
internal class FakePasswordsRepo : PasswordsRepo, KeyValueRepo<UserId, Password> by MapKeyValueRepo()

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

    private fun buildFeature(usersRepo: FakeUsersRepo): UsersManagementFeature {
        val authService = AuthFeatureService(usersRepo, usersRepo, FakePasswordsRepo())
        return UsersManagementFeature(usersRepo, authService, FakeWishlistRepo(), FakeWishlistItemRepo())
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
}
