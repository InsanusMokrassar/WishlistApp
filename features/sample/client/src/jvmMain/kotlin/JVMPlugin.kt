package project_group.project_name.features.sample.client

import project_group.project_name.features.sample.common.JVMPlugin.setupDI
import dev.inmo.micro_utils.startup.plugin.StartPlugin
import kotlinx.serialization.json.JsonObject
import org.koin.core.Koin
import org.koin.core.module.Module

object JVMPlugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        with(project_group.project_name.features.sample.common.JVMPlugin) { setupDI(config) }
        with(Plugin) { setupDI(config) }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
        project_group.project_name.features.sample.common.JVMPlugin.startPlugin(koin)
        Plugin.startPlugin(koin)
    }
}