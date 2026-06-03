package dev.inmo.wishlist.features.currency.server

import dev.inmo.micro_utils.koin.singleWithRandomQualifier
import dev.inmo.micro_utils.ktor.server.configurators.ApplicationRoutingConfigurator
import dev.inmo.micro_utils.startup.plugin.StartPlugin
import dev.inmo.wishlist.features.currency.common.CurrencyFeature
import dev.inmo.wishlist.features.currency.server.configurators.CurrencyRoutingsConfigurator
import dev.inmo.wishlist.features.currency.server.services.OpenExchangeRatesService
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.koin.core.Koin
import org.koin.core.module.Module
import org.koin.core.qualifier.named

/**
 * Server startup plugin for the currency feature.
 *
 * Registers, all into the shared DI graph:
 * - [CurrencyConfig] decoded from the root server config JSON (provides the OXR App ID).
 * - A dedicated [HttpClient] (OkHttp engine, JSON content negotiation) used to call Open Exchange Rates.
 * - [OpenExchangeRatesService] bound as the [CurrencyFeature], with its in-memory 1-hour TTL cache.
 * - [CurrencyRoutingsConfigurator] exposing the read-only currency endpoints.
 *
 * The module targets only the JVM, so the OkHttp engine reference is safe in common code here.
 */
object Plugin : StartPlugin {
    private val currencyHttpClientQualifier = named("currencyHttpClient")

    override fun Module.setupDI(config: JsonObject) {
        single { get<Json>().decodeFromJsonElement(CurrencyConfig.serializer(), config) }

        single(qualifier = currencyHttpClientQualifier) {
            val json = get<Json>()
            HttpClient(OkHttp) {
                install(ContentNegotiation) {
                    json(json)
                }
            }
        }

        single {
            OpenExchangeRatesService(
                appId = get<CurrencyConfig>().openExchangeRatesAppId,
                httpClient = get(qualifier = currencyHttpClientQualifier)
            )
        }
        single<CurrencyFeature> { get<OpenExchangeRatesService>() }

        singleWithRandomQualifier<ApplicationRoutingConfigurator.Element> {
            CurrencyRoutingsConfigurator(get())
        }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
    }
}
