package dev.inmo.wishlist.features.ui.booking

import dev.inmo.micro_utils.koin.singleWithRandomQualifier
import dev.inmo.micro_utils.startup.plugin.StartPlugin
import dev.inmo.navigation.core.NavigationNodeFactory
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.ui.booking.ui.BookingView
import dev.inmo.wishlist.features.ui.booking.ui.BookingViewConfig
import dev.inmo.wishlist.features.ui.booking.ui.MyPresentsBooksView
import dev.inmo.wishlist.features.ui.booking.ui.MyPresentsBooksViewConfig
import kotlinx.serialization.json.JsonObject
import org.koin.core.Koin
import org.koin.core.module.Module

/**
 * JS platform startup plugin for the booking UI scenario.
 *
 * Delegates to [Plugin] and registers [NavigationNodeFactory] entries for both JS Compose-HTML
 * views (gift-booking view A and my-presents view B).
 */
object JSPlugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        with(Plugin) { setupDI(config) }

        singleWithRandomQualifier<NavigationNodeFactory<ViewConfig>> {
            NavigationNodeFactory.Typed<BookingViewConfig, ViewConfig> { chain, cfg -> BookingView(chain, cfg) }
        }
        singleWithRandomQualifier<NavigationNodeFactory<ViewConfig>> {
            NavigationNodeFactory.Typed<MyPresentsBooksViewConfig, ViewConfig> { chain, cfg -> MyPresentsBooksView(chain, cfg) }
        }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
        Plugin.startPlugin(koin)
    }
}
