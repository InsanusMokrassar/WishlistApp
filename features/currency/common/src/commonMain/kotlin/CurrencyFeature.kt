package dev.inmo.wishlist.features.currency.common

import dev.inmo.wishlist.features.currency.common.models.CurrencyInfo
import dev.inmo.wishlist.features.currency.common.models.CurrencyRates

/**
 * Capability exposed by the currency feature, implemented identically on the server (real upstream
 * fetch with caching) and on the client (HTTP wrapper over the server endpoints).
 *
 * When the feature is disabled (no Open Exchange Rates App ID configured) [isFeatureEnabled] returns
 * `false`, [getCurrencies] returns an empty list, and [getRates] returns `null`, making the whole
 * client feature a no-op.
 */
interface CurrencyFeature {
    /**
     * Whether currency conversion is enabled.
     *
     * @return `true` when an upstream App ID is configured server-side, `false` otherwise.
     */
    suspend fun isFeatureEnabled(): Boolean

    /**
     * Lists the currencies available for conversion.
     *
     * @return Available currencies, or an empty list when the feature is disabled.
     */
    suspend fun getCurrencies(): List<CurrencyInfo>

    /**
     * Returns the latest exchange-rate snapshot.
     *
     * @return Current [CurrencyRates], or `null` when the feature is disabled or rates are unavailable.
     */
    suspend fun getRates(): CurrencyRates?
}
