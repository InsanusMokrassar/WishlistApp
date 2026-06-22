Model: claude-opus-4-8 (Opus 4.8 — chosen per PLAN.md priority `fable`/`opus`/`sonnet`; `fable` unavailable to this agent, so highest available = opus, best for ambiguity-resolving design work)
Execution time: 540
Tokens used: ~84000
Changed files: agents/task/3ae853f7-b464-4d51-82c8-fcdf97d6a92f/0-Planning.md

# PLAN — issue #45 deeplinks feature (server-only)

Dense AML-HIP. Server-only feature. Match `features/booking` + `features/files` conventions EXACTLY.

---

## 0. Decisions (resolved, NOT open)

### D1. `links/<uuid>` route MUST be at site ROOT, NOT under `/api`
Evidence:
- `features/common/server/.../InternalApplicationRoutingConfigurator.kt`: collects ALL `ApplicationRoutingConfigurator.Element` (registered via `singleWithRandomQualifier<ApplicationRoutingConfigurator.Element>`), wraps them in `route(apiPathPart)` where `apiPathPart="api"` (`features/common/common/.../Constants.kt`), AND adds `route("{...}"){ handle{ respond(NotFound) } }` catch-all under `/api`.
- So ANY `Element` route is forced to `/api/...`. Issue says deeplink = `links/<deeplink_uuid>` (a URL users click → must be root).
- Root-level routes use the OTHER mechanism: `KtorApplicationConfigurator` whose `Application.configure()` opens `routing{}` directly at root. Proof: in `common/server/.../JVMPlugin.kt` the static-folders serving is itself a `singleWithRandomQualifier<KtorApplicationConfigurator>{ ApplicationRoutingConfigurator(listOf(Element{ staticFiles(path,file){ default("index.html") } })) }` mounted at root `""`.
DECISION: DeepLink call route registered as a **root-level `KtorApplicationConfigurator`** (NOT an `Element`) → serves at `GET /links/{deeplink_uuid}`. Rationale: issue verbatim `links/<deeplink_uui>`; clickable user-facing link; `/api` is the internal API namespace + has a 404 catch-all that would swallow non-matching subpaths.
Route-ordering risk + resolution: static fallback mount `""` uses `default("index.html")` → could serve SPA for unknown paths. Ktor resolves by route SPECIFICITY, not install order; explicit `route("links"){ get("{deeplink_uuid}") }` is strictly more specific than the static catch-all, so the deeplink route wins regardless of `getAllDistinct` (unordered) registration order. Implement deeplink configurator exactly like the static one: `KtorApplicationConfigurator` → `routing { route(Constants.linksPrefixPathPart) { get("{deeplinkId}"){...} } }`.

### D2. `DeepLinkHandler.tryHandle(deeplinkId, handlerInfo: Any)` — the `Any` typing
- Handler info = serializable data class, stored as JSON (issue). Stored JSON loses concrete type.
- Storage value type: `String` (raw JSON text) in DB, same blob pattern as `ExposedFilesMetaInfoRepo`. On call route, decode the stored String → `kotlinx.serialization.json.JsonElement` via injected `Json`, and pass THAT `JsonElement` as the `handlerInfo: Any` argument.
- Each `DeepLinkHandler` impl knows its own concrete type and does: `val info = runCatching { json.decodeFromJsonElement(MyInfo.serializer(), handlerInfo as JsonElement) }.getOrNull() ?: return false`; if decode succeeds and the shape is its own → process, return `true`; else return `false`. `tryHandle` returns `true` ONLY when that handler owned + processed it.
- WHY `JsonElement` (not raw `String`, not a sealed type): keeps `handlerInfo: Any` faithful to issue signature; `decodeFromJsonElement` lets each handler attempt structural decode without coupling handlers to one polymorphic registry. `Json` already has `ignoreUnknownKeys=true` (`features/common/common/.../Plugin.kt`) — so decode of a foreign shape may SUCCEED with garbage. Mitigation: handlers must validate required fields after decode, OR store a discriminator. SIMPLEST robust approach (CHOSEN, see D2a).

