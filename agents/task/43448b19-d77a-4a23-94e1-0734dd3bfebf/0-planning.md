Model: opus (claude-opus-4-8)
Execution time: 480
Tokens used: ~95000
Changed files: agents/task/43448b19-d77a-4a23-94e1-0734dd3bfebf/0-planning.md

# Step 0 — PLANNING — GitHub issue #44 (Email feature)

## Model argumentation

PLAN role model priority (ALL.md / SHORTCUTS.md) = `fable` / `opus` / `sonnet`.
`fable` unavailable in this run → next priority `opus` selected. Justification recorded per ALL.md header rule.

## Protocol note

AGENTS.md mandates "AML-HIP" prose style; ALL.md mandates `/caveman full` and DISABLES auto-memory. The orchestrator PROMPT.md for this role explicitly requests a "thorough, code-free PLAN" in normal structured Markdown and a concise summary. PROMPT.md (the direct role task) is treated as authoritative for the deliverable format; auto-memory ignored per ALL.md. No code edited; only this single step file written, per PLAN.md hard constraint.

---

# 1. CRITICAL DESIGN DECISION — `Email` value class placement & dependency direction

## 1.1 The rule, precisely read

`agents/ARCHITECTURE.md` → "Dependency Graph Between Modules":
> "Each feature may depend on any other one. **Modules of `features/common` CANNOT depend on any other internal module.**"

Interpretation question: does "Modules of `features/common`" mean (A) the literal directory `features/common/*` (`features/common/common`, `features/common/server`, `features/common/client`), or (B) every `*/common` submodule of every feature?

## 1.2 Evidence — interpretation (A) is correct

Verified against the real build graph:

- `features/auth/common/build.gradle` declares `api project(":wishlist.features.users.common")`.
  → An `auth/common` module already depends on another feature's `users/common`. If interpretation (B) were the rule, this existing, shipped module would violate it. Therefore the rule applies ONLY to the literal `features/common/*` directory (interpretation A).
- `features/users/common/build.gradle` depends on `:wishlist.features.common.common` (the allowed base module) and nothing else.
- `features/currency/common/build.gradle` likewise depends only on `:wishlist.features.common.common`.

**Conclusion:** A feature's `*/common` module MAY depend on another feature's `*/common` module. Only `features/common/common`, `features/common/server`, `features/common/client` are forbidden from depending on other internal modules.

## 1.3 Resolution (RECOMMENDED)

- Place the `Email` `@JvmInline value class` in **`features/email/common`** (`dev.inmo.wishlist.features.email.common.models.Email`). This is the natural owner — the email feature owns the email type.
- Add to **`features/users/common/build.gradle`**: `api project(":wishlist.features.email.common")`. This is ALLOWED (proven by the auth→users precedent) and gives the User model access to `Email`.
- The nullable per-user email column + model field live in `features/users` (the owner of `User` + `ExposedUsersRepo`), referencing `Email` from `features/email/common`.

This keeps the `Email` type single-sourced (no duplication), respects the dependency rule, and matches the existing `auth/common → users/common` precedent exactly.

### Rejected alternative
Placing `Email` in `features/common/common` (the shared base) was considered as a "neutral" location. REJECTED because: (a) it is unnecessary — the users→email dependency is legal; (b) it pollutes the shared base module with a feature-specific type; (c) it diverges from the prior PR #49 layout which (per `git show origin/issue/44-email:.../models/Email.kt`) placed `Email` in `features/email/common`. The prior placement was not the reason for the silent rejection (no wiring/route differences are visible as the rejection cause). Keep `Email` in `features/email/common`.

### Dependency-cycle check
`features/email/common` will depend only on `:wishlist.features.common.common` (base). `features/users/common` will depend on `:wishlist.features.common.common` + `:wishlist.features.email.common`. No cycle: email/common does NOT depend on users/common. The email SERVER feature (for the per-user-email self-service route + test-email) may depend on `users` server/common as needed; that is a feature→feature server dep and is allowed.

---

# 2. SMTP library choice

## 2.1 Findings

- `gradle/libs.versions.toml` currently has NO mail/SMTP/jakarta/angus entry (grep confirmed).
- The server module template is `mppJavaProject` (`defaultProject` + JVM only) — a JVM-only mail library is fully acceptable.
- Prior PR #49 used `org.eclipse.angus:angus-mail` (Jakarta Mail implementation).

