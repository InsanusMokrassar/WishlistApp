Model: claude-opus-4-8 (Opus 4.8 — per PLAN priority fable/opus/sonnet; fable unavailable, opus = highest available, best for ambiguity-resolving design)
Execution time: 480
Tokens used: ~120000
Changed files: agents/task/3ae853f7-b464-4d51-82c8-fcdf97d6a92f/1-Architecture.md

# ARCHITECTURE — issue #45 deeplinks feature (server-only)

Dense AML-HIP. Turns `0-Planning.md` into a mechanical build spec. Source-verified against `features/{files,booking,common,sample}` and `MicroUtils`. Coding role implements verbatim; deviations only where a `// CODING:` note explicitly permits.

GROUND TRUTH (verified this step):
- `KtorApplicationConfigurator` = `interface { fun Application.configure() }` (MicroUtils `dev.inmo.micro_utils.ktor.server.configurators`). Root-level static-files configurator in `common/server JVMPlugin` proves the root-routing idiom (`singleWithRandomQualifier<KtorApplicationConfigurator>{ ApplicationRoutingConfigurator(listOf(Element{ ... })) }`). We instead implement our OWN `KtorApplicationConfigurator` that opens `routing{}` directly (like `InternalApplicationRoutingConfigurator.configure()` does).
- `ExposedFilesMetaInfoRepo` pattern = top-level `private fun createDelegate(database, json): KeyValueRepo<K,V>` building `ExposedKeyValueRepo<String,String>(database, {text(col)}, {text(col)}, "table").withMapper<K,V,String,String>(keyFromToTo, valueFromToTo, keyToToFrom, valueToToFrom)`, then `class ExposedXxx(database, json) : XxxRepo, KeyValueRepo<K,V> by createDelegate(database, json)`.
- `FileId` = `@Serializable @JvmInline value class FileId(val string: String)`.
- generate_feature.sh substitutes `{{$module_package}}` → `features.deeplinks`. Generated packages: `dev.inmo.wishlist.features.deeplinks.{common,server,client}`. Generated `common/.../Constants.kt` = ONLY a `package` line (NO object). Generated `common/Plugin.kt` empty; `common/jvmMain/JVMPlugin.kt` delegates `with(Plugin){setupDI}`; `server/commonMain/Plugin.kt` empty; `server/jvmMain/JVMPlugin.kt` delegates to `deeplinks.common.JVMPlugin` + server `Plugin`; client 4 plugins empty-delegating.
- booking `server/jvmMain/JVMPlugin` delegates to `booking.common.JVMPlugin` + server `Plugin` (no extra bindings) — exact shape we keep.
- Route param read idiom (verified `WishlistItemRoutingsConfigurator`): `call.parameters["x"]?...?: run { call.respond(HttpStatusCode.BadRequest); return@get }`.
- uuid4 = `com.benasher44.uuid.uuid4()` (FilesService import line 3).

---

## A. FINAL MODULE / FILE TREE

Root package per module: `dev.inmo.wishlist.features.deeplinks.<sub>`.
Legend: NEW = create new file; EDIT = modify generated scaffold; SCAFFOLD = leave generated file untouched.

### `features/deeplinks/common`  (template `mppJvmJsAndroid`, `com.android.library`)

```
common/src/commonMain/kotlin/
  Constants.kt                         EDIT  (generated has only package line)
  DeepLinkHandler.kt                   NEW
  models/DeepLinkId.kt                 NEW
  models/DeepLinkHandlerInfo.kt        NEW
  repo/DeepLinksRepo.kt                NEW
  Plugin.kt                            SCAFFOLD (empty)
common/src/jvmMain/kotlin/
  repo/ExposedDeepLinksRepo.kt         NEW
  JVMPlugin.kt                         EDIT  (bind DeepLinksRepo)
common/src/jsMain/kotlin/JSPlugin.kt   SCAFFOLD
common/src/androidMain/kotlin/AndroidPlugin.kt  SCAFFOLD
common/build.gradle                    SCAFFOLD (template default deps suffice — see G)
```

File-by-file (package, types, signatures, one-line KDoc intent):

