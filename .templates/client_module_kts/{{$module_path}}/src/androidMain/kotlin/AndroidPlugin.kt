package dev.inmo.wishlist.{{$module_package}}

import dev.inmo.micro_utils.koin.singleWithRandomQualifier
import dev.inmo.micro_utils.startup.plugin.StartPlugin
import dev.inmo.navigation.core.NavigationNodeFactory
import kotlinx.serialization.json.JsonObject
import org.koin.core.Koin
import org.koin.core.module.Module
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.{{$module_package}}.ui.{{$module_ui_name}}View
import dev.inmo.wishlist.{{$module_package}}.ui.{{$module_ui_name}}ViewConfig

object AndroidPlugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        with(Plugin) { setupDI(config) }

        singleWithRandomQualifier<NavigationNodeFactory<ViewConfig>> {
            NavigationNodeFactory.Typed<{{$module_ui_name}}ViewConfig, ViewConfig> { chain, config ->
                {{$module_ui_name}}View(chain, config)
            }
        }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
        Plugin.startPlugin(koin)
    }
}
