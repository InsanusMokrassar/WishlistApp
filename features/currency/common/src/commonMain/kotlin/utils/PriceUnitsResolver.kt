package dev.inmo.wishlist.features.currency.common.utils

import dev.inmo.wishlist.features.currency.common.models.CurrencyCode

/**
 * Best-effort resolver mapping a free-form `priceUnits` label (as stored on wishlist items) to an
 * ISO [CurrencyCode] suitable for conversion.
 *
 * Wishlist items store `priceUnits` as arbitrary text (e.g. `"$"`, `"€"`, `"USD"`), so conversion is
 * only possible when the label can be recognized. Anything unrecognized resolves to `null`, in which
 * case callers must display the price unchanged.
 */
object PriceUnitsResolver {
    /** Common currency symbols mapped to their ISO code. */
    val symbolToCode: Map<String, CurrencyCode> = mapOf(
        "$" to CurrencyCode("USD"),
        "€" to CurrencyCode("EUR"),
        "£" to CurrencyCode("GBP"),
        "¥" to CurrencyCode("JPY"),
        "₽" to CurrencyCode("RUB"),
        "₴" to CurrencyCode("UAH"),
        "₹" to CurrencyCode("INR")
    )

    /**
     * Resolves [priceUnits] to a [CurrencyCode] on a best-effort basis.
     *
     * Resolution order: exact symbol match, then an already-ISO three-letter alphabetic code.
     * Returns `null` for blank input or anything that cannot be confidently recognized; the returned
     * code is not validated against any rates table — callers should treat a missing rate as
     * "not convertible".
     *
     * @param priceUnits Free-form units label stored on the item.
     * @return Resolved [CurrencyCode] or `null` when unrecognized.
     */
    fun resolve(priceUnits: String): CurrencyCode? {
        val trimmed = priceUnits.trim()
        if (trimmed.isEmpty()) return null
        symbolToCode[trimmed]?.let { return it }
        if (trimmed.length == 3 && trimmed.all { it.isLetter() }) {
            return CurrencyCode.of(trimmed)
        }
        return null
    }
}