**`common/.../Constants.kt`** — pkg `dev.inmo.wishlist.features.deeplinks.common`
- `object DeepLinksConstants` — KDoc: "Shared URL path parts for the deeplinks feature, kept in one place so server routing and any future client never drift."
  - `const val linksPrefixPathPart = "links"` — KDoc: "Root path segment under which a deeplink resolves: `GET /links/{deeplink_uuid}` (served at SITE ROOT, NOT under `/api`)."
  - `const val deeplinkIdParameter = "deeplink_uuid"` — KDoc: "Name of the path parameter holding the deeplink UUID; shared by the route declaration and the handler reading `call.parameters[...]`."

  > Naming note: object is `DeepLinksConstants` (matches `EchoConstants` precedent in `common/common`). The CODING role MUST replace the generated bare-package `Constants.kt` with this object (do not add a second file).

**`common/.../DeepLinkId.kt`** under `models/` — pkg `...deeplinks.common.models`
- `@Serializable @JvmInline value class DeepLinkId(val string: String)` — KDoc: "Type-safe opaque identifier of a stored deeplink (a server-generated UUID); the `{deeplink_uuid}` path part of `links/{uuid}`." `@property string` KDoc: "Raw UUID value." Mirror `FileId.kt` exactly (imports `kotlinx.serialization.Serializable`, `kotlin.jvm.JvmInline`).

**`common/.../models/DeepLinkHandlerInfo.kt`** — pkg `...deeplinks.common.models`
- `@Serializable data class DeepLinkHandlerInfo(val type: String, val payload: JsonElement)` — KDoc: "Serializable record stored as JSON for each deeplink. `type` is a caller-chosen discriminator naming the owning handler; `payload` is that handler's own data class already encoded to a `JsonElement`. This is the object decoded at dispatch time and passed as `handlerInfo: Any` to every `DeepLinkHandler`." `@property type` KDoc: "Logical handler key; a handler claims a deeplink only when this equals its own constant." `@property payload` KDoc: "Opaque per-handler JSON body; the owning handler decodes it with its own serializer." Import `kotlinx.serialization.json.JsonElement`, `kotlinx.serialization.Serializable`. (`JsonElement` is `@Serializable` via kotlinx — no `@Contextual` needed.)

**`common/.../DeepLinkHandler.kt`** — pkg `dev.inmo.wishlist.features.deeplinks.common`
- `interface DeepLinkHandler` — KDoc: "Contract a feature implements to react when one of ITS deeplinks is opened. The deeplinks feature ships ZERO implementations; it only declares this interface and the dispatch infrastructure. Lives in `common` so a handler-providing feature depends only on `deeplinks/common`, not on the server module."
  - `suspend fun tryHandle(deeplinkId: DeepLinkId, handlerInfo: Any): Boolean` — KDoc (CONTRACT — quote in code): "Attempt to process the opened deeplink. `handlerInfo` is the decoded `DeepLinkHandlerInfo` (cast it: `(handlerInfo as? DeepLinkHandlerInfo)`). A handler MUST: (1) return `false` immediately if `info.type` is not its own key; (2) otherwise decode `info.payload` with its own serializer, perform its side-effect, and return `true`. Returning `true` means 'this handler owned and processed the deeplink'; the service then stops and reports it handled. Returning `false` means 'not mine / could not process', and the service tries the next handler. EXACT signature mandated by issue #45 — do not change the `Any` parameter."

  > The param type is `Any` verbatim per issue. Concrete cast/contract documented above; this is the ONLY place the `Any`↔`DeepLinkHandlerInfo` bridge is specified.

**`common/.../repo/DeepLinksRepo.kt`** — pkg `...deeplinks.common.repo`
- `interface DeepLinksRepo : KeyValueRepo<DeepLinkId, DeepLinkHandlerInfo>` — KDoc: "Persistent store mapping each `DeepLinkId` to its `DeepLinkHandlerInfo`. Implemented on the server by an Exposed-backed `KeyValueRepo` (see `ExposedDeepLinksRepo`). Mirrors `FilesMetaInfoRepo`." Import `dev.inmo.micro_utils.repos.KeyValueRepo`.

**`common/jvmMain/.../repo/ExposedDeepLinksRepo.kt`** — pkg `...deeplinks.common.repo`
- top-level `private fun createDelegate(database: Database, json: Json): KeyValueRepo<DeepLinkId, DeepLinkHandlerInfo>` — KDoc: "Builds the mapped `KeyValueRepo` backing `ExposedDeepLinksRepo`; stores each `DeepLinkHandlerInfo` as a JSON string in `deeplinks` table (`deeplink_id` text PK, `handler_info_json` text), reusing the `ExposedKeyValueRepo` + `withMapper` pattern from `ExposedFilesMetaInfoRepo`. `@param database` / `@param json` documented."
  - body:
    ```
    ExposedKeyValueRepo<String, String>(
        database = database,
        keyColumnAllocator = { text("deeplink_id") },
        valueColumnAllocator = { text("handler_info_json") },
        tableName = "deeplinks"
    ).withMapper<DeepLinkId, DeepLinkHandlerInfo, String, String>(
        keyFromToTo = { string },
        valueFromToTo = { json.encodeToString(DeepLinkHandlerInfo.serializer(), this) },
        keyToToFrom = { DeepLinkId(this) },
        valueToToFrom = { json.decodeFromString(DeepLinkHandlerInfo.serializer(), this) }
    )
    ```
