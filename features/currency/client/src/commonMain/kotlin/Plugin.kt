package dev.inmo.wishlist.features.currency.client

import dev.inmo.micro_utils.startup.plugin.StartPlugin
import dev.inmo.wishlist.features.currency.common.CurrencyFeature
import kotlinx.serialization.json.JsonObject
import org.koin.core.Koin
import org.koin.core.module.Module

/**
 * Common client startup plugin for the currency feature.
 *
 * Registers the HTTP-only [KtorCurrencyFeature] bound as [CurrencyFeature], and the [CurrencyService]
 * wrapper that owns the shared currency selection and client-side memoization. The shared [io.ktor.client.HttpClient]
 * is resolved from `features/common/client`.
 */
object Plugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        single { KtorCurrencyFeature(get()) }
        single<CurrencyFeature> { get<KtorCurrencyFeature>() }
        single { CurrencyService(get()) }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
    }
}