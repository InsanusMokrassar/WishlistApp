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
 * - Otherwise the converted amount with the [target] currency symbol (falling back to its ISO code) is returned, with the original price in parentheses (e.g. `"42.5 € (50.0 $)"`). When the item is already expressed in the target's units, the raw form is returned unchanged.
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

    val targetUnits = PriceUnitsResolver.resolve(target) ?: target.code
    if (targetUnits == priceUnits) return raw

    val source = PriceUnitsResolver.resolve(priceUnits) ?: return raw
    val converted = convert(price, source, target, rates) ?: return raw

    return "$converted $targetUnits ($price $priceUnits)"
}

/**
 * The currency most often present among [priceUnitsList] — used as the common currency to compare
 * prices against when sorting by cost. Only resolvable labels (see [PriceUnitsResolver.resolve])
 * participate; ties are broken arbitrarily.
 *
 * @param priceUnitsList Free-form `priceUnits` labels of the items under consideration.
 * @return Most frequent resolvable [CurrencyCode], or `null` when none can be resolved.
 */
fun dominantCurrency(priceUnitsList: Iterable<String>): CurrencyCode? =
    priceUnitsList.mapNotNull { PriceUnitsResolver.resolve(it) }
        .groupingBy { it }
        .eachCount()
        .maxByOrNull { it.value }
        ?.key

/**
 * Comparable sort key for an item's price expressed in a [common] currency.
 *
 * - `null` [price] yields `null` (callers sort these last).
 * - When [common] or [rates] is `null` (no conversion context — e.g. every item already shares one
 *   currency), the raw numeric [price] is used directly.
 * - Otherwise the price is converted from its resolved currency into [common]; an unresolvable label
 *   or a missing rate yields `null` so the item is sorted last.
 *
 * @param price Item price, possibly `null`.
 * @param priceUnits Free-form units label stored on the item.
 * @param common Common currency to express all prices in, or `null` to compare raw amounts.
 * @param rates Latest rates snapshot, or `null` when unavailable.
 * @return Comparable [Double] key, or `null` when the item cannot be placed.
 */
fun costSortKey(
    price: Amount?,
    priceUnits: String,
    common: CurrencyCode?,
    rates: CurrencyRates?
): Double? {
    if (price == null) return null
    if (common == null || rates == null) return price.toDouble()
    val source = PriceUnitsResolver.resolve(priceUnits) ?: return null
    val converted = convert(price, source, common, rates) ?: return null
    return converted.toDouble()
}

/**
 * Whether sort-by-price is meaningful for a set of items: either currency conversion is available
 * ([currencyEnabled]) or every non-blank currency label is identical, so prices are directly
 * comparable without conversion.
 *
 * @param priceUnitsList Free-form `priceUnits` labels of the items under consideration.
 * @param currencyEnabled `true` when the currency-conversion feature is enabled server-side.
 * @return `true` when a price ordering can be produced.
 */
fun isCostSortAvailable(priceUnitsList: Iterable<String>, currencyEnabled: Boolean): Boolean {
    if (currencyEnabled) return true
    val distinct = priceUnitsList.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
    return distinct.size <= 1
}
