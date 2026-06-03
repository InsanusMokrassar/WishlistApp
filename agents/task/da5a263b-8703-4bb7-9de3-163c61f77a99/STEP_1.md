# STEP_1 — Architecturing

task_id=issue-18-currency-conversion; uuid=da5a263b-8703-4bb7-9de3-163c61f77a99; depends_on=STEP_0

This step defines the concrete architecture. No source files edited (only this report) per ARCHITECTURE.md.

## A. Module: new full-stack feature `currency`

Scaffold via `./generate_feature.sh` (enter `currency`) -> creates:
- features/currency/common/{build.gradle, src/.../Plugin.kt + platform plugins + Constants}
- features/currency/server/{build.gradle, Plugin.kt, jvm JVMPlugin}
- features/currency/client/{build.gradle, Plugin.kt, platform plugins}

### settings.gradle additions (includes array)
```
":features:currency:common",
":features:currency:server",
":features:currency:client",
```

### Dependency wiring
- server/build.gradle: `api project(":wishlist.features.currency.server")`
- client/build.gradle (commonMain): `api project(":wishlist.features.currency.client")`
- client/android/build.gradle (commonMain): `api project(":wishlist.features.currency.client")`
- features/currency/server/build.gradle commonMain: `api project(":wishlist.features.currency.common")`, `api project(":wishlist.features.common.server")`; jvmMain: `api libs.ktor.client.okhttp`, `api libs.ktor.client.content.negotiation`, `api libs.ktor.serialization.kotlinx.json` (server-side HTTP client to call OXR).
- features/currency/client/build.gradle commonMain: `api project(":wishlist.features.currency.common")`, `api project(":wishlist.features.common.client")`.
- features/currency/common/build.gradle commonMain: `api project(":wishlist.features.common.common")` (for Amount). Template `mppJvmJsAndroid` + `com.android.library`.

### sample.config.json
- Add to `"plugins"`: `"dev.inmo.wishlist.features.currency.server.JVMPlugin"`.
- Add top-level field: `"openExchangeRatesAppId": ""` (empty => feature disabled by default). Document in README + ARCHITECTURE table is doc-only (do not edit agents/ARCHITECTURE.md; document in feature README).

### Client entry points (add plugins)
- client/src/jsMain/kotlin/Main.kt: `dev.inmo.wishlist.features.currency.client.JSPlugin`
- client/src/jvmMain/kotlin/Main.kt: `dev.inmo.wishlist.features.currency.client.JVMPlugin`
- client/android/src/main/kotlin/MainActivity.kt: `dev.inmo.wishlist.features.currency.client.AndroidPlugin`

## B. features/currency/common (commonMain)

### Constants.kt
```
object CurrencyConstants {
  const val prefixPathPart = "currency"
  const val enabledPathPart = "enabled"
  const val currenciesPathPart = "currencies"
  const val ratesPathPart = "rates"
}
```

### models/CurrencyCode.kt
`@Serializable @JvmInline value class CurrencyCode(val code: String)` — normalized ISO uppercase. Companion `fun of(raw:String):CurrencyCode = CurrencyCode(raw.trim().uppercase())`.

### models/CurrencyInfo.kt
`@Serializable data class CurrencyInfo(val code: CurrencyCode, val name: String)` — one dropdown entry.

### models/CurrencyRates.kt
`@Serializable data class CurrencyRates(val base: CurrencyCode, val rates: Map<String, Double>, val fetchedAtMillis: Long)`.
- `rates` key = ISO code string -> base->code multiplier (base USD). Map<String,Double> for trivial serialization.
- helper `fun rateOf(code: CurrencyCode): Double?`.

### utils/CurrencyConversion.kt
Pure conversion, shared by client.
```
fun convert(amount: Amount, from: CurrencyCode, to: CurrencyCode, rates: CurrencyRates): Amount?
// = amount * (rates.rateOf(to)!! / rates.rateOf(from)!!); null if either missing or from==to returns amount
```

