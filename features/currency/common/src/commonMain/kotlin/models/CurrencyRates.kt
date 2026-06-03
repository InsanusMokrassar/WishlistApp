package dev.inmo.wishlist.features.currency.common.models

import kotlinx.serialization.Serializable

/**
 * Snapshot of exchange rates fetched from the upstream provider.
 *
 * Each value in [rates] is the multiplier converting one unit of [base] into the keyed currency
 * (i.e. `amountInCode = amountInBase * rates[code]`). On the Open Exchange Rates free plan [base] is
 * always `USD`.
 *
 * @property base Base currency all [rates] are expressed against.
 * @property rates Map of ISO currency code (upper-case string) to the base→code multiplier.
 * @property fetchedAtMillis Unix epoch milliseconds when this snapshot was retrieved; used for TTL.
 */
@Serializable
data class CurrencyRates(
    val base: CurrencyCode,
    val rates: Map<String, Double>,
    val fetchedAtMillis: Long
) {
    /**
     * Returns the base→[code] multiplier, or `null` when the currency is not present in this snapshot.
     *
     * @param code Currency to look up.
     * @return The multiplier, or `null` if unknown.
     */
    fun rateOf(code: CurrencyCode): Double? = rates[code.code]
}
