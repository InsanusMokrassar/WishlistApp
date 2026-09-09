package dev.inmo.wishlist.client

import dev.inmo.navigation.core.repo.ConfigHolder
import dev.inmo.navigation.core.NavigationNode
import dev.inmo.navigation.core.NavigationNodeFactory
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
import dev.inmo.wishlist.features.ui.wishlist.ui.WishlistItemViewConfig
import dev.inmo.wishlist.features.ui.wishlist.ui.WishlistViewConfig
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.wishlist.common.models.WishlistId
import dev.inmo.wishlist.features.wishlist.common.models.WishlistItemId
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.promise
import kotlinx.coroutines.yield
import kotlinx.serialization.json.JsonObject
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.w3c.dom.HTMLBaseElement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

private fun browserGlobal(): dynamic = js("globalThis")
private fun browserDom(url: String): dynamic = js("new (require('jsdom').JSDOM)('<!doctype html><html><head></head><body></body></html>', { url: url, pretendToBeVisual: true })")

private fun ConfigHolder<ViewConfig>.allConfigs(): List<ViewConfig> = when (this) {
    is ConfigHolder.Chain -> firstNodeConfig?.allConfigs().orEmpty()
    is ConfigHolder.Node -> listOf(config) + subnode?.allConfigs().orEmpty() + subchains.flatMap { it.allConfigs() }
}

/** Runs URL persistence against the actual browser adapter under the Mocha/Node JSDOM host. */
class PasswordChangeNavigationBrowserTest {
    @Test
    fun canonicalApprovalReloadsAndCompletionPersistsWithoutCredential() = MainScope().promise {
        val global = browserGlobal()
        val previousWindow = global.window
        val previousDocument = global.document
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

            val application = startKoin { modules(module { with(ClientPlugin) { setupDI(JsonObject(emptyMap())) } }) }
            val liveChain = checkNotNull(restoreHierarchy(
                restored,
                NavigationNodeFactory<ViewConfig> { chain, config -> NavigationNode.Empty(chain, config) },
            ))
            val liveJob = liveChain.start(this)
            val savingJob = WishlistsAppUrlNavigationConfigsRepo().enableSavingHierarchy(liveChain, this)
            try {
                yield()
                val scaffoldNode = checkNotNull(liveChain.stackFlow.value.single().subnode)
                val mainChain = checkNotNull(scaffoldNode.subchains.firstOrNull { it.id == MainNavigationChainId })
                val pendingNode = mainChain.stackFlow.value.last() as NavigationNode<PasswordChangeViewConfig, ViewConfig>
                application.koin.get<dev.inmo.wishlist.features.ui.users.ui.PasswordChangeViewInteractor>()
                    .onChanged(pendingNode)
                yield()
                yield()
                assertTrue(mainChain.stackFlow.value.last().config is PasswordChangeViewConfig.Completed)
                assertEquals("/ui/password-changed", window.location.pathname)
                assertFalse(liveChain.stackFlow.value.any { it.config.toString().contains(approval.string) })
            } finally {
                savingJob.cancel()
                liveJob.cancel()
                stopKoin()
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
                    "https://wishlist.test/ui/?email-approved=true#marker",
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
        }
    }
}
