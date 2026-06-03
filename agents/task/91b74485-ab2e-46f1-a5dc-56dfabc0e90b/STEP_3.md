# STEP_3 — Coding (symbol display + already-in-target short-circuit)

ENTITY:
entity_id=formatItemPrice; type=pure-function; state=DONE
entity_id=PriceUnitsResolver.resolve(CurrencyCode); type=function; state=ADDED

CONTEXT:
- task_id=91b74485-ab2e-46f1-a5dc-56dfabc0e90b; agent_id=orchestrator
- coding_agent=caveman:cavecrew-builder (.kt KDoc/comments); docs_agent=haiku (README *.md)
- source_of_change=operator working-tree edits (uncommitted) refined STEP_2 output

ACTIONS_DONE:
1. action=add; target=PriceUnitsResolver.kt; entity=codeToSymbol:Map<CurrencyCode,String>=inverse(symbolToCode); reason=lookup symbol-from-code
2. action=add; target=PriceUnitsResolver.kt; entity=resolve(CurrencyCode):String?; result=codeToSymbol[code]; reason=inverse lookup symbol
3. action=edit; target=CurrencyConversion.kt:formatItemPrice; change=targetUnits=resolve(target)?:target.code; if(targetUnits==priceUnits) return raw; success-branch="$converted $targetUnits ($price $priceUnits)"; reason=show target SYMBOL not CODE + skip self-conversion
4. action=fix-doc; target=PriceUnitsResolver.kt:24; change=codeToSymbol comment corrected (was reverse-of-truth copy-paste)
5. action=fix-doc; target=PriceUnitsResolver.kt:27; change=KDoc added to resolve(CurrencyCode); large KDoc re-attached to resolve(String)
6. action=fix-doc; target=CurrencyConversion.kt:35; change=KDoc bullet+example -> symbol form `42.5 € (50.0 $)` + already-in-target note
7. action=fix-doc; target=features/currency/README.md:47; change=`target code` -> `target currency symbol (falling back to ISO code)`
8. action=fix-doc; target=features/ui/wishlist/README.md:108; change=example -> `42.5 € (50.0 $)`; wording -> symbol (Operator Notes untouched)

VERIFICATION:
- check=compile :wishlist.features.currency.common:compileKotlinJvm; expected=EXIT=0; actual=EXIT=0 PASS
- check=symbol-known-branch (target=EUR); expected=`<conv> € (<price> <units>)`; actual=match PASS
- check=symbol-unknown-branch (target w/o symbol); expected=fallback target.code; actual=match PASS
- check=already-in-target short-circuit (targetUnits==priceUnits); expected=raw; actual=match PASS
- check=signature/callers; expected=no change; actual=no change PASS

OPEN_ITEMS (raised as PR#20 review comments, NOT changed):
- entity_id=resolve(CurrencyCode); concern=overload name reuse for inverse direction is confusing; recommend=rename symbolFor(code)
- entity_id=formatItemPrice:targetUnits==priceUnits; concern=string-symbol compare misses code-form labels (priceUnits="USD",target=USD -> "$"!="USD" -> no short-circuit, then redundant self-convert "50 $ (50 USD)"); recommend=compare resolved codes (resolve(priceUnits)==target)

RESULT_REPEAT:
- entity_id=formatItemPrice; stored_in=CurrencyConversion.kt; status=available; new_state=symbol-display + already-in-target-short-circuit
- entity_id=PriceUnitsResolver.codeToSymbol/resolve(CurrencyCode); stored_in=PriceUnitsResolver.kt; status=available

TASK_STATE=complete
