package project_group.project_name.features.common.client

import dev.inmo.micro_utils.startup.plugin.StartPlugin
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.android.Android
import kotlinx.serialization.json.JsonObject
import org.koin.core.Koin
import org.koin.core.module.Module

object AndroidPlugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        with(Plugin) { setupDI(config) }
        single<HttpClientEngineFactory<*>> { Android }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
        Plugin.startPlugin(koin)
    }
}
