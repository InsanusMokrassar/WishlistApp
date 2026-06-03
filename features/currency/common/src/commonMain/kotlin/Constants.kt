package dev.inmo.wishlist.features.currency.common

/**
 * Shared URL path parts for the currency feature, used by both the server routing configurator and
 * the client Ktor implementation so the two never drift apart.
 */
object CurrencyConstants {
    /** Root path segment under which all currency endpoints are mounted. */
    const val prefixPathPart = "currency"

    /** Sub-path returning a JSON [Boolean] flag indicating whether the feature is enabled. */
    const val enabledPathPart = "enabled"

    /** Sub-path returning the list of available currencies as JSON. */
    const val currenciesPathPart = "currencies"

    /** Sub-path returning the latest exchange rates as JSON (or `204 No Content` when disabled). */
    const val ratesPathPart = "rates"
}