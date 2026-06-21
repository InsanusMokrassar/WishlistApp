package dev.inmo.wishlist.features.deeplinks.server

import dev.inmo.micro_utils.startup.plugin.StartPlugin
import dev.inmo.wishlist.features.deeplinks.server.repo.DeepLinksRepo
import dev.inmo.wishlist.features.deeplinks.server.repo.ExposedDeepLinksRepo
import kotlinx.serialization.json.JsonObject
import org.koin.core.Koin
import org.koin.core.module.Module

/**
 * JVM entry-point plugin for the deeplinks server module.
 *
 * Adds the [ExposedDeepLinksRepo] binding on top of the common [Plugin] registrations.
 * Must be loaded after `features/common/server.JVMPlugin` (provides [org.jetbrains.exposed.v1.jdbc.Database]
 * and [kotlinx.serialization.json.Json]) and after
 * `features/deeplinks/common.JVMPlugin` (provides shared models).
 */
object JVMPlugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        with(dev.inmo.wishlist.features.deeplinks.common.JVMPlugin) { setupDI(config) }
        with(Plugin) { setupDI(config) }
        single<DeepLinksRepo> { ExposedDeepLinksRepo(get(), get()) }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
        dev.inmo.wishlist.features.deeplinks.common.JVMPlugin.startPlugin(koin)
        Plugin.startPlugin(koin)
    }
}
