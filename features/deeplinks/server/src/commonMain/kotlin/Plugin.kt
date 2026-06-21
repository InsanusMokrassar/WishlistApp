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
 * Server-common plugin for the deeplinks feature.
 * Registers [DeepLinksService], the [DeepLinksFeature] binding, and [DeepLinksRoutingsConfigurator].
 *
 * Requires [dev.inmo.wishlist.features.deeplinks.server.repo.DeepLinksRepo] to be registered
 * before the DI graph is resolved — supplied by
 * [dev.inmo.wishlist.features.deeplinks.server.JVMPlugin] for JVM targets.
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
