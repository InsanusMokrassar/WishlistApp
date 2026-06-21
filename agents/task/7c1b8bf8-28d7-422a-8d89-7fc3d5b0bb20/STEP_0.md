# STEP_0 — Planning

task_id=7c1b8bf8-28d7-422a-8d89-7fc3d5b0bb20; issue=#55; title="Make static available from root of site and API via `api` prefix"

## Requirement (issue #55)
- requirement_1: web UI (static) served from `/` (as previously), NOT from `/ui` sub-path.
- requirement_2: every API request served under `api` prefix → `/api/<feature>/<action>`.
- requirement_3: update server route configuration + config files.

## Current state (investigation result)
- server/common/jvmMain/JVMPlugin.kt: `get("/")` → `respondRedirect("/ui")`; static mounted from `config.staticFolders` (key `ui`); SPA fallback `default("index.html")`.
- `defaultWebClientSubPath = "ui"` (models/Config.kt).
- All feature API routes registered as `ApplicationRoutingConfigurator.Element` (auth, users, currency, wishlist, files+temp_upload, admin, booking, sample, echo). MicroUtils `ApplicationRoutingConfigurator` invokes every Element flat on `routing{}` root. Element receiver type = `Route` → can wrap with `route("api"){...}`.
- Static element is itself one of those Elements (registered in common/server JVMPlugin lines 162-187).
- Client: `KtorXxxFeature` classes build RELATIVE paths from per-feature `Constants.prefixPathPart`, sent through shared `HttpClient`. Stored server URL filled via `fillAbsentPartsWith` (path kept if present).
- Browser-direct API URL: ONLY `FilesClientService.fileUrl(id)` = `files/{id}`, consumed by `imageUrl` → `Img(src=...)` in ui/users + ui/wishlist (loaded by browser, NOT via HttpClient). `download(id)` reuses `client.get(fileUrl(id))` (HttpClient).
- All other client API calls go through `HttpClient` only.

## Problems / decisions
- prob_1: cannot wrap ALL Elements in `route("api")` blindly — static Element must stay at root. → split: collect feature Elements, wrap under `route(apiPathPart)`; build static Element separately at root.
- prob_2: `fileUrl` is dual-use (browser img + HttpClient download). A central HttpClient `api`-prefix plugin would double-prefix the browser string if reused. → decision: central HttpClient plugin prepends `api`; `fileUrl` (browser) hardcodes `/api/...` and is NO LONGER used for HttpClient `download` (download builds raw non-api path).
- prob_3: shared constant needed both sides. → add `const val apiPathPart = "api"` to existing empty `features/common/common/.../Constants.kt` (pkg accessible from server jvmMain + client).
- decision_static_root: change configs `"ui"`→`""`; `defaultWebClientSubPath = ""`; remove `/ui` redirect; static mounts at root with SPA `default("index.html")`.

## No operator-note conflicts found (no feature README Operator Notes block touched).
## No new feature/module → generate_feature.sh NOT needed.
status=plan_complete; next=STEP_1 architecture
