package dev.inmo.wishlist.features.common.server

import dev.inmo.micro_utils.koin.singleWithRandomQualifier
import dev.inmo.micro_utils.ktor.server.configurators.ApplicationRoutingConfigurator
import dev.inmo.micro_utils.startup.plugin.StartPlugin
import kotlinx.serialization.json.JsonObject
import org.koin.core.Koin
import org.koin.core.module.Module
import dev.inmo.wishlist.features.common.server.echo.EchoFeature
import dev.inmo.wishlist.features.common.server.echo.configurators.EchoRoutingsConfigurator
import dev.inmo.wishlist.features.common.server.echo.services.SimpleEchoFeatureService

object Plugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        single { SimpleEchoFeatureService() }
        single<EchoFeature> { get<SimpleEchoFeatureService>() }

        singleWithRandomQualifier<ApplicationRoutingConfigurator.Element> {
            EchoRoutingsConfigurator(get())
        }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
    }
}
