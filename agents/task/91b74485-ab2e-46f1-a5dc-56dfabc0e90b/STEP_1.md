# STEP_1 — Architecturing

ENTITY:
entity_id=formatItemPrice; type=pure-function; state=design-final

CONTEXT:
- task_id=91b74485-ab2e-46f1-a5dc-56dfabc0e90b; agent_id=orchestrator
- depends_on=STEP_0 plan

DESIGN:
- no new types, no signature change, no new params
- signature stays: `formatItemPrice(price: Amount?, priceUnits: String, target: CurrencyCode?, rates: CurrencyRates?): String`
- branch map UNCHANGED:
  - price==null → `""`
  - target==null || rates==null → raw `"$price $priceUnits".trim()`
  - source unresolvable → raw
  - convert null → raw
  - SUCCESS branch ONLY changes: was `"$converted ${target.code}"`, now `"$converted ${target.code} ($price $priceUnits)"`
- common currency symbol/name source = `target.code` (ISO code; CurrencyCode has only `.code`, no symbol field) → use code as name
- original currency symbol/name source = `priceUnits` (raw stored label, already symbol or name)
- common amount = `converted` (Amount.toString); original amount = `price` (Amount.toString)

RATIONALE:
- CurrencyCode value class exposes only `code` (no symbol map reverse-lookup); spec accepts "symbol/name" → ISO code satisfies "name"
- parens wrap original = literal spec format
- pure, no suspend, composition-safe (callers unchanged)

FILES:
1. features/currency/common/src/commonMain/kotlin/utils/CurrencyConversion.kt — line 56 return + line ~35 docstring → CODING agent (sonnet/builder)
2. features/ui/wishlist/README.md — line 108 description → DOCS agent (haiku)

VERIFICATION:
- check=compile features/currency/common; expected=success
- check=success-branch string; expected=`<conv> <CODE> (<price> <units>)`

ARCH_RISKS=none; no incompatibility → proceed STEP_2 coding
NEXT=STEP_2 coding
