# STEP_0 — Planning

ENTITY:
entity_id=task-currency-display-format; type=pr-review-fix; state=planned

CONTEXT:
- task_id=91b74485-ab2e-46f1-a5dc-56dfabc0e90b; agent_id=orchestrator; memory_ref=[PR#20]
- source=PR#20 review comment
- requirement=wishlist item price, common-currency-set → format `<common amount> <common currency symbol/name> (<original amount> <original currency symbol/name>)`
- constraints=[do not modify README Operator Notes, docs via haiku, code via sonnet/builder]

CURRENT_STATE:
- entity_id=formatItemPrice; file=features/currency/common/src/commonMain/kotlin/utils/CurrencyConversion.kt; line=45-57
- current_converted_return=`"$converted ${target.code}"` (line 56)
- raw_return (no target/rates/unresolvable)=`"$price $priceUnits"` (unchanged, line 52/53/54/55)
- callers=all WishlistView/UserWishlistsView/WishlistItemView (js/jvm/android); shared pure helper → single edit propagates
- no test files exist for helper

PLAN:
1. action=edit; target=formatItemPrice line 56; params={new_return=`"$converted ${target.code} ($price $priceUnits)"`}
   reason: common amount=converted, common currency name=target.code, original amount=price, original units=priceUnits
2. action=edit; target=formatItemPrice docstring line 35; params={update "Otherwise the converted amount followed by target code" → new composite format}
3. action=edit; target=README.md line 108; params={update formatItemPrice description to composite format} (haiku agent)

SCOPE_BOUNDARY:
- raw (no-conversion) path UNCHANGED — composite format only when conversion succeeds
- grid WishlistItemCard footer (raw `"$price ${item.priceUnits}"`) NOT wired to formatItemPrice — out of review scope; note for operator

REASON:
- condition=common currency selected AND price resolvable AND rate present → requirement=show both common+original
- single-point change in pure helper = minimal blast radius

EXPECTED_RESULT:
- entity_id=formatItemPrice; new_state=returns composite string on successful conversion; location=CurrencyConversion.kt

VERIFICATION:
- check=converted-branch output; expected=`42.5 EUR (50.0 $)`-shaped
- check=raw-branch output; expected=unchanged `50.0 $`

NEXT=STEP_1 architecture