### utils/PriceUnitsResolver.kt
Best-effort `priceUnits` String -> CurrencyCode? :
- symbol map: "$"->USD, "€"->EUR, "£"->GBP, "¥"->JPY, "₽"->RUB, "₴"->UAH, "₹"->INR.
- else if trimmed length==3 and alphabetic -> CurrencyCode.of(trimmed) (accept already-ISO; validity checked later against rates map).
- else null. Returns null => view shows raw.

### Plugin.kt + platform plugins: empty (delegate to parent, standard generated shape).

## C. features/currency/server

### commonMain/CurrencyFeature.kt  (interface — shared capability)
```
interface CurrencyFeature {
  suspend fun isFeatureEnabled(): Boolean
  suspend fun getCurrencies(): List<CurrencyInfo>   // empty when disabled
  suspend fun getRates(): CurrencyRates?            // null when disabled
}
```
> Per ALL.md note: identical interface server & client. Could be placed in common module. DECISION: declare canonical `CurrencyFeature` in features/currency/common/commonMain so both server impl and client KtorXxx implement the SAME interface (reduces duplication, matches "fully identical structure" note). server/client Plugins bind their impls to it.
RELOCATE: CurrencyFeature.kt -> features/currency/common/src/commonMain/kotlin/CurrencyFeature.kt.

### commonMain/CurrencyConfig.kt
`@Serializable data class CurrencyConfig(val openExchangeRatesAppId: String = "")`. Decoded from root config JsonObject. `enabled = openExchangeRatesAppId.isNotBlank()`.

### commonMain/services/OpenExchangeRatesService.kt  (the real impl + cache)
Implements `CurrencyFeature`.
ctor(appId: String, httpClient: HttpClient, ttlMillis: Long = 3_600_000).
State: `private val locker = SmartRWLocker()` (or Mutex), `private var cachedRates: CurrencyRates? = null`, `private var cachedCurrencies: List<CurrencyInfo>? = null`.
- `isFeatureEnabled() = appId.isNotBlank()`.
- `getRates()`: if disabled return null. Else under write lock: if cachedRates==null OR (DateTime.now().unixMillisLong - cachedRates.fetchedAtMillis) >= ttlMillis -> fetch `GET https://openexchangerates.org/api/latest.json?app_id=$appId` -> parse `{base, rates:{...}}` into CurrencyRates(fetchedAtMillis=now). Return cachedRates.
- `getCurrencies()`: if disabled return emptyList. cache currencies similarly (separate TTL/fetch from `GET .../currencies.json` — currencies.json needs no app_id but call with it anyway). Cache with same 1h TTL semantics (own fetchedAt). Map -> List<CurrencyInfo> sorted by code.
- Clock: `korlibs.time.DateTime.now().unixMillisLong` (klock already on classpath via common.server -> matches auth usage). HTTP fetch wrapped in runCatchingLogging; on failure return last good cache or null/empty (do not crash).
- DTOs for OXR JSON: private `@Serializable data class OxrLatest(val base:String, val rates: Map<String,Double>)`; currencies.json is `Map<String,String>` (code->name) decoded directly.

### commonMain/configurators/CurrencyRoutingsConfigurator.kt
`ApplicationRoutingConfigurator.Element`:
```
authenticate {
  route(prefixPathPart) {
    get(enabledPathPart)    { call.respond(feature.isFeatureEnabled()) }       // Boolean JSON
    get(currenciesPathPart) { call.respond(feature.getCurrencies()) }          // List<CurrencyInfo>
    get(ratesPathPart)      { feature.getRates()?.let{call.respond(it)} ?: call.respond(HttpStatusCode.NoContent) }
  }
}
```
Use `call.respond(...)` (ContentNegotiation JSON already installed by common.server). For enabled, respond Boolean; client reads via typed body.

