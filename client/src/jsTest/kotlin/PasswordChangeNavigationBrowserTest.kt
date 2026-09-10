package dev.inmo.wishlist.client

import androidx.compose.runtime.Composition
import dev.inmo.navigation.compose.initNavigation
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.core.NavigationNode
import dev.inmo.navigation.core.NavigationNodeFactory
import dev.inmo.navigation.core.onDestroyFlow
import dev.inmo.navigation.core.repo.ConfigHolder
import dev.inmo.navigation.core.repo.NavigationConfigsRepo
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
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.promise
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
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
import kotlin.test.assertTrue

/** Returns the browser global object used by the JSDOM harness. */
private fun browserGlobal(): dynamic = js("globalThis")

/** Creates an isolated JSDOM window at [url] for URL and navigation assertions. */
private fun browserDom(url: String): dynamic =
    js("new (require('jsdom').JSDOM)('<!doctype html><html><head></head><body></body></html>', { url: url, pretendToBeVisual: true })")

private val passwordNavigationGlobalKeys = listOf(
    "window", "document", "Node", "Element", "HTMLElement", "HTMLDivElement", "HTMLBaseElement",
    "Text", "Comment", "DocumentFragment", "Event", "requestAnimationFrame", "cancelAnimationFrame",
)

private fun capturePasswordNavigationGlobals(): dynamic {
    val objectApi: dynamic = js("Object")
    val global = browserGlobal()
    val snapshot: dynamic = js("({})")
    passwordNavigationGlobalKeys.forEach { key -> snapshot[key] = objectApi.getOwnPropertyDescriptor(global, key) }
    return snapshot
}

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

private fun ConfigHolder<ViewConfig>.allConfigs(): List<ViewConfig> = when (this) {
    is ConfigHolder.Chain -> firstNodeConfig?.allConfigs().orEmpty()
    is ConfigHolder.Node -> listOf(config) + subnode?.allConfigs().orEmpty() + subchains.flatMap { it.allConfigs() }
}

private suspend fun NavigationChain<ViewConfig>.awaitStack(): List<NavigationNode<out ViewConfig, ViewConfig>> =
    stackFlow.value.takeIf { it.isNotEmpty() } ?: stackFlow.filter { it.isNotEmpty() }.first()

private suspend fun awaitBrowserPhase(name: String, block: suspend () -> Unit) {
    check(withTimeoutOrNull(5_000L) {
        block()
        true
    } == true) { "Browser password-change phase did not complete: $name" }
}

private suspend fun restoredMainChain(root: NavigationChain<ViewConfig>): NavigationChain<ViewConfig> {
    val rootNode = root.awaitStack().single()
    val scaffold = rootNode.subchains.single().awaitStack().single()
    return checkNotNull(scaffold.subchains.firstOrNull { it.id == MainNavigationChainId })
}

