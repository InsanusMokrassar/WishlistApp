package dev.inmo.wishlist.client

import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.core.NavigationNode
import dev.inmo.navigation.core.NavigationNodeFactory
import dev.inmo.navigation.core.repo.ConfigHolder
import dev.inmo.navigation.core.repo.NavigationConfigsRepo
import dev.inmo.micro_utils.common.MPPFile
import dev.inmo.wishlist.features.auth.common.models.AuthFeatureUser
import dev.inmo.wishlist.features.auth.common.models.CompletePasswordChangeRequest
import dev.inmo.wishlist.features.auth.common.models.Password
import dev.inmo.wishlist.features.auth.common.models.PasswordChangeEmailRequestResult
import dev.inmo.wishlist.features.auth.common.models.PasswordChangeResult
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.deeplinks.common.models.DeepLinkId
import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.email.common.models.EmailVerificationRequestResult
import dev.inmo.wishlist.features.files.common.models.FileId
import dev.inmo.wishlist.features.ui.users.ui.PasswordChangeViewModel
import dev.inmo.wishlist.features.ui.users.ui.PasswordChangeViewConfig
import dev.inmo.wishlist.features.ui.users.ui.PasswordChangeViewInteractor
import dev.inmo.wishlist.features.ui.users.ui.UsersListViewConfig
import dev.inmo.wishlist.features.ui.users.ui.UsersModel
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.users.common.models.Username
import dev.inmo.wishlist.features.users.common.models.UsersFeatureUser
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Minimal production-interface model that holds completion until the lifecycle test releases it. */
internal class HeldPasswordChangeUsersModel : UsersModel {
    /** Completion gate controlled by the enclosing test. */
    val completion = CompletableDeferred<PasswordChangeResult?>()

    /** Immutable requests accepted by the actual submitting ViewModel. */
    val requests = mutableListOf<CompletePasswordChangeRequest>()

    /** Makes the test double return a result after cancellation to exercise the ViewModel guard. */
    var returnsChangedAfterCancellation = false

    /** Inert authorization state required by the shared users model interface. */
    override val userAuthorisedState: StateFlow<Boolean> = MutableStateFlow(false)

    /** Inert current-user state required by the shared users model interface. */
    override val currentUserIdFlow: StateFlow<UserId?> = MutableStateFlow(null)

    /** Inert root state required by the shared users model interface. */
    override val isCurrentUserRootFlow: StateFlow<Boolean> = MutableStateFlow(false)

    /** Inert avatar capability state required by the shared users model interface. */
    override val canChangeAvatarForOthersFlow: StateFlow<Boolean> = MutableStateFlow(false)

    override suspend fun getAllUsers(): List<UsersFeatureUser> = emptyList()
    override suspend fun getUser(id: UserId): UsersFeatureUser? = null
    override suspend fun getMyProfile(): AuthFeatureUser? = null
    override suspend fun isEmailFeatureEnabled(): Boolean = false
    override suspend fun setMyEmail(email: Email?): Boolean = false
    override suspend fun requestMyEmailVerification(expectedEmail: Email): EmailVerificationRequestResult = EmailVerificationRequestResult.Unavailable
    override suspend fun requestPasswordChangeEmail(expectedEmail: Email): PasswordChangeEmailRequestResult? = null
    override suspend fun completePasswordChange(request: CompletePasswordChangeRequest): PasswordChangeResult? {
        requests += request
        return try {
            completion.await()
        } catch (cause: CancellationException) {
            if (returnsChangedAfterCancellation) PasswordChangeResult.Changed else throw cause
        }
    }
    override suspend fun updateUsername(id: UserId, username: Username): Boolean = false
    override suspend fun setPassword(id: UserId, password: Password): Boolean = false
    override suspend fun deleteUser(id: UserId): Boolean = false
    override suspend fun getAvatar(userId: UserId): FileId? = null
    override suspend fun uploadAvatar(userId: UserId, file: MPPFile): FileId? = null
    override fun imageUrl(id: FileId): String = ""
    override suspend fun loadImageBytes(id: FileId): ByteArray? = null
}

