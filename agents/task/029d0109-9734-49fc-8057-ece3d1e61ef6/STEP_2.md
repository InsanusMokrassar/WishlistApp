# STEP_2 — Coding (results)

task_id=029d0109-9734-49fc-8057-ece3d1e61ef6
prev=STEP_1
status=done; verification=compile_passed(jvm,js,android)

## Helper added

features/currency/common/src/commonMain/kotlin/utils/CurrencyConversion.kt
+ fun formatItemPriceWithAmount(price: Amount?, priceUnits, amount: UInt, target, rates): String
  - amount<=1 → formatItemPrice (unchanged)
  - amount>1 && price==null → "×<amount>"
  - amount>1 && price!=null → "<unit>x<amount> (<whole>)", whole=price*amount.toLong().toDouble()

## View edits (×3 platforms: jsMain, androidMain, jvmMain)

WishlistView.kt (list row): formatItemPrice→formatItemPriceWithAmount; render only when result non-empty; import swap.
WishlistItemCard.kt (grid card): removed standalone amount line/badge near title; footer now formatItemPriceWithAmount(...,null,null) [raw, no currency ctx]; render when non-empty; import add; JS dropped unused Span import; doc comment updated.
WishlistItemView.kt (detail): removed separate amount label+value block; price section uses formatItemPriceWithAmount; shows noPrice only when result empty; import swap.

files_changed=10 (1 util + 9 views)

## Verify

cmd: ./gradlew :wishlist.features.currency.common:compileKotlinJvm :wishlist.features.ui.wishlist:compileKotlinJvm :wishlist.features.ui.wishlist:compileKotlinJs → BUILD SUCCESSFUL
cmd: ./gradlew :wishlist.features.ui.wishlist:compileDebugKotlinAndroid → BUILD SUCCESSFUL
ast-index: update run (index current).

## Examples (amount=3, price=100 USD)

list/card/detail → "100 USDx3 (300 USD)"
amount=3, no price → "×3"
amount=1 → "100 USD" (unchanged)

## Not done

- git commit: NOT performed (operator did not request).
- features/currency/common missing README.md (pre-existing; doc creation belongs to haiku per SHORTCUTS.md) — out of scope.
