package dev.inmo.wishlist.features.ui.scaffold

import dev.inmo.micro_utils.koin.singleWithRandomQualifier
import dev.inmo.micro_utils.startup.plugin.StartPlugin
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.ui.scaffold.ui.ScaffoldViewConfig
import dev.inmo.wishlist.features.ui.scaffold.ui.ScaffoldViewModel
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.modules.SerializersModule
import org.koin.core.Koin
import org.koin.core.module.Module

/**
 * Platform-agnostic startup plugin for the scaffold UI feature.
 *
 * Registers in Koin:
 * - Polymorphic serializer for [ScaffoldViewConfig]
 * - Koin factory for [ScaffoldViewModel]
 *
 * Platform plugins delegate to this object and register their [NavigationNodeFactory] entries.
 */
object Plugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        singleWithRandomQualifier {
            SerializersModule {
                polymorphic(Any::class, ScaffoldViewConfig::class, ScaffoldViewConfig.serializer())
                polymorphic(ViewConfig::class, ScaffoldViewConfig::class, ScaffoldViewConfig.serializer())
            }
        }
        factory { ScaffoldViewModel(it.get()) }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
    }
}
