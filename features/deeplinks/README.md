# Feature: DeepLinks

## Operator Notes

<!-- Human operator writes here. Agents MUST read and respect before making any changes. Agents MUST NOT modify this section. -->

## Overview

Server-only feature for declaring and resolving deeplinks (issue #45). A deeplink is a UUID with an attached, serializable handler-info object. Other server features create deeplinks through the server-only `DeepLinksFeature` and register a `DeepLinkHandler` to react when the deeplink is invoked at `links/<deeplink_uuid>`.

There is no client logic: the `client` module contains only delegating scaffold plugins (boilerplate). Deeplink creation is a server-side concern; resolution is reached over the public HTTP route. The feature depends only on `features/common/common` (common module) and `features/common/server` (server module — `Database`, `Json`, routing collection). It deliberately does not depend on any other feature: handler-info types stay opaque (`JsonElement`) to the storage layer, so consuming features depend on `deeplinks/server`, never the reverse.

The server plugin must be loaded after `features/common/server` (it reuses the `Database` and `Json` singletons). It is registered last in `server/sample.config.json`.

## Routes

| Method | Path | Auth | Body / Response | Description |
|--------|------|------|-----------------|-------------|
| GET | `/links/{deeplinkId}` | none | `→ 200 \| 404 \| 400` | Resolve a deeplink: offers its handler-info to every registered `DeepLinkHandler` until one processes it. `200` = handled; `404` = unknown deeplink or no handler matched; `400` = missing/blank id. |

The route is public on purpose: the deeplink UUID is itself the capability token.

## Models

- `DeepLinkId` — `@Serializable @JvmInline value class(String)` wrapping the deeplink UUID (`features/deeplinks/common`).
- `DeepLinksConstants.linksPrefixPathPart = "links"` — shared root path segment (`features/deeplinks/common`).
- `StoredDeepLink(id: DeepLinkId, handlerInfo: JsonElement)` — persisted record; `handlerInfo` is the polymorphic JSON of the original `Any` (`features/deeplinks/server`).
- `DeepLinkHandler` — `fun interface { suspend fun tryHandle(deeplinkId: DeepLinkId, handlerInfo: Any): Boolean }`. Returns `true` when the handler recognized the handler-info type and tried to process it.
- `DeepLinksFeature` — server-only interface: `createDeepLink(handlerInfo: Any): DeepLinkId`, `resolveDeepLink(deeplinkId): Boolean`.
- `DeepLinksRepo : KeyValueRepo<DeepLinkId, StoredDeepLink>` — persistence contract; Exposed-backed JSON-text impl `ExposedDeepLinksRepo` (table `deeplinks`).
- `DeepLinksService` — server implementation of `DeepLinksFeature`.
- `DeepLinksRoutingsConfigurator` — `ApplicationRoutingConfigurator.Element` registering the `links/{deeplinkId}` route.

## Architecture Notes

- **Handler-info as opaque polymorphic JSON.** The feature never knows the concrete handler-info classes. `DeepLinksService` encodes any handler-info via `Json.encodeToJsonElement(PolymorphicSerializer(Any::class), value)` and decodes it back the same way at resolution time. The shared `Json` (from `features/common/common`) has `useArrayPolymorphism = true` and aggregates every `SerializersModule` registered with `singleWithRandomQualifier<SerializersModule>`, so the inline `[type, body]` array carries the type discriminator. Consuming features MUST register their handler-info subclasses with `polymorphic(Any::class, X::class, X.serializer())` exactly like `ViewConfig` registration; otherwise encode/decode fails.
- **Handler collection.** `DeepLinksService` receives `handlers: List<DeepLinkHandler>` from `getAllDistinct()` in the common `Plugin`. Resolution iterates with short-circuiting `handlers.any { it.tryHandle(...) }`, stopping at the first handler that returns `true`.
- **Persistence.** `ExposedDeepLinksRepo` mirrors `ExposedFilesMetaInfoRepo`: `ExposedKeyValueRepo<String,String>` (`deeplink_id` text PK, `data_json` text, table `deeplinks`) wrapped with `withMapper`, serializing the whole `StoredDeepLink` to a JSON string. `initTable()` runs implicitly through the `ExposedKeyValueRepo` constructor.
- **Server-only.** `DeepLinksService` is bound only as `DeepLinksFeature` (no client interface, no Ktor client class), matching the `FilesService` convention. The `client` module is scaffold-only to keep the documented feature-adding wiring consistent.
- **DI layering.** Common `Plugin` registers the service, the `DeepLinksFeature` binding and the routing configurator. JVM `JVMPlugin` adds the `DeepLinksRepo` (Exposed) binding and delegates to the common module + common `Plugin`. `Database`/`Json` come from `features/common/server`.
