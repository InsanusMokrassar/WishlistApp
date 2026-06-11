package dev.inmo.wishlist.features.booking.server

import dev.inmo.micro_utils.koin.singleWithRandomQualifier
import dev.inmo.micro_utils.ktor.server.configurators.ApplicationRoutingConfigurator
import dev.inmo.micro_utils.startup.plugin.StartPlugin
import dev.inmo.wishlist.features.booking.server.configurators.BookingRoutingsConfigurator
import dev.inmo.wishlist.features.booking.server.services.BookingService
import kotlinx.serialization.json.JsonObject
import org.koin.core.Koin
import org.koin.core.module.Module

/**
 * Platform-agnostic startup plugin for the booking server module.
 *
 * Registers in Koin:
 * - [BookingService] — enforces booking visibility/authorization rules (owner-hidden, single-booking).
 * - [BookingRoutingsConfigurator] as [ApplicationRoutingConfigurator.Element].
 *
 * [BookingService] depends on the booking [dev.inmo.wishlist.features.booking.common.repo.BookingRepo]
 * (provided by the booking common JVM plugin) and on the wishlist read repos
 * [dev.inmo.wishlist.features.wishlist.common.repo.WishlistItemRepo] /
 * [dev.inmo.wishlist.features.wishlist.common.repo.WishlistRepo] (provided by the wishlist common
 * JVM plugin, loaded earlier in the server plugin list).
 */
object Plugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        single { BookingService(get(), get(), get()) }

        singleWithRandomQualifier<ApplicationRoutingConfigurator.Element> {
            BookingRoutingsConfigurator(get())
        }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
    }
}
