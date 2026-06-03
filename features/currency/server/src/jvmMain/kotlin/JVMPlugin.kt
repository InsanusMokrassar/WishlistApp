package dev.inmo.wishlist.features.currency.server

import dev.inmo.micro_utils.startup.plugin.StartPlugin
import kotlinx.serialization.json.JsonObject
import org.koin.core.Koin
import org.koin.core.module.Module

/**
 * JVM entry-point startup plugin for the currency server feature, listed in the server config's
 * `plugins` array. Delegates DI setup and startup to the common currency module plugin and the
 * server [Plugin] which performs the actual wiring.
 */
object JVMPlugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        with(dev.inmo.wishlist.features.currency.common.JVMPlugin) { setupDI(config) }
        with(Plugin) { setupDI(config) }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
        dev.inmo.wishlist.features.currency.common.JVMPlugin.startPlugin(koin)
        Plugin.startPlugin(koin)
    }
}