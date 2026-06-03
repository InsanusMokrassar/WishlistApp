# Feature: Currency

## Operator Notes

<!-- Human operator writes here. Agents MUST read and respect before making any changes. Agents MUST NOT modify this section. -->

## Overview

Optional full-stack feature that lets the client convert displayed wishlist item prices into a
user-selected target currency. The server fetches the latest exchange rates from the
[Open Exchange Rates](https://docs.openexchangerates.org/reference/api-introduction) (OXR) API,
caches them in-memory with a configurable TTL (default one hour), and exposes read-only endpoints. The client wraps those
endpoints (HTTP only) and adds the shared currency-selection state and conversion logic.

The feature is **optional and disabled by default**: when no OXR App ID is configured server-side,
`isFeatureEnabled()` returns `false`, the currency dropdown is not shown on any screen, and prices are
rendered in their original units with no conversion.

## Routes

All routes are mounted under the `currency` prefix and are public (no authentication required).

| Method | Path                   | Auth | Body / Response          | Description |
|--------|------------------------|------|--------------------------|-------------|
| GET    | `currency/enabled`     | None | `Boolean` (JSON)         | Whether the feature is enabled (an OXR App ID is configured). |
| GET    | `currency/currencies`  | None | `List<CurrencyInfo>`     | Available currencies (code + name); empty when disabled. |
| GET    | `currency/rates`       | None | `CurrencyRates` or `204` | Latest rates snapshot; `204 No Content` when disabled/unavailable. |

## Models

Defined in `features/currency/common`:

- `CurrencyCode` — `@JvmInline value class` wrapping a normalized (trimmed, upper-cased) ISO-4217 code.
- `CurrencyInfo(code, name)` — one selectable dropdown entry.
- `CurrencyRates(base, rates, fetchedAtMillis)` — a rates snapshot; `rates` maps ISO code → base→code
  multiplier; `base` is `USD` on the OXR free plan; `fetchedAtMillis` drives TTL invalidation.
- `CurrencyFeature` — shared capability interface (`isFeatureEnabled`, `getCurrencies`, `getRates`),
  implemented identically by the server (real fetch + cache) and the client (HTTP wrapper).

Utilities (`features/currency/common/utils`):

- `PriceUnitsResolver.resolve(priceUnits): CurrencyCode?` — best-effort mapping of a wishlist item's
  free-form `priceUnits` label (`"$"`, `"€"`, `"USD"`, …) to an ISO code; `null` when unrecognized.
- `convert(amount, from, to, rates): Amount?` — pivots through the snapshot base currency.
- `formatItemPrice(price, priceUnits, target, rates): String` — pure display formatter; returns the
  raw `"price priceUnits"` whenever conversion is impossible (no target/rates, unresolved units, or a
  currency missing from the rates), otherwise the converted amount plus the target currency symbol (falling back to its ISO code).
- `dominantCurrency(priceUnitsList): CurrencyCode?` — identifies the most frequently resolvable currency among items; used by cost-sorting to establish a common comparison baseline.
- `costSortKey(price, priceUnits, common, rates): Double?` — computes a comparable numeric key in the common currency for cost sorting; `null` values sort last.
- `isCostSortAvailable(priceUnitsList, currencyEnabled): Boolean` — determines whether cost sorting is meaningful (true when currency feature is enabled or all priced items already share one currency label).

## Architecture Notes

- **Config:** `CurrencyConfig(openExchangeRatesAppId: String? = null, openExchangeRatesRefreshTTLMillis: Long = 1.hours)` is decoded from the same root
  server config JSON used by the rest of the server (same approach as `features/files` `FilesConfig`).
  The App ID is shown in `server/sample.config.json` as `"openExchangeRatesAppId": null`; a `null`
  (key absent) or blank value ⇒ feature disabled by default. The TTL field (`openExchangeRatesRefreshTTLMillis`, default `3_600_000` ms / one hour) controls cache invalidation. It is **never** hardcoded.
- **Server caching / TTL:** `OpenExchangeRatesService` (server `commonMain`) implements
  `CurrencyFeature`. It caches the rates snapshot and the currency dictionary in-memory; each cache
  entry is invalidated once the TTL (`openExchangeRatesRefreshTTLMillis`, default `3_600_000` ms / one hour) has elapsed since its own last
  successful retrieval (`fetchedAtMillis` for rates; a private timestamp for currencies), so the next
  access triggers a fresh fetch. Fetches are guarded by a `Mutex` and timestamped via
  `korlibs.time.DateTime.now().unixMillisLong`. Upstream failures are logged and fall back to the last
  good cache (or empty/`null`), so an OXR outage never crashes the server.
- **OXR endpoints used:** `GET /api/latest.json?app_id=<id>` (base `USD` on the free plan) and
  `GET /api/currencies.json?app_id=<id>`.
- **Server HTTP client:** registered in the server `Plugin` as a dedicated OkHttp `HttpClient` (named
  Koin qualifier `currencyHttpClient`) with JSON content negotiation. The module targets JVM only, so
  the OkHttp engine reference is safe in `commonMain`.
- **Client Ktor rule:** `KtorCurrencyFeature` (client `commonMain`) only calls the HTTP endpoints — no
  caching, state, or business logic, and is an internal transport (not bound as the public
  `CurrencyFeature`). `CurrencyService` implements `CurrencyFeature`, wraps `KtorCurrencyFeature`, and is
  the binding consumers resolve when they ask for `CurrencyFeature`. It owns the shared
  `selectedCurrency: StateFlow<CurrencyCode?>` (`null` = no conversion), plus light client-side
  memoization of the enabled flag / currency list / rates. The shared `HttpClient` comes from
  `features/common/client`.
- **Conversion correctness:** because items store `priceUnits` as free-form text (not guaranteed ISO
  codes), conversion is best-effort. Items whose units cannot be resolved, or whose source/target
  currency is missing from the rates, are displayed unchanged. UI integration lives in
  `features/ui/wishlist` (see that feature's README).
- **Wiring:** server plugin FQCN
  `dev.inmo.wishlist.features.currency.server.JVMPlugin` is registered in `server/sample.config.json`;
  client plugins (`JSPlugin`/`JVMPlugin`/`AndroidPlugin`) are registered in the three client entry
  points; modules are listed in `settings.gradle`.
