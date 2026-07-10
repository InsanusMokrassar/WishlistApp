Model: Sonnet 5 (claude-sonnet-5)
Execution time: not exposed to this agent — session spanned reading ~15 files (agents/*.md, task folder, current email-feature source, MicroUtils reference sources) plus drafting this report
Tokens used: not exposed to this agent
Changed files: agents/task/10.07.2026_06.40.48-2407c3c3-e09a-4139-976a-305652d931a3/003-architecturing.md (this file only — no source file touched, per `agents/ARCHITECTURE.md`)

# 003 — Architecturing

## Model choice note

`agents/SHORTCUTS.md` sets the Architecture role's model priority as `fable / opus / sonnet`. This session was started as `Sonnet 5` by the invoking harness; there is no in-task mechanism available to this agent to switch its own underlying model mid-run. Recording this as a deviation from the stated priority for transparency, same as `001-planning.md` did for the Planning role.

## Tooling note

`ast-index` is on `PATH` (v3.45.0). `002-planning.md` flagged its `usages`/`search` cache as possibly stale for `Plugin.kt`/`EmailFeatureService.kt` (predating the uncommitted WIP edits). Per this task's explicit instruction, I did **not** rely on `ast-index` output for those two files — I `Read` both directly and confirmed their current on-disk contents match exactly what `002-planning.md` describes as "uncommitted WIP" (verified via `git diff` too). No `.kt` file was changed in this step, so the `agents/ALL.md` "`ast-index rebuild` after `.kt` changes" obligation does not apply here — only a step-report markdown file was written.

I additionally read source directly from the vendored `dev.inmo` package checkout at `/home/aleksey/projects/own/MicroUtils` (per `agents/local.ALL.md`) to confirm the exact shape of `CRUDRepo`/`ReadCRUDRepo`/`WriteCRUDRepo`/`MapCRUDRepo`/`MapKeyValueRepo` — this directly informed the test-double design below (see "Test doubles" section).

## Inputs read (in full)

`agents/ALL.md`, `agents/local.ALL.md`, `agents/ARCHITECTURE.md`, `agents/CODING.md`, `agents/GIT.md`, `agents/PROTOCOL.md`, `features/email/README.md`, `agents/task/.../PROMPT.md`, `agents/task/.../001-planning.md`, `agents/task/.../002-planning.md` (the finalized, READY plan — treated as the primary spec per this step's instructions), plus direct `Read`s of every current file `002-planning.md` discusses: `EmailConfig.kt`, `EmailFeature.kt`, `EmailsService.kt`, `services/SmtpEmailService.kt`, `services/EmailFeatureService.kt` (confirmed uncommitted WIP), `Plugin.kt` (confirmed uncommitted WIP), `configurators/EmailRoutingsConfigurator.kt`, `commonTest/kotlin/services/SmtpEmailServiceDisabledTest.kt`, `commonTest/kotlin/services/EmailAttachmentDataSourceTest.kt`, `features/users/common/.../repo/{UsersRepo,ReadUsersRepo,WriteUsersRepo,CacheUsersRepo}.kt`, `features/users/common/.../models/{User,Username}.kt`, `features/email/common/.../models/Email.kt`, `features/email/server/build.gradle`, `features/users/common/build.gradle`, `features/common/common/build.gradle`, `server/sample.config.json`, and confirmed `server/dev.config.json` has no `email`/`smtp` key (independent re-verification of `002-planning.md`'s claim).

## Verdict on `002-planning.md`

The plan is coherent, internally consistent, and every open question from `001-planning.md` (Q1–Q4) has a concrete operator-confirmed resolution. I traced every planned production-file change against the actual current file contents and found no discrepancy. I am adopting `002-planning.md`'s file-by-file plan as-is for all **production** code (items 1–7, 11–14), with two small, non-blocking deviations described below that exist purely to satisfy this step's Test Planning Requirement. I found **no genuine architectural ambiguity** `002-planning.md` left unresolved — no open questions are being raised to the operator from this step.

## Deviations from `002-planning.md`'s draft Kotlin (with reasoning)

### 1. Extract two pure decision functions out of `Plugin.kt`'s `setupDI`

`002-planning.md`'s `Plugin.kt` draft (item 6) inlines both conditional decisions directly inside `single { }` blocks:
- the `"email"` key presence/`JsonNull` check gating the `EmailConfig`/`SmtpEmailService`/`EmailsService` trio, and
- the `when (emailsService) { null -> DisabledEmailFeature(...); else -> EmailFeatureService(...) }` implementation choice.

This step's mandate (`agents/ARCHITECTURE.md` Test Planning Requirement) requires me to determine "whether/how [Plugin.kt's conditional DI wiring] needs test coverage." I searched the whole repo (`ast-index` + direct reads) for any precedent of testing a Koin `Module.setupDI`/spinning up a `Koin`/`koinApplication` container in a unit test — **none exists anywhere in this codebase** (only 3 test files exist total, all pre-dating this task, none touch DI). `koin-test` is not in the version catalog either. Introducing a first-of-its-kind Koin-test harness just to cover a 3-line `if` and a 4-line `when` would be disproportionate new test infrastructure with zero precedent to justify it.

Instead, I extract both decisions into `internal` top-level **pure functions** in `Plugin.kt` (no `Module`/`Koin`/`Scope` receiver, plain parameters in, plain value out):

```kotlin
internal fun emailConfigElementOrNull(config: JsonObject): JsonElement? =
    config["email"]?.takeUnless { it == JsonNull }

internal fun selectEmailFeature(emailsService: EmailsService?, usersRepo: UsersRepo): EmailFeature =
    when (emailsService) {
        null -> DisabledEmailFeature(usersRepo)
        else -> EmailFeatureService(emailsService, usersRepo)
    }
```

`setupDI` becomes a thin caller of both:

```kotlin
override fun Module.setupDI(config: JsonObject) {
    val emailConfigElement = emailConfigElementOrNull(config)
    if (emailConfigElement != null) {
        single { get<Json>().decodeFromJsonElement(EmailConfig.serializer(), emailConfigElement) }
        single { SmtpEmailService(get<EmailConfig>()) }
        single<EmailsService> { get<SmtpEmailService>() }
    }
    single<EmailFeature> { selectEmailFeature(getOrNull<EmailsService>(), get<UsersRepo>()) }
    singleWithRandomQualifier<ApplicationRoutingConfigurator.Element> {
        EmailRoutingsConfigurator(get())
    }
}
```

Both functions are byte-for-byte behaviorally identical to the inlined version — this is a pure refactor for testability, not a logic change, and is exactly the kind of "free implementation-detail choice for Architecture to make and record" `002-planning.md`'s own closing section pre-authorizes. `internal` visibility matches this module's existing precedent (`EmailAttachmentDataSource` is `internal class` in `commonMain`, directly instantiated by `commonTest` in the same package — confirmed Kotlin/KMP `internal`-across-test-friend-sourceset access already works in this module). Both functions are directly unit-tested with zero Koin involvement (see `PluginTest.kt` below); the remaining `single { }` glue is covered by compile-time type-checking (`agents/CODING.md`'s mandatory post-change build) plus manual boot verification against `server/sample.config.json` (SMTP enabled after this task) and `server/dev.config.json`/`local.config.json` (no `email` key) — see "Testability decision" section below for why this residual gap is acceptable and not a blocking "untestable functionality" flag.

**New import needed in `Plugin.kt` vs. `002-planning.md`'s draft:** `kotlinx.serialization.json.JsonElement` (needed for the explicit `JsonElement?` return type on `emailConfigElementOrNull` — the inlined version never needed an explicit type). All other imports are unchanged from `002-planning.md` item 6.

Coding may instead place these two functions in a separate file (e.g. `PluginHelpers.kt`) if preferred — I recommend keeping them in `Plugin.kt` itself since each is 2–6 lines, single-purpose, and has exactly one caller (`Plugin.setupDI`); a separate file would be minor unrequested ceremony. Either placement satisfies the test stubs below as long as the functions stay `internal` (not `private`) and package `dev.inmo.wishlist.features.email.server` (needed for `PluginTest.kt`'s import-free same-package access, in the `emailConfigElementOrNull` case) — actually `PluginTest.kt` lives in the **same** package (`dev.inmo.wishlist.features.email.server`), so if Coding does split the functions into a second file, they must stay in that same package for the test file below to compile without changes.

### 2. Test-double package placement: reuse `services`, no new `testutils` package

`002-planning.md` (item 10, closing paragraph) flags that no `UsersRepo`/`EmailsService` test double exists anywhere in the repo and defers "where exactly" to Architecture. I place both new fakes (`FakeUsersRepo.kt`, `FakeEmailsService.kt`) in `features/email/server/src/commonTest/kotlin/services/` under package `dev.inmo.wishlist.features.email.server.services` — the **same** package as the production `services/` classes and the same package the two test files that consume them (`DisabledEmailFeatureTest.kt`, `EmailFeatureServiceTest.kt`) already live in per `002-planning.md`. This matches this module's only existing test-infra precedent (`EmailAttachmentDataSourceTest.kt` lives in `services`, same-package as what it tests, no `testutils`/`fixtures` subpackage anywhere in the repo) and avoids inventing a new package convention for a single-feature test suite.

### 3. `FakeUsersRepo` design: extend `MapCRUDRepo`, mirroring `CacheUsersRepo`'s own composition shape

`dev.inmo.micro_utils.repos.MapCRUDRepo` (abstract class, `dev.inmo:micro_utils.repos.inmemory` artifact) is **already on `features/email/server`'s test classpath transitively** — no new `build.gradle` dependency is needed. Chain of `api` dependencies: `features/common/common/build.gradle` → `api libs.microutils.repos.cache` → (verified by reading `MicroUtils/repos/cache/build.gradle`) `api internalProject("micro_utils.repos.inmemory")` → `features/users/common` (`api project(":wishlist.features.common.common")`) → `features/email/server` (`api project(":wishlist.features.users.common")`). This is the exact same artifact `CacheUsersRepo.kt` already uses for `MapKeyValueRepo`.

`UsersRepo`'s own production cache implementation, `CacheUsersRepo`, is composed exactly as `class CacheUsersRepo(...) : UsersRepo, FullCRUDCacheRepo<RegisteredUser, UserId, NewUser>(...) { override suspend fun getUserByUsername(...) = ... }` — implement the base `CRUDRepo` surface via a library base class, add `UsersRepo`'s one extra method by hand. `FakeUsersRepo` mirrors this shape exactly, swapping `FullCRUDCacheRepo` for the simpler `MapCRUDRepo`:

```kotlin
package dev.inmo.wishlist.features.email.server.services

import dev.inmo.micro_utils.repos.MapCRUDRepo
import dev.inmo.wishlist.features.users.common.models.NewUser
import dev.inmo.wishlist.features.users.common.models.RegisteredUser
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.users.common.models.Username
import dev.inmo.wishlist.features.users.common.repo.UsersRepo

/**
 * In-memory [UsersRepo] test double backed by [MapCRUDRepo] (`dev.inmo:micro_utils.repos.inmemory`,
 * already on this module's test classpath transitively via `features/common/common`'s
 * `api libs.microutils.repos.cache` dependency — no new `build.gradle` entry needed). Mirrors the
 * composition shape of the production [dev.inmo.wishlist.features.users.common.repo.CacheUsersRepo]:
 * a library base class supplies the [dev.inmo.micro_utils.repos.CRUDRepo] surface, [getUserByUsername]
 * is added by hand.
 *
 * IDs are assigned sequentially starting one past the highest seeded [UserId]. [getUserByUsername]
 * performs a linear scan via [getAll] — acceptable for the small fixtures used in these tests.
 *
 * @param initialUsers Users the repo is pre-seeded with, keyed by their [UserId].
 */
internal class FakeUsersRepo(
    initialUsers: Map<UserId, RegisteredUser> = emptyMap()
) : UsersRepo, MapCRUDRepo<RegisteredUser, UserId, NewUser>(initialUsers.toMutableMap()) {

    private var nextId: Long = (initialUsers.keys.maxOfOrNull { it.long } ?: 0L) + 1L

    override suspend fun updateObject(newValue: NewUser, id: UserId, old: RegisteredUser): RegisteredUser =
        old.copy(username = newValue.username, email = newValue.email)

    override suspend fun createObject(newValue: NewUser): Pair<UserId, RegisteredUser> {
        val id = UserId(nextId++)
        return id to RegisteredUser(id, newValue.username, newValue.email)
    }

    override suspend fun getUserByUsername(username: Username): RegisteredUser? =
        getAll().values.firstOrNull { it.username == username }
}
```

`FakeEmailsService` — call-recording double for `EmailsService`, mirroring the existing invocation-counter style already used in `SmtpEmailServiceDisabledTest.kt` (`var invocations = 0`):

```kotlin
package dev.inmo.wishlist.features.email.server.services

import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.email.server.EmailsService
import dev.inmo.wishlist.features.email.server.models.EmailAttachment

/**
 * Call-recording [EmailsService] test double. [sendText] appends every call's arguments to
 * [sendTextCalls] before returning [result]; the other two methods only increment a call counter
 * (no test in this module currently asserts their arguments — [EmailFeatureService] only ever calls
 * [sendText]).
 *
 * @param result Value returned by every method on this instance.
 */
internal class FakeEmailsService(
    private val result: Boolean = true
) : EmailsService {

    /** One recorded call's arguments. */
    data class SendTextCall(val recipient: Email, val subject: String, val text: String)

    /** Recorded arguments from every [sendText] call, in call order. */
    val sendTextCalls = mutableListOf<SendTextCall>()

    /** Number of times [sendTextWithAttachments] was invoked. */
    var sendTextWithAttachmentsCallCount: Int = 0
        private set

    /** Number of times [sendHtml] was invoked. */
    var sendHtmlCallCount: Int = 0
        private set

    override suspend fun sendText(recipient: Email, subject: String, text: String): Boolean {
        sendTextCalls += SendTextCall(recipient, subject, text)
        return result
    }

    override suspend fun sendTextWithAttachments(
        recipient: Email,
        subject: String,
        text: String,
        attachments: List<EmailAttachment>
    ): Boolean {
        sendTextWithAttachmentsCallCount++
        return result
    }

    override suspend fun sendHtml(recipient: Email, subject: String, html: String): Boolean {
        sendHtmlCallCount++
        return result
    }
}
```

### 4. `SmtpEmailServiceDisabledTest.kt`: keep filename/class name (no rename)

Considered renaming to something like `SmtpEmailServiceTest.kt` since the class only tests the blank-host no-op now (no more "disabled via null smtp" cases). Decided **against** renaming: `002-planning.md` item 8 already resolves the semantic drift via a KDoc rewrite ("Verifies `SmtpEmailService`'s remaining no-op trigger: a configured-but-blank SMTP host"), a rename adds git churn with no functional benefit, and the task's own instructions refer to this file by its current name throughout. Kept as-is.

### 5. `UpdateStoredEmail.kt` naming and no-dedicated-test-file decision — confirmed, not deviated

Keeping `002-planning.md`'s exact naming (`UpdateStoredEmail.kt`, `internal suspend fun updateStoredEmail(...)`) — it is descriptive, matches this repo's file-per-top-level-declaration convention (see `agents/CODING.md`'s "Typed definition & accessor helpers" section using the same one-file-one-purpose style), and I found no better name. Also confirming `002-planning.md`'s call not to add a dedicated `UpdateStoredEmailTest.kt`: it is a 2-line `internal` function with exactly one branch (`?: return false`), and both branches are exercised at least once by each of `DisabledEmailFeatureTest` and `EmailFeatureServiceTest`'s `setMyEmail*` cases (found+persist, found+clear-to-null, not-found) below — transitive coverage is complete, a dedicated file would just duplicate those same assertions a third time.

## Testability decision: `Plugin.kt`'s residual DI-wiring glue

After extracting `emailConfigElementOrNull`/`selectEmailFeature` (deviation 1), the only untested code left in `Plugin.kt` is the `single { }`/`singleWithRandomQualifier { }` registration calls themselves — pure Koin DSL glue with no conditional logic of its own (the one `if` and one `when` that existed are now both pure-function-tested). This matches **every other Koin `single { }` registration in this entire codebase** — I confirmed via search that zero DI-wiring tests exist anywhere in the repo (not just in `features/email`). Coverage for this residual glue is: (a) the mandatory post-change module build (`agents/CODING.md` — `./gradlew :wishlist.features.email.server:build`, which type-checks every `get<T>()`/`single<T>()` call site), and (b) manual boot-time verification — start the server against `server/sample.config.json` (SMTP block present → `GET /email/enabled` should report `true`) and confirm `server/dev.config.json`/`local.config.json` (no `email` key, and `dev.config.json` doesn't even load the email plugin) still boot cleanly. This is not a new gap introduced by this task — it is the established, repo-wide convention, and I am not flagging it as a blocking "untestable functionality" item under `agents/ARCHITECTURE.md`'s escalation clause, since a stub/test IS possible here (I could add `koin-test` and spin up a container) but doing so would be materially disproportionate new infrastructure for a first-of-its-kind pattern with no other precedent. Flagging this explicitly per the task's request; not blocking Coding.

## Genuinely untestable functionality (flagged, not blocking)

**`SmtpEmailService`'s live-SMTP `Transport.send` success path** (all three `sendText`/`sendTextWithAttachments`/`sendHtml`, when the host is non-blank) remains genuinely untestable via automated unit test — it requires a real or stubbed SMTP server, which this module has no infrastructure for. This is **pre-existing**, not introduced by this task: `features/email/README.md`'s current "Architecture Notes" already documents this exact carve-out ("the live-SMTP success path is intentionally not unit-tested — external integration — verified via build + the manual `POST /email/sendTest` path"), and it was already accepted by whatever prior task added `SmtpEmailService`. Nothing in this task's 7 points touches that code path (only the `isFeatureEnabled()` removal and the dead-code half of the `send()` guard change), so no new untestable surface is introduced. Restating it here only for completeness per this step's mandate to flag untestable functionality explicitly.

No other planned change (across all 14 items of `002-planning.md` plus my two extractions) is untestable — every other new/changed unit is a pure `suspend fun`/pure `fun` over injectable interfaces or plain data, fully coverable by the test stubs below.

## Open questions

**None.** `002-planning.md` closed Q1–Q4 from `001-planning.md` with operator-confirmed answers, and I found no further ambiguity while turning it into concrete test specs. The two deviations above are implementation-detail decisions squarely within Architecture's discretion (explicitly pre-authorized by `002-planning.md`'s closing section), not points requiring operator escalation.

---

## Concrete file list for Coding

**Modified (production):**
1. `features/email/server/src/commonMain/kotlin/EmailConfig.kt`
2. `features/email/server/src/commonMain/kotlin/services/SmtpEmailService.kt`
3. `features/email/server/src/commonMain/kotlin/services/EmailFeatureService.kt`
4. `features/email/server/src/commonMain/kotlin/Plugin.kt`
5. `server/sample.config.json`
6. `features/email/README.md` (Overview / Models / Architecture Notes only — `## Operator Notes` untouched; see notes below)

**New (production):**
7. `features/email/server/src/commonMain/kotlin/services/DisabledEmailFeature.kt`
8. `features/email/server/src/commonMain/kotlin/services/UpdateStoredEmail.kt`

**Rewritten (test):**
9. `features/email/server/src/commonTest/kotlin/services/SmtpEmailServiceDisabledTest.kt`

**New (test):**
10. `features/email/server/src/commonTest/kotlin/EmailConfigTest.kt`
11. `features/email/server/src/commonTest/kotlin/PluginTest.kt`
12. `features/email/server/src/commonTest/kotlin/services/DisabledEmailFeatureTest.kt`
13. `features/email/server/src/commonTest/kotlin/services/EmailFeatureServiceTest.kt`
14. `features/email/server/src/commonTest/kotlin/services/FakeUsersRepo.kt`
15. `features/email/server/src/commonTest/kotlin/services/FakeEmailsService.kt`

**Confirmed unchanged (do not touch):** `EmailFeature.kt`, `EmailsService.kt`, `configurators/EmailRoutingsConfigurator.kt`, `models/EmailAttachment.kt`, its two existing tests (`EmailAttachmentTest.kt`, `EmailAttachmentDataSourceTest.kt`), `jvmMain/kotlin/JVMPlugin.kt`, `features/email/client/**`, `features/email/common/**`, `server/dev.config.json`, `server/local.config.json` (gitignored).

---

## Production code specs (for Coding — see `002-planning.md` items 1–5, 7 for full KDoc/rationale; only deltas from that plan are repeated here)

### `EmailConfig.kt` — per `002-planning.md` item 1, verbatim. No deviation.

### `services/SmtpEmailService.kt` — per `002-planning.md` item 2, verbatim. No deviation.

### `services/DisabledEmailFeature.kt` — per `002-planning.md` item 3, verbatim. No deviation.

### `services/UpdateStoredEmail.kt` — per `002-planning.md` item 4, verbatim. No deviation.

### `services/EmailFeatureService.kt` — per `002-planning.md` item 5, verbatim. No deviation. (Confirms dropping the WIP's erroneous second `emailsService.sendTestEmail(recipient)` call — `EmailsService` has no such method; `sendTestEmail` issues exactly one `sendText(...)` call.)

### `Plugin.kt` — per `002-planning.md` item 6, **with deviation 1 above** (two pure functions extracted). Full final file:

```kotlin
package dev.inmo.wishlist.features.email.server

import dev.inmo.micro_utils.koin.singleWithRandomQualifier
import dev.inmo.micro_utils.ktor.server.configurators.ApplicationRoutingConfigurator
import dev.inmo.micro_utils.startup.plugin.StartPlugin
import dev.inmo.wishlist.features.email.server.configurators.EmailRoutingsConfigurator
import dev.inmo.wishlist.features.email.server.services.DisabledEmailFeature
import dev.inmo.wishlist.features.email.server.services.EmailFeatureService
import dev.inmo.wishlist.features.email.server.services.SmtpEmailService
import dev.inmo.wishlist.features.users.common.repo.UsersRepo
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import org.koin.core.Koin
import org.koin.core.module.Module

/**
 * Common (JVM) startup plugin for the email server feature.
 *
 * Registers in the shared DI graph:
 * - [EmailConfig], [SmtpEmailService], and the [SmtpEmailService]-backed [EmailsService] binding —
 *   ALL THREE registered together, and ONLY when [emailConfigElementOrNull] returns non-null (a
 *   non-null `"email"` JSON object is present at `config["email"]` in the root server config). When
 *   absent (or explicitly JSON `null`), none of the three are registered — "email delivery disabled"
 *   is a DI-graph-shape fact rather than a runtime value check.
 * - [EmailFeature], always registered unconditionally. [selectEmailFeature] resolves
 *   `getOrNull<EmailsService>()`: present → [EmailFeatureService]; absent → [DisabledEmailFeature]
 *   (both wrapping [UsersRepo], so per-user email storage keeps working even with SMTP disabled).
 * - [EmailRoutingsConfigurator], registered with a random qualifier so Ktor picks it up automatically.
 *
 * [emailConfigElementOrNull] and [selectEmailFeature] are extracted as pure, Koin-free top-level
 * functions specifically so the two conditional decisions this plugin makes are directly unit
 * testable (see `PluginTest`) without needing a Koin test harness (none exists in this repo).
 *
 * The email server module targets JVM only (`mppJavaProject`), so Jakarta Mail references are safe
 * in this `commonMain` source set — the same approach used by `currency/server` with OkHttp.
 */
object Plugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        val emailConfigElement = emailConfigElementOrNull(config)
        if (emailConfigElement != null) {
            single { get<Json>().decodeFromJsonElement(EmailConfig.serializer(), emailConfigElement) }
            single { SmtpEmailService(get<EmailConfig>()) }
            single<EmailsService> { get<SmtpEmailService>() }
        }
        single<EmailFeature> { selectEmailFeature(getOrNull<EmailsService>(), get<UsersRepo>()) }
        singleWithRandomQualifier<ApplicationRoutingConfigurator.Element> {
            EmailRoutingsConfigurator(get())
        }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
    }
}

/**
 * Returns the nested `"email"` JSON object from [config], or `null` when the key is absent or
 * explicitly JSON `null` — both are treated as "email feature disabled" (see [EmailConfig] and
 * [Plugin]). Pure and Koin-free so it is directly unit-testable (see `PluginTest`).
 *
 * @param config Root server config JSON object.
 * @return The `"email"` [JsonElement] to decode [EmailConfig] from, or `null` to skip registration.
 */
internal fun emailConfigElementOrNull(config: JsonObject): JsonElement? =
    config["email"]?.takeUnless { it == JsonNull }

/**
 * Selects the [EmailFeature] implementation to register: [EmailFeatureService] wrapping
 * [emailsService] when present, or [DisabledEmailFeature] when SMTP is not configured. Pure and
 * Koin-free so it is directly unit-testable (see `PluginTest`) independently of the `single { }`
 * registration that calls it.
 *
 * @param emailsService Result of `getOrNull<EmailsService>()` at the call site, or `null`.
 * @param usersRepo Repository passed to whichever implementation is selected.
 * @return A ready-to-use [EmailFeature] implementation.
 */
internal fun selectEmailFeature(emailsService: EmailsService?, usersRepo: UsersRepo): EmailFeature =
    when (emailsService) {
        null -> DisabledEmailFeature(usersRepo)
        else -> EmailFeatureService(emailsService, usersRepo)
    }
```

### `server/sample.config.json` — per `002-planning.md` item 11, verbatim: replace line 26 (`"smtp": null,`) with:

```json
  "email": {
    "smtp": {
      "host": "smtp.example.com",
      "port": 587,
      "username": "user",
      "password": "pass",
      "from": "noreply@example.com",
      "useTls": true,
      "useSsl": false
    }
  },
```

(Verified current file: line 26 is exactly `"smtp": null,`, sitting between the `"plugins"` array close and `"filesFolder"`. `server/dev.config.json` and `server/local.config.json` confirmed independently by this agent — grep for `email`/`smtp` returns no match in `dev.config.json`; `local.config.json` is gitignored and out of scope. No change to either.)

---

## Test stubs / specs (full detail — Coding implements these directly)

### 1. `EmailConfig.kt` — is it unit-testable in isolation? **Yes, fully.**

`EmailConfig`/`SmtpConfig` are plain `@Serializable` data classes with no dependency on Koin or `Plugin`. Decoding is a pure `Json.decodeFromJsonElement(EmailConfig.serializer(), element)` call using a plain default `Json` instance — the presence/absence/`JsonNull` gating around *whether* to attempt this decode is `Plugin`'s concern (covered separately by `emailConfigElementOrNull`'s own tests in `PluginTest.kt` below), not `EmailConfig`'s. These tests build the nested `element` directly (i.e., what `config["email"]`'s value would be), never touching Koin.

