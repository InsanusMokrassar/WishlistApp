package dev.inmo.wishlist.features.email.client

import dev.inmo.wishlist.features.email.common.JVMPlugin.setupDI
import dev.inmo.micro_utils.startup.plugin.StartPlugin
import kotlinx.serialization.json.JsonObject
import org.koin.core.Koin
import org.koin.core.module.Module

/** JVM-specific startup plugin for the email client feature. Delegates to the shared [Plugin]. */
object JVMPlugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        with(dev.inmo.wishlist.features.email.common.JVMPlugin) { setupDI(config) }
        with(Plugin) { setupDI(config) }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
        dev.inmo.wishlist.features.email.common.JVMPlugin.startPlugin(koin)
        Plugin.startPlugin(koin)
    }
}