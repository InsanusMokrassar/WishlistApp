package dev.inmo.wishlist.features.wishlist.server

import dev.inmo.micro_utils.startup.plugin.StartPlugin
import dev.inmo.wishlist.features.wishlist.server.services.WishlistCopyService
import kotlinx.serialization.json.JsonObject
import org.koin.core.Koin
import org.koin.core.module.Module

/**
 * JVM-specific startup plugin for the wishlist server module.
 *
 * Delegates DI setup to both the common JVM plugin (Exposed repos) and the
 * platform-agnostic server [Plugin] (services + routing configurators).
 * After both are started, boots the [WishlistCopyService] background worker so queued
 * whole-wishlist copy jobs are resumed and processed concurrently.
 * Entry point registered in `sample.config.json`.
 */
object JVMPlugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        with(dev.inmo.wishlist.features.wishlist.common.JVMPlugin) { setupDI(config) }
        with(Plugin) { setupDI(config) }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
        dev.inmo.wishlist.features.wishlist.common.JVMPlugin.startPlugin(koin)
        Plugin.startPlugin(koin)
        koin.get<WishlistCopyService>().start()
    }
}
