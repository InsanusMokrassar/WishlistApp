# STEP_0 — Planning

task_id=issue-18-currency-conversion; uuid=da5a263b-8703-4bb7-9de3-163c61f77a99; role=root/ORCHESTRATOR

## 1. Task restatement

Goal: client-side currency conversion of displayed wishlist item prices via a user-selectable target currency dropdown.

Constraints (from issue #18 + operator prompt):
- Server-side fetch of latest rates from Open Exchange Rates (OXR) API; base=USD (free plan).
- Server caches OXR answers in-memory; cache invalidated 1 hour after latest retrieval (TTL keyed off last fetch time).
- Feature optional: feature interface MUST expose `isFeatureEnabled(): Boolean`. When disabled -> no dropdown, prices shown raw.
- OXR App ID read from server config JSON (NOT hardcoded). No key / disabled => `isFeatureEnabled()==false`, whole client feature is no-op.
- Server exposes: enabled-flag endpoint, list of available currencies, rates (or convert endpoint).
- Client wraps server endpoints via KtorXxxFeature (HTTP only); extra logic in a service (CODING.md Ktor rule).
- Dropdown shown where item prices render: WishlistView, UserWishlistsView, WishlistItemView (across JS/JVM/Android).
- Shared client state for selected target currency so all item views agree.
- Add OXR App ID field to server/sample.config.json with empty placeholder (feature disabled by default). Document it.

Do NOT commit/push/PR. Build-verify only. Do NOT modify any `## Operator Notes`.

## 2. Investigation findings (current state of repo)

### 2.1 Price data model
- File: features/wishlist/common/src/commonMain/kotlin/models/WishlistItem.kt
- Item price = `approximatePrice: Amount?` (nullable) + `priceUnits: String` (FREE-FORM label, e.g. "$", "€", "USD"; KDoc explicitly: `"Currency or unit label ... Empty when not applicable."`).
- `Amount` (features/common/common/.../models/Amount.kt) = fixed-point value class with `toDouble()`, `Amount(Double)`, `times(Double)`, comparable, `toString()` => decimal string. Multiplication by a Double rate is directly supported.

### 2.2 Price rendering sites (3 views x 3 platforms = 9 files)
- features/ui/wishlist/src/{jsMain,jvmMain,androidMain}/kotlin/ui/WishlistView.kt   (line ~82-86 JS: `Text("${price} ${item.priceUnits}")`)
- features/ui/wishlist/src/{jsMain,jvmMain,androidMain}/kotlin/ui/UserWishlistsView.kt (JS line ~141-145; ItemRow helper)
- features/ui/wishlist/src/{jsMain,jvmMain,androidMain}/kotlin/ui/WishlistItemView.kt  (JS line ~76-83)
- All render pattern: `Text("$approximatePrice $priceUnits")`.

### 2.3 "WishlistItemCard" / "cards feature" referenced by operator prompt
- DOES NOT EXIST on this branch. `find features -iname '*card*'` => empty; no `WishlistItemCard` symbol anywhere.
- DECISION: treat the three existing views (WishlistView, UserWishlistsView, WishlistItemView) as the price-rendering surface. Report this discrepancy to operator. No blocker — proceed with existing views.

### 2.4 Reference patterns confirmed
- Full-stack feature reference: features/sample (common Constants, server SampleFeature interface + service + RoutingsConfigurator + Plugin + jvm JVMPlugin, client SampleFeature + KtorSampleFeature + Plugin + platform plugins).
- Server config read pattern: decode a feature-local `@Serializable data class` from the SAME root config JsonObject via `get<Json>().decodeFromJsonElement(XxxConfig.serializer(), config)` (see features/files FilesConfig, features/auth Config).
- HttpClient built once in features/common/client Plugin from registered `HttpClientConfigurator`s; client KtorXxx features inject `HttpClient`.
- UI feature MVVM: Model interface (commonMain) wraps client feature; ViewModel factory; Interactor bound in client/ClientPlugin; platform Views.
- Shared client state precedent: ClientPlugin holds singletons (e.g. NavigationChain, MutableRedeliverStateFlow in AuthViewInteractor). A shared currency-selection holder can be a `single` in the currency client feature consumed by all three wishlist views' ViewModel/Model.

### 2.5 Server route auth
- sample routes wrap in `authenticate { }`. Currency endpoints: enabled-flag + currencies + rates. These are not user-private; but to match app convention and because the whole app is behind auth, wrap in `authenticate { }`. DECISION: wrap currency routes in `authenticate { }` (consistent with sample/wishlist). Low risk.

## 3. Problems / design decisions

P1 (CENTRAL): `priceUnits` is free-form, OXR needs ISO codes (base USD). Items may store "$", "€", "USD", "руб", or arbitrary text.
  DECISION: Conversion is best-effort. Provide a symbol/alias -> ISO map (common symbols: $→USD, €→EUR, £→GBP, ¥→JPY, ₽→RUB, plus accept already-ISO 3-letter codes case-insensitively). An item whose `priceUnits` does not resolve to a currency present in the OXR rates is displayed UNCHANGED (raw price + original units). This keeps the feature safe and non-destructive. The dropdown lists currencies from the server (OXR `currencies` list, ISO codes + names).

P2: OXR free plan base = USD only. Convert from source ISO -> target ISO using USD as pivot: `amountTarget = amountSrc * (rate[target] / rate[source])` where `rate[X]` is USD->X. Source/target must both be in the rates map; otherwise no conversion.

P3: TTL cache invalidation. Server keeps `(lastFetchInstant, ratesSnapshot)`; on access, if `now - lastFetch >= 1h` OR empty, re-fetch from OXR under a mutex (SmartRWLocker / Mutex). Use `kotlinx-datetime` Clock or `dev.inmo.micro_utils`/`DateTime` (auth uses `DateTime.now()` from microutils) — pick the one already on classpath in server commonMain. Will confirm in ARCHITECTURE step.

P4: `isFeatureEnabled()` server-side = (config.openExchangeRatesAppId is non-blank). Client `isFeatureEnabled()` = calls server enabled endpoint (cached). When server disabled, currencies endpoint returns empty / enabled=false; client shows no dropdown, no conversion.

P5: Feature module type. This is a FULL-STACK feature (server fetch + cache + endpoints, client HTTP wrapper). Use ./generate_feature.sh to scaffold `features/currency/{common,server,client}`. Register in settings.gradle, server deps + sample.config.json plugin, client deps + 3 entry points. Then the conversion/dropdown UI is consumed by the EXISTING `features/ui/wishlist` views (no new UI feature module needed) via its WishlistsModel + a shared selection holder. The shared selection holder + currency list access will be added into features/ui/wishlist (its Model/Plugin) depending on features/currency/client.

P6: HTTP-only Ktor rule. `KtorCurrencyFeature` only calls endpoints. A `CurrencyService` (client) wraps it to add the shared selected-currency StateFlow + symbol->ISO mapping + conversion math + client-side caching of the currency list/rates. Conversion math (pure) can live in common.

## 4. Planned changes (high level; detailed in STEP_1)

A. Scaffold feature `currency` via ./generate_feature.sh -> features/currency/{common,server,client}.
B. common: Constants (path parts), models: `CurrencyCode` (ISO value class), `CurrencyInfo(code,name)`, `Rates` (base + map code->Double + fetchedAt), conversion util `convertAmount(...)`, symbol->ISO alias map.
C. server: `CurrencyFeature` interface (isFeatureEnabled, getCurrencies, getRates); `CurrencyService` impl (OXR HTTP fetch via a server HttpClient, in-memory TTL cache 1h, mutex); `CurrencyConfig` (openExchangeRatesAppId, optional ttl); RoutingsConfigurator (enabled / currencies / rates endpoints); Plugin + jvm JVMPlugin. Needs a server-side Ktor HttpClient to call OXR.
D. client: mirror `CurrencyFeature` interface (or reuse common interface); `KtorCurrencyFeature` (HTTP only); `CurrencyService` client wrapper (shared `selectedCurrency` StateFlow, cached currency list + rates, conversion); Plugin + platform plugins.
E. Wire-up: settings.gradle (+3 modules); server/build.gradle (+currency.server); client/build.gradle + client/android/build.gradle (+currency.client); sample.config.json (+ plugin FQCN + `openExchangeRatesAppId:""`); 3 client Main entry points (+JS/JVM/Android plugins).
F. UI integration in features/ui/wishlist: build.gradle depends on currency.client; WishlistsModel gains currency access (isEnabled, currencies, selectedCurrency flow, setSelected, convert(price,units)->displayString); add a `CurrencySelector` composable per platform (JS Bootstrap select + Stylesheet rule, JVM/Android dropdown); each of the 3 views' ViewModels expose currency state + converted display; views render dropdown when enabled and converted prices.
G. README.md: fill currency feature README (required structure; Operator Notes left for human/empty placeholder, NOT authored beyond the empty marker). Update features/ui/wishlist README Architecture Notes (do not touch its Operator Notes).
H. KDocs on all new .kt. JS Stylesheet object for any JS view needing CSS. MVVM rules.

## 5. Build verification plan
- After coding: run gradle build for affected modules. Likely:
  - `./gradlew :wishlist.features.currency.common:build :wishlist.features.currency.server:build :wishlist.features.currency.client:build`
  - `./gradlew :wishlist.features.ui.wishlist:build`
  - Possibly `:client:build` / `:server:build` for wiring. Will choose minimal-but-sufficient set in STEP_2.
- One fix cycle per CODING.md; do not re-fix identical errors — report instead.

## 6. Open questions for operator (non-blocking; proceeding with stated decisions)
- Q1: "WishlistItemCard / just-merged cards feature" does not exist on this branch. Proceeding with WishlistView/UserWishlistsView/WishlistItemView. Confirm acceptable.
- Q2: `priceUnits` is free-form text, not ISO codes. Proceeding with best-effort symbol/code->ISO mapping; unresolved units shown raw. Confirm acceptable (alternative: only convert items whose priceUnits already an ISO code).

## 7. Result
entity=STEP_0; state=COMPLETE; next=STEP_1 Architecturing; stored_in=agents/task/da5a263b-8703-4bb7-9de3-163c61f77a99/STEP_0.md