**New file `features/email/server/src/commonTest/kotlin/EmailConfigTest.kt`:**

```kotlin
package dev.inmo.wishlist.features.email.server

import dev.inmo.wishlist.features.email.common.models.Email
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/**
 * Verifies [EmailConfig]'s decode contract in isolation — a plain [Json] instance, no Koin/Plugin
 * involved. [EmailConfig] is decoded from the *nested* `"email"` [kotlinx.serialization.json.JsonElement]
 * (the value already extracted from `config["email"]` by [Plugin]), not the whole root config object.
 */
class EmailConfigTest {

    private val json = Json

    /** Fully-populated `smtp` object decodes every field exactly, including non-default ones. */
    @Test
    fun decodesAllFieldsWhenFullyPopulated() {
        val element = buildJsonObject {
            putJsonObject("smtp") {
                put("host", "smtp.example.com")
                put("port", 25)
                put("username", "user")
                put("password", "pass")
                put("from", "noreply@example.com")
                put("useTls", false)
                put("useSsl", true)
            }
        }

        val config = json.decodeFromJsonElement(EmailConfig.serializer(), element)

        assertEquals("smtp.example.com", config.smtp.host)
        assertEquals(25, config.smtp.port)
        assertEquals("user", config.smtp.username)
        assertEquals("pass", config.smtp.password)
        assertEquals(Email("noreply@example.com"), config.smtp.from)
        assertEquals(false, config.smtp.useTls)
        assertEquals(true, config.smtp.useSsl)
    }

    /** Omitted optional `smtp` fields fall back to [SmtpConfig]'s declared defaults. */
    @Test
    fun appliesSmtpDefaultsForOmittedOptionalFields() {
        val element = buildJsonObject {
            putJsonObject("smtp") {
                put("host", "smtp.example.com")
                put("from", "noreply@example.com")
            }
        }

        val config = json.decodeFromJsonElement(EmailConfig.serializer(), element)

        assertEquals(587, config.smtp.port)
        assertNull(config.smtp.username)
        assertNull(config.smtp.password)
        assertEquals(true, config.smtp.useTls)
        assertEquals(false, config.smtp.useSsl)
    }

    /** [EmailConfig.smtp] has no default: an `"email"` object with no `"smtp"` key fails to decode. */
    @Test
    fun decodingFailsWhenSmtpKeyIsMissing() {
        val element = buildJsonObject {}

        assertFailsWith<SerializationException> {
            json.decodeFromJsonElement(EmailConfig.serializer(), element)
        }
    }

    /** `smtp` is non-nullable: an explicit `"smtp": null` also fails to decode. */
    @Test
    fun decodingFailsWhenSmtpIsExplicitNull() {
        val element = buildJsonObject {
            put("smtp", JsonNull)
        }

        assertFailsWith<SerializationException> {
            json.decodeFromJsonElement(EmailConfig.serializer(), element)
        }
    }
}
```

