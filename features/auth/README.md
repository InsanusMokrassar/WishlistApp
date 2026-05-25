# Feature: Auth

## Operator Notes

<!-- Human operator writes here. Agents MUST read and respect before making any changes. Agents MUST NOT modify this section. -->

## Overview

End-to-end bearer-token authentication. Handles login (BCrypt password check), token issuance (UUID), token refresh, logout, and `getMe`. Client-side installs bearer auth automatically on every `HttpClient` request and transparently refreshes expired tokens. Depends on `features/users` for `UsersRepo` and `Username`.

## Routes

| Method | Path | Auth | Body / Response | Description |
|--------|------|------|-----------------|-------------|
| POST | `/auth/login` | None | `LoginRequest → AuthCredentials \| 401` | Validates credentials, returns token + refreshToken |
| POST | `/auth/refresh` | None | `RefreshRequest → AuthCredentials \| 401` | Exchanges refreshToken for new credentials |
| POST | `/auth/logout` | Bearer | `→ 200` | Invalidates the bearer token |
| GET | `/auth/getMe` | Bearer | `→ RegisteredUser \| 401` | Returns the user record for the current token |

## Models

| Type | Description |
|------|-------------|
| `Token` | `@JvmInline value class(String)` — short-lived access token |
| `RefreshToken` | `@JvmInline value class(String)` — long-lived refresh token |
| `Password` | `@JvmInline value class(String)` — BCrypt-hashed at rest |
| `AuthCredentials` | Wire DTO: `token: Token`, `refreshToken: RefreshToken` |
| `LoginRequest` | Wire DTO: `username: Username`, `password: Password` |
| `RefreshRequest` | Wire DTO: `refreshToken: RefreshToken` |
| `AuthFeature` | Shared interface: `login`, `refresh` |
| `ServerAuthFeature` | Server-only extension: `logout`, `getUser` |
| `ServerUrlStorage` | Client-side interface: `getServerUrl / saveServerUrl` (platform-specific impls) |
| `AuthCredentialsStorage` | Client-side interface: `get / save AuthCredentials` (platform-specific impls) |

## Architecture Notes

- Tokens are in-memory `MapKeyValueRepo` on the server — **tokens are lost on server restart**.
- `BearerAuthenticationConfigurator` installs `bearer()` Ktor auth block. Protect routes with `authenticate() { ... }`.
- `getCallerUserIdOrAnswerUnauthorized()` utility (in `auth/server/utils/`) resolves `UserIdPrincipal → UserId` and auto-responds 401 on failure. All ownership-guarded routes must use this.
- `BearerAuthHttpClientConfigurator` installs Ktor `Auth` plugin on `HttpClient`; `refreshTokens` calls the refresh endpoint using the inner `client` (avoids recursion).
- `sendWithoutRequest` skips preemptive auth for `/auth/login` and `/auth/refresh` endpoints.
- `ServerUrlStorage` and `AuthCredentialsStorage` use `SmartRWLocker` for concurrent access safety.
