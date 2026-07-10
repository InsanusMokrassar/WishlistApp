# Feature: Users

## Operator Notes

<!-- Human operator writes here. Agents MUST read and respect before making any changes. Agents MUST NOT modify this section. -->

## Overview

User identity storage and public read-only API. Provides the `UsersRepo` CRUD repository and `UserId`/`Username`/`User` domain models used across all features. Exposes a single public HTTP endpoint `GET /users/getAll` for listing all registered users. Client-side mirror feature allows consumers to fetch the user list.

## Routes

> All paths below are served under the global `/api` prefix (e.g. `/api/users/getAll`). The prefix is applied centrally by `features/common/server` (`InternalApplicationRoutingConfigurator`) and added on the client by `DefaultUrlHttpClientConfigurator`, which appends `/api` to the configured server base URL.

| Method | Path | Auth | Body / Response | Description |
|--------|------|------|-----------------|-------------|
| GET | `/users/getAll` | None | `List<RegisteredUser>` | Fetch all registered users |

## Models

| Type | Description |
|------|-------------|
| `UserId` | `@JvmInline value class` wrapping `Long` — primary key |
| `Username` | `@JvmInline value class` wrapping `String` |
| `NewUser` | Create payload: `username: Username`, `email: Email? = null` |
| `RegisteredUser` | Persisted entity: `id: UserId`, `username: Username`, `email: Email? = null` |
| `ReadUsersRepo` | Read-only repo interface |
| `WriteUsersRepo` | Write-only repo interface |
| `UsersRepo` | Combined CRUD repo interface |
| `CacheUsersRepo` | In-memory `FullCRUDCacheRepo` wrapper |
| `ExposedUsersRepo` | JVM/PostgreSQL implementation (`users` table) |
| `DuplicateUserFieldException` | Thrown by `ExposedUsersRepo.update`/`create` on a Postgres unique-constraint violation (username or email); propagates through `CacheUsersRepo` untouched; callers should catch and respond `409 Conflict`. |
| `UsersFeature` | Server interface: `suspend fun getAll(): List<RegisteredUser>` |
| `UsersService` | Server implementation of `UsersFeature`, delegates to `ReadUsersRepo` |

## Architecture Notes

- **Server-side:** `UsersFeature` interface defines `getAll()` method returning all registered users. `UsersService` implements this interface and delegates to `ReadUsersRepo`. `UsersRoutingsConfigurator` (JVM) registers public endpoint `GET /users/getAll` (path constants: `usersPrefixPathPart = "users"`, `usersGetAllPathPart = "getAll"` in `features/users/common/Constants.kt`).
- **Client-side:** Mirror `UsersFeature` interface + `KtorUsersFeature` implementation in `features/users/client/src/commonMain/kotlin/`, registered in `client/Plugin.kt` (consumed by `features/ui/users`).
- `server/Plugin.kt` is intentionally empty. `auth/server/JVMPlugin` calls `users.common.JVMPlugin.setupDI` directly to wire `ExposedUsersRepo → CacheUsersRepo → UsersRepo` into the DI graph.
- `singleWithBinds<UsersRepo>` registers `CacheUsersRepo` as `UsersRepo`, `ReadUsersRepo`, and `WriteUsersRepo` simultaneously.
- Root user bootstrap happens inside `auth/server/JVMPlugin.startPlugin`: if `UsersRepo.count() == 0`, creates a `root` user and prints generated password once via KSLog — see `roles/README.md` for which roles get granted to a newly created user and by what mechanism (issue #68).
- **Email field (added in issue #44):** `User`, `NewUser`, and `RegisteredUser` all carry `email: Email? = null` (defaults to `null` for back-compat). `ExposedUsersRepo` has a `nullable text("email")` column, now also `.uniqueIndex()`-constrained — `NULL` values are exempt from the uniqueness check under standard SQL unique-index semantics, so users without a stored email never collide with each other, while two users sharing the same non-null email throws `DuplicateUserFieldException` (see "Duplicate-key-to-409 convention" below). `createMissingTablesAndColumns` (via `initTable()`) adds the column, and now also the unique index, to existing tables on first startup after this change, without a separate migration step. Invalid stored values are read defensively via `Email.parse(...).getOrNull()`. `features/users/common` now depends on `features/email/common` for the `Email` type.
- Self-service email update is exposed via `PUT /api/email/myEmail` in `features/email/server` (not under `/users`) to keep the email-routing scope cohesive.
- **Duplicate-key-to-409 convention:** `usernameColumn` and `emailColumn` are both `.uniqueIndex()`-constrained. `ExposedUsersRepo.update`/`create` override the library defaults (`AbstractExposedWriteCRUDRepo.update`/`create`) to catch `org.jetbrains.exposed.v1.exceptions.ExposedSQLException`, check `isUniqueViolation()` (SQL state `23505`, the Postgres `unique_violation` code), and translate a match into `dev.inmo.wishlist.features.users.common.repo.exceptions.DuplicateUserFieldException` — every other exception is rethrown unchanged. `CacheUsersRepo`'s `FullCRUDCacheRepo` write wrapper does not catch exceptions from the wrapped repo, so the exception propagates through it untouched. Consumers that need to distinguish "duplicate" from other failures catch `DuplicateUserFieldException` at the HTTP boundary and respond `409 Conflict` instead of the generic `500 Internal Server Error` an unmapped exception would otherwise produce (Ktor's engine-level `DefaultEnginePipeline.handleFailure` fallback). Current consumers: `features/email/server`'s `PUT /email/myEmail`, and `features/admin/server`'s `PUT /admin/users/update/{id}` and `POST /admin/users/create`. A future feature hitting the same shape (a unique-constrained write whose caller needs a distinguishable "duplicate" signal) should follow this same pattern rather than invent a new one.
