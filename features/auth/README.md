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
| GET | `/auth/config` | None | `→ AuthConfig` | Returns registration availability and email-policy flags |
| POST | `/auth/register` | None | `RegisterRequest → RegistrationResult \| 400` | Creates an approved account or a pending required-email account; 400 covers unavailable registration, invalid fields, duplicate values, and failed invite delivery |
| POST | `/auth/login` | None | `LoginRequest → AuthCredentials \| 401` | Validates credentials, returns token + refreshToken |
| POST | `/auth/refresh` | None | `RefreshRequest → AuthCredentials \| 401` | Exchanges refreshToken for new credentials |
| POST | `/auth/logout` | Bearer | `→ 200` | Invalidates the bearer token |
| GET | `/auth/getMe` | Bearer | `→ AuthFeatureUser \| 401` | Returns the caller's own record for the current token |
| POST | `/auth/requestPasswordChangeEmail` | Bearer | `PasswordChangeEmailRequest → PasswordChangeEmailRequestResult` | Requests one approval message for the caller's exact approved current email; ordinary outcomes remain `200` and the response is `Cache-Control: no-store` |
| POST | `/auth/completePasswordChange` | None | `CompletePasswordChangeRequest → PasswordChangeResult` | Anonymous redemption of the delivered approval; the supplied user id is an equality assertion against persisted approval state, and the response is `Cache-Control: no-store` plus `Referrer-Policy: no-referrer` |

## Models

| Type | Description |
|------|-------------|
| `Token` | `@JvmInline value class(String)` — short-lived access token |
| `RefreshToken` | `@JvmInline value class(String)` — long-lived refresh token |
| `Password` | `@JvmInline value class(String)` — BCrypt-hashed at rest |
| `AuthCredentials` | Wire DTO: `token: Token`, `refreshToken: RefreshToken` |
| `RegistrationResult` | Polymorphic successful-registration DTO: `authorized` wraps `AuthCredentials`; credential-free `pendingEmailVerification` means the password was stored and email verification is still required. |
| `AuthConfig` | Common wire DTO: `enableRegistration: Boolean`, `requireEmailForRegistration: Boolean` |
| `LoginRequest` | Wire DTO: `username: Username`, `password: Password` |
| `RegisterRequest` | Wire DTO: `username: Username`, `password: Password`, `email: Email?` — used for registration |
| `RefreshRequest` | Wire DTO: `refreshToken: RefreshToken` |
| `AuthFeatureUser` | `@Serializable` feature model returned by `getMe`/`getUser`/the "me" state flow: `id: UserId`, `username: Username`, `email: Email?`, `emailApproved: Boolean = false`. Deliberately keeps both email fields — this is the authenticated caller's own record, not a public listing; see its class KDoc. |
| `AuthFeature` | Shared interface: `login`, `refresh`, two-argument legacy `register`, email-aware `register`, `getConfig(): AuthConfig`, `isRegistrationAvailable`; the email-aware overload and `getConfig()` default to the legacy surfaces for source compatibility |
| `RegistrationEmailSender` | Server hook invoked for a provisional required-email account; after a successful invite the password is stored and registration returns credential-free `pendingEmailVerification`; normal login after verification is the first credential-producing step. |
| `CompensableRegistrationEmailSender` | Additive server capability extending `RegistrationEmailSender`; returns a request-local `RegistrationEmailDeliveryHandle` after successful delivery while retaining the legacy Boolean fallback for existing senders. |
| `RegistrationEmailDeliveryHandle` | Request-local rollback handle that removes exactly the delivered invite artifact when later required-email finalization fails or is cancelled. |
| `RegistrationRoleLifecycle` | Auth-owned dependency-inversion contract implemented by `features/roles`; marks a required self-registration pending and removes direct roles during compensation |
| `UserRoleAuthorization` | Auth-owned server port implemented by `features/roles`; synchronously ensures optional registration has direct `User` and checks current direct `User` membership. Missing binding denies access. |
| `ClientAuthFeature` | Client-only extension: `logout`, `getMe(): AuthFeatureUser?` |
| `ServerAuthFeature` | Server-only extension: `logout`, `getUser(token): AuthFeatureUser?` |
| `ServerUrlStorage` | Client-side interface: `getServerUrl / saveServerUrl` (platform-specific impls) |
| `AuthCredentialsStorage` | Client-side interface: `get / save AuthCredentials` (platform-specific impls) |
| `PasswordChangeEmailRequest` / `PasswordChangeEmailRequestResult` | Common request containing the expected approved `Email`; results are `Sent`, `Unavailable`, `Ineligible`, or `DeliveryFailed` without credentials or approvals. |
| `CompletePasswordChangeRequest` / `PasswordChangeResult` | Common anonymous completion DTO containing only `userId`, the existing `DeepLinkId`, and a plaintext `Password`; results are `Changed`, `InvalidApproval`, or `InvalidPassword`. Its diagnostic representation redacts the UUID and plaintext. |
| `PasswordChangeFeature` / `KtorPasswordChangeFeature` | Client-only HTTP capability for issuance and completion. Completion requires successful typed HTTP JSON and never performs a retry. |
| `ServerPasswordChangeFeature` | Auth-owned server port implemented by Email; makes Auth routes fail closed when Email/deeplinks infrastructure is absent without introducing an Auth-to-Email dependency. |