### D2a. Discriminator to disambiguate handlers (avoids `ignoreUnknownKeys` false-positive)
Stored handler-info record = `@Serializable data class DeepLinkHandlerInfo(val type: String, val payload: JsonElement)`. `type` = caller-supplied logical handler key; `payload` = the serializable data class encoded to `JsonElement`. The call route passes the WHOLE `DeepLinkHandlerInfo` (or just `payload` + `type`) — DECISION: pass `handlerInfo: Any = the decoded DeepLinkHandlerInfo` object. Each handler checks `(handlerInfo as? DeepLinkHandlerInfo)?.type == MY_TYPE` then decodes `.payload`. This makes handler dispatch deterministic and immune to `ignoreUnknownKeys`. (Architecture role MAY simplify to passing raw `JsonElement` if it prefers handlers to self-validate; both satisfy the issue signature since param is `Any`. Recommend the discriminator form.)

### D3. Multi-handler collection + iteration
- Collect via Koin `getAllDistinct<DeepLinkHandler>()` (pattern proven: `features/common/server/.../JVMPlugin.kt` uses `getAllDistinct()` for configurators; `features/ui/wishlist` for VM extras).
- Each handler registered by ITS feature via `singleWithRandomQualifier<DeepLinkHandler>{ ... }` (random qualifier so multiple distinct singletons of same type coexist — same idiom as `singleWithRandomQualifier<KtorApplicationConfigurator>`).
- Dispatch: iterate handlers, call `tryHandle(id, info)`, stop at first returning `true`. If none → unhandled.
- This feature ships ZERO concrete handlers (server-only infra). Other features add their own later. `getAllDistinct<DeepLinkHandler>()` returns empty list safely when none registered.

### D4. Create-deeplink = server-only IN-PROCESS API (NO public HTTP create route)
- Issue: "server only feature for creating deeplinks". Convention match: `FilesService`/`WishlistItemService` expose server-only methods consumed by other server code, not necessarily a public route. `createDeepLink` is infra other server features call to mint a link.
- DECISION: `DeepLinksService.createDeepLink(handlerInfo): DeepLinkId` — server-only Kotlin API (Koin `single`). Generates `DeepLinkId(uuid4().toString())`, persists `DeepLinkId -> DeepLinkHandlerInfo(json)`. NO HTTP POST create endpoint (avoids unauthenticated link minting; if a future feature needs HTTP creation it gates behind admin/auth itself). State this clearly in README + as a note for operator (see Blockers Q1 — low-risk, proceed).

### D5. Module layout (server-only → client = scaffold only)
deeplinks/common:
- `commonMain/models/DeepLinkId.kt` — `@Serializable @JvmInline value class DeepLinkId(val string: String)` (mirror `FileId.kt`).
- `commonMain/models/DeepLinkHandlerInfo.kt` — `@Serializable data class DeepLinkHandlerInfo(val type: String, val payload: JsonElement)`.
- `commonMain/DeepLinkHandler.kt` — `interface DeepLinkHandler { suspend fun tryHandle(deeplinkId: DeepLinkId, handlerInfo: Any): Boolean }` (EXACT signature from issue; lives in common so handler-providing features depend only on common).
- `commonMain/Constants.kt` — `object Constants { const val linksPrefixPathPart = "links" }`.
- `commonMain/repo/DeepLinksRepo.kt` — `interface DeepLinksRepo : KeyValueRepo<DeepLinkId, DeepLinkHandlerInfo>` (mirror `FilesMetaInfoRepo`).
- `jvmMain/repo/ExposedDeepLinksRepo.kt` — `ExposedKeyValueRepo<String,String>(keyColumnAllocator={text("deeplink_id")}, valueColumnAllocator={text("handler_info_json")}, tableName="deeplinks").withMapper<...>` encode/decode `DeepLinkHandlerInfo` with injected `Json` (mirror `ExposedFilesMetaInfoRepo.kt` byte-for-byte structurally).
- `commonMain/Plugin.kt`, `jvmMain/JVMPlugin.kt`, `jsMain/JSPlugin.kt`, `androidMain/AndroidPlugin.kt` — generated scaffold; jvm/common Plugins extended (below).
deeplinks/server (commonMain + jvmMain):
- `commonMain/services/DeepLinksService.kt` — `createDeepLink(handlerInfo): DeepLinkId` (mint+persist) + `handle(id): HandleResult` (load info, iterate handlers).
- `commonMain/configurators/DeepLinksRoutingConfigurator.kt` — implements `KtorApplicationConfigurator` (root-level, per D1), registers `GET /links/{deeplinkId}`.
- `commonMain/Plugin.kt` — registers `DeepLinksService` (`single`) + the root configurator (`singleWithRandomQualifier<KtorApplicationConfigurator>`).
- `jvmMain/JVMPlugin.kt` — delegates to `deeplinks.common.JVMPlugin` + server `Plugin`; binds `DeepLinksRepo` to `ExposedDeepLinksRepo`.
deeplinks/client (scaffold ONLY): keep generated `commonMain/Plugin.kt`, `jvmMain/JVMPlugin.kt`, `jsMain/JSPlugin.kt`, `androidMain/AndroidPlugin.kt` UNCHANGED (empty `setupDI`). No models, no feature, no HTTP client. (Server-only.)