### 2. `Plugin.kt`'s conditional DI wiring — decision restated: pure-function-tested, glue not separately tested (see "Testability decision" section above).

**New file `features/email/server/src/commonTest/kotlin/PluginTest.kt`:**

```kotlin
package dev.inmo.wishlist.features.email.server

import dev.inmo.wishlist.features.email.server.services.DisabledEmailFeature
import dev.inmo.wishlist.features.email.server.services.EmailFeatureService
import dev.inmo.wishlist.features.email.server.services.FakeEmailsService
import dev.inmo.wishlist.features.email.server.services.FakeUsersRepo
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Verifies the two pure decision functions [Plugin.kt] uses to drive its conditional Koin wiring:
 * [emailConfigElementOrNull] (whether the `"email"` config block is present) and
 * [selectEmailFeature] (which [EmailFeature] implementation is selected). Both are pure — no Koin
 * container is constructed anywhere in this file. The `single { }` registration calls that wire
 * these functions' results into Koin are intentionally not separately unit tested — see
 * `003-architecturing.md`'s "Testability decision" section.
 */
class PluginTest {

    // --- emailConfigElementOrNull ---

    /** No `"email"` key at all (other root keys present) — must return `null`. */
    @Test
    fun emailConfigElementOrNullReturnsNullWhenKeyAbsent() {
        val config = buildJsonObject {
            put("host", JsonPrimitive("0.0.0.0"))
            put("port", JsonPrimitive(8196))
        }
        assertNull(emailConfigElementOrNull(config))
    }

    /** `"email": null` (key present, value explicitly JSON null) — must return `null`. */
    @Test
    fun emailConfigElementOrNullReturnsNullWhenKeyIsJsonNull() {
        val config = buildJsonObject {
            put("email", JsonNull)
        }
        assertNull(emailConfigElementOrNull(config))
    }

    /** A present, non-null `"email"` object — must return that exact element. */
    @Test
    fun emailConfigElementOrNullReturnsElementWhenKeyPresentAndNonNull() {
        val emailBlock = buildJsonObject {
            putJsonObject("smtp") {
                put("host", JsonPrimitive("smtp.example.com"))
                put("from", JsonPrimitive("noreply@example.com"))
            }
        }
        val config = buildJsonObject {
            put("email", emailBlock)
        }

        assertEquals(emailBlock, emailConfigElementOrNull(config))
    }

    // --- selectEmailFeature ---

    /** `emailsService == null` — must select [DisabledEmailFeature]. */
    @Test
    fun selectEmailFeatureReturnsDisabledEmailFeatureWhenEmailsServiceNull() = runTest {
        val result = selectEmailFeature(emailsService = null, usersRepo = FakeUsersRepo())
        assertIs<DisabledEmailFeature>(result)
    }

    /** `emailsService != null` — must select [EmailFeatureService], reporting itself enabled. */
    @Test
    fun selectEmailFeatureReturnsEmailFeatureServiceWhenEmailsServicePresent() = runTest {
        val result = selectEmailFeature(emailsService = FakeEmailsService(), usersRepo = FakeUsersRepo())
        assertIs<EmailFeatureService>(result)
        assertTrue(result.isFeatureEnabled())
    }
}
```

