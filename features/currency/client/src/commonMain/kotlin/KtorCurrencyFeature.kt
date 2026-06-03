package dev.inmo.wishlist.features.currency.client

import dev.inmo.wishlist.features.currency.common.CurrencyConstants
import dev.inmo.wishlist.features.currency.common.CurrencyFeature
import dev.inmo.wishlist.features.currency.common.models.CurrencyInfo
import dev.inmo.wishlist.features.currency.common.models.CurrencyRates
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess

/**
 * HTTP-only [CurrencyFeature] implementation that calls the server currency endpoints over the shared
 * [HttpClient]. Per the project's Ktor realization rule this class performs no caching, state, or
 * business logic — that lives in [CurrencyService].
 *
 * @param client Shared HTTP client (already configured with auth, serialization, base URL).
 */
class KtorCurrencyFeature(
    private val client: HttpClient
) : CurrencyFeature {
    private val enabledPath = "${CurrencyConstants.prefixPathPart}/${CurrencyConstants.enabledPathPart}"
    private val currenciesPath = "${CurrencyConstants.prefixPathPart}/${CurrencyConstants.currenciesPathPart}"
    private val ratesPath = "${CurrencyConstants.prefixPathPart}/${CurrencyConstants.ratesPathPart}"

    override suspend fun isFeatureEnabled(): Boolean {
        val response = client.get(enabledPath)
        return if (response.status.isSuccess()) response.body() else false
    }

    override suspend fun getCurrencies(): List<CurrencyInfo> {
        val response = client.get(currenciesPath)
        return if (response.status.isSuccess()) response.body() else emptyList()
    }

    override suspend fun getRates(): CurrencyRates? {
        val response = client.get(ratesPath)
        return if (response.status.isSuccess() && response.status != HttpStatusCode.NoContent) {
            response.body()
        } else {
            null
        }
    }
}
