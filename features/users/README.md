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
| `NewUser` | Create payload: `username: Username` |
| `RegisteredUser` | Persisted entity: `id: UserId`, `username: Username` |
| `ReadUsersRepo` | Read-only repo interface |
| `WriteUsersRepo` | Write-only repo interface |
| `UsersRepo` | Combined CRUD repo interface |
| `CacheUsersRepo` | In-memory `FullCRUDCacheRepo` wrapper |
| `ExposedUsersRepo` | JVM/PostgreSQL implementation (`users` table) |
| `UsersFeature` | Server interface: `suspend fun getAll(): List<RegisteredUser>` |
| `UsersService` | Server implementation of `UsersFeature`, delegates to `ReadUsersRepo` |

## Architecture Notes

- **Server-side:** `UsersFeature` interface defines `getAll()` method returning all registered users. `UsersService` implements this interface and delegates to `ReadUsersRepo`. `UsersRoutingsConfigurator` (JVM) registers public endpoint `GET /users/getAll` (path constants: `usersPrefixPathPart = "users"`, `usersGetAllPathPart = "getAll"` in `features/users/common/Constants.kt`).
- **Client-side:** Mirror `UsersFeature` interface + `KtorUsersFeature` implementation in `features/users/client/src/commonMain/kotlin/`, registered in `client/Plugin.kt` (consumed by `features/ui/users`).
- `server/Plugin.kt` is intentionally empty. `auth/server/JVMPlugin` calls `users.common.JVMPlugin.setupDI` directly to wire `ExposedUsersRepo → CacheUsersRepo → UsersRepo` into the DI graph.
- `singleWithBinds<UsersRepo>` registers `CacheUsersRepo` as `UsersRepo`, `ReadUsersRepo`, and `WriteUsersRepo` simultaneously.
- Root user bootstrap happens inside `auth/server/JVMPlugin.startPlugin`: if `UsersRepo.count() == 0`, creates a `root` user and prints generated password once via KSLog.
