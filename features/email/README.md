# Feature: Email

## Operator Notes

<!-- Human operator writes here. Agents MUST read and respect before making any changes. Agents MUST NOT modify this section. -->

## Overview

The email feature provides SMTP-backed transactional email delivery and per-user email address storage. It is structured as a standard full-stack feature with `common`, `server`, and `client` modules.

Required-email registration also uses this feature's server-only invite sender. The sender mints an
existing deeplink, emails its absolute URL, and lets the deeplink handler approve the account. A
successful required-email registration remains logged out: it stores a password but produces no
credentials until verification promotes `NewUser` to `User`; a later normal login is the first
credential-producing step.

**Two independent capabilities:**
- **Email storage** (`PUT /email/myEmail`) — any authenticated user can store or clear their own email address; does NOT require SMTP to be configured.
- **Email delivery** (`POST /email/sendTest`) — root-only action to verify SMTP configuration by sending a test message to a supplied address; requires SMTP to be configured.

When no `"email"` object is present in the server config (the key is entirely absent — or, if ever set, is JSON `null`), the feature operates in no-op mode: `GET /email/enabled` returns `false`, `POST /email/sendTest` returns `false` without attempting a connection, and `PUT /email/myEmail` (storage) keeps working normally — see Architecture Notes.

## Routes

> All paths below are served under the global `/api` prefix. Paths are assembled from constants in `EmailConstants`.

| Method | Path | Auth | Body / Response | Description |
|--------|------|------|-----------------|-------------|
| GET | `/email/enabled` | None | `Boolean` | Returns whether SMTP delivery is configured server-side |
| POST | `/email/sendTest` | Bearer + root only | `TestEmailRequest { recipient: Email }` / `200 OK` or `500` | Sends a test message to `recipient` via configured SMTP |
| PUT | `/email/myEmail` | Bearer (self) | `SetEmailRequest { email: Email? }` / `200 OK` or `409 Conflict` | Stores or clears the authenticated caller's own email address; `409` when the address is already stored for a different user |

## Models

| Type | Module | Description |
|------|--------|-------------|
| `Email` | `email/common` | `@JvmInline value class` wrapping a validated RFC-ish email string. Private constructor; constructed via `Email(value)` (throws `IllegalArgumentException`) or `Email.parse(value): Result<Email>` (non-throwing). Serializes as a bare JSON string; re-validates on decode. |
| `EmailSerializer` | `email/common` | `KSerializer<Email>` using `PrimitiveKind.STRING`; used via `@Serializable(with = EmailSerializer::class)`. |
| `EmailConstants` | `email/common` | Shared path-segment constants: `prefixPathPart`, `enabledPathPart`, `sendTestPathPart`, `myEmailPathPart`. |
| `EmailFeature` (server) | `email/server` | Server-side interface: `isFeatureEnabled()`, `sendTestEmail(callerId, recipient)`, `setMyEmail(callerId, email?)` — every caller-scoped method receives the authenticated `UserId` explicitly. Implemented by `EmailFeatureService`. |
| `EmailFeature` (client) | `email/client` | Client-side interface: `isFeatureEnabled()`, `sendTestEmail(recipient)`, `setMyEmail(email?)` — caller identity is resolved server-side from the bearer token. Implemented by `KtorEmailFeature`. |
| `TestEmailRequest` | `email/common` | `@Serializable data class(recipient: Email)` — body for `POST /email/sendTest`. |
| `SetEmailRequest` | `email/common` | `@Serializable data class(email: Email? = null)` — body for `PUT /email/myEmail`. |
| `EmailConfig` | `email/server` | Config slice: `smtp: SmtpConfig` (non-nullable) — decoded from the nested `"email"` object (`config["email"]`) in the server config JSON, not the whole root object. |
| `SmtpConfig` | `email/server` | SMTP settings: `host`, `port` (587), `username?`, `password?`, `from: Email`, `useTls` (true), `useSsl` (false). |
| `SmtpEmailService` | `email/server` | SMTP transport; implements `EmailsService`. Builds `jakarta.mail.Session` from `SmtpConfig` and calls `Transport.send` on `Dispatchers.IO`. Returns `false` when the configured host is blank or on a non-cancellation error; coroutine cancellation always propagates. Only ever constructed by `Plugin` when the `"email"` key is present and non-null. Wrapped by `EmailFeatureService` for the `EmailFeature` surface. |
| `EmailVerificationAccountCoordinator` | `email/server` | Unconditional server singleton owning `UsersRepo`, `RolesRepo`, and one process-local mutex. Serializes self-service stored-email mutation with invited-email equality checking plus pending-to-approved role promotion. |
| `EmailFeatureService` | `email/server` | SMTP-enabled `EmailFeature` implementation; wraps a non-nullable `EmailsService`, the shared `EmailVerificationAccountCoordinator`, and `RolesFeature`. Test sending remains SuperAdmin-only; `setMyEmail` uses the shared coordinator. |
| `DisabledEmailFeature` | `email/server` | SMTP-disabled `EmailFeature` implementation; sending remains disabled while `setMyEmail` uses the same shared coordinator, so storage behavior and verification atomicity do not depend on SMTP configuration. |
| `EmailsService` | `email/server` | Server-only send interface (no HTTP exposure): `sendText(recipient, subject, text)`, `sendTextWithAttachments(recipient, subject, text, attachments)`, `sendHtml(recipient, subject, html)` — all `suspend`, all return `Boolean` (`false` when SMTP is disabled or on error). Implemented by `SmtpEmailService`; bound in Koin only when SMTP is configured. |
| `EmailVerificationPayload` | `email/server` | Server-only polymorphic deeplink payload carrying the pending `UserId` and invited `Email`; the email is nullable only so persisted pre-change payloads decode and fail closed. |
| `EmailRegistrationInviteSender` | `email/server` | Auth registration hook that creates an email-bound verification deeplink and sends an absolute URL below validated `publicHttpOrigin`; removes the link on false/ordinary failure and non-cancellably removes it before propagating cancellation. |
| `EmailVerificationDeepLinkHandler` | `email/server` | Deeplink handler that delegates nullable invited-email validation, current-user lookup, exact email comparison, and role promotion to the shared coordinator; wrong payload types remain unhandled. |
| `EmailAttachment` | `email/server` | Attachment model for `sendTextWithAttachments`: `fileName`, `mimeType` (default `application/octet-stream`), `content: suspend () -> InputStream`. The provider may be invoked multiple times and must return a fresh stream on each call; content is streamed, never buffered as a whole `ByteArray`. |
| `KtorEmailFeature` | `email/client` | Client `EmailFeature` impl; HTTP-only, no caching or business logic. |

