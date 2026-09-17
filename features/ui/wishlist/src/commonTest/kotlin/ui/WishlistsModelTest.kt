package dev.inmo.wishlist.features.ui.wishlist.ui

import dev.inmo.wishlist.features.auth.client.AuthCredentialsStorage
import dev.inmo.wishlist.features.auth.client.meQualifier
import dev.inmo.wishlist.features.auth.common.models.AuthCredentials
import dev.inmo.wishlist.features.auth.common.models.AuthFeatureUser
import dev.inmo.wishlist.features.currency.client.CurrencyService
import dev.inmo.wishlist.features.currency.common.CurrencyFeature
import dev.inmo.wishlist.features.currency.common.models.CurrencyCode
import dev.inmo.wishlist.features.currency.common.models.CurrencyInfo
import dev.inmo.wishlist.features.currency.common.models.CurrencyRates
import dev.inmo.wishlist.features.files.client.FilesClientService
import dev.inmo.wishlist.features.files.client.FilesFeature
import dev.inmo.wishlist.features.files.common.models.FileId
import dev.inmo.wishlist.features.files.common.models.FilesFeatureMetaInfo
import dev.inmo.wishlist.features.files.common.models.FinalizeFileRequest
import dev.inmo.wishlist.features.ui.wishlist.Plugin
import dev.inmo.wishlist.features.users.client.UsersFeature
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.users.common.models.Username
import dev.inmo.wishlist.features.users.common.models.UsersFeatureUser
import dev.inmo.wishlist.features.wishlist.client.WishlistCopyFeature
import dev.inmo.wishlist.features.wishlist.client.WishlistsFeature
import dev.inmo.wishlist.features.wishlist.client.WishlistsItemsFeature
import dev.inmo.wishlist.features.wishlist.common.models.CopyItemRequest
import dev.inmo.wishlist.features.wishlist.common.models.CopyWishlistRequest
import dev.inmo.wishlist.features.wishlist.common.models.NewWishlistInFeature
import dev.inmo.wishlist.features.wishlist.common.models.NewWishlistItem
import dev.inmo.wishlist.features.wishlist.common.models.Priority
import dev.inmo.wishlist.features.wishlist.common.models.WishlistId
import dev.inmo.wishlist.features.wishlist.common.models.WishlistItemId
import dev.inmo.wishlist.features.wishlist.common.models.WishlistsFeatureItem
import dev.inmo.wishlist.features.wishlist.common.models.WishlistsFeatureWishlist
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/** Verifies wishlist behavior, exact request mappings, interface defaults, and DI lifetime. */
class WishlistsModelTest {
    internal companion object {
        /** Builds the production model around a JVM-test file service. */
        fun modelForFileTests(
            filesService: FilesClientService,
            scope: CoroutineScope,
        ): DefaultWishlistsModel = DefaultWishlistsModel(
            wishlistsFeature = RecordingWishlistsFeature(),
            itemsFeature = RecordingItemsFeature(),
            copyFeature = RecordingCopyFeature(),
            meState = MutableStateFlow(null),
            filesService = filesService,
            usersFeature = RecordingUsersFeature(),
            currencyService = CurrencyService(RecordingCurrencyFeature()),
            viewModeStorage = RecordingViewModeStorage(),
            scope = scope,
            credentialsStorage = TestCredentialsStorage(),
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun pluginModelPreservesEveryPlatformNeutralOperation() = runTest {
        val ownerId = UserId(5L)
        val meState = MutableStateFlow<AuthFeatureUser?>(null)
        val wishlists = RecordingWishlistsFeature()
        val items = RecordingItemsFeature()
        val copy = RecordingCopyFeature()
        val users = RecordingUsersFeature()
        val currencyFeature = RecordingCurrencyFeature()
        val currencyService = CurrencyService(currencyFeature)
        val viewModeStorage = RecordingViewModeStorage()
        val filesFeature = RecordingFilesFeature()
        val client = HttpClient()
        val filesService = FilesClientService(client, filesFeature)
        val credentialsStorage = TestCredentialsStorage()
        val koin = startKoin {
            modules(
                module {
                    with(Plugin) { setupDI(JsonObject(emptyMap())) }
                    single<WishlistsFeature> { wishlists }
                    single<WishlistsItemsFeature> { items }
                    single<WishlistCopyFeature> { copy }
                    single<StateFlow<AuthFeatureUser?>>(meQualifier) { meState }
                    single<FilesClientService> { filesService }
                    single<UsersFeature> { users }
                    single<CurrencyService> { currencyService }
                    single<WishlistViewModeStorage> { viewModeStorage }
                    single<CoroutineScope> { backgroundScope }
                    single<AuthCredentialsStorage> { credentialsStorage }
                }
            )
        }
        try {
            val model = koin.koin.get<WishlistsModel>()
            assertIs<DefaultWishlistsModel>(model)
            assertSame(model, koin.koin.get<WishlistsModel>())
            assertSame(credentialsStorage.userAuthorised, model.userAuthorisedState)

            runCurrent()
            assertNull(model.currentUserIdFlow.value)
            meState.value = AuthFeatureUser(ownerId, Username("owner"), email = null)
            runCurrent()
            assertEquals(ownerId, model.currentUserIdFlow.value)

            assertSame(wishlists.mine, model.getMyWishlists())
            assertSame(wishlists.byUser, model.getUserWishlists(ownerId))
            assertEquals(wishlists.wishlist, model.getWishlist(wishlists.wishlist.id))
            assertEquals(listOf(ownerId), wishlists.byUserCalls)
            assertEquals(listOf(wishlists.wishlist.id), wishlists.byIdCalls)

            assertSame(items.values, model.getWishlistItems(wishlists.wishlist.id))
            assertEquals(listOf(wishlists.wishlist.id), items.byWishlistCalls)

            assertEquals(wishlists.wishlist, model.createWishlist("created", "EUR"))
            assertEquals(listOf(NewWishlistInFeature("created", "EUR")), wishlists.createCalls)
            assertTrue(model.updateWishlist(wishlists.wishlist.id, "updated", "JPY"))
            assertEquals(
                listOf(wishlists.wishlist.id to NewWishlistInFeature("updated", "JPY")),
                wishlists.updateCalls,
            )
            assertTrue(model.deleteWishlist(wishlists.wishlist.id))
            assertEquals(listOf(wishlists.wishlist.id), wishlists.deleteCalls)

            val newItem = NewWishlistItem(wishlists.wishlist.id, "new item")
            assertEquals(items.item, model.createWishlistItem(newItem))
            assertTrue(model.updateWishlistItem(items.item.id, newItem))
            assertTrue(model.deleteWishlistItem(items.item.id))
            assertEquals(listOf(newItem), items.createCalls)
            assertEquals(listOf(items.item.id to newItem), items.updateCalls)
            assertEquals(listOf(items.item.id), items.deleteCalls)

            val targetWishlistId = WishlistId(99L)
            assertEquals(
                items.item,
                model.copyItemToWishlist(items.item.id, wishlists.wishlist.id, targetWishlistId),
            )
            assertEquals(
                listOf(CopyItemRequest(items.item.id, wishlists.wishlist.id, targetWishlistId)),
                items.copyCalls,
            )
            assertTrue(model.enqueueWishlistCopy(wishlists.wishlist.id))
            assertEquals(listOf(CopyWishlistRequest(wishlists.wishlist.id)), copy.calls)

            assertEquals("first", model.getUserName(ownerId))
            assertNull(model.getUserName(UserId(404L)))
            assertEquals(2, users.calls)
            assertEquals("/api/files/image-id", model.imageUrl(FileId("image-id")))

            assertSame(currencyService.selectedCurrency, model.selectedCurrency)
            assertTrue(model.isCurrencyEnabled())
            assertSame(currencyFeature.currencies, model.availableCurrencies())
            assertEquals(currencyFeature.rates, model.currencyRates())
            assertEquals(1, currencyFeature.enabledCalls)
            assertEquals(1, currencyFeature.currencyCalls)
            assertEquals(1, currencyFeature.rateCalls)
            model.selectCurrency(currencyFeature.eur)
            assertEquals(currencyFeature.eur, model.selectedCurrency.value)
            model.selectCurrency(null)
            assertNull(model.selectedCurrency.value)

            assertEquals(WishlistViewMode.Grid, model.getSavedViewMode())
            viewModeStorage.value = WishlistViewMode.List
            assertEquals(WishlistViewMode.List, model.getSavedViewMode())
            model.saveViewMode(WishlistViewMode.List)
            assertEquals(listOf(WishlistViewMode.List), viewModeStorage.saved)

            assertFalse(model.isOwner(ownerId, null))
            assertTrue(model.isOwner(null, ownerId))
            assertTrue(model.isOwner(ownerId, ownerId))
            assertFalse(model.isOwner(UserId(6L), ownerId))
            assertTrue(model.isOwnerFlow(ownerId).first())
            meState.value = null
            runCurrent()
            assertFalse(model.isOwnerFlow(ownerId).first())
        } finally {
            stopKoin()
            client.close()
        }
    }

    private class RecordingWishlistsFeature : WishlistsFeature {
        val wishlist = WishlistsFeatureWishlist(WishlistId(10L), UserId(5L), "list", "USD")
        val mine = listOf(wishlist)
        val byUser = listOf(wishlist.copy(title = "public"))
        val byIdCalls = mutableListOf<WishlistId>()
        val byUserCalls = mutableListOf<UserId>()
        val createCalls = mutableListOf<NewWishlistInFeature>()
        val updateCalls = mutableListOf<Pair<WishlistId, NewWishlistInFeature>>()
        val deleteCalls = mutableListOf<WishlistId>()

        override suspend fun getById(id: WishlistId): WishlistsFeatureWishlist {
            byIdCalls += id
            return wishlist
        }
        override suspend fun getByUserId(userId: UserId): List<WishlistsFeatureWishlist> {
            byUserCalls += userId
            return byUser
        }
        override suspend fun getMyWishlists(): List<WishlistsFeatureWishlist> = mine
        override suspend fun create(newWishlist: NewWishlistInFeature): WishlistsFeatureWishlist {
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

    private class RecordingItemsFeature : WishlistsItemsFeature {
        val item = WishlistsFeatureItem(
            id = WishlistItemId(20L),
            wishlistId = WishlistId(10L),
            title = "item",
            amount = 2u,
            approximatePrice = null,
            priceUnits = "USD",
            links = emptyList(),
            description = "description",
            priority = Priority.High,
            imageIds = emptyList(),
        )
        val values = listOf(item)
        val byWishlistCalls = mutableListOf<WishlistId>()
        val createCalls = mutableListOf<NewWishlistItem>()
        val updateCalls = mutableListOf<Pair<WishlistItemId, NewWishlistItem>>()
        val deleteCalls = mutableListOf<WishlistItemId>()
        val copyCalls = mutableListOf<CopyItemRequest>()

        override suspend fun getByWishlistId(wishlistId: WishlistId): List<WishlistsFeatureItem> {
            byWishlistCalls += wishlistId
            return values
        }
        override suspend fun create(newWishlistItem: NewWishlistItem): WishlistsFeatureItem {
            createCalls += newWishlistItem
            return item
        }
        override suspend fun copy(request: CopyItemRequest): WishlistsFeatureItem {
            copyCalls += request
            return item
        }
        override suspend fun update(id: WishlistItemId, newWishlistItem: NewWishlistItem): Boolean {
            updateCalls += id to newWishlistItem
            return true
        }
        override suspend fun delete(id: WishlistItemId): Boolean {
            deleteCalls += id
            return true
        }
    }

    private class RecordingCopyFeature : WishlistCopyFeature {
        val calls = mutableListOf<CopyWishlistRequest>()
        override suspend fun enqueueCopy(request: CopyWishlistRequest): Boolean {
            calls += request
            return true
        }
    }

    private class RecordingUsersFeature : UsersFeature {
        val values = listOf(
            UsersFeatureUser(UserId(5L), Username("first")),
            UsersFeatureUser(UserId(5L), Username("second")),
        )
        var calls = 0
        override suspend fun getAll(): List<UsersFeatureUser> {
            calls += 1
            return values
        }
    }

    private class RecordingCurrencyFeature : CurrencyFeature {
        val eur = CurrencyCode("EUR")
        val currencies = listOf(CurrencyInfo(eur, "Euro"))
        val rates = CurrencyRates(CurrencyCode("USD"), mapOf("EUR" to 0.9), 123L)
        var enabledCalls = 0
        var currencyCalls = 0
        var rateCalls = 0
        override suspend fun isFeatureEnabled(): Boolean {
            enabledCalls += 1
            return true
        }
        override suspend fun getCurrencies(): List<CurrencyInfo> {
            currencyCalls += 1
            return currencies
        }
        override suspend fun getRates(): CurrencyRates {
            rateCalls += 1
            return rates
        }
    }

    private class RecordingViewModeStorage : WishlistViewModeStorage {
        var value: WishlistViewMode? = null
        val saved = mutableListOf<WishlistViewMode>()
        override suspend fun getViewMode(): WishlistViewMode? = value
        override suspend fun saveViewMode(mode: WishlistViewMode) {
            saved += mode
        }
    }

    private class RecordingFilesFeature : FilesFeature {
        override suspend fun finalize(request: FinalizeFileRequest): FilesFeatureMetaInfo? = error("unused")
        override suspend fun getMeta(id: FileId): FilesFeatureMetaInfo? = error("unused")
        override suspend fun getAvatar(userId: UserId): FileId? = error("unused")
        override suspend fun setAvatar(userId: UserId, fileId: FileId): Boolean = error("unused")
    }

    private class TestCredentialsStorage : AuthCredentialsStorage {
        override val userAuthorised = MutableStateFlow(true)
        override suspend fun get(): AuthCredentials? = null
        override suspend fun save(credentials: AuthCredentials?) = Unit
    }
}
