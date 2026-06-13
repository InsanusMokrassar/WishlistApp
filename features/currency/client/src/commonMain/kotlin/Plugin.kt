package dev.inmo.wishlist.features.currency.client

import dev.inmo.micro_utils.startup.plugin.StartPlugin
import dev.inmo.wishlist.features.currency.common.CurrencyFeature
import kotlinx.serialization.json.JsonObject
import org.koin.core.Koin
import org.koin.core.module.Module

/**
 * Common client startup plugin for the currency feature.
 *
 * Registers the HTTP-only [KtorCurrencyFeature] (internal transport) and the [CurrencyService] wrapper
 * that owns the shared currency selection and client-side memoization. [CurrencyService] is bound as the
 * public [CurrencyFeature]; consumers resolving [CurrencyFeature] get the memoizing service, not the raw
 * Ktor transport. The shared [io.ktor.client.HttpClient] is resolved from `features/common/client`.
 */
object Plugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        single { KtorCurrencyFeature(get()) }
        single { CurrencyService(get<KtorCurrencyFeature>()) }
        single<CurrencyFeature> { get<CurrencyService>() }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
    }
}