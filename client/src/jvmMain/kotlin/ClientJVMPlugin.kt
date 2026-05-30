package dev.inmo.wishlist.client

import androidx.compose.runtime.collectAsState
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import dev.inmo.micro_utils.startup.plugin.StartPlugin
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.core.repo.NavigationConfigsRepo
import dev.inmo.wishlist.features.auth.client.ServerUrlStorage
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.ui.scaffold.ui.ScaffoldViewConfig
import dev.inmo.wishlist.features.ui.serverUrl.ui.ServerUrlViewConfig
import kotlinx.coroutines.CompletableJob
import kotlinx.serialization.json.JsonObject
import org.koin.core.Koin
import org.koin.core.module.Module

/**
 * JVM (Desktop) shell.
 *
 * Bootstraps the root chain: if a server URL is already saved, jumps straight to
 * the main scaffold; otherwise pushes [ServerUrlViewConfig] so the user must
 * configure the URL before reaching the main page.
 *
 * @param rootJob Completable job released when the window closes.
 */
class ClientJVMPlugin(
    private val rootJob: CompletableJob
) : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        with(ClientPlugin) { setupDI(config) }

        single<NavigationConfigsRepo<ViewConfig>> {
            NavigationConfigsRepo.InMemory<ViewConfig>()
        }
    }

    override suspend fun startPlugin(koin: Koin) {
        ClientPlugin.startPlugin(koin)
        super.startPlugin(koin)
        val rootChain = koin.get<NavigationChain<ViewConfig>>()
        val storage = koin.get<ServerUrlStorage>()
        val savedUrl = storage.getServerUrl()
        if (savedUrl.isNullOrBlank()) {
            if (rootChain.stackFlow.value.none { it.config is ServerUrlViewConfig }) {
                rootChain.push(ServerUrlViewConfig())
            }
        } else if (rootChain.stackFlow.value.none { it.config is ScaffoldViewConfig }) {
            rootChain.push(ClientPlugin.mainScaffoldConfig)
        }
        application {
            Window(
                onCloseRequest = { rootJob.cancel(); exitApplication() },
                title = "wishlist",
            ) {
                ClientPlugin.currentDrawingBlock.collectAsState().value.invoke()
            }
        }
    }
}
