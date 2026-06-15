package dev.inmo.wishlist.features.deeplinks.server

import dev.inmo.micro_utils.startup.plugin.StartPlugin
import dev.inmo.wishlist.features.deeplinks.server.repo.DeepLinksRepo
import dev.inmo.wishlist.features.deeplinks.server.repo.ExposedDeepLinksRepo
import kotlinx.serialization.json.JsonObject
import org.koin.core.Koin
import org.koin.core.module.Module

/**
 * JVM-specific startup plugin for the deeplinks server module, listed in the server config's
 * `plugins` array.
 *
 * Adds the JVM-only [DeepLinksRepo] binding (Exposed-backed [ExposedDeepLinksRepo]) to the DI graph;
 * `Database` and `Json` are resolved from the graph populated by `features/common/server`. Delegates
 * the rest of the wiring (service, routing) to the common [Plugin] and the common-module plugin.
 */
object JVMPlugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        with(dev.inmo.wishlist.features.deeplinks.common.JVMPlugin) { setupDI(config) }
        with(Plugin) { setupDI(config) }

        single<DeepLinksRepo> {
            ExposedDeepLinksRepo(get(), get())
        }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
        dev.inmo.wishlist.features.deeplinks.common.JVMPlugin.startPlugin(koin)
        Plugin.startPlugin(koin)
    }
}
