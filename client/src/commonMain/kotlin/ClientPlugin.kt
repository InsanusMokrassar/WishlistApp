package dev.inmo.wishlist.client

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import dev.inmo.micro_utils.common.either
import dev.inmo.micro_utils.coroutines.MutableRedeliverStateFlow
import dev.inmo.micro_utils.coroutines.subscribeLoggingDropExceptions
import dev.inmo.micro_utils.startup.plugin.StartPlugin
import dev.inmo.navigation.compose.InjectNavigationChain
import dev.inmo.navigation.compose.InjectNavigationNode
import dev.inmo.navigation.compose.getChainFromLocalProvider
import dev.inmo.navigation.compose.initNavigation
import dev.inmo.navigation.compose.nodeFactory
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.core.extensions.changesInSubTreeFlow
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.navigation.core.extensions.changesInSubtreeFlow
import dev.inmo.navigation.core.extensions.dropNodesInSubTree
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.merge
import kotlinx.serialization.json.JsonObject
import org.koin.core.Koin
import org.koin.core.module.Module
import dev.inmo.wishlist.features.common.client.models.EmptyConfig
import dev.inmo.wishlist.features.common.client.models.RootNodeFactoryGetter
import dev.inmo.wishlist.features.ui.auth.ui.AuthViewConfig
import dev.inmo.wishlist.features.ui.auth.ui.AuthViewInteractor
import dev.inmo.wishlist.features.ui.sample.ui.SampleViewConfig
import dev.inmo.wishlist.features.ui.wishlist.ui.WishlistEditViewConfig
import dev.inmo.wishlist.features.ui.wishlist.ui.WishlistEditViewInteractor
import dev.inmo.wishlist.features.ui.wishlist.ui.WishlistItemEditViewConfig
import dev.inmo.wishlist.features.ui.wishlist.ui.WishlistItemEditViewInteractor
import dev.inmo.wishlist.features.ui.wishlist.ui.WishlistItemViewConfig
import dev.inmo.wishlist.features.ui.wishlist.ui.WishlistItemViewInteractor
import dev.inmo.wishlist.features.ui.wishlist.ui.WishlistViewConfig
import dev.inmo.wishlist.features.ui.wishlist.ui.WishlistViewInteractor
import dev.inmo.wishlist.features.ui.wishlist.ui.WishlistsListViewConfig
import dev.inmo.wishlist.features.ui.wishlist.ui.WishlistsListViewInteractor
import dev.inmo.navigation.core.NavigationNode
import dev.inmo.navigation.core.NavigationNodeFactory
import dev.inmo.navigation.core.NavigationNodeState
import dev.inmo.wishlist.features.wishlist.common.models.WishlistId
import dev.inmo.wishlist.features.wishlist.common.models.WishlistItemId

