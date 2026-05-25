package project_group.project_name.features.auth.server

import dev.inmo.micro_utils.koin.singleWithRandomQualifier
import dev.inmo.micro_utils.ktor.server.configurators.ApplicationRoutingConfigurator
import dev.inmo.micro_utils.startup.plugin.StartPlugin
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.koin.core.Koin
import org.koin.core.module.Module
import project_group.project_name.features.auth.common.AuthFeature
import project_group.project_name.features.auth.server.ServerAuthFeature
import project_group.project_name.features.auth.server.configurators.AuthRoutingsConfigurator
import project_group.project_name.features.auth.server.configurators.BearerAuthenticationConfigurator
import project_group.project_name.features.auth.server.services.AuthFeatureService
import project_group.project_name.features.common.server.configurators.ApplicationAuthenticationConfigurator

object Plugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        single { get<Json>().decodeFromJsonElement(Config.serializer(), config) }
        single {
            val config = get<Config>()
            AuthFeatureService(get(), get(), config.tokenTtl, config.refreshTokenTtl)
        }
        single<ServerAuthFeature> { get<AuthFeatureService>() }
        single<AuthFeature> { get<AuthFeatureService>() }

        singleWithRandomQualifier<ApplicationRoutingConfigurator.Element> {
            AuthRoutingsConfigurator(get())
        }
        singleWithRandomQualifier<ApplicationAuthenticationConfigurator.Element> {
            BearerAuthenticationConfigurator(get())
        }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
    }
}