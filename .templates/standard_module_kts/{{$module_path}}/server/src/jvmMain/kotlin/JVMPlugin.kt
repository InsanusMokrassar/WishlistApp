package project_group.project_name.{{$module_package}}.server

import dev.inmo.micro_utils.startup.plugin.StartPlugin
import kotlinx.serialization.json.JsonObject
import org.koin.core.Koin
import org.koin.core.module.Module

object JVMPlugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        with(project_group.project_name.{{$module_package}}.common.JVMPlugin) { setupDI(config) }
        with(Plugin) { setupDI(config) }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
        project_group.project_name.{{$module_package}}.common.JVMPlugin.startPlugin(koin)
        Plugin.startPlugin(koin)
    }
}
