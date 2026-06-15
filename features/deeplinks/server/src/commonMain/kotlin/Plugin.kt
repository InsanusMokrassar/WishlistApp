package dev.inmo.wishlist.features.deeplinks.server

import dev.inmo.micro_utils.koin.getAllDistinct
import dev.inmo.micro_utils.koin.singleWithRandomQualifier
import dev.inmo.micro_utils.ktor.server.configurators.ApplicationRoutingConfigurator
import dev.inmo.micro_utils.startup.plugin.StartPlugin
import dev.inmo.wishlist.features.deeplinks.server.configurators.DeepLinksRoutingsConfigurator
import dev.inmo.wishlist.features.deeplinks.server.services.DeepLinksService
import kotlinx.serialization.json.JsonObject
import org.koin.core.Koin
import org.koin.core.module.Module

/**
 * Platform-agnostic startup plugin for the deeplinks server feature.
 *
 * Registers into the shared DI graph:
 * - [DeepLinksService] bound as the server-only [DeepLinksFeature]; it collects every registered
 *   [DeepLinkHandler] via `getAllDistinct()`.
 * - [DeepLinksRoutingsConfigurator] exposing the public `links/{deeplinkId}` resolution route.
 *
 * The [dev.inmo.wishlist.features.deeplinks.server.repo.DeepLinksRepo] binding (JVM-only, Exposed)
 * is added by [JVMPlugin]; `Json` is provided by `features/common`.
 */
object Plugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        single {
            DeepLinksService(
                json = get(),
                repo = get(),
                handlers = getAllDistinct()
            )
        }
        single<DeepLinksFeature> { get<DeepLinksService>() }

        singleWithRandomQualifier<ApplicationRoutingConfigurator.Element> {
            DeepLinksRoutingsConfigurator(get())
        }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
    }
}