### 3. `SmtpEmailService.kt` post-`isFeatureEnabled()`-removal behavior

**Rewritten file `features/email/server/src/commonTest/kotlin/services/SmtpEmailServiceDisabledTest.kt`:**

Exactly which of the 5 current tests are removed/kept, and what's added:

| Test | Action | Reason |
|---|---|---|
| `sendTextReturnsFalseWhenSmtpIsNull` | **REMOVE** | Won't compile — `EmailConfig(smtp = null)` no longer valid |
| `sendHtmlReturnsFalseWhenSmtpIsNull` | **REMOVE** | Same |
| `sendTextWithAttachmentsReturnsFalseWhenSmtpIsNullAndDoesNotInvokeProvider` | **REMOVE** | Same |
| `sendTextReturnsFalseWhenHostIsBlank` | **KEEP/ADAPT** | Already uses a non-null `SmtpConfig(host = "")` — unaffected by the nullability change, only extracted into the shared `blankHostConfig()` helper below |
| `isFeatureEnabledFalseWhenSmtpIsNull` | **REMOVE** | `isFeatureEnabled()` no longer exists on `SmtpEmailService` — nothing left to test for this concern on this class (moved to `DisabledEmailFeatureTest`/`PluginTest`) |
| `sendHtmlReturnsFalseWhenHostIsBlank` | **ADD** | Restores 1:1 breadth against the removed `sendHtml`-null-smtp case, now keyed on blank host |
| `sendTextWithAttachmentsReturnsFalseWhenHostIsBlankAndDoesNotInvokeProvider` | **ADD** | Restores 1:1 breadth against the removed attachments-null-smtp case, including the "must not touch attachment content" assertion |

