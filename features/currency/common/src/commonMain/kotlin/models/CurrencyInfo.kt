package dev.inmo.wishlist.features.currency.common.models

import kotlinx.serialization.Serializable

/**
 * One selectable currency entry as presented in the client-side currency dropdown.
 *
 * @property code ISO currency code identifying the currency.
 * @property name Human-readable currency name (e.g. `United States Dollar`).
 */
@Serializable
data class CurrencyInfo(
    val code: CurrencyCode,
    val name: String
)
