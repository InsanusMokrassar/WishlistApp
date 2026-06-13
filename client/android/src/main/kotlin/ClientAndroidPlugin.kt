package dev.inmo.wishlist.client

import android.app.Application
import android.content.Context
import android.content.res.Resources
import dev.inmo.micro_utils.coroutines.subscribeLoggingDropExceptions
import dev.inmo.micro_utils.startup.plugin.StartPlugin
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.core.extensions.changesInSubTreeFlow
import dev.inmo.navigation.core.extensions.findNodeInSubTree
import dev.inmo.navigation.core.repo.NavigationConfigsRepo
import dev.inmo.wishlist.features.auth.client.ServerUrlStorage
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.ui.scaffold.ui.ScaffoldViewConfig
import dev.inmo.wishlist.features.ui.serverUrl.ui.ServerUrlViewConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.merge
import kotlinx.serialization.json.JsonObject
import org.koin.core.Koin
import org.koin.core.module.Module

/**
 * Android shell. Bootstraps the root chain identically to [ClientJVMPlugin] —
 * server URL editor first when no URL is saved, otherwise the main scaffold.
 *
 * @param mainActivity Hosts the Compose content and resources.
 */
class ClientAndroidPlugin(
    private val mainActivity: MainActivity
) : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        with(ClientPlugin) { setupDI(config) }

        single<NavigationConfigsRepo<ViewConfig>> {
            NavigationConfigsRepo.InMemory<ViewConfig>()
        }
        single { mainActivity }
        single<Context> { mainActivity }
        single<Resources> { mainActivity.resources }
        single<Application> { mainActivity.application }
    }

    override suspend fun startPlugin(koin: Koin) {
        ClientPlugin.startPlugin(koin)
        super.startPlugin(koin)
        val rootChain = koin.get<NavigationChain<ViewConfig>>()
        val storage = koin.get<ServerUrlStorage>()
        val scope = koin.get<CoroutineScope>()
        rootChain.changesInSubTreeFlow().subscribeLoggingDropExceptions(scope) {
            val scaffoldView = rootChain.findNodeInSubTree { it.config is ScaffoldViewConfig }
            val savedUrl = storage.getServerUrl()
            if (scaffoldView != null && savedUrl == null && scaffoldView.chain.stackFlow.value.none { it.config is ServerUrlViewConfig }) {
                scaffoldView.chain.push(
                    ServerUrlViewConfig()
                )
            }
        }
    }
}
