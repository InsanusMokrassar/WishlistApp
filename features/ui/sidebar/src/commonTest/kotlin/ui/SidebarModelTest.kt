package dev.inmo.wishlist.features.ui.sidebar.ui

import dev.inmo.micro_utils.common.MPPFile
import dev.inmo.wishlist.features.auth.common.models.AuthFeatureUser
import dev.inmo.wishlist.features.auth.common.models.Password
import dev.inmo.wishlist.features.booking.common.models.BookingFeatureItem
import dev.inmo.wishlist.features.booking.common.models.BookingState
import dev.inmo.wishlist.features.currency.common.models.CurrencyCode
import dev.inmo.wishlist.features.currency.common.models.CurrencyInfo
import dev.inmo.wishlist.features.currency.common.models.CurrencyRates
import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.email.common.models.EmailVerificationRequestResult
import dev.inmo.wishlist.features.files.common.models.FileId
import dev.inmo.wishlist.features.ui.booking.ui.BookingModel
import dev.inmo.wishlist.features.ui.sidebar.Plugin
import dev.inmo.wishlist.features.ui.users.ui.UsersModel
import dev.inmo.wishlist.features.ui.wishlist.ui.WishlistViewMode
import dev.inmo.wishlist.features.ui.wishlist.ui.WishlistsModel
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.users.common.models.Username
import dev.inmo.wishlist.features.users.common.models.UsersFeatureUser
import dev.inmo.wishlist.features.wishlist.common.models.NewWishlistItem
import dev.inmo.wishlist.features.wishlist.common.models.WishlistId
import dev.inmo.wishlist.features.wishlist.common.models.WishlistItemId
import dev.inmo.wishlist.features.wishlist.common.models.WishlistsFeatureItem
import dev.inmo.wishlist.features.wishlist.common.models.WishlistsFeatureWishlist
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.collections.AbstractList
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame

/** Verifies sidebar composition and the production singleton binding. */
class SidebarModelTest {
    @Test
    fun exposesDependencyFlowsAndDelegatesSidebarValues() = runTest {
        val wishlists = StubWishlistsModel()
        val booking = StubBookingModel()
        val users = StubUsersModel()
        val model = DefaultSidebarModel(
            wishlistsModel = wishlists,
            bookingModel = booking,
            usersModel = users,
        )

        assertSame(wishlists.currentUserIdFlow, model.currentUserIdFlow)
        assertSame(users.isCurrentUserRootFlow, model.isCurrentUserRootFlow)
        assertSame(wishlists.myWishlists, model.getMyWishlists())
        assertEquals(3, model.getReservedCount())
        assertEquals("owner", model.getUserName(UserId(4L)))
        assertEquals(listOf(UserId(4L)), wishlists.nameCalls)
        assertEquals(1, booking.presentsCalls)
    }

    @Test
    fun pluginBindsDefaultModelAsOneInterfaceSingleton() {
        val koin = startKoin {
            modules(
                module {
                    with(Plugin) { setupDI(JsonObject(emptyMap())) }
                    single<WishlistsModel> { StubWishlistsModel() }
                    single<BookingModel> { StubBookingModel() }
                    single<UsersModel> { StubUsersModel() }
                }
            )
        }
        try {
            val first = koin.koin.get<SidebarModel>()
            val second = koin.koin.get<SidebarModel>()

            assertIs<DefaultSidebarModel>(first)
            assertSame(first, second)
        } finally {
            stopKoin()
        }
    }

    private class StubWishlistsModel : WishlistsModel {
        override val userAuthorisedState = MutableStateFlow(true)
        override val currentUserIdFlow = MutableStateFlow<UserId?>(UserId(4L))
        override val selectedCurrency = MutableStateFlow<CurrencyCode?>(null)
        val myWishlists = listOf(WishlistsFeatureWishlist(WishlistId(8L), UserId(4L), "mine", "USD"))
        val nameCalls = mutableListOf<UserId>()

        override suspend fun getMyWishlists(): List<WishlistsFeatureWishlist> = myWishlists
        override suspend fun getUserName(userId: UserId): String {
            nameCalls += userId
            return "owner"
        }

