package project_group.project_name.features.auth.client

import dev.inmo.micro_utils.startup.plugin.StartPlugin
import kotlinx.serialization.json.JsonObject
import org.koin.core.Koin
import org.koin.core.module.Module

object JVMPlugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        with(project_group.project_name.features.auth.common.JVMPlugin) { setupDI(config) }
        with(Plugin) { setupDI(config) }

        single<ServerUrlStorage> { PreferencesServerUrlStorage() }
        single<AuthCredentialsStorage> { PreferencesAuthCredentialsStorage(get()) }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
        project_group.project_name.features.auth.common.JVMPlugin.startPlugin(koin)
        Plugin.startPlugin(koin)
    }
}