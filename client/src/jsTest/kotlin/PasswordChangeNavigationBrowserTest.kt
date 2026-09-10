package dev.inmo.wishlist.client

import androidx.compose.runtime.Composition
import androidx.compose.runtime.SideEffect
import dev.inmo.navigation.compose.initNavigation
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.core.NavigationChainId
import dev.inmo.navigation.core.NavigationNode
import dev.inmo.navigation.core.NavigationNodeFactory
import dev.inmo.navigation.core.findNodeInSubTree
import dev.inmo.navigation.core.onDestroyFlow
import dev.inmo.navigation.core.repo.ConfigHolder
import dev.inmo.navigation.core.repo.NavigationConfigsRepo
import dev.inmo.navigation.core.repo.storeHierarchy
import dev.inmo.wishlist.client.utils.WithPasswordChangeNavigationBinding
import dev.inmo.wishlist.features.common.client.models.EmptyConfig
import dev.inmo.wishlist.features.common.client.models.LeftNavigationChainId
import dev.inmo.wishlist.features.common.client.models.MainNavigationChainId
import dev.inmo.wishlist.features.common.client.models.TopNavigationChainId
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.deeplinks.common.models.DeepLinkId
import dev.inmo.wishlist.features.ui.scaffold.ui.ScaffoldViewConfig
import dev.inmo.wishlist.features.ui.sidebar.ui.SidebarViewConfig
import dev.inmo.wishlist.features.ui.topBar.ui.TopBarViewConfig
import dev.inmo.wishlist.features.ui.users.ui.PasswordChangeViewConfig
import dev.inmo.wishlist.features.ui.users.ui.PasswordChangeViewInteractor
import dev.inmo.wishlist.features.ui.users.ui.PasswordChangeViewModel
import dev.inmo.wishlist.features.ui.users.ui.UsersListViewConfig
import dev.inmo.wishlist.features.ui.wishlist.ui.WishlistItemViewConfig
import dev.inmo.wishlist.features.ui.wishlist.ui.WishlistViewConfig
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.wishlist.common.models.WishlistId
import dev.inmo.wishlist.features.wishlist.common.models.WishlistItemId
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.children
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.promise
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.yield
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.jetbrains.compose.web.renderComposable
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import org.w3c.dom.HTMLBaseElement
import org.w3c.dom.HTMLDivElement
import kotlin.coroutines.coroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/** Returns the browser global object used by the JSDOM harness. */
private fun browserGlobal(): dynamic = js("globalThis")

/** Creates an isolated JSDOM window at [url] for URL and navigation assertions. */
private fun browserDom(url: String): dynamic =
    js("new (require('jsdom').JSDOM)('<!doctype html><html><head></head><body></body></html>', { url: url, pretendToBeVisual: true })")

/** Global browser properties captured and restored around the Compose/JSDOM production mount. */
private val passwordNavigationGlobalKeys = listOf(
    "window", "document", "Node", "Element", "HTMLElement", "HTMLDivElement", "HTMLBaseElement",
    "Text", "Comment", "DocumentFragment", "Event", "requestAnimationFrame", "cancelAnimationFrame",
)

/** Captures property descriptors before installing the isolated JSDOM window. */
private fun capturePasswordNavigationGlobals(): dynamic {
    val objectApi: dynamic = js("Object")
    val global = browserGlobal()
    val snapshot: dynamic = js("({})")
    passwordNavigationGlobalKeys.forEach { key -> snapshot[key] = objectApi.getOwnPropertyDescriptor(global, key) }
    return snapshot
}

/** Installs JSDOM window, document, constructors, and animation APIs for Compose HTML. */
private fun installPasswordNavigationGlobals(dom: dynamic) {
    val global = browserGlobal()
    global.window = dom.window
    global.document = dom.window.document
    global.Node = dom.window.Node
    global.Element = dom.window.Element
    global.HTMLElement = dom.window.HTMLElement
    global.HTMLDivElement = dom.window.HTMLDivElement
    global.HTMLBaseElement = dom.window.HTMLBaseElement
    global.Text = dom.window.Text
    global.Comment = dom.window.Comment
    global.DocumentFragment = dom.window.DocumentFragment
    global.Event = dom.window.Event
    global.requestAnimationFrame = dom.window.requestAnimationFrame
    global.cancelAnimationFrame = dom.window.cancelAnimationFrame
}

