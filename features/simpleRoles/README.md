# Feature: SimpleRoles

## Operator Notes

<!-- Human operator writes here. Agents MUST read and respect before making any changes. Agents MUST NOT modify this section. -->

## Overview

Narrow, full-stack "am I superadmin" feature (issue #68 point 7). Exposes exactly one capability —
whether a user currently holds the SuperAdmin role — deliberately without any broader access to the
general role graph owned by `features/roles`. This is the feature every root-privilege check in the
app (issue #68 points 8/9) now goes through.

## Routes

> Served under the global `/api` prefix (e.g. `/api/simpleRoles/isSuperAdmin`), applied centrally by
> `features/common/server`.

| Method | Path | Auth | Body / Response | Description |
|--------|------|------|-----------------|-------------|
| GET | `/simpleRoles/isSuperAdmin` | Bearer | — → `Boolean` | Returns whether the authenticated caller (resolved from the bearer token) currently holds SuperAdmin |

## Models

| Type | Module | Description |
|------|--------|-------------|
| `SimpleRolesFeature` (server) | `simpleRoles/server` | `suspend fun isSuperAdmin(userId: UserId): Boolean` — caller identity passed explicitly, matching this app's server-feature convention. Implemented by `SimpleRolesFeatureService`. |
| `SimpleRolesFeature` (client) | `simpleRoles/client` | `suspend fun isSuperAdmin(): Boolean` — no argument; the server resolves the caller from the bearer token. **Not** mirrored 1:1 with the server interface — the signatures genuinely differ (arg vs. no-arg), which `agents/CODING.md`'s Full-Stack Feature pattern already anticipates. |
| `SimpleRolesFeatureService` | `simpleRoles/server` | Delegates to `features/roles`' `ReadRolesRepo.contains(...)`. |
| `KtorSimpleRolesFeature` | `simpleRoles/client` | HTTP-only realization (point 10, 1 of 2); no caching or logic beyond the call. |
| `CacheSimpleRolesFeature` | `simpleRoles/client` | Caching realization (point 10, 2 of 2); caches only the boolean answer, refreshed off `features/auth/client`'s `meStateFlow`. Also exposes `isSuperAdminStateFlow: StateFlow<Boolean>` beyond the `SimpleRolesFeature` interface — see Architecture Notes. Bound as the `SimpleRolesFeature` implementation in Koin. |

## Architecture Notes

- **Ktor + Cache split (point 10):** `KtorSimpleRolesFeature` performs the HTTP call only, per this
  codebase's "Ktor realization rule" (`agents/CODING.md`). `CacheSimpleRolesFeature` wraps it and is
  the concrete type bound as `SimpleRolesFeature` in `Plugin.setupDI` — every consumer that injects
  the interface gets the cached path.
- **Cache invalidation is keyed off `meStateFlow`, not a bare login/logout boolean.** Mirrors
  `features/auth/client/Plugin.kt`'s own `meStateFlow` refresh pattern
  (`merge(flowOf(Unit), meState).subscribeLoggingDropExceptions(scope) { ... }`) exactly. Using the
  richer `StateFlow<RegisteredUser?>` (rather than `AuthCredentialsStorage.userAuthorised: StateFlow<Boolean>`
  directly) means the cache also correctly re-fetches when the authenticated *identity* changes within
  one session (e.g. logout then a different login), not only on a bare auth-state flip.
- **`isSuperAdminStateFlow` is a deliberate, narrow exception to "depend on interfaces."**
  `UsersModel.isCurrentUserRootFlow` (see `features/ui/users/README.md`) needs a reactive
  `StateFlow<Boolean>` for its existing `combine(...)`/`.stateIn(...)` wiring, but point 7's interface
  is deliberately narrow (`suspend fun isSuperAdmin(): Boolean`, no `StateFlow`) and widening it would
  violate that scope. `CacheSimpleRolesFeature` exposes `isSuperAdminStateFlow` as an additional,
  concrete-type-only property — `features/ui/users/Plugin.kt` injects `CacheSimpleRolesFeature`
  directly (not the `SimpleRolesFeature` interface) specifically to read it, following the same "Typed
  definition & accessor helpers" shape this codebase already uses for `Koin.meStateFlow`/
  `Koin.secretMeMutableStateFlow`.
- **Dependencies:** `simpleRoles/server` depends on `roles/common` (for `ReadRolesRepo`/`SuperAdminRole`),
  `features/common/server`, `features/auth/server` (bearer caller resolution), `features/users/common`
  (`UserId`). `simpleRoles/client` depends on `features/common/client`, `features/auth/client` (for
  `meStateFlow`), `features/users/common` (for `RegisteredUser` in `meState`'s type).
