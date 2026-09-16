package dev.inmo.wishlist.features.ui.adminPanel

import dev.inmo.wishlist.features.admin.client.AdminFeature
import dev.inmo.wishlist.features.admin.client.AdminWishlistItemsFeature
import dev.inmo.wishlist.features.admin.client.AdminWishlistsFeature
import dev.inmo.wishlist.features.admin.client.UsersManagementFeature
import dev.inmo.wishlist.features.admin.common.models.AdminUser
import dev.inmo.wishlist.features.admin.common.models.AdminWishlist
import dev.inmo.wishlist.features.admin.common.models.AdminWishlistItem
import dev.inmo.wishlist.features.admin.common.models.NewUserWithPassword
import dev.inmo.wishlist.features.auth.client.AuthCredentialsStorage
import dev.inmo.wishlist.features.auth.common.models.AuthCredentials
import dev.inmo.wishlist.features.auth.common.models.Password
import dev.inmo.wishlist.features.email.client.EmailFeature
import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.email.common.models.EmailVerificationRequestResult
import dev.inmo.wishlist.features.ui.adminPanel.ui.AdminPanelModel
import dev.inmo.wishlist.features.ui.adminPanel.ui.DefaultAdminPanelModel
import dev.inmo.wishlist.features.users.common.models.NewUser
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.users.common.models.Username
import dev.inmo.wishlist.features.wishlist.common.models.NewWishlist
import dev.inmo.wishlist.features.wishlist.common.models.NewWishlistInFeature
import dev.inmo.wishlist.features.wishlist.common.models.NewWishlistItem
import dev.inmo.wishlist.features.wishlist.common.models.Priority
import dev.inmo.wishlist.features.wishlist.common.models.WishlistId
import dev.inmo.wishlist.features.wishlist.common.models.WishlistItemId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

/** Verifies every admin-panel delegation and the production singleton binding. */
class AdminPanelModelTest {
    @Test
    fun pluginModelDelegatesEveryOperationWithoutChangingArguments() = runTest {
        val users = RecordingUsersManagementFeature()
        val wishlists = RecordingWishlistsFeature()
        val items = RecordingWishlistItemsFeature()
        val email = RecordingEmailFeature()
        val storage = TestCredentialsStorage()
        val koin = startModelKoin(users, wishlists, items, email, storage)
        try {
            val model = koin.koin.get<AdminPanelModel>()
            assertIs<DefaultAdminPanelModel>(model)
            assertSame(model, koin.koin.get<AdminPanelModel>())
            assertSame(storage.userAuthorised, model.userAuthorisedState)

            val userId = UserId(9L)
            val ignoredOwnerId = UserId(91L)
            val username = Username("renamed")
            val password = Password("secret")
            val newUserWithPassword = NewUserWithPassword(username, password)
            val newUser = NewUser(username, Email("renamed@example.com"))

            assertSame(users.users, model.getAllUsers())
            assertEquals(users.user, model.getUserById(userId))
            assertEquals(users.user, model.createUser(newUserWithPassword))
            assertTrue(model.updateUser(userId, newUser))
            assertTrue(model.updateUsername(userId, username))
            assertTrue(model.deleteUser(userId))
            assertEquals(listOf(userId), users.byIdCalls)
            assertEquals(listOf(newUserWithPassword), users.createCalls)
            assertEquals(listOf(userId to newUser), users.updateCalls)
            assertEquals(listOf(userId to username), users.usernameCalls)
            assertEquals(listOf(userId), users.deleteCalls)

            val wishlistId = WishlistId(12L)
            val newWishlist = NewWishlist(userId, "created", "EUR")
            assertSame(wishlists.wishlists, model.getAllWishlists())
            assertSame(wishlists.wishlists, model.getWishlistsByUser(userId))
            assertEquals(wishlists.wishlist, model.getWishlistById(wishlistId))
            assertEquals(wishlists.wishlist, model.createWishlist(newWishlist))
            assertTrue(model.updateWishlist(wishlistId, ignoredOwnerId, "updated"))
            assertTrue(model.deleteWishlist(wishlistId))
            assertEquals(listOf(userId), wishlists.byUserCalls)
            assertEquals(listOf(wishlistId), wishlists.byIdCalls)
            assertEquals(listOf(newWishlist), wishlists.createCalls)
            assertEquals(listOf(wishlistId to NewWishlistInFeature("updated")), wishlists.updateCalls)
            assertEquals(listOf(wishlistId), wishlists.deleteCalls)

            val itemId = WishlistItemId(20L)
            val newItem = NewWishlistItem(wishlistId, "gift")
            assertSame(items.items, model.getItemsByWishlist(wishlistId))
            assertEquals(items.item, model.createWishlistItem(newItem))
            assertTrue(model.updateWishlistItem(itemId, newItem))
            assertTrue(model.deleteWishlistItem(itemId))
            assertEquals(listOf(wishlistId), items.byWishlistCalls)
            assertEquals(listOf(newItem), items.createCalls)
            assertEquals(listOf(itemId to newItem), items.updateCalls)
            assertEquals(listOf(itemId), items.deleteCalls)

            val recipient = Email("smtp-recipient@example.com")
            assertTrue(model.isEmailFeatureEnabled())
            assertTrue(model.sendTestEmail(recipient))
            assertEquals(1, email.enabledCalls)
            assertEquals(listOf(recipient), email.sendCalls)
        } finally {
            stopKoin()
        }
    }

