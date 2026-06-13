package dev.inmo.wishlist.features.sample.server

import dev.inmo.micro_utils.koin.singleWithRandomQualifier
import dev.inmo.micro_utils.ktor.server.configurators.ApplicationRoutingConfigurator
import dev.inmo.micro_utils.startup.plugin.StartPlugin
import kotlinx.serialization.json.JsonObject
import org.koin.core.Koin
import org.koin.core.module.Module
import dev.inmo.wishlist.features.sample.server.configurators.SampleRoutingsConfigurator
import dev.inmo.wishlist.features.sample.server.services.SimpleSampleFeatureService
import kotlin.random.Random
import kotlin.random.nextUInt

object Plugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        single {
            SimpleSampleFeatureService(
                "Sample text from server, random part: ${Random.nextUInt()}"
            )
        }

        single<SampleFeature> { get<SimpleSampleFeatureService>() }

        singleWithRandomQualifier<ApplicationRoutingConfigurator.Element> {
            SampleRoutingsConfigurator(get())
        }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
    }
}