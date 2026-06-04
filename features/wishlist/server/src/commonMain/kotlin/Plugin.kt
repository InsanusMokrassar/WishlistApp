package dev.inmo.wishlist.features.wishlist.server

import dev.inmo.micro_utils.koin.singleWithRandomQualifier
import dev.inmo.micro_utils.ktor.server.configurators.ApplicationRoutingConfigurator
import dev.inmo.micro_utils.startup.plugin.StartPlugin
import dev.inmo.wishlist.features.wishlist.server.configurators.BookingRoutingsConfigurator
import dev.inmo.wishlist.features.wishlist.server.configurators.WishlistItemRoutingsConfigurator
import dev.inmo.wishlist.features.wishlist.server.configurators.WishlistRoutingsConfigurator
import dev.inmo.wishlist.features.wishlist.server.services.BookingService
import dev.inmo.wishlist.features.wishlist.server.services.WishlistItemService
import dev.inmo.wishlist.features.wishlist.server.services.WishlistService
import kotlinx.serialization.json.JsonObject
import org.koin.core.Koin
import org.koin.core.module.Module

/**
 * Platform-agnostic startup plugin for the wishlist server module.
 *
 * Registers in Koin:
 * - [WishlistService] — enforces caller ownership on mutable wishlist operations
 * - [WishlistItemService] — enforces parent-wishlist ownership on mutable item operations
 * - [BookingService] — enforces booking visibility/authorization rules (owner-hidden, single-booking)
 * - [WishlistRoutingsConfigurator] as [ApplicationRoutingConfigurator.Element]
 * - [WishlistItemRoutingsConfigurator] as [ApplicationRoutingConfigurator.Element]
 * - [BookingRoutingsConfigurator] as [ApplicationRoutingConfigurator.Element]
 *
 * Neither [WishlistService] nor [WishlistItemService] is bound to a client-facing feature
 * interface because their mutation methods carry an explicit caller
 * [dev.inmo.wishlist.features.users.common.models.UserId] parameter that is absent from
 * the client-facing interfaces.
 *
 * Use [JVMPlugin] on JVM targets to also wire in the Exposed repo layer.
 */
object Plugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        single { WishlistService(get()) }

        single { WishlistItemService(get(), get()) }

        single { BookingService(get(), get(), get()) }

        singleWithRandomQualifier<ApplicationRoutingConfigurator.Element> {
            WishlistRoutingsConfigurator(get())
        }
        singleWithRandomQualifier<ApplicationRoutingConfigurator.Element> {
            WishlistItemRoutingsConfigurator(get())
        }
        singleWithRandomQualifier<ApplicationRoutingConfigurator.Element> {
            BookingRoutingsConfigurator(get())
        }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
    }
}
