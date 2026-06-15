# Feature: Email

## Operator Notes

<!-- Human operator writes here. Agents MUST read and respect before making any changes. Agents MUST NOT modify this section. -->

## Overview

The email feature provides server-side SMTP sending and a root-only "send test email" action, plus a
shared, self-validating `Email` value class. It also adds an optional e-mail address to users.

First iteration scope:

- Server: SMTP configuration decoded from the root server config, an `EmailFeature.sendEmail(to, subject, body)`
  backed by Jakarta Mail (Angus Mail), and two routes.
- Client: `EmailFeature` (Ktor) with `sendTestEmail` / `setMyEmail`, surfaced as a root-only test-email
  action in the admin panel UI.
- Common: `Email` `@JvmInline value class` with RFC-ish validation, shared by all sides.
- Users: optional (nullable) `email` stored on the user model/table.

### Modules

| Module | Path | Purpose |
|---|---|---|
| `wishlist.features.email.common` | `features/email/common` | `Email` value class, request DTOs, path constants |
| `wishlist.features.email.server` | `features/email/server` | SMTP config, `EmailFeature` + Jakarta Mail service, routing |
| `wishlist.features.email.client` | `features/email/client` | `EmailFeature` interface + Ktor implementation |

## Routes

All routes are under `/email` and require bearer authentication.

| Method | Path | Auth | Body / Response | Description |
|--------|------|------|-----------------|-------------|
| POST | `/email/sendTest` | root only (403 otherwise) | `TestEmailRequest` / `200`,`500` | Send a fixed test message to the address in the body |
| PUT | `/email/myEmail` | any authenticated user | `SetEmailRequest` / `200`,`404`,`500` | Set or clear the caller's own stored e-mail |

## Models

- `Email` (`email.common.models`) — `@Serializable(EmailSerializer) @JvmInline value class Email private constructor(val raw)`.
  Construction only via companion: `Email(value)` (throws `IllegalArgumentException` on invalid),
  `Email.parse(value): Result<Email>` (non-throwing), `Email.isValid(value): Boolean`. Validation: trims,
  rejects blank, enforces max length 254, matches a basic RFC-ish regex (`local@domain.tld`). Serialized as a
  bare JSON string; decoding re-validates.
- `TestEmailRequest(to: Email)` — POST body for `/email/sendTest`.
- `SetEmailRequest(email: Email?)` — PUT body for `/email/myEmail`; caller identified by token.
- `EmailConfig(smtp: SmtpConfig?)` / `SmtpConfig(host, port, username?, password?, from: Email, useTls, useSsl)` —
  server config slice; `null` smtp (or blank host) disables sending.

User model change (`users.common.models.User`): added `val email: Email?` (default `null`) to `User`,
`NewUser`, `RegisteredUser`. Persisted by `ExposedUsersRepo` as a nullable `email` text column.

## Architecture Notes

### Server side

- `EmailConfig` is decoded from the root server config JSON (mirrors `CurrencyConfig`) — no shared `Config`
  type change. `smtp: null` (the sample default) makes the feature a no-op.
- `JavaMailEmailService` implements `EmailFeature` using Jakarta Mail (`jakarta.mail.*`). Runtime dependency:
  `org.eclipse.angus:angus-mail` (version catalog `angus-mail` / `angusMail = 2.0.3`), added to
  `features/email/server/build.gradle`. The server module is JVM-only (`mppJavaProject`), so the
  `jakarta.mail` imports live in `commonMain` safely. Blocking send runs on `Dispatchers.IO`. Disabled config
  -> logs a warning and returns `false`.
- `EmailRoutingsConfigurator` registers both routes under `authenticate { }`. `sendTest` is root-gated via a
  `requireRoot()` helper that checks `usersRepo.getById(callerId).username == "root"` (same gating shape as the
  admin feature; `admin.server` is intentionally NOT depended on — only `auth.server` for the caller-id util
  and `users.common` for the repo). `myEmail` updates the caller's own user record via `UsersRepo.update`.
- `Plugin` decodes the config, binds `JavaMailEmailService` as `EmailFeature`, and registers the routing
  element. `JVMPlugin` delegates to `email.common.JVMPlugin` + `Plugin`.
- Register in `server/sample.config.json` plugins list (after users/auth):
  `"dev.inmo.wishlist.features.email.server.JVMPlugin"`. SMTP block: top-level `"smtp": { host, port, ... }`
  (or `null`).

### Client side

- `EmailFeature` interface: `sendTestEmail(to)`, `setMyEmail(email)`. `KtorEmailFeature` is HTTP-only (POST
  `/email/sendTest`, PUT `/email/myEmail`) per the Ktor-realization rule. Bound in the common `Plugin`.
- Platform plugins delegate to `email.common.<platform>Plugin` then `Plugin`.

### UI wiring (admin panel)

- `features/ui/adminPanel` depends on `email.client`. `AdminPanelModel.sendTestEmail(to: String)` parses the
  string into `Email` (returns `false` on invalid) and delegates to `EmailFeature`. The model impl in
  `adminPanel/Plugin.kt` injects `EmailFeature`.
- `AdminPanelViewModel` adds `testEmailAddressState`, `sendingTestEmailState`, `testEmailResultState`, and
  `onTestEmailAddressChanged` / `onSendTestEmail`. The dashboard view (JS/JVM/Android) renders an address
  input + send button + result text. The admin panel is already reachable only by `root` (auth overlay).

### Dependency notes

- `users.common` now depends on `email.common` (for the `Email` type). `email.common` depends only on
  `common.common` — no dependency cycle (the test-email/set-email DTOs use only `Email`, never `UserId`).
