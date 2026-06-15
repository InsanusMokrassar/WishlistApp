package dev.inmo.wishlist.features.deeplinks.client

import dev.inmo.wishlist.features.deeplinks.common.AndroidPlugin
import dev.inmo.wishlist.features.deeplinks.common.AndroidPlugin.setupDI
import dev.inmo.micro_utils.startup.plugin.StartPlugin
import kotlinx.serialization.json.JsonObject
import org.koin.core.Koin
import org.koin.core.module.Module

object AndroidPlugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        with(dev.inmo.wishlist.features.deeplinks.common.AndroidPlugin) { setupDI(config) }
        with(Plugin) { setupDI(config) }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
        dev.inmo.wishlist.features.deeplinks.common.AndroidPlugin.startPlugin(koin)
        Plugin.startPlugin(koin)
    }
}