Full final file:

```kotlin
package dev.inmo.wishlist.features.email.server.services

import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.email.server.EmailConfig
import dev.inmo.wishlist.features.email.server.SmtpConfig
import dev.inmo.wishlist.features.email.server.models.EmailAttachment
import kotlinx.coroutines.test.runTest
import java.io.ByteArrayInputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/**
 * Verifies [SmtpEmailService]'s remaining no-op trigger: a configured-but-blank SMTP host.
 * [EmailConfig.smtp] is non-nullable now, so "no SMTP configured at all" is a DI-graph-shape fact
 * handled by [DisabledEmailFeature] (see `DisabledEmailFeatureTest`), not something this class can
 * represent — every case here constructs [SmtpEmailService] with a real, non-null [EmailConfig]
 * whose host happens to be blank.
 */
class SmtpEmailServiceDisabledTest {

    /** Shared recipient address used by every blank-host assertion. */
    private val recipient = Email("recipient@example.com")

    /** Builds an [EmailConfig] with a blank SMTP host — the only remaining no-op trigger. */
    private fun blankHostConfig() =
        EmailConfig(smtp = SmtpConfig(host = "", from = Email("noreply@example.com")))

    /** `sendText` must return `false` when the configured SMTP host is blank. */
    @Test
    fun sendTextReturnsFalseWhenHostIsBlank() = runTest {
        val service = SmtpEmailService(blankHostConfig())
        assertFalse(service.sendText(recipient, "subject", "body"))
    }

    /** `sendHtml` must return `false` when the configured SMTP host is blank. */
    @Test
    fun sendHtmlReturnsFalseWhenHostIsBlank() = runTest {
        val service = SmtpEmailService(blankHostConfig())
        assertFalse(service.sendHtml(recipient, "subject", "<p>body</p>"))
    }

    /**
     * `sendTextWithAttachments` must return `false` when the configured SMTP host is blank AND must
     * never invoke an attachment's content provider — the no-op path must not touch attachment
     * content at all.
     */
    @Test
    fun sendTextWithAttachmentsReturnsFalseWhenHostIsBlankAndDoesNotInvokeProvider() = runTest {
        val service = SmtpEmailService(blankHostConfig())
        var invocations = 0
        val attachment = EmailAttachment("file.txt", "text/plain") {
            invocations++
            ByteArrayInputStream(ByteArray(0))
        }

        val result = service.sendTextWithAttachments(recipient, "subject", "body", listOf(attachment))

        assertFalse(result)
        assertEquals(0, invocations)
    }
}
```

