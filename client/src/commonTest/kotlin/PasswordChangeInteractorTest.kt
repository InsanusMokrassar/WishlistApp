package dev.inmo.wishlist.client

import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.core.NavigationNode
import dev.inmo.navigation.core.NavigationNodeFactory
import dev.inmo.navigation.core.onDestroyFlow
import dev.inmo.navigation.core.extensions.rootChain
import dev.inmo.navigation.core.repo.ConfigHolder
import dev.inmo.navigation.core.repo.NavigationConfigsRepo
import dev.inmo.micro_utils.common.MPPFile
import dev.inmo.wishlist.features.auth.common.models.AuthFeatureUser
import dev.inmo.wishlist.features.auth.common.models.CompletePasswordChangeRequest
import dev.inmo.wishlist.features.auth.common.models.Password
import dev.inmo.wishlist.features.auth.common.models.PasswordChangeEmailRequestResult
import dev.inmo.wishlist.features.auth.common.models.PasswordChangeResult
import dev.inmo.wishlist.features.auth.client.PasswordChangeFeature
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.deeplinks.common.models.DeepLinkId
import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.email.common.models.EmailVerificationRequestResult
import dev.inmo.wishlist.features.files.common.models.FileId
import dev.inmo.wishlist.features.ui.users.ui.PasswordChangeViewModel
import dev.inmo.wishlist.features.ui.users.ui.PasswordChangeViewConfig
import dev.inmo.wishlist.features.ui.users.ui.PasswordChangeViewInteractor
import dev.inmo.wishlist.features.ui.users.ui.UserViewConfig
import dev.inmo.wishlist.features.ui.users.ui.UsersListViewConfig
import dev.inmo.wishlist.features.ui.users.ui.UsersModel
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.users.common.models.Username
import dev.inmo.wishlist.features.users.common.models.UsersFeatureUser
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
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
import kotlin.test.assertSame
import kotlin.test.assertTrue

/** Minimal production-interface model that holds completion until the lifecycle test releases it. */
internal class HeldPasswordChangeUsersModel(
    private val completionFeature: PasswordChangeFeature? = null,
) : UsersModel {
    /** Completion gate controlled by the enclosing test. */
    val completion = CompletableDeferred<PasswordChangeResult?>()

    /** Immutable requests accepted by the actual submitting ViewModel. */
    val requests = mutableListOf<CompletePasswordChangeRequest>()

    /** Signals that the submitting ViewModel reached the held completion boundary. */
    val requestReceived = CompletableDeferred<Unit>()

    val returnedAfterCancellation = CompletableDeferred<Unit>()

    val completionReturned = CompletableDeferred<PasswordChangeResult?>()

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

    /** Returns no public users for this navigation-only test. */
    override suspend fun getAllUsers(): List<UsersFeatureUser> = emptyList()
    /** Returns no user detail for this navigation-only test. */
    override suspend fun getUser(id: UserId): UsersFeatureUser? = null
    /** Returns no authenticated profile for this navigation-only test. */
    override suspend fun getMyProfile(): AuthFeatureUser? = null
    /** Reports email infrastructure as unavailable. */
    override suspend fun isEmailFeatureEnabled(): Boolean = false
    /** Rejects private email mutation in this test double. */
    override suspend fun setMyEmail(email: Email?): Boolean = false
    /** Rejects email verification requests in this test double. */
    override suspend fun requestMyEmailVerification(expectedEmail: Email): EmailVerificationRequestResult = EmailVerificationRequestResult.Unavailable
    /** Does not issue password-change email approvals. */
    override suspend fun requestPasswordChangeEmail(expectedEmail: Email): PasswordChangeEmailRequestResult? = null
    /** Holds completion until the test-controlled deferred result is released. */
    override suspend fun completePasswordChange(request: CompletePasswordChangeRequest): PasswordChangeResult? {
        requests += request.copy(password = Password(request.password.string))
        requestReceived.complete(Unit)
        return try {
            val result = if (completionFeature != null) {
                completionFeature.completePasswordChange(request)
            } else {
                completion.await()
            }
            completionReturned.complete(result)
            result
        } catch (cause: CancellationException) {
            if (returnsChangedAfterCancellation) {
                returnedAfterCancellation.complete(Unit)
                PasswordChangeResult.Changed
            } else {
                throw cause
            }
        }
    }
    /** Returns no administrator username mutation. */
    override suspend fun updateUsername(id: UserId, username: Username): Boolean = false
    /** Returns no administrator password mutation. */
    override suspend fun setPassword(id: UserId, password: Password): Boolean = false
    /** Returns no administrator deletion. */
    override suspend fun deleteUser(id: UserId): Boolean = false
    /** Returns no avatar metadata. */
    override suspend fun getAvatar(userId: UserId): FileId? = null
    /** Rejects avatar uploads in this test double. */
    override suspend fun uploadAvatar(userId: UserId, file: MPPFile): FileId? = null
    /** Returns no avatar URL. */
    override fun imageUrl(id: FileId): String = ""
    /** Returns no downloaded avatar bytes. */
    override suspend fun loadImageBytes(id: FileId): ByteArray? = null
}