/** Restores every captured global descriptor after production composition cleanup. */
private fun restorePasswordNavigationGlobals(snapshot: dynamic) {
    val objectApi: dynamic = js("Object")
    val reflectApi: dynamic = js("Reflect")
    val global = browserGlobal()
    passwordNavigationGlobalKeys.forEach { key ->
        val descriptor = snapshot[key]
        if (descriptor == null) {
            reflectApi.deleteProperty(global, key)
        } else {
            objectApi.defineProperty(global, key, descriptor)
        }
    }
}

/** Traverses persisted navigation holders for typed password-route assertions. */
private fun ConfigHolder<ViewConfig>.allConfigs(): List<ViewConfig> = when (this) {
    is ConfigHolder.Chain -> firstNodeConfig?.allConfigs().orEmpty()
    is ConfigHolder.Node -> listOf(config) + subnode?.allConfigs().orEmpty() + subchains.flatMap { it.allConfigs() }
}

/** Waits for the first non-empty navigation stack without sleeping. */
private suspend fun NavigationChain<ViewConfig>.awaitStack(): List<NavigationNode<out ViewConfig, ViewConfig>> =
    stackFlow.value.takeIf { it.isNotEmpty() } ?: stackFlow.filter { it.isNotEmpty() }.first()

/** Runs one bounded asynchronous browser phase and reports the phase label on timeout. */
private suspend fun awaitBrowserPhase(name: String, block: suspend () -> Unit) {
    check(withTimeoutOrNull(5_000L) {
        block()
        true
    } == true) { "Browser password-change phase did not complete: $name" }
}

/** Locates the restored main chain beneath the production root and scaffold chains. */
private suspend fun restoredMainChain(root: NavigationChain<ViewConfig>): NavigationChain<ViewConfig> {
    val rootNode = root.awaitStack().single()
    val scaffold = rootNode.subchains.single().awaitStack().single()
    return checkNotNull(scaffold.subchains.firstOrNull { it.id == MainNavigationChainId })
}

/** Mounts production root binding and initNavigation into the supplied Compose HTML host. */
private fun mountPasswordNavigation(
    host: HTMLDivElement,
    owner: PasswordChangeNavigationOwner,
    root: NavigationChain<ViewConfig>,
    configsRepo: NavigationConfigsRepo<ViewConfig>,
    scopeCaptured: CompletableDeferred<CoroutineScope>,
    nodesFactory: NavigationNodeFactory<ViewConfig>,
): Composition = renderComposable(host) {
    WithPasswordChangeNavigationBinding(owner, root) { rootScope ->
        initNavigation(
            rootNodeConfig = EmptyConfig(),
            configsRepo = configsRepo,
            nodesFactory = nodesFactory,
            scope = rootScope,
            dropRedundantChainsOnRestore = true,
            rootChain = root,
        ) {}
        SideEffect { scopeCaptured.complete(rootScope) }
    }
}

