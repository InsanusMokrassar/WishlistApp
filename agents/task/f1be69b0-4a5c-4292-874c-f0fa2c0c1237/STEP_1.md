# STEP_1 Architecture — Issue #43 (UI subpath)

ENTITY:
entity_id=issue_43; type=feature_request; state=architected
CONTEXT: task_id=f1be69b0-4a5c-4292-874c-f0fa2c0c1237; prev_step=STEP_0(planned, read)

## Confirmed investigation
- confirm1=client_nav_in_memory; ast-index search `window.location`/`location.pathname` = 0 results; client renders into `<div id="content">` via renderComposable; navigation = NavigationChain push/pop (in-memory ViewConfig). RESULT: deeplink `/ui/users` does NOT map to a client route; SPA fallback requirement (req2) = serve app shell (index.html) for any `/ui/**` so the SPA boots regardless of entered URL.
- confirm2=relative_assets; index.html asset URLs relative → require `<base href="/ui/">` so `/ui/<anything>` resolves `wishlist.client.js` etc. against `/ui/`.

## Design decisions
- D1=subpath_constant; introduce `const val defaultWebClientSubPath = "ui"` in features/common/server/src/jvmMain/kotlin/models/Config.kt companion (single source of truth; consumed by Config default + routing warning check). Rationale: avoid literal "ui" drift between default mapping and warning detection.
- D2=Config_default; `staticFolders` default changes from mapping `staticFolder` to `"/"` → mapping to `defaultWebClientSubPath`. Explicit `staticFolders` map in JSON still overrides. KDoc added (file currently has none → KDoc REQUIRED by CODING.md for new const; existing data class gets KDoc on new member only, but CODING rule = update KDocs when touching; add class+members KDoc minimally for changed/added symbols).
- D3=routing; replace static block:
  - normalize each configured prefix: trim leading/trailing `/`; compare to `defaultWebClientSubPath`.
  - `staticFiles(path, file){ default("index.html") }` → `default("index.html")` = SPA fallback (req2): any unmatched sub-path under the mount serves index.html.
  - add `get("/"){ call.respondRedirect("/$defaultWebClientSubPath") }` (req3). respondRedirect import = io.ktor.server.response.respondRedirect.
  - after building mounts: `if (no configured prefix normalizes to ui) logger.w("Static content is not configured for '/ui' subpath ...")` (req4). logger = KSLog `logger` already used in this object (`logger.d/i/e`); add `dev.inmo.kslog.common.w` import.
- D4=index_html; add `<base href="/ui/">` in <head> so relative assets load under `/ui` and any `/ui/**` fallback. Justification: without it, fallback-served index.html at `/ui/users` would request `/ui/wishlist.client.js`... actually browser resolves against `/ui/users/` → 404; `<base>` pins resolution to `/ui/`.
- D5=sample.config.json; `staticFolders` `""` → `ui`. Aligns deployed default with new subpath.

## NEVER-else-if compliance
- warning detection + redirect use single `if`/`when`; no `else if` chains. Static mount detection via `config.staticFolders.keys.none { it.trim('/') == defaultWebClientSubPath }`.

## Files to change (coding step)
1. features/common/server/src/jvmMain/kotlin/models/Config.kt — add const, change default, KDocs.
2. features/common/server/src/jvmMain/kotlin/JVMPlugin.kt — routing block: default() SPA fallback, root redirect, missing-static warning, imports (respondRedirect, kslog.w).
3. server/sample.config.json — staticFolders ui.
4. client/src/jsMain/resources/index.html — <base href="/ui/">.
5. features/common/README.md — Architecture Notes update (web client served under /ui, SPA fallback, root redirect). Operator Notes untouched.

## Risks / constraints
- R1: build is JVM server only — compile features/common/server. index.html + sample.config.json are non-Kotlin (no compile impact).
- R2: respondRedirect default is 302 Found — acceptable for `/`→`/ui` (req3 says "redirect", no permanence specified). Use default (302).

VERIFICATION: check=`./gradlew :wishlist.features.common.server:build` (or :compileKotlinJvm); expected=BUILD SUCCESSFUL.
REPETITION OF RESULT: entity_id=issue_43; stored_in=agents/task/f1be69b0-4a5c-4292-874c-f0fa2c0c1237/STEP_1.md; status=available
