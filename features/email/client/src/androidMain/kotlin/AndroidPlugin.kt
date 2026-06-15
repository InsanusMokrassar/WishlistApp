package dev.inmo.wishlist.features.email.client

import dev.inmo.micro_utils.startup.plugin.StartPlugin
import kotlinx.serialization.json.JsonObject
import org.koin.core.Koin
import org.koin.core.module.Module

/**
 * Android client startup plugin for the email feature. Delegates to the common email Android plugin and
 * the common client [Plugin].
 */
object AndroidPlugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        with(dev.inmo.wishlist.features.email.common.AndroidPlugin) { setupDI(config) }
        with(Plugin) { setupDI(config) }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
        dev.inmo.wishlist.features.email.common.AndroidPlugin.startPlugin(koin)
        Plugin.startPlugin(koin)
    }
}