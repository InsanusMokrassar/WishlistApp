# STEP_2 Coding — Issue #43 (UI subpath)

ENTITY:
entity_id=issue_43; type=feature_request; state=implemented_built
CONTEXT: task_id=f1be69b0-4a5c-4292-874c-f0fa2c0c1237; prev_step=STEP_1(architected, read)

## Files changed

MODIFIED:
- features/common/server/src/jvmMain/kotlin/models/Config.kt
  - ADDED `const val defaultWebClientSubPath = "ui"` (single source of truth; KDoc'd).
  - CHANGED `staticFolders` default: `staticFolder?.let{mapOf("/" to it)}` → `mapOf(defaultWebClientSubPath to it)`. req1.
  - ADDED class+member KDocs (file previously had none).
- features/common/server/src/jvmMain/kotlin/JVMPlugin.kt
  - imports ADDED: `dev.inmo.kslog.common.w`, `io.ktor.server.response.respondRedirect`, `io.ktor.server.routing.get`, `models.defaultWebClientSubPath`.
  - routing configurator block REWRITTEN:
    - `webClientSubPathConfigured = config.staticFolders.keys.any { it.trim('/') == defaultWebClientSubPath }`.
    - `if (!webClientSubPathConfigured) this@JVMPlugin.logger.w("Static content is not configured for the '/ui' sub-path ...")`. req4.
    - `get("/") { call.respondRedirect("/$defaultWebClientSubPath") }`. req3 (302 Found).
    - `staticFiles(path, file) { default("index.html") }` for each entry → SPA fallback for any `/ui/**`. req2.
- server/sample.config.json
  - `staticFolders` key `""` → `"ui"`. req1 (deployed default).
- client/src/jsMain/resources/index.html
  - ADDED `<base href="/ui/">` so relative asset URLs (`css/...`, `js/...`, `wishlist.client.js`) resolve under `/ui` and any `/ui/**` fallback. Supports req2.
- features/common/README.md
  - Architecture Notes: ADDED bullet documenting `/ui` sub-path, redirect, SPA fallback, warning, `<base>`. Operator Notes UNTOUCHED.

## Build fix cycle (1 cycle, per CODING.md)
- ISSUE: `logger.w(...)` inside Koin `single { }` lambda → receiver-type-mismatch (receiver = Koin Scope; `Any.logger` resolution failed for `w`).
- FIX: qualified receiver `this@JVMPlugin.logger.w(...)` (matches `logger.d/i/e` usage in `startPlugin`). No re-attempt of same fix.

## Build
- rebuild_ast=ast-index rebuild (twice: after edits, final).
- command1=`./gradlew :wishlist.features.common.server:compileKotlinJvm` → BUILD SUCCESSFUL.
- command2=`./gradlew :wishlist.features.common.server:build` → BUILD SUCCESSFUL (allTests NO-SOURCE).

## Requirements coverage
- req1 default subpath ui = DONE (Config default + sample.config.json).
- req2 /ui/** SPA fallback = DONE (`default("index.html")` + `<base href="/ui/">`).
- req3 / → /ui redirect = DONE (`get("/") respondRedirect`).
- req4 warn when ui static missing = DONE (`logger.w`).

## Assumptions held
- A1: client navigation in-memory (NavigationChain), NOT URL-path-driven (verified: 0 `window.location`/`location.pathname` refs). SPA fallback = serve app shell for any `/ui/**`.
- A2: redirect = 302 (respondRedirect default); issue specified no permanence.

## NOT done (out of scope / no git per orchestrator constraint)
- no git commit/branch/push; no PR. Orchestrator owns git.

VERIFICATION: check=server module build; expected=BUILD SUCCESSFUL; actual=BUILD SUCCESSFUL.
REPETITION OF RESULT: entity_id=issue_43; state=implemented_built; stored_in=agents/task/f1be69b0-4a5c-4292-874c-f0fa2c0c1237/STEP_2.md; status=available
