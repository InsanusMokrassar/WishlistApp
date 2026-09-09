package dev.inmo.wishlist.client

import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.core.NavigationNode
import dev.inmo.navigation.core.NavigationNodeFactory
import dev.inmo.navigation.core.repo.ConfigHolder
import dev.inmo.navigation.core.repo.NavigationConfigsRepo
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.deeplinks.common.models.DeepLinkId
import dev.inmo.wishlist.features.ui.users.ui.PasswordChangeViewConfig
import dev.inmo.wishlist.features.ui.users.ui.PasswordChangeViewInteractor
import dev.inmo.wishlist.features.ui.users.ui.UsersListViewConfig
import dev.inmo.wishlist.features.users.common.models.UserId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
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
import kotlin.test.assertTrue

/** Exercises the production client password-change navigation binding against an active chain. */
@OptIn(ExperimentalCoroutinesApi::class)
class PasswordChangeInteractorTest {
    private val approvalId = DeepLinkId("123e4567-e89b-42d3-a456-426614174000")

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
            val chain = NavigationChain<ViewConfig>(null, NavigationNodeFactory { parent, config ->
                NavigationNode.Empty(parent, config)
            })
            val chainJob = chain.start(this)
            try {
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
            } finally {
                chainJob.cancel()
            }
        } finally {
            stopKoin()
        }
    }
}
