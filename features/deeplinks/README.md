# Feature: DeepLinks

## Operator Notes

<!-- Human operator writes here. Agents MUST read and respect before making any changes. Agents MUST NOT modify this section. -->

## Overview

Server-only feature for storing and resolving declarative deep links. A deep link is identified by a UUID and carries opaque handler info (serialized as polymorphic JSON). The server exposes a resolution HTTP endpoint that invokes registered handlers in sequence until one reports success. Handlers are supplied by consuming features via dependency injection; consuming features must register their handler-info types in the shared `SerializersModule` to enable polymorphic JSON serialization.

## Routes

> All paths below are served under the global `/api` prefix (e.g. `/api/links/{deeplinkId}`). The prefix is applied centrally by `features/common/server` (`InternalApplicationRoutingConfigurator`).

| Method | Path | Auth | Body / Response | Description |
|--------|------|------|-----------------|-------------|
| GET | `/links/{deeplinkId}` | None | `→ 200/404/400` | Resolve deep link by ID; 200 if a handler matched, 404 if not found or unhandled, 400 if ID missing/blank |

## Models

| Type | Module | Description |
|------|--------|-------------|
| `DeepLinkId` | `common/common` | `@Serializable @JvmInline value class` wrapping `String` (UUID) — primary key and capability token |
| `StoredDeepLink` | `server/common` | Persisted record: `id: DeepLinkId`, `handlerInfo: JsonElement` |
| `DeepLinksRepo` | `server/common` | `KeyValueRepo<DeepLinkId, StoredDeepLink>` interface for persistence |
| `ExposedDeepLinksRepo` | `server/jvm` | JVM/PostgreSQL implementation (table `deeplinks`: `deeplink_id TEXT`, `data_json TEXT`) |
| `DeepLinksFeature` | `server/common` | Server interface: `suspend fun createDeepLink(handlerInfo: Any): DeepLinkId`, `suspend fun resolveDeepLink(deeplinkId: DeepLinkId): Boolean` |
| `DeepLinksService` | `server/common` | Implementation of `DeepLinksFeature`; encodes/decodes handler info via `PolymorphicSerializer(Any::class)` |
| `DeepLinkHandler` | `server/common` | `fun interface` for consuming features: `suspend fun tryHandle(deeplinkId: DeepLinkId, handlerInfo: Any): Boolean` — returns `true` if handled |
| `DeepLinksRoutingsConfigurator` | `server/common` | Ktor routing element registering `GET {linksPrefix}/{deeplinkId}` |
| `DeepLinksConstants` | `common/common` | Path constants: `linksPrefixPathPart = "links"`, `deeplinkIdPathParam` |

## Architecture Notes

- **Handler-info polymorphism:** Handler info is stored as opaque `JsonElement` to decouple the server module from consuming feature types. Serialization uses `PolymorphicSerializer(Any::class)` with the shared `Json` instance from `features/common/common`. Consuming features must (1) register their handler-info type in a `SerializersModule` via `singleWithRandomQualifier<SerializersModule> { SerializersModule { polymorphic(Any::class, MyHandlerInfo::class, MyHandlerInfo.serializer()) } }`, and (2) register a `DeepLinkHandler` implementation via `singleWithRandomQualifier<DeepLinkHandler> { MyHandler(...) }`. Handlers are collected with `getAllDistinct()` and tried in registration order until one returns `true`.
- **Handler resolution:** `DeepLinksService.resolveDeepLink` loads the stored handler info from the repo, decodes it polymorphically, and offers it to each registered handler. The first handler to return `true` stops iteration; if all return `false` or no handlers exist, the endpoint returns 404.
- **UUID as capability token:** The `DeepLinkId` (a random UUID string) is the sole capability mechanism — no separate authentication is required. HTTP clients must know or discover the UUID to access the link.
- **Repository pattern:** `ExposedDeepLinksRepo` extends `ExposedKeyValueRepo` with a mapper; the underlying Exposed JDBC initialization happens automatically via the `init {}` block inherited from `ExposedReadKeyValueRepo`, so no explicit `initTable()` call is needed.
- **Server plugin dependency order:** `server/JVMPlugin` must load after `features/common/server/JVMPlugin` (provides `Database` and `Json`) and after `features/deeplinks/common/JVMPlugin` (provides shared models). Server routes are registered relative to `links`; the `/api` prefix is applied centrally by `InternalApplicationRoutingConfigurator`.
- **Client-side:** `client` module is scaffold-only (no business logic) — all deep-link lifecycle (creation, resolution) is server-side.
