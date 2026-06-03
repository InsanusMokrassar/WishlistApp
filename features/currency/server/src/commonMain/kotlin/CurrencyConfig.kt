package dev.inmo.wishlist.features.currency.server

import korlibs.time.hours
import korlibs.time.millisecondsLong
import kotlinx.serialization.Serializable

/**
 * Currency-feature slice of the server config JSON. Decoded from the same root config object the rest
 * of the server config is decoded from (see `features/files` `FilesConfig` for the same approach), so
 * adding this feature requires no change to any shared `Config` type.
 *
 * @property openExchangeRatesAppId Open Exchange Rates App ID (API key). `null` (the default, e.g. key
 * absent from config) or blank disables the feature and makes it a no-op.
 */
@Serializable
data class CurrencyConfig(
    val openExchangeRatesAppId: String? = null,
    val openExchangeRatesRefreshTTLMillis: Long = 1.hours.millisecondsLong
)