/** Exercises the production client password-change navigation binding against an active chain. */
@OptIn(ExperimentalCoroutinesApi::class)
class PasswordChangeInteractorTest {
    /** Canonical approval reused by all navigation fixtures. */
    private val approvalId = DeepLinkId("123e4567-e89b-42d3-a456-426614174000")

    /** Rejects both stale callbacks when immutable ancestry still points at a detached former root. */
    @Test
    fun detachedPendingAtEntryStartsNoTransition() = runTest {
        val repo = RecordingPasswordNavigationRepo()
        val owner = PasswordChangeNavigationOwner(repo)
        var completedFactoryCalls = 0
        val root = NavigationChain<ViewConfig>(null, NavigationNodeFactory { parent, config ->
            NavigationNode.Empty(parent, config).also {
                if (config == PasswordChangeViewConfig.Completed) completedFactoryCalls += 1
            }
        })
        val rootJob = root.start(this)
        val unbind = owner.bind(root, this)
        try {
            val ancestor = checkNotNull(root.push(UsersListViewConfig()))
            val leaf = ancestor.createEmptySubChain()
            val pending = checkNotNull(leaf.push(PasswordChangeViewConfig.Pending(UserId(7), approvalId)))
                as NavigationNode<PasswordChangeViewConfig, ViewConfig>
            advanceUntilIdle()

            assertSame(root, pending.chain.rootChain())
            assertSame(ancestor, pending.chain.parentNode)
            val replacement = checkNotNull(root.replace(ancestor, UsersListViewConfig())).second
            advanceUntilIdle()
            assertSame(root, pending.chain.rootChain())
            assertSame(ancestor, pending.chain.parentNode)

            owner.onChanged(pending)
            owner.onContinue(pending)
            runCurrent()

            assertEquals(0, owner.activeTransitionCount)
            assertEquals(0, completedFactoryCalls)
            assertEquals(0, repo.saveAttempts)
            assertSame(replacement, root.stackFlow.value.single())
            assertTrue(leaf.stackFlow.value.single() === pending)
        } finally {
            unbind()
            rootJob.cancelAndJoin()
        }
    }

