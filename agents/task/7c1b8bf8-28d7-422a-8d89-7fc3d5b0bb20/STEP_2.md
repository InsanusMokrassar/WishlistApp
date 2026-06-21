# STEP_2 — Coding

task_id=7c1b8bf8-28d7-422a-8d89-7fc3d5b0bb20; issue=#55; status=done; build=SUCCESS

## Changes applied
1. features/common/common/.../Constants.kt — added `const val apiPathPart = "api"` (shared, KDoc).
2. features/common/server/.../JVMPlugin.kt:
   - `single<ApplicationRoutingConfigurator>` now builds two Elements: `apiRoot` wraps all feature `getAllDistinct<...Element>()` in `route(apiPathPart)`; `staticRoot` serves `config.staticFolders` at their keys (root `""`) with SPA `default("index.html")`.
   - removed standalone static `Element` registration + `get("/")`→`respondRedirect("/ui")`.
   - imports: +`routing.route`, +`common.common.apiPathPart`; -`respondRedirect`, -`routing.get`, -`defaultWebClientSubPath`.
   - root-static-missing warning updated to check empty (`""`) key.
3. features/common/server/.../models/Config.kt — `defaultWebClientSubPath = ""` (root mount), KDoc updated.
4. server/dev.config.json, server/local.config.json, server/sample.config.json — `staticFolders` key `"ui"`→`""` (local.* gitignored, updated on disk).
5. features/common/client/.../configurators/ApiPrefixHttpClientConfigurator.kt (NEW) — `createClientPlugin` onRequest prepends `apiPathPart`; order-independent vs DefaultUrl plugin (keys off `url.host.isEmpty()`), idempotent (skips if first seg already `api`), handles relative + leading-slash (`/temp_upload`) inputs.
6. features/common/client/.../Plugin.kt — registered `ApiPrefixHttpClientConfigurator` as `singleWithRandomQualifier<HttpClientConfigurator>`.
7. features/files/client/.../FilesClientService.kt — `fileUrl(id)`→`/$apiPathPart/files/{id}` (browser img, absolute); `downloadBytes` uses bare `files/{id}` (client plugin adds prefix) to avoid `/api/api/...`.
8. features/files/README.md — Routes note + `fileUrl` model note + Architecture `/api` prefix split bullet.

## Verification
- `./gradlew :wishlist.features.common.common:build :wishlist.features.common.server:build :wishlist.features.common.client:build :wishlist.features.files.client:build` → BUILD SUCCESSFUL (44s), all targets (JS/JVM/Android commonMain + jvmMain).
- git status: only intended files modified; NO code reverted by any agent (no subagents spawned).

## Result
- UI served from `/` (static at root, SPA fallback). API under `/api/...`.
- Browser `<img>` file URLs → `/api/files/{id}`. All HttpClient API calls → `/api/...` via central plugin.

## Not done / notes
- Full server run / live HTTP smoke test not executed (no DB/Postgres started). Compile-level verification only.
- common/* features have no README.md (pre-existing repo gap); not created (would need human Operator Notes).
- ast-index rebuild recommended after source changes.
