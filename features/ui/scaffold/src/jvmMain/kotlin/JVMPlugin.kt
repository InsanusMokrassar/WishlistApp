package dev.inmo.wishlist.features.ui.scaffold

import dev.inmo.micro_utils.koin.singleWithRandomQualifier
import dev.inmo.micro_utils.startup.plugin.StartPlugin
import dev.inmo.navigation.core.NavigationNodeFactory
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.ui.scaffold.ui.ScaffoldView
import dev.inmo.wishlist.features.ui.scaffold.ui.ScaffoldViewConfig
import kotlinx.serialization.json.JsonObject
import org.koin.core.Koin
import org.koin.core.module.Module

/**
 * JVM platform startup plugin for the scaffold UI feature.
 *
 * Delegates to [Plugin] for common DI setup and registers the JVM [ScaffoldView]
 * factory so the navigation system can instantiate it for [ScaffoldViewConfig] nodes.
 */
object JVMPlugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        with(Plugin) { setupDI(config) }
        singleWithRandomQualifier<NavigationNodeFactory<ViewConfig>> {
            NavigationNodeFactory.Typed<ScaffoldViewConfig, ViewConfig> { chain, config ->
                ScaffoldView(chain, config)
            }
        }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
        Plugin.startPlugin(koin)
    }
}
