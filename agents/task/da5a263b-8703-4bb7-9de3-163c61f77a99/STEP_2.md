# STEP_2 — Coding

task_id=issue-18-currency-conversion; uuid=da5a263b-8703-4bb7-9de3-163c61f77a99; depends_on=STEP_1

## Scaffolding
- Ran `./generate_feature.sh` with input `currency` → created features/currency/{common,server,client} stubs.
- Registered modules in settings.gradle (3 includes).

## Files created (feature `currency`)
- common/src/commonMain/kotlin/Constants.kt — CurrencyConstants path parts (overwrote stub).
- common/src/commonMain/kotlin/CurrencyFeature.kt — shared capability interface (isFeatureEnabled/getCurrencies/getRates).
- common/src/commonMain/kotlin/models/CurrencyCode.kt — ISO value class + normalizing `of()`.
- common/src/commonMain/kotlin/models/CurrencyInfo.kt — dropdown entry (code+name).
- common/src/commonMain/kotlin/models/CurrencyRates.kt — base+rates map+fetchedAtMillis, `rateOf()`.
- common/src/commonMain/kotlin/utils/PriceUnitsResolver.kt — best-effort free-form units → ISO.
- common/src/commonMain/kotlin/utils/CurrencyConversion.kt — pure `convert()` + `formatItemPrice()`.
- common/src/commonMain/kotlin/Plugin.kt — KDoc, empty (no shared DI).
- server/src/commonMain/kotlin/CurrencyConfig.kt — `openExchangeRatesAppId` decoded from root config.
- server/src/commonMain/kotlin/services/OpenExchangeRatesService.kt — OXR fetch + in-memory 1h TTL cache (Mutex, DateTime.now().unixMillisLong); failures fall back to last good cache.
- server/src/commonMain/kotlin/configurators/CurrencyRoutingsConfigurator.kt — GET enabled/currencies/rates under `authenticate`.
- server/src/commonMain/kotlin/Plugin.kt — wires CurrencyConfig, OkHttp HttpClient (qualifier `currencyHttpClient`), service, CurrencyFeature binding, routing element.
- server/src/jvmMain/kotlin/JVMPlugin.kt — KDoc added; delegates to common.JVMPlugin + Plugin (generated shape).
- client/src/commonMain/kotlin/KtorCurrencyFeature.kt — HTTP-only impl of CurrencyFeature.
- client/src/commonMain/kotlin/CurrencyService.kt — shared `selectedCurrency` StateFlow + memoized enabled/currencies/rates + `select()`.
- client/src/commonMain/kotlin/Plugin.kt — binds KtorCurrencyFeature as CurrencyFeature + CurrencyService single.
- README.md — required structure; Operator Notes left as empty marker (NOT authored).

## Files modified (wiring)
- settings.gradle — +3 currency includes.
- server/build.gradle — +api currency.server.
- server/sample.config.json — +currency.server.JVMPlugin in plugins; +`"openExchangeRatesAppId": ""`.
- features/currency/server/build.gradle — +ktor.client.okhttp, +ktor.client.content.negotiation, +ktor.serialization.kotlinx.json.
- client/build.gradle — +api currency.client (commonMain).
- client/src/jsMain/kotlin/Main.kt — +currency.client.JSPlugin.
- client/src/jvmMain/kotlin/Main.kt — +currency.client.JVMPlugin.
- client/android/src/main/kotlin/MainActivity.kt — +currency.client.AndroidPlugin (android client gets the module transitively via :wishlist.client).

## Files modified (UI integration in features/ui/wishlist)
- build.gradle — +api currency.client.
- src/commonMain/kotlin/ui/WishlistsModel.kt — +currency surface (selectedCurrency, isCurrencyEnabled, availableCurrencies, currencyRates, selectCurrency).
- src/commonMain/kotlin/Plugin.kt — inject CurrencyService; implement new WishlistsModel members.
- src/commonMain/kotlin/WishlistStrings.kt — +currencyLabel, +currencyOriginal (EN+RU).
- src/commonMain/kotlin/ui/WishlistViewModel.kt — +currency state flows + onCurrencySelected + init load.
- src/commonMain/kotlin/ui/UserWishlistsViewModel.kt — same.
- src/commonMain/kotlin/ui/WishlistItemViewModel.kt — same.
- src/{jsMain,jvmMain,androidMain}/kotlin/ui/CurrencySelector.kt — NEW per-platform selector (JS Bootstrap form-select; JVM material DropdownMenu; Android material3 DropdownMenu).
- src/{jsMain,jvmMain,androidMain}/kotlin/ui/WishlistView.kt — selector + formatItemPrice.
- src/{jsMain,jvmMain,androidMain}/kotlin/ui/UserWishlistsView.kt — selector + formatItemPrice (ItemRow gained selectedCurrency/rates params).
- src/{jsMain,jvmMain,androidMain}/kotlin/ui/WishlistItemView.kt — selector + formatItemPrice.
- README.md — appended a Currency conversion bullet to ## Architecture Notes (## Operator Notes untouched).

## Build verification (PASS)
- `:wishlist.features.currency.common:build :wishlist.features.currency.server:build :wishlist.features.currency.client:build` → BUILD SUCCESSFUL.
- `:wishlist.features.ui.wishlist:build` (all 3 platforms incl. Android lint) → BUILD SUCCESSFUL.
- `:wishlist.server:build` → BUILD SUCCESSFUL.
- `:wishlist.client:compileKotlinJs`, `:compileKotlinJvm` → BUILD SUCCESSFUL.
- `:wishlist.client.android:compileDebugKotlin`, `:wishlist.client:compileDebugKotlinAndroid` → BUILD SUCCESSFUL.
- Final aggregate build of all affected modules → BUILD SUCCESSFUL.
- No fix cycle needed beyond two trivial in-flight import corrections (Box import location JVM CurrencySelector; reading files before Edit). No identical error re-fixed.

## Design decisions
- `priceUnits` is free-form text, not ISO codes → best-effort symbol/ISO resolution; unresolved units shown raw (non-destructive). `formatItemPrice` is pure so views stay dumb.
- OXR free plan base=USD; convert via USD pivot `amount * rate(to)/rate(from)`.
- Server caches rates AND currency dictionary separately, each with its own 1h TTL keyed off last fetch time.
- `isFeatureEnabled()` server-side = App ID non-blank. Disabled ⇒ enabled endpoint false, currencies empty, rates 204 ⇒ client shows no dropdown, prices raw.
- Currency routes wrapped in `authenticate` (app convention).
- Selected currency held as a single shared StateFlow in client `CurrencyService`, surfaced through WishlistsModel ⇒ all three views agree.
- No new UI feature module: the existing 3 price-rendering views consume the currency client feature.

## Open questions for operator (non-blocking)
- Q1: Operator prompt referenced "the new WishlistItemCard from the just-merged cards feature" — that feature/file does NOT exist on this branch. Integrated into the actual existing price-rendering views (WishlistView, UserWishlistsView, WishlistItemView) across JS/JVM/Android. Confirm acceptable.
- Q2: Best-effort `priceUnits`→ISO mapping accepted as the conversion strategy (vs. converting only items already storing an ISO code). Confirm acceptable.

## Result
entity=STEP_2; state=COMPLETE; build=PASS; stored_in=agents/task/da5a263b-8703-4bb7-9de3-163c61f77a99/STEP_2.md
