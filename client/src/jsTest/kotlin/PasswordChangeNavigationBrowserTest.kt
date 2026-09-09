package dev.inmo.wishlist.client

import dev.inmo.navigation.core.repo.ConfigHolder
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.core.NavigationNode
import dev.inmo.navigation.core.NavigationNodeFactory
import dev.inmo.navigation.core.repo.NavigationConfigsRepo
import dev.inmo.navigation.core.repo.enableSavingHierarchy
import dev.inmo.navigation.core.repo.restoreHierarchy
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
import dev.inmo.wishlist.features.ui.users.ui.PasswordChangeViewModel
import dev.inmo.wishlist.features.ui.wishlist.ui.WishlistItemViewConfig
import dev.inmo.wishlist.features.ui.wishlist.ui.WishlistViewConfig
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.wishlist.common.models.WishlistId
import dev.inmo.wishlist.features.wishlist.common.models.WishlistItemId
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.promise
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Job
import kotlin.coroutines.coroutineContext
import kotlinx.serialization.json.JsonObject
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import org.w3c.dom.HTMLBaseElement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Returns the browser global object used by the JSDOM harness. */
private fun browserGlobal(): dynamic = js("globalThis")
/** Creates an isolated JSDOM window at [url] for URL and navigation assertions. */
private fun browserDom(url: String): dynamic = js("new (require('jsdom').JSDOM)('<!doctype html><html><head></head><body></body></html>', { url: url, pretendToBeVisual: true })")

/** Flattens a restored configuration hierarchy for credential and route assertions. */
private fun ConfigHolder<ViewConfig>.allConfigs(): List<ViewConfig> = when (this) {
    is ConfigHolder.Chain -> firstNodeConfig?.allConfigs().orEmpty()
    is ConfigHolder.Node -> listOf(config) + subnode?.allConfigs().orEmpty() + subchains.flatMap { it.allConfigs() }
}

/** Waits until the started chain has processed queued restoration pushes. */
private suspend fun NavigationChain<ViewConfig>.awaitRestoredStack(): List<NavigationNode<out ViewConfig, ViewConfig>> =
    stackFlow.filter { it.isNotEmpty() }.first()