### D6. DeepLinksRepo binding location
Put `single<DeepLinksRepo>{ ExposedDeepLinksRepo(get(), get()) }` in **deeplinks/common/jvmMain/JVMPlugin** (mirrors `files/common/jvmMain` placement via `files/server/jvmMain` binding `FilesMetaInfoRepo`). NOTE: files binds repo in SERVER/jvmMain; booking binds in COMMON/jvmMain. Both valid. DECISION: bind in **common/jvmMain/JVMPlugin** (booking pattern — repo is common-layer, only Exposed impl is jvm). Server JVMPlugin delegates to common JVMPlugin first.

---

## 1. Ordered execution steps (for Architecture/Coding roles)

### S0. Clean stale dir (CRITICAL pre-step)
`features/deeplinks/` currently holds ONLY stale `build/` artifacts (no source, not git-tracked, NOT in settings.gradle). DELETE it fully before scaffolding:
`rm -rf features/deeplinks` (verify `find features/deeplinks -name '*.kt'` returns nothing first — confirmed none exist now).
Then run scaffold. (Coding role: this is `rm`, allowed; NOT git.)

### S1. Scaffold
Run `./generate_feature.sh`, enter module name `deeplinks` at prompt. Creates `features/deeplinks/{common,server,client}` with stub `build.gradle`, `Plugin.kt`, `JVMPlugin.kt`, `JSPlugin.kt`, `AndroidPlugin.kt`, `Constants.kt`. Template substitutes `module_package=features.deeplinks` → package `dev.inmo.wishlist.features.deeplinks.*`. Verify generated tree matches `features/sample` layout.

### S2. common module sources (see D5 list)
Create/extend:
- `common/.../models/DeepLinkId.kt`, `models/DeepLinkHandlerInfo.kt`, `DeepLinkHandler.kt`, `Constants.kt` (linksPrefixPathPart), `repo/DeepLinksRepo.kt`.
- `common/jvmMain/repo/ExposedDeepLinksRepo.kt`.
- `common/build.gradle`: generated stub already `api project(":wishlist.features.common.common")` — ADD nothing else needed for common (KeyValueRepo + Json from micro_utils/serialization already on classpath via `mppJvmJsAndroid` + common.common). Verify `dev.inmo.micro_utils.repos.*` resolves (it does — used by files/booking common modules with same template). If exposed repo needs extra dep, mirror `features/files/common/build.gradle` deps.
- `common/jvmMain/JVMPlugin.kt`: add `single<DeepLinksRepo>{ ExposedDeepLinksRepo(get(), get()) }` (args: Database, Json) AFTER `with(Plugin){setupDI(config)}`.

### S3. server module sources (see D5)
- `server/.../services/DeepLinksService.kt`:
  - ctor `(private val repo: DeepLinksRepo, private val json: Json, private val handlersProvider: () -> List<DeepLinkHandler>)` OR inject `getAllDistinct` result. Match booking: pass `get()`s. Simplest: ctor takes `repo`, `json`, and `handlers: List<DeepLinkHandler>` resolved via `getAllDistinct()` in Plugin.
  - `suspend fun createDeepLink(type: String, payload: JsonElement): DeepLinkId` (or `(handlerInfo: DeepLinkHandlerInfo)`): `val id = DeepLinkId(uuid4().toString()); repo.set(id, DeepLinkHandlerInfo(type, payload)); return id`. (uuid4 = `com.benasher44.uuid.uuid4`, as `FilesService`.)
  - `suspend fun handle(id: DeepLinkId): HandleResult`: `val info = repo.get(id) ?: return NotFound; handlers.forEach { if (it.tryHandle(id, info)) return Handled }; return Unhandled`. `HandleResult` = sealed (NotFound/Handled/Unhandled) in service file or models.
