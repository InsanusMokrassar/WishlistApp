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
| `findConfig<T, R>` | `common/client` | Extension on `ConfigHolder<T>`: DFS traversal (Chain→Node→subnode→subchains) returning first match of type `R`; defined in `utils/ConfigHolderFind.kt` |
| `actionOrBackUntil` / `pushOrBackUntil` / `replaceLastOrBackUntil` | `common/client` | Navigation helpers in `utils/NavigationPushOrBackUpTo.kt`: back up the chain to an existing node matching `config` (default predicate `node.config == config`) when present, otherwise perform an action — push, replace the last node, or a custom action. Used for contextual Back navigation. |

## Architecture Notes

- `common/server/JVMPlugin` is the **root server plugin**: connects PostgreSQL, creates `VersionsRepo`, registers all Ktor configurators, builds `EmbeddedServer<Netty>`.
- `HttpClient` is built once in `common/client/Plugin.kt` by collecting all `HttpClientConfigurator` instances registered via `singleWithRandomQualifier`. Features add configurators (bearer auth, default URL) without touching the client construction.
- `fillAbsentPartsWith` (`utils/MergeUrlBuilders.kt`) merges a stored base URL into per-request URLs — only fills absent components, never overrides already-set ones.
- `Amount` stores value as two `Long` fields (integer and decimal parts) to avoid floating-point precision issues. DB columns: `approx_price_int BIGINT NULL`, `approx_price_dec BIGINT NULL`.
- `EchoFeature` and its routing configurator live under `common/server` (not a separate feature module) because it is a tiny cross-cutting helper.
- **Web client sub-path (`/ui`)** — the web client (JS single page application) is served under the `/ui` sub-path. `defaultWebClientSubPath = "ui"` (in `common/server` `models/Config.kt`) is the single source of truth: it is the default `staticFolders` key when only a single `staticFolder` is configured, and the redirect target for the root path. The static routing configurator in `common/server/JVMPlugin` (1) adds `get("/") { respondRedirect("/ui") }`, (2) mounts every `staticFolders` entry with `staticFiles(path, dir) { default("index.html") }` so any unmatched `/ui/**` path serves the SPA shell (client-side navigation is in-memory via `NavigationChain`, so deeplinked sub-paths must still boot the app), and (3) logs a WARN when no configured `staticFolders` key normalizes to `ui` (web client would not be served). `client/src/jsMain/resources/index.html` declares `<base href="/ui/">` so its relative asset URLs resolve correctly under `/ui` regardless of sub-path depth.
- **Shared UI components** live in `common/client/src/<jsMain|jvmMain|androidMain>/kotlin/ui/components/ListComponents.kt` (package `dev.inmo.wishlist.features.common.client.ui.components`): `ScreenTitle`, `BackButton`, and `ListRow`. Each is a platform-specific Composable with an identical public signature per target (JS = Bootstrap/Compose-HTML, JVM/Android = Material3); no `expect/actual` is used because callers are themselves per-platform `View`s. `ListRow` has two overloads: `ListRow(text, onSelect, trailing)` for a plain-text row and `ListRow(onSelect, trailing) { content }` for a custom primary slot (badges, prices, secondary text). All list/detail/edit views across `ui/wishlist`, `ui/users`, and `ui/adminPanel` consume these instead of hand-rolling titles, back buttons, and list rows.
