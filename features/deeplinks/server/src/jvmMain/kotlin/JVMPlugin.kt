package dev.inmo.wishlist.features.deeplinks.server

import dev.inmo.micro_utils.startup.plugin.StartPlugin
import kotlinx.serialization.json.JsonObject
import org.koin.core.Koin
import org.koin.core.module.Module

/**
 * JVM server plugin for the deeplinks feature.
 *
 * Delegates to the deeplinks common JVM plugin (which registers the Exposed-backed `DeepLinksRepo`)
 * and to the server `Plugin` (service + the standard `/api` routing Element). Requires `features/common/server` (which
 * provides `Database`, `Json`, and the `KtorApplicationConfigurator` collection) loaded earlier.
 */
object JVMPlugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        with(dev.inmo.wishlist.features.deeplinks.common.JVMPlugin) { setupDI(config) }
        with(Plugin) { setupDI(config) }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
        dev.inmo.wishlist.features.deeplinks.common.JVMPlugin.startPlugin(koin)
        Plugin.startPlugin(koin)
    }
}