### 4. `DisabledEmailFeature.kt`

**New file `features/email/server/src/commonTest/kotlin/services/DisabledEmailFeatureTest.kt`:**

```kotlin
package dev.inmo.wishlist.features.email.server.services

import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.users.common.models.RegisteredUser
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.users.common.models.Username
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Verifies [DisabledEmailFeature]: [isFeatureEnabled]/[sendTestEmail] are hard no-ops; [setMyEmail]
 * still persists via [UsersRepo] — storage stays independent of SMTP configuration.
 */
class DisabledEmailFeatureTest {

    private val rootUser = RegisteredUser(UserId(1L), Username("root"))
    private val plainUser = RegisteredUser(UserId(2L), Username("alice"))

    /** Always `false` — trivial no-op. */
    @Test
    fun isFeatureEnabledReturnsFalse() = runTest {
        val feature = DisabledEmailFeature(FakeUsersRepo())
        assertFalse(feature.isFeatureEnabled())
    }

    /** `false` even for a caller who genuinely is root — there is no SMTP transport to use. */
    @Test
    fun sendTestEmailReturnsFalseForRootCaller() = runTest {
        val repo = FakeUsersRepo(mapOf(rootUser.id to rootUser))
        val feature = DisabledEmailFeature(repo)

        assertFalse(feature.sendTestEmail(rootUser.id, Email("recipient@example.com")))
    }

    /** `false` for a non-root caller and for a caller id that doesn't resolve to any user. */
    @Test
    fun sendTestEmailReturnsFalseForNonRootOrMissingCaller() = runTest {
        val repo = FakeUsersRepo(mapOf(plainUser.id to plainUser))
        val feature = DisabledEmailFeature(repo)

        assertFalse(feature.sendTestEmail(plainUser.id, Email("recipient@example.com")))
        assertFalse(feature.sendTestEmail(UserId(999L), Email("recipient@example.com")))
    }

    /** A found user's stored email is updated and the call reports success. */
    @Test
    fun setMyEmailPersistsViaUsersRepoWhenUserFound() = runTest {
        val repo = FakeUsersRepo(mapOf(plainUser.id to plainUser))
        val feature = DisabledEmailFeature(repo)
        val newEmail = Email("alice@example.com")

        val result = feature.setMyEmail(plainUser.id, newEmail)

        assertTrue(result)
        assertEquals(newEmail, repo.getById(plainUser.id)?.email)
    }

    /** Passing `null` clears a previously-stored email address. */
    @Test
    fun setMyEmailClearsStoredEmailWhenPassedNull() = runTest {
        val userWithEmail = plainUser.copy(email = Email("alice@example.com"))
        val repo = FakeUsersRepo(mapOf(userWithEmail.id to userWithEmail))
        val feature = DisabledEmailFeature(repo)

        val result = feature.setMyEmail(userWithEmail.id, null)

        assertTrue(result)
        assertNull(repo.getById(userWithEmail.id)?.email)
    }

    /** A caller id that doesn't resolve to any user reports failure and updates nothing. */
    @Test
    fun setMyEmailReturnsFalseWhenUserNotFound() = runTest {
        val feature = DisabledEmailFeature(FakeUsersRepo())

        assertFalse(feature.setMyEmail(UserId(999L), Email("alice@example.com")))
    }
}
```