    /** Stops a queued leaf transition after an outer ancestor replacement without leaf stack delivery. */
    @Test
    fun detachedWhileQueuedStopsWithoutLeafEmission() = runTest {
        val repo = RecordingPasswordNavigationRepo()
        val owner = PasswordChangeNavigationOwner(repo)
        val root = NavigationChain<ViewConfig>(null, NavigationNodeFactory { parent, config ->
            NavigationNode.Empty(parent, config)
        })
        val rootJob = root.start(this)
        val rootScopeJob = SupervisorJob(coroutineContext[Job])
        val rootScope = CoroutineScope(coroutineContext + rootScopeJob)
        val unbind = owner.bind(root, rootScope)
        val timerScheduler = TestCoroutineScheduler()
        val heldDispatcher = HeldNavigationDispatcher(StandardTestDispatcher(timerScheduler))
        val heldScopeJob = SupervisorJob(coroutineContext[Job])
        val heldScope = CoroutineScope(heldScopeJob + heldDispatcher)
        var leafStartJob: Job? = null
        try {
            val ancestor = checkNotNull(root.push(UsersListViewConfig()))
            val leaf = ancestor.createEmptySubChain()
            val pending = checkNotNull(leaf.push(PasswordChangeViewConfig.Pending(UserId(7), approvalId)))
                as NavigationNode<PasswordChangeViewConfig, ViewConfig>
            advanceUntilIdle()

            leafStartJob = leaf.start(heldScope)
            heldDispatcher.drain()
            owner.onChanged(pending)
            runCurrent()
            assertEquals(1, owner.activeTransitionCount)
            assertTrue(heldDispatcher.queuedTaskCount > 0)
            val leafStackBeforeOuterReplacement = leaf.stackFlow.value

            val outerReplacement = checkNotNull(root.replace(ancestor, UsersListViewConfig())).second
            runCurrent()

            assertTrue(rootScopeJob.children.all { it.isCompleted })
            assertEquals(0, owner.activeTransitionCount)
            assertEquals(0, repo.saveAttempts)
            assertSame(outerReplacement, root.stackFlow.value.single())
            assertEquals(leafStackBeforeOuterReplacement, leaf.stackFlow.value)
        } finally {
            unbind()
            rootScopeJob.cancelAndJoin()
            leafStartJob?.cancel()
            heldScopeJob.cancel()
            heldDispatcher.drain()
            leafStartJob?.join()
            heldScopeJob.join()
            rootJob.cancelAndJoin()
        }
    }