### commonMain/Plugin.kt (server)
```
object Plugin : StartPlugin {
  fun Module.setupDI(config) {
    single { get<Json>().decodeFromJsonElement(CurrencyConfig.serializer(), config) }
    single { /* server HttpClient for OXR */ HttpClient(OkHttp){ install(ContentNegotiation){ json(get<Json>()) } } }  // qualifier to avoid clash? server has no other HttpClient -> plain single ok
    single { OpenExchangeRatesService(appId=get<CurrencyConfig>().openExchangeRatesAppId, httpClient=get(), ) }
    single<CurrencyFeature> { get<OpenExchangeRatesService>() }
    singleWithRandomQualifier<ApplicationRoutingConfigurator.Element> { CurrencyRoutingsConfigurator(get()) }
  }
}
```
NOTE: server currently registers NO HttpClient anywhere (verified). A plain `single { HttpClient(OkHttp){...} }` is safe. To avoid any future collision, may use a named qualifier; DECISION: plain single (no collision today). The OXR client base URL is absolute in requests, so no defaultRequest needed.

### jvmMain/JVMPlugin.kt (server)
Standard: delegate to currency.common.JVMPlugin + Plugin (matches sample/files). The OkHttp engine is JVM; HttpClient single is in commonMain Plugin but `OkHttp` is JVM-only -> MUST move the HttpClient `single` into jvmMain JVMPlugin (engine reference is JVM-only). DECISION: register the OXR HttpClient single + OpenExchangeRatesService single in jvmMain JVMPlugin; keep CurrencyConfig + interface binding + routing configurator in commonMain Plugin IF they don't reference the client. Re-split:
  - commonMain Plugin: CurrencyConfig single, CurrencyRoutingsConfigurator (needs CurrencyFeature -> bound in jvm), interface binding can be in jvm. Simplest: put EVERYTHING server-impl in jvmMain JVMPlugin (CurrencyConfig decode, HttpClient, service, interface binding, routing configurator), keep commonMain Plugin empty. server module commonMain still holds the routing configurator + service CLASSES (they don't reference OkHttp), only the `HttpClient(OkHttp)` construction is jvm. FINAL DECISION:
    * Classes OpenExchangeRatesService, CurrencyRoutingsConfigurator, CurrencyConfig live in commonMain (no engine ref).
    * jvmMain JVMPlugin.setupDI registers: CurrencyConfig decode, `single<HttpClient>{ HttpClient(OkHttp){install(ContentNegotiation){json(get())}} }`, OpenExchangeRatesService, `single<CurrencyFeature>`, routing configurator. commonMain server Plugin stays empty (only parent delegate).

## D. features/currency/client

### commonMain/KtorCurrencyFeature.kt  (HTTP ONLY — implements common CurrencyFeature)
```
class KtorCurrencyFeature(private val client: HttpClient): CurrencyFeature {
  override suspend fun isFeatureEnabled() = client.get("$prefix/$enabled").body<Boolean>()  // false on failure handled by wrapper? -> keep HTTP-only, may throw
  override suspend fun getCurrencies() = client.get("$prefix/$currencies").body<List<CurrencyInfo>>()
  override suspend fun getRates(): CurrencyRates? = client.get("$prefix/$rates").let{ if(it.status==NoContent) null else it.body() }
}
```
HTTP-only, no caching/logic (CODING.md rule). Bound NOT as the public service.

### commonMain/CurrencyService.kt  (wrapper service: caching + shared selection + conversion)
This holds business logic forbidden in KtorXxx.
```
class CurrencyService(private val feature: CurrencyFeature, private val scope: CoroutineScope) {
  private val _selected = MutableRedeliverStateFlow<CurrencyCode?>(null)   // null = no conversion (original units)
  val selectedCurrency: StateFlow<CurrencyCode?> = _selected
  private val locker = SmartRWLocker()
  private var cachedEnabled: Boolean? ; private var cachedCurrencies: List<CurrencyInfo>? ; private var cachedRates: CurrencyRates?
  suspend fun isEnabled(): Boolean (cached, runCatchingLogging -> false on error)
  suspend fun currencies(): List<CurrencyInfo> (cached)
  suspend fun rates(): CurrencyRates? (cached; client side simple memo, server already TTL-caches; optional short client memo)
  fun select(code: CurrencyCode?) { _selected.value = code }
  // pure display helper:
  suspend fun convertedPriceText(price: Amount?, priceUnits: String): String  // returns display string
}
```
`convertedPriceText`: if price==null -> "". target=_selected.value. if target==null OR !enabled -> "$price $priceUnits" (raw). else resolve source=PriceUnitsResolver.resolve(priceUnits); r=rates(); if source==null||r==null||r.rateOf(source)==null||r.rateOf(target)==null -> raw. else converted=convert(price,source,target,r); "$converted ${target.code}". Errors -> raw fallback.

Registered as `single { CurrencyService(get<CurrencyFeature>(), get()) }`. CoroutineScope from common.common.

### commonMain/Plugin.kt (client)
```
single { KtorCurrencyFeature(get()) }
single<CurrencyFeature> { get<KtorCurrencyFeature>() }
single { CurrencyService(get(), get()) }
```
### platform plugins: standard delegate shape.

## E. UI integration in features/ui/wishlist (existing UI feature; NO new ui module)

### build.gradle
add commonMain dep: `api project(":wishlist.features.currency.client")`.

### WishlistsModel.kt (interface) — add currency surface
```
val selectedCurrency: StateFlow<CurrencyCode?>
suspend fun isCurrencyEnabled(): Boolean
suspend fun availableCurrencies(): List<CurrencyInfo>
fun selectCurrency(code: CurrencyCode?)
suspend fun displayPrice(price: Amount?, priceUnits: String): String
```
Implementation in features/ui/wishlist/Plugin.kt: inject `CurrencyService`, delegate.

### ViewModels (WishlistViewModel, UserWishlistsViewModel, WishlistItemViewModel)
Add:
- `val currencyEnabledState: StateFlow<Boolean>` (loaded in init via model.isCurrencyEnabled()).
- `val currenciesState: StateFlow<List<CurrencyInfo>>` (loaded in init when enabled).
- `val selectedCurrencyState: StateFlow<CurrencyCode?> = model.selectedCurrency` (shared).
- `fun onCurrencySelected(code: CurrencyCode?) = model.selectCurrency(code)`.
- A way for the view to render converted price text. Since displayPrice is suspend, expose a derived flow:
  `displayItemsState`: combine(itemsState, selectedCurrencyState, ratesAvailable) -> recompute price strings.
  SIMPLER & dumb-view-friendly DECISION: ViewModel exposes `priceText(item): String` is not reactive. Instead make conversion synchronous in the VM by pre-loading rates: VM loads rates once (model.currentRates()) into a state, and conversion is a PURE function in common used directly. So add to model: `suspend fun currentRates(): CurrencyRates?` and expose `val ratesState: StateFlow<CurrencyRates?>`. Then VM derives a `Map<itemId,String>` or the View calls a pure `formatPrice(price, units, selected, rates)` helper (in commonMain utils, pure, no suspend). View stays dumb: passes item.approximatePrice/item.priceUnits + selectedCurrency + rates to the pure formatter.
  FINAL: add pure `fun formatItemPrice(price: Amount?, priceUnits: String, target: CurrencyCode?, rates: CurrencyRates?): String` in features/currency/common/utils. VM exposes `selectedCurrencyState`, `ratesState`, `currencyEnabledState`, `currenciesState`. Views collect these and call the pure formatter. No suspend in render path. CurrencyService still owns shared `_selected` + cached fetch feeding `ratesState`.

  To feed ratesState: model exposes `suspend fun currentRates(): CurrencyRates?`; VM init loads rates when enabled and also reloads on selection change (rates are target-independent so load once). CurrencyService.select just flips shared flow; rates already cached.

### CurrencySelector composable (per platform, in features/ui/wishlist ui package)
- jsMain: `CurrencySelector.kt` — Bootstrap `<select class="form-select form-select-sm">` with options: a "None/original" entry (value empty) + each CurrencyInfo. onChange -> onCurrencySelected. JS Stylesheet rule: if custom CSS needed add `CurrencySelectorStylesheet: StyleSheet()`; if only Bootstrap classes used, no stylesheet object required (rule applies only when custom CSS needed). Will keep Bootstrap-only -> no stylesheet object.
- jvmMain: Compose Desktop `DropdownMenu`/`ExposedDropdownMenuBox` (material) like WishlistSortSelector pattern. Reuse WishlistSortSelector.kt style.
- androidMain: material3 dropdown.
Shown only when `currencyEnabledState && currenciesState.isNotEmpty()`, placed near the top of each list view / item view (above the items list). Selecting drives shared state so all three views agree.

### View edits (9 files)
Replace `Text("$price $priceUnits")` with `Text(formatItemPrice(price, item.priceUnits, selectedCurrency, rates))`. Add CurrencySelector render guarded by enabled flag. WishlistView/UserWishlistsView/WishlistItemView x {js,jvm,android}.

Reference existing selector: features/ui/wishlist/.../WishlistSortSelector.kt (per platform) — mirror its structure for CurrencySelector to minimize platform-API risk.

## F. Strings
Add to WishlistStrings (or new CurrencyStrings in ui/wishlist): `currencyLabel` ("Currency"/"Валюта"), `currencyOriginal` ("Original"/"Исходная"). Use buildStringResource.

## G. README / docs
- features/currency/README.md: created by generator; fill required structure (Operator Notes empty marker, Overview, Routes table [GET currency/enabled, currency/currencies, currency/rates], Models, Architecture Notes incl. config field `openExchangeRatesAppId`, 1h TTL cache, OXR base USD, best-effort priceUnits resolution). DO NOT author Operator Notes content (leave the HTML comment placeholder only).
- features/ui/wishlist/README.md: update ## Architecture Notes only (currency dropdown + shared selection). DO NOT touch its ## Operator Notes.

## H. KDoc / rules compliance checklist for STEP_2
- KDoc on every new class/interface/object/fun/top-level val.
- JS Stylesheet object only if custom CSS used (plan: Bootstrap-only selector -> none needed; if inline style added, add stylesheet).
- MVVM: Views dumb; VM holds state; Model wraps client feature; no feature interface injected into VM directly (VM uses WishlistsModel).
- Plugin composition: platform plugin delegates only to parent/common within feature.
- KtorCurrencyFeature HTTP-only; logic in CurrencyService.

## I. Risks / fallbacks
- R1: server has no prior HttpClient/engine dep. Adding ktor-client-okhttp to currency/server jvmMain. Verify it resolves.
- R2: `call.respond(Boolean)` content negotiation for a bare Boolean — acceptable with kotlinx json. Client reads `body<Boolean>()`.
- R3: korlibs DateTime API name `unixMillisLong` — verify exact accessor at coding time (alt: `DateTime.now().unixMillisDouble.toLong()`).
- R4: NoContent handling for rates — client checks status before body.
- R5: Compose dropdown APIs differ per platform — mirror WishlistSortSelector exactly.

## J. Build verification (STEP_2)
1. `./gradlew :wishlist.features.currency.common:build :wishlist.features.currency.server:build :wishlist.features.currency.client:build`
2. `./gradlew :wishlist.features.ui.wishlist:build`
3. `./gradlew :wishlist.server:build` (wiring) and JS/JVM/Android client compile as feasible; if full client build too heavy, at least `:wishlist.client:compileKotlinJvm`/js metadata. Choose minimal sufficient set. One fix cycle only.

## Result
entity=STEP_1; state=COMPLETE; next=STEP_2 Coding; stored_in=agents/task/da5a263b-8703-4bb7-9de3-163c61f77a99/STEP_1.md
