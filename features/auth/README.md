# Feature: Auth

## Operator Notes

<!-- Human operator writes here. Agents MUST read and respect before making any changes. Agents MUST NOT modify this section. -->

## Overview

End-to-end bearer-token authentication. Handles login (BCrypt password check), optional self-service registration, token issuance (UUID), token refresh, logout, and `getMe`. Client-side installs bearer auth automatically on every `HttpClient` request and transparently refreshes expired tokens. Depends on `features/users` for `UsersRepo` and `Username`.

## Routes

> All paths below are served under the global `/api` prefix (e.g. `/api/auth/login`). The prefix is applied centrally by `features/common/server` (`InternalApplicationRoutingConfigurator`) and added on the client by `DefaultUrlHttpClientConfigurator`, which appends `/api` to the configured server base URL.

| Method | Path | Auth | Body / Response | Description |
|--------|------|------|-----------------|-------------|
| GET | `/auth/is_registration_available` | None | `→ Boolean` | Returns `true` when self-service registration is open |
| POST | `/auth/register` | None | `RegisterRequest → AuthCredentials \| 400` | Creates account; 400 when disabled, username taken, or password length is outside 8..72 |
| POST | `/auth/login` | None | `LoginRequest → AuthCredentials \| 401` | Validates credentials, returns token + refreshToken |
| POST | `/auth/refresh` | None | `RefreshRequest → AuthCredentials \| 401` | Exchanges refreshToken for new credentials |
| POST | `/auth/logout` | Bearer | `→ 200` | Invalidates the bearer token |
| GET | `/auth/getMe` | Bearer | `→ AuthFeatureUser \| 401` | Returns the caller's own record for the current token |

## Models

| Type | Description |
|------|-------------|
| `Token` | `@JvmInline value class(String)` — short-lived access token |
| `RefreshToken` | `@JvmInline value class(String)` — long-lived refresh token |
| `Password` | `@JvmInline value class(String)` — BCrypt-hashed at rest |
| `AuthCredentials` | Wire DTO: `token: Token`, `refreshToken: RefreshToken` |
| `AuthConfig` | **Server-only** DTO: `enableRegistration: Boolean` — in `features/auth/server/models/` |
| `LoginRequest` | Wire DTO: `username: Username`, `password: Password` |
| `RegisterRequest` | Wire DTO: `username: Username`, `password: Password` — used for registration |
| `RefreshRequest` | Wire DTO: `refreshToken: RefreshToken` |
| `AuthFeatureUser` | `@Serializable` feature model returned by `getMe`/`getUser`/the "me" state flow: `id: UserId`, `username: Username`, `email: Email?`. Deliberately keeps `email` — this is the authenticated caller's own record, not a public listing; see its class KDoc. |
| `AuthFeature` | Shared interface: `login`, `refresh`, `register`, `isRegistrationAvailable` |
| `ClientAuthFeature` | Client-only extension: `logout`, `getMe(): AuthFeatureUser?` |
| `ServerAuthFeature` | Server-only extension: `logout`, `getUser(token): AuthFeatureUser?` |
| `ServerUrlStorage` | Client-side interface: `getServerUrl / saveServerUrl` (platform-specific impls) |
| `AuthCredentialsStorage` | Client-side interface: `get / save AuthCredentials` (platform-specific impls) |

## Architecture Notes

- Tokens are in-memory `MapKeyValueRepo` on the server — **tokens are lost on server restart**.
- `BearerAuthenticationConfigurator` installs `bearer()` Ktor auth block. Protect routes with `authenticate() { ... }`.
- `getCallerUserIdOrAnswerUnauthorized()` utility (in `auth/server/utils/`) resolves `UserIdPrincipal → UserId` and auto-responds 401 on failure. All ownership-guarded routes must use this.
- `BearerAuthHttpClientConfigurator` installs Ktor `Auth` plugin on `HttpClient`; `refreshTokens` calls the refresh endpoint using the inner `client` (avoids recursion).
- `sendWithoutRequest` skips preemptive auth for `/auth/login`, `/auth/refresh`, `/auth/register`, and `/auth/is_registration_available` endpoints.
- `Config.enableRegistration` (default `false`) gates the register endpoint; disabled → service returns `null` → router responds 400.
- `AuthFeatureService.register` enforces a password length policy (8..72): too-short/empty passwords are refused, and the upper bound avoids BCrypt silently ignoring input past 72 bytes. Returns `null` (→ 400) on violation. Admin-set passwords (root-only path) are not subject to this check.
- `AuthFeature.isRegistrationAvailable()` is the cross-cutting flag; server impl returns `enableRegistration` directly; client impl calls `GET /auth/is_registration_available` and deserializes the `Boolean` body.
- `AuthConfig` is server-only (package `dev.inmo.wishlist.features.auth.server.models`) — client never imports it.
- `AuthFeatureService` (server) requires `WriteUsersRepo` in addition to `ReadUsersRepo` to create accounts during registration.
- `AuthFeatureService.purgeUser(userId)` (server-only) removes the stored password hash and every active access/refresh session for a user; used by the admin user-delete cascade (`features/admin`).
- **Feature Interface Return Model Rule:** `getMe`/`getUser` and the "me" state flow now return `AuthFeatureUser` (a `common/models/` feature model) instead of the persistence entity `RegisteredUser` directly, per `agents/CODING.md`'s Feature Interface Return Model Rule. `AuthFeatureUser` deliberately keeps `email` (own-record surface); contrast with `features/users`' `UsersFeatureUser`, which drops it on the public listing.
- `SerializationConfigurator` sets `defaultRequest { contentType(ContentType.Application.Json) }` so individual request builders need not repeat it.
- `ServerUrlStorage` and `AuthCredentialsStorage` use `SmartRWLocker` for concurrent access safety.
- JS `LocalStorageServerUrlStorage` takes `useFallbackToWindowAddress` (default `true`): when no URL is stored in `localStorage`, `getServerUrl()` falls back to `window.location.origin` so a web client served from the same host as the API works without explicit configuration. Pass `false` to disable and return `null` on absence.

## Client-side "me" State

- New `features/auth/client/src/commonMain/kotlin/Me.kt` defines `meQualifier = named("me")` (Koin qualifier) and extensions `Koin.meStateFlow` / `Scope.meStateFlow` returning `StateFlow<AuthFeatureUser?>`.
- `features/auth/client/.../Plugin.kt` registers `MutableRedeliverStateFlow<AuthFeatureUser?>(null)` under internal qualifier `secretMeMutablemeStateFlowQualifier = named("secret_me")`; exposes read-only `StateFlow<AuthFeatureUser?>` under `meQualifier` via `asStateFlow()`.
- Internal accessors `Koin.secretMeMutableStateFlow` / `Scope.secretMeMutableStateFlow` return `MutableStateFlow<AuthFeatureUser?>` for write access.
- In `startPlugin`: on authorised → wraps `feature.getMe()` in `runCatchingLogging { }.getOrElse { null }` (failure → flow value=null); on logout → sets flow to `null`.
- Consumers should read the "me" flow via `Scope.meStateFlow` instead of calling `getMe()` per request, reducing redundant API calls. Login gate check in `features/ui/auth` still uses raw `getMe()` request intentionally (requires fresh auth validation).