private fun mountPasswordNavigation(
    host: HTMLDivElement,
    owner: PasswordChangeNavigationOwner,
    root: NavigationChain<ViewConfig>,
    configsRepo: NavigationConfigsRepo<ViewConfig>,
    scopeCaptured: CompletableDeferred<CoroutineScope>,
): Composition = renderComposable(host) {
    WithPasswordChangeNavigationBinding(owner, root) { rootScope ->
        scopeCaptured.complete(rootScope)
        initNavigation(
            rootNodeConfig = EmptyConfig(),
            configsRepo = configsRepo,
            nodesFactory = NavigationNodeFactory { chain, config -> NavigationNode.Empty(chain, config) },
            scope = rootScope,
            dropRedundantChainsOnRestore = true,
            rootChain = root,
        ) {}
    }
}

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

            val completedSave = CompletableDeferred<ConfigHolder<ViewConfig>>()
            val usersListSave = CompletableDeferred<ConfigHolder<ViewConfig>>()
            val persistenceRepo = object : NavigationConfigsRepo<ViewConfig> {
                override fun save(holder: ConfigHolder<ViewConfig>) {
                    urlRepo.save(holder)
                    when {
                        holder.allConfigs().any { it is PasswordChangeViewConfig.Completed } && holder.allConfigs().none { it is PasswordChangeViewConfig.Pending } && !completedSave.isCompleted -> completedSave.complete(holder)
                        holder.allConfigs().any { it is UsersListViewConfig } && holder.allConfigs().none { it is PasswordChangeViewConfig } && !usersListSave.isCompleted -> usersListSave.complete(holder)
                    }
                }

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
            val rootA = NavigationChain<ViewConfig>(null, NavigationNodeFactory { chain, config -> NavigationNode.Empty(chain, config) })
            val scopeA = CompletableDeferred<CoroutineScope>()
            compositionA = mountPasswordNavigation(hostA, owner, rootA, persistenceRepo, scopeA)
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
            val pendingViewModelJob = checkNotNull(pendingViewModel.scope.coroutineContext[Job])
            assertEquals(PasswordChangeViewConfig.Pending(UserId(7), approval), pendingViewModel.config)
            assertFalse(pendingViewModel.completedState)
            assertFalse(pendingViewModel.loadingState.value)
            assertFalse(pendingViewModel.terminalInvalidApprovalState.value)
            val pendingViewModelLifecycleSubscribed = CompletableDeferred<Unit>()
            pendingViewModel.scope.launch { pendingViewModelLifecycleSubscribed.complete(Unit) }
            awaitBrowserPhase("pending ViewModel lifecycle subscription") { pendingViewModelLifecycleSubscribed.await() }
            val pendingDestroyed = CompletableDeferred<Unit>()
            val pendingDestroyObserver = launch(start = CoroutineStart.UNDISPATCHED) {
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
                pendingViewModelJob.join()
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
            pendingDestroyObserver.cancelAndJoin()

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
            val completedViewModelJob = checkNotNull(completedViewModel.scope.coroutineContext[Job])
            val completedViewModelLifecycleSubscribed = CompletableDeferred<Unit>()
            completedViewModel.scope.launch { completedViewModelLifecycleSubscribed.complete(Unit) }
            awaitBrowserPhase("completed ViewModel lifecycle subscription") { completedViewModelLifecycleSubscribed.await() }
            completedViewModel.onSubmitPasswordChange()
            assertEquals(1, model.requests.size)
            assertEquals(1, transport.requests.size)
            val completedDestroyed = CompletableDeferred<Unit>()
            val completedDestroyObserver = launch(start = CoroutineStart.UNDISPATCHED) {
                completedNode.onDestroyFlow.first()
                completedDestroyed.complete(Unit)
            }
            completedViewModel.onContinue()
            withTimeout(5_000L) {
                completedDestroyed.await()
                completedViewModelJob.join()
            }
            val persistedUsersList = withTimeout(5_000L) { usersListSave.await() }
            assertTrue(persistedUsersList.allConfigs().any { it is UsersListViewConfig })
            assertFalse(persistedUsersList.allConfigs().any { it is PasswordChangeViewConfig })
            assertEquals("/ui", window.location.pathname)
            assertEquals("", window.location.search)
            assertEquals(null, window.history.state)
            assertEquals(1, model.requests.size)
            assertEquals(1, transport.requests.size)
            completedDestroyObserver.cancelAndJoin()

            val stalePending = mainA.push(PasswordChangeViewConfig.Pending(UserId(7), approval)) as NavigationNode<PasswordChangeViewConfig, ViewConfig>
            interactor.onChanged(stalePending)
            hostB = document.createElement("div") as HTMLDivElement
            document.body!!.appendChild(hostB)
            val rootB = NavigationChain<ViewConfig>(null, NavigationNodeFactory { chain, config -> NavigationNode.Empty(chain, config) })
            val scopeB = CompletableDeferred<CoroutineScope>()
            compositionB = mountPasswordNavigation(hostB, owner, rootB, persistenceRepo, scopeB)
            awaitBrowserPhase("composition-B scope") { scopeB.await() }
            scopeBJob = checkNotNull(scopeB.await().coroutineContext[Job])
            compositionA.dispose()
            compositionA = null
            withTimeout(5_000L) { scopeAJob.join() }
            interactor.onContinue(stalePending)
            awaitBrowserPhase("composition-B navigation start") { rootB.awaitStack() }
            val pendingB = rootB.push(PasswordChangeViewConfig.Pending(UserId(8), approval)) as NavigationNode<PasswordChangeViewConfig, ViewConfig>
            awaitBrowserPhase("composition-B pending root node") {
                rootB.stackFlow.filter { stack -> stack.lastOrNull() === pendingB }.first()
            }
            interactor.onChanged(pendingB)
            awaitBrowserPhase("composition-B completed transition") {
                rootB.stackFlow.filter { stack ->
                    stack.any { it.config == PasswordChangeViewConfig.Completed }
                }.first()
            }
            compositionB.dispose()
            compositionB = null
            withTimeout(5_000L) { scopeBJob.join() }
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
            scopeAJob?.cancelAndJoin()
            scopeBJob?.cancelAndJoin()
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
