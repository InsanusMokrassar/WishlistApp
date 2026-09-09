package dev.inmo.wishlist.features.ui.adminPanel.ui

import dev.inmo.kslog.common.KSLog
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.core.NavigationNode
import dev.inmo.navigation.core.NavigationNodeFactory
import dev.inmo.navigation.core.NavigationNodeState
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
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

@OptIn(ExperimentalCoroutinesApi::class)
class AdminPanelViewModelTest {
    private val alice = AdminUser(UserId(7L), Username("alice"), Email("alice@example.com"))
    private val bob = AdminUser(UserId(8L), Username("bob"), Email("bob@example.com"))
    private val recipient = Email("smtp-test@example.com")

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
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModel = AdminPanelViewModel(
            adminPanelTestNode(),
            model,
            RecordingDashboardInteractor(),
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
            advanceUntilIdle()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun resumeRefreshesChangedUsersAndEmptyResultIsNotLoadFailure() = runTest {
        withSilentPlatformLogs {
            var currentUsers = listOf(alice)
            val model = DashboardTestModel().apply {
                usersHandler = { currentUsers }
            }
            val node = adminPanelTestNode()
            val viewModel = AdminPanelViewModel(
                node,
                model,
                RecordingDashboardInteractor(),
                StandardTestDispatcher(testScheduler),
            )
            try {
                advanceUntilIdle()
                assertEquals(listOf(alice), viewModel.usersState.value)

                currentUsers = listOf(bob)
                node.changeState(NavigationNodeState.RESUMED)
                advanceUntilIdle()
                assertEquals(listOf(bob), viewModel.usersState.value)

                currentUsers = emptyList()
                viewModel.onRetryUsers()
                advanceUntilIdle()
                assertTrue(viewModel.usersState.value.isEmpty())
                assertFalse(viewModel.usersLoadingState.value)
                assertFalse(viewModel.usersLoadFailedState.value)
            } finally {
                viewModel.scope.cancel()
            }
        }
    }

    @Test
    fun failedUsersLoadCanRetrySuccessfully() = runTest {
        var failLoad = true
        val model = DashboardTestModel().apply {
            usersHandler = {
                if (failLoad) throw IllegalStateException("users unavailable")
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
            advanceUntilIdle()
            assertTrue(viewModel.usersState.value.isEmpty())
            assertTrue(viewModel.usersLoadFailedState.value)

            failLoad = false
            viewModel.onRetryUsers()
            advanceUntilIdle()

            assertEquals(listOf(alice), viewModel.usersState.value)
            assertFalse(viewModel.usersLoadFailedState.value)
            assertFalse(viewModel.usersLoadingState.value)
        } finally {
            viewModel.scope.cancel()
        }
    }

    @Test
    fun newerUsersRefreshWinsOverLateNonCooperativeOlderResponse() = runTest {
        val olderEntered = CompletableDeferred<Unit>()
        val releaseOlder = CompletableDeferred<Unit>()
        val model = DashboardTestModel().apply {
            usersHandler = { emptyList() }
        }
        val viewModel = AdminPanelViewModel(
            adminPanelTestNode(),
            model,
            RecordingDashboardInteractor(),
            StandardTestDispatcher(testScheduler),
        )
        try {
            advanceUntilIdle()
            model.usersHandler = {
                olderEntered.complete(Unit)
                withContext(NonCancellable) { releaseOlder.await() }
                listOf(alice)
            }
            viewModel.onRetryUsers()
            runCurrent()
            assertTrue(olderEntered.isCompleted)

            model.usersHandler = { listOf(bob) }
            viewModel.onRetryUsers()
            advanceUntilIdle()
            assertEquals(listOf(bob), viewModel.usersState.value)

            releaseOlder.complete(Unit)
            advanceUntilIdle()
            assertEquals(listOf(bob), viewModel.usersState.value)
            assertFalse(viewModel.usersLoadingState.value)
        } finally {
            releaseOlder.complete(Unit)
            viewModel.scope.cancel()
        }
    }

    @Test
    fun disabledSmtpNeverForwardsTestEmail() = runTest {
        val model = DashboardTestModel().apply { probeHandler = { false } }
        val viewModel = AdminPanelViewModel(
            adminPanelTestNode(),
            model,
            RecordingDashboardInteractor(),
            StandardTestDispatcher(testScheduler),
        )
        try {
            advanceUntilIdle()
            assertFalse(viewModel.emailFeatureEnabledState.value)

            viewModel.onSendTestEmail(recipient)
            advanceUntilIdle()

            assertTrue(model.sentRecipients.isEmpty())
            assertNull(viewModel.sendTestEmailState.value)
        } finally {
            viewModel.scope.cancel()
        }
    }

    @Test
    fun smtpBusyStateSuppressesDuplicateAndForwardsTrueFalseAndThrowResults() = runTest {
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Boolean>()
        val model = DashboardTestModel().apply {
            sendHandler = {
                entered.complete(Unit)
                withContext(NonCancellable) { release.await() }
            }
        }
        val viewModel = AdminPanelViewModel(
            adminPanelTestNode(),
            model,
            RecordingDashboardInteractor(),
            StandardTestDispatcher(testScheduler),
        )
        try {
            advanceUntilIdle()
            assertTrue(viewModel.emailFeatureEnabledState.value)

            viewModel.onSendTestEmail(recipient)
            viewModel.onSendTestEmail(Email("duplicate@example.com"))
            runCurrent()
            assertTrue(entered.isCompleted)
            assertEquals(listOf(recipient), model.sentRecipients)
            assertTrue(viewModel.sendTestEmailInProgressState.value)

            release.complete(true)
            advanceUntilIdle()
            assertEquals(true, viewModel.sendTestEmailState.value)
            assertFalse(viewModel.sendTestEmailInProgressState.value)

            model.sendHandler = { false }
            viewModel.onSendTestEmail(recipient)
            advanceUntilIdle()
            assertEquals(false, viewModel.sendTestEmailState.value)

            model.sendHandler = { throw IllegalStateException("SMTP unavailable") }
            viewModel.onSendTestEmail(recipient)
            advanceUntilIdle()
            assertEquals(false, viewModel.sendTestEmailState.value)
            assertFalse(viewModel.sendTestEmailInProgressState.value)
        } finally {
            release.complete(false)
            viewModel.scope.cancel()
        }
    }

    @Test
    fun cancellingSmtpRequestClearsBusyState() = runTest {
        withSilentPlatformLogs {
            val entered = CompletableDeferred<Unit>()
            val model = DashboardTestModel().apply {
                sendHandler = {
                    entered.complete(Unit)
                    CompletableDeferred<Boolean>().await()
                }
            }
            val viewModel = AdminPanelViewModel(
                adminPanelTestNode(),
                model,
                RecordingDashboardInteractor(),
                StandardTestDispatcher(testScheduler),
            )
            try {
                advanceUntilIdle()
                viewModel.onSendTestEmail(recipient)
                runCurrent()
                assertTrue(entered.isCompleted)
                assertTrue(viewModel.sendTestEmailInProgressState.value)

                viewModel.scope.cancel()
                advanceUntilIdle()

                assertFalse(viewModel.sendTestEmailInProgressState.value)
            } finally {
                viewModel.scope.cancel()
            }
        }
    }

    @Test
    fun dashboardCallbacksForwardExactDestinations() = runTest {
        val interactor = RecordingDashboardInteractor()
        val viewModel = AdminPanelViewModel(
            adminPanelTestNode(),
            DashboardTestModel(),
            interactor,
            StandardTestDispatcher(testScheduler),
        )
        try {
            viewModel.onOpenUsers()
            viewModel.onOpenWishlists()
            viewModel.onUserSelected(alice.id)
            advanceUntilIdle()

            assertEquals(1, interactor.openUsersCalls)
            assertEquals(1, interactor.openWishlistsCalls)
            assertEquals(listOf(alice.id), interactor.selectedUserIds)
        } finally {
            viewModel.scope.cancel()
        }
    }
}

private class DashboardTestModel : AdminPanelModel {
    val authorisedState = MutableStateFlow(true)
    var usersHandler: suspend () -> List<AdminUser> = { emptyList() }
    var probeHandler: suspend () -> Boolean = { true }
    var sendHandler: suspend (Email) -> Boolean = { false }
    val sentRecipients = mutableListOf<Email>()

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
    override suspend fun sendTestEmail(recipient: Email): Boolean {
        sentRecipients += recipient
        return sendHandler(recipient)
    }
}

private class RecordingDashboardInteractor : AdminPanelViewInteractor {
    var openUsersCalls = 0
    var openWishlistsCalls = 0
    val selectedUserIds = mutableListOf<UserId>()

    override suspend fun onOpenUsers(node: NavigationNode<AdminPanelViewConfig, ViewConfig>) {
        openUsersCalls += 1
    }

    override suspend fun onOpenWishlists(node: NavigationNode<AdminPanelViewConfig, ViewConfig>) {
        openWishlistsCalls += 1
    }

    override suspend fun onUserSelected(node: NavigationNode<AdminPanelViewConfig, ViewConfig>, userId: UserId) {
        selectedUserIds += userId
    }
}

private fun adminPanelTestNode(): NavigationNode<AdminPanelViewConfig, ViewConfig> {
    val chain = NavigationChain<ViewConfig>(
        parentNode = null,
        nodeFactory = NavigationNodeFactory { _, _ -> null },
    )
    return NavigationNode.Empty(chain, AdminPanelViewConfig())
}

private suspend fun <T> withSilentPlatformLogs(block: suspend () -> T): T {
    val originalLogger = KSLog.default
    KSLog.default = KSLog { _, _, _, _ -> }
    return try {
        block()
    } finally {
        KSLog.default = originalLogger
    }
}
