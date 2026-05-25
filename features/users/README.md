# Feature: Users

## Operator Notes

<!-- Human operator writes here. Agents MUST read and respect before making any changes. Agents MUST NOT modify this section. -->

## Overview

User identity storage. Provides the `UsersRepo` CRUD repository and `UserId`/`Username`/`User` domain models used across all features. Does **not** expose any HTTP routes — it is purely a repository module consumed by `auth` and other features.

## Routes

None. No HTTP endpoints.

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

## Architecture Notes

- `server/Plugin.kt` is intentionally empty. `auth/server/JVMPlugin` calls `users.common.JVMPlugin.setupDI` directly to wire `ExposedUsersRepo → CacheUsersRepo → UsersRepo` into the DI graph.
- `singleWithBinds<UsersRepo>` registers `CacheUsersRepo` as `UsersRepo`, `ReadUsersRepo`, and `WriteUsersRepo` simultaneously.
- Root user bootstrap happens inside `auth/server/JVMPlugin.startPlugin`: if `UsersRepo.count() == 0`, creates a `root` user and prints generated password once via KSLog.
- `users/client` and `users/server` modules exist for structural completeness; both are currently empty shells.
