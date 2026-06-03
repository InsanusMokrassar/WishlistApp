package dev.inmo.wishlist.features.currency.common.utils

import dev.inmo.wishlist.features.common.common.models.Amount
import dev.inmo.wishlist.features.currency.common.models.CurrencyCode
import dev.inmo.wishlist.features.currency.common.models.CurrencyRates

/**
 * Converts [amount] from currency [from] into currency [to] using a [rates] snapshot.
 *
 * Conversion pivots through the snapshot base currency: `result = amount * (rate(to) / rate(from))`.
 * Returns `null` when either currency is absent from [rates] (i.e. conversion cannot be performed).
 * When [from] equals [to] the original [amount] is returned unchanged.
 *
 * @param amount Source amount.
 * @param from Currency of [amount].
 * @param to Target currency.
 * @param rates Exchange-rate snapshot to use.
 * @return Converted [Amount], or `null` when conversion is impossible.
 */
fun convert(amount: Amount, from: CurrencyCode, to: CurrencyCode, rates: CurrencyRates): Amount? {
    if (from == to) return amount
    val fromRate = rates.rateOf(from) ?: return null
    val toRate = rates.rateOf(to) ?: return null
    if (fromRate == 0.0) return null
    return amount * (toRate / fromRate)
}

/**
 * Produces the user-facing price string for an item, applying currency conversion when possible.
 *
 * Behavior:
 * - `null` [price] yields an empty string.
 * - When [target] is `null` (no selection) or [rates] is `null`, the raw `"price priceUnits"` is returned.
 * - When the item's [priceUnits] cannot be resolved, or conversion is impossible, the raw form is returned.
 * - Otherwise the converted amount followed by the [target] code is returned (e.g. `"42.5 EUR"`).
 *
 * This is a pure function so views can call it directly during composition without suspending.
 *
 * @param price Item price, possibly `null`.
 * @param priceUnits Free-form units label stored on the item.
 * @param target Currency the user selected to convert into, or `null` for no conversion.
 * @param rates Latest rates snapshot, or `null` when unavailable.
 * @return Display string for the price.
 */
fun formatItemPrice(
    price: Amount?,
    priceUnits: String,
    target: CurrencyCode?,
    rates: CurrencyRates?
): String {
    if (price == null) return ""
    val raw = "$price $priceUnits".trim()
    if (target == null || rates == null) return raw
    val source = PriceUnitsResolver.resolve(priceUnits) ?: return raw
    val converted = convert(price, source, target, rates) ?: return raw
    return "$converted ${target.code}"
}