- `class ExposedDeepLinksRepo(database: Database, json: Json) : DeepLinksRepo, KeyValueRepo<DeepLinkId, DeepLinkHandlerInfo> by createDelegate(database, json)` — KDoc: "Exposed JDBC implementation of `DeepLinksRepo` backed by the `deeplinks` table; persists `DeepLinkHandlerInfo` as JSON text. `@param database` / `@param json`."
- Imports: `dev.inmo.micro_utils.repos.KeyValueRepo`, `...exposed.keyvalue.ExposedKeyValueRepo`, `...mappers.withMapper`, models, `kotlinx.serialization.json.Json`, `org.jetbrains.exposed.v1.jdbc.Database`.
- NOTE: `ExposedKeyValueRepo` self-inits its table (the `withMapper`/Exposed KV ctor handles table creation; `ExposedFilesMetaInfoRepo` has NO explicit `initTable()` block and works — same here, do NOT add an `init{}`).

**`common/jvmMain/.../JVMPlugin.kt`** — EDIT generated. After `with(Plugin){setupDI(config)}` add:
```
single<DeepLinksRepo> { ExposedDeepLinksRepo(get(), get()) }   // get()=Database, get()=Json
```
KDoc on the object: "JVM startup plugin for deeplinks common module; registers the Exposed-backed `DeepLinksRepo`. The `Database` comes from `features/common/server`, `Json` from `features/common/common`, so this plugin must run after the common server plugin." Keep generated `startPlugin` delegation untouched.

### `features/deeplinks/server`  (template `mppJavaProject`)

```
server/src/commonMain/kotlin/
  services/DeepLinksService.kt                NEW
  models/HandleResult.kt                      NEW
  configurators/DeepLinksRoutingConfigurator.kt  NEW
  Plugin.kt                                   EDIT (register service + root configurator)
server/src/jvmMain/kotlin/JVMPlugin.kt        SCAFFOLD (generated delegation is exactly right)
server/build.gradle                           SCAFFOLD (template deps suffice — see G)
```

**`server/.../models/HandleResult.kt`** — pkg `...deeplinks.server.models`
- `sealed interface HandleResult` — KDoc: "Outcome of resolving an opened deeplink, mapped to an HTTP status by `DeepLinksRoutingConfigurator`."
  - `data object NotFound : HandleResult` — KDoc: "No deeplink stored for the given id → HTTP 404."
  - `data object Unhandled : HandleResult` — KDoc: "Deeplink exists but no registered handler claimed it → HTTP 404 (a stored-but-dead link is indistinguishable to the caller from a missing one)."
  - `data object Handled : HandleResult` — KDoc: "A handler owned and processed the deeplink → HTTP 200."

  > Declared as a separate file (not nested in the service) so the configurator imports it cleanly. `data object` requires Kotlin 1.9+ (project is on modern Kotlin — `data object` is fine; if the Coding role hits a target issue, fall back to `object`).

