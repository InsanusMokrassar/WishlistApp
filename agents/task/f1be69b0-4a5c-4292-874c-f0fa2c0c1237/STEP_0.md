# STEP_0 Planning — Issue #43 (UI subpath)

ENTITY:
entity_id=issue_43; type=feature_request; state=planned
CONTEXT: task_id=f1be69b0-4a5c-4292-874c-f0fa2c0c1237; role=root; prev_step=none

## Requirements (issue #43)
1. req1=default_web_client_subpath; value=`ui`; web client served under `/ui`.
2. req2=spa_fallback; rule: any `/ui/**` path → serve web client index (client-side routing/deeplinks work).
3. req3=root_redirect; relation: `/` → redirect → `/ui`.
4. req4=log_warning; condition: static content NOT configured for `ui` subpath → emit WARN log.

## Target location
- file=features/common/server/src/jvmMain/kotlin/JVMPlugin.kt; symbol=`ApplicationRoutingConfigurator.Element` block (lines 158-166); current behavior: iterates `config.staticFolders` map (path→folder) and calls `staticFiles(path, file)`.
- file=features/common/server/src/jvmMain/kotlin/models/Config.kt; symbol=`Config.staticFolders`; current default: `staticFolder?.let{mapOf("/" to it)} ?: emptyMap()`.
- file=server/sample.config.json; current `staticFolders` = `{ "": "/static" }` (root prefix).

## Investigated facts
- fact1=ktor_version; value=3.5.0 (gradle/libs.versions.toml).
- fact2=staticFiles_spa_api; Ktor 3.5 `staticFiles(path, dir){ default("index.html") }` serves index.html for any unmatched sub-path inside the static route → satisfies req2 SPA fallback for `/ui/**`.
- fact3=index_html_assets; file=client/src/jsMain/resources/index.html uses RELATIVE asset URLs (`css/bootstrap.min.css`, `js/...`, `wishlist.client.js`). RISK: when SPA fallback serves index.html for `/ui/users`, browser resolves relatives against `/ui/users/` → assets 404. MITIGATION required: client-side routing in this app uses navigation stack (in-memory), NOT URL-path-based; there is no router reading `window.location.pathname` into a route. So deeplinks `/ui/users` are NOT currently produced by the client. req2 = serve web client for any `/ui/**` so a manually-typed/deeplinked sub-path still loads the app. Add `<base href="/ui/">` to index.html so relative assets resolve correctly regardless of sub-path depth. ASSUMPTION A1.
- fact4=config_static_prefix_form; staticFiles path prefix `""` and `/` both mount at root in Ktor. To serve under `/ui`, prefix must be `ui` (or `/ui`).
- fact5=no_existing_root_route; no `get("/")` route exists; root currently served by static mount at `/`.

## Plan of changes
1. change1=Config.staticFolders default; new default maps single `staticFolder` to `ui` prefix instead of `/`. Keeps explicit `staticFolders` map authoritative when provided.
2. change2=routing block; build SPA-capable static mounts: for each (path, folder) call `staticFiles(path, file){ default("index.html") }`. Add `get("/"){ call.respondRedirect("/ui") }` (req3). Detect whether any configured static prefix normalizes to `ui`; if absent → `logger.w(...)` warning (req4).
3. change3=sample.config.json; change `staticFolders` `""`→`ui` so default deployment serves under `/ui`.
4. change4=index.html; add `<base href="/ui/">` so relative assets resolve under sub-paths (A1).

## Assumptions
- A1: app navigation is in-memory (navigation.mvvm stack), not URL-path-driven; SPA fallback = "serve the app shell for any /ui/** URL". `<base href="/ui/">` fixes relative asset loading. Confirmed: no router reads location.pathname (architecture doc: navigation via NavigationChain push/pop, ViewConfig serialized in-memory).
- A2: `ui` is a literal constant; introduce `const val defaultWebClientSubPath = "ui"` for single-source-of-truth.

## Open questions
- none blocking. Proceeding to architecture.

VERIFICATION: check=server module compiles after change; expected=BUILD SUCCESSFUL.
REPETITION OF RESULT: entity_id=issue_43; stored_in=agents/task/f1be69b0-4a5c-4292-874c-f0fa2c0c1237/STEP_0.md; status=available