## 2.2 Recommendation

Add **Jakarta Mail (Angus Mail implementation)** to the version catalog:

- `[versions]`: `angus-mail = "2.0.3"` (latest stable Angus Mail 2.x line; CODING role MUST confirm the exact newest stable version at implementation time — do not hardcode blindly).
- `[libraries]`: `angus-mail = { module = "org.eclipse.angus:angus-mail", version.ref = "angus-mail" }`.
- Consume ONLY in `features/email/server` (JVM-only). `org.eclipse.angus:angus-mail` transitively provides the `jakarta.mail` API, so a single dependency suffices; if the API artifact is needed explicitly, add `jakarta-mail-api = { module = "jakarta.mail:jakarta.mail-api", version.ref = ... }` as well.

Rationale: matches prior art, is the de-facto standard JVM SMTP stack, actively maintained, and confined to the JVM-only server module so it does not affect KMP common/client targets.

**Open confirmation for CODING role:** verify the newest stable `angus-mail` coordinate/version against the project's repositories before pinning. Default fallback if unsure: `2.0.3`.

---

# 3. Module layout (the new `features/email` feature)

Scaffold with `./generate_feature.sh` (enter name `email`) → creates `features/email/{common,server,client}` with stub `build.gradle` + `Plugin.kt` per platform (`common`, `server`, `client`, each with JVM/JS/Android delegating plugins where applicable). Then customize.

## 3.1 `features/email/common`  (template `mppJvmJsAndroid`; deps: `:wishlist.features.common.common`)

| File | Purpose |
|---|---|
| `models/Email.kt` | `@JvmInline value class Email private constructor(val raw: String)` + companion `invoke(String): Email` (validates, throws `IllegalArgumentException`), `parse(String): Result<Email>` (non-throwing), `isValid(String): Boolean`; `@Serializable(with = EmailSerializer::class)`; `object EmailSerializer : KSerializer<Email>` PrimitiveKind.STRING, re-validates on decode. (Mirror `git show origin/issue/44-email:features/email/common/src/commonMain/kotlin/models/Email.kt` — see §6.) |
| `EmailConstants.kt` | Shared path parts: `prefixPathPart = "email"`, `sendTestPathPart = "sendTest"`, `enabledPathPart = "enabled"`, `myEmailPathPart = "myEmail"` (self-service). One source of truth shared by server routes + client. |
| `EmailFeature.kt` | `interface EmailFeature` capability: `suspend fun isFeatureEnabled(): Boolean`; `suspend fun sendTestEmail(target: Email): Boolean` (root-only on server side); optionally `suspend fun setMyEmail(email: Email?): Boolean` for self-service. No-op semantics when disabled (mirror `CurrencyFeature` doc contract: disabled → `isFeatureEnabled()==false`, `sendTestEmail` returns false). |
| models for request bodies | e.g. `SendTestEmailRequest(target: Email)` `@Serializable` if a body wrapper is preferred over raw `Email`. CODING may send raw `Email` (serializes as string) to keep it minimal. |
| `Plugin.kt` + `jvmMain/JVMPlugin.kt`, `jsMain/JSPlugin.kt`, `androidMain/AndroidPlugin.kt` | Delegating stubs (mirror `currency/common`). `common/Plugin` may be empty (no DI needed in common). |

## 3.2 `features/email/server`  (template `mppJavaProject`; deps: `:wishlist.features.email.common`, `:wishlist.features.common.server`, `:wishlist.features.auth.server` (for root check util), `:wishlist.features.users.common` (UsersRepo for per-user email self-service), `angus-mail`)