    private fun startModelKoin(
        users: RecordingUsersManagementFeature,
        wishlists: RecordingWishlistsFeature,
        items: RecordingWishlistItemsFeature,
        email: RecordingEmailFeature,
        storage: TestCredentialsStorage,
    ) = startKoin {
        modules(
            module {
                with(Plugin) { setupDI(JsonObject(emptyMap())) }
                single<AdminFeature> {
                    object : AdminFeature {
                        override val usersManagement: UsersManagementFeature = users
                        override val wishlists: AdminWishlistsFeature = wishlists
                        override val wishlistItems: AdminWishlistItemsFeature = items
                    }
                }
                single<EmailFeature> { email }
                single<AuthCredentialsStorage> { storage }
            }
        )
    }

    private class RecordingUsersManagementFeature : UsersManagementFeature {
        val user = AdminUser(UserId(9L), Username("alice"), Email("alice@example.com"), emailApproved = true)
        val users = listOf(user)
        val byIdCalls = mutableListOf<UserId>()
        val createCalls = mutableListOf<NewUserWithPassword>()
        val updateCalls = mutableListOf<Pair<UserId, NewUser>>()
        val usernameCalls = mutableListOf<Pair<UserId, Username>>()
        val deleteCalls = mutableListOf<UserId>()

        override suspend fun getAll(): List<AdminUser> = users
        override suspend fun getById(id: UserId): AdminUser {
            byIdCalls += id
            return user
        }
        override suspend fun create(newUser: NewUserWithPassword): AdminUser {
            createCalls += newUser
            return user
        }
        override suspend fun update(id: UserId, newUser: NewUser): Boolean {
            updateCalls += id to newUser
            return true
        }
        override suspend fun updateUsername(id: UserId, username: Username): Boolean {
            usernameCalls += id to username
            return true
        }
        override suspend fun setPassword(id: UserId, password: Password): Boolean = error("unused")
        override suspend fun delete(id: UserId): Boolean {
            deleteCalls += id
            return true
        }
    }

    private class RecordingWishlistsFeature : AdminWishlistsFeature {
        val wishlist = AdminWishlist(WishlistId(12L), UserId(9L), "list", "USD")
        val wishlists = listOf(wishlist)
        val byUserCalls = mutableListOf<UserId>()
        val byIdCalls = mutableListOf<WishlistId>()
        val createCalls = mutableListOf<NewWishlist>()
        val updateCalls = mutableListOf<Pair<WishlistId, NewWishlistInFeature>>()
        val deleteCalls = mutableListOf<WishlistId>()

        override suspend fun getAll(): List<AdminWishlist> = wishlists
        override suspend fun getByUserId(userId: UserId): List<AdminWishlist> {
            byUserCalls += userId
            return wishlists
        }
        override suspend fun getById(id: WishlistId): AdminWishlist {
            byIdCalls += id
            return wishlist
        }
        override suspend fun create(newWishlist: NewWishlist): AdminWishlist {
            createCalls += newWishlist
            return wishlist
        }
        override suspend fun update(id: WishlistId, newWishlist: NewWishlistInFeature): Boolean {
            updateCalls += id to newWishlist
            return true
        }
        override suspend fun delete(id: WishlistId): Boolean {
            deleteCalls += id
            return true
        }
    }

    private class RecordingWishlistItemsFeature : AdminWishlistItemsFeature {
        val item = AdminWishlistItem(
            id = WishlistItemId(20L),
            wishlistId = WishlistId(12L),
            title = "gift",
            amount = 1u,
            approximatePrice = null,
            priceUnits = "USD",
            links = emptyList(),
            description = "",
            priority = Priority.Medium,
            imageIds = emptyList(),
        )
        val items = listOf(item)
        val byWishlistCalls = mutableListOf<WishlistId>()
        val createCalls = mutableListOf<NewWishlistItem>()
        val updateCalls = mutableListOf<Pair<WishlistItemId, NewWishlistItem>>()
        val deleteCalls = mutableListOf<WishlistItemId>()

        override suspend fun getByWishlistId(wishlistId: WishlistId): List<AdminWishlistItem> {
            byWishlistCalls += wishlistId
            return items
        }
        override suspend fun create(item: NewWishlistItem): AdminWishlistItem {
            createCalls += item
            return this.item
        }
        override suspend fun update(id: WishlistItemId, item: NewWishlistItem): Boolean {
            updateCalls += id to item
            return true
        }
        override suspend fun delete(id: WishlistItemId): Boolean {
            deleteCalls += id
            return true
        }
    }

    private class RecordingEmailFeature : EmailFeature {
        var enabledCalls = 0
        val sendCalls = mutableListOf<Email>()
        override suspend fun isFeatureEnabled(): Boolean {
            enabledCalls += 1
            return true
        }
        override suspend fun sendTestEmail(recipient: Email): Boolean {
            sendCalls += recipient
            return true
        }
        override suspend fun setMyEmail(email: Email?): Boolean = error("unused")
        override suspend fun requestMyEmailVerification(expectedEmail: Email): EmailVerificationRequestResult = error("unused")
    }

    private class TestCredentialsStorage : AuthCredentialsStorage {
        override val userAuthorised = MutableStateFlow(true)
        override suspend fun get(): AuthCredentials? = null
        override suspend fun save(credentials: AuthCredentials?) = Unit
    }
}
