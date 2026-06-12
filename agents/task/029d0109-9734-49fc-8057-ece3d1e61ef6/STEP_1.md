# STEP_1 — Architecture

task_id=029d0109-9734-49fc-8057-ece3d1e61ef6
prev=STEP_0

## New shared helper (pure, commonMain)

location: features/currency/common/src/commonMain/kotlin/utils/CurrencyConversion.kt
signature:
```kotlin
fun formatItemPriceWithAmount(
    price: Amount?,
    priceUnits: String,
    amount: UInt,
    target: CurrencyCode?,
    rates: CurrencyRates?
): String
```
logic:
- amount <= 1u → return formatItemPrice(price, priceUnits, target, rates)   // unchanged behavior, "" when price null
- price == null (amount>1) → return "×$amount"
- else:
  - unit  = formatItemPrice(price, priceUnits, target, rates)
  - whole = formatItemPrice(price * amount.toLong().toDouble(), priceUnits, target, rates)
  - return "${unit}x$amount ($whole)"

rationale: reuse formatItemPrice so currency conversion + unit labels stay consistent; one place to format.
edge(currency-converted): formatItemPrice may itself add "(orig)" → nested parens; accepted, info correct.

## Per-site integration

### list_rows (WishlistView.kt ×3) — HAS target+rates
replace `if (approximatePrice != null) { Text(formatItemPrice(...)) }`
with:
```
val priceText = formatItemPriceWithAmount(item.approximatePrice, item.priceUnits, item.amount, selectedCurrency, rates)
if (priceText.isNotEmpty()) { <span/Text>(priceText) }
```
import swap: formatItemPrice → formatItemPriceWithAmount

### grid_card (WishlistItemCard.kt ×3) — NO target/rates → pass null,null (raw form, same as today for amount==1)
1. remove standalone amount block near title (the `if (item.amount != 1u) { ×amount }`)
2. footer: replace `approximatePrice?.let { Text("$price $priceUnits") }` with:
```
val priceText = formatItemPriceWithAmount(item.approximatePrice, item.priceUnits, item.amount, null, null)
if (priceText.isNotEmpty()) { <footer>(priceText) }
```
add import formatItemPriceWithAmount; JS card also drop now-unused Span/×amount badge.

### detail_view (WishlistItemView.kt ×3) — HAS target+rates
1. remove separate amount block (`if (it.amount != 1u) { amountLabel + ×amount }`)
2. price section: compute priceText = formatItemPriceWithAmount(it.approximatePrice, it.priceUnits, it.amount, selectedCurrency, rates); if non-empty show it else show noPrice.
import swap formatItemPrice → formatItemPriceWithAmount.

## Post

- run ast-index rebuild (source changed).
- build affected client targets to verify compile.

result: stored_in=this_file; status=available; next=STEP_2 coding