| File | Purpose |
|---|---|
| `EmailConfig.kt` (`commonMain`) | `@Serializable data class EmailConfig(val smtp: SmtpConfig? = null)` (or flat nullable fields) — slice-decoded from root config JSON exactly like `CurrencyConfig`. `null`/absent `smtp` block ⇒ feature disabled / no-op. `SmtpConfig(host, port, username, password, from, useTls/useStartTls, ...)`. |
| `services/SmtpEmailService.kt` (`jvmMain` — uses Jakarta Mail) | Implements `EmailFeature`. Constructed with `EmailConfig`. `isFeatureEnabled()` = `config.smtp != null && host non-blank`. `sendTestEmail(target)` builds a `jakarta.mail.Session` + `MimeMessage` and `Transport.send(...)` **wrapped in `withContext(Dispatchers.IO)`** (blocking SMTP off the event loop). Returns `false` (and logs) when disabled or on exception. JVM-only ⇒ Jakarta Mail imports live safely here. |
| `configurators/EmailRoutingsConfigurator.kt` (`commonMain`, Ktor) | Registers routes under `EmailConstants.prefixPathPart` (`/email`, auto-prefixed to `/api/email`). See §4 for the exact routes + root guard. `ApplicationRoutingConfigurator.Element`, registered via `singleWithRandomQualifier<...Element>` in server `Plugin` (mirror `CurrencyRoutingsConfigurator`). |
| `Plugin.kt` (`commonMain`) | `single { get<Json>().decodeFromJsonElement(EmailConfig.serializer(), config) }`; `single { SmtpEmailService(get<EmailConfig>()) }` (in `jvmMain` if it references Jakarta types — keep the binding split like currency keeps OkHttp in common because module is JVM-only; here the SERVICE is in jvmMain, so its `single { }` binding must be in `jvmMain/JVMPlugin` OR the service kept JVM-actual). RECOMMEND: put SmtpEmailService construction + `single<EmailFeature> { get<SmtpEmailService>() }` in `jvmMain/JVMPlugin` to avoid Jakarta refs in `commonMain`. Config decode + routing configurator registration can live in `commonMain/Plugin`. `singleWithRandomQualifier<ApplicationRoutingConfigurator.Element> { EmailRoutingsConfigurator(get(), get<UsersRepo>()) }`. |
| `jvmMain/JVMPlugin.kt` | Listed in `sample.config.json` `plugins`. Delegates: `with(common.JVMPlugin){setupDI}`, `with(Plugin){setupDI}`, and registers the JVM SmtpEmailService + EmailFeature binding. |

NOTE on currency-vs-email DI placement: `currency/server` is also a `mppJavaProject` and put OkHttp refs in `commonMain` because the module compiles only JVM. The same is technically possible for Jakarta Mail. However, to keep the Email value-class/common code platform-clean and mirror the cleanest separation, CODING may keep Jakarta Mail strictly in `jvmMain`. Either is acceptable; document the chosen split in README Architecture Notes.

## 3.3 `features/email/client`  (template `mppJvmJsAndroidWithCompose` per scaffold, BUT no Compose needed → may stay `mppJvmJsAndroid`; deps: `:wishlist.features.email.common`)

| File | Purpose |
|---|---|
| `KtorEmailFeature.kt` (`commonMain`) | HTTP-only `EmailFeature` over the shared `HttpClient` (resolved via `get()` from `features/common/client`). Mirrors `KtorCurrencyFeature`: `isFeatureEnabled()` → `GET email/enabled`; `sendTestEmail(target)` → `POST email/sendTest` with body = `target` (`Email` serializes to string) ⇒ returns `response.status.isSuccess()`; `setMyEmail(email)` → `PUT email/myEmail`. No caching/state/business logic (project Ktor-realization rule). |
| `Plugin.kt` (`commonMain`) | `single { KtorEmailFeature(get()) }`; `single<EmailFeature> { get<KtorEmailFeature>() }`. (Mirror currency client Plugin; no extra service wrapper needed unless memoization desired — NOT needed here.) |
| `jvmMain/JVMPlugin.kt`, `jsMain/JSPlugin.kt`, `androidMain/AndroidPlugin.kt` | Delegating stubs (mirror `currency/client`). |

---

# 4. Server routes & root-only guard

`EmailRoutingsConfigurator : ApplicationRoutingConfigurator.Element`, mounted at `route(EmailConstants.prefixPathPart)`:

| Method | Path | Auth | Body | Behaviour |
|---|---|---|---|---|
| GET | `/email/enabled` | public | — | `call.respond(feature.isFeatureEnabled())` (Boolean). Mirrors currency `enabled`. |
| POST | `/email/sendTest` | bearer + ROOT only | `Email` (string) | Root guard then `feature.sendTestEmail(target)` → 200 / 500. |
| PUT | `/email/myEmail` | bearer (authenticated self) | `Email?` (nullable string; allow clearing) | Sets the caller's own user email via `ExposedUsersRepo`/`UsersRepo`. Self-service, NOT root-only. |

## 4.1 Root guard (mirror `AdminRoutingsConfigurator.requireAdmin`)

Inside `authenticate { route(prefix) { ... } }`:
```
private suspend fun RoutingContext.requireRoot(): UserId? {
    val callerId = getCallerUserIdOrAnswerUnauthorized() ?: return null   // 401 if no principal
    val user = usersRepo.getById(callerId)
    if (user == null || user.username.string != "root") { call.respond(Forbidden); return null }
    return callerId
}
```
`getCallerUserIdOrAnswerUnauthorized()` imported from `dev.inmo.wishlist.features.auth.server.utils`. The `sendTest` handler calls `requireRoot() ?: return@post` before sending. This requires the server module to depend on `features/auth/server` (for the util) and `features/users/common` (UsersRepo + Username). The `/myEmail` handler uses only `getCallerUserIdOrAnswerUnauthorized()` (no root check).

## 4.2 Route ownership decision (RECOMMENDED: email feature owns its routes)

The email feature owns BOTH `/email/sendTest` (root) and `/email/myEmail` (self). It does NOT reuse `/admin`.
Justification:
- Cohesion: all email endpoints live in one feature; the path prefix `/email` is self-describing.
- The `admin` feature is specifically a CRUD surface over users/wishlists; bolting email onto it couples unrelated concerns.
- The adminPanel UI (a `features/ui` module) calls the **email client feature** (`EmailFeature` from `features/email/client`) — exactly as it already calls `AdminFeature`. The UI layer composes multiple client features; it does not require the routes to live under `/admin`.
- Self-service `/myEmail` clearly does not belong under `/admin` (it is not root-only), reinforcing that email should own its own routes.

---

# 5. Per-user optional Email (User model + ExposedUsersRepo)

## 5.1 Model change — `features/users/common/.../models/User.kt`

Add a nullable `email: Email?` (default `null`) to the `User` surface:
- `sealed interface User` gains `val email: Email?` (with a default cannot be on interface; provide in implementors).
- `NewUser(username, email: Email? = null)` — back-compatible default.
- `RegisteredUser(id, username, email: Email? = null)` — back-compatible default.
- Import `Email` from `dev.inmo.wishlist.features.email.common.models.Email`. Requires `features/users/common/build.gradle` to add `api project(":wishlist.features.email.common")` (see §1.3 — ALLOWED).

Back-compat for serialization: defaulting `email = null` means existing JSON payloads (without `email`) still decode. `Email` serializes as a plain string, so a present value round-trips. Confirmed by reading `Username`/`UserId` patterns + `EmailSerializer`.

## 5.2 Storage — `features/users/common/jvmMain/repo/ExposedUsersRepo.kt`

- Add a NULLABLE column: `private val emailColumn = text("email").nullable()`.
- `ResultRow.asObject`: `email = get(emailColumn)?.let { Email(it) }` (re-validates; consider `Email.parse(it).getOrNull()` to be defensive against legacy/manually-inserted bad rows — RECOMMEND `parse(...).getOrNull()` to avoid a runtime throw on a corrupt row).
- `update(id, value, it)`: `it[emailColumn] = value.email?.raw`.
- `InsertStatement.asObject(value)`: include `email = value.email` in the returned `RegisteredUser`.
- Back-compatible migration: Exposed `createMissingTablesAndColumns` (invoked via the repo's `initTable()` / micro_utils `AbstractExposedCRUDRepo`) ADDS the nullable column to the existing `users` table. Existing rows get `NULL` ⇒ remain valid. NO data migration needed. (Confirm `initTable()` triggers `createMissingTablesAndColumns` in micro_utils; it does in this codebase's pattern — VALIDATOR to confirm at runtime.)
- `CacheUsersRepo` needs no change (it delegates to the underlying repo and caches `RegisteredUser`, which now simply carries the extra field).

## 5.3 Wiring the `/myEmail` write