**`server/.../services/DeepLinksService.kt`** — pkg `...deeplinks.server.services`
- `class DeepLinksService(private val repo: DeepLinksRepo, private val handlers: List<DeepLinkHandler>)` — KDoc: "Server-only, in-process API for the deeplinks feature: mints deeplinks with attached handler info and dispatches an opened deeplink to the first handler that claims it. There is NO public HTTP create endpoint — other server features call `createDeepLink` directly."
  - `suspend fun createDeepLink(handlerInfo: DeepLinkHandlerInfo): DeepLinkId` — KDoc: "Generate a fresh UUID, persist `id → handlerInfo`, and return the new id. Server-only; the caller (another feature) constructs the `DeepLinkHandlerInfo(type, payload)` for its own handler." Body: `val id = DeepLinkId(uuid4().toString()); repo.set(id, handlerInfo); return id`. (Import `dev.inmo.micro_utils.repos.set` — verified used by FilesService — or use the KeyValueRepo `set`; CODING: use the `dev.inmo.micro_utils.repos.set` extension that FilesService imports.)
  - `suspend fun handle(deeplinkId: DeepLinkId): HandleResult` — KDoc: "Load the stored `DeepLinkHandlerInfo`; if absent return `NotFound`. Otherwise pass it (as `Any`) to each registered `DeepLinkHandler.tryHandle` in turn, returning `Handled` at the first `true`; if none claim it return `Unhandled`."
    Body (MUST use the loop + early return; NO else-if):
    ```
    val info = repo.get(deeplinkId) ?: return HandleResult.NotFound
    handlers.forEach { handler ->
        if (handler.tryHandle(deeplinkId, info)) {
            return HandleResult.Handled
        }
    }
    return HandleResult.Unhandled
    ```
  - Imports: `com.benasher44.uuid.uuid4`, `dev.inmo.micro_utils.repos.set`, models, repo, `DeepLinkHandler`.

  > `handlers: List<DeepLinkHandler>` is resolved once at construction via `getAllDistinct()` in the server `Plugin`. ZERO handlers ship → empty list → `handle` returns `Unhandled` for any existing link. This is correct infra behavior (R6). `getAllDistinct` snapshot is taken at service-construction (singleton) time — acceptable: all plugins finish `setupDI` before any `single{}` is first resolved at server start.

**`server/.../configurators/DeepLinksRoutingConfigurator.kt`** — pkg `...deeplinks.server.configurators`
- `class DeepLinksRoutingConfigurator(private val service: DeepLinksService) : KtorApplicationConfigurator` — KDoc: "Installs the user-facing deeplink route `GET /links/{deeplink_uuid}` at the SITE ROOT (NOT under `/api`). Implemented as a `KtorApplicationConfigurator` opening its own `routing{}` (like `InternalApplicationRoutingConfigurator`), because every `ApplicationRoutingConfigurator.Element` is force-wrapped under `/api` by `InternalApplicationRoutingConfigurator` plus a `/api` 404 catch-all — so an Element could never serve a root `links/...` link. Route specificity makes this explicit route win over the static-SPA `default(\"index.html\")` fallback mounted at root."
  - `override fun Application.configure()`:
    ```
    routing {
        route(DeepLinksConstants.linksPrefixPathPart) {
            get("{${DeepLinksConstants.deeplinkIdParameter}}") {
                val raw = call.parameters[DeepLinksConstants.deeplinkIdParameter]
                val deeplinkId = raw?.takeIf { it.isNotBlank() }?.let(::DeepLinkId) ?: run {
                    call.respond(HttpStatusCode.BadRequest)
                    return@get
                }
                val status = when (service.handle(deeplinkId)) {
                    HandleResult.Handled -> HttpStatusCode.OK
                    HandleResult.NotFound -> HttpStatusCode.NotFound
                    HandleResult.Unhandled -> HttpStatusCode.NotFound
                }
                call.respond(status)
            }
        }
    }
    ```
  - Imports: `dev.inmo.micro_utils.ktor.server.configurators.KtorApplicationConfigurator`, `io.ktor.server.application.Application`, `io.ktor.server.routing.{routing,route,get}`, `io.ktor.server.response.respond`, `io.ktor.http.HttpStatusCode`, `DeepLinksConstants`, `DeepLinkId`, `HandleResult`.
  - `when` over the sealed `HandleResult` (exhaustive, no else; NotFound and Unhandled both → 404 by design, two explicit arms — NOT an else-if). VALID uuid that decodes but is structurally not a uuid: we do NOT validate UUID format (DeepLinkId is opaque String); a non-uuid id simply won't be found → 404. "invalid uuid → 400" from the task is realized as "blank/missing param → 400"; a present-but-bogus string is a normal lookup-miss 404. Document this in README.

**`server/.../Plugin.kt`** — EDIT generated empty body to:
```
single { DeepLinksService(get(), getAllDistinct()) }   // get()=DeepLinksRepo, getAllDistinct()=List<DeepLinkHandler>
singleWithRandomQualifier<KtorApplicationConfigurator> { DeepLinksRoutingConfigurator(get()) }
```
KDoc: "Server startup plugin for deeplinks. Registers `DeepLinksService` (collecting all `DeepLinkHandler`s via `getAllDistinct`) and the root-level `DeepLinksRoutingConfigurator` as a `KtorApplicationConfigurator` (NOT an `ApplicationRoutingConfigurator.Element`) so `GET /links/{uuid}` is served at the site root, not under `/api`." Imports: `dev.inmo.micro_utils.koin.{singleWithRandomQualifier,getAllDistinct}`, `...configurators.KtorApplicationConfigurator`, service+configurator.

