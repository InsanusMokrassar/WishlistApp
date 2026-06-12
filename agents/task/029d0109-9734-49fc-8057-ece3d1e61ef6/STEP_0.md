# STEP_0 — Planning

task_id=029d0109-9734-49fc-8057-ece3d1e61ef6
source=github_issue#34
title="Add amount showing in wishlist items component (if above 1)"

## Requirement (clarified with operator)

condition: item.amount > 1
format: `<price>x<amount> (<whole>)` where whole = price * amount
no_price_case: show `×<amount>` alone
scope: ALL three display sites, ALL three client platforms (js, android, jvm)
- site_1=list_rows (WishlistView, list view mode) — amount currently NOT shown → add
- site_2=grid_card (WishlistItemCard) — amount currently standalone line/badge near title → move after cost
- site_3=detail_view (WishlistItemView) — amount currently separate label+value block → move after cost
amount==1: no change (price-only as today)

## Files in scope (9 view files + 1 util)

util: features/currency/common/src/commonMain/kotlin/utils/CurrencyConversion.kt (add helper)
views (×3 platforms each under features/ui/wishlist/src/{jsMain,androidMain,jvmMain}/kotlin/ui/):
- WishlistView.kt (list row)
- WishlistItemCard.kt (grid card)
- WishlistItemView.kt (detail)

## Constraints

- JS views MUST use Bootstrap classes (local.CODING.md).
- Card footer uses RAW price (no currency conversion context — no selectedCurrency/rates available).
- List + detail HAVE selectedCurrency + rates → use conversion.
- approximatePrice: dev.inmo...models.Amount; Amount has `operator times(Double)`.
- item.amount: UInt.

result: stored_in=this_file; status=available; next=STEP_1 architecture
