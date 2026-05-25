package project_group.project_name.client

import android.app.Application
import android.content.Context
import android.content.res.Resources
import dev.inmo.micro_utils.startup.plugin.StartPlugin
import dev.inmo.navigation.core.repo.NavigationConfigsRepo
import kotlinx.serialization.json.JsonObject
import org.koin.core.Koin
import org.koin.core.module.Module
import project_group.project_name.client.ClientPlugin
import project_group.project_name.features.common.client.models.ViewConfig

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
    }
}