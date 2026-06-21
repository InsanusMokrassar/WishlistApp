# Task: GitHub issue #44 — Email feature

Branch: `fix/44-email` (parent handles all git/PR; orchestrator + roles only modify working-tree files).

## Issue body (verbatim)

Add base email feature. First its iteration must contain:
1. Feature
    * Server side must include config for smtp sendings
    * Client side must include opportunity for root to send test email
2. Add optional (for now) opportunity to setup user email

Email must be value class. This value class must contains all required checks of incoming email string

## Context

- Prior PR #49 (branch `issue/44-email`) attempted this and was CLOSED by the operator WITHOUT comments (silent rejection). Its source code is NOT present on branch `fix/44-email` (verified: `features/email/` has no tracked sources, no wiring in settings.gradle / server build.gradle / sample.config.json). Treat as a FRESH max-effort implementation. Prior work may be inspected for reference only via `git show origin/issue/44-email:<path>`, NOT blindly copied.
- There is also a local-only branch `issue/44-email-redo` (not the email work — it holds agents-doc commits).

## Spec requirements (ALL must be satisfied)

1. New email feature module wired exactly like existing full-stack features (`features/email/{common,server,client}`), registered in `settings.gradle`, server/client `build.gradle`, `sample.config.json`, and all three client entry points.
2. Server side: SMTP config decoded from the server config JSON (same `single { get<Json>().decodeFromJsonElement(EmailConfig.serializer(), config) }` slice pattern as `CurrencyConfig`). Disabled / no-op when SMTP block absent.
3. Client side: a root-only capability to send a test email (mirror the `admin` feature's `requireAdmin()` root check: bearer auth + `username == "root"`). Surface it in the `features/ui/adminPanel` dashboard.
4. Optional per-user email: add an optional (nullable) `Email` to the user model/storage. Existing DB rows must remain valid — nullable / back-compatible column migration (Exposed `createMissingTablesAndColumns` adds a nullable column).
5. `Email` must be a `@JvmInline value class` containing ALL required validation of the incoming email string (construction validates; provide a non-throwing parse path too, matching project conventions — e.g. `Email(value)` throwing + `Email.parse(value): Result<Email>`).

## Definition of done

- All spec requirements implemented and wired the same way existing features are.
- Affected/new module(s), the server, and the client targets (js/jvm/android) BUILD successfully (`./gradlew ... build` → BUILD SUCCESSFUL).
- New `features/email/README.md` complete per the required structure in `agents/ALL.md` (incl. empty `## Operator Notes`).
- Validation role confirms correctness.

## Established conventions to match (verified by orchestrator)

- Config-slice decode: `CurrencyConfig` (`features/currency/server`).
- Root-only protection: `AdminRoutingsConfigurator.requireAdmin()` (`username == "root"` after `authenticate { }`) + `getCallerUserIdOrAnswerUnauthorized()` (`features/auth/server/utils`).
- Value class + serializer: `Username`, `auth` `Token`/`Password`; prior `Email.kt` used a private-ctor `@JvmInline value class` + `EmailSerializer : KSerializer<Email>` (primitive STRING, re-validates on decode).
- User model: `features/users/common/.../models/User.kt` (sealed `User`, `NewUser`, `RegisteredUser`); `ExposedUsersRepo` (`features/users/common/jvmMain`).
- Client feature/Ktor pattern: `KtorUsersManagementFeature` (`features/admin/client`).
- adminPanel UI: `AdminPanelView`/`AdminPanelViewModel`/`AdminPanelModel`/`AdminPanelStrings`/`Plugin` + per-platform views; root-only test-email action belongs on the dashboard.
- Feature scaffold: `./generate_feature.sh`. SMTP dependency to add in `gradle/libs.versions.toml`.
