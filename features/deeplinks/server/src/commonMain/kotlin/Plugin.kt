package dev.inmo.wishlist.features.deeplinks.server

import dev.inmo.micro_utils.koin.getAllDistinct
import dev.inmo.micro_utils.koin.singleWithRandomQualifier
import dev.inmo.micro_utils.ktor.server.configurators.KtorApplicationConfigurator
import dev.inmo.micro_utils.startup.plugin.StartPlugin
import dev.inmo.wishlist.features.deeplinks.server.configurators.DeepLinksRoutingConfigurator
import dev.inmo.wishlist.features.deeplinks.server.services.DeepLinksService
import kotlinx.serialization.json.JsonObject
import org.koin.core.Koin
import org.koin.core.module.Module

/**
 * Server startup plugin for the deeplinks feature.
 *
 * Registers in Koin:
 * - [DeepLinksService] — collects every registered
 *   [dev.inmo.wishlist.features.deeplinks.common.DeepLinkHandler] via `getAllDistinct` and mints /
 *   resolves deeplinks (server-only, no HTTP create endpoint).
 * - [DeepLinksRoutingConfigurator] as a [KtorApplicationConfigurator] (NOT an
 *   `ApplicationRoutingConfigurator.Element`) so `GET /links/{deeplink_uuid}` is served at the site
 *   root, not under `/api`.
 *
 * The `DeepLinksRepo` consumed by [DeepLinksService] is provided by the deeplinks common JVM plugin
 * (the deeplinks server JVM plugin delegates to it first).
 */
object Plugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        single { DeepLinksService(get(), getAllDistinct()) }

        singleWithRandomQualifier<KtorApplicationConfigurator> {
            DeepLinksRoutingConfigurator(get())
        }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
    }
}
