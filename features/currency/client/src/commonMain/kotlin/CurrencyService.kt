package dev.inmo.wishlist.features.currency.client

import dev.inmo.micro_utils.coroutines.MutableRedeliverStateFlow
import dev.inmo.micro_utils.coroutines.SmartRWLocker
import dev.inmo.micro_utils.coroutines.runCatchingLogging
import dev.inmo.micro_utils.coroutines.withReadAcquire
import dev.inmo.micro_utils.coroutines.withWriteLock
import dev.inmo.wishlist.features.currency.common.CurrencyFeature
import dev.inmo.wishlist.features.currency.common.models.CurrencyCode
import dev.inmo.wishlist.features.currency.common.models.CurrencyInfo
import dev.inmo.wishlist.features.currency.common.models.CurrencyRates
import kotlinx.coroutines.flow.StateFlow

/**
 * Client-side wrapper around the HTTP-only [CurrencyFeature] that adds the logic the Ktor layer is not
 * allowed to hold: the user's shared target-currency selection plus light client-side memoization of
 * the (rarely changing) enabled flag, currency list and rates snapshot.
 *
 * The selected currency is exposed as a single shared [StateFlow] so every wishlist view agrees on the
 * conversion target. A `null` selection means "show original prices, no conversion".
 *
 * @param feature HTTP-backed currency capability.
 */
class CurrencyService(
    private val feature: CurrencyFeature
) : CurrencyFeature {
    private val _selectedCurrency = MutableRedeliverStateFlow<CurrencyCode?>(null)

    /** Currently selected conversion target shared across all views; `null` means no conversion. */
    val selectedCurrency: StateFlow<CurrencyCode?> = _selectedCurrency

    private val locker = SmartRWLocker()
    private var cachedEnabled: Boolean? = null
    private var cachedCurrencies: List<CurrencyInfo>? = null
    private var cachedRates: CurrencyRates? = null

    /**
     * Reports whether the feature is enabled, memoizing the first successful answer.
     *
     * @return `true` when the server reports the feature enabled; `false` on disable or any error.
     */
    override suspend fun isFeatureEnabled(): Boolean {
        locker.withReadAcquire { cachedEnabled }?.let { return it }
        val value = runCatchingLogging { feature.isFeatureEnabled() }.getOrDefault(false)
        locker.withWriteLock { cachedEnabled = value }
        return value
    }

    /**
     * Returns the available currencies, memoizing the first non-empty answer.
     *
     * @return Available currencies, or an empty list when disabled/unavailable.
     */
    override suspend fun getCurrencies(): List<CurrencyInfo> {
        locker.withReadAcquire { cachedCurrencies }?.let { return it }
        val value = runCatchingLogging { feature.getCurrencies() }.getOrDefault(emptyList())
        if (value.isNotEmpty()) {
            locker.withWriteLock { cachedCurrencies = value }
        }
        return value
    }

    /**
     * Returns the latest rates snapshot, memoizing the first successful answer. The server already
     * enforces a one-hour TTL, so a single client fetch per session is sufficient for display.
     *
     * @return Latest [CurrencyRates], or `null` when disabled/unavailable.
     */
    override suspend fun getRates(): CurrencyRates? {
        locker.withReadAcquire { cachedRates }?.let { return it }
        val value = runCatchingLogging { feature.getRates() }.getOrNull()
        if (value != null) {
            locker.withWriteLock { cachedRates = value }
        }
        return value
    }

    /**
     * Updates the shared conversion target.
     *
     * @param code Target currency, or `null` to disable conversion (show original prices).
     */
    fun select(code: CurrencyCode?) {
        _selectedCurrency.value = code
    }
}
