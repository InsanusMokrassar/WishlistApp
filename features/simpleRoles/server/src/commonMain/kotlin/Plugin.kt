package dev.inmo.wishlist.features.simpleRoles.server

import dev.inmo.kroles.repos.ReadRolesRepo
import dev.inmo.micro_utils.koin.singleWithRandomQualifier
import dev.inmo.micro_utils.ktor.server.configurators.ApplicationRoutingConfigurator
import dev.inmo.micro_utils.startup.plugin.StartPlugin
import dev.inmo.wishlist.features.simpleRoles.server.configurators.SimpleRolesRoutingsConfigurator
import dev.inmo.wishlist.features.simpleRoles.server.services.SimpleRolesFeatureService
import kotlinx.serialization.json.JsonObject
import org.koin.core.Koin
import org.koin.core.module.Module

/**
 * Common (JVM) startup plugin for the `simpleRoles` server feature.
 *
 * Registers [SimpleRolesFeatureService] bound to [SimpleRolesFeature], and
 * [SimpleRolesRoutingsConfigurator] as an [ApplicationRoutingConfigurator.Element] with a random
 * qualifier so Ktor picks it up automatically.
 */
object Plugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        single { SimpleRolesFeatureService(get<ReadRolesRepo>()) }
        single<SimpleRolesFeature> { get<SimpleRolesFeatureService>() }

        singleWithRandomQualifier<ApplicationRoutingConfigurator.Element> {
            SimpleRolesRoutingsConfigurator(get())
        }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
    }
}
