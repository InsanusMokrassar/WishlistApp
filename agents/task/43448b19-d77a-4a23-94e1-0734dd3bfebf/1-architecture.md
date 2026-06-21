Model: opus (claude-opus-4-8)
Execution time: 1080
Tokens used: ~115000
Changed files: agents/task/43448b19-d77a-4a23-94e1-0734dd3bfebf/1-architecture.md

# Step 1 — ARCHITECTURE — GitHub issue #44 (Email feature)

## Model argumentation

ARCHITECTURE role model priority (ALL.md / SHORTCUTS) = `fable` / `opus` / `sonnet`.
`fable` unavailable in this run → next priority `opus` selected. Recorded per ALL.md header rule.

## Protocol note

ALL.md DISABLES auto-memory and mandates `/caveman full`; the orchestrator PROMPT.md for this role
explicitly requests a precise, code-free, PINNED architecture in structured Markdown plus a concise
summary. The role PROMPT.md (direct task) is authoritative for deliverable format. Auto-memory ignored.
No code edited; only this single step file written, per ARCHITECTURE.md hard constraint
(MUST NOT EDIT ANY FILE except this step report).

## Inconsistencies found in prior steps (0-planning.md)

**NONE that block.** All load-bearing planning claims VERIFIED against the live tree (evidence inline
below). Three refinements / clarifications carried forward (not contradictions — sharper pins):

1. **DI placement of the JVM mail service.** 0-planning §3.2 hedged ("either is acceptable"). PINNED here:
   because `features/email/server` uses template `mppJavaProject` (JVM-only — verified the prior PR #49
   `features/email/server/build.gradle` applies `$mppJavaProject`, and `currency/server` does the same and
   freely references the JVM-only OkHttp engine in `commonMain`), the mail service + its `single<>` binding
   live in **`server commonMain`** (NOT `jvmMain`). This MATCHES prior PR #49 exactly
   (`services/JavaMailEmailService.kt` was in `commonMain`) and matches `OpenExchangeRatesService` placement.
   `jvmMain/JVMPlugin` stays a thin delegating entry point only. This is simpler and is the established
   convention; see §3.2.
2. **`GET /email/enabled` route + client `isFeatureEnabled()`.** 0-planning proposed mirroring currency's
   `enabled` endpoint. Prior PR #49 did NOT have it. The orchestrator PROMPT pins the client surface as
   `sendTestEmail(recipient) + setMyEmail(email)` only. PINNED: include `GET /email/enabled` +
   `isFeatureEnabled()` on BOTH server and client `EmailFeature` — it lets the adminPanel hide/disable the
   button when SMTP is unconfigured and mirrors `CurrencyFeature`. This is an ADDITION, not a conflict.
3. **Server `sendTestEmail` signature.** Prior PR #49 server `EmailFeature` exposed a generic
   `sendEmail(to, subject, body)`. The PROMPT pins `sendTestEmail(recipient: Email?)`. PINNED: server
   `EmailFeature.sendTestEmail(recipient: Email): Boolean` (the route supplies a fixed test subject/body),
   keeping the public capability test-email-shaped per the issue ("root to send test email"). A private
   helper may build the message; no generic send API is exposed in iteration 1.

## Blocking questions

**NONE.** All defaults from 0-planning confirmed and re-pinned below (SMTP coordinate, route ownership,
`Email` placement, android-transitive wiring, test recipient = request-supplied `Email`). See §12.

---

# 0. Evidence ledger (every load-bearing claim, file:line)