The email server `EmailRoutingsConfigurator` receives `UsersRepo` (already in DI as `single<UsersRepo>`). `/myEmail` handler: `usersRepo.getById(callerId)` → `usersRepo.update(callerId, existing.toNewUser().copy(email = received))` (or a dedicated `setEmail`). Since `update` takes `NewUser`, construct `NewUser(existing.username, received)`. Confirmed `UsersRepo : CRUDRepo<RegisteredUser, UserId, NewUser>` supports `update(id, NewUser)`.

---

# 6. `Email` value class spec (reference prior art, re-implement)

Re-implement (do not blind-copy) the prior `Email.kt` (retrieved via `git show origin/issue/44-email:features/email/common/src/commonMain/kotlin/models/Email.kt`). Required shape:
- `@Serializable(with = EmailSerializer::class) @JvmInline value class Email private constructor(val raw: String)`.
- Companion: `MAX_LENGTH = 254` (RFC 5321), permissive RFC-ish `REGEX` (`^[A-Za-z0-9!#$%&'*+/=?^_`{|}~.-]+@[A-Za-z0-9-]+(\.[A-Za-z0-9-]+)+$`), `isValid(String): Boolean` (trim + non-blank + length + regex), `operator fun invoke(String): Email` (trims, `require(isValid)` else throws `IllegalArgumentException`), `parse(String): Result<Email> = runCatching { invoke(value) }`.
- `object EmailSerializer : KSerializer<Email>` — `PrimitiveSerialDescriptor(..., PrimitiveKind.STRING)`, `serialize` writes `value.raw`, `deserialize` routes the string through `Email(...)` ⇒ re-validates on decode (malformed wire value fails fast).

This satisfies spec requirement #5 (value class + ALL validation + non-throwing parse + primitive-string serialization re-validated on decode).

---

# 7. Wiring checklist (exact files to edit)

1. **`settings.gradle`** — add three includes:
   `":features:email:common"`, `":features:email:server"`, `":features:email:client"`.
2. **`server/build.gradle`** — add `api project(":wishlist.features.email.server")`.
3. **`client/build.gradle`** (commonMain) — add `api project(":wishlist.features.email.client")`.
4. **`client/android/build.gradle`** — NO change needed (Android gets email client transitively via `api project(":wishlist.client")`; confirmed android build.gradle only declares `:wishlist.client`). For safety/consistency with the ARCHITECTURE.md feature-adding rule which lists adding to `client/android/build.gradle`, CODING may add `api project(":wishlist.features.email.client")` to `client/android/build.gradle` commonMain — but verify whether that module even has a commonMain deps block; current file only has `api project(":wishlist.client")`. DEFAULT: rely on transitive (matches how currency/booking are wired — they are NOT separately added to android). **No android build.gradle change.**
5. **`server/sample.config.json`** — add `"dev.inmo.wishlist.features.email.server.JVMPlugin"` to `plugins`; add an SMTP config block (default DISABLED), e.g. `"smtp": null` (or omit — absence ⇒ disabled). Optionally add a commented/example smtp object for operators.
6. **`features/users/common/build.gradle`** — add `api project(":wishlist.features.email.common")` (the §1.3 dependency).
7. **`gradle/libs.versions.toml`** — add `angus-mail` version + library entry; reference it in `features/email/server/build.gradle`.
8. **Client entry points** — add the email client platform plugin to ALL THREE:
   - `client/src/jsMain/kotlin/Main.kt` → `dev.inmo.wishlist.features.email.client.JSPlugin` (in the full-stack feature block, alongside `currency.client.JSPlugin`).
   - `client/src/jvmMain/kotlin/Main.kt` → `dev.inmo.wishlist.features.email.client.JVMPlugin`.
   - `client/android/src/main/kotlin/MainActivity.kt` → `dev.inmo.wishlist.features.email.client.AndroidPlugin`.
9. **`features/email/README.md`** — create per ALL.md required structure (Title, empty `## Operator Notes`, Overview, Routes table, Models, Architecture Notes). DOC content authored by `haiku` agent per SHORTCUTS rule #4.

---

# 8. adminPanel UI — surface root-only "send test email"

The adminPanel dashboard (`features/ui/adminPanel`) gains a test-email action. Files to touch:

