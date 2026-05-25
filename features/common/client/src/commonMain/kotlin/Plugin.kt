package project_group.project_name.features.common.client

import dev.inmo.micro_utils.coroutines.runCatchingLogging
import dev.inmo.micro_utils.koin.getAllDistinct
import dev.inmo.micro_utils.koin.singleWithRandomQualifier
import dev.inmo.micro_utils.startup.plugin.StartPlugin
import dev.inmo.navigation.core.NavigationNode
import dev.inmo.navigation.core.NavigationNodeFactory
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngineFactory
import project_group.project_name.features.common.client.models.ViewConfig
import kotlinx.serialization.json.JsonObject
import org.koin.core.Koin
import org.koin.core.module.Module
import project_group.project_name.features.common.client.configurators.GZipHttpClientConfigurator
import project_group.project_name.features.common.client.configurators.HttpClientConfigurator
import project_group.project_name.features.common.client.configurators.SerializationConfigurator
import project_group.project_name.features.common.client.configurators.TimeoutsHttpClientConfigurator
import project_group.project_name.features.common.client.echo.EchoFeature
import project_group.project_name.features.common.client.echo.KtorEchoFeature
import project_group.project_name.features.common.client.models.EmptyConfig

object Plugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        singleWithRandomQualifier<NavigationNodeFactory<ViewConfig>> {
            NavigationNodeFactory.Typed(
                EmptyConfig::class
            ) { navigationChain, config ->
                NavigationNode.Empty(navigationChain, config)
            }
        }

        singleWithRandomQualifier<HttpClientConfigurator> { SerializationConfigurator(get()) }
        singleWithRandomQualifier<HttpClientConfigurator> { GZipHttpClientConfigurator() }
        singleWithRandomQualifier<HttpClientConfigurator> { TimeoutsHttpClientConfigurator() }

        single {
            val configurators = getAllDistinct<HttpClientConfigurator>()
            val engineFactory = getOrNull<HttpClientEngineFactory<*>>()
            val configurationLambda: HttpClientConfig<*>.() -> Unit = {
                configurators.forEach {
                    with (it) {
                        runCatchingLogging {
                            configure()
                        }
                    }
                }
            }
            if (engineFactory == null) {
                HttpClient(configurationLambda)
            } else {
                HttpClient(engineFactory, configurationLambda)
            }
        }

        single { KtorEchoFeature(get()) }
        single<EchoFeature> { get<KtorEchoFeature>() }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
    }
}