**`server/jvmMain/.../JVMPlugin.kt`** — SCAFFOLD. Generated form already does `with(deeplinks.common.JVMPlugin){setupDI}` + `with(Plugin){setupDI}` and mirrored `startPlugin`. That brings the repo (common.jvm) and service+configurator (server.Plugin) into the graph. Add a KDoc to the object (the file is otherwise correct as generated): "JVM server plugin; delegates to deeplinks common JVM plugin (Exposed repo) and the server `Plugin` (service + root route). Requires `features/common/server` (Database, Json, `KtorApplicationConfigurator` collection) loaded earlier." No body change beyond KDoc.

### `features/deeplinks/client`  (scaffold ONLY — server-only feature)

```
client/src/commonMain/kotlin/Plugin.kt          SCAFFOLD (empty)
client/src/jvmMain/kotlin/JVMPlugin.kt          SCAFFOLD
client/src/jsMain/kotlin/JSPlugin.kt            SCAFFOLD
client/src/androidMain/kotlin/AndroidPlugin.kt  SCAFFOLD
client/build.gradle                             SCAFFOLD (template default: api deeplinks.common + common.client)
```
NO models, NO feature interface, NO Ktor client, NO HTTP code. All four generated plugins keep empty/delegating `setupDI`. They exist only so the client graph stays uniform with every other feature and compiles. (Issue: "must not have any code excluding template one on client side".)

> KDoc rule vs scaffold: generated scaffold `.kt` files currently have NO KDoc. CODING role MUST add a one-line KDoc to every `object` it TOUCHES (the edited ones: common Constants, common.jvm JVMPlugin, server Plugin). Untouched pure-scaffold client plugins are generated artifacts; do not gold-plate them with KDoc unless the build/lint requires it — but if CODING opens/edits any of them, add a one-liner. (This matches existing repo state where booking/sample scaffold client plugins carry minimal/none KDoc.)

---

## B. HANDLER-INFO MODEL — the `Any` resolved concretely

- STORED REPRESENTATION: `@Serializable data class DeepLinkHandlerInfo(val type: String, val payload: JsonElement)`. Fully serializable (JsonElement is kotlinx-serializable). NO `Any` is ever serialized.
- PERSISTENCE: `DeepLinkId → DeepLinkHandlerInfo` via `DeepLinksRepo` (`KeyValueRepo`); Exposed impl encodes the WHOLE `DeepLinkHandlerInfo` to a JSON string in `handler_info_json` using the injected `Json` (`json.encodeToString(DeepLinkHandlerInfo.serializer(), this)`), decodes symmetrically. One blob column, no per-field schema (same as files meta).
- DISPATCH BRIDGE: `DeepLinksService.handle` loads the decoded `DeepLinkHandlerInfo` object and passes THAT SAME OBJECT as `handlerInfo: Any` to each `DeepLinkHandler.tryHandle`. The decoded value the service holds == the value handlers receive (contract documented on the interface).
- HANDLER CONTRACT (how a future concrete handler inspects it — this feature ships none):
  1. `val info = handlerInfo as? DeepLinkHandlerInfo ?: return false`
  2. `if (info.type != MY_TYPE_CONSTANT) return false`
  3. `val data = runCatching { json.decodeFromJsonElement(MyPayload.serializer(), info.payload) }.getOrNull() ?: return false`
  4. perform side-effect; `return true`.
- WHY the `type` discriminator (not raw `JsonElement`): the global `Json` has `ignoreUnknownKeys=true`, so a blind `decodeFromJsonElement` of a foreign payload can falsely succeed. The `type` gate makes dispatch deterministic and decouples handlers from any central polymorphic registry while honoring the exact `Any` signature (R1/R2 resolved). README documents that `type` keys must be globally unique per handler (R3).

---

## C. REPOSITORY LAYER