1. **`build.gradle`** — add `api project(":wishlist.features.email.client")`.
2. **`AdminPanelModel.kt`** — add `suspend fun sendTestEmail(target: Email): Boolean` (and optionally `suspend fun isEmailFeatureEnabled(): Boolean`). Import `Email` from email/common (already transitively available via email client dep).
3. **`Plugin.kt`** — extend the `single<AdminPanelModel>` object: resolve `get<EmailFeature>()` (email client binding) and implement `sendTestEmail` → `email.sendTestEmail(target)`. (Mirrors how `AdminFeature` is resolved.)
4. **`AdminPanelViewModel.kt`** — add `fun onSendTestEmail(target: Email)` launching `scope.launchLoggingDropExceptions { model.sendTestEmail(target) }` (note: VM currently keeps `model` private/unused; now it gets a real use). Surface result state (StateFlow) if UI feedback desired.
5. **`AdminPanelStrings.kt`** — add `sendTestEmailSection`, `sendTestEmailButton`, target-input label, success/failure strings (EN + RU `IetfLang.Russian`).
6. **Per-platform dashboard views** (`jsMain`, `jvmMain`, `androidMain` `ui/AdminPanelView.kt`) — add an action/button (`CalmButton` on JS; platform equivalent on JVM/Android) that collects a target address, validates via `Email.parse(input)`, and calls `viewModel.onSendTestEmail(...)`. Only root reaches the admin panel already (admin routes are root-guarded server-side; the dashboard is the admin surface), so no extra client gating is strictly required — the server enforces root on `POST /email/sendTest`. UI may additionally hide the action behind the existing admin-panel entry.

NOTE: this dashboard already exists and is root-scoped in practice (it drives `/admin/*` which is root-only). The new button merely adds a call to the email feature. Real authorization is server-side (`requireRoot`).

---

# 9. Disabled / no-op behaviour (spec #2)

- `EmailConfig.smtp == null` (absent block) ⇒ `SmtpEmailService.isFeatureEnabled() == false`; `sendTestEmail` returns `false` immediately (no SMTP attempt, logs at debug). `GET /email/enabled` ⇒ `false`. Client `KtorEmailFeature.isFeatureEnabled()` reflects it. adminPanel can hide/disable the button when `false`.
- Per-user `/myEmail` self-service does NOT require SMTP configured — storing an email is independent of sending. It works regardless of the SMTP block. (Document this: email STORAGE always on; email SENDING gated by SMTP config.)

---

# 10. Build/verification expectations (for later roles)

- New/changed modules + server + client targets (js/jvm/android) must `BUILD SUCCESSFUL`.
- After any source change: `ast-index rebuild` (ALL.md rule).
- VALIDATOR confirms: Email validation (valid/invalid/parse), nullable column migration is additive, root guard returns 403 for non-root and 200 for root, disabled no-op path, serialization round-trip of `Email` as a bare string, existing user JSON without `email` still decodes.

---

# Blocking questions for operator

NONE strictly block planning. Defaults chosen and documented:

1. **SMTP library/version** — DEFAULT: `org.eclipse.angus:angus-mail` (Jakarta Mail), version `2.0.3` (CODING to confirm newest stable). If operator prefers a different SMTP stack (e.g. Simple Java Mail, commons-email), state it; otherwise Angus Mail proceeds.
2. **Route ownership** — DEFAULT: email feature owns `/email/sendTest` (root) + `/email/myEmail` (self); adminPanel UI calls the email client feature. Proceed unless operator wants routes under `/admin`.
3. **`Email` placement** — DEFAULT: `features/email/common`, with `features/users/common` depending on `features/email/common` (proven legal by existing `auth/common → users/common`). Proceed unless operator wants `Email` relocated to `features/common/common`.
4. **android client wiring** — DEFAULT: rely on transitive `:wishlist.client` (matches currency/booking); no `client/android/build.gradle` edit. Proceed unless operator wants the explicit dependency added.
5. **SMTP test-email recipient/sender semantics** — `POST /email/sendTest` body = target `Email`; the "from" address comes from `EmailConfig.smtp.from`. If operator wants the test sent to the root user's own stored email instead of a request-supplied target, note it; DEFAULT = request-supplied target `Email`.