### 5. `UpdateStoredEmail.kt` — no dedicated test file (per `002-planning.md`, confirmed above); covered transitively by `DisabledEmailFeatureTest`'s and `EmailFeatureServiceTest`'s `setMyEmail*` cases (found+persist, found+clear-to-null, not-found — all present in both files).

### 6. `EmailFeatureService.kt`

**New file `features/email/server/src/commonTest/kotlin/services/EmailFeatureServiceTest.kt`:**

```kotlin
package dev.inmo.wishlist.features.email.server.services

import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.users.common.models.RegisteredUser
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.users.common.models.Username
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Verifies [EmailFeatureService]: [isFeatureEnabled] reports `emailsService != null`;
 * [sendTestEmail] enforces the root-only guard before delegating exactly one
 * [EmailsService.sendText] call; [setMyEmail] persists via [UsersRepo] regardless of
 * [EmailsService] availability (storage stays independent of SMTP).
 */
class EmailFeatureServiceTest {

    private val rootUser = RegisteredUser(UserId(1L), Username("root"))
    private val plainUser = RegisteredUser(UserId(2L), Username("alice"))
    private val recipient = Email("recipient@example.com")

    @Test
    fun isFeatureEnabledTrueWhenEmailsServiceNonNull() = runTest {
        val service = EmailFeatureService(FakeEmailsService(), FakeUsersRepo())
        assertTrue(service.isFeatureEnabled())
    }

    @Test
    fun isFeatureEnabledFalseWhenEmailsServiceNull() = runTest {
        val service = EmailFeatureService(null, FakeUsersRepo())
        assertFalse(service.isFeatureEnabled())
    }

    /** `emailsService == null` short-circuits before the root check even runs its course. */
    @Test
    fun sendTestEmailReturnsFalseWhenEmailsServiceNullEvenForRootCaller() = runTest {
        val repo = FakeUsersRepo(mapOf(rootUser.id to rootUser))
        val service = EmailFeatureService(null, repo)

        assertFalse(service.sendTestEmail(rootUser.id, recipient))
    }

    /** Root caller + present `emailsService` → exactly one `sendText` call with the fixed subject/text; result is `sendText`'s own `true`. */
    @Test
    fun sendTestEmailDelegatesToSendTextForRootCallerAndReturnsTrueResult() = runTest {
        val repo = FakeUsersRepo(mapOf(rootUser.id to rootUser))
        val emailsService = FakeEmailsService(result = true)
        val service = EmailFeatureService(emailsService, repo)

        val result = service.sendTestEmail(rootUser.id, recipient)

        assertTrue(result)
        assertEquals(1, emailsService.sendTextCalls.size)
        val call = emailsService.sendTextCalls.single()
        assertEquals(recipient, call.recipient)
        assertEquals("Test email from WishlistApp", call.subject)
        assertEquals(
            "This is a test email sent from WishlistApp to verify SMTP configuration.",
            call.text
        )
    }

    /** Same as above but `sendText` fails — the `false` result must propagate through unchanged. */
    @Test
    fun sendTestEmailDelegatesToSendTextForRootCallerAndReturnsFalseResult() = runTest {
        val repo = FakeUsersRepo(mapOf(rootUser.id to rootUser))
        val emailsService = FakeEmailsService(result = false)
        val service = EmailFeatureService(emailsService, repo)

        val result = service.sendTestEmail(rootUser.id, recipient)

        assertFalse(result)
        assertEquals(1, emailsService.sendTextCalls.size)
    }

    /** Non-root caller → `false`, and `sendText` must never be invoked. */
    @Test
    fun sendTestEmailReturnsFalseForNonRootCallerAndDoesNotCallSendText() = runTest {
        val repo = FakeUsersRepo(mapOf(plainUser.id to plainUser))
        val emailsService = FakeEmailsService()
        val service = EmailFeatureService(emailsService, repo)

        val result = service.sendTestEmail(plainUser.id, recipient)

        assertFalse(result)
        assertEquals(0, emailsService.sendTextCalls.size)
    }

    /** Caller id resolves to no user → `false`, and `sendText` must never be invoked. */
    @Test
    fun sendTestEmailReturnsFalseWhenCallerNotFound() = runTest {
        val emailsService = FakeEmailsService()
        val service = EmailFeatureService(emailsService, FakeUsersRepo())

        val result = service.sendTestEmail(UserId(999L), recipient)

        assertFalse(result)
        assertEquals(0, emailsService.sendTextCalls.size)
    }

    /** Storage keeps working even when `emailsService` is `null` — proves the documented independence. */
    @Test
    fun setMyEmailPersistsViaUsersRepoRegardlessOfEmailsService() = runTest {
        val repo = FakeUsersRepo(mapOf(plainUser.id to plainUser))
        val service = EmailFeatureService(null, repo)
        val newEmail = Email("alice@example.com")

        val result = service.setMyEmail(plainUser.id, newEmail)

        assertTrue(result)
        assertEquals(newEmail, repo.getById(plainUser.id)?.email)
    }

    @Test
    fun setMyEmailReturnsFalseWhenUserNotFound() = runTest {
        val service = EmailFeatureService(FakeEmailsService(), FakeUsersRepo())

        assertFalse(service.setMyEmail(UserId(999L), Email("alice@example.com")))
    }
}
```