/** Exercises the production client password-change navigation binding against an active chain. */
@OptIn(ExperimentalCoroutinesApi::class)
class PasswordChangeInteractorTest {
    private val approvalId = DeepLinkId("123e4567-e89b-42d3-a456-426614174000")

    /** Proves replacement destroys the submitting ViewModel before the root-owned save completes. */
    @Test
    fun actualSubmittingViewModelDestructionPrecedesCompletedPersistence() = runTest {
        val navigationConfigsRepo = object : NavigationConfigsRepo<ViewConfig> {
            var saved: ConfigHolder<ViewConfig>? = null
            override fun save(holder: ConfigHolder<ViewConfig>) { saved = holder }
            override fun get(): ConfigHolder<ViewConfig>? = null
        }
        val application = startKoin {
            modules(module {
                with(dev.inmo.wishlist.features.common.common.Plugin) { setupDI(JsonObject(emptyMap())) }
                with(dev.inmo.wishlist.features.ui.users.Plugin) { setupDI(JsonObject(emptyMap())) }
                with(ClientPlugin) { setupDI(JsonObject(emptyMap())) }
                single<NavigationConfigsRepo<ViewConfig>> { navigationConfigsRepo }
            })
        }
        try {
            val owner = application.koin.get<PasswordChangeNavigationOwner>()
            val interactor = application.koin.get<PasswordChangeViewInteractor>()
            val chain = NavigationChain<ViewConfig>(null, NavigationNodeFactory { parent, config ->
                NavigationNode.Empty(parent, config)
            })
            val chainJob = chain.start(this)
            val unbind = owner.bind(chain, this)
            try {
                val model = HeldPasswordChangeUsersModel()
                chain.push(UsersListViewConfig())
                val pending = PasswordChangeViewConfig.Pending(UserId(7), approvalId)
                val node = checkNotNull(chain.push(pending)) as NavigationNode<PasswordChangeViewConfig, ViewConfig>
                val viewModel = PasswordChangeViewModel(node, model, interactor, StandardTestDispatcher(testScheduler))
                runCurrent()
                viewModel.onPasswordChanged("replacement-password")
                viewModel.onConfirmationChanged("replacement-password")
                viewModel.onSubmitPasswordChange()
                runCurrent()
                assertEquals(listOf(CompletePasswordChangeRequest(UserId(7), approvalId, Password("replacement-password"))), model.requests)
                model.completion.complete(PasswordChangeResult.Changed)
                advanceUntilIdle()
                assertEquals(PasswordChangeViewConfig.Completed, chain.stackFlow.value.last().config)
                assertTrue(viewModel.passwordState.value.isEmpty())
                assertTrue(viewModel.confirmationState.value.isEmpty())
                assertEquals(dev.inmo.navigation.core.NavigationNodeState.NEW, node.state)
                val saved = assertNotNull(navigationConfigsRepo.saved) as ConfigHolder.Chain<ViewConfig>
                assertEquals(
                    PasswordChangeViewConfig.Completed,
                    (saved.firstNodeConfig as ConfigHolder.Node<ViewConfig>).subnode?.config,
                )
                assertFalse(saved.toString().contains(approvalId.string))
                assertFalse(saved.toString().contains("replacement-password"))

                val completedNode = chain.stackFlow.value.last() as NavigationNode<PasswordChangeViewConfig, ViewConfig>
                val completedViewModel = PasswordChangeViewModel(
                    completedNode,
                    model,
                    interactor,
                    StandardTestDispatcher(testScheduler),
                )
                completedViewModel.onSubmitPasswordChange()
                advanceUntilIdle()
                assertEquals(1, model.requests.size)
                interactor.onContinue(completedNode)
                advanceUntilIdle()
                assertTrue(chain.stackFlow.value.last().config is UsersListViewConfig)
                assertNotNull(navigationConfigsRepo.saved)
                assertEquals(0, owner.activeTransitionCount)
            } finally {
                unbind()
                chainJob.cancel()
            }
        } finally {
            stopKoin()
        }
    }

