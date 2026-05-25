package project_group.project_name.{{$module_package}}

import dev.inmo.micro_utils.koin.singleWithRandomQualifier
import dev.inmo.micro_utils.startup.plugin.StartPlugin
import dev.inmo.navigation.core.NavigationNodeFactory
import kotlinx.serialization.json.JsonObject
import org.koin.core.Koin
import org.koin.core.module.Module
import project_group.project_name.features.common.client.models.ViewConfig
import project_group.project_name.{{$module_package}}.ui.{{$module_ui_name}}View
import project_group.project_name.{{$module_package}}.ui.{{$module_ui_name}}ViewConfig

object JVMPlugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        with(Plugin) { setupDI(config) }

        singleWithRandomQualifier<NavigationNodeFactory<ViewConfig>> {
            NavigationNodeFactory.Typed<{{$module_ui_name}}ViewConfig, ViewConfig> { chain, config ->
                {{$module_ui_name}}View(chain, config)
            }
        }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
        Plugin.startPlugin(koin)
    }
}
