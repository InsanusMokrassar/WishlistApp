package project_group.project_name.features.auth.client

import dev.inmo.micro_utils.koin.singleWithRandomQualifier
import dev.inmo.micro_utils.startup.plugin.StartPlugin
import kotlinx.serialization.json.JsonObject
import org.koin.core.Koin
import org.koin.core.module.Module
import project_group.project_name.features.auth.client.configurators.BearerAuthHttpClientConfigurator
import project_group.project_name.features.auth.client.configurators.DefaultUrlHttpClientConfigurator
import project_group.project_name.features.auth.common.AuthFeature
import project_group.project_name.features.common.client.configurators.HttpClientConfigurator

object Plugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        singleWithRandomQualifier<HttpClientConfigurator> {
            DefaultUrlHttpClientConfigurator(get())
        }

        singleWithRandomQualifier<HttpClientConfigurator> {
            BearerAuthHttpClientConfigurator(get())
        }

        single { KtorAuthFeature(get()) }
        single { AuthFeatureService(get(), get<KtorAuthFeature>()) }
        single<ClientAuthFeature> { get<AuthFeatureService>() }
        single<AuthFeature> { get<AuthFeatureService>() }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
    }
}
