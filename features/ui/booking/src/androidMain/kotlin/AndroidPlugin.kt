package dev.inmo.wishlist.features.ui.booking

import dev.inmo.micro_utils.koin.singleWithRandomQualifier
import dev.inmo.micro_utils.startup.plugin.StartPlugin
import dev.inmo.navigation.core.NavigationNodeFactory
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.ui.booking.ui.BookingView
import dev.inmo.wishlist.features.ui.booking.ui.BookingViewConfig
import dev.inmo.wishlist.features.ui.booking.ui.MyPresentsView
import dev.inmo.wishlist.features.ui.booking.ui.MyPresentsViewConfig
import kotlinx.serialization.json.JsonObject
import org.koin.core.Koin
import org.koin.core.module.Module

/**
 * Android platform startup plugin for the booking UI scenario.
 *
 * Delegates to [Plugin] and registers [NavigationNodeFactory] entries for both Android
 * Compose-Material3 views (gift-booking view A and my-presents view B).
 */
object AndroidPlugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        with(Plugin) { setupDI(config) }

        singleWithRandomQualifier<NavigationNodeFactory<ViewConfig>> {
            NavigationNodeFactory.Typed<BookingViewConfig, ViewConfig> { chain, cfg -> BookingView(chain, cfg) }
        }
        singleWithRandomQualifier<NavigationNodeFactory<ViewConfig>> {
            NavigationNodeFactory.Typed<MyPresentsViewConfig, ViewConfig> { chain, cfg -> MyPresentsView(chain, cfg) }
        }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
        Plugin.startPlugin(koin)
    }
}