## Architecture Notes

- Tokens are in-memory `MapKeyValueRepo` on the server — **tokens are lost on server restart**.
- `BearerAuthenticationConfigurator` installs `bearer()` Ktor auth block. Protect routes with `authenticate() { ... }`.
- `getCallerUserIdOrAnswerUnauthorized()` utility (in `auth/server/utils/`) resolves `UserIdPrincipal → UserId` and auto-responds 401 on failure. All ownership-guarded routes must use this.
- `BearerAuthHttpClientConfigurator` installs Ktor `Auth` plugin on `HttpClient`; `refreshTokens` calls the refresh endpoint using the inner `client` (avoids recursion).
- `sendWithoutRequest` skips preemptive auth for `/auth/login`, `/auth/refresh`, `/auth/register`, and `/auth/is_registration_available` endpoints.
- `Config.enableRegistration` (default `false`) gates the register endpoint; disabled → service returns `null` → router responds 400.
- `Config.requireEmailForRegistration` (default `false`) requires a validated `Email` in `RegisterRequest`. Required-email registration is a compensated two-phase flow: under the global auth write lock it creates a provisional user without a password/session and marks the id pending through `RegistrationRoleLifecycle`; BCrypt hashing and `RegistrationEmailSender` delivery then run outside that lock; a final locked phase verifies the same user and email still exist before installing the password and returns credential-free `pendingEmailVerification`. Missing infrastructure, a false/ordinary failed delivery, or a failed pending transition removes the user, auth state, and direct roles so the same username can be retried. Cancellation performs cleanup in a non-cancellable context and then propagates. Optional-email registration synchronously ensures direct `User` through `UserRoleAuthorization`, then returns `authorized` only after guarded credential issuance.
- **Required-email create consistency:** provisional account creation and immediate user-compensation enrollment run in a bounded non-cancellable region bracketed by parent-context active checks. Cancellation received while the repository is returning propagates before the pending-role transition, after rollback ownership exists. The repository must eventually return; optional registration is unchanged, while process termination or a post-commit repository exception that withholds the created identity still requires repository-owned compensation or a durable commit receipt.
- **Delivered-invite compensation:** Auth detects `CompensableRegistrationEmailSender` at runtime without changing the existing `RegistrationEmailSender` binding or Boolean method. A successful capability call enrolls the returned request-local handle after the provisional-user and pending-role rollback actions, so reverse rollback removes the exact link first, then pending-role state, then user/password/session/direct roles. A Boolean-only sender preserves legacy behavior. Link cleanup runs in `NonCancellable`; a link-cleanup failure is suppressed on the original failure so later account cleanup still runs. Process termination after SMTP acceptance and before finalization remains outside this in-process rollback guarantee.
- Every credential issuance plus login, refresh, bearer authentication, and token-to-user lookup requires current direct `User` membership through `UserRoleAuthorization`. The absent bridge fails closed; role checks are not cached in token entries, so revocation invalidates still-unexpired bearer tokens immediately.
- `DuplicateUserFieldException` from the narrow registration create call maps to the existing `null`/HTTP 400 failure contract without disclosing whether username or email collided; other repository failures still propagate.
- `AuthFeatureService.register` enforces a password length policy (8..72): too-short/empty passwords are refused, and the upper bound avoids BCrypt silently ignoring input past 72 bytes. Returns `null` (→ 400) on violation. Admin-set passwords (root-only path) are not subject to this check.
- `AuthFeature.isRegistrationAvailable()` is the cross-cutting flag; server impl returns `enableRegistration` directly; client impl calls `GET /auth/is_registration_available` and deserializes the `Boolean` body.
- `GET /auth/config` returns the common `AuthConfig` DTO, allowing JS/JVM/Android registration forms to show and validate the email field consistently. A new client falls back to `GET /auth/is_registration_available` when the config request/status/body fails and treats the legacy server as optional-email; a successful config response does not make the fallback request.
- `AuthFeatureService` (server) requires `WriteUsersRepo` in addition to `ReadUsersRepo` to create accounts during registration.
- `AuthFeatureService.purgeUser(userId)` (server-only) removes the stored password hash and every active access/refresh session for a user; used by the admin user-delete cascade (`features/admin`).
- **Email-authorized password change:** an authenticated owner can request an approval only for the exact currently approved email. Email owns the persisted `DeepLinkId`; Auth's anonymous completion route treats its `userId` as an assertion, not a target selector. The approval must retain the same password-purpose payload, subject, approved email, credential-state fingerprint, and unexpired timestamp through final submission. Missing Email/deeplinks bindings yield `Unavailable` for issuance and `InvalidApproval` for completion.
- **Credential-state and commit boundary:** Auth derives a server-private SHA-256 fingerprint from a domain tag, user id, and current BCrypt record. Email's account coordinator is always acquired before Auth's write lock. Auth rechecks direct User membership, account, hash, and fingerprint; a bounded non-cancellable commit rereads/removes the exact approval before persisting the already-generated BCrypt hash. Cancellation then propagates instead of claiming success. Password completion creates no credentials and does not revoke, clear, refresh, or otherwise change existing sessions or roles.
- **Password-change policy:** only this new flow requires at least eight Kotlin characters and at most 72 UTF-8 bytes, without trimming or normalization. Existing registration still uses its documented Kotlin string-length check and root-only admin password behavior is unchanged.
- **Browser completion transport:** ordinary Auth requests still use the saved server URL. JS constructs `PasswordChangeCompletionUrl` only from `window.location.origin` and the fixed completion path; that request carries `AuthCircuitBreaker` and a request-local default-URL opt-out so it cannot refresh bearer credentials or be rewritten to a saved host/path/query. Native completion keeps normal configured-server selection.
- **Feature Interface Return Model Rule:** `getMe`/`getUser` and the "me" state flow now return `AuthFeatureUser` (a `common/models/` feature model) instead of the persistence entity `RegisteredUser` directly, per `agents/CODING.md`'s Feature Interface Return Model Rule. `AuthFeatureUser` deliberately keeps the current email and its approval flag for the authenticated owner; contrast with `features/users`' `UsersFeatureUser`, which drops both on the public listing.
- Role assignment for newly created/bootstrapped users (issue #68 and #73) is handled separately by `features/roles` — see `roles/README.md`.
- `SerializationConfigurator` sets `defaultRequest { contentType(ContentType.Application.Json) }` so individual request builders need not repeat it.
- `ServerUrlStorage` and `AuthCredentialsStorage` use `SmartRWLocker` for concurrent access safety.
- JS `LocalStorageServerUrlStorage` takes `useFallbackToWindowAddress` (default `true`): when no URL is stored in `localStorage`, `getServerUrl()` falls back to `window.location.origin` so a web client served from the same host as the API works without explicit configuration. Pass `false` to disable and return `null` on absence.

## Client-side "me" State

- New `features/auth/client/src/commonMain/kotlin/Me.kt` defines `meQualifier = named("me")` (Koin qualifier) and extensions `Koin.meStateFlow` / `Scope.meStateFlow` returning `StateFlow<AuthFeatureUser?>`.
- `features/auth/client/.../Plugin.kt` registers `MutableRedeliverStateFlow<AuthFeatureUser?>(null)` under internal qualifier `secretMeMutablemeStateFlowQualifier = named("secret_me")`; exposes read-only `StateFlow<AuthFeatureUser?>` under `meQualifier` via `asStateFlow()`.
- Internal accessors `Koin.secretMeMutableStateFlow` / `Scope.secretMeMutableStateFlow` return `MutableStateFlow<AuthFeatureUser?>` for write access.
- In `startPlugin`: on authorised → wraps `feature.getMe()` in `runCatchingLogging { }.getOrElse { null }` (failure → flow value=null); on logout → sets flow to `null`.
- Consumers should read the "me" flow via `Scope.meStateFlow` instead of calling `getMe()` per request, reducing redundant API calls. Login gate check in `features/ui/auth` still uses raw `getMe()` request intentionally (requires fresh auth validation).