    @Test
    fun changedPendingReplacesCredentialRouteAndContinueAlwaysReachesUsersList() = runTest {
        val navigationConfigsRepo = object : NavigationConfigsRepo<ViewConfig> {
            var saved: ConfigHolder<ViewConfig>? = null

            override fun save(holder: ConfigHolder<ViewConfig>) {
                saved = holder
            }

            override fun get(): ConfigHolder<ViewConfig>? = null
        }
        val application = startKoin {
            modules(module {
                with(dev.inmo.wishlist.features.common.common.Plugin) { setupDI(JsonObject(emptyMap())) }
                with(dev.inmo.wishlist.features.ui.users.Plugin) { setupDI(JsonObject(emptyMap())) }
                with(ClientPlugin) { setupDI(JsonObject(emptyMap())) }
                single<NavigationConfigsRepo<ViewConfig>> { navigationConfigsRepo }
            })
        }
        try {
            val json = application.koin.get<Json>()
            val pending = PasswordChangeViewConfig.Pending(UserId(7), approvalId)
            assertEquals(
                pending,
                json.decodeFromString(
                    PolymorphicSerializer(ViewConfig::class),
                    json.encodeToString(PolymorphicSerializer(ViewConfig::class), pending),
                ),
            )
            val interactor = application.koin.get<PasswordChangeViewInteractor>()
            val owner = application.koin.get<PasswordChangeNavigationOwner>()
            val chain = NavigationChain<ViewConfig>(null, NavigationNodeFactory { parent, config ->
                NavigationNode.Empty(parent, config)
            })
            val chainJob = chain.start(this)
            try {
                val unbind = owner.bind(chain, this)
                chain.push(UsersListViewConfig())
                val pendingNode = checkNotNull(chain.push(pending))
                advanceUntilIdle()

                interactor.onChanged(pendingNode as NavigationNode<PasswordChangeViewConfig, ViewConfig>)
                advanceUntilIdle()

                assertEquals(PasswordChangeViewConfig.Completed, chain.stackFlow.value.last().config)
                assertFalse(chain.stackFlow.value.any { it.config.toString().contains(approvalId.string) })
                val persistedChain = navigationConfigsRepo.saved as ConfigHolder.Chain<ViewConfig>
                assertEquals(
                    PasswordChangeViewConfig.Completed,
                    (persistedChain.firstNodeConfig as ConfigHolder.Node<ViewConfig>).subnode?.config,
                )

                val completedNode = chain.stackFlow.value.last() as NavigationNode<PasswordChangeViewConfig, ViewConfig>
                interactor.onContinue(completedNode)
                advanceUntilIdle()
                assertTrue(chain.stackFlow.value.last().config is UsersListViewConfig)
                assertTrue(chain.stackFlow.value.isNotEmpty())
                assertNotNull(navigationConfigsRepo.saved)
                unbind()
            } finally {
                chainJob.cancel()
            }
        } finally {
            stopKoin()
        }
    }

