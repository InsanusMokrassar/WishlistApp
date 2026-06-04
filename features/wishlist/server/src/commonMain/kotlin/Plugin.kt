package dev.inmo.wishlist.features.wishlist.server

import dev.inmo.micro_utils.koin.singleWithRandomQualifier
import dev.inmo.micro_utils.ktor.server.configurators.ApplicationRoutingConfigurator
import dev.inmo.micro_utils.startup.plugin.StartPlugin
import dev.inmo.wishlist.features.wishlist.server.configurators.WishlistItemRoutingsConfigurator
import dev.inmo.wishlist.features.wishlist.server.configurators.WishlistRoutingsConfigurator
import dev.inmo.wishlist.features.wishlist.server.services.WishlistCopyService
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
 * - [WishlistCopyService] — persistent background queue worker for whole-wishlist deep copies
 * - [WishlistRoutingsConfigurator] as [ApplicationRoutingConfigurator.Element]
 * - [WishlistItemRoutingsConfigurator] as [ApplicationRoutingConfigurator.Element]
 *
 * [WishlistCopyService] depends on JVM-only repo bindings; its worker is started from
 * [JVMPlugin.startPlugin] (after the common JVM repo plugin has registered them).
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

        single { WishlistCopyService(get(), get(), get(), get()) }

        singleWithRandomQualifier<ApplicationRoutingConfigurator.Element> {
            WishlistRoutingsConfigurator(get(), get())
        }
        singleWithRandomQualifier<ApplicationRoutingConfigurator.Element> {
            WishlistItemRoutingsConfigurator(get())
        }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
    }
}
