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
import dev.inmo.wishlist.features.auth.common.models.Password
import dev.inmo.wishlist.features.email.client.EmailFeature
import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.email.common.models.EmailVerificationRequestResult
import dev.inmo.wishlist.features.ui.adminPanel.ui.AdminPanelModel
import dev.inmo.wishlist.features.users.common.models.NewUser
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.users.common.models.Username
import dev.inmo.wishlist.features.wishlist.common.models.NewWishlist
import dev.inmo.wishlist.features.wishlist.common.models.NewWishlistInFeature
import dev.inmo.wishlist.features.wishlist.common.models.NewWishlistItem
import dev.inmo.wishlist.features.wishlist.common.models.WishlistId
import dev.inmo.wishlist.features.wishlist.common.models.WishlistItemId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import org.koin.core.KoinApplication
import org.koin.core.context.stopKoin
import org.koin.core.context.startKoin
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Resolves the production admin-panel model and checks its owning client-feature delegation. */
class AdminPanelModelTest {
    @Test
    fun pluginModelDelegatesUsersAndEmailOperationsWithoutChangingArguments() = runTest {
        val users = RecordingUsersManagementFeature()
        val email = RecordingEmailFeature()
        val koin = startModelKoin(users, email)
        try {
            val model = koin.koin.get<AdminPanelModel>()
            val id = UserId(9L)
            val name = Username("renamed")
            val recipient = Email("smtp-recipient@example.com")

            assertEquals(users.users, model.getAllUsers())
            assertTrue(model.updateUsername(id, name))
            assertTrue(model.isEmailFeatureEnabled())
            assertTrue(model.sendTestEmail(recipient))
            assertEquals(listOf(id to name), users.usernameCalls)
            assertEquals(listOf(recipient), email.sendCalls)
        } finally {
            stopKoin()
        }
    }

    private fun startModelKoin(
        users: RecordingUsersManagementFeature,
        email: RecordingEmailFeature,
    ): KoinApplication = startKoin {
        modules(
            module {
                with(Plugin) { setupDI(JsonObject(emptyMap())) }
                single<AdminFeature> {
                    object : AdminFeature {
                        override val usersManagement: UsersManagementFeature = users
                        override val wishlists: AdminWishlistsFeature = UnusedWishlistsFeature
                        override val wishlistItems: AdminWishlistItemsFeature = UnusedWishlistItemsFeature
                    }
                }
                single<EmailFeature> { email }
                single<AuthCredentialsStorage> { TestCredentialsStorage }
            }
        )
    }

    private class RecordingUsersManagementFeature : UsersManagementFeature {
        val users = listOf(AdminUser(UserId(9L), Username("alice"), Email("alice@example.com"), emailApproved = true))
        val usernameCalls = mutableListOf<Pair<UserId, Username>>()

        override suspend fun getAll(): List<AdminUser> = users
        override suspend fun getById(id: UserId): AdminUser? = users.singleOrNull { it.id == id }
        override suspend fun create(newUser: NewUserWithPassword): AdminUser? = error("unused")
        override suspend fun update(id: UserId, newUser: NewUser): Boolean = error("unused")
        override suspend fun updateUsername(id: UserId, username: Username): Boolean {
            usernameCalls += id to username
            return true
        }
        override suspend fun setPassword(id: UserId, password: Password): Boolean = error("unused")
        override suspend fun delete(id: UserId): Boolean = error("unused")
    }

    private object TestCredentialsStorage : AuthCredentialsStorage {
        override val userAuthorised = MutableStateFlow(true)
        override suspend fun get() = null
        override suspend fun save(credentials: dev.inmo.wishlist.features.auth.common.models.AuthCredentials?) = Unit
    }

    private class RecordingEmailFeature : EmailFeature {
        val sendCalls = mutableListOf<Email>()
        override suspend fun isFeatureEnabled(): Boolean = true
        override suspend fun sendTestEmail(recipient: Email): Boolean {
            sendCalls += recipient
            return true
        }
        override suspend fun setMyEmail(email: Email?): Boolean = error("unused")
        override suspend fun requestMyEmailVerification(expectedEmail: Email): EmailVerificationRequestResult = error("unused")
    }

    private object UnusedWishlistsFeature : AdminWishlistsFeature {
        override suspend fun getAll(): List<AdminWishlist> = error("unused")
        override suspend fun getByUserId(userId: UserId): List<AdminWishlist> = error("unused")
        override suspend fun getById(id: WishlistId): AdminWishlist? = error("unused")
        override suspend fun create(newWishlist: NewWishlist): AdminWishlist? = error("unused")
        override suspend fun update(id: WishlistId, newWishlist: NewWishlistInFeature): Boolean = error("unused")
        override suspend fun delete(id: WishlistId): Boolean = error("unused")
    }

    private object UnusedWishlistItemsFeature : AdminWishlistItemsFeature {
        override suspend fun getByWishlistId(wishlistId: WishlistId): List<AdminWishlistItem> = error("unused")
        override suspend fun create(item: NewWishlistItem): AdminWishlistItem? = error("unused")
        override suspend fun update(id: WishlistItemId, item: NewWishlistItem): Boolean = error("unused")
        override suspend fun delete(id: WishlistItemId): Boolean = error("unused")
    }
}