- `server/.../configurators/DeepLinksRoutingConfigurator.kt`: `class DeepLinksRoutingConfigurator(private val service: DeepLinksService) : KtorApplicationConfigurator { override fun Application.configure(){ routing { route(Constants.linksPrefixPathPart){ get("{deeplinkId}"){ ... } } } } }`. Response semantics (D-route):
  - missing/blank param → `400 Bad Request`.
  - `NotFound` → `404 Not Found`.
  - `Unhandled` (link exists, no handler claimed it) → `404 Not Found` (or `204`? choose 404 — link effectively dead to caller; document). DECISION: `404`.
  - `Handled` → `200 OK` (handler performed side-effect; deeplink processing returns 200; a handler that needs a redirect can be added later via richer HandleResult — out of scope now).
- `server/.../Plugin.kt`:
  ```
  single { DeepLinksService(get(), get(), getAllDistinct()) }
  singleWithRandomQualifier<KtorApplicationConfigurator> { DeepLinksRoutingConfigurator(get()) }
  ```
  (NOTE: root-level configurator = `KtorApplicationConfigurator`, NOT `ApplicationRoutingConfigurator.Element` — this is the load-bearing difference vs booking/files. Import `dev.inmo.micro_utils.ktor.server.configurators.KtorApplicationConfigurator`, `io.ktor.server.routing.routing`.)
- `server/.../jvmMain/JVMPlugin.kt`: generated form already delegates to `deeplinks.common.JVMPlugin.setupDI/startPlugin` + server `Plugin`. Keep that. Common JVMPlugin (S2) provides the repo. No extra jvm bindings needed in server JVMPlugin (unlike files, no Disk/config repo). Verify `server/build.gradle` generated stub has `api project(":wishlist.features.deeplinks.common")` + `api project(":wishlist.features.common.server")` (template default) — that gives `KtorApplicationConfigurator`, `getAllDistinct`, `singleWithRandomQualifier`, `Json`, `Database`. Good.

### S4. README.md (haiku-written per ALL.md §"All fillings of documentation by haiku")
Create `features/deeplinks/README.md` per ALL.md required structure: `# Feature: DeepLinks` / `## Operator Notes` (empty placeholder comment) / `## Overview` / `## Routes` / `## Models` / `## Architecture Notes`. ROUTES section MUST state: `GET /links/{deeplink_uuid}` is served at SITE ROOT (NOT under `/api`) — unlike all other features — because it's a user-clickable link; registered as a root-level `KtorApplicationConfigurator`, resolved ahead of the static SPA fallback by route specificity. Document: create is server-only in-process (`DeepLinksService.createDeepLink`), no public POST. Document `DeepLinkHandler` contract + `DeepLinkHandlerInfo(type,payload:JsonElement)` discriminator. (Architecture role updates `## Architecture Notes` after coding.)

### S5. Wiring (EXACT)
1. `settings.gradle`: add after booking block (before `:features:ui:*`):
   ```
   ":features:deeplinks:common",
   ":features:deeplinks:server",
   ":features:deeplinks:client",
   ```
2. `server/build.gradle`: add `api project(":wishlist.features.deeplinks.server")` after the booking line (~L23).
3. `server/sample.config.json`: add `"dev.inmo.wishlist.features.deeplinks.server.JVMPlugin"` to `plugins` array, LAST (after booking). Order: after common/users/auth/wishlist (deeplinks repo only needs Database+Json from common.server → can sit anywhere after common.server.JVMPlugin; put last for safety).
4. `client/build.gradle` (commonMain deps): add `api project(":wishlist.features.deeplinks.client")` after booking.client (~L28). (Server-only feature STILL wires its scaffold client module — confirmed: EVERY feature incl. files/booking registers client in client/build.gradle + all 3 Main.kt. The client module is an empty scaffold but must compile + be on the client graph.)
5. `client/android/build.gradle`: android gets client transitively via `:client` (per ARCHITECTURE.md UI rule + booking has no explicit android line — VERIFY: booking.client absent from android/build.gradle grep → android inherits through client). DECISION: add to `client/android/build.gradle` ONLY if booking.client is present there; grep showed it is NOT → do NOT add (matches convention).
6. Client entrypoints — add scaffold plugin to ALL 3, after booking:
   - `client/src/jsMain/kotlin/Main.kt`: `dev.inmo.wishlist.features.deeplinks.client.JSPlugin,`
   - `client/src/jvmMain/kotlin/Main.kt`: `dev.inmo.wishlist.features.deeplinks.client.JVMPlugin,`
   - `client/android/src/main/kotlin/MainActivity.kt`: `dev.inmo.wishlist.features.deeplinks.client.AndroidPlugin,`
   Place in the `features.*.client` group (before the `features.ui.*` group), mirroring booking. (These are empty scaffolds — required so the build/plugin list stays consistent with every other feature.)

