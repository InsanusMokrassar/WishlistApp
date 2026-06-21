# Feature: Admin

## Operator Notes

<!-- Human operator writes here. Agents MUST read and respect before making any changes. Agents MUST NOT modify this section. -->

* Only `root` user must have access to the admin panel and features
* If `root` use is not registered - it must be registered in server `Plugin`

Expected opportunities:

* Full users management access
* Full wishlists management access
* It is expected that on server and client sides will be created `AdminFeature` which will allow all required operations
* For users management must be created feature (in the admin module) named `UsersManagementFeature`. It must contain CRUD operations for working with users management: getting of all users, creating new one, updating users info and removing users. Creating of users must be made with provided password
* All other operations inside of `AdminFeature` on server side MUST call exists functionality. No new functionality must be created. For example, for management of wishlists must be used `WishlistsFeature`

## Overview

The admin feature provides a root-only management API for users and wishlists. All endpoints require bearer auth and the authenticated caller must be the `root` user — non-root callers receive `403 Forbidden`.

Root user bootstrapping is handled by `features/auth/server/src/jvmMain/kotlin/JVMPlugin.kt`: if no users exist, a `root` account is created with a randomly generated password printed to server logs (printed once, store it immediately).

### Modules

| Module | Path | Purpose |
|---|---|---|
| `wishlist.features.admin.common` | `features/admin/common` | Shared constants and models |
| `wishlist.features.admin.server` | `features/admin/server` | Server-side feature, services, routing |
| `wishlist.features.admin.client` | `features/admin/client` | Client-side feature interfaces and Ktor implementations |

## Routes

> All paths below are served under the global `/api` prefix (e.g. `/api/admin/users/getAll`). The prefix is applied centrally by `features/common/server` (`InternalApplicationRoutingConfigurator`) and added on the client by `DefaultUrlHttpClientConfigurator`, which appends `/api` to the configured server base URL.

All routes are under `/admin` prefix and require bearer authentication. Caller must be the `root` user.

### Users Management (`/admin/users/...`)

| Method | Path | Body | Response | Description |
|---|---|---|---|---|
| GET | `/admin/users/getAll` | — | `List<RegisteredUser>` | Get all registered users |
| POST | `/admin/users/create` | `NewUserWithPassword` | `RegisteredUser` | Create user with plaintext password |
| PUT | `/admin/users/update/{id}` | `NewUser` | `200 OK` / `404` | Update user info by id |
| PUT | `/admin/users/setPassword/{id}` | `Password` | `200 OK` / `404` | Replace a user's password; delegates to existing `AuthFeatureService.setPassword` |
| DELETE | `/admin/users/delete/{id}` | — | `200 OK` / `404` | Delete user by id; cascades all related data (wishlists, items, password, sessions) |

### Wishlists Management (`/admin/wishlists/...`)

Ownership checks are bypassed on mutating routes — admin can update or delete any wishlist regardless of owner. Read and create routes delegate to the existing `WishlistService`. Write routes that bypass ownership use `WishlistRepo` directly (existing functionality, no new repos created).

| Method | Path | Body | Response | Description |
|---|---|---|---|---|
| GET | `/admin/wishlists/getByUserId/{userId}` | — | `List<RegisteredWishlist>` | Get all wishlists for a user |
| GET | `/admin/wishlists/getById/{id}` | — | `RegisteredWishlist` / `404` | Get one wishlist by id |
| POST | `/admin/wishlists/create` | `NewWishlist` | `RegisteredWishlist` | Create wishlist for any user (userId in body) |
| PUT | `/admin/wishlists/update/{id}` | `NewWishlistInFeature` | `RegisteredWishlist` / `404` | Update any wishlist; owner preserved |
| DELETE | `/admin/wishlists/delete/{id}` | — | `200 OK` / `404` | Delete any wishlist |

## Models

### `NewUserWithPassword` (`admin.common.models`)

Used as POST body for admin user creation.

```kotlin
@Serializable
data class NewUserWithPassword(
    val username: Username,
    val password: Password   // plaintext; hashed server-side via BCrypt
)
```

### `NewWishlist` (`wishlist.common.models`) — reused

Used as POST body for admin wishlist creation. Contains explicit `userId` so admin can create for any user.

```kotlin
@Serializable
data class NewWishlist(
    val userId: UserId,
    val title: String
)
```

## Architecture Notes

### Server side

- `UsersManagementFeature` — service class; wraps `UsersRepo` (CRUD) + `AuthFeatureService` (password hashing via BCrypt) + `WishlistRepo` + `WishlistItemRepo`. No new repo or table. `delete(id)` cascades: for each wishlist owned by the user it deletes all items then the wishlist, then `AuthFeatureService.purgeUser(id)` removes the password hash and all active access/refresh sessions, then the user record is removed.
- `AdminFeature` — thin wrapper holding `UsersManagementFeature`. Injected into `AdminRoutingsConfigurator`.
- `AdminRoutingsConfigurator` — registers all `/admin/...` routes under `authenticate { }`. Uses `requireAdmin()` helper (private `RoutingContext` extension) to verify caller is `root`.
  - Wishlist reads delegate to `WishlistService` (existing).
  - Wishlist writes that bypass ownership (update, delete) delegate to `WishlistRepo` directly (existing functionality, per operator constraint).
- `Plugin` — registers `UsersManagementFeature`, `AdminFeature`, and `AdminRoutingsConfigurator` as `ApplicationRoutingConfigurator.Element`.
- `JVMPlugin` — delegates to `Plugin`.

Register in `server/sample.config.json` plugins list:
```json
"dev.inmo.wishlist.features.admin.server.JVMPlugin"
```
Must come after `wishlist.server.JVMPlugin`.

### Client side

- `UsersManagementFeature` — interface: `getAll`, `getById`, `create`, `update`, `setPassword`, `delete`. `setPassword(id, Password)` delegates server-side to the existing `AuthFeatureService.setPassword` (no new functionality), used by the public profile-edit screen when `root` changes another user's password.
- `AdminWishlistsFeature` — interface: `getByUserId`, `getById`, `create`, `update`, `delete`.  
  Create takes `NewWishlist` (explicit `userId`) unlike the regular `WishlistsFeature.create`.
- `AdminFeature` — interface: `val usersManagement: UsersManagementFeature`, `val wishlists: AdminWishlistsFeature`.
- `KtorUsersManagementFeature` — Ktor impl; calls `/admin/users/...` endpoints.
- `KtorAdminWishlistsFeature` — Ktor impl; calls `/admin/wishlists/...` endpoints.
- `KtorAdminFeature` — concrete impl of `AdminFeature` composing the two Ktor features.
- `Plugin` — registers all three Ktor implementations + interface bindings in Koin.
- `JVMPlugin`, `JSPlugin`, `AndroidPlugin` — delegate to the corresponding `admin.common` platform plugin then to `Plugin`.
