package dev.inmo.wishlist.features.common.client

import dev.inmo.wishlist.features.common.common.JSPlugin
import dev.inmo.micro_utils.startup.plugin.StartPlugin
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.js.Js
import kotlinx.serialization.json.JsonObject
import org.koin.core.Koin
import org.koin.core.module.Module

object JSPlugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        with(JSPlugin) { setupDI(config) }
        with(Plugin) { setupDI(config) }

        single<HttpClientEngineFactory<*>> { Js }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
        JSPlugin.startPlugin(koin)
        Plugin.startPlugin(koin)
    }
}