object ClientPlugin : StartPlugin {
    val currentDrawingBlock = MutableRedeliverStateFlow<@Composable () -> Unit>({})
    override fun Module.setupDI(config: JsonObject) {
        single<RootNodeFactoryGetter> {
            val nodeFactory: NavigationNodeFactory<ViewConfig> = getKoin().nodeFactory<ViewConfig>()

            return@single RootNodeFactoryGetter { nodeFactory }
        }
        single {
            NavigationChain<ViewConfig>(
                null,
                get<RootNodeFactoryGetter>().invoke()
            )
        }
        single {
            { drawable: @Composable () -> Unit ->
                currentDrawingBlock.value = drawable
            }
        }

        single<WishlistsListViewInteractor> {
            object : WishlistsListViewInteractor {
                override suspend fun onWishlistSelected(
                    node: NavigationNode<WishlistsListViewConfig, ViewConfig>,
                    wishlistId: WishlistId
                ) {
                    node.chain.push(WishlistViewConfig(wishlistId))
                }
                override suspend fun onCreateWishlist(
                    node: NavigationNode<WishlistsListViewConfig, ViewConfig>
                ) {
                    node.chain.push(WishlistEditViewConfig(null))
                }
            }
        }

        single<WishlistViewInteractor> {
            object : WishlistViewInteractor {
                override suspend fun onBack(
                    node: NavigationNode<WishlistViewConfig, ViewConfig>
                ) {
                    node.chain.pop()
                }
                override suspend fun onEditWishlist(
                    node: NavigationNode<WishlistViewConfig, ViewConfig>
                ) {
                    node.chain.push(WishlistEditViewConfig(node.config.wishlistId))
                }
                override suspend fun onViewItem(
                    node: NavigationNode<WishlistViewConfig, ViewConfig>,
                    itemId: WishlistItemId
                ) {
                    node.chain.push(WishlistItemViewConfig(itemId, node.config.wishlistId))
                }
                override suspend fun onAddItem(
                    node: NavigationNode<WishlistViewConfig, ViewConfig>
                ) {
                    node.chain.push(WishlistItemEditViewConfig(null, node.config.wishlistId))
                }
            }
        }

        single<WishlistEditViewInteractor> {
            object : WishlistEditViewInteractor {
                override suspend fun onNavigateBack(
                    node: NavigationNode<WishlistEditViewConfig, ViewConfig>
                ) {
                    node.chain.pop()
                }
                override suspend fun onSaved(
                    node: NavigationNode<WishlistEditViewConfig, ViewConfig>
                ) {
                    node.chain.pop()
                }
            }
        }

        single<WishlistItemEditViewInteractor> {
            object : WishlistItemEditViewInteractor {
                override suspend fun onNavigateBack(
                    node: NavigationNode<WishlistItemEditViewConfig, ViewConfig>
                ) {
                    node.chain.pop()
                }
                override suspend fun onSaved(
                    node: NavigationNode<WishlistItemEditViewConfig, ViewConfig>
                ) {
                    node.chain.pop()
                }
            }
        }

        single<WishlistItemViewInteractor> {
            object : WishlistItemViewInteractor {
                override suspend fun onBack(
                    node: NavigationNode<WishlistItemViewConfig, ViewConfig>
                ) {
                    node.chain.pop()
                }
                override suspend fun onEditItem(
                    node: NavigationNode<WishlistItemViewConfig, ViewConfig>
                ) {
                    node.chain.push(WishlistItemEditViewConfig(node.config.wishlistItemId, node.config.wishlistId))
                }
            }
        }

        single<AuthViewInteractor> {
            val rootChain = get<NavigationChain<ViewConfig>>()
            val scope = get<CoroutineScope>()
            object : AuthViewInteractor {
                private val userLoggedIn = MutableRedeliverStateFlow(true)

                init {
                    merge(
                        userLoggedIn,
                        rootChain.changesInSubTreeFlow()
                    ).conflate().subscribeLoggingDropExceptions(scope) {
                        // Place here reaction onto user deauth
                        // By default - it will push auth view in root chain
                        when {
                            userLoggedIn.value -> {
                                rootChain.dropNodesInSubTree { it.config is AuthViewConfig }
                            }
                            else -> {
                                if (rootChain.stackFlow.value.lastOrNull() ?.config !is AuthViewConfig) {
                                    rootChain.push(AuthViewConfig())
                                }
                            }
                        }
                    }
                }

                override suspend fun onUserLoggedIn(node: NavigationNode<AuthViewConfig, ViewConfig>) {
                    userLoggedIn.value = true
                }

                override suspend fun onUserLoggedOut() {
                    userLoggedIn.value = false
                }
            }
        }
    }

    private fun NavigationNode<*, ViewConfig>.makeNodeString(spacers: String): String {
        return "$spacers[Node ${id.string}] ${this::class.simpleName} -> ${this.config::class.simpleName}; State: $state\n${subchainsFlow.value.joinToString("\n") { it.makeChainString("$spacers  ") }}"
    }
    private fun NavigationChain<ViewConfig>.makeChainString(spacers: String): String {
        val (id, stack) = runCatching { // some kotlin/js bug -.-
            id to stackFlow.value
        }.getOrElse {
            return "${spacers}Error"
        }
        return "$spacers[Chain ${id?.string ?: "Anonymous"}] State ${parentNode ?.state ?: NavigationNodeState.RESUMED}\n${stack.joinToString("\n") { it.makeNodeString("$spacers  ") }}"
    }
    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
        val rootChain = koin.get<NavigationChain<ViewConfig>>()
        koin.get<(@Composable () -> Unit) -> Unit>().invoke {
            initNavigation<ViewConfig>(
                EmptyConfig(),
                configsRepo = koin.get(),
                nodesFactory = koin.get<RootNodeFactoryGetter>().invoke(),
                dropRedundantChainsOnRestore = true,
                rootChain = rootChain
            ) {
                val rootChain = getChainFromLocalProvider<ViewConfig>()!!
                LaunchedEffect(rootChain) {
                    rootChain.either<NavigationChain<ViewConfig>, NavigationNode<out ViewConfig, ViewConfig>>().changesInSubtreeFlow().conflate().collect {
                        println(rootChain.makeChainString(""))
                    }
                }
            }
        }
    }
}
