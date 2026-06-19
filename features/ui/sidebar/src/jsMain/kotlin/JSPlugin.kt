package dev.inmo.wishlist.features.ui.sidebar

import dev.inmo.micro_utils.koin.singleWithRandomQualifier
import dev.inmo.micro_utils.startup.plugin.StartPlugin
import dev.inmo.navigation.core.NavigationNodeFactory
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.ui.sidebar.ui.SidebarView
import dev.inmo.wishlist.features.ui.sidebar.ui.SidebarViewConfig
import kotlinx.serialization.json.JsonObject
import org.koin.core.Koin
import org.koin.core.module.Module

/**
 * JS startup plugin for the sidebar UI feature.
 *
 * Delegates to the common [Plugin] and registers the JS [SidebarView] factory so the navigation
 * system can instantiate it for [SidebarViewConfig] nodes. Only the web client loads this plugin;
 * the Android and Desktop clients keep their own navigation chrome.
 */
object JSPlugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        with(Plugin) { setupDI(config) }

        singleWithRandomQualifier<NavigationNodeFactory<ViewConfig>> {
            NavigationNodeFactory.Typed<SidebarViewConfig, ViewConfig> { chain, cfg ->
                SidebarView(chain, cfg)
            }
        }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
        Plugin.startPlugin(koin)
    }
}
