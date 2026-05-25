package project_group.project_name.features.sample.client

import dev.inmo.micro_utils.startup.plugin.StartPlugin
import kotlinx.serialization.json.JsonObject
import org.koin.core.Koin
import org.koin.core.module.Module

object Plugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        single { KtorSampleFeature(get()) }
        single<SampleFeature> { get<KtorSampleFeature>() }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
    }
}