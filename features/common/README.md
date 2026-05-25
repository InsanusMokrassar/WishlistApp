# Feature: Common

## Operator Notes

<!-- Human operator writes here. Agents MUST read and respect before making any changes. Agents MUST NOT modify this section. -->

## Overview

Cross-cutting infrastructure loaded by every consumer. Provides: shared domain models (`Amount`), the echo/ping endpoint, the `HttpClient` with all configurators, server infrastructure setup (Ktor, Exposed/PostgreSQL, Json, ContentNegotiation, GZip, WebSockets, Auth, Sessions, StatusPages), and client navigation infrastructure (`ViewConfig`, `NavigationChain`). `features/common` modules MUST NOT depend on any other internal feature.

## Routes

| Method | Path | Auth | Body / Response | Description |
|--------|------|------|-----------------|-------------|
| GET | `/echo/echo` | None | `→ String` | Server health/status ping; returns a static echo string |

## Models

| Type | Module | Description |
|------|--------|-------------|
| `Amount` | `common/common` | Monetary/numeric amount: `intPart: Long`, `decimalPart: Long` (e.g. 12 and 99 for 12.99) |
| `ViewConfig` | `common/client` | Navigation screen identifier interface; every screen config implements this |
| `EmptyConfig` | `common/client` | Empty `@Serializable` startup config placeholder |

## Architecture Notes

- `common/server/JVMPlugin` is the **root server plugin**: connects PostgreSQL, creates `VersionsRepo`, registers all Ktor configurators, builds `EmbeddedServer<Netty>`.
- `HttpClient` is built once in `common/client/Plugin.kt` by collecting all `HttpClientConfigurator` instances registered via `singleWithRandomQualifier`. Features add configurators (bearer auth, default URL) without touching the client construction.
- `fillAbsentPartsWith` (`utils/MergeUrlBuilders.kt`) merges a stored base URL into per-request URLs — only fills absent components, never overrides already-set ones.
- `Amount` stores value as two `Long` fields (integer and decimal parts) to avoid floating-point precision issues. DB columns: `approx_price_int BIGINT NULL`, `approx_price_dec BIGINT NULL`.
- `EchoFeature` and its routing configurator live under `common/server` (not a separate feature module) because it is a tiny cross-cutting helper.
