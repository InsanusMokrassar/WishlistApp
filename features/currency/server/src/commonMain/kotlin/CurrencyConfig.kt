package dev.inmo.wishlist.features.currency.server

import kotlinx.serialization.Serializable

/**
 * Currency-feature slice of the server config JSON. Decoded from the same root config object the rest
 * of the server config is decoded from (see `features/files` `FilesConfig` for the same approach), so
 * adding this feature requires no change to any shared `Config` type.
 *
 * @property openExchangeRatesAppId Open Exchange Rates App ID (API key). When blank the feature is
 * disabled and behaves as a no-op.
 */
@Serializable
data class CurrencyConfig(
    val openExchangeRatesAppId: String = ""
)
