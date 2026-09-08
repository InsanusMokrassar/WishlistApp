package dev.inmo.wishlist.client

import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.core.NavigationNode
import dev.inmo.navigation.core.NavigationNodeFactory
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.ui.adminPanel.ui.AdminPanelViewConfig
import dev.inmo.wishlist.features.ui.adminPanel.ui.AdminPanelViewInteractor
import dev.inmo.wishlist.features.ui.adminPanel.ui.AdminUserViewConfig
import dev.inmo.wishlist.features.users.common.models.UserId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertEquals

/** Exercises the production dashboard interactor against a running navigation stack. */
@OptIn(ExperimentalCoroutinesApi::class)
class AdminPanelNavigationTest {
    @Test
    fun selectedDashboardUserPushesTheExactAdminUserConfig() = runTest {
        val koin = startKoin {
            modules(
                module {
                    with(ClientPlugin) { setupDI(JsonObject(emptyMap())) }
                }
            )
        }
        try {
            val interactor = koin.koin.get<AdminPanelViewInteractor>()
            val chain = NavigationChain(
                parentNode = null,
                nodeFactory = NavigationNodeFactory<ViewConfig> { navigationChain, config ->
                    NavigationNode.Empty(navigationChain, config)
                },
            )
            val chainJob = chain.start(this)
            try {
                val node = NavigationNode.Empty(chain, AdminPanelViewConfig())
                val selectedId = UserId(23L)

                runCurrent()
                interactor.onUserSelected(node, selectedId)
                advanceUntilIdle()

                assertEquals(AdminUserViewConfig(selectedId), chain.stackFlow.value.single().config)
            } finally {
                chainJob.cancel()
            }
        } finally {
            stopKoin()
        }
    }
}
