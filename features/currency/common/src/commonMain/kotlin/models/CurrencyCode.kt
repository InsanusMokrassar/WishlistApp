package dev.inmo.wishlist.features.currency.common.models

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/**
 * Normalized ISO-4217 currency code (e.g. `USD`, `EUR`).
 *
 * Always stored upper-cased and trimmed via [of] so equality and map lookups are case-insensitive
 * with respect to the raw input. Backed by a plain [String] with zero runtime overhead.
 *
 * @property code The upper-cased three-letter currency code.
 */
@Serializable
@JvmInline
value class CurrencyCode(val code: String) {
    companion object {
        /**
         * Normalizes [raw] into a [CurrencyCode] by trimming surrounding whitespace and upper-casing.
         *
         * @param raw Arbitrary currency code string.
         * @return Normalized [CurrencyCode].
         */
        fun of(raw: String): CurrencyCode = CurrencyCode(raw.trim().uppercase())
    }
}
