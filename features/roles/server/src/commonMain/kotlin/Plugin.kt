package dev.inmo.wishlist.features.roles.server

import dev.inmo.micro_utils.koin.singleWithRandomQualifier
import dev.inmo.micro_utils.ktor.server.configurators.ApplicationRoutingConfigurator
import dev.inmo.micro_utils.startup.plugin.StartPlugin
import dev.inmo.wishlist.features.roles.server.configurators.RolesRoutingsConfigurator
import dev.inmo.wishlist.features.roles.server.services.RolesFeatureService
import kotlinx.serialization.json.JsonObject
import org.koin.core.Koin
import org.koin.core.module.Module

/**
 * Common (JVM) startup plugin for the `roles` server feature.
 *
 * Registers [RolesFeatureService] bound to [RolesFeature] (consulting `FeatureRolesRegistry` +
 * `ReadRolesRepo`, both wired by `roles/common`), and [RolesRoutingsConfigurator] as an
 * [ApplicationRoutingConfigurator.Element] so the `/roles/isFunctionalityAvailable/{...}` endpoint is
 * served.
 */
object Plugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        single<RolesFeature> { RolesFeatureService(get(), get()) }
        singleWithRandomQualifier<ApplicationRoutingConfigurator.Element> {
            RolesRoutingsConfigurator(get())
        }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
    }
}
