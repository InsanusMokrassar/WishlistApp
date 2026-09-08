package dev.inmo.wishlist.features.ui.adminPanel.ui

import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.core.NavigationNode
import dev.inmo.navigation.core.NavigationNodeFactory
import dev.inmo.wishlist.features.admin.common.models.AdminUser
import dev.inmo.wishlist.features.admin.common.models.AdminWishlist
import dev.inmo.wishlist.features.admin.common.models.AdminWishlistItem
import dev.inmo.wishlist.features.admin.common.models.NewUserWithPassword
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.users.common.models.NewUser
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.users.common.models.Username
import dev.inmo.wishlist.features.wishlist.common.models.NewWishlist
import dev.inmo.wishlist.features.wishlist.common.models.NewWishlistItem
import dev.inmo.wishlist.features.wishlist.common.models.WishlistId
import dev.inmo.wishlist.features.wishlist.common.models.WishlistItemId
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AdminPanelViewModelTest {
    private val alice = AdminUser(UserId(7L), Username("alice"), Email("alice@example.com"))

    @Test
    fun loadsRealUsersWhenCapabilityProbeFails() = runTest {
        val model = DashboardTestModel().apply {
            usersHandler = { listOf(alice) }
            probeHandler = { throw IllegalStateException("SMTP probe unavailable") }
        }
        val viewModel = AdminPanelViewModel(
            adminPanelTestNode(),
            model,
            RecordingDashboardInteractor(),
            StandardTestDispatcher(testScheduler),
        )
        try {
            advanceUntilIdle()

            assertEquals(listOf(alice), viewModel.usersState.value)
            assertFalse(viewModel.usersLoadingState.value)
            assertFalse(viewModel.usersLoadFailedState.value)
            assertFalse(viewModel.emailFeatureEnabledState.value)
        } finally {
            viewModel.scope.cancel()
        }
    }

    @Test
    fun logoutClearsSuspendedUsersRefreshBeforeLateResponse() = runTest {
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val model = DashboardTestModel().apply {
            usersHandler = {
                entered.complete(Unit)
                withContext(NonCancellable) { release.await() }
                listOf(alice)
            }
        }
        val viewModel = AdminPanelViewModel(
            adminPanelTestNode(),
            model,
            RecordingDashboardInteractor(),
            StandardTestDispatcher(testScheduler),
        )
        try {
            runCurrent()
            assertTrue(entered.isCompleted)

            model.authorisedState.value = false
            runCurrent()
            assertTrue(viewModel.usersState.value.isEmpty())
            assertFalse(viewModel.usersLoadingState.value)

            release.complete(Unit)
            advanceUntilIdle()
            assertTrue(viewModel.usersState.value.isEmpty())
            assertFalse(viewModel.usersLoadingState.value)
        } finally {
            release.complete(Unit)
            viewModel.scope.cancel()
        }
    }
}

private class DashboardTestModel : AdminPanelModel {
    val authorisedState = MutableStateFlow(true)
    var usersHandler: suspend () -> List<AdminUser> = { emptyList() }
    var probeHandler: suspend () -> Boolean = { true }

    override val userAuthorisedState: StateFlow<Boolean> = authorisedState
    override suspend fun getAllUsers(): List<AdminUser> = usersHandler()
    override suspend fun getUserById(id: UserId): AdminUser? = null
    override suspend fun createUser(newUser: NewUserWithPassword): AdminUser? = null
    override suspend fun updateUser(id: UserId, newUser: NewUser): Boolean = false
    override suspend fun updateUsername(id: UserId, username: Username): Boolean = false
    override suspend fun deleteUser(id: UserId): Boolean = false
    override suspend fun getAllWishlists(): List<AdminWishlist> = emptyList()
    override suspend fun getWishlistsByUser(userId: UserId): List<AdminWishlist> = emptyList()
    override suspend fun getWishlistById(id: WishlistId): AdminWishlist? = null
    override suspend fun createWishlist(newWishlist: NewWishlist): AdminWishlist? = null
    override suspend fun updateWishlist(id: WishlistId, userId: UserId, title: String): Boolean = false
    override suspend fun deleteWishlist(id: WishlistId): Boolean = false
    override suspend fun getItemsByWishlist(wishlistId: WishlistId): List<AdminWishlistItem> = emptyList()
    override suspend fun createWishlistItem(item: NewWishlistItem): AdminWishlistItem? = null
    override suspend fun updateWishlistItem(id: WishlistItemId, item: NewWishlistItem): Boolean = false
    override suspend fun deleteWishlistItem(id: WishlistItemId): Boolean = false
    override suspend fun isEmailFeatureEnabled(): Boolean = probeHandler()
    override suspend fun sendTestEmail(recipient: Email): Boolean = false
}

private class RecordingDashboardInteractor : AdminPanelViewInteractor {
    override suspend fun onOpenUsers(node: NavigationNode<AdminPanelViewConfig, ViewConfig>) = Unit
    override suspend fun onOpenWishlists(node: NavigationNode<AdminPanelViewConfig, ViewConfig>) = Unit
    override suspend fun onUserSelected(node: NavigationNode<AdminPanelViewConfig, ViewConfig>, userId: UserId) = Unit
}

private fun adminPanelTestNode(): NavigationNode<AdminPanelViewConfig, ViewConfig> {
    val chain = NavigationChain<ViewConfig>(
        parentNode = null,
        nodeFactory = NavigationNodeFactory { _, _ -> null },
    )
    return NavigationNode.Empty(chain, AdminPanelViewConfig())
}