| Claim | Evidence |
|---|---|
| `*/common` MAY depend on another feature's `*/common` (rule bans only literal `features/common/*`) | `features/auth/common/build.gradle` declares `api project(":wishlist.features.users.common")` (verified via `cat`). `users/common/build.gradle` + `currency/common/build.gradle` depend only on `:wishlist.features.common.common`. |
| Config-slice decode pattern | `features/currency/server/src/commonMain/kotlin/Plugin.kt:34` — `single { get<Json>().decodeFromJsonElement(CurrencyConfig.serializer(), config) }`. `CurrencyConfig.kt:15-19` nullable-field default = disabled. |
| Routing configurator registration | `currency/server/Plugin.kt:55-57` — `singleWithRandomQualifier<ApplicationRoutingConfigurator.Element> { CurrencyRoutingsConfigurator(get()) }`. |
| JVMPlugin delegation shape | `currency/server/jvmMain/JVMPlugin.kt:13-23` — `with(common.JVMPlugin){setupDI}; with(Plugin){setupDI}` + matching `startPlugin`. |
| Root guard | `admin/server/.../AdminRoutingsConfigurator.kt:65-73` `requireAdmin()`; uses `getCallerUserIdOrAnswerUnauthorized()` + `user.username.string != "root"` → `403`. Inside `authenticate { route(prefix) { … } }` (lines 76-77). |
| Auth util location/signature | `features/auth/server/src/commonMain/kotlin/utils/ApplicationCallUserId.kt:22-24` — `suspend fun RoutingContext.getCallerUserIdOrAnswerUnauthorized(): UserId?`. |
| Ktor client feature pattern | `currency/client/.../KtorCurrencyFeature.kt:20-44` (HTTP-only, `response.status.isSuccess()`); client `Plugin.kt:18-22`. |
| User model | `features/users/common/src/commonMain/kotlin/models/User.kt` — `sealed interface User { val username }`, `data class NewUser(username)`, `data class RegisteredUser(id, username)`. |
| ExposedUsersRepo | `features/users/common/src/jvmMain/kotlin/repo/ExposedUsersRepo.kt` — columns, `asObject` (25-29), `update` (36-38), `InsertStatement.asObject` (40-44), `initTable()` (52). |
| `UsersRepo` is `CRUDRepo<RegisteredUser, UserId, NewUser>` (supports `update(id, NewUser)`) | `users/common/.../repo/UsersRepo.kt:8`. |
| `NewUser` construction sites (all single-arg) | admin/server `UsersManagementFeature.kt:38`; auth/server `AuthFeatureService.kt:118`; auth/server `JVMPlugin.kt:46`; adminPanel `AdminUserEditViewModel.kt:139`; ui/users `Plugin.kt:77` (`ast-index usages NewUser`). |
| `RegisteredUser` construction sites | ONLY `ExposedUsersRepo.kt:26` + `:41` (the two we edit) — all other 48 usages are type references, not constructor calls (verified `grep "RegisteredUser(" … | grep -v StateFlow/List`). |
| Value-class + serializer reference | prior `git show origin/issue/44-email:…/models/Email.kt` (full shape retrieved, §6). `Username.kt` = `@JvmInline value class Username(val string)`. |
| libs.versions.toml has no mail entry | verified full read — no angus/jakarta/mail key. |
| Calm components for JS UI | `CalmButton`/`CalmButtonVariant`, `ContentColumn`, `PageHead` (used in adminPanel JS view); `CalmTextField` (`features/common/client/src/jsMain/.../ui/components/CalmForms.kt:59-82`, supports `InputType.Email`). No `.css`, no Bootstrap. |
| adminPanel dashboard views (3 platforms) | JS `AdminPanelView.kt` (CalmButton in `PageHead.actions`); JVM `AdminPanelView.kt` (Material `Button`); Android `AdminPanelView.kt` (Material3 `Button` + `LocalResources`). |
| Client entry points | js `client/src/jsMain/kotlin/Main.kt:25-26`; jvm `client/src/jvmMain/kotlin/Main.kt:21-22`; android `client/android/src/main/kotlin/MainActivity.kt:36-37` (currency plugin lines = insertion anchors). |
| settings.gradle includes | lines 32-34 currency triple; 36-38 booking triple (anchor for new email triple). |
| server/build.gradle | line 22 currency server dep (anchor). client/build.gradle line 27 currency client dep (anchor). |
| sample.config.json plugins array | currency JVMPlugin present; `openExchangeRatesAppId: null` at root shows slice-config keys live at the root object. |
| generate_feature.sh client scaffold = Compose template | `.templates/standard_module_kts/{{$module_path}}/client/build.gradle` applies `$mppJvmJsAndroidWithCompose` + compose plugins. |

---

# 1. `Email` placement & dependency direction (CONFIRMED)

- `Email` value class OWNED by **`features/email/common`**, package
  `dev.inmo.wishlist.features.email.common.models`.