### 7. Test doubles — full Kotlin already given in "Deviations" items 2/3 above (`FakeUsersRepo.kt`, `FakeEmailsService.kt`). Both `internal class`, package `dev.inmo.wishlist.features.email.server.services`, no new `build.gradle` dependency required (verified transitive availability of `dev.inmo:micro_utils.repos.inmemory`'s `MapCRUDRepo`).

---

## `features/email/README.md` — intended updates for Coding to apply (I did not edit this file — Architecture makes no code/doc changes per `agents/ARCHITECTURE.md`)

Base content per `002-planning.md` item 14 (Overview / Models table / Architecture Notes rewrites — adopt verbatim), **plus** these two additions reflecting deviation 1:

- **Architecture Notes — DI placement paragraph**: append a sentence noting that the `"email"`-key-presence check and the `EmailFeature`-implementation choice are each implemented as a small `internal` pure function (`emailConfigElementOrNull`, `selectEmailFeature`) in `Plugin.kt` specifically so they can be unit-tested (`PluginTest.kt`) without a Koin test harness — this repo has none, and the two conditionals are the only branchy logic in the plugin.
- **Models table**: no new row needed for the two helper functions (they're implementation details of `Plugin`, not part of the feature's public model surface) — only the `EmailConfig`/`DisabledEmailFeature`/`EmailFeatureService`/`SmtpEmailService` row edits from `002-planning.md` item 14 apply.
- **Sample config block**: exact JSON given in the "Production code specs" section above (`server/sample.config.json`), matches `002-planning.md` item 14's intended replacement.

---

## Status

**READY for Coding.** No source file was modified in this step — only this report was written, and it will be committed alone (`003-architecturing.md` only), per `agents/GIT.md`. All test stubs above are concrete, complete Kotlin (not just names/descriptions) so Coding can transcribe them directly and focus effort on the production code + wiring them together, then run `./gradlew :wishlist.features.email.server:build` per `agents/CODING.md`.
