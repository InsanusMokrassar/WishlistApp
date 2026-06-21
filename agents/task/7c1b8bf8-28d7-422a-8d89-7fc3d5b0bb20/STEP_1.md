# STEP_1 — Architecture

task_id=7c1b8bf8-28d7-422a-8d89-7fc3d5b0bb20; issue=#55

## Design: single shared `api` prefix, applied centrally on both tiers

### Shared
- entity: `apiPathPart`; location=`features/common/common/src/commonMain/kotlin/Constants.kt`; value=`"api"`; visibility=public top-level const; pkg=`dev.inmo.wishlist.features.common.common`.
- rationale: one source of truth; commonMain visible to server(jvmMain) + client(commonMain). Mirrors per-feature `Constants.prefixPathPart` convention.

### Server (`features/common/server/src/jvmMain/kotlin/JVMPlugin.kt`)
- change_1: replace `single { ApplicationRoutingConfigurator(getAllDistinct()) }` with a builder that:
  - collects feature elements `getAllDistinct<ApplicationRoutingConfigurator.Element>()` (NO static element among them — see change_3);
  - builds `apiRoot = Element { route(apiPathPart) { feature elements.forEach { invoke() } } }`;
  - builds `staticRoot = Element { ...static at root, SPA fallback... }`;
  - `ApplicationRoutingConfigurator(listOf(apiRoot, staticRoot))` — apiRoot first (explicit `/api` segment outranks static wildcard fallback regardless, order kept for clarity).
- change_2: drop `get("/") respondRedirect("/ui")` (UI now at root; static index served directly).
- change_3: REMOVE the standalone `singleWithRandomQualifier<ApplicationRoutingConfigurator.Element>` static registration so static is not double-collected; fold its `staticFolders` iteration into `staticRoot` built inside change_1. Warning log: warn when no root (`""`) static folder configured.
- invariant: every feature Element (auth/users/currency/wishlist/files/temp_upload/admin/booking/sample/echo) is now under `/api`; static stays root.

### Server config model (`features/common/server/src/jvmMain/kotlin/models/Config.kt`)
- change: `defaultWebClientSubPath = ""` (root). Keeps `staticFolder` shorthand mounting at root. KDoc updated.

### Server config files (dev/local/sample.config.json)
- change: `staticFolders` key `"ui"` → `""`.

### Client
- change_client_1: new `ApiPrefixHttpClientConfigurator : HttpClientConfigurator` in `features/common/client/src/commonMain/kotlin/configurators/`. Installs a `createClientPlugin` whose `onRequest` prepends `apiPathPart` segment to `request.url.encodedPathSegments` (build `["", apiPathPart] + nonEmptySegments`). Registered `singleWithRandomQualifier<HttpClientConfigurator>` in common client `Plugin.kt`.
  - composes with serverAddress `DefaultServerUrlPlugin` (that only fills host/proto/port; path already set) — order-independent.
  - covers EVERY HttpClient API call (auth, users, currency, wishlist, files download, temp_upload, admin, booking, sample, echo) → no per-feature edits.
- change_client_2: `FilesClientService` — `fileUrl(id)` (browser) returns absolute `/${apiPathPart}/${filesPrefixPathPart}/${id}` for `Img(src)`. `download(id)` switches to raw `client.get("${filesPrefixPathPart}/${id}")` (HttpClient plugin adds `api`) — avoids double prefix.

## Module dependency check
- `apiPathPart` in common/common commonMain — already a transitive dep of common/server + common/client. No build.gradle change.

## Affected files (final)
1. features/common/common/src/commonMain/kotlin/Constants.kt  (add const)
2. features/common/server/src/jvmMain/kotlin/JVMPlugin.kt     (routing split + api wrap + drop redirect)
3. features/common/server/src/jvmMain/kotlin/models/Config.kt (defaultWebClientSubPath="")
4. server/dev.config.json, server/local.config.json, server/sample.config.json (ui→"")
5. features/common/client/src/commonMain/kotlin/configurators/ApiPrefixHttpClientConfigurator.kt (new)
6. features/common/client/src/commonMain/kotlin/Plugin.kt      (register configurator)
7. features/files/client/src/commonMain/kotlin/FilesClientService.kt (fileUrl/download split)

status=architecture_complete; next=STEP_2 coding