- **`features/users/common/build.gradle`** gains `api project(":wishlist.features.email.common")`.
  LEGAL — proven by the existing `features/auth/common → :wishlist.features.users.common` precedent
  (a `*/common` depending on another feature's `*/common`). The dependency-graph ban applies ONLY to the
  literal `features/common/*` base modules.
- **No cycle:** `email/common` depends only on `:wishlist.features.common.common`. `users/common` depends
  on `:wishlist.features.common.common` + `:wishlist.features.email.common`. `email/common` does NOT depend
  on `users/common`. Acyclic.

---

# 2. SMTP dependency (PINNED EXACTLY)

Add to `gradle/libs.versions.toml`:

- `[versions]` block, append: `angusMail = "2.0.3"`
- `[libraries]` block (under `# Misc`), append:
  `angus-mail = { module = "org.eclipse.angus:angus-mail", version.ref = "angusMail" }`

**Coordinate:** `org.eclipse.angus:angus-mail:2.0.3` (Eclipse Angus = the reference Jakarta Mail 2.x
implementation; transitively provides the `jakarta.mail` API, so a single dependency suffices — no separate
`jakarta.mail:jakarta.mail-api` entry needed). This is the de-facto JVM SMTP stack and exactly what prior
PR #49 used (`angusMail = "2.0.3"`, `org.eclipse.angus:angus-mail`). Catalog accessor `libs.angus.mail`
(dashes → dots).

**Consumed ONLY in** `features/email/server/build.gradle` (`commonMain` deps): `api libs.angus.mail`.
The module is JVM-only (`mppJavaProject`), so Jakarta Mail imports compile cleanly in `commonMain`.

> Note for CODING: `2.0.3` is the pinned default (matches prior art). If the project's repositories do not
> resolve it, fall back to the newest stable `org.eclipse.angus:angus-mail` 2.x — do not switch artifacts.

---

# 3. `features/email` source tree (PINNED — mirror currency + admin)

Scaffold first: `./generate_feature.sh` → enter `email`. Produces `features/email/{common,server,client}`
with stub `build.gradle` + per-platform `Plugin.kt`/`JVMPlugin.kt`/`JSPlugin.kt`/`AndroidPlugin.kt`. Then
customize as below. After all source edits: `ast-index rebuild` (ALL.md).

> Package root for the feature: `dev.inmo.wishlist.features.email.{common|server|client}`.

## 3.1 `features/email/common` — template `mppJvmJsAndroid`; dep: `:wishlist.features.common.common`

| File (path under `features/email/common/src/`) | Symbol | Package | Responsibility |
|---|---|---|---|
| `commonMain/kotlin/models/Email.kt` | `value class Email` + `object EmailSerializer` | `…email.common.models` | `@Serializable(with = EmailSerializer::class) @JvmInline value class Email private constructor(val raw: String)`. Companion: `private const MAX_LENGTH = 254`, `private val REGEX`, `fun isValid(String): Boolean`, `operator fun invoke(String): Email` (trims + `require(isValid)` → throws `IllegalArgumentException`), `fun parse(String): Result<Email> = runCatching { invoke(value) }`. `EmailSerializer : KSerializer<Email>` — `PrimitiveSerialDescriptor("dev.inmo.wishlist.features.email.common.models.Email", PrimitiveKind.STRING)`; `serialize` writes `value.raw`; `deserialize` routes through `Email(decoder.decodeString())` (re-validates on decode). Exact reference shape in §6. |
| `commonMain/kotlin/EmailConstants.kt` | `object EmailConstants` | `…email.common` | Shared path parts: `const val prefixPathPart = "email"`, `const val enabledPathPart = "enabled"`, `const val sendTestPathPart = "sendTest"`, `const val myEmailPathPart = "myEmail"`. Single source of truth for server routes + client. (Prior PR named this `Constants`; PINNED name `EmailConstants` to match `CurrencyConstants` naming convention — orchestrator PROMPT explicitly asks for `EmailConstants`.) |
| `commonMain/kotlin/models/TestEmailRequest.kt` | `@Serializable data class TestEmailRequest(val recipient: Email)` | `…email.common.models` | Body for `POST /email/sendTest`. `Email` serializes as a bare string, so the JSON body is `{"recipient":"a@b.com"}`. |
| `commonMain/kotlin/models/SetEmailRequest.kt` | `@Serializable data class SetEmailRequest(val email: Email? = null)` | `…email.common.models` | Body for `PUT /email/myEmail`. Nullable → allows clearing; default `null`. |
| `commonMain/kotlin/EmailFeature.kt` | `interface EmailFeature` | `…email.common` | SHARED capability surface (server impl + client impl both implement it — mirrors `CurrencyFeature`). Methods: `suspend fun isFeatureEnabled(): Boolean`; `suspend fun sendTestEmail(recipient: Email): Boolean`; `suspend fun setMyEmail(email: Email?): Boolean`. KDoc: disabled ⇒ `isFeatureEnabled()==false`, `sendTestEmail` returns `false`. (Decision: ONE common interface, not separate server/client copies — cleaner than prior PR's two interfaces; both sides implement the same contract. Client's `setMyEmail` and `sendTestEmail` are server-routed; `isFeatureEnabled` → `GET /email/enabled`.) |
| `commonMain/kotlin/Plugin.kt` | `object Plugin : StartPlugin` | `…email.common` | Empty body (no common DI needed) — mirrors scaffold stub. |
| `jvmMain/kotlin/JVMPlugin.kt` | `object JVMPlugin : StartPlugin` | `…email.common` | Delegates to `Plugin` (scaffold stub, mirror `currency/common/jvmMain`). |
| `jsMain/kotlin/JSPlugin.kt` | `object JSPlugin : StartPlugin` | `…email.common` | Scaffold stub. |
| `androidMain/kotlin/AndroidPlugin.kt` | `object AndroidPlugin : StartPlugin` | `…email.common` | Scaffold stub. |

## 3.2 `features/email/server` — template `mppJavaProject`; deps below

`build.gradle` (`commonMain`):
```
api project(":wishlist.features.email.common")
api project(":wishlist.features.common.server")
api project(":wishlist.features.users.common")   // UsersRepo + Username for /myEmail + root check
api project(":wishlist.features.auth.server")     // getCallerUserIdOrAnswerUnauthorized
api libs.angus.mail
```

| File (under `features/email/server/src/`) | Symbol | Package | Responsibility |
|---|---|---|---|
| `commonMain/kotlin/EmailConfig.kt` | `@Serializable data class EmailConfig(val smtp: SmtpConfig? = null)` + `@Serializable data class SmtpConfig(...)` | `…email.server` | Slice-decoded from root config JSON exactly like `CurrencyConfig`. `smtp == null` (default/absent) ⇒ feature disabled. `SmtpConfig(host: String, port: Int = 587, username: String? = null, password: String? = null, from: Email, useTls: Boolean = true, useSsl: Boolean = false)`. (Matches prior PR #49 shape verbatim.) |
| `commonMain/kotlin/services/SmtpEmailService.kt` | `class SmtpEmailService(private val config: EmailConfig) : EmailFeature` | `…email.server.services` | Implements the COMMON `EmailFeature`. `isFeatureEnabled()` = `config.smtp != null && config.smtp.host.isNotBlank()`. `sendTestEmail(recipient)` builds `jakarta.mail.Session` + `MimeMessage` and `Transport.send(...)` inside `withContext(Dispatchers.IO)` (blocking SMTP off the event loop); returns `false` + logs (`dev.inmo.kslog`) when disabled or on exception. `setMyEmail` is NOT a server-service concern (storage handled in the route via `UsersRepo`) → server impl `setMyEmail` returns `false`/throws-not-supported OR (PINNED) the server `EmailFeature` binding used by routes is `SmtpEmailService` only for `isFeatureEnabled` + `sendTestEmail`; `/myEmail` writes through `UsersRepo` directly (see §4). Lazy `Session` built from `SmtpConfig` (mirror prior `JavaMailEmailService`). Lives in `commonMain` — module is JVM-only, Jakarta refs are safe (same as currency's OkHttp). |
| `commonMain/kotlin/configurators/EmailRoutingsConfigurator.kt` | `class EmailRoutingsConfigurator(private val feature: EmailFeature, private val usersRepo: UsersRepo) : ApplicationRoutingConfigurator.Element` | `…email.server.configurators` | Registers routes under `EmailConstants.prefixPathPart`. See §4 for exact routes + `requireRoot`. |
| `commonMain/kotlin/Plugin.kt` | `object Plugin : StartPlugin` | `…email.server` | `setupDI`: `single { get<Json>().decodeFromJsonElement(EmailConfig.serializer(), config) }`; `single { SmtpEmailService(get<EmailConfig>()) }`; `single<EmailFeature> { get<SmtpEmailService>() }`; `singleWithRandomQualifier<ApplicationRoutingConfigurator.Element> { EmailRoutingsConfigurator(get(), get()) }` (2nd `get()` = `UsersRepo`, registered by users server feature). Mirrors `currency/server/Plugin.kt` exactly. |
| `jvmMain/kotlin/JVMPlugin.kt` | `object JVMPlugin : StartPlugin` | `…email.server` | The FQCN listed in `sample.config.json`. Body mirrors `currency/server/jvmMain/JVMPlugin.kt`: `with(common.JVMPlugin){setupDI}; with(Plugin){setupDI}` in `setupDI`; `common.JVMPlugin.startPlugin(koin); Plugin.startPlugin(koin)` in `startPlugin`. |

**DI placement decision (PINNED):** mail service + bindings in `commonMain/Plugin` (NOT split into
`jvmMain`), because the module is JVM-only and this matches both `currency/server` (OkHttp in commonMain)
and prior PR #49 (`JavaMailEmailService` in commonMain). `jvmMain/JVMPlugin` = thin delegator only.

**Plugin delegation chain (PINNED):** server bootstrap (`features/common/server` JVMPlugin) connects the
PostgreSQL `Database` and the users server plugin registers `single<UsersRepo>`. The email JVMPlugin is
listed AFTER `users.server.JVMPlugin` in `sample.config.json` (it already is — users is line 3, email
appended at the end), so `UsersRepo` is in DI before `EmailRoutingsConfigurator` resolves it. Koin resolves
lazily at first injection, but ordering is respected for safety.

## 3.3 `features/email/client` — scaffold template `mppJvmJsAndroidWithCompose`; dep: `:wishlist.features.email.common`

(Keep the scaffold's Compose template even though no Compose UI ships here — it is what
`generate_feature.sh` produces and what `currency/client` effectively uses; building is unaffected. Do NOT
hand-trim to `mppJvmJsAndroid` — leave the generated `build.gradle` as scaffolded + add the email.common
dep. `build.gradle commonMain`: `api project(":wishlist.features.email.common")` + `api project(":wishlist.features.common.client")`.)

| File (under `features/email/client/src/`) | Symbol | Package | Responsibility |
|---|---|---|---|
| `commonMain/kotlin/KtorEmailFeature.kt` | `class KtorEmailFeature(private val client: HttpClient) : EmailFeature` | `…email.client` | HTTP-only over the shared `HttpClient` (resolved via `get()` from `features/common/client`). `isFeatureEnabled()` → `GET email/enabled` (`response.body()` Boolean else `false`). `sendTestEmail(recipient)` → `POST email/sendTest` with body `TestEmailRequest(recipient)` → `response.status.isSuccess()`. `setMyEmail(email)` → `PUT email/myEmail` with body `SetEmailRequest(email)` → `response.status.isSuccess()`. No caching/state (project Ktor-realization rule). Path constants from `EmailConstants`. Mirror `KtorCurrencyFeature`. |
| `commonMain/kotlin/Plugin.kt` | `object Plugin : StartPlugin` | `…email.client` | `single { KtorEmailFeature(get()) }`; `single<EmailFeature> { get<KtorEmailFeature>() }`. (No service wrapper — no memoization needed, unlike currency's `CurrencyService`.) Mirror `currency/client/Plugin.kt`. |
| `jvmMain/kotlin/JVMPlugin.kt`, `jsMain/kotlin/JSPlugin.kt`, `androidMain/kotlin/AndroidPlugin.kt` | platform `*Plugin` objects | `…email.client` | Delegating stubs (mirror `currency/client` per platform). These are the FQCNs added to the 3 client entry points. |

---

# 4. Server routes & root guard (PINNED)

`EmailRoutingsConfigurator : ApplicationRoutingConfigurator.Element`. `override fun Route.invoke()`:

```
authenticate {
    route(EmailConstants.prefixPathPart) {
        get(EmailConstants.enabledPathPart) { ... }       // NOTE: see auth note below
        post(EmailConstants.sendTestPathPart) { ... }
        put(EmailConstants.myEmailPathPart) { ... }
    }
}
```

| Method | Path (full, after `/api` prefix) | Auth | Body | Behaviour |
|---|---|---|---|---|
| GET | `/api/email/enabled` | public | — | `call.respond(feature.isFeatureEnabled())` (Boolean). |
| POST | `/api/email/sendTest` | bearer + ROOT | `TestEmailRequest` | `requireRoot()` guard, then `feature.sendTestEmail(req.recipient)` → `200 OK` / `500`. |
| PUT | `/api/email/myEmail` | bearer (self) | `SetEmailRequest` | `getCallerUserIdOrAnswerUnauthorized()`, load user via `usersRepo.getById`, `usersRepo.update(callerId, NewUser(user.username, req.email))` → `200` / `404` / `500`. NOT root-only. |

**Auth-block placement note (PINNED):** `GET /email/enabled` must be PUBLIC (no token). To avoid wrapping
it in `authenticate { }`, structure the configurator as: register the public `get(enabledPathPart)` under a
plain `route(prefixPathPart) { }` AND the two protected endpoints under
`authenticate { route(prefixPathPart) { post(...); put(...) } }`. Two `route(prefixPathPart)` blocks (one
public, one authenticated) are valid in Ktor and keep `enabled` token-free (currency's `enabled` is fully
public). CODING: mirror this split; do not place `enabled` inside `authenticate`.

**Root guard (PINNED, mirror `AdminRoutingsConfigurator.requireAdmin`):**
```
private val rootUsername = "root"
private suspend fun RoutingContext.requireRoot(): Boolean {
    val callerId = getCallerUserIdOrAnswerUnauthorized() ?: return false   // responds 401
    val user = usersRepo.getById(callerId)
    if (user == null || user.username.string != rootUsername) { call.respond(Forbidden); return false }
    return true
}
```
`POST /sendTest` handler: `if (!requireRoot()) return@post`. `getCallerUserIdOrAnswerUnauthorized` imported
from `dev.inmo.wishlist.features.auth.server.utils`.

**Route ownership (CONFIRMED):** email feature OWNS `/email/sendTest` (root) + `/email/myEmail` (self).
Does NOT reuse `/admin`. The adminPanel UI calls the email CLIENT feature, exactly as it calls `AdminFeature`.

---

# 5. Per-user optional Email (User model + storage) (PINNED)

## 5.1 `features/users/common/src/commonMain/kotlin/models/User.kt`

- Add `import dev.inmo.wishlist.features.email.common.models.Email`.
- `sealed interface User { val username: Username; val email: Email? }` (add `val email: Email?` to the
  interface; implementors provide it).
- `data class NewUser(override val username: Username, override val email: Email? = null) : User` —
  back-compatible default `= null`.
- `data class RegisteredUser(val id: UserId, override val username: Username, override val email: Email? = null) : User`
  — back-compatible default `= null`.

**Serialization back-compat:** `email = null` default means existing JSON without `email` decodes fine;
`Email` serializes as a bare string so present values round-trip. (`@Serializable` already on all three.)

## 5.2 `features/users/common/build.gradle`

`commonMain` deps: add `api project(":wishlist.features.email.common")` (LEGAL per §1).

## 5.3 `features/users/common/src/jvmMain/kotlin/repo/ExposedUsersRepo.kt`

- Add import `dev.inmo.wishlist.features.email.common.models.Email`.
- New NULLABLE column: `private val emailColumn = text("email").nullable()`.
- `ResultRow.asObject` (line ~25): add `email = get(emailColumn)?.let { Email.parse(it).getOrNull() }`
  (use `parse(...).getOrNull()` — defensive against legacy/manually-inserted malformed rows; avoids a
  runtime throw on read).
- `update(id, value, it)` (line ~36): add `it[emailColumn] = value.email?.raw`.
- `InsertStatement.asObject(value)` (line ~40): add `email = value.email` to the returned `RegisteredUser`.
- Migration: `initTable()` (line 52, already called in `init`) → micro_utils `AbstractExposedCRUDRepo`
  triggers `createMissingTablesAndColumns`, which ADDS the nullable `email` column to the existing `users`
  table. Existing rows get `NULL` ⇒ valid. No data migration. (VALIDATOR confirms at runtime.)
- `CacheUsersRepo`: NO change (delegates + caches `RegisteredUser`, which now carries the extra field).

## 5.4 Call-site ripple (VERIFIED — all safe via `= null` default)

`NewUser(...)` constructed at 5 sites, ALL single-arg `NewUser(username)` — compile unchanged because the
new `email` param defaults to `null`:
- `features/admin/server/.../UsersManagementFeature.kt:38`
- `features/auth/server/.../services/AuthFeatureService.kt:118`
- `features/auth/server/.../jvmMain/JVMPlugin.kt:46`
- `features/ui/adminPanel/.../ui/AdminUserEditViewModel.kt:139`
- `features/ui/users/.../Plugin.kt:77`

`RegisteredUser(...)` constructed at only 2 sites — both inside `ExposedUsersRepo` (`:26`, `:41`), which we
edit anyway. No other constructor call sites exist (all other `RegisteredUser` references are type
positions: `List<RegisteredUser>`, `StateFlow<RegisteredUser?>`, etc.). **Zero external breakage.**

---

# 6. `Email` value class — exact reference shape (re-implement, do not blind-copy)

Retrieved via `git show origin/issue/44-email:features/email/common/src/commonMain/kotlin/models/Email.kt`.
Required shape (CODING re-implements, keeping KDoc):

- `@Serializable(with = EmailSerializer::class) @JvmInline value class Email private constructor(val raw: String)`.
- Companion: `private const val MAX_LENGTH = 254` (RFC 5321); permissive regex
  `^[A-Za-z0-9!#$%&'*+/=?^_`{|}~.-]+@[A-Za-z0-9-]+(\.[A-Za-z0-9-]+)+$`;
  `fun isValid(value): Boolean` = `trim().isNotBlank() && length <= MAX_LENGTH && REGEX.matches(...)`;
  `operator fun invoke(value): Email` = trims, `require(isValid(trimmed)) { "Invalid email address: …" }`,
  returns `Email(trimmed)`; `fun parse(value): Result<Email> = runCatching { invoke(value) }`.
- `object EmailSerializer : KSerializer<Email>` — `PrimitiveSerialDescriptor(<FQN>, PrimitiveKind.STRING)`;
  `serialize` → `encoder.encodeString(value.raw)`; `deserialize` → `Email(decoder.decodeString())`
  (re-validates; malformed wire value fails fast).

Satisfies spec #5 (value class + ALL validation + non-throwing `parse` + primitive-string serialization
re-validated on decode).

---

# 7. Full wiring checklist (exact files + anchors)

1. **`settings.gradle`** — after line 38 (booking triple) or after currency triple, add:
   `":features:email:common",` `":features:email:server",` `":features:email:client",`.
2. **`server/build.gradle`** — after line 23 (booking server) add `api project(":wishlist.features.email.server")`.
3. **`client/build.gradle`** (`commonMain`) — after line 28 (currency client) add
   `api project(":wishlist.features.email.client")`.
4. **`client/android/build.gradle`** — NO change. Android gets the email client transitively via
   `:wishlist.client` (matches currency/booking — neither is separately added to android). CONFIRMED by the
   android entry-point list (uses `currency.client.AndroidPlugin` with no android-build dep). DEFAULT: rely
   on transitive.
5. **`server/sample.config.json`** — append `"dev.inmo.wishlist.features.email.server.JVMPlugin"` to the
   `plugins` array (last entry). Add a sample SMTP block at the ROOT object (slice keys live at root, like
   `openExchangeRatesAppId`). PINNED default = DISABLED: add `"smtp": null` (absence ⇒ disabled; explicit
   `null` documents the key). Optionally add a commented operator example in README (JSON has no comments).
   Concrete example block for README docs (NOT committed enabled):
   ```json
   "smtp": { "host": "smtp.example.com", "port": 587, "username": "user", "password": "pass",
             "from": "noreply@example.com", "useTls": true, "useSsl": false }
   ```
6. **`features/users/common/build.gradle`** — add `api project(":wishlist.features.email.common")`.
7. **`gradle/libs.versions.toml`** — add `angusMail = "2.0.3"` + `angus-mail = { module = "org.eclipse.angus:angus-mail", version.ref = "angusMail" }`; reference `api libs.angus.mail` in `features/email/server/build.gradle`.
8. **Client entry points** — add the email client platform plugin to ALL THREE (alongside `currency.client`):
   - `client/src/jsMain/kotlin/Main.kt` (after line 25) → `dev.inmo.wishlist.features.email.client.JSPlugin,`
   - `client/src/jvmMain/kotlin/Main.kt` (after line 21) → `dev.inmo.wishlist.features.email.client.JVMPlugin,`
   - `client/android/src/main/kotlin/MainActivity.kt` (after line 36) → `dev.inmo.wishlist.features.email.client.AndroidPlugin,`
9. **`features/email/README.md`** — create per ALL.md structure: `# Feature: Email`, EMPTY `## Operator Notes`,
   `## Overview`, `## Routes` (the 3-row table from §4), `## Models` (`Email`, `TestEmailRequest`,
   `SetEmailRequest`, `EmailFeature`, `EmailConfig`/`SmtpConfig`), `## Architecture Notes` (placement of
   `Email` in email/common + users→email dep rationale; storage-vs-sending split; Angus Mail on
   `Dispatchers.IO`; no-op when SMTP null). Authored by the DOC (haiku) agent per SHORTCUTS; ARCHITECTURE
   role updates `## Architecture Notes` only.

---

# 8. adminPanel UI — root-only "send test email" (PINNED)

UX DECISION (simplest buildable on all 3 platforms): **a single text input + one button on the dashboard.**
Root types a recipient address, the View validates with `Email.parse(input)`, and on success calls
`viewModel.onSendTestEmail(email)`. Rationale: the issue says "root to send a test email"; a recipient field
is the minimal honest implementation (lets root verify deliverability to an arbitrary address) and `Email`
validation is already required. The button is disabled while input is blank/invalid and (optionally) hidden
when `isEmailFeatureEnabled()` is false. Real authorization is server-side (`requireRoot` on
`POST /email/sendTest`); the dashboard is already the root-only admin surface.

Files to touch (KEEP the View dumb; no business logic in views; Calm Studio rules — no Bootstrap, no `.css`,
reuse existing `CalmTextField`/`CalmButton`, no edits to shared components):

1. **`features/ui/adminPanel/build.gradle`** — `commonMain` deps add `api project(":wishlist.features.email.client")`.
2. **`…/commonMain/kotlin/ui/AdminPanelModel.kt`** — add to the interface:
   `suspend fun sendTestEmail(recipient: Email): Boolean` and `suspend fun isEmailFeatureEnabled(): Boolean`.
   Import `Email` from `…email.common.models` (transitively available via the email client dep).
3. **`…/commonMain/kotlin/Plugin.kt`** — in the `single<AdminPanelModel>` object: resolve
   `val email = get<EmailFeature>()` (the email client binding; import
   `dev.inmo.wishlist.features.email.common.EmailFeature`) and implement
   `override suspend fun sendTestEmail(recipient: Email) = email.sendTestEmail(recipient)` +
   `override suspend fun isEmailFeatureEnabled() = email.isFeatureEnabled()`. (Mirrors how `AdminFeature` is
   resolved + delegated.)
4. **`…/commonMain/kotlin/ui/AdminPanelViewModel.kt`** — add a result/loading `StateFlow` (e.g.
   `private val _sendState = MutableRedeliverStateFlow<Boolean?>(null)` exposed as `val sendState: StateFlow`)
   and `fun onSendTestEmail(recipient: Email) { scope.launchLoggingDropExceptions { _sendState.value = model.sendTestEmail(recipient) } }`.
   The currently-"unused" `model` field becomes used. Optionally an `init`/`onEnabledCheck` calling
   `model.isEmailFeatureEnabled()` into a StateFlow to gate the button.
5. **`…/commonMain/kotlin/AdminPanelStrings.kt`** — add EN + `IetfLang.Russian` entries:
   `sendTestEmailSection` ("Send test email" / "Отправить тестовое письмо"),
   `sendTestEmailRecipientLabel` ("Recipient" / "Получатель"),
   `sendTestEmailButton` ("Send" / "Отправить"),
   `sendTestEmailSuccess` ("Test email sent." / "Тестовое письмо отправлено."),
   `sendTestEmailFailure` ("Failed to send test email." / "Не удалось отправить тестовое письмо."),
   `sendTestEmailInvalid` ("Invalid email address." / "Некорректный адрес.").
6. **Per-platform dashboard views** — add the input + button + result feedback:
   - **JS** `…/jsMain/kotlin/ui/AdminPanelView.kt`: inside `ContentColumn`, after the `PageHead`, add a
     `CalmTextField(value, onValueChange, label = sendTestEmailRecipientLabel.translation(), type = InputType.Email)`
     bound to a `remember { mutableStateOf("") }`, then a `CalmButton(text = sendTestEmailButton.translation(),
     onClick = { Email.parse(value).onSuccess { viewModel.onSendTestEmail(it) } })`. Show success/failure via
     `FormHint` reading `viewModel.sendState`. Reuse existing Calm components only.
   - **JVM** `…/jvmMain/kotlin/ui/AdminPanelView.kt`: a Material `TextField` + `Button` row (matches the
     existing Material `Button` usage in that file), same validate-then-`onSendTestEmail` flow.
   - **Android** `…/androidMain/kotlin/ui/AdminPanelView.kt`: Material3 `TextField` + `Button`, using
     `LocalResources` for `translation(resources)` (matches the existing Android view).

---

# 9. Disabled / no-op behaviour (spec #2) (PINNED)

- `EmailConfig.smtp == null` (or blank host) ⇒ `SmtpEmailService.isFeatureEnabled() == false`;
  `sendTestEmail` returns `false` immediately (no SMTP attempt; logs at warn). `GET /email/enabled` ⇒ `false`.
  Client `KtorEmailFeature.isFeatureEnabled()` reflects it; adminPanel hides/disables the button.
- **Storage vs sending split:** `PUT /email/myEmail` does NOT require SMTP — storing a user email is
  independent of sending. Per-user email works regardless of the SMTP block. (Document in README Architecture
  Notes: email STORAGE always on; email SENDING gated by SMTP config.)

---

# 10. Obligations for CODING / later roles

- **KDoc** on every new public symbol (class/interface/object/fun/property) — matches existing files
  (currency/admin all carry KDoc).
- **No `else if`** chains (project rule) — use `when` (see `AdminRoutingsConfigurator` `when (updated)` pattern).
- **Calm Studio** (JS UI): no Bootstrap, no `.css`, reuse `CalmTextField`/`CalmButton`/`FormHint`/
  `ContentColumn`/`PageHead`, do not edit shared components.
- **Ktor realization rule:** `KtorEmailFeature` is transport-only (no caching/state/business logic).
- After any source change: **`ast-index rebuild`** (ALL.md).
- Build target: new/changed modules + server + client js/jvm/android must `BUILD SUCCESSFUL`.

---

# 11. VALIDATOR checklist (later role)

- `Email`: valid accepted (trimmed), invalid rejected (`invoke` throws, `parse` → `Result.failure`),
  serialization round-trips as bare string, malformed wire value fails on decode.
- Nullable `email` column added by `createMissingTablesAndColumns` (additive); existing rows `NULL`-valid;
  existing user JSON without `email` still decodes.
- `POST /email/sendTest`: 401 unauthenticated, 403 non-root, 200/500 root.
- `PUT /email/myEmail`: self-service writes/clears caller email.
- Disabled no-op: SMTP null ⇒ `enabled=false`, `sendTest` returns false without network attempt.
- adminPanel button present on js/jvm/android; validates input; reflects success/failure.

---

# 12. Confirmed defaults (no blocking questions)

1. SMTP = `org.eclipse.angus:angus-mail:2.0.3` (catalog `libs.angus.mail`), JVM-only in email/server.
2. Routes owned by email feature: `/email/enabled` (public), `/email/sendTest` (root), `/email/myEmail` (self).
3. `Email` in `features/email/common`; `users/common` depends on `email/common` (legal precedent cited).
4. Android client wiring = transitive via `:wishlist.client`; no `client/android/build.gradle` edit.
5. Test-email recipient = request-supplied `Email` (`TestEmailRequest.recipient`); "from" = `SmtpConfig.from`.
6. Mail send service + DI bindings live in email/server `commonMain` (module is JVM-only); `jvmMain/JVMPlugin`
   = thin delegator.
7. ONE shared `EmailFeature` interface in `email/common`, implemented by both `SmtpEmailService` (server) and
   `KtorEmailFeature` (client).
