package dev.inmo.wishlist.features.ui.wishlist.ui

import dev.inmo.micro_utils.common.MPPFile
import dev.inmo.kslog.common.KSLog
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.core.NavigationNode
import dev.inmo.navigation.core.NavigationNodeFactory
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.currency.common.models.CurrencyCode
import dev.inmo.wishlist.features.currency.common.models.CurrencyInfo
import dev.inmo.wishlist.features.currency.common.models.CurrencyRates
import dev.inmo.wishlist.features.files.common.models.FileId
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.wishlist.common.models.NewWishlistItem
import dev.inmo.wishlist.features.wishlist.common.models.WishlistId
import dev.inmo.wishlist.features.wishlist.common.models.WishlistItemId
import dev.inmo.wishlist.features.wishlist.common.models.WishlistsFeatureItem
import dev.inmo.wishlist.features.wishlist.common.models.WishlistsFeatureWishlist
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Exercises auth-driven own-list loading through the real ViewModel flow and navigation node. */
@OptIn(ExperimentalCoroutinesApi::class)
class WishlistsListViewModelTest {
    /** Caller identity used by the protected-list test fixture. */
    private val caller = UserId(7L)

    /** A caller-owned list returned after authentication. */
    private val callerWishlist = WishlistsFeatureWishlist(WishlistId(10L), caller, "Private", "USD")

    /** Anonymous own-list startup does not call the bearer-protected read. */
    @Test
    fun anonymousOwnListSkipsProtectedLoadAndClearsCallerState() = runViewModelTest { model, viewModel ->
        advanceUntilIdle()
        assertEquals(0, model.myCalls)
        assertTrue(viewModel.wishlistsState.value.isEmpty())
        assertNull(viewModel.profileUserIdState.value)
        assertNull(viewModel.userNameState.value)
        assertFalse(viewModel.loadingState.value)
    }

    /** Login loads caller data and logout immediately removes it without another protected request. */
    @Test
    fun loginLoadsAndLogoutClearsOwnList() = runViewModelTest { model, viewModel ->
        model.myHandler = { listOf(callerWishlist) }
        model.currentUserId.value = caller
        model.authorised.value = true
        advanceUntilIdle()
        assertEquals(1, model.myCalls)
        assertEquals(listOf(callerWishlist), viewModel.wishlistsState.value)
        assertEquals(caller, viewModel.profileUserIdState.value)

        model.authorised.value = false
        runCurrent()
        assertEquals(1, model.myCalls)
        assertTrue(viewModel.wishlistsState.value.isEmpty())
        assertNull(viewModel.profileUserIdState.value)
        assertNull(viewModel.userNameState.value)
        assertFalse(viewModel.loadingState.value)
    }

    /** A late noncooperative authorized response cannot restore private rows after logout. */
    @Test
    fun logoutPreventsLateProtectedLoadPublication() = runTest {
        withSilentPlatformLogs {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val model = WishlistListTestModel().apply {
            authorised.value = true
            currentUserId.value = caller
            myHandler = {
                entered.complete(Unit)
                withContext(NonCancellable) { release.await() }
                listOf(callerWishlist)
            }
        }
        val viewModel = WishlistsListViewModel(wishlistsListTestNode(), model, RecordingWishlistsListInteractor(), StandardTestDispatcher(testScheduler))
        try {
            runCurrent()
            assertTrue(entered.isCompleted)
            model.authorised.value = false
            runCurrent()
            release.complete(Unit)
            advanceUntilIdle()
            assertTrue(viewModel.wishlistsState.value.isEmpty())
            assertNull(viewModel.profileUserIdState.value)
            assertFalse(viewModel.loadingState.value)
        } finally {
            release.complete(Unit)
            viewModel.scope.cancel()
            Dispatchers.resetMain()
        }
        }
    }

    /** Public user-id browsing stays readable without authorization and never calls the protected read. */
    @Test
    fun anonymousPublicUserListRemainsReadable() = runTest {
        withSilentPlatformLogs {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val publicOwner = UserId(9L)
        val publicWishlist = WishlistsFeatureWishlist(WishlistId(11L), publicOwner, "Public", "EUR")
        val model = WishlistListTestModel().apply { publicHandler = { listOf(publicWishlist) } }
        val viewModel = WishlistsListViewModel(wishlistsListTestNode(publicOwner), model, RecordingWishlistsListInteractor(), StandardTestDispatcher(testScheduler))
        try {
            advanceUntilIdle()
            assertEquals(0, model.myCalls)
            assertEquals(listOf(publicOwner), model.publicCalls)
            assertEquals(listOf(publicWishlist), viewModel.wishlistsState.value)
            assertEquals(publicOwner, viewModel.profileUserIdState.value)
        } finally {
            viewModel.scope.cancel()
            Dispatchers.resetMain()
        }
        }
    }

    /** Runs an own-list ViewModel with its scope pinned to the test scheduler. */
    private fun runViewModelTest(block: suspend TestScope.(WishlistListTestModel, WishlistsListViewModel) -> Unit) = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val model = WishlistListTestModel()
        val viewModel = WishlistsListViewModel(wishlistsListTestNode(), model, RecordingWishlistsListInteractor(), StandardTestDispatcher(testScheduler))
        try {
            withSilentPlatformLogs { block(model, viewModel) }
        } finally {
            viewModel.scope.cancel()
            Dispatchers.resetMain()
        }
    }
}