    /** Rejects an owner transition whose queued Pending replacement loses to an earlier exact replacement. */
    @Test
    fun queuedNodeSupersessionNeverSavesCompleted() = runTest {
        val repo = RecordingPasswordNavigationRepo()
        val owner = PasswordChangeNavigationOwner(repo)
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

            owner.onChanged(pending)
            val competingReplacement = checkNotNull(chain.replace(pending, UsersListViewConfig())).second
            advanceUntilIdle()

            assertSame(competingReplacement, chain.stackFlow.value.last())
            assertEquals(0, repo.saveAttempts)
            assertEquals(0, owner.activeTransitionCount)
            assertFalse(chain.stackFlow.value.any { it.config == PasswordChangeViewConfig.Completed })
        } finally {
            unbind()
            chainJob.cancelAndJoin()
        }
    }

    /** Rejects a completed replacement that loses current-root membership before root persistence resumes. */
    @Test
    fun replacementSupersededBeforeSaveIsIgnored() = runTest {
        val repo = RecordingPasswordNavigationRepo()
        val owner = PasswordChangeNavigationOwner(repo)
        val chain = NavigationChain<ViewConfig>(null, NavigationNodeFactory { parent, config ->
            NavigationNode.Empty(parent, config)
        })
        val chainJob = chain.start(this)
        val timerScheduler = TestCoroutineScheduler()
        val heldDispatcher = HeldNavigationDispatcher(StandardTestDispatcher(timerScheduler))
        val rootJob = SupervisorJob(coroutineContext[Job])
        val unbind = owner.bind(chain, CoroutineScope(rootJob + heldDispatcher))
        try {
            chain.push(UsersListViewConfig())
            val pending = checkNotNull(chain.push(PasswordChangeViewConfig.Pending(UserId(7), approvalId)))
                as NavigationNode<PasswordChangeViewConfig, ViewConfig>
            advanceUntilIdle()

            owner.onChanged(pending)
            assertTrue(heldDispatcher.runNext())
            runCurrent()
            val completed = chain.stackFlow.value.last()
            assertEquals(PasswordChangeViewConfig.Completed, completed.config)
            val newer = checkNotNull(chain.replace(completed, UsersListViewConfig())).second
            runCurrent()
            heldDispatcher.drain()

            assertSame(newer, chain.stackFlow.value.last())
            assertEquals(0, repo.saveAttempts)
            assertEquals(0, owner.activeTransitionCount)
        } finally {
            unbind()
            rootJob.cancel()
            heldDispatcher.drain()
            rootJob.join()
            chainJob.cancelAndJoin()
        }
    }

    /** Persists a newer destination above Completed when the exact replacement remains reachable. */
    @Test
    fun newerDestinationAboveCompletedIsPreservedInSavedHierarchy() = runTest {
        val repo = RecordingPasswordNavigationRepo()
        val owner = PasswordChangeNavigationOwner(repo)
        val chain = NavigationChain<ViewConfig>(null, NavigationNodeFactory { parent, config ->
            NavigationNode.Empty(parent, config)
        })
        val chainJob = chain.start(this)
        val timerScheduler = TestCoroutineScheduler()
        val heldDispatcher = HeldNavigationDispatcher(StandardTestDispatcher(timerScheduler))
        val rootJob = SupervisorJob(coroutineContext[Job])
        val unbind = owner.bind(chain, CoroutineScope(rootJob + heldDispatcher))
        try {
            chain.push(UsersListViewConfig())
            val pending = checkNotNull(chain.push(PasswordChangeViewConfig.Pending(UserId(7), approvalId)))
                as NavigationNode<PasswordChangeViewConfig, ViewConfig>
            advanceUntilIdle()

            owner.onChanged(pending)
            assertTrue(heldDispatcher.runNext())
            runCurrent()
            assertEquals(PasswordChangeViewConfig.Completed, chain.stackFlow.value.last().config)
            val newer = checkNotNull(chain.push(UserViewConfig(UserId(8))))
            runCurrent()
            heldDispatcher.drain()

            assertSame(newer, chain.stackFlow.value.last())
            assertEquals(1, repo.saveAttempts)
            assertEquals(0, owner.activeTransitionCount)
            val saved = repo.holders.single() as ConfigHolder.Chain<ViewConfig>
            val users = checkNotNull(saved.firstNodeConfig)
            val completed = checkNotNull(users.subnode)
            val savedNewer = checkNotNull(completed.subnode)
            assertTrue(users.config is UsersListViewConfig)
            assertEquals(PasswordChangeViewConfig.Completed, completed.config)
            assertEquals(UserViewConfig(UserId(8)), savedNewer.config)
            assertEquals(null, savedNewer.subnode)
        } finally {
            unbind()
            rootJob.cancel()
            heldDispatcher.drain()
            rootJob.join()
            chainJob.cancelAndJoin()
        }
    }

    /** Keeps a replacement binding alive while stale cleanup cancels only former-root transitions. */
    @Test
    fun oldBindingCleanupCannotClearNewBinding() = runTest {
        val repo = RecordingPasswordNavigationRepo()
        val owner = PasswordChangeNavigationOwner(repo)
        val factory = NavigationNodeFactory<ViewConfig> { parent, config -> NavigationNode.Empty(parent, config) }
        val rootA = NavigationChain<ViewConfig>(null, factory)
        val rootB = NavigationChain<ViewConfig>(null, factory)
        val rootAJob = rootA.start(this)
        val rootBJob = rootB.start(this)
        val scopeAJob = SupervisorJob(coroutineContext[Job])
        val scopeBJob = SupervisorJob(coroutineContext[Job])
        val timerScheduler = TestCoroutineScheduler()
        val heldDispatcher = HeldNavigationDispatcher(StandardTestDispatcher(timerScheduler))
        val unbindA = owner.bind(rootA, CoroutineScope(scopeAJob + heldDispatcher))
        try {
            rootA.push(UsersListViewConfig())
            val pendingA = checkNotNull(rootA.push(PasswordChangeViewConfig.Pending(UserId(7), approvalId)))
                as NavigationNode<PasswordChangeViewConfig, ViewConfig>
            advanceUntilIdle()
            owner.onChanged(pendingA)
            assertTrue(heldDispatcher.runNext())
            runCurrent()
            assertEquals(1, owner.activeTransitionCount)

            val unbindB = owner.bind(rootB, CoroutineScope(coroutineContext + scopeBJob))
            try {
                unbindA()
                advanceUntilIdle()
                assertEquals(0, owner.activeTransitionCount)
                owner.onContinue(pendingA)
                runCurrent()
                assertEquals(0, repo.saveAttempts)

                rootB.push(UsersListViewConfig())
                val pendingB = checkNotNull(rootB.push(PasswordChangeViewConfig.Pending(UserId(8), approvalId)))
                    as NavigationNode<PasswordChangeViewConfig, ViewConfig>
                advanceUntilIdle()
                owner.onChanged(pendingB)
                advanceUntilIdle()
                assertEquals(1, repo.saveAttempts)
                assertEquals(PasswordChangeViewConfig.Completed, rootB.stackFlow.value.last().config)

                val inactiveScopeJob = SupervisorJob(coroutineContext[Job]).apply { cancel() }
                owner.bind(rootB, CoroutineScope(coroutineContext + inactiveScopeJob))
                owner.onContinue(rootB.stackFlow.value.last() as NavigationNode<PasswordChangeViewConfig, ViewConfig>)
                runCurrent()
                assertEquals(0, owner.activeTransitionCount)
                inactiveScopeJob.join()
            } finally {
                unbindB()
            }
        } finally {
            unbindA()
            scopeAJob.cancel()
            heldDispatcher.drain()
            scopeAJob.join()
            scopeBJob.cancelAndJoin()
            rootAJob.cancelAndJoin()
            rootBJob.cancelAndJoin()
        }
    }

    /** Proves replacement destroys the submitting ViewModel before the root-owned save completes. */
    @Test
    fun actualSubmittingViewModelDestructionPrecedesCompletedPersistence() = runTest {
        val navigationConfigsRepo = RecordingPasswordNavigationRepo()
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
            val rootTimerScheduler = TestCoroutineScheduler()
            val heldRootDispatcher = HeldNavigationDispatcher(StandardTestDispatcher(rootTimerScheduler))
            val rootScopeJob = SupervisorJob(coroutineContext[Job])
            val rootScope = CoroutineScope(rootScopeJob + heldRootDispatcher)
            val unbind = owner.bind(chain, rootScope)
            var destroyObserver: Job? = null
            try {
                val model = HeldPasswordChangeUsersModel()
                chain.push(UsersListViewConfig())
                val pending = PasswordChangeViewConfig.Pending(UserId(7), approvalId)
                val node = checkNotNull(chain.push(pending)) as NavigationNode<PasswordChangeViewConfig, ViewConfig>
                val viewModel = PasswordChangeViewModel(node, model, interactor, StandardTestDispatcher(testScheduler))
                val viewModelJob = checkNotNull(viewModel.scope.coroutineContext[Job])
                val viewModelLifecycleSubscribed = CompletableDeferred<Unit>()
                viewModel.scope.launch { viewModelLifecycleSubscribed.complete(Unit) }
                withContext(Dispatchers.Default) {
                    withTimeout(5_000L) { viewModelLifecycleSubscribed.await() }
                }
                val destroyObserved = CompletableDeferred<Unit>()
                destroyObserver = launch(start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) {
                    node.onDestroyFlow.first()
                    destroyObserved.complete(Unit)
                }
                runCurrent()
                viewModel.onPasswordChanged("replacement-password")
                viewModel.onConfirmationChanged("replacement-password")
                viewModel.onSubmitPasswordChange()
                runCurrent()
                model.requestReceived.await()
                val request = model.requests.single()
                assertEquals(UserId(7), request.userId)
                assertEquals(approvalId, request.approvalId)
                assertEquals("replacement-password", request.password.string)
                model.completion.complete(PasswordChangeResult.Changed)
                runCurrent()
                assertTrue(heldRootDispatcher.runNext())
                advanceUntilIdle()
                assertEquals(PasswordChangeViewConfig.Completed, chain.stackFlow.value.last().config)
                withContext(Dispatchers.Default) {
                    withTimeout(5_000L) {
                        while (!destroyObserved.isCompleted || !viewModelJob.isCompleted) {
                            testScheduler.runCurrent()
                            yield()
                        }
                        destroyObserved.await()
                        viewModelJob.join()
                    }
                }
                assertTrue(viewModelJob.isCancelled)
                assertTrue(viewModelJob.isCompleted)
                assertTrue(viewModel.passwordState.value.isEmpty())
                assertTrue(viewModel.confirmationState.value.isEmpty())
                assertEquals(dev.inmo.navigation.core.NavigationNodeState.NEW, node.state)
                assertEquals(0, navigationConfigsRepo.saveAttempts)
                val transitionJob = rootScopeJob.children.single()
                heldRootDispatcher.drain()
                runCurrent()
                transitionJob.join()
                assertEquals(1, navigationConfigsRepo.saveAttempts)
                val saved = navigationConfigsRepo.holders.single() as ConfigHolder.Chain<ViewConfig>
                val users = assertNotNull(saved.firstNodeConfig)
                val completed = assertNotNull(users.subnode)
                assertTrue(users.config is UsersListViewConfig)
                assertEquals(PasswordChangeViewConfig.Completed, completed.config)
                assertEquals(null, completed.subnode)
                assertTrue(users.subchains.isEmpty())
                assertTrue(completed.subchains.isEmpty())
                assertFalse(saved.passwordNavigationConfigs().any { it is PasswordChangeViewConfig.Pending })
                val serialized = serializedPasswordNavigationHolder(application.koin.get(), saved)
                assertFalse(serialized.contains(approvalId.string))
                assertFalse(serialized.contains("replacement-password"))

                val completedNode = chain.stackFlow.value.last() as NavigationNode<PasswordChangeViewConfig, ViewConfig>
                val completedViewModel = PasswordChangeViewModel(
                    completedNode,
                    model,
                    interactor,
                    StandardTestDispatcher(testScheduler),
                )
                val completedViewModelJob = checkNotNull(completedViewModel.scope.coroutineContext[Job])
                val completedViewModelLifecycleSubscribed = CompletableDeferred<Unit>()
                completedViewModel.scope.launch { completedViewModelLifecycleSubscribed.complete(Unit) }
                withContext(Dispatchers.Default) {
                    withTimeout(5_000L) { completedViewModelLifecycleSubscribed.await() }
                }
                completedViewModel.onSubmitPasswordChange()
                runCurrent()
                assertEquals(1, model.requests.size)
                completedViewModel.onContinue()
                runCurrent()
                assertTrue(heldRootDispatcher.runNext())
                advanceUntilIdle()
                withContext(Dispatchers.Default) {
                    withTimeout(5_000L) {
                        while (!completedViewModelJob.isCompleted) {
                            testScheduler.runCurrent()
                            yield()
                        }
                        completedViewModelJob.join()
                    }
                }
                heldRootDispatcher.drain()
                runCurrent()
                assertTrue(chain.stackFlow.value.last().config is UsersListViewConfig)
                assertEquals(2, navigationConfigsRepo.saveAttempts)
                val usersListSaved = navigationConfigsRepo.holders.last()
                assertTrue(usersListSaved.passwordNavigationConfigs().any { it is UsersListViewConfig })
                assertFalse(usersListSaved.passwordNavigationConfigs().any { it is PasswordChangeViewConfig })
                assertEquals(0, owner.activeTransitionCount)
            } finally {
                unbind()
                destroyObserver?.cancelAndJoin()
                rootScopeJob.cancel()
                heldRootDispatcher.drain()
                rootScopeJob.join()
                chainJob.cancelAndJoin()
            }
        } finally {
            stopKoin()
        }
    }

    /** Proves Continue persists a users-list destination after completion. */
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
    /** Proves a stale Pending node cannot replace a newer destination. */
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
    /** Proves cancellation during submission cannot start a changed handoff. */
    @Test
    fun cancelledSubmittingViewModelCannotStartChangedHandoff() = runTest {
        val repo = RecordingPasswordNavigationRepo()
        val owner = PasswordChangeNavigationOwner(repo)
        val completedNodes = mutableListOf<NavigationNode<out ViewConfig, ViewConfig>>()
        val chain = NavigationChain<ViewConfig>(null, NavigationNodeFactory { parent, config ->
            NavigationNode.Empty(parent, config).also {
                if (config == PasswordChangeViewConfig.Completed) completedNodes += it
            }
        })
        val chainJob = chain.start(this)
        val unbind = owner.bind(chain, this)
        var destroyObserver: Job? = null
        try {
            val model = HeldPasswordChangeUsersModel().apply { returnsChangedAfterCancellation = true }
            chain.push(UsersListViewConfig())
            val pending = checkNotNull(chain.push(PasswordChangeViewConfig.Pending(UserId(7), approvalId)))
                as NavigationNode<PasswordChangeViewConfig, ViewConfig>
            advanceUntilIdle()
            val viewModel = PasswordChangeViewModel(pending, model, owner, StandardTestDispatcher(testScheduler))
            val viewModelJob = checkNotNull(viewModel.scope.coroutineContext[Job])
            val viewModelLifecycleSubscribed = CompletableDeferred<Unit>()
            viewModel.scope.launch { viewModelLifecycleSubscribed.complete(Unit) }
            withContext(Dispatchers.Default) {
                withTimeout(5_000L) { viewModelLifecycleSubscribed.await() }
            }
            val destroyObserved = CompletableDeferred<Unit>()
            destroyObserver = launch(start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) {
                pending.onDestroyFlow.first()
                destroyObserved.complete(Unit)
            }
            viewModel.onPasswordChanged("cancellation-resistant")
            viewModel.onConfirmationChanged("cancellation-resistant")
            viewModel.onSubmitPasswordChange()
            runCurrent()
            model.requestReceived.await()
            val competing = checkNotNull(chain.replace(pending, UsersListViewConfig())).second
            advanceUntilIdle()
            withContext(Dispatchers.Default) {
                withTimeout(5_000L) {
                    while (!destroyObserved.isCompleted || !model.returnedAfterCancellation.isCompleted || !viewModelJob.isCompleted) {
                        testScheduler.runCurrent()
                        yield()
                    }
                    destroyObserved.await()
                    model.returnedAfterCancellation.await()
                    viewModelJob.join()
                }
            }
            assertEquals(dev.inmo.navigation.core.NavigationNodeState.NEW, pending.state)
            assertEquals(1, model.requests.size)
            assertTrue(viewModel.passwordState.value.isEmpty())
            assertTrue(viewModel.confirmationState.value.isEmpty())
            assertEquals(0, completedNodes.size)
            assertEquals(0, repo.saveAttempts)
            assertSame(competing, chain.stackFlow.value.last())
            assertEquals(0, owner.activeTransitionCount)
        } finally {
            unbind()
            destroyObserver?.cancelAndJoin()
            chainJob.cancelAndJoin()
        }
    }

    /** A root binding disposal cancels a queued replacement before a stopped chain can publish it. */
    /** Proves root binding disposal cancels and removes owned transitions. */
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
    /** Proves unobserved replacement times out and cleans owner jobs. */
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
    /** Proves save failure is not replayed and later Continue remains usable. */
    @Test
    fun saveFailureDoesNotReplayAndLaterContinuePersistsUsersList() = runTest {
        val application = startKoin {
            modules(module {
                with(dev.inmo.wishlist.features.common.common.Plugin) { setupDI(JsonObject(emptyMap())) }
                with(dev.inmo.wishlist.features.ui.users.Plugin) { setupDI(JsonObject(emptyMap())) }
            })
        }
        val json = application.koin.get<Json>()
        val transport = HeldPasswordChangeTransport(json)
        val repo = RecordingPasswordNavigationRepo(failFirstSave = true)
        val owner = PasswordChangeNavigationOwner(repo)
        val chain = NavigationChain<ViewConfig>(null, NavigationNodeFactory { parent, config -> NavigationNode.Empty(parent, config) })
        val chainJob = chain.start(this)
        val unbind = owner.bind(chain, this)
        var pendingDestroyObserver: Job? = null
        var completedDestroyObserver: Job? = null
        try {
            val model = HeldPasswordChangeUsersModel(transport.feature)
            chain.push(UsersListViewConfig())
            val pending = checkNotNull(chain.push(PasswordChangeViewConfig.Pending(UserId(7), approvalId)))
                as NavigationNode<PasswordChangeViewConfig, ViewConfig>
            runCurrent()
            val pendingViewModel = PasswordChangeViewModel(pending, model, owner, Dispatchers.Default)
            val pendingViewModelJob = checkNotNull(pendingViewModel.scope.coroutineContext[Job])
            val pendingDestroyed = CompletableDeferred<Unit>()
            pendingDestroyObserver = launch(start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) {
                pending.onDestroyFlow.first()
                pendingDestroyed.complete(Unit)
            }
            pendingViewModel.onPasswordChanged("transport-password")
            pendingViewModel.onConfirmationChanged("transport-password")
            pendingViewModel.onSubmitPasswordChange()
            runCurrent()
            model.requestReceived.await()
            transport.requestReceived.await()
            assertEquals(1, model.requests.size)
            assertEquals(1, transport.requests.size)
            assertEquals(model.requests.single(), transport.requests.single())
            assertEquals("https://wishlist.test/api/auth/completePasswordChange", transport.httpRequest?.url.toString())
            transport.releaseChanged()
            withContext(Dispatchers.Default) {
                withTimeout(5_000L) { model.completionReturned.await() }
            }
            advanceUntilIdle()
            withContext(Dispatchers.Default) {
                withTimeout(5_000L) {
                    pendingDestroyed.await()
                    pendingViewModelJob.join()
                }
            }
            runCurrent()
            assertEquals(1, repo.saveAttempts)
            val completed = chain.stackFlow.value.last() as NavigationNode<PasswordChangeViewConfig, ViewConfig>
            assertEquals(PasswordChangeViewConfig.Completed, completed.config)
            assertTrue(pendingViewModel.passwordState.value.isEmpty())
            assertTrue(pendingViewModel.confirmationState.value.isEmpty())
            assertEquals(0, repo.holders.size)
            assertEquals(0, owner.activeTransitionCount)
            pendingViewModel.onSubmitPasswordChange()
            runCurrent()
            val completedViewModel = PasswordChangeViewModel(completed, model, owner, Dispatchers.Default)
            val completedViewModelJob = checkNotNull(completedViewModel.scope.coroutineContext[Job])
            val completedDestroyed = CompletableDeferred<Unit>()
            completedDestroyObserver = launch(start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) {
                completed.onDestroyFlow.first()
                completedDestroyed.complete(Unit)
            }
            completedViewModel.onSubmitPasswordChange()
            runCurrent()
            completedViewModel.onContinue()
            advanceUntilIdle()
            withContext(Dispatchers.Default) {
                withTimeout(5_000L) {
                    completedDestroyed.await()
                    completedViewModelJob.join()
                }
            }
            runCurrent()
            assertEquals(2, repo.saveAttempts)
            assertEquals(1, repo.holders.size)
            assertTrue(repo.holders.single().passwordNavigationConfigs().any { it is UsersListViewConfig })
            assertTrue(chain.stackFlow.value.last().config is UsersListViewConfig)
            assertEquals(1, model.requests.size)
            assertEquals(1, transport.requests.size)
            assertEquals(0, owner.activeTransitionCount)
        } finally {
            unbind()
            pendingDestroyObserver?.cancelAndJoin()
            completedDestroyObserver?.cancelAndJoin()
            chainJob.cancelAndJoin()
            transport.close()
            stopKoin()
        }
    }
}