## Architecture Notes

- **`Email` ownership:** The `Email` value class lives in `features/email/common` and is the single source of truth. `features/users/common` declares `api project(":wishlist.features.email.common")` — this is legal per the existing `auth/common → users/common` precedent (a feature's `*/common` may depend on another feature's `*/common`; only the literal `features/common/*` base modules are restricted).
- **Config-slice pattern (nested key):** `EmailConfig` is decoded via `get<Json>().decodeFromJsonElement(EmailConfig.serializer(), config["email"])` in `email/server/Plugin.kt` — a **nested-key** pattern, deliberately different from `CurrencyConfig`'s root-flat-key pattern (`CurrencyConfig` decodes from the whole root object). This is the first feature in the codebase to gate `single` registrations on JSON-key presence (a DI-graph-shape "disabled" state) rather than handling a null/absent value at runtime. Omitting the `"email"` key entirely (not setting a nested `"smtp"` to `null` within it) is the documented way to disable the feature.
- **DI-graph-shape "disabled" state:** `EmailConfig`, `SmtpEmailService`, and the `EmailsService` binding are registered together, conditionally, only when `config["email"]` is present and non-null — implemented via the pure, Koin-free helper `emailConfigElementOrNull(config)` in `Plugin.kt` (unit-tested directly in `PluginTest`, while dedicated Koin graph tests cover singleton coordination in both shapes). `EmailFeature` is always registered unconditionally; it resolves `getOrNull<EmailsService>()` directly inside an inline `single<EmailFeature>` block and picks `EmailFeatureService` (present) or `DisabledEmailFeature` (absent).
- **Storage vs sending:** Per-user email storage (`PUT /email/myEmail`) is independent of SMTP and works through both `EmailFeatureService` and `DisabledEmailFeature`. Both implementations receive the same Koin-singleton `EmailVerificationAccountCoordinator`; no email service updates `UsersRepo` directly. The coordinator is also used by verification, making self-service mutation atomic with current-email equality checking plus role promotion.
- **Jakarta Mail (Angus):** `org.eclipse.angus:angus-mail:2.0.3` is consumed only in `features/email/server` (JVM-only module via `mppJavaProject`). Jakarta Mail imports are safe in `commonMain` of that module. Blocking `Transport.send` is wrapped in `withContext(Dispatchers.IO)`.
- **`EmailsService` (server-only send surface):** `EmailsService` (`sendText` / `sendTextWithAttachments` / `sendHtml`, each with an explicit `subject`) is an internal server capability implemented by `SmtpEmailService` and bound in Koin (`single<EmailsService> { get<SmtpEmailService>() }`, only when SMTP is configured). It is deliberately NOT wired into `EmailFeature`, `KtorEmailFeature`, or any HTTP route. All three send paths share one private `send(...)` skeleton (blank-host check, session, envelope, `Transport.send` on `Dispatchers.IO`, cancellation propagation, warn-log on other failures) — `SmtpEmailService` no longer exposes an `isFeatureEnabled()` method; "not configured at all" is now a DI-graph-shape fact handled by `DisabledEmailFeature`, and the only runtime no-op trigger left inside `SmtpEmailService` is a configured-but-blank host. Attachments stream through the `internal` `EmailAttachmentDataSource` bridge: every `getInputStream()` call re-invokes the attachment's `suspend () -> InputStream` provider via a bare `runBlocking` (safe — Jakarta Mail calls it on the `Dispatchers.IO` worker inside `Transport.send`) and must yield a fresh stream, so content is encoded on the fly without whole-payload buffering; `getOutputStream()` throws (read-only source). Blank-host, streaming, and production-wrapper cancellation contracts are unit-tested in `src/commonTest`; the live-SMTP success path is intentionally not unit-tested (external integration — verified via build + the manual `POST /email/sendTest` path).
- **Superadmin guard:** Both `POST /email/sendTest` and `PUT /email/myEmail` use only `getCallerUserIdOrAnswerUnauthorized()` at the routing layer (self-service auth — 401 on missing/invalid bearer token). Superadmin-only enforcement for `sendTest` happens inside `EmailFeatureService.sendTestEmail` by calling `rolesFeature.isFunctionalityAvailable(callerId, sendTestFunctionalityId)` (issue #68) — replaces the previous inline `caller.username.string == "root"` comparison, and also removes the separate `usersRepo.getById` lookup that comparison needed (an unknown `UserId` now resolves to `isFunctionalityAvailable == false` directly, the same net outcome as the old "caller not found" branch). On failure — whether the caller isn't superadmin or the SMTP send itself failed — the route responds `500 Internal Server Error` (the two failure modes are indistinguishable at the HTTP layer, unchanged).
- **Role requirement (issue #68):** this feature owns the `email.sendTest` gate. `EmailConstants.sendTestFunctionalityId` (`email/common`) declares the `FunctionalityId`, and `email/server` `Plugin.setupDI` registers `FeatureRolesRegistry.Requirement(sendTestFunctionalityId, SuperAdminRole)` via `singleRequirement` — per `agents/ARCHITECTURE.md` "Role requirement placement" (a gate's requirement lives in the feature it gates). `email/common` therefore `api`-depends on `roles/common`. The actual enforcement uses `RolesFeature.isFunctionalityAvailable` with the same functionality id, so registration and enforcement both use the same mechanism.
- **Duplicate email → 409:** `EmailVerificationAccountCoordinator.updateStoredEmail` deliberately propagates `DuplicateUserFieldException` from `UsersRepo.update`. `EmailFeatureService` and `DisabledEmailFeature` do not catch it; `PUT /email/myEmail` catches it only at the HTTP boundary and responds `409 Conflict`. Coordinator serialization changes no status or duplicate-field disclosure behavior.
- **Public `GET /enabled`:** Lives outside the `authenticate { }` block so callers without a bearer token can check availability.
- **DI placement:** `EmailVerificationAccountCoordinator` is registered unconditionally with `single` and owns the shared `UsersRepo`/`RolesRepo` concurrency boundary. The conditional `EmailFeature` definition passes that exact singleton to either `EmailFeatureService` or `DisabledEmailFeature`, and the qualified `EmailVerificationDeepLinkHandler` receives the same singleton. Thus SMTP-enabled and SMTP-disabled graphs each contain exactly one coordinator shared by mutation and verification.
- **Client:** `KtorEmailFeature` is transport-only; no service wrapper needed (no memoization). Platform plugins (JS/JVM/Android) delegate to the shared `Plugin`.
- **Registration invites (issue #73):** Every new payload stores the pending user id and exact recipient email; legacy null-email payloads remain decodable but fail closed. Self-service email mutation and equality-check-plus-promotion share one coordinator mutex. If mutation to B linearizes first, verification of A observes the mismatch and leaves `NewUserRole`; if verification linearizes first, promotion completes while A is current and a waiting post-approval edit to B may then succeed. Wrong payload type, missing user, cleared email, and mismatch remain unhandled. This preserves post-approval editing while preventing B from being written between a successful A comparison and promotion. Invite URL validation and false/error/cancellation cleanup remain unchanged.
- Verification promotes only direct `NewUserRole` to direct `UserRole`; it does not issue Auth credentials. The account remains logged out until a normal password login follows promotion.
- **Sample config SMTP block (enabled in `sample.config.json`; omit the whole `"email"` key to disable):**
  ```json
  "email": {
    "smtp": { "host": "smtp.example.com", "port": 587, "username": "user", "password": "pass",
              "from": "noreply@example.com", "useTls": true, "useSsl": false }
  }
  ```