/** Suppresses platform logging from deliberate coroutine cancellation in common Android/JVM test runs. */
private suspend fun <T> withSilentPlatformLogs(block: suspend () -> T): T {
    val originalLogger = KSLog.default
    KSLog.default = KSLog { _, _, _, _ -> }
    return try {
        block()
    } finally {
        KSLog.default = originalLogger
    }
}

/** Configures the minimal navigation context required by the list ViewModel. */
private fun wishlistsListTestNode(userId: UserId? = null): NavigationNode<WishlistsListViewConfig, ViewConfig> {
    val chain = NavigationChain<ViewConfig>(parentNode = null, nodeFactory = NavigationNodeFactory { _, _ -> null })
    return NavigationNode.Empty(chain, WishlistsListViewConfig(userId))
}

/** Records navigation callbacks without affecting data-loading assertions. */
private class RecordingWishlistsListInteractor : WishlistsListViewInteractor {
    override suspend fun onWishlistSelected(node: NavigationNode<WishlistsListViewConfig, ViewConfig>, wishlistId: WishlistId) = Unit
    override suspend fun onCreateWishlist(node: NavigationNode<WishlistsListViewConfig, ViewConfig>) = Unit
    override suspend fun onBack(node: NavigationNode<WishlistsListViewConfig, ViewConfig>) = Unit
    override suspend fun onShowUserWishlists(node: NavigationNode<WishlistsListViewConfig, ViewConfig>, userId: UserId) = Unit
    override suspend fun onShowUser(node: NavigationNode<WishlistsListViewConfig, ViewConfig>, userId: UserId) = Unit
}

/** Controllable full-surface model fake that records protected and public list reads. */
private class WishlistListTestModel : WishlistsModel {
    /** Mutable authorization state emitted to the ViewModel. */
    val authorised = MutableStateFlow(false)

    /** Mutable authenticated caller identity emitted to the ViewModel. */
    val currentUserId = MutableStateFlow<UserId?>(null)

    /** Number of protected own-list requests received by the fake. */
    var myCalls = 0

    /** Owners requested through the anonymous-safe public-list API. */
    val publicCalls = mutableListOf<UserId>()

    /** Configurable protected own-list response. */
    var myHandler: suspend () -> List<WishlistsFeatureWishlist> = { emptyList() }

    /** Configurable public owner-list response. */
    var publicHandler: suspend (UserId) -> List<WishlistsFeatureWishlist> = { emptyList() }
    override val userAuthorisedState: StateFlow<Boolean> = authorised
    override val currentUserIdFlow: StateFlow<UserId?> = currentUserId
    override val selectedCurrency: StateFlow<CurrencyCode?> = MutableStateFlow(null)
    override suspend fun getMyWishlists(): List<WishlistsFeatureWishlist> { myCalls += 1; return myHandler() }
    override suspend fun getUserWishlists(userId: UserId): List<WishlistsFeatureWishlist> { publicCalls += userId; return publicHandler(userId) }
    override suspend fun getWishlist(id: WishlistId): WishlistsFeatureWishlist? = null
    override suspend fun getWishlistItems(wishlistId: WishlistId): List<WishlistsFeatureItem> = emptyList()
    override suspend fun createWishlist(title: String, defaultPriceUnits: String): WishlistsFeatureWishlist? = null
    override suspend fun updateWishlist(id: WishlistId, title: String, defaultPriceUnits: String): Boolean = false
    override suspend fun deleteWishlist(id: WishlistId): Boolean = false
    override suspend fun createWishlistItem(item: NewWishlistItem): WishlistsFeatureItem? = null
    override suspend fun updateWishlistItem(id: WishlistItemId, item: NewWishlistItem): Boolean = false
    override suspend fun deleteWishlistItem(id: WishlistItemId): Boolean = false
    override suspend fun copyItemToWishlist(sourceItemId: WishlistItemId, sourceWishlistId: WishlistId, targetWishlistId: WishlistId): WishlistsFeatureItem? = null
    override suspend fun enqueueWishlistCopy(sourceWishlistId: WishlistId): Boolean = false
    override suspend fun getUserName(userId: UserId): String? = if (userId == UserId(7L)) "caller" else "public"
    override suspend fun uploadImage(file: MPPFile): FileId? = null
    override fun imageUrl(id: FileId): String = ""
    override suspend fun loadImageBytes(id: FileId): ByteArray? = null
    override suspend fun isCurrencyEnabled(): Boolean = false
    override suspend fun availableCurrencies(): List<CurrencyInfo> = emptyList()
    override suspend fun currencyRates(): CurrencyRates? = null
    override fun selectCurrency(code: CurrencyCode?) = Unit
    override suspend fun getSavedViewMode(): WishlistViewMode = WishlistViewMode.Grid
    override suspend fun saveViewMode(mode: WishlistViewMode) = Unit
}
