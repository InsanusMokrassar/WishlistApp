package dev.inmo.wishlist.client

import androidx.compose.runtime.collectAsState
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import dev.inmo.micro_utils.startup.plugin.StartPlugin
import dev.inmo.navigation.core.repo.NavigationConfigsRepo
import kotlinx.coroutines.CompletableJob
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import kotlinx.serialization.json.JsonObject
import org.koin.core.Koin
import org.koin.core.module.Module

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