    /** Rejects a stale pending callback after another destination has become the active last node. */
    @Test
    fun stalePendingCannotReplaceNewerDestination() = runTest {
        var saves = 0
        val navigationConfigsRepo = object : NavigationConfigsRepo<ViewConfig> {
            override fun save(holder: ConfigHolder<ViewConfig>) { saves += 1 }
            override fun get(): ConfigHolder<ViewConfig>? = null
        }
        val application = startKoin {
            modules(module {
                with(dev.inmo.wishlist.features.common.common.Plugin) { setupDI(JsonObject(emptyMap())) }
                with(dev.inmo.wishlist.features.ui.users.Plugin) { setupDI(JsonObject(emptyMap())) }
                with(ClientPlugin) { setupDI(JsonObject(emptyMap())) }
                single<NavigationConfigsRepo<ViewConfig>> { navigationConfigsRepo }
            })
        }
        try {
            val owner = application.koin.get<PasswordChangeNavigationOwner>()
            val interactor = application.koin.get<PasswordChangeViewInteractor>()
            val chain = NavigationChain<ViewConfig>(null, NavigationNodeFactory { parent, config ->
                NavigationNode.Empty(parent, config)
            })
            val chainJob = chain.start(this)
            val unbind = owner.bind(chain, this)
            try {
                chain.push(UsersListViewConfig())
                val pending = checkNotNull(chain.push(PasswordChangeViewConfig.Pending(UserId(7), approvalId)))
                    as NavigationNode<PasswordChangeViewConfig, ViewConfig>
                advanceUntilIdle()
                chain.push(UsersListViewConfig())
                advanceUntilIdle()
                interactor.onChanged(pending)
                advanceUntilIdle()
                assertTrue(chain.stackFlow.value.last().config is UsersListViewConfig)
                assertEquals(0, saves)
            } finally {
                unbind()
                chainJob.cancel()
            }
        } finally {
            stopKoin()
        }
    }

    /** Cancellation-resistant model output cannot hand a destroyed pending page to the root owner. */
    @Test
    fun cancelledSubmittingViewModelCannotStartChangedHandoff() = runTest {
        var saves = 0
        val repo = object : NavigationConfigsRepo<ViewConfig> {
            override fun save(holder: ConfigHolder<ViewConfig>) { saves += 1 }
            override fun get(): ConfigHolder<ViewConfig>? = null
        }
        val owner = PasswordChangeNavigationOwner(repo)
        val chain = NavigationChain<ViewConfig>(null, NavigationNodeFactory { parent, config -> NavigationNode.Empty(parent, config) })
        val chainJob = chain.start(this)
        val unbind = owner.bind(chain, this)
        try {
            val model = HeldPasswordChangeUsersModel().apply { returnsChangedAfterCancellation = true }
            chain.push(UsersListViewConfig())
            val pending = checkNotNull(chain.push(PasswordChangeViewConfig.Pending(UserId(7), approvalId)))
                as NavigationNode<PasswordChangeViewConfig, ViewConfig>
            advanceUntilIdle()
            val viewModel = PasswordChangeViewModel(pending, model, owner, StandardTestDispatcher(testScheduler))
            viewModel.onPasswordChanged("cancellation-resistant")
            viewModel.onConfirmationChanged("cancellation-resistant")
            viewModel.onSubmitPasswordChange()
            advanceUntilIdle()
            chain.replace(pending, UsersListViewConfig())
            advanceUntilIdle()
            assertEquals(dev.inmo.navigation.core.NavigationNodeState.NEW, pending.state)
            advanceUntilIdle()
            assertEquals(1, model.requests.size)
            assertEquals(0, saves)
            assertTrue(chain.stackFlow.value.last().config is UsersListViewConfig)
            assertEquals(0, owner.activeTransitionCount)
        } finally {
            unbind()
            chainJob.cancelAndJoin()
        }
    }