        override suspend fun getUserWishlists(userId: UserId): List<WishlistsFeatureWishlist> = error("unused")
        override suspend fun getWishlist(id: WishlistId): WishlistsFeatureWishlist? = error("unused")
        override suspend fun getWishlistItems(wishlistId: WishlistId): List<WishlistsFeatureItem> = error("unused")
        override suspend fun createWishlist(title: String, defaultPriceUnits: String): WishlistsFeatureWishlist? = error("unused")
        override suspend fun updateWishlist(id: WishlistId, title: String, defaultPriceUnits: String): Boolean = error("unused")
        override suspend fun deleteWishlist(id: WishlistId): Boolean = error("unused")
        override suspend fun createWishlistItem(item: NewWishlistItem): WishlistsFeatureItem? = error("unused")
        override suspend fun updateWishlistItem(id: WishlistItemId, item: NewWishlistItem): Boolean = error("unused")
        override suspend fun deleteWishlistItem(id: WishlistItemId): Boolean = error("unused")
        override suspend fun copyItemToWishlist(sourceItemId: WishlistItemId, sourceWishlistId: WishlistId, targetWishlistId: WishlistId): WishlistsFeatureItem? = error("unused")
        override suspend fun enqueueWishlistCopy(sourceWishlistId: WishlistId): Boolean = error("unused")
        override suspend fun uploadImage(file: MPPFile): FileId? = error("unused")
        override fun imageUrl(id: FileId): String = error("unused")
        override suspend fun loadImageBytes(id: FileId): ByteArray? = error("unused")
        override suspend fun isCurrencyEnabled(): Boolean = error("unused")
        override suspend fun availableCurrencies(): List<CurrencyInfo> = error("unused")
        override suspend fun currencyRates(): CurrencyRates? = error("unused")
        override fun selectCurrency(code: CurrencyCode?) = error("unused")
        override suspend fun getSavedViewMode(): WishlistViewMode = error("unused")
        override suspend fun saveViewMode(mode: WishlistViewMode) = error("unused")
    }

    private class StubBookingModel : BookingModel {
        var presentsCalls = 0
        private val presents = object : AbstractList<BookingFeatureItem>() {
            override val size: Int = 3
            override fun get(index: Int): BookingFeatureItem = error("elements are not read")
        }

        override suspend fun myPresentsBooks(): List<BookingFeatureItem> {
            presentsCalls += 1
            return presents
        }

        override suspend fun getBookingState(itemId: WishlistItemId): BookingState? = error("unused")
        override suspend fun bookItem(itemId: WishlistItemId): Boolean = error("unused")
        override suspend fun cancelBooking(itemId: WishlistItemId): Boolean = error("unused")
    }

    private class StubUsersModel : UsersModel {
        override val userAuthorisedState = MutableStateFlow(true)
        override val currentUserIdFlow = MutableStateFlow<UserId?>(UserId(4L))
        override val isCurrentUserRootFlow = MutableStateFlow(true)
        override val canChangeAvatarForOthersFlow = MutableStateFlow(true)

        override suspend fun getAllUsers(): List<UsersFeatureUser> = error("unused")
        override suspend fun getUser(id: UserId): UsersFeatureUser? = error("unused")
        override suspend fun getMyProfile(): AuthFeatureUser? = error("unused")
        override suspend fun isEmailFeatureEnabled(): Boolean = error("unused")
        override suspend fun setMyEmail(email: Email?): Boolean = error("unused")
        override suspend fun requestMyEmailVerification(expectedEmail: Email): EmailVerificationRequestResult = error("unused")
        override suspend fun updateUsername(id: UserId, username: Username): Boolean = error("unused")
        override suspend fun setPassword(id: UserId, password: Password): Boolean = error("unused")
        override suspend fun deleteUser(id: UserId): Boolean = error("unused")
        override suspend fun getAvatar(userId: UserId): FileId? = error("unused")
        override suspend fun uploadAvatar(userId: UserId, file: MPPFile): FileId? = error("unused")
        override fun imageUrl(id: FileId): String = error("unused")
        override suspend fun loadImageBytes(id: FileId): ByteArray? = error("unused")
    }
}
