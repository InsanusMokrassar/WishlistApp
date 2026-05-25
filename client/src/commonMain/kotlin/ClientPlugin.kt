package project_group.project_name.client

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
import dev.inmo.navigation.core.NavigationNode
import dev.inmo.navigation.core.NavigationNodeFactory
import dev.inmo.navigation.core.NavigationNodeState
import dev.inmo.navigation.core.extensions.changesInSubTreeFlow
import project_group.project_name.features.common.client.models.ViewConfig
import dev.inmo.navigation.core.extensions.changesInSubtreeFlow
import dev.inmo.navigation.core.extensions.dropNodesInSubTree
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.merge
import kotlinx.serialization.json.JsonObject
import org.koin.core.Koin
import org.koin.core.module.Module
import project_group.project_name.features.common.client.models.EmptyConfig
import project_group.project_name.features.common.client.models.RootNodeFactoryGetter
import project_group.project_name.features.ui.auth.ui.AuthViewConfig
import project_group.project_name.features.ui.auth.ui.AuthViewInteractor
import project_group.project_name.features.ui.sample.ui.SampleViewConfig

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
                InjectNavigationChain<ViewConfig> {
                    InjectNavigationNode(
                        SampleViewConfig()
                    )
                }
            }
        }
    }
}
