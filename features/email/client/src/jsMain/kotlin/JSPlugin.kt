package dev.inmo.wishlist.features.email.client

import dev.inmo.micro_utils.startup.plugin.StartPlugin
import kotlinx.serialization.json.JsonObject
import org.koin.core.Koin
import org.koin.core.module.Module

/**
 * JS client startup plugin for the email feature. Delegates to the common email JS plugin and the common
 * client [Plugin].
 */
object JSPlugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        with(dev.inmo.wishlist.features.email.common.JSPlugin) { setupDI(config) }
        with(Plugin) { setupDI(config) }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
        dev.inmo.wishlist.features.email.common.JSPlugin.startPlugin(koin)
        Plugin.startPlugin(koin)
    }
}