# Feature: Email

## Operator Notes

<!-- Human operator writes here. Agents MUST read and respect before making any changes. Agents MUST NOT modify this section. -->

## Overview

The email feature provides SMTP-backed transactional email delivery and per-user email address storage. It is structured as a standard full-stack feature with `common`, `server`, and `client` modules.

**Two independent capabilities:**
- **Email storage** (`PUT /email/myEmail`) — any authenticated user can store or clear their own email address; does NOT require SMTP to be configured.
- **Email delivery** (`POST /email/sendTest`) — root-only action to verify SMTP configuration by sending a test message to a supplied address; requires SMTP to be configured.

When no SMTP block is present in the server config (or `smtp` is `null`), the feature operates in no-op mode: `GET /email/enabled` returns `false` and all delivery calls return `false` without attempting a connection.

## Routes

> All paths below are served under the global `/api` prefix. Paths are assembled from constants in `EmailConstants`.

| Method | Path | Auth | Body / Response | Description |
|--------|------|------|-----------------|-------------|
| GET | `/email/enabled` | None | `Boolean` | Returns whether SMTP delivery is configured server-side |
| POST | `/email/sendTest` | Bearer + root only | `TestEmailRequest { recipient: Email }` / `200 OK` or `500` | Sends a test message to `recipient` via configured SMTP |
| PUT | `/email/myEmail` | Bearer (self) | `SetEmailRequest { email: Email? }` / `200 OK` | Stores or clears the authenticated caller's own email address |

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
| `EmailConfig` | `email/server` | Config slice decoded from root server JSON: `smtp: SmtpConfig? = null`. |
| `SmtpConfig` | `email/server` | SMTP settings: `host`, `port` (587), `username?`, `password?`, `from: Email`, `useTls` (true), `useSsl` (false). |
| `SmtpEmailService` | `email/server` | SMTP transport; implements `EmailsService`. Builds `jakarta.mail.Session` from `SmtpConfig` and calls `Transport.send` on `Dispatchers.IO`. Returns `false` when disabled or on error. Wrapped by `EmailFeatureService` for the `EmailFeature` surface. |
| `EmailFeatureService` | `email/server` | Server `EmailFeature` impl; wraps `SmtpEmailService` + `UsersRepo`: enforces root-only access for `sendTestEmail` and persists the caller's email address for `setMyEmail`. |
| `EmailsService` | `email/server` | Server-only send interface (no HTTP exposure): `sendText(recipient, subject, text)`, `sendTextWithAttachments(recipient, subject, text, attachments)`, `sendHtml(recipient, subject, html)` — all `suspend`, all return `Boolean` (`false` when SMTP is disabled or on error). Implemented by `SmtpEmailService`; bound in Koin. |
| `EmailAttachment` | `email/server` | Attachment model for `sendTextWithAttachments`: `fileName`, `mimeType` (default `application/octet-stream`), `content: suspend () -> InputStream`. The provider may be invoked multiple times and must return a fresh stream on each call; content is streamed, never buffered as a whole `ByteArray`. |
| `KtorEmailFeature` | `email/client` | Client `EmailFeature` impl; HTTP-only, no caching or business logic. |

## Architecture Notes

- **`Email` ownership:** The `Email` value class lives in `features/email/common` and is the single source of truth. `features/users/common` declares `api project(":wishlist.features.email.common")` — this is legal per the existing `auth/common → users/common` precedent (a feature's `*/common` may depend on another feature's `*/common`; only the literal `features/common/*` base modules are restricted).
- **Config-slice pattern:** `EmailConfig` is decoded via `get<Json>().decodeFromJsonElement(EmailConfig.serializer(), config)` in `email/server/Plugin.kt` — identical to `CurrencyConfig`. The `smtp` key lives at the JSON root object alongside `openExchangeRatesAppId`. Set `"smtp": null` (or omit) to disable.
- **Storage vs sending:** Per-user email storage (`PUT /email/myEmail`) is independent of SMTP — it always works. Delivery (`POST /email/sendTest`) requires SMTP configured. This separation is intentional.
- **Jakarta Mail (Angus):** `org.eclipse.angus:angus-mail:2.0.3` is consumed only in `features/email/server` (JVM-only module via `mppJavaProject`). Jakarta Mail imports are safe in `commonMain` of that module. Blocking `Transport.send` is wrapped in `withContext(Dispatchers.IO)`.
- **`EmailsService` (server-only send surface):** `EmailsService` (`sendText` / `sendTextWithAttachments` / `sendHtml`, each with an explicit `subject`) is an internal server capability implemented by `SmtpEmailService` and bound in Koin (`single<EmailsService> { get<SmtpEmailService>() }`). It is deliberately NOT wired into `EmailFeature`, `KtorEmailFeature`, or any HTTP route. All four send paths (including `sendTestEmail`) share one private `send(...)` skeleton (disabled-mode check, session, envelope, `Transport.send` on `Dispatchers.IO`, warn-log on failure). Attachments stream through the `internal` `EmailAttachmentDataSource` bridge: every `getInputStream()` call re-invokes the attachment's `suspend () -> InputStream` provider via a bare `runBlocking` (safe — Jakarta Mail calls it on the `Dispatchers.IO` worker inside `Transport.send`) and must yield a fresh stream, so content is encoded on the fly without whole-payload buffering; `getOutputStream()` throws (read-only source). Disabled-mode and streaming contracts are unit-tested in `src/commonTest`; the live-SMTP success path is intentionally not unit-tested (external integration — verified via build + the manual `POST /email/sendTest` path).
- **Root guard:** `POST /email/sendTest` uses `requireRoot()` — mirrors `AdminRoutingsConfigurator.requireAdmin()`. Responds 401 (no token) or 403 (non-root). `PUT /email/myEmail` uses only `getCallerUserIdOrAnswerUnauthorized()` (self-service).
- **Public `GET /enabled`:** Lives outside the `authenticate { }` block so callers without a bearer token can check availability.
- **DI placement:** Server Plugin wires `EmailConfig → SmtpEmailService → EmailFeature` (via `EmailFeatureService`), additionally binds `SmtpEmailService` under `EmailsService`, and registers `EmailRoutingsConfigurator` (with random qualifier). `jvmMain/JVMPlugin` is a thin delegator listed in `sample.config.json`.
- **Client:** `KtorEmailFeature` is transport-only; no service wrapper needed (no memoization). Platform plugins (JS/JVM/Android) delegate to the shared `Plugin`.
- **Sample config SMTP block (disabled by default):**
  ```json
  "smtp": null
  ```
  To enable, replace with:
  ```json
  "smtp": { "host": "smtp.example.com", "port": 587, "username": "user", "password": "pass",
            "from": "noreply@example.com", "useTls": true, "useSsl": false }
  ```