- INTERFACE (common): `DeepLinksRepo : KeyValueRepo<DeepLinkId, DeepLinkHandlerInfo>` — single interface, in `deeplinks/common/commonMain/repo`. No read/write split: this is internal infra with exactly two operations (`set` on create, `get` on dispatch); the CRUD-split convention is for client-facing entity repos (users/wishlist), not a 2-call KV blob (files uses a single `FilesMetaInfoRepo` for the same reason — we follow files, not the CRUD section).
- EXPOSED IMPL location: `deeplinks/common/jvmMain/repo/ExposedDeepLinksRepo.kt`, bound in `deeplinks/common/jvmMain/JVMPlugin`. RATIONALE: this is the FILES placement (`files/common/jvmMain` holds `ExposedFilesMetaInfoRepo`; binding is in files plugins). The repo type is a common-layer abstraction; only the Exposed realization is JVM. Server module never needs a server-only repo variant. (Booking binds in common/jvmMain too — both references agree.)
- CACHE WRAPPER: NONE. A `FullCRUDCacheRepo`/`CacheRepo` is unwarranted: deeplinks are write-once (mint), read-rarely (only when a link is opened), and there is no hot-loop read or list/getAll traffic. Files' `FilesMetaInfoRepo` likewise uses a PLAIN mapped `KeyValueRepo` with no cache — we match it. (Booking caches because it does frequent reads in authorization checks; deeplinks do not.) A plain `KeyValueRepo` (files-style) is sufficient and simpler. Justification recorded in README Architecture Notes.

---

## D. DeepLinksService SEMANTICS (server, commonMain)

- `createDeepLink(handlerInfo: DeepLinkHandlerInfo): DeepLinkId`: `DeepLinkId(uuid4().toString())` → `repo.set(id, handlerInfo)` → return id. Pure mint+persist, no validation of `type`/`payload` (the calling feature owns correctness). Server-only Koin `single`; no HTTP exposure (D4).
- `handle(deeplinkId): HandleResult` branch semantics (exhaustive, `when`/early-return, NO else-if):
  - `repo.get(id) == null` → `HandleResult.NotFound`.
  - else iterate `handlers`; first `tryHandle == true` → `HandleResult.Handled` (short-circuit).
  - loop exhausted with no `true` → `HandleResult.Unhandled`.
- Return type is the sealed `HandleResult`, so the route maps outcome→status without re-inspecting the repo. Three distinct outcomes are preserved end-to-end (the route collapses NotFound+Unhandled to 404, but the service keeps them distinct for testability/future redirect semantics).

---

## E. ROOT-ROUTE CONFIGURATOR — HTTP responses

