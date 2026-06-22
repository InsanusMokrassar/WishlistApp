# Feature: DeepLinks

## Operator Notes

<!-- Human operator writes here. Agents MUST read and respect before making any changes. Agents MUST NOT modify this section. -->

## Overview

Server-only feature. It stores declared deeplink UUIDs together with attached handler info, mints
deeplinks in-process, and resolves an opened `links/{deeplink_uuid}` by dispatching it to the handler
registered under the stored `DeepLinkHandlerId`.

The feature ships **zero** concrete handlers — it only declares the `DeepLinkHandler` interface and
the dispatch infrastructure. Other features provide their own handlers (registered in their own
server plugins) and mint links via the in-process `DeepLinksService.createDeepLink` API.

The client side is **template scaffold only** (no client code): per issue #45 this is a server-only
feature. The four generated client plugins exist solely so the client dependency graph stays uniform
with every other feature and compiles.

## Routes

| Method | Path | Auth | Body / Response | Description |
|--------|------|------|-----------------|-------------|
| GET | `/api/links/{deeplink_uuid}` | none | empty body; `200` handled, `404` not-found or unhandled, `400` blank/missing id | User-clickable deeplink, served under the standard `/api` prefix via a normal `ApplicationRoutingConfigurator.Element`, auto-wrapped by `InternalApplicationRoutingConfigurator`. |

There is **no HTTP create endpoint**. Creating a deeplink is the in-process
`DeepLinksService.createDeepLink(handlerId, value)` API, called by other server features (avoids
unauthenticated link minting). A present-but-bogus id is a normal lookup miss (`404`), since
`DeepLinkId` is an opaque string and UUID format is not validated.

## Models

Key data types:

- `DeepLinkId` — `@Serializable @JvmInline value class DeepLinkId(val string: String)`; opaque
  server-generated UUID, the `{deeplink_uuid}` path part and the repo primary key.
- `DeepLinkHandlerId` — `@Serializable @JvmInline value class DeepLinkHandlerId(val string: String)`;
  type-safe identifier of a handler, used as the map key in the dispatch service and stored in each
  deeplink record. Handler ids MUST be globally unique; duplicate ids cause startup failure.
- `DeepLinkHandlerInfo` — `@Serializable data class DeepLinkHandlerInfo(val handlerId: DeepLinkHandlerId, @Polymorphic val value: Any)`;
  the record stored as JSON for each deeplink. `handlerId` identifies the owning handler; `value` is
  that handler's own payload, serialized polymorphically via the global `Json` aggregated
  `SerializersModule` (all handler-providing features register their value type via
  `polymorphic(Any::class, T::class, T.serializer())`).
- `DeepLinkHandler` — interface in `common` with `val id: DeepLinkHandlerId` and
  `suspend fun tryHandle(deeplinkId: DeepLinkId, value: Any): Boolean`. The service selects the
  handler by `id` (map lookup), then passes the decoded `value` (no wrapper, no id). The handler
  casts `value` to its concrete type, performs its side-effect, and returns `true` if processed,
  `false` otherwise.
- `DeepLinksRepo : KeyValueRepo<DeepLinkId, DeepLinkHandlerInfo>` — persistent store (Exposed-backed
  `ExposedDeepLinksRepo`, `deeplinks` table, JSON blob value column).
- `HandleResult` — `@Serializable` sealed interface: `NotFound` / `Unhandled` / `Handled`; mapped to HTTP status by the route.

## Architecture Notes

- **Map-based dispatch by `DeepLinkHandlerId`.** The service builds `handlersById: Map<DeepLinkHandlerId, DeepLinkHandler>`
  at construction and looks up the handler directly by `info.handlerId`. Unknown handler ids → `Unhandled`
  (→ `404`). No list scan, no first-true-wins.
- **Polymorphic value serialization via aggregated `SerializersModule`.** `@Polymorphic val value: Any`
  is resolved through the global `Json` (`features/common/common/.../Plugin.kt`, `useArrayPolymorphism = true`)
  which aggregates every Koin-registered `SerializersModule` via `getAllDistinct<SerializersModule>()`.
  Each handler-providing feature MUST register its concrete value type in its server plugin:
  `singleWithRandomQualifier { SerializersModule { polymorphic(Any::class, T::class, T.serializer()) } }`.
  Unregistered value types fail fast with `SerializationException`.
- **Duplicate handler ids throw at service construction.** Registering two handlers under the same `DeepLinkHandlerId`
  causes startup failure (fail-fast during Koin `single{}` build); handler ids MUST be globally unique.
- **Standard `Element` under `/api`.** The route is a normal `ApplicationRoutingConfigurator.Element`,
  auto-wrapped under `/api` by `InternalApplicationRoutingConfigurator`, so the full path is
  `/api/links/{deeplink_uuid}`. No exception or special routing — deeplinks follow the same pattern as
  every other feature.
- **Plain `KeyValueRepo`, no cache.** Deeplinks are write-once (mint) and read-rarely (only when a
  link is opened); there is no hot-read or list traffic, so no `FullCRUDCacheRepo` wrapper is used —
  matching `FilesMetaInfoRepo`.
- **`DeepLinkHandler` lives in `deeplinks/common`** (it references only `DeepLinkId` and `Any`, no
  server/Ktor types) so a handler-providing feature depends only on `deeplinks/common`, never on
  `deeplinks/server`. Handlers register via `singleWithRandomQualifier<DeepLinkHandler> { ... }` in
  their own server plugin; the dispatch infra in `deeplinks/server` collects them.
- **In-process create, no public POST.** `createDeepLink` is a server-only Kotlin API; minting a
  link is not exposed over HTTP to avoid unauthenticated link creation.
- **Empty handler list is correct.** With zero handlers registered, `handle` returns `Unhandled`
  (→ `404`) for any stored link until a feature provides a handler. This is expected infra behavior.
