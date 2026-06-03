package dev.inmo.wishlist.features.currency.server.services

import dev.inmo.micro_utils.coroutines.runCatchingLogging
import dev.inmo.wishlist.features.currency.common.CurrencyFeature
import dev.inmo.wishlist.features.currency.common.models.CurrencyCode
import dev.inmo.wishlist.features.currency.common.models.CurrencyInfo
import dev.inmo.wishlist.features.currency.common.models.CurrencyRates
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.URLBuilder
import io.ktor.http.Url
import korlibs.time.DateTime
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlin.collections.plus

/**
 * Server-side [CurrencyFeature] implementation backed by the Open Exchange Rates (OXR) REST API.
 *
 * Responsibilities:
 * - Fetches the latest rates (`/api/latest.json`) and the currency dictionary (`/api/currencies.json`).
 * - Caches both answers in-memory; each cache entry is invalidated once [ttlMillis] (default one hour)
 *   has elapsed since its own last successful retrieval, so the next access triggers a fresh fetch.
 * - Is a no-op when [appId] is `null` or blank: the feature reports itself disabled and returns empty/`null`.
 *
 * Upstream failures are swallowed (logged) and fall back to the last good cache, or to
 * empty/`null` when nothing was ever fetched, so the server never crashes on an OXR outage.
 *
 * @param appId Open Exchange Rates App ID; `null` or blank disables the feature.
 * @param httpClient HTTP client used to call OXR. Content negotiation (JSON) must be installed on it.
 * @param ttlMillis Cache lifetime in milliseconds; defaults to one hour.
 */
class OpenExchangeRatesService(
    private val appId: String?,
    private val httpClient: HttpClient,
    private val ttlMillis: Long = 60L * 60L * 1000L
) : CurrencyFeature {
    /** Wire-format DTO for the OXR `latest.json` response (only the parts the feature needs). */
    @Serializable
    private data class OxrLatest(
        val base: String,
        val rates: Map<String, Double>
    )

    private val ratesMutex = Mutex()
    private var cachedRates: CurrencyRates? = null

    private val currenciesMutex = Mutex()
    private var cachedCurrencies: List<CurrencyInfo>? = null
    private var currenciesFetchedAtMillis: Long = 0L

    private val baseUrl = "https://openexchangerates.org/api"

    private fun buildUrl(block: URLBuilder.() -> Unit): Url? {
        val builder = URLBuilder(baseUrl)
        builder.parameters.set("app_id", appId ?: return null)
        builder.block()
        return builder.build()
    }

    /** @return `true` only when a non-blank [appId] is configured. */
    override suspend fun isFeatureEnabled(): Boolean = !appId.isNullOrBlank()

    /**
     * Returns the cached currency dictionary, refreshing it from OXR when missing or stale.
     *
     * @return Available currencies sorted by code, or an empty list when disabled.
     */
    override suspend fun getCurrencies(): List<CurrencyInfo> {
        if (!isFeatureEnabled()) return emptyList()
        return currenciesMutex.withLock {
            val cached = cachedCurrencies
            if (cached != null && (DateTime.now().unixMillisLong - currenciesFetchedAtMillis) < ttlMillis) {
                cached
            } else {
                val fetched = runCatchingLogging {
                    val map: Map<String, String> = httpClient.get(
                        buildUrl {
                            pathSegments += "currencies.json"
                        } ?: return emptyList()
                    ).body()
                    map.entries
                        .map { CurrencyInfo(CurrencyCode.of(it.key), it.value) }
                        .sortedBy { it.code.code }
                }.getOrNull()
                if (fetched != null) {
                    cachedCurrencies = fetched
                    currenciesFetchedAtMillis = DateTime.now().unixMillisLong
                    fetched
                } else {
                    cached ?: emptyList()
                }
            }
        }
    }

    /**
     * Returns the cached rates snapshot, refreshing it from OXR when missing or stale.
     *
     * @return Latest [CurrencyRates], or `null` when disabled and no cache exists.
     */
    override suspend fun getRates(): CurrencyRates? {
        if (!isFeatureEnabled()) return null
        return ratesMutex.withLock {
            val cached = cachedRates
            if (cached != null && (DateTime.now().unixMillisLong - cached.fetchedAtMillis) < ttlMillis) {
                cached
            } else {
                val fetched = runCatchingLogging {
                    val latest: OxrLatest = httpClient.get(
                        buildUrl {
                            pathSegments += "latest.json"
                        } ?: return null
                    ) {
                        parameter("app_id", appId)
                    }.body()
                    CurrencyRates(
                        base = CurrencyCode.of(latest.base),
                        rates = latest.rates,
                        fetchedAtMillis = DateTime.now().unixMillisLong
                    )
                }.getOrNull()
                if (fetched != null) {
                    cachedRates = fetched
                    fetched
                } else {
                    cached
                }
            }
        }
    }
}
