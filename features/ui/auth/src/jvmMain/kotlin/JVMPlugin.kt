package project_group.project_name.features.ui.auth

import dev.inmo.micro_utils.koin.singleWithRandomQualifier
import dev.inmo.micro_utils.startup.plugin.StartPlugin
import dev.inmo.navigation.core.NavigationNodeFactory
import kotlinx.serialization.json.JsonObject
import org.koin.core.Koin
import org.koin.core.module.Module
import project_group.project_name.features.common.client.models.ViewConfig
import project_group.project_name.features.ui.auth.ui.AuthView
import project_group.project_name.features.ui.auth.ui.AuthViewConfig

object JVMPlugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        with(Plugin) { setupDI(config) }

        singleWithRandomQualifier<NavigationNodeFactory<ViewConfig>> {
            NavigationNodeFactory.Typed<AuthViewConfig, ViewConfig> { chain, config ->
                AuthView(chain, config)
            }
        }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
        Plugin.startPlugin(koin)
    }
}