/** Runs URL persistence against the actual browser adapter under the Mocha/Node JSDOM host. */
class PasswordChangeNavigationBrowserTest {
    /** Proves reload restores Pending, completion persists Completed, and Continue needs no HTTP. */
    @Test
    fun canonicalApprovalReloadsAndCompletionPersistsWithoutCredential() = MainScope().promise {
        val global = browserGlobal()
        val previousWindow = global.window
        val previousDocument = global.document
        val previousNode = global.Node
        val previousElement = global.Element
        val previousHTMLElement = global.HTMLElement
        val previousHTMLBaseElement = global.HTMLBaseElement
        val dom = browserDom("https://wishlist.test/ui/password-change/7/123e4567-e89b-42d3-a456-426614174000")
        val previousProvider = ClientPlugin.mainScaffoldConfigProvider
        global.window = dom.window
        global.document = dom.window.document
        global.Node = dom.window.Node
        global.Element = dom.window.Element
        global.HTMLElement = dom.window.HTMLElement
        global.HTMLBaseElement = dom.window.HTMLBaseElement
        try {
            val base = document.createElement("base") as HTMLBaseElement
            base.href = "/ui/"
            document.head!!.appendChild(base)
            ClientPlugin.mainScaffoldConfigProvider = {
                ScaffoldViewConfig(TopBarViewConfig(), SidebarViewConfig(), dev.inmo.wishlist.features.ui.wishlist.ui.WishlistsListViewConfig())
            }
            val approval = DeepLinkId("123e4567-e89b-42d3-a456-426614174000")
            val restored = assertNotNull(WishlistsAppUrlNavigationConfigsRepo().get())
            val configs = restored.allConfigs()
            assertTrue(configs.first() is EmptyConfig)
            assertEquals(
                PasswordChangeViewConfig.Pending(UserId(7), approval),
                configs.filterIsInstance<PasswordChangeViewConfig.Pending>().single(),
            )
            val scaffold = ((restored as ConfigHolder.Chain<ViewConfig>).firstNodeConfig
                as ConfigHolder.Node<ViewConfig>).subchains.single().firstNodeConfig
                as ConfigHolder.Node<ViewConfig>
            val ids = scaffold.subchains.mapNotNull { it.id }
            assertTrue(TopNavigationChainId in ids)
            assertTrue(LeftNavigationChainId in ids)
            assertTrue(MainNavigationChainId in ids)

            WishlistsAppUrlNavigationConfigsRepo().save(restored)
            assertEquals("/ui/password-change/7/123e4567-e89b-42d3-a456-426614174000", window.location.pathname)
            assertEquals(
                PasswordChangeViewConfig.Pending(UserId(7), approval),
                assertNotNull(WishlistsAppUrlNavigationConfigsRepo().get())
                    .allConfigs().filterIsInstance<PasswordChangeViewConfig.Pending>().single(),
            )

            val urlRepo = WishlistsAppUrlNavigationConfigsRepo()
            val completedSave = CompletableDeferred<ConfigHolder<ViewConfig>>()
            val persistenceRepo = object : NavigationConfigsRepo<ViewConfig> {
                override fun save(holder: ConfigHolder<ViewConfig>) {
                    urlRepo.save(holder)
                    if (holder.allConfigs().any { it is PasswordChangeViewConfig.Completed }) {
                        completedSave.complete(holder)
                    }
                }

                override fun get(): ConfigHolder<ViewConfig>? = urlRepo.get()
            }
            val liveFactory = NavigationNodeFactory<ViewConfig> { chain, config ->
                NavigationNode.Empty(chain, config)
            }
            val rootChain = NavigationChain<ViewConfig>(null, liveFactory)
            val navigationJob = SupervisorJob()
            val navigationScope = CoroutineScope(coroutineContext + navigationJob)
            val savingJob = persistenceRepo.enableSavingHierarchy(rootChain, navigationScope)
            val application = koinApplication {
                modules(module {
                    with(ClientPlugin) { setupDI(JsonObject(emptyMap())) }
                    single<NavigationConfigsRepo<ViewConfig>> { persistenceRepo }
                })
            }
            val liveChain = checkNotNull(restoreHierarchy(
                restored,
                liveFactory,
                rootChain,
            ))
            assertTrue(liveChain === rootChain)
            val rootJob = rootChain.start(navigationScope)
            val owner = application.koin.get<PasswordChangeNavigationOwner>()
            val unbind = owner.bind(rootChain, navigationScope)
            try {
                val restoredRootNode = rootChain.awaitRestoredStack().single()
                val scaffoldNode = restoredRootNode.subchains.single().awaitRestoredStack().single()
                val mainChain = checkNotNull(scaffoldNode.subchains.firstOrNull { it.id == MainNavigationChainId })
                val pendingNode = mainChain.awaitRestoredStack().last() as NavigationNode<PasswordChangeViewConfig, ViewConfig>
                val model = HeldPasswordChangeUsersModel()
                val interactor = application.koin.get<dev.inmo.wishlist.features.ui.users.ui.PasswordChangeViewInteractor>()
                val pendingViewModel = PasswordChangeViewModel(pendingNode, model, interactor)
                pendingViewModel.onPasswordChanged("browser-password")
                pendingViewModel.onConfirmationChanged("browser-password")
                pendingViewModel.onSubmitPasswordChange()
                model.completion.complete(dev.inmo.wishlist.features.auth.common.models.PasswordChangeResult.Changed)
                assertTrue(mainChain.stackFlow.filter { it.lastOrNull()?.config is PasswordChangeViewConfig.Completed }.first().isNotEmpty())
                val persistedCompletion = completedSave.await()
                assertEquals(1, model.requests.size)
                assertEquals("/ui/password-changed", window.location.pathname)
                assertEquals(null, window.history.state)
                assertTrue(persistedCompletion.allConfigs().any { it is PasswordChangeViewConfig.Completed })
                assertFalse(persistedCompletion.allConfigs().any { it is PasswordChangeViewConfig.Pending })
                assertTrue(pendingViewModel.passwordState.value.isEmpty())
                assertTrue(pendingViewModel.confirmationState.value.isEmpty())
                assertEquals(dev.inmo.navigation.core.NavigationNodeState.NEW, pendingNode.state)
                val completedNode = mainChain.stackFlow.value.last() as NavigationNode<PasswordChangeViewConfig, ViewConfig>
                val completedViewModel = PasswordChangeViewModel(completedNode, model, interactor)
                completedViewModel.onSubmitPasswordChange()
                assertEquals(1, model.requests.size)
                interactor.onContinue(completedNode)
                assertTrue(mainChain.stackFlow.filter { it.lastOrNull()?.config is dev.inmo.wishlist.features.ui.users.ui.UsersListViewConfig }.first().isNotEmpty())
                assertEquals("/ui/", window.location.pathname)
                assertEquals(null, window.history.state)
                assertEquals(0, owner.activeTransitionCount)
            } finally {
                unbind()
                savingJob.cancelAndJoin()
                rootJob.cancelAndJoin()
                navigationJob.cancelAndJoin()
                application.close()
            }

            val completed = ConfigHolder.Chain<ViewConfig>(
                ConfigHolder.Node(PasswordChangeViewConfig.Completed, null, emptyList()),
                null,
            )
            WishlistsAppUrlNavigationConfigsRepo().save(completed)
            assertEquals("/ui/password-changed", window.location.pathname)
            val completedReload = assertNotNull(WishlistsAppUrlNavigationConfigsRepo().get())
            assertTrue(completedReload.allConfigs().any { it is PasswordChangeViewConfig.Completed })
            assertFalse(completedReload.allConfigs().any { it is PasswordChangeViewConfig.Pending })

            window.history.replaceState(null, "", "/ui/wishlist/9/item/11")
            val itemConfigs = assertNotNull(WishlistsAppUrlNavigationConfigsRepo().get()).allConfigs()
            assertTrue(itemConfigs.contains(WishlistViewConfig(WishlistId(9))))
            assertTrue(itemConfigs.contains(WishlistItemViewConfig(WishlistItemId(11), WishlistId(9))))
            window.history.replaceState(null, "", "/ui/password-change/7/not-a-uuid")
            assertEquals(null, WishlistsAppUrlNavigationConfigsRepo().get())
            val notifications = mutableListOf<String>()
            assertTrue(
                consumeEmailApprovalNotification(
                    "https://wishlist.test/ui/?emailApproval=approved#marker",
                    notifications::add,
                    { window.history.replaceState(null, "", it) },
                ),
            )
            assertEquals(listOf("Email has been approved."), notifications)
            assertEquals("/ui/", window.location.pathname)
        } finally {
            ClientPlugin.mainScaffoldConfigProvider = previousProvider
            dom.window.close()
            global.window = previousWindow
            global.document = previousDocument
            global.Node = previousNode
            global.Element = previousElement
            global.HTMLElement = previousHTMLElement
            global.HTMLBaseElement = previousHTMLBaseElement
        }
    }
}