    /** A root binding disposal cancels a queued replacement before a stopped chain can publish it. */
    @Test
    fun rootBindingDisposalCancelsPendingTransitionAndCleansOwnerJobs() = runTest {
        var saves = 0
        val repo = object : NavigationConfigsRepo<ViewConfig> {
            override fun save(holder: ConfigHolder<ViewConfig>) { saves += 1 }
            override fun get(): ConfigHolder<ViewConfig>? = null
        }
        val chain = NavigationChain<ViewConfig>(null, NavigationNodeFactory { parent, config -> NavigationNode.Empty(parent, config) })
        val chainJob = chain.start(this)
        chain.push(UsersListViewConfig())
        val pending = checkNotNull(chain.push(PasswordChangeViewConfig.Pending(UserId(7), approvalId)))
            as NavigationNode<PasswordChangeViewConfig, ViewConfig>
        advanceUntilIdle()
        chainJob.cancelAndJoin()
        val rootJob = SupervisorJob()
        val owner = PasswordChangeNavigationOwner(repo)
        val unbind = owner.bind(chain, CoroutineScope(coroutineContext + rootJob))
        try {
            owner.onChanged(pending)
            runCurrent()
            assertEquals(1, owner.activeTransitionCount)
            unbind()
            advanceUntilIdle()
            assertEquals(0, owner.activeTransitionCount)
            assertEquals(0, saves)
        } finally {
            rootJob.cancelAndJoin()
        }
    }

    /** A stopped chain reaches the finite five-second observer timeout without a stale save. */
    @Test
    fun unprocessedReplacementTimesOutAndCleansOwnerJobs() = runTest {
        var saves = 0
        val repo = object : NavigationConfigsRepo<ViewConfig> {
            override fun save(holder: ConfigHolder<ViewConfig>) { saves += 1 }
            override fun get(): ConfigHolder<ViewConfig>? = null
        }
        val chain = NavigationChain<ViewConfig>(null, NavigationNodeFactory { parent, config -> NavigationNode.Empty(parent, config) })
        val chainJob = chain.start(this)
        chain.push(UsersListViewConfig())
        val pending = checkNotNull(chain.push(PasswordChangeViewConfig.Pending(UserId(7), approvalId)))
            as NavigationNode<PasswordChangeViewConfig, ViewConfig>
        advanceUntilIdle()
        chainJob.cancelAndJoin()
        val owner = PasswordChangeNavigationOwner(repo)
        val unbind = owner.bind(chain, this)
        try {
            owner.onChanged(pending)
            runCurrent()
            assertEquals(1, owner.activeTransitionCount)
            advanceTimeBy(5_001)
            advanceUntilIdle()
            assertEquals(0, saves)
            assertEquals(0, owner.activeTransitionCount)
        } finally {
            unbind()
        }
    }

    /** A synchronous save failure never replays HTTP and a later completed-page exit remains usable. */
    @Test
    fun saveFailureDoesNotReplayAndLaterContinuePersistsUsersList() = runTest {
        var saves = 0
        val repo = object : NavigationConfigsRepo<ViewConfig> {
            var persisted: ConfigHolder<ViewConfig>? = null
            override fun save(holder: ConfigHolder<ViewConfig>) {
                saves += 1
                if (saves == 1) error("first save fails")
                persisted = holder
            }
            override fun get(): ConfigHolder<ViewConfig>? = null
        }
        val owner = PasswordChangeNavigationOwner(repo)
        val chain = NavigationChain<ViewConfig>(null, NavigationNodeFactory { parent, config -> NavigationNode.Empty(parent, config) })
        val chainJob = chain.start(this)
        val unbind = owner.bind(chain, this)
        try {
            chain.push(UsersListViewConfig())
            val pending = checkNotNull(chain.push(PasswordChangeViewConfig.Pending(UserId(7), approvalId)))
                as NavigationNode<PasswordChangeViewConfig, ViewConfig>
            advanceUntilIdle()
            owner.onChanged(pending)
            advanceUntilIdle()
            assertEquals(1, saves)
            val completed = chain.stackFlow.value.last() as NavigationNode<PasswordChangeViewConfig, ViewConfig>
            owner.onContinue(completed)
            advanceUntilIdle()
            assertEquals(2, saves)
            assertNotNull(repo.persisted)
            assertTrue(chain.stackFlow.value.last().config is UsersListViewConfig)
            assertEquals(0, owner.activeTransitionCount)
        } finally {
            unbind()
            chainJob.cancelAndJoin()
        }
    }
}
