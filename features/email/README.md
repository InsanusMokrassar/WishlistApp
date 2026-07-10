# Feature: Email

## Operator Notes

<!-- Human operator writes here. Agents MUST read and respect before making any changes. Agents MUST NOT modify this section. -->

## Overview

The email feature provides SMTP-backed transactional email delivery and per-user email address storage. It is structured as a standard full-stack feature with `common`, `server`, and `client` modules.

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
| `SmtpEmailService` | `email/server` | SMTP transport; implements `EmailsService`. Builds `jakarta.mail.Session` from `SmtpConfig` and calls `Transport.send` on `Dispatchers.IO`. Returns `false` when the configured host is blank or on error. Only ever constructed by `Plugin` when the `"email"` key is present and non-null. Wrapped by `EmailFeatureService` for the `EmailFeature` surface. |
| `EmailFeatureService` | `email/server` | Server `EmailFeature` impl; wraps a non-nullable `EmailsService` + `UsersRepo` + `SimpleRolesFeature`. `isFeatureEnabled()` always returns `true` — this class is only ever constructed by `Plugin` when a real `EmailsService` exists; `sendTestEmail` enforces a SuperAdmin-only check (via `simpleRoles.server`'s `SimpleRolesFeature`) before delegating; `setMyEmail` persists the caller's email address. See `DisabledEmailFeature` for the substituted no-op implementation used when SMTP is not configured. |
| `DisabledEmailFeature` | `email/server` | No-op `EmailFeature` substituted in DI (`Plugin.kt`) whenever no `EmailsService` is registered (SMTP unconfigured): `isFeatureEnabled`/`sendTestEmail` return `false`; `setMyEmail` still persists via `UsersRepo` (storage stays independent of SMTP, per the feature's own architecture rule). |
| `EmailsService` | `email/server` | Server-only send interface (no HTTP exposure): `sendText(recipient, subject, text)`, `sendTextWithAttachments(recipient, subject, text, attachments)`, `sendHtml(recipient, subject, html)` — all `suspend`, all return `Boolean` (`false` when SMTP is disabled or on error). Implemented by `SmtpEmailService`; bound in Koin only when SMTP is configured. |
| `EmailAttachment` | `email/server` | Attachment model for `sendTextWithAttachments`: `fileName`, `mimeType` (default `application/octet-stream`), `content: suspend () -> InputStream`. The provider may be invoked multiple times and must return a fresh stream on each call; content is streamed, never buffered as a whole `ByteArray`. |
| `KtorEmailFeature` | `email/client` | Client `EmailFeature` impl; HTTP-only, no caching or business logic. |

## Architecture Notes

- **`Email` ownership:** The `Email` value class lives in `features/email/common` and is the single source of truth. `features/users/common` declares `api project(":wishlist.features.email.common")` — this is legal per the existing `auth/common → users/common` precedent (a feature's `*/common` may depend on another feature's `*/common`; only the literal `features/common/*` base modules are restricted).
- **Config-slice pattern (nested key):** `EmailConfig` is decoded via `get<Json>().decodeFromJsonElement(EmailConfig.serializer(), config["email"])` in `email/server/Plugin.kt` — a **nested-key** pattern, deliberately different from `CurrencyConfig`'s root-flat-key pattern (`CurrencyConfig` decodes from the whole root object). This is the first feature in the codebase to gate `single` registrations on JSON-key presence (a DI-graph-shape "disabled" state) rather than handling a null/absent value at runtime. Omitting the `"email"` key entirely (not setting a nested `"smtp"` to `null` within it) is the documented way to disable the feature.
- **DI-graph-shape "disabled" state:** `EmailConfig`, `SmtpEmailService`, and the `EmailsService` binding are registered together, conditionally, only when `config["email"]` is present and non-null — implemented via the pure, Koin-free helper `emailConfigElementOrNull(config)` in `Plugin.kt` (unit-tested directly in `PluginTest`, with no Koin test harness needed since none exists in this repo). `EmailFeature` is always registered unconditionally; it resolves `getOrNull<EmailsService>()` directly inside an inline `single<EmailFeature>` block and picks `EmailFeatureService` (present) or `DisabledEmailFeature` (absent).
- **Storage vs sending:** Per-user email storage (`PUT /email/myEmail`) is independent of SMTP — it always works, even when `EmailFeature` resolves to `DisabledEmailFeature`. Delivery (`POST /email/sendTest`) requires SMTP configured. `EmailFeatureService.setMyEmail` and `DisabledEmailFeature.setMyEmail` both delegate to a shared `internal` `updateStoredEmail(usersRepo, callerId, email)` helper, so this independence guarantee is implemented in exactly one place and cannot regress even when the `EmailFeature` implementation itself changes based on SMTP availability.
- **Jakarta Mail (Angus):** `org.eclipse.angus:angus-mail:2.0.3` is consumed only in `features/email/server` (JVM-only module via `mppJavaProject`). Jakarta Mail imports are safe in `commonMain` of that module. Blocking `Transport.send` is wrapped in `withContext(Dispatchers.IO)`.
- **`EmailsService` (server-only send surface):** `EmailsService` (`sendText` / `sendTextWithAttachments` / `sendHtml`, each with an explicit `subject`) is an internal server capability implemented by `SmtpEmailService` and bound in Koin (`single<EmailsService> { get<SmtpEmailService>() }`, only when SMTP is configured). It is deliberately NOT wired into `EmailFeature`, `KtorEmailFeature`, or any HTTP route. All three send paths share one private `send(...)` skeleton (blank-host check, session, envelope, `Transport.send` on `Dispatchers.IO`, warn-log on failure) — `SmtpEmailService` no longer exposes an `isFeatureEnabled()` method; "not configured at all" is now a DI-graph-shape fact handled by `DisabledEmailFeature`, and the only runtime no-op trigger left inside `SmtpEmailService` is a configured-but-blank host. Attachments stream through the `internal` `EmailAttachmentDataSource` bridge: every `getInputStream()` call re-invokes the attachment's `suspend () -> InputStream` provider via a bare `runBlocking` (safe — Jakarta Mail calls it on the `Dispatchers.IO` worker inside `Transport.send`) and must yield a fresh stream, so content is encoded on the fly without whole-payload buffering; `getOutputStream()` throws (read-only source). Blank-host and streaming contracts are unit-tested in `src/commonTest`; the live-SMTP success path is intentionally not unit-tested (external integration — verified via build + the manual `POST /email/sendTest` path).
- **Superadmin guard:** Both `POST /email/sendTest` and `PUT /email/myEmail` use only `getCallerUserIdOrAnswerUnauthorized()` at the routing layer (self-service auth — 401 on missing/invalid bearer token). Superadmin-only enforcement for `sendTest` happens inside `EmailFeatureService.sendTestEmail` by calling `simpleRoles.server`'s `SimpleRolesFeature.isSuperAdmin(callerId)` (issue #68) — replaces the previous inline `caller.username.string == "root"` comparison, and also removes the separate `usersRepo.getById` lookup that comparison needed (an unknown `UserId` now resolves to `isSuperAdmin == false` directly, the same net outcome as the old "caller not found" branch). On failure — whether the caller isn't superadmin or the SMTP send itself failed — the route responds `500 Internal Server Error` (the two failure modes are indistinguishable at the HTTP layer, unchanged).
- **Duplicate email → 409:** `PUT /email/myEmail`'s handler wraps only the `feature.setMyEmail(...)` call in a `try`/`catch (e: DuplicateUserFieldException)`, responding `409 Conflict` and returning before the `when { updated -> ... }` block runs. The exception originates in `ExposedUsersRepo.update` (see `features/users/README.md`'s "Duplicate-key-to-409 convention") and propagates unchanged through `updateStoredEmail`/`EmailFeatureService.setMyEmail`/`DisabledEmailFeature.setMyEmail` — none of those three add a `try`/`catch` of their own, by design; only the HTTP boundary (`EmailRoutingsConfigurator`) does. `sendTest`'s handler is unaffected — it writes no unique-constrained column.
- **Public `GET /enabled`:** Lives outside the `authenticate { }` block so callers without a bearer token can check availability.
- **DI placement:** Server Plugin wires `EmailConfig → SmtpEmailService → EmailsService` as one conditional trio (see above), always registers `EmailFeature` (`EmailFeatureService` or `DisabledEmailFeature`, selected inline by a `getOrNull<EmailsService>()` check in the `single<EmailFeature>` block), and registers `EmailRoutingsConfigurator` (with random qualifier) unconditionally. `jvmMain/JVMPlugin` is a thin delegator listed in `sample.config.json`. The `"email"`-key-presence check is implemented as a small `internal` pure function (`emailConfigElementOrNull`) in `Plugin.kt` specifically so it can be unit-tested (`PluginTest.kt`) without a Koin test harness — this repo has none. The `EmailFeature`-implementation choice (`getOrNull<EmailsService>()?.let { EmailFeatureService(it, get(), get()) } ?: DisabledEmailFeature(get())`) is simple enough that it stays inline rather than being extracted to its own testable helper.
- **Client:** `KtorEmailFeature` is transport-only; no service wrapper needed (no memoization). Platform plugins (JS/JVM/Android) delegate to the shared `Plugin`.
- **Sample config SMTP block (enabled in `sample.config.json`; omit the whole `"email"` key to disable):**
  ```json
  "email": {
    "smtp": { "host": "smtp.example.com", "port": 587, "username": "user", "password": "pass",
              "from": "noreply@example.com", "useTls": true, "useSsl": false }
  }
  ```
