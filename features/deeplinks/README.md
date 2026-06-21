# Feature: DeepLinks

## Operator Notes

<!-- Human operator writes here. Agents MUST read and respect before making any changes. Agents MUST NOT modify this section. -->

## Overview

Server-only feature. It stores declared deeplink UUIDs together with attached handler info, mints
deeplinks in-process, and resolves an opened `links/{deeplink_uuid}` by dispatching it to the first
registered handler that claims it.

The feature ships **zero** concrete handlers — it only declares the `DeepLinkHandler` interface and
the dispatch infrastructure. Other features provide their own handlers (registered in their own
server plugins) and mint links via the in-process `DeepLinksService.createDeepLink` API.

The client side is **template scaffold only** (no client code): per issue #45 this is a server-only
feature. The four generated client plugins exist solely so the client dependency graph stays uniform
with every other feature and compiles.

## Routes

| Method | Path | Auth | Body / Response | Description |
|--------|------|------|-----------------|-------------|
| GET | `/links/{deeplink_uuid}` | none | empty body; `200` handled, `404` not-found or unhandled, `400` blank/missing id | User-clickable deeplink, served at the **site root** (NOT under `/api`) via a root-level `KtorApplicationConfigurator`; resolved ahead of the static-SPA fallback by route specificity. |

There is **no HTTP create endpoint**. Creating a deeplink is the in-process
`DeepLinksService.createDeepLink(handlerInfo)` API, called by other server features (avoids
unauthenticated link minting). A present-but-bogus id is a normal lookup miss (`404`), since
`DeepLinkId` is an opaque string and UUID format is not validated.

## Models

Key data types:

- `DeepLinkId` — `@Serializable @JvmInline value class DeepLinkId(val string: String)`; opaque
  server-generated UUID, the `{deeplink_uuid}` path part and the repo primary key.
- `DeepLinkHandlerInfo` — `@Serializable data class DeepLinkHandlerInfo(val type: String, val payload: JsonElement)`;
  the record stored as JSON for each deeplink. `type` is a discriminator naming the owning handler;
  `payload` is that handler's own data class encoded to a `JsonElement`.
- `DeepLinkHandler` — interface in `common` with `suspend fun tryHandle(deeplinkId: DeepLinkId, handlerInfo: Any): Boolean`.
  A handler casts `handlerInfo` to `DeepLinkHandlerInfo`, returns `false` immediately when
  `info.type` is not its own key, otherwise decodes `info.payload` with its own serializer, performs
  its side-effect, and returns `true`. Returning `true` means "owned and processed".
- `DeepLinksRepo : KeyValueRepo<DeepLinkId, DeepLinkHandlerInfo>` — persistent store (Exposed-backed
  `ExposedDeepLinksRepo`, `deeplinks` table, JSON blob value column).
- `HandleResult` — sealed: `NotFound` / `Unhandled` / `Handled`; mapped to HTTP status by the route.

## Architecture Notes

- **Root route, not an `Element`.** The route is a `KtorApplicationConfigurator` that opens its own
  `routing { }` at the site root (like `InternalApplicationRoutingConfigurator`). It is deliberately
  NOT an `ApplicationRoutingConfigurator.Element`: every such `Element` is force-wrapped under `/api`
  plus a `/api` 404 catch-all, so an `Element` could never serve a root `links/...` link.
- **Route specificity beats the SPA fallback.** The explicit `route("links") { get("{deeplink_uuid}") }`
  is strictly more specific than the static `default("index.html")` SPA fallback mounted at root, so
  Ktor resolves the deeplink route regardless of the unordered `getAllDistinct` install order.
- **The `Any` parameter is honored via a JSON-blob discriminator.** No `Any` value is ever
  serialized: only the fully-serializable `DeepLinkHandlerInfo(type, payload)` is persisted. At
  dispatch the decoded record is passed as the `Any` argument. The global `Json` has
  `ignoreUnknownKeys = true`, so a blind `decodeFromJsonElement` of a foreign payload could falsely
  succeed; the `type` gate makes dispatch deterministic and decouples handlers from any central
  polymorphic registry. `type` keys must be **globally unique per handler** (dispatch is
  first-true-wins over the unordered `getAllDistinct<DeepLinkHandler>()` list).
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