/** Exercises production browser URL restoration, completion transport, persistence, and rebinding. */
class PasswordChangeNavigationBrowserTest {
    @Test
    fun canonicalApprovalReloadsAndCompletionPersistsWithoutCredential() = CoroutineScope(Dispatchers.Unconfined).promise(
        start = CoroutineStart.UNDISPATCHED,
    ) {
        val globals = capturePasswordNavigationGlobals()
        val dom = browserDom("https://wishlist.test/ui/password-change/7/123e4567-e89b-42d3-a456-426614174000")
        val previousProvider = ClientPlugin.mainScaffoldConfigProvider
        var compositionA: Composition? = null
        var compositionB: Composition? = null
        var scopeAJob: Job? = null
        var scopeBJob: Job? = null
        var transitionAJob: Job? = null
        var transitionBJob: Job? = null
        var leafStartAJob: Job? = null
        var leafStartBJob: Job? = null
        var heldScopeAJob: Job? = null
        var heldScopeBJob: Job? = null
        var heldDispatcherA: HeldNavigationDispatcher? = null
        var heldDispatcherB: HeldNavigationDispatcher? = null
        var pendingViewModelJob: Job? = null
        var completedViewModelJob: Job? = null
        var pendingDestroyObserver: Job? = null
        var completedDestroyObserver: Job? = null
        var application: org.koin.core.KoinApplication? = null
        var transport: HeldPasswordChangeTransport? = null
        var hostA: HTMLDivElement? = null
        var hostB: HTMLDivElement? = null
        try {
            installPasswordNavigationGlobals(dom)
            hostA = document.createElement("div") as HTMLDivElement
            document.body!!.appendChild(hostA)
            val base = document.createElement("base") as HTMLBaseElement
            base.href = "/ui/"
            document.head!!.appendChild(base)
            ClientPlugin.mainScaffoldConfigProvider = {
                ScaffoldViewConfig(TopBarViewConfig(), SidebarViewConfig(), dev.inmo.wishlist.features.ui.wishlist.ui.WishlistsListViewConfig())
            }
            val approval = DeepLinkId("123e4567-e89b-42d3-a456-426614174000")
            val urlRepo = WishlistsAppUrlNavigationConfigsRepo()
            val restored = assertNotNull(urlRepo.get())
            val restoredConfigs = restored.allConfigs()
            assertTrue(restoredConfigs.first() is EmptyConfig)
            assertEquals(PasswordChangeViewConfig.Pending(UserId(7), approval), restoredConfigs.filterIsInstance<PasswordChangeViewConfig.Pending>().single())
            val scaffold = ((restored as ConfigHolder.Chain<ViewConfig>).firstNodeConfig as ConfigHolder.Node<ViewConfig>).subchains.single().firstNodeConfig as ConfigHolder.Node<ViewConfig>
            val ids = scaffold.subchains.mapNotNull { it.id }
            assertTrue(TopNavigationChainId in ids)
            assertTrue(LeftNavigationChainId in ids)
            assertTrue(MainNavigationChainId in ids)
            urlRepo.save(restored)
            assertEquals("/ui/password-change/7/123e4567-e89b-42d3-a456-426614174000", window.location.pathname)

            val rootAId = NavigationChainId("password-change-browser-root-A")
            val rootBId = NavigationChainId("password-change-browser-root-B")
            val completedSave = CompletableDeferred<ConfigHolder<ViewConfig>>()
            val usersListSave = CompletableDeferred<ConfigHolder<ViewConfig>>()
            val completedSaveB = CompletableDeferred<ConfigHolder<ViewConfig>>()
            val recordedSavesA = RecordingPasswordNavigationRepo()
            val recordedSavesB = RecordingPasswordNavigationRepo()
            var ownerSaveAttemptsA = 0
            var ownerSaveAttemptsB = 0
            val persistenceRepo = object : NavigationConfigsRepo<ViewConfig> {
                /** Delegates owner snapshots by exact root id while preserving real URL persistence. */
                override fun save(holder: ConfigHolder<ViewConfig>) {
                    val rootHolder = holder as? ConfigHolder.Chain<ViewConfig>
                    when (rootHolder?.id) {
                        rootAId -> {
                            ownerSaveAttemptsA += 1
                            urlRepo.save(holder)
                            recordedSavesA.save(holder)
                            when {
                                holder.allConfigs().any { it is PasswordChangeViewConfig.Completed } &&
                                    holder.allConfigs().none { it is PasswordChangeViewConfig.Pending } &&
                                    !completedSave.isCompleted -> completedSave.complete(recordedSavesA.holders.last())
                                holder.allConfigs().any { it is UsersListViewConfig } &&
                                    holder.allConfigs().none { it is PasswordChangeViewConfig } &&
                                    !usersListSave.isCompleted -> usersListSave.complete(recordedSavesA.holders.last())
                            }
                        }
                        rootBId -> {
                            ownerSaveAttemptsB += 1
                            urlRepo.save(holder)
                            recordedSavesB.save(holder)
                            if (
                                holder.allConfigs().any { it is PasswordChangeViewConfig.Completed } &&
                                holder.allConfigs().none { it is PasswordChangeViewConfig.Pending } &&
                                !completedSaveB.isCompleted
                            ) {
                                completedSaveB.complete(recordedSavesB.holders.last())
                            }
                        }
                        else -> error("Unexpected password-navigation owner root id: ${rootHolder?.id}")
                    }
                }

                /** Restores navigation from the current browser URL through the production adapter. */
                override fun get(): ConfigHolder<ViewConfig>? = urlRepo.get()
            }
            application = koinApplication {
                modules(module {
                    with(dev.inmo.wishlist.features.common.common.Plugin) { setupDI(JsonObject(emptyMap())) }
                    with(dev.inmo.wishlist.features.common.client.Plugin) { setupDI(JsonObject(emptyMap())) }
                    with(dev.inmo.wishlist.features.ui.users.Plugin) { setupDI(JsonObject(emptyMap())) }
                    with(dev.inmo.wishlist.features.ui.scaffold.Plugin) { setupDI(JsonObject(emptyMap())) }
                    with(dev.inmo.wishlist.features.ui.topBar.Plugin) { setupDI(JsonObject(emptyMap())) }
                    with(dev.inmo.wishlist.features.ui.sidebar.Plugin) { setupDI(JsonObject(emptyMap())) }
                    with(dev.inmo.wishlist.features.ui.wishlist.Plugin) { setupDI(JsonObject(emptyMap())) }
                    with(ClientPlugin) { setupDI(JsonObject(emptyMap())) }
                    single<NavigationConfigsRepo<ViewConfig>> { persistenceRepo }
                })
            }
            val json = application.koin.get<Json>()
            transport = HeldPasswordChangeTransport(json)
            val model = HeldPasswordChangeUsersModel(transport.feature)
            val owner = application.koin.get<PasswordChangeNavigationOwner>()
            val interactor = application.koin.get<PasswordChangeViewInteractor>()
            val replacementCreatedA = CompletableDeferred<NavigationNode<out ViewConfig, ViewConfig>>()
            val replacementCreatedB = CompletableDeferred<NavigationNode<out ViewConfig, ViewConfig>>()
            var replacementAArmed = false
            var replacementBArmed = false
            val nodesFactoryA = NavigationNodeFactory<ViewConfig> { chain, config ->
                NavigationNode.Empty(chain, config).also { node ->
                    if (
                        replacementAArmed &&
                        config == PasswordChangeViewConfig.Completed &&
                        !replacementCreatedA.isCompleted
                    ) {
                        replacementCreatedA.complete(node)
                    }
                }
            }
            val nodesFactoryB = NavigationNodeFactory<ViewConfig> { chain, config ->
                NavigationNode.Empty(chain, config).also { node ->
                    if (
                        replacementBArmed &&
                        config == PasswordChangeViewConfig.Completed &&
                        !replacementCreatedB.isCompleted
                    ) {
                        replacementCreatedB.complete(node)
                    }
                }
            }
            val rootA = NavigationChain<ViewConfig>(rootAId, nodesFactoryA)
            val scopeA = CompletableDeferred<CoroutineScope>()
            compositionA = mountPasswordNavigation(hostA, owner, rootA, urlRepo, scopeA, nodesFactoryA)
            awaitBrowserPhase("composition-A scope") { scopeA.await() }
            scopeAJob = checkNotNull(scopeA.await().coroutineContext[Job])
            lateinit var mainA: NavigationChain<ViewConfig>
            awaitBrowserPhase("restored main chain") { mainA = restoredMainChain(rootA) }
            lateinit var pendingNode: NavigationNode<PasswordChangeViewConfig, ViewConfig>
            awaitBrowserPhase("restored pending node") {
                pendingNode = mainA.stackFlow.filter { stack ->
                    stack.lastOrNull()?.config is PasswordChangeViewConfig.Pending
                }.first().last() as NavigationNode<PasswordChangeViewConfig, ViewConfig>
            }
            val pendingViewModel = PasswordChangeViewModel(pendingNode, model, interactor, Dispatchers.Unconfined)
            pendingViewModelJob = checkNotNull(pendingViewModel.scope.coroutineContext[Job])
            assertEquals(PasswordChangeViewConfig.Pending(UserId(7), approval), pendingViewModel.config)
            assertFalse(pendingViewModel.completedState)
            assertFalse(pendingViewModel.loadingState.value)
            assertFalse(pendingViewModel.terminalInvalidApprovalState.value)
            val pendingViewModelLifecycleSubscribed = CompletableDeferred<Unit>()
            pendingViewModel.scope.launch { pendingViewModelLifecycleSubscribed.complete(Unit) }
            awaitBrowserPhase("pending ViewModel lifecycle subscription") { pendingViewModelLifecycleSubscribed.await() }
            val pendingDestroyed = CompletableDeferred<Unit>()
            pendingDestroyObserver = launch(start = CoroutineStart.UNDISPATCHED) {
                pendingNode.onDestroyFlow.first()
                pendingDestroyed.complete(Unit)
            }
            pendingViewModel.onPasswordChanged("browser-password")
            pendingViewModel.onConfirmationChanged("browser-password")
            assertEquals("browser-password", pendingViewModel.passwordState.value)
            assertEquals("browser-password", pendingViewModel.confirmationState.value)
            pendingViewModel.onSubmitPasswordChange()
            assertTrue(pendingViewModel.loadingState.value)
            awaitBrowserPhase("actual pending ViewModel request") { model.requestReceived.await() }
            awaitBrowserPhase("MockEngine POST") { transport.requestReceived.await() }
            assertEquals(dev.inmo.wishlist.features.auth.common.models.CompletePasswordChangeRequest(UserId(7), approval, dev.inmo.wishlist.features.auth.common.models.Password("browser-password")), model.requests.single())
            assertEquals(model.requests.single(), transport.requests.single())
            assertEquals("POST", transport.httpRequest?.method?.value)
            assertEquals("https://wishlist.test/api/auth/completePasswordChange", transport.httpRequest?.url.toString())
            transport.releaseChanged()
            withTimeout(5_000L) {
                pendingDestroyed.await()
                checkNotNull(pendingViewModelJob).join()
            }
            val persistedCompletion = withTimeout(5_000L) { completedSave.await() }
            assertEquals("/ui/password-changed", window.location.pathname)
            assertEquals("", window.location.search)
            assertEquals(null, window.history.state)
            assertFalse("${window.location.pathname}${window.location.search}${JSON.stringify(window.history.state)}".contains(approval.string))
            assertFalse("${window.location.pathname}${window.location.search}${JSON.stringify(window.history.state)}".contains("browser-password"))
            assertTrue(persistedCompletion.allConfigs().any { it is PasswordChangeViewConfig.Completed })
            assertFalse(persistedCompletion.allConfigs().any { it is PasswordChangeViewConfig.Pending })
            assertTrue(pendingViewModel.passwordState.value.isEmpty())
            assertTrue(pendingViewModel.confirmationState.value.isEmpty())
            assertEquals(dev.inmo.navigation.core.NavigationNodeState.NEW, pendingNode.state)
            checkNotNull(pendingDestroyObserver).cancelAndJoin()

            val completedReload = assertNotNull(WishlistsAppUrlNavigationConfigsRepo().get())
            assertTrue(completedReload.allConfigs().any { it is PasswordChangeViewConfig.Completed })
            assertFalse(completedReload.allConfigs().any { it is PasswordChangeViewConfig.Pending })
            lateinit var completedNode: NavigationNode<PasswordChangeViewConfig, ViewConfig>
            awaitBrowserPhase("restored completed node") {
                completedNode = mainA.stackFlow.filter { stack ->
                    stack.lastOrNull()?.config == PasswordChangeViewConfig.Completed
                }.first().last() as NavigationNode<PasswordChangeViewConfig, ViewConfig>
            }
            val completedViewModel = PasswordChangeViewModel(completedNode, model, interactor, Dispatchers.Unconfined)
            completedViewModelJob = checkNotNull(completedViewModel.scope.coroutineContext[Job])
            val completedViewModelLifecycleSubscribed = CompletableDeferred<Unit>()
            completedViewModel.scope.launch { completedViewModelLifecycleSubscribed.complete(Unit) }
            awaitBrowserPhase("completed ViewModel lifecycle subscription") { completedViewModelLifecycleSubscribed.await() }
            completedViewModel.onSubmitPasswordChange()
            assertEquals(1, model.requests.size)
            assertEquals(1, transport.requests.size)
            val completedDestroyed = CompletableDeferred<Unit>()
            completedDestroyObserver = launch(start = CoroutineStart.UNDISPATCHED) {
                completedNode.onDestroyFlow.first()
                completedDestroyed.complete(Unit)
            }
            completedViewModel.onContinue()
            withTimeout(5_000L) {
                completedDestroyed.await()
                checkNotNull(completedViewModelJob).join()
            }
            val persistedUsersList = withContext(Dispatchers.Default) {
                withTimeout(5_000L) { usersListSave.await() }
            }
            assertTrue(persistedUsersList.allConfigs().any { it is UsersListViewConfig })
            assertFalse(persistedUsersList.allConfigs().any { it is PasswordChangeViewConfig })
            assertEquals("/ui", window.location.pathname)
            assertEquals("", window.location.search)
            assertEquals(null, window.history.state)
            assertEquals(1, model.requests.size)
            assertEquals(1, transport.requests.size)
            checkNotNull(completedDestroyObserver).cancelAndJoin()

            scopeA.await().launch {}.join()
            assertEquals(0, owner.activeTransitionCount)
            ownerSaveAttemptsA = 0
            recordedSavesA.resetObservations()

            val stalePending = mainA.push(PasswordChangeViewConfig.Pending(UserId(7), approval)) as NavigationNode<PasswordChangeViewConfig, ViewConfig>
            awaitBrowserPhase("composition-A stale pending publication") {
                mainA.stackFlow.filter { stack -> stack.lastOrNull() === stalePending }.first()
            }
            assertSame(stalePending, rootA.findNodeInSubTree { candidate -> candidate === stalePending })
            awaitBrowserPhase("composition-A stale pending URL") {
                withContext(Dispatchers.Default) {
                    while (window.location.pathname != "/ui/password-change/7/${approval.string}") yield()
                }
            }

            val timerA = TestCoroutineScheduler()
            heldDispatcherA = HeldNavigationDispatcher(StandardTestDispatcher(timerA))
            heldScopeAJob = SupervisorJob(checkNotNull(scopeAJob))
            val heldScopeA = CoroutineScope(checkNotNull(heldScopeAJob) + checkNotNull(heldDispatcherA))
            leafStartAJob = mainA.start(heldScopeA)
            checkNotNull(heldDispatcherA).drain()
            assertTrue(checkNotNull(leafStartAJob).isActive)
            assertEquals(0, checkNotNull(heldDispatcherA).queuedTaskCount)
            assertSame(stalePending, mainA.stackFlow.value.lastOrNull())

            replacementAArmed = true
            val scopeAChildrenBefore = checkNotNull(scopeAJob).children.toSet()
            interactor.onChanged(stalePending)
            val scopeANewChildren = checkNotNull(scopeAJob).children.filter { child -> child !in scopeAChildrenBefore }.toList()
            assertEquals(1, scopeANewChildren.size)
            transitionAJob = scopeANewChildren.single()
            assertTrue(checkNotNull(transitionAJob).isActive)
            assertEquals(1, owner.activeTransitionCount)

            lateinit var replacementA: NavigationNode<out ViewConfig, ViewConfig>
            awaitBrowserPhase("composition-A replacement creation") {
                replacementA = withContext(Dispatchers.Default) { replacementCreatedA.await() }
            }
            assertSame(mainA, replacementA.chain)
            assertSame(stalePending, mainA.stackFlow.value.lastOrNull())
            assertEquals(null, rootA.findNodeInSubTree { candidate -> candidate === replacementA })
            assertTrue(checkNotNull(heldDispatcherA).queuedTaskCount > 0)
            assertTrue(checkNotNull(transitionAJob).isActive)
            assertEquals(1, owner.activeTransitionCount)
            assertEquals(0, ownerSaveAttemptsA)
            assertTrue(recordedSavesA.holders.isEmpty())

            hostB = document.createElement("div") as HTMLDivElement
            document.body!!.appendChild(hostB)
            val rootB = NavigationChain<ViewConfig>(rootBId, nodesFactoryB)
            val scopeB = CompletableDeferred<CoroutineScope>()
            compositionB = mountPasswordNavigation(hostB, owner, rootB, urlRepo, scopeB, nodesFactoryB)
            awaitBrowserPhase("composition-B scope") { scopeB.await() }
            scopeBJob = checkNotNull(scopeB.await().coroutineContext[Job])
            lateinit var mainB: NavigationChain<ViewConfig>
            awaitBrowserPhase("composition-B restored main chain") { mainB = restoredMainChain(rootB) }
            lateinit var pendingB: NavigationNode<PasswordChangeViewConfig, ViewConfig>
            awaitBrowserPhase("composition-B restored pending node") {
                pendingB = mainB.stackFlow.filter { stack ->
                    stack.lastOrNull()?.config is PasswordChangeViewConfig.Pending
                }.first().last() as NavigationNode<PasswordChangeViewConfig, ViewConfig>
            }
            assertFalse(pendingB === stalePending)
            assertSame(pendingB, rootB.findNodeInSubTree { candidate -> candidate === pendingB })
            assertSame(pendingB, mainB.stackFlow.value.lastOrNull())
            assertEquals("/ui/password-change/7/${approval.string}", window.location.pathname)

            checkNotNull(compositionA).dispose()
            compositionA = null
            checkNotNull(leafStartAJob).cancel()
            checkNotNull(heldScopeAJob).cancel()
            checkNotNull(heldDispatcherA).drain()
            withTimeout(5_000L) {
                checkNotNull(transitionAJob).join()
                checkNotNull(leafStartAJob).join()
                checkNotNull(heldScopeAJob).join()
                checkNotNull(scopeAJob).join()
            }
            assertTrue(checkNotNull(transitionAJob).isCancelled)
            assertTrue(checkNotNull(transitionAJob).isCompleted)
            assertTrue(checkNotNull(scopeAJob).isCancelled)
            assertTrue(checkNotNull(scopeAJob).isCompleted)
            assertEquals(0, ownerSaveAttemptsA)
            assertTrue(recordedSavesA.holders.isEmpty())
            assertTrue(checkNotNull(scopeAJob).children.none())
            assertTrue(checkNotNull(scopeBJob).isActive)
            assertSame(pendingB, mainB.stackFlow.value.lastOrNull())

            val attemptsABeforeStaleCallbacks = ownerSaveAttemptsA
            val attemptsBBeforeStaleCallbacks = ownerSaveAttemptsB
            interactor.onChanged(stalePending)
            interactor.onContinue(stalePending)
            assertEquals(attemptsABeforeStaleCallbacks, ownerSaveAttemptsA)
            assertEquals(attemptsBBeforeStaleCallbacks, ownerSaveAttemptsB)
            assertEquals(0, owner.activeTransitionCount)
            assertSame(pendingB, mainB.stackFlow.value.lastOrNull())

            val timerB = TestCoroutineScheduler()
            heldDispatcherB = HeldNavigationDispatcher(StandardTestDispatcher(timerB))
            heldScopeBJob = SupervisorJob(checkNotNull(scopeBJob))
            val heldScopeB = CoroutineScope(checkNotNull(heldScopeBJob) + checkNotNull(heldDispatcherB))
            leafStartBJob = mainB.start(heldScopeB)
            checkNotNull(heldDispatcherB).drain()
            assertTrue(checkNotNull(leafStartBJob).isActive)
            assertEquals(0, checkNotNull(heldDispatcherB).queuedTaskCount)
            val scopeBChildrenBefore = checkNotNull(scopeBJob).children.toSet()
            replacementBArmed = true
            interactor.onChanged(pendingB)
            val scopeBNewChildren = checkNotNull(scopeBJob).children.filter { child -> child !in scopeBChildrenBefore }.toList()
            assertEquals(1, scopeBNewChildren.size)
            transitionBJob = scopeBNewChildren.single()
            assertTrue(checkNotNull(transitionBJob).isActive)
            assertEquals(1, owner.activeTransitionCount)

            lateinit var replacementB: NavigationNode<out ViewConfig, ViewConfig>
            awaitBrowserPhase("composition-B replacement creation") {
                replacementB = withContext(Dispatchers.Default) { replacementCreatedB.await() }
            }
            assertSame(mainB, replacementB.chain)
            assertSame(pendingB, mainB.stackFlow.value.lastOrNull())
            assertEquals(null, rootB.findNodeInSubTree { candidate -> candidate === replacementB })
            assertTrue(checkNotNull(heldDispatcherB).queuedTaskCount > 0)
            assertEquals(0, ownerSaveAttemptsB)
            assertEquals(0, ownerSaveAttemptsA)
            assertTrue(recordedSavesA.holders.isEmpty())

            checkNotNull(heldDispatcherB).drain()
            awaitBrowserPhase("composition-B exact completed replacement") {
                mainB.stackFlow.filter { stack ->
                    stack.lastOrNull() === replacementB &&
                        stack.none { node -> node.config is PasswordChangeViewConfig.Pending }
                }.first()
            }
            val persistedCompletionB = withContext(Dispatchers.Default) { completedSaveB.await() }
            withTimeout(5_000L) { checkNotNull(transitionBJob).join() }
            assertTrue(checkNotNull(transitionBJob).isCompleted)
            assertFalse(checkNotNull(transitionBJob).isCancelled)
            assertEquals(1, ownerSaveAttemptsB)
            assertEquals(1, recordedSavesB.holders.size)
            assertEquals(0, owner.activeTransitionCount)
            assertTrue(checkNotNull(scopeBJob).isActive)
            assertEquals(0, ownerSaveAttemptsA)
            assertTrue(recordedSavesA.holders.isEmpty())
            assertSame(replacementB, mainB.stackFlow.value.lastOrNull())

            val liveB = checkNotNull(rootB.storeHierarchy())
            assertEquals(
                serializedPasswordNavigationHolder(json, persistedCompletionB),
                serializedPasswordNavigationHolder(json, liveB),
            )
            val savedRootB = persistedCompletionB as ConfigHolder.Chain<ViewConfig>
            assertEquals(rootBId, savedRootB.id)
            val savedRootNode = savedRootB.firstNodeConfig as ConfigHolder.Node<ViewConfig>
            assertTrue(savedRootNode.config is EmptyConfig)
            assertEquals(null, savedRootNode.subnode)
            val savedScaffold = savedRootNode.subchains.single().firstNodeConfig as ConfigHolder.Node<ViewConfig>
            assertEquals(
                ScaffoldViewConfig(
                    TopBarViewConfig(),
                    SidebarViewConfig(),
                    dev.inmo.wishlist.features.ui.wishlist.ui.WishlistsListViewConfig(),
                ),
                savedScaffold.config,
            )
            val savedTop = savedScaffold.subchains.single { chain -> chain.id == TopNavigationChainId }
            val savedSidebar = savedScaffold.subchains.single { chain -> chain.id == LeftNavigationChainId }
            val savedMain = savedScaffold.subchains.single { chain -> chain.id == MainNavigationChainId }
            assertEquals(TopBarViewConfig(), (savedTop.firstNodeConfig as ConfigHolder.Node<ViewConfig>).config)
            assertEquals(SidebarViewConfig(), (savedSidebar.firstNodeConfig as ConfigHolder.Node<ViewConfig>).config)
            val savedUsersList = savedMain.firstNodeConfig as ConfigHolder.Node<ViewConfig>
            assertTrue(savedUsersList.config is UsersListViewConfig)
            val savedCompleted = checkNotNull(savedUsersList.subnode)
            assertEquals(PasswordChangeViewConfig.Completed, savedCompleted.config)
            assertEquals(null, savedCompleted.subnode)
            assertTrue(savedCompleted.subchains.isEmpty())
            assertFalse(persistedCompletionB.passwordNavigationConfigs().any { it is PasswordChangeViewConfig.Pending })
            val serializedB = serializedPasswordNavigationHolder(json, persistedCompletionB)
            assertFalse(serializedB.contains(approval.string))
            assertFalse(serializedB.contains("browser-password"))
            assertEquals("/ui/password-changed", window.location.pathname)
            assertEquals("", window.location.search)
            assertEquals(null, window.history.state)
            assertFalse("${window.location.pathname}${window.location.search}${JSON.stringify(window.history.state)}".contains(approval.string))
            assertFalse("${window.location.pathname}${window.location.search}${JSON.stringify(window.history.state)}".contains("browser-password"))
            assertEquals(1, model.requests.size)
            assertEquals(1, transport.requests.size)

            checkNotNull(compositionB).dispose()
            compositionB = null
            checkNotNull(leafStartBJob).cancel()
            checkNotNull(heldScopeBJob).cancel()
            checkNotNull(heldDispatcherB).drain()
            withTimeout(5_000L) {
                checkNotNull(leafStartBJob).join()
                checkNotNull(heldScopeBJob).join()
                checkNotNull(scopeBJob).join()
            }
            assertTrue(checkNotNull(scopeBJob).isCancelled)
            assertTrue(checkNotNull(scopeBJob).isCompleted)
            assertEquals(0, owner.activeTransitionCount)

            window.history.replaceState(null, "", "/ui/wishlist/9/item/11")
            val itemConfigs = assertNotNull(WishlistsAppUrlNavigationConfigsRepo().get()).allConfigs()
            assertTrue(itemConfigs.contains(WishlistViewConfig(WishlistId(9))))
            assertTrue(itemConfigs.contains(WishlistItemViewConfig(WishlistItemId(11), WishlistId(9))))
            window.history.replaceState(null, "", "/ui/password-change/7/not-a-uuid")
            assertEquals(null, WishlistsAppUrlNavigationConfigsRepo().get())
            val notifications = mutableListOf<String>()
            assertTrue(consumeEmailApprovalNotification("https://wishlist.test/ui/?emailApproval=approved#marker", notifications::add) { window.history.replaceState(null, "", it) })
            assertEquals(listOf("Email has been approved."), notifications)
            assertEquals("/ui/", window.location.pathname)
        } finally {
            compositionA?.dispose()
            compositionB?.dispose()
            leafStartAJob?.cancel()
            heldScopeAJob?.cancel()
            leafStartBJob?.cancel()
            heldScopeBJob?.cancel()
            heldDispatcherA?.drain()
            heldDispatcherB?.drain()
            transitionAJob?.cancelAndJoin()
            transitionBJob?.cancelAndJoin()
            leafStartAJob?.cancelAndJoin()
            heldScopeAJob?.cancelAndJoin()
            scopeAJob?.cancelAndJoin()
            leafStartBJob?.cancelAndJoin()
            heldScopeBJob?.cancelAndJoin()
            scopeBJob?.cancelAndJoin()
            pendingDestroyObserver?.cancelAndJoin()
            completedDestroyObserver?.cancelAndJoin()
            pendingViewModelJob?.cancelAndJoin()
            completedViewModelJob?.cancelAndJoin()
            hostA?.remove()
            hostB?.remove()
            transport?.close()
            application?.close()
            ClientPlugin.mainScaffoldConfigProvider = previousProvider
            dom.window.close()
            restorePasswordNavigationGlobals(globals)
        }
    }
}
