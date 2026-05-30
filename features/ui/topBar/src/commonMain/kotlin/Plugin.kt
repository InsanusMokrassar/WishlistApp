package dev.inmo.wishlist.features.ui.topBar

import dev.inmo.micro_utils.koin.singleWithRandomQualifier
import dev.inmo.micro_utils.startup.plugin.StartPlugin
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.ui.topBar.ui.TopBarViewConfig
import dev.inmo.wishlist.features.ui.topBar.ui.TopBarViewModel
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.modules.SerializersModule
import org.koin.core.Koin
import org.koin.core.module.Module

/**
 * Platform-agnostic startup plugin for the top bar UI feature.
 *
 * Registers polymorphic serializer for [TopBarViewConfig] and the [TopBarViewModel] factory.
 */
object Plugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        singleWithRandomQualifier {
            SerializersModule {
                polymorphic(Any::class, TopBarViewConfig::class, TopBarViewConfig.serializer())
                polymorphic(ViewConfig::class, TopBarViewConfig::class, TopBarViewConfig.serializer())
            }
        }
        factory { TopBarViewModel(node = it.get(), interactor = get()) }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
    }
}