`DeepLinksRoutingConfigurator : KtorApplicationConfigurator`, root `routing { route("links") { get("{deeplink_uuid}") { ... } } }`. Status map:
| Condition | Status | Justification |
|---|---|---|
| param missing or blank | `400 Bad Request` | malformed request (the task's "invalid uuid → 400" realized as empty/missing id) |
| `HandleResult.NotFound` (no stored link) | `404 Not Found` | unknown resource; also covers a present-but-bogus id (opaque DeepLinkId → simple lookup miss) |
| `HandleResult.Unhandled` (stored, no handler claimed) | `404 Not Found` | a stored link no handler owns is dead to the caller — indistinguishable from missing; avoids leaking link existence. (Planning Q2 default; documented) |
| `HandleResult.Handled` | `200 OK` | handler performed its side-effect; richer redirect/body semantics deferred to a future `HandleResult` extension (out of scope) |

Route-ordering invariant (R5): the explicit `route("links"){ get("{...}") }` is strictly more specific than the static `default("index.html")` SPA fallback at root, so Ktor resolves the deeplink route regardless of `getAllDistinct` (unordered) install order. Validation MUST `curl /links/<random>` and assert 404 (NOT a 200 index.html).

---

## F. PLUGIN WIRING (exact)

| Plugin | Action |
|---|---|
| `deeplinks/common` commonMain `Plugin` | SCAFFOLD empty. (No common-platform DI needed; models/interface are pure types.) |
| `deeplinks/common` `JVMPlugin` | EDIT: `with(Plugin){setupDI}` (kept) + `single<DeepLinksRepo>{ ExposedDeepLinksRepo(get(), get()) }`. |
| `deeplinks/common` `JSPlugin` / `AndroidPlugin` | SCAFFOLD (delegate to Plugin; no JS/Android impl — repo is JVM-only). |
| `deeplinks/server` commonMain `Plugin` | EDIT: `single{ DeepLinksService(get(), getAllDistinct()) }` + `singleWithRandomQualifier<KtorApplicationConfigurator>{ DeepLinksRoutingConfigurator(get()) }`. |
| `deeplinks/server` `JVMPlugin` | SCAFFOLD: delegates to `deeplinks.common.JVMPlugin` + server `Plugin` (generated form correct) + add object KDoc. |
| `deeplinks/client` (4 plugins) | SCAFFOLD ONLY (empty/delegating). |

WHERE `DeepLinkHandler` LIVES — DECISION: **`deeplinks/common/commonMain`** (`dev.inmo.wishlist.features.deeplinks.common.DeepLinkHandler`). JUSTIFICATION: the interface references only `DeepLinkId` (common model) + `Any` — no server/Ktor/JVM types — so it is legal in commonMain. A handler-providing feature registers its handler via `singleWithRandomQualifier<DeepLinkHandler>{ ... }` in ITS OWN server module, depending only on `deeplinks/common` (NOT `deeplinks/server`). The dispatch infra (`DeepLinksService`, `getAllDistinct<DeepLinkHandler>`) lives in `deeplinks/server` and consumes whatever handlers are on the graph. This keeps the dependency arrow correct: feature→deeplinks.common (cheap), and only the server entrypoint pulls deeplinks.server. `DeepLinkHandlerInfo` and `DeepLinkId` likewise in common (a caller must build a `DeepLinkHandlerInfo` to mint a link, and `createDeepLink` is server-side, so the calling feature's server module depends on `deeplinks.server` for the service AND transitively gets common types — fine).

---

## G. BUILD / WIRING FILE CHANGES (exact text)

1. **`settings.gradle`** — insert after the `:features:booking:*` block (after line 38, before `:features:ui:sample`):
   ```
       ":features:deeplinks:common",
       ":features:deeplinks:server",
       ":features:deeplinks:client",
   ```
2. **`server/build.gradle`** — after line 23 (`api project(":wishlist.features.booking.server")`):
   ```
       api project(":wishlist.features.deeplinks.server")
   ```
3. **`server/sample.config.json`** — make line 22 end with a comma and append after it:
   ```
       "dev.inmo.wishlist.features.booking.server.JVMPlugin",
       "dev.inmo.wishlist.features.deeplinks.server.JVMPlugin"
   ```
   (Order: after common.server so Database/Json/configurator-collection exist; placed last is safe — deeplinks needs no other feature.)
4. **`client/build.gradle`** — after line 28 (`api project(":wishlist.features.booking.client")`), before the `ui.*` group:
   ```
                   api project(":wishlist.features.deeplinks.client")
   ```
5. **`client/android/build.gradle`** — NO CHANGE (no per-feature lines there; android inherits client transitively — verified empty grep, matches booking/files).
6. **Client entrypoints** — add the empty scaffold client plugin to all three, in the `features.*.client` group right after the booking line:
   - `client/src/jsMain/kotlin/Main.kt` after L26: `dev.inmo.wishlist.features.deeplinks.client.JSPlugin,`
   - `client/src/jvmMain/kotlin/Main.kt` after L22: `dev.inmo.wishlist.features.deeplinks.client.JVMPlugin,`
   - `client/android/src/main/kotlin/MainActivity.kt` after L37: `dev.inmo.wishlist.features.deeplinks.client.AndroidPlugin,`
7. **`deeplinks/common/build.gradle`** — SCAFFOLD default is sufficient: template emits `api project(":wishlist.features.common.common")` under `mppJvmJsAndroid`. That transitively provides `dev.inmo.micro_utils.repos.*` (`KeyValueRepo`, `ExposedKeyValueRepo`, `withMapper`), `Json`, serialization — same template files/booking common use. NO extra deps. (If, and only if, `withMapper`/`ExposedKeyValueRepo` fail to resolve at compile, mirror `features/files/common/build.gradle`'s explicit `api libs.microutils.repos.common` line — CODING fallback, not expected.)
8. **`deeplinks/server/build.gradle`** — SCAFFOLD default is sufficient: template emits `api project(":wishlist.features.deeplinks.common")` + `api project(":wishlist.features.common.server")` under `mppJavaProject`. `features/common/server` provides `KtorApplicationConfigurator`, `getAllDistinct`, `singleWithRandomQualifier`, `Database`, `Json`, and Ktor server routing. NO `features/auth/server` dep (create is in-process, no auth-gated route — D4). NO extra deps.

---

## H. README.md CONTENT PLAN — `features/deeplinks/README.md`

Full required structure (CODING writes the prose; substance specified here):
- `# Feature: DeepLinks`
- `## Operator Notes` — EMPTY placeholder comment ONLY: `<!-- Human operator writes here. Agents MUST read and respect before making any changes. Agents MUST NOT modify this section. -->` (never author content here).
- `## Overview` — Server-only feature. Stores declared deeplink UUIDs + attached handler info; mints deeplinks in-process; resolves an opened `links/{uuid}` to the first handler that claims it. Ships ZERO concrete handlers — only the `DeepLinkHandler` interface and dispatch infra; other features provide handlers. Client side is template scaffold only (no client code).
- `## Routes` table — single row:
  | Method | Path | Auth | Body / Response | Description |
  |GET|`/links/{deeplink_uuid}`|none|empty body; 200 handled / 404 not-found or unhandled / 400 blank id|User-clickable deeplink, served at SITE ROOT (NOT `/api`) via a root-level `KtorApplicationConfigurator`; resolved ahead of the static-SPA fallback by route specificity.|
  Plus a note: there is NO HTTP create endpoint — creation is the in-process `DeepLinksService.createDeepLink` API called by other server features.
- `## Models` — `DeepLinkId` (value class, UUID); `DeepLinkHandlerInfo(type, payload: JsonElement)` (stored-as-JSON discriminated record); `DeepLinkHandler` interface (`tryHandle(deeplinkId, handlerInfo: Any): Boolean`, with the cast-to-`DeepLinkHandlerInfo` contract and the `type`-gate-then-decode rule); `DeepLinksRepo : KeyValueRepo<DeepLinkId, DeepLinkHandlerInfo>`; `HandleResult` sealed (NotFound/Unhandled/Handled).
- `## Architecture Notes` — (1) root route via `KtorApplicationConfigurator` not `Element` and WHY (`/api` wrapping + 404 catch-all); (2) route specificity beats static SPA fallback; (3) `Any`→`DeepLinkHandlerInfo` JSON-blob design + `type` discriminator + `ignoreUnknownKeys` mitigation; (4) plain `KeyValueRepo` (no cache) and why; (5) `DeepLinkHandler` in common so handler features depend only on `deeplinks/common`; register via `singleWithRandomQualifier<DeepLinkHandler>`; `type` keys must be globally unique (first-true-wins, unordered `getAllDistinct`); (6) in-process create, no public POST (rationale: avoid unauthenticated link minting).
  ARCHITECTURE role updates this section after coding per ARCHITECTURE.md.

---

## I. RISKS / INVARIANTS for CODING (must respect)

- **INV1 root configurator type**: route MUST be a `KtorApplicationConfigurator` (own `routing{}`), registered `singleWithRandomQualifier<KtorApplicationConfigurator>`. NEVER an `ApplicationRoutingConfigurator.Element` (that forces `/api` + hits the 404 catch-all). This is THE load-bearing difference vs booking/files and a likely cause of a prior silent-rejected PR (R8).
- **INV2 client = scaffold only**: do NOT add any model/feature/HTTP/Ktor code under `deeplinks/client`. Issue forbids it; another likely prior-PR failure mode.
- **INV3 no public create route**: do NOT add a `POST` create endpoint. Create is in-process `DeepLinksService.createDeepLink` (D4). (Operator Q1 default; flagged.)
- **INV4 never serialize `Any`**: only `DeepLinkHandlerInfo` is persisted; the `Any` exists solely as the dispatch parameter.
- **INV5 control flow**: `handle` uses loop + early-return, route uses exhaustive `when` over sealed `HandleResult` — NO `else if`.
- **INV6 KDoc**: every NEW `.kt` (and every edited `object`) gets purposeful KDoc per CODING.md.
- **INV7 stale dir + scaffold**: `features/deeplinks/` currently holds ONLY stale `build/` artifacts (no `.kt`, not git-tracked, NOT in settings.gradle). CODING MUST `rm -rf features/deeplinks` (verify `find features/deeplinks -name '*.kt'` empty first) BEFORE running `./generate_feature.sh` and entering name `deeplinks`. `rm` is allowed (NOT git). After scaffolding, apply the EDITs/NEW files above onto the generated tree.
- **INV8 Constants object name**: replace the bare-package generated `Constants.kt` with `object DeepLinksConstants` (single file; matches `EchoConstants` precedent).
- **R3 handler uniqueness**: `getAllDistinct<DeepLinkHandler>()` is unordered, first-true-wins → `type` keys must be globally unique per handler. Documented; not enforced in code (no handlers ship).
- **R6 empty handler list is correct**: no handlers ship → `handle` returns `Unhandled` (→404) for any stored link until a feature registers one. Validation must NOT treat this as failure.
- **Build expectation**: after edits, `./gradlew :wishlist.features.deeplinks.server:build` and the server build should succeed; CODING runs the required compile, ONE fix cycle, then rebuilds ast-index.

---

## STEP FILE
`/home/aleksey/projects/own/WishlistApp/agents/task/3ae853f7-b464-4d51-82c8-fcdf97d6a92f/1-Architecture.md`