### S6. Rebuild ast-index (ALL.md: rebuild on source changes) — Coding role after edits.

---

## 2. File manifest (new/changed)
NEW (deeplinks/common): models/DeepLinkId.kt, models/DeepLinkHandlerInfo.kt, DeepLinkHandler.kt, Constants.kt(edit scaffold), repo/DeepLinksRepo.kt, jvmMain/repo/ExposedDeepLinksRepo.kt, jvmMain/JVMPlugin.kt(edit).
NEW (deeplinks/server): services/DeepLinksService.kt, configurators/DeepLinksRoutingConfigurator.kt, commonMain/Plugin.kt(edit), jvmMain/JVMPlugin.kt(scaffold ok).
SCAFFOLD UNCHANGED (deeplinks/client): all 4 plugin files.
NEW: features/deeplinks/README.md.
EDIT (wiring): settings.gradle, server/build.gradle, server/sample.config.json, client/build.gradle, client/src/jsMain/kotlin/Main.kt, client/src/jvmMain/kotlin/Main.kt, client/android/src/main/kotlin/MainActivity.kt.

---

## 3. Risks / problems (point-8 pre-list)
- R1 `Any` serialization: `handlerInfo: Any` can't be auto-serialized by kotlinx. Mitigated: NEVER serialize the `Any`; store `DeepLinkHandlerInfo(type,payload:JsonElement)` (fully serializable), pass decoded object as `Any` at dispatch only. ✔ resolved (D2/D2a).
- R2 `ignoreUnknownKeys=true` (global Json) → blind `decodeFromJsonElement` of foreign payload may falsely succeed. Mitigated by `type` discriminator (D2a). Architecture role: ensure handlers check `type` BEFORE decoding payload.
- R3 Handler ordering: `getAllDistinct` is UNORDERED. First-true-wins means non-deterministic if two handlers claim same `type`. Document: `type` keys must be unique per handler (operator/feature responsibility). Acceptable.
- R4 Route prefix: RESOLVED D1 — root, not `/api`. Risk that an `Element` would be wrong. Coding MUST use `KtorApplicationConfigurator` + `routing{}`, NOT `ApplicationRoutingConfigurator.Element`.
- R5 Static SPA fallback collision at root: RESOLVED — route specificity beats `default("index.html")`. Validation should curl `/links/<random>` expecting 404 (not index.html 200).
- R6 No concrete handlers ship → call route always 404/Unhandled until a feature registers one. Expected for infra feature; Validation must NOT treat empty handler list as failure.
- R7 server-only create with no HTTP route → Validation can't exercise create via curl. Validation strategy: build success + (optional) a throwaway test or reflective check that `DeepLinksService` is in Koin. Note for operator (Q1).
- R8 Two prior PRs silently CLOSED. Unknown rejection reason. Mitigation: strictly match conventions; the most likely prior mistakes = put route under `/api` (Element) instead of root, OR added client feature code instead of scaffold-only, OR HTTP create route. This plan avoids all three.
- R9 `DeepLinkHandler` placed in COMMON (not server): correct so handler-providing features depend only on `deeplinks.common`. But interface uses `Any` (no platform types) → safe in commonMain. ✔.

---

## Blockers / Questions for operator
(Proceeding with stated decisions; flag for awareness, not blocking.)
- Q1 (D4): "server only feature for creating deeplinks" interpreted as in-process `DeepLinksService.createDeepLink` (no public HTTP create). If operator intended an admin-gated HTTP POST create endpoint, say so; trivial to add a `POST /api/deeplinks` behind `authenticate{}`+root-gate later. Default: no HTTP create.
- Q2 (S3 response): `Unhandled` (link exists, no handler claimed) → chosen `404`. If operator prefers `200`/`204`/redirect semantics for a "claimed but no-op" link, specify; trivial to change. Default: 404.
- Q3 (D2a): chose discriminator `DeepLinkHandlerInfo(type,payload)` passed as `Any` over raw `JsonElement`. Both honor the exact `tryHandle(...,Any)` signature. Default: discriminator form (more robust).
