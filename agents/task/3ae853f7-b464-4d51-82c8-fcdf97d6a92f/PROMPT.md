# Task: GitHub issue #45 — Add deeplinks feature

## Issue body (verbatim)

This feature will:
1. Store declared deeplinks uuids + handler info
2. Provide server only feature for creating deeplinks with attached handler info
3. Call handler when deeplink have been called.

Deeplink here is just subpart like `links/<deeplink_uui>`

Handler info is serializable data class, stored as json. In this feature must be declared interface `DeepLinkHandler` with suspend fun `tryHandle(deeplinkId: DeepLinkId, handlerInfo: Any): Boolean` - this fun must return true when deeplink have been processed (handler knows the type of this handler info and tried to process it)

This feature must not have any code (excluding template one) on client side, during it is planned as server-only feature

## Orchestration constraints (from operator prompt)

- Branch already `fix/45-deeplinks`. NO git branch/commit/push/stash. Parent handles git/PR.
- Two prior PRs (#48 `issue/45-deeplinks`, #59 `issue/45-deeplinks-redo`) were CLOSED by operator WITHOUT comments (silent rejection). Fresh max-effort implementation; reference-only inspection of priors allowed; do NOT blindly copy. Match repo conventions exactly.
- EXACT API required: interface `DeepLinkHandler` with `suspend fun tryHandle(deeplinkId: DeepLinkId, handlerInfo: Any): Boolean`; URL subpath `links/<deeplink_uuid>`; handler info = serializable data class stored as JSON.
- Client side: template/scaffold code ONLY (server-only feature).

## Definition of Done

- New deeplinks feature matching issue spec, wired into server + build like existing features.
- Affected/new module(s) + server BUILD successfully (gradle, BUILD SUCCESSFUL).
- Feature dir has complete README.md per required structure (incl. `## Operator Notes`).
- Validation role confirms correctness.

## Orchestrator-collected reference facts (for downstream roles)

- Reference full-stack feature: `features/booking` (CRUD + service + routing configurator + plugins).
- JSON-blob storage pattern: `features/files/common/jvmMain/repo/ExposedFilesMetaInfoRepo.kt` — `ExposedKeyValueRepo<String,String>` + `withMapper`, encodes value with injected `Json`.
- `DeepLinkId` modeled like `features/files/common/.../models/FileId.kt`: `@Serializable @JvmInline value class DeepLinkId(val string: String)`.
- `Json` is a Koin `single` provided by `features/common/common` Plugin (binds StringFormat/SerialFormat).
- uuid4 via `com.benasher44.uuid.uuid4()` (see `features/files/.../FilesService.kt`, `features/auth/.../AuthFeatureService.kt`).
- Route registration: feature server plugin registers `ApplicationRoutingConfigurator.Element` via `singleWithRandomQualifier`; paths are RELATIVE (no `/api`, added centrally). Server-only feature => route under `links/{deeplink_uuid}`.
- Scaffolding: `./generate_feature.sh` (prompt feature name); then register 3 modules in `settings.gradle`, add `server/build.gradle` dep, add server plugin FQCN to `server/sample.config.json`. Client modules remain template scaffold (server-only => add client plugins to Main.kt entrypoints ONLY as scaffold, matching how generated client stubs are wired for every feature — investigate whether server-only features still register client plugins).
- IMPORTANT pre-existing state: `features/deeplinks/` dir currently exists in working tree but contains ONLY stale `build/` artifacts (no source, not git-tracked). Must be removed/regenerated cleanly. NOT registered in settings.gradle.
</content>
