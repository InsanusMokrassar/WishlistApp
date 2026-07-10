Model: Claude Sonnet 5 (claude-sonnet-5), assigned by the orchestrator for this invocation.
Execution time: ~20 minutes (~1200s) wall-clock (task folder's `002-planning.md` written 14:12, this
report begun immediately after and finished ~14:32, same day).
Tokens used: not precisely instrumented from inside this agent. Rough estimate: ~55k-70k tokens (reading
`agents/ALL.md`/`local.ALL.md`/`ARCHITECTURE.md`/`CODING.md`/`GIT.md`/`PROTOCOL.md`, both prior planning
reports, all seven affected source/doc/config files at their actual current content, plus independent
`grep` cross-checks of every claim in `002-planning.md` before transcribing it).
Changed files: `agents/task/10.07.2026_07.55.45-816cb326-39a3-4582-8f48-3c5a24b0eac8/003-architecturing.md`
(this file) only. No source, doc, or config file was edited by this step — Architecturing is not
authorized to edit them (`agents/ARCHITECTURE.md` line 3).

---

# Architecturing: fallout cleanup after the operator's hand-edit (fix/44-email)

## 0. Verification method

Per this task's brief, `002-planning.md` is READY and already contains a detailed, line-anchored,
file-by-file plan; my job was to transcribe/formalize it into concrete test stubs and confirm nothing
was missed, not to re-derive the design. Before transcribing, I independently re-read every file
`002-planning.md` cites at its **current** actual content (not trusting the planning report's quoted
text) and re-ran the load-bearing greps myself:

- Read `features/email/server/src/commonMain/kotlin/services/EmailFeatureService.kt`,
  `features/email/server/src/commonMain/kotlin/Plugin.kt` (both hand-edited, uncommitted, out of scope
  to revert per Root's resolution),
  `features/email/server/src/commonTest/kotlin/services/EmailFeatureServiceTest.kt`,
  `features/email/server/src/commonTest/kotlin/PluginTest.kt`,
  `features/email/server/src/commonTest/kotlin/services/DisabledEmailFeatureTest.kt`,
  `features/email/server/src/commonTest/kotlin/services/FakeEmailsService.kt`,
  `features/email/server/src/commonMain/kotlin/configurators/EmailRoutingsConfigurator.kt`,
  `features/email/README.md`, root `README.md` (lines 75-120), `server/dev.config.json`,
  `server/sample.config.json` in full.
- Independently ran `grep -rn "selectEmailFeature"`, `grep -rn "EmailFeatureService("`,
  `grep -rn "requireRoot"` across the whole repo (not just `features/email/`).

Result: **every factual claim in `002-planning.md` checked out exactly** against current source —
line numbers, quoted text, call-site counts, import lists, all matched verbatim. No material gap or
compile-error-it-didn't-catch was found. One tangential observation (not a plan gap) is noted in §7.
The rest of this report formalizes `002-planning.md`'s dispositions into the exact file contents Coding
needs, per `agents/ARCHITECTURE.md`'s test-stub requirement.

## 1. `EmailFeatureServiceTest.kt` — final concrete test list

**Verified current state** (128 lines): 9 `@Test` methods. Three call sites pass `null` as the first
constructor argument — lines 38, 46, 113 — which no longer compiles against the hand-edited
non-nullable `EmailFeatureService(emailsService: EmailsService, usersRepo: UsersRepo)` constructor.

### Delete outright (2 tests)

- **`isFeatureEnabledFalseWhenEmailsServiceNull`** (current lines 36-40). Reason: asserted
  `isFeatureEnabled()` is `false` when constructed with `null` — that code path no longer exists on this
  class (`isFeatureEnabled()` is now a hardcoded `= true`). The "disabled → `isFeatureEnabled()` false"
  behavior is already fully covered by `DisabledEmailFeatureTest.isFeatureEnabledReturnsFalse` (that
  file's lines 28-32, verified by direct read) — no coverage gap from deleting this.
- **`sendTestEmailReturnsFalseWhenEmailsServiceNullEvenForRootCaller`** (current lines 42-49, including
  its one-line doc comment on line 42). Reason: asserted `sendTestEmail` returns `false` for a root
  caller when `emailsService` is `null` — already fully covered by
  `DisabledEmailFeatureTest.sendTestEmailReturnsFalseForRootCaller` (that file's lines 35-41, verified by
  direct read), which tests the identical scenario against the class that now owns the disabled state.
  No coverage gap from deleting this.

### Rework in place (1 test)

- **`setMyEmailPersistsViaUsersRepoRegardlessOfEmailsService`** (current lines 109-120) → rename to
  **`setMyEmailPersistsViaUsersRepoForFoundUser`**, replace `EmailFeatureService(null, repo)` with
  `EmailFeatureService(FakeEmailsService(), repo)` (the only constructible shape now). This is *not*
  redundant with any `DisabledEmailFeatureTest` case — `DisabledEmailFeatureTest` exercises a different
  class (`DisabledEmailFeature`), so deleting this would leave `EmailFeatureService.setMyEmail`'s
  found-user-succeeds path completely untested (the only other `setMyEmail` test in this file,
  `setMyEmailReturnsFalseWhenUserNotFound`, covers the not-found path only).

### Rename only, body unchanged (1 test)

- **`isFeatureEnabledTrueWhenEmailsServiceNonNull`** (current lines 30-34) → rename to
  **`isFeatureEnabledAlwaysReturnsTrue`**. It already constructs with `FakeEmailsService()` (never
  `null`) so it compiles as-is, but its old name implies a null-vs-non-null contrast that no longer
  exists on this class.

### Untouched (5 tests)

`sendTestEmailDelegatesToSendTextForRootCallerAndReturnsTrueResult`,
`sendTestEmailDelegatesToSendTextForRootCallerAndReturnsFalseResult`,
`sendTestEmailReturnsFalseForNonRootCallerAndDoesNotCallSendText`,
`sendTestEmailReturnsFalseWhenCallerNotFound`, `setMyEmailReturnsFalseWhenUserNotFound` — all already
construct with a non-null `FakeEmailsService()`, compile unchanged, no rework needed.

**Net effect: 9 → 7 test methods.** No import changes needed in this file (`Email`, `RegisteredUser`,
`UserId`, `Username`, `runTest`, `Test`, `assertEquals`, `assertFalse`, `assertTrue` all remain used by
the 7 surviving methods).

### Exact resulting file content (full file, for Coding to apply directly)

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
 * Verifies [EmailFeatureService]: [EmailFeatureService.isFeatureEnabled] always returns `true`
 * (this class is only ever constructed with a real, non-null `EmailsService` — see
 * `DisabledEmailFeatureTest` for the SMTP-disabled no-op path); [EmailFeatureService.sendTestEmail]
 * enforces the root-only guard before delegating exactly one `EmailsService.sendText` call;
 * [EmailFeatureService.setMyEmail] persists via `UsersRepo` for a found user.
 */
class EmailFeatureServiceTest {

    /** Fixture user whose username is the privileged "root" account. */
    private val rootUser = RegisteredUser(UserId(1L), Username("root"))

    /** Fixture user whose username is an ordinary, non-root account. */
    private val plainUser = RegisteredUser(UserId(2L), Username("alice"))

    /** Shared test-email recipient used by every `sendTestEmail` assertion. */
    private val recipient = Email("recipient@example.com")

    @Test
    fun isFeatureEnabledAlwaysReturnsTrue() = runTest {
        val service = EmailFeatureService(FakeEmailsService(), FakeUsersRepo())
        assertTrue(service.isFeatureEnabled())
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

    /** A found user's stored email is updated and persisted via `UsersRepo`, exercising `EmailFeatureService.setMyEmail` directly (the SMTP-disabled path is covered separately by `DisabledEmailFeatureTest`). */
    @Test
    fun setMyEmailPersistsViaUsersRepoForFoundUser() = runTest {
        val repo = FakeUsersRepo(mapOf(plainUser.id to plainUser))
        val service = EmailFeatureService(FakeEmailsService(), repo)
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

## 2. `PluginTest.kt` — final concrete cleanup

**Verified current state** (84 lines): 5 `@Test` methods — 3 for `emailConfigElementOrNull`, 2 for
`selectEmailFeature`. `grep -rn "selectEmailFeature"` (whole repo) confirms `selectEmailFeature` has
**zero** production call sites (the operator's Plugin.kt edit replaced its only call site with an inline
expression) and exactly these two test call sites — it is genuinely dead code once `Plugin.kt`'s own
copy is deleted (§3).

### Delete (2 tests)

- `selectEmailFeatureReturnsDisabledEmailFeatureWhenEmailsServiceNull` (current lines 70-74)
- `selectEmailFeatureReturnsEmailFeatureServiceWhenEmailsServicePresent` (current lines 76-82)
- Also delete the `// --- selectEmailFeature ---` section header (current line 67) and the blank line
  at 68. Net range to remove: current lines 67-82 (the closing `}` of the class on line 83 stays).

Nothing is reworked here — once `selectEmailFeature` itself is deleted from `Plugin.kt`, there is no
function left to test, and the inline `getOrNull<EmailsService>()?.let{...}?:...` expression that
replaces it is exercised only through Koin wiring, which this file's own class doc already documents as
intentionally not unit-tested (no Koin test harness exists in this repo) — no new test is owed.

### Imports that become unused (7 lines to delete)

Verified by tracing every remaining test body after the deletion: only the three
`emailConfigElementOrNull` tests remain, using `buildJsonObject`/`JsonNull`/`JsonPrimitive`/`put`/
`putJsonObject`/`assertNull`/`assertEquals`/`Test`. None of the survivors reference `DisabledEmailFeature`,
`EmailFeatureService`, `FakeEmailsService`, `FakeUsersRepo`, `runTest`, `assertIs`, or `assertTrue`.
Delete these 7 import lines (current lines 3-7, 15, 17):

```
import dev.inmo.wishlist.features.email.server.services.DisabledEmailFeature
import dev.inmo.wishlist.features.email.server.services.EmailFeatureService
import dev.inmo.wishlist.features.email.server.services.FakeEmailsService
import dev.inmo.wishlist.features.email.server.services.FakeUsersRepo
import kotlinx.coroutines.test.runTest
import kotlin.test.assertIs
import kotlin.test.assertTrue
```

### Class-level KDoc rewrite (current lines 19-27)

Currently frames the file as testing "the two pure decision functions" (`emailConfigElementOrNull` and
`selectEmailFeature`) — now false. Replace with:

```
/**
 * Verifies `emailConfigElementOrNull` — the pure decision function `Plugin.kt` uses to drive its
 * conditional Koin wiring for whether the `"email"` config block is present. Pure — no Koin
 * container is constructed anywhere in this file. The `single { }` registration calls that wire
 * this function's result (and the inline `EmailFeature`-selection logic) into Koin are
 * intentionally not separately unit tested — see
 * `agents/task/10.07.2026_06.40.48-2407c3c3-e09a-4139-976a-305652d931a3/003-architecturing.md`'s
 * "Testability decision" section.
 */
```

### Exact resulting file content (full file, for Coding to apply directly)

```kotlin
package dev.inmo.wishlist.features.email.server

import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Verifies `emailConfigElementOrNull` — the pure decision function `Plugin.kt` uses to drive its
 * conditional Koin wiring for whether the `"email"` config block is present. Pure — no Koin
 * container is constructed anywhere in this file. The `single { }` registration calls that wire
 * this function's result (and the inline `EmailFeature`-selection logic) into Koin are
 * intentionally not separately unit tested — see
 * `agents/task/10.07.2026_06.40.48-2407c3c3-e09a-4139-976a-305652d931a3/003-architecturing.md`'s
 * "Testability decision" section.
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
}
```

## 3. `Plugin.kt` — delete dead `selectEmailFeature` function + fix its own KDoc

**Verified current state**: the `single<EmailFeature>` block (lines 47-51) is the operator's hand-edit,
already inline and out of scope to change:

```kotlin
        single<EmailFeature> {
            getOrNull<EmailsService>() ?.let {
                EmailFeatureService(it, get<UsersRepo>())
            } ?: DisabledEmailFeature(get<UsersRepo>())
        }
```

`selectEmailFeature` (current lines 73-87) is unchanged since the prior task and now has zero callers
(confirmed via repo-wide `grep -rn "selectEmailFeature"` — only its own definition and the two
`PluginTest.kt` cases being deleted in §2 reference it; no production code calls it). `emailConfigElementOrNull`
(lines 70-71) is confirmed **unaffected**: still called at line 41 in production (`val emailConfigElement
= emailConfigElementOrNull(config)`) and still has its 3 `PluginTest.kt` tests — keep it exactly as-is.

### Edit A — delete the `selectEmailFeature` function and its KDoc entirely

Remove current lines 73-87 in full:

```kotlin
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

No import changes result from this deletion: `DisabledEmailFeature`, `EmailFeatureService`, and
`UsersRepo` remain imported and used (by the still-present inline `single<EmailFeature>` block);
`EmailsService`, `EmailFeature`, and `DisabledEmailFeature`/`EmailFeatureService` are all in the same
`dev.inmo.wishlist.features.email.server`(`.services`) package tree already resolved by existing
imports — verified no import in the current file exists solely for `selectEmailFeature`.

### Edit B — object-level KDoc, `EmailFeature` registration bullet (current lines 27-29)

Before:
```
 * - [EmailFeature], always registered unconditionally. [selectEmailFeature] resolves
 *   `getOrNull<EmailsService>()`: present → [EmailFeatureService]; absent → [DisabledEmailFeature]
 *   (both wrapping [UsersRepo], so per-user email storage keeps working even with SMTP disabled).
```

After:
```
 * - [EmailFeature], always registered unconditionally via an inline `single<EmailFeature>` block
 *   that resolves `getOrNull<EmailsService>()`: present → [EmailFeatureService]; absent →
 *   [DisabledEmailFeature] (both wrapping [UsersRepo], so per-user email storage keeps working even
 *   with SMTP disabled).
```

### Edit C — object-level KDoc, "why helpers extracted" paragraph (current lines 32-34)

Before:
```
 * [emailConfigElementOrNull] and [selectEmailFeature] are extracted as pure, Koin-free top-level
 * functions specifically so the two conditional decisions this plugin makes are directly unit
 * testable (see `PluginTest`) without needing a Koin test harness (none exists in this repo).
```

After:
```
 * [emailConfigElementOrNull] is extracted as a pure, Koin-free top-level function specifically so the
 * `"email"` config-key-presence decision is directly unit testable (see `PluginTest`) without needing
 * a Koin test harness (none exists in this repo). The [EmailFeature]-implementation choice
 * (present-[EmailsService] vs [DisabledEmailFeature]) is simple enough that it stays inline in the
 * `single<EmailFeature>` block rather than being extracted to its own named helper.
```

`setupDI`/`startPlugin` bodies and `emailConfigElementOrNull` itself stay untouched.

## 4. `EmailFeatureService.kt` — exact KDoc fix

**Verified current state**: constructor is `EmailFeatureService(private val emailsService: EmailsService,
private val usersRepo: UsersRepo)` (non-nullable), `isFeatureEnabled()` is `= true` hardcoded,
`sendTestEmail` calls `emailsService.sendText(...)` directly with no null-guard — all confirmed by direct
read. The KDoc above the class, the `@param emailsService` line, `isFeatureEnabled`'s doc, and one
clause of `sendTestEmail`'s doc still describe the old nullable/checks-null behavior and are now
self-contradictory against the code beneath them. This is a KDoc-only fix — no code line changes.

### Edit A — class-level KDoc (current lines 9-30)

Before:
```kotlin
/**
 * Server-side [EmailFeature] implementation that unifies SMTP delivery and user-email persistence.
 *
 * [emailsService] stays nullable so this class remains directly constructible (e.g. in unit tests)
 * without an SMTP transport — [isFeatureEnabled] reports that state via `emailsService != null`. In
 * the DI graph wired by [dev.inmo.wishlist.features.email.server.Plugin], `EmailFeatureService` is
 * only ever constructed with a **non-null** [emailsService]: when no [EmailsService] is registered
 * (SMTP not configured), [DisabledEmailFeature] is substituted for the whole [EmailFeature] binding
 * instead of passing `null` in here. The nullable type is retained anyway, per the operator's
 * explicit instruction and for direct-construction testability.
 *
 * Resolves the caller's user record from [usersRepo] to enforce access rules and to perform
 * email-address storage updates:
 * - [sendTestEmail] verifies [callerId] is root AND that [emailsService] is present before
 *   delegating SMTP delivery.
 * - [setMyEmail] updates the caller's stored email address via [updateStoredEmail] — independent of
 *   [emailsService].
 *
 * @param emailsService SMTP delivery service used for sends, or `null` when constructed without one
 *   (never `null` via the production DI wiring — see class doc).
 * @param usersRepo User repository used for privilege checking and email-address persistence.
 */
```

After:
```kotlin
/**
 * Server-side [EmailFeature] implementation that unifies SMTP delivery and user-email persistence.
 *
 * [emailsService] is always a real, non-null [EmailsService] — [dev.inmo.wishlist.features.email.server.Plugin]
 * only ever constructs this class when one is registered (SMTP configured). When no [EmailsService] is
 * registered (SMTP not configured), [DisabledEmailFeature] is substituted for the whole [EmailFeature]
 * binding instead. Because of that, [isFeatureEnabled] always returns `true`.
 *
 * Resolves the caller's user record from [usersRepo] to enforce access rules and to perform
 * email-address storage updates:
 * - [sendTestEmail] verifies [callerId] is root before delegating SMTP delivery via [emailsService].
 * - [setMyEmail] updates the caller's stored email address via [updateStoredEmail] — independent of
 *   [emailsService].
 *
 * @param emailsService SMTP delivery service used for sends. Always non-null — see class doc.
 * @param usersRepo User repository used for privilege checking and email-address persistence.
 */
```

### Edit B — `isFeatureEnabled()` doc (current lines 39-43)

Before:
```kotlin
    /**
     * Returns whether an SMTP delivery service is available.
     *
     * @return `true` when [emailsService] is non-null; `false` otherwise.
     */
```

After:
```kotlin
    /**
     * Returns whether an SMTP delivery service is available.
     *
     * @return Always `true` — this class is only ever constructed with a real [emailsService]; see
     *   [DisabledEmailFeature] for the SMTP-disabled no-op path.
     */
```

### Edit C — `sendTestEmail`'s doc (current lines 46-57)

Before:
```kotlin
    /**
     * Sends a test email to [recipient] if [callerId] belongs to the root account and an SMTP
     * delivery service is available.
     *
     * Returns `false` immediately when the caller is not found, is not root, or [emailsService] is
     * `null`.
     *
     * @param callerId Caller whose username is checked against the root account.
     * @param recipient Target address for the test message.
     * @return `true` when delivery succeeded; `false` when the caller lacks privilege, SMTP is
     *   unavailable, or SMTP delivery fails.
     */
```

After:
```kotlin
    /**
     * Sends a test email to [recipient] if [callerId] belongs to the root account.
     *
     * Returns `false` immediately when the caller is not found or is not root.
     *
     * @param callerId Caller whose username is checked against the root account.
     * @param recipient Target address for the test message.
     * @return `true` when delivery succeeded; `false` when the caller lacks privilege or SMTP
     *   delivery fails.
     */
```

`setMyEmail`'s doc (unchanged — still accurate) and the `rootUsername` val's KDoc (unchanged — still
accurate) need no edits. No code lines change in this file, only the three doc blocks above.

## 5. Doc/config edits — exact before/after text

### (a) `features/email/README.md`

Never touches the empty `## Operator Notes` section. Four edits, all verified against current source
(`EmailRoutingsConfigurator.kt`, `Plugin.kt`, `EmailFeatureService.kt`) rather than the README's own
prior text:

**1. Models table, `EmailFeatureService` row (current line 41).**

Before:
```
| `EmailFeatureService` | `email/server` | Server `EmailFeature` impl; wraps a nullable `EmailsService?` + `UsersRepo`. `isFeatureEnabled()` reports `emailsService != null`; `sendTestEmail` enforces root-only access before delegating; `setMyEmail` persists the caller's email address independently of `emailsService`. In the DI graph, only ever constructed with a non-null `EmailsService` — see `DisabledEmailFeature`. |
```

After:
```
| `EmailFeatureService` | `email/server` | Server `EmailFeature` impl; wraps a non-nullable `EmailsService` + `UsersRepo`. `isFeatureEnabled()` always returns `true` — this class is only ever constructed by `Plugin` when a real `EmailsService` exists; `sendTestEmail` enforces root-only access before delegating; `setMyEmail` persists the caller's email address. See `DisabledEmailFeature` for the substituted no-op implementation used when SMTP is not configured. |
```

**2. Architecture Notes, "DI-graph-shape 'disabled' state" bullet, ending clause (current line 51).**

Before (full bullet, changed portion is the final sentence):
```
- **DI-graph-shape "disabled" state:** `EmailConfig`, `SmtpEmailService`, and the `EmailsService` binding are registered together, conditionally, only when `config["email"]` is present and non-null — implemented via the pure, Koin-free helper `emailConfigElementOrNull(config)` in `Plugin.kt` (unit-tested directly in `PluginTest`, with no Koin test harness needed since none exists in this repo). `EmailFeature` is always registered unconditionally; it resolves `getOrNull<EmailsService>()` via another pure helper, `selectEmailFeature(emailsService, usersRepo)`, and picks `EmailFeatureService` (present) or `DisabledEmailFeature` (absent).
```

After:
```
- **DI-graph-shape "disabled" state:** `EmailConfig`, `SmtpEmailService`, and the `EmailsService` binding are registered together, conditionally, only when `config["email"]` is present and non-null — implemented via the pure, Koin-free helper `emailConfigElementOrNull(config)` in `Plugin.kt` (unit-tested directly in `PluginTest`, with no Koin test harness needed since none exists in this repo). `EmailFeature` is always registered unconditionally; it resolves `getOrNull<EmailsService>()` directly inside an inline `single<EmailFeature>` block and picks `EmailFeatureService` (present) or `DisabledEmailFeature` (absent).
```

**3. Architecture Notes, "Root guard" bullet (current line 55).** This is the one concrete inaccuracy
independent of the operator's hand-edit — `EmailRoutingsConfigurator.kt` was re-read directly to confirm:
there is no `requireRoot()` function anywhere in the repo; both `sendTest` and `myEmail` call only
`getCallerUserIdOrAnswerUnauthorized()`; both failure branches respond `HttpStatusCode.InternalServerError`
(500), never 403.

Before:
```
- **Root guard:** `POST /email/sendTest` uses `requireRoot()` — mirrors `AdminRoutingsConfigurator.requireAdmin()`. Responds 401 (no token) or 403 (non-root). `PUT /email/myEmail` uses only `getCallerUserIdOrAnswerUnauthorized()` (self-service).
```

After:
```
- **Root guard:** Both `POST /email/sendTest` and `PUT /email/myEmail` use only `getCallerUserIdOrAnswerUnauthorized()` at the routing layer (self-service auth — 401 on missing/invalid bearer token). Root-only enforcement for `sendTest` happens inside `EmailFeatureService.sendTestEmail` by comparing `caller.username.string` against the literal `"root"`; on failure there — whether the caller isn't root or the SMTP send itself failed — the route responds `500 Internal Server Error` (the two failure modes are indistinguishable at the HTTP layer).
```

**4. Architecture Notes, "DI placement" bullet (current line 57).**

Before:
```
- **DI placement:** Server Plugin wires `EmailConfig → SmtpEmailService → EmailsService` as one conditional trio (see above), always registers `EmailFeature` (`EmailFeatureService` or `DisabledEmailFeature`, selected by `selectEmailFeature`), and registers `EmailRoutingsConfigurator` (with random qualifier) unconditionally. `jvmMain/JVMPlugin` is a thin delegator listed in `sample.config.json`. The `"email"`-key-presence check and the `EmailFeature`-implementation choice are each implemented as a small `internal` pure function (`emailConfigElementOrNull`, `selectEmailFeature`) in `Plugin.kt` specifically so they can be unit-tested (`PluginTest.kt`) without a Koin test harness — this repo has none, and these two conditionals are the only branchy logic in the plugin.
```

After:
```
- **DI placement:** Server Plugin wires `EmailConfig → SmtpEmailService → EmailsService` as one conditional trio (see above), always registers `EmailFeature` (`EmailFeatureService` or `DisabledEmailFeature`, selected inline by a `getOrNull<EmailsService>()` check in the `single<EmailFeature>` block), and registers `EmailRoutingsConfigurator` (with random qualifier) unconditionally. `jvmMain/JVMPlugin` is a thin delegator listed in `sample.config.json`. The `"email"`-key-presence check is implemented as a small `internal` pure function (`emailConfigElementOrNull`) in `Plugin.kt` specifically so it can be unit-tested (`PluginTest.kt`) without a Koin test harness — this repo has none. The `EmailFeature`-implementation choice (`getOrNull<EmailsService>()?.let { EmailFeatureService(it, get()) } ?: DisabledEmailFeature(get())`) is simple enough that it stays inline rather than being extracted to its own testable helper.
```

### (b) Root `README.md`

Two edits, verified against current `server/sample.config.json` (read in full).

**1. "Server configuration" Key Fields table** — insert a new row immediately after the
`openExchangeRatesRefreshTTLMillis` row (current line 98), before the table's trailing blank line:

Before (last row of the table, line 98):
```
| `openExchangeRatesRefreshTTLMillis` | currency-rates cache lifetime in milliseconds |
```

After (add this row directly beneath it, as the new last row):
```
| `openExchangeRatesRefreshTTLMillis` | currency-rates cache lifetime in milliseconds |
| `email` | Nested SMTP config object (`{ smtp: { host, port, username?, password?, from, useTls, useSsl } }`) that enables the email feature's SMTP test-email delivery; omit the key (or set it to JSON `null`) to disable SMTP while per-user email-address storage (`PUT /email/myEmail`) keeps working |
```

**2. "Production deployment" table, `server/sample.config.json` row, "What to change before use" cell
(current line 109).**

Before:
```
| `server/sample.config.json` | Production server config template. Serves the web bundle from `/static`, stores uploads under `/data/uploaded_files`, and points the database at the `postgres` service host. | Replace the `database` `url` / `username` / `password` (placeholders `TEST_DB` / `TEST_USERNAME` / `TEST_PASSWORD`), set `publicHost` to your real public address, set `openExchangeRatesAppId` if you use the currency feature, and review `enableRegistration`. Mount the finished file into the container at `/config.json`. |
```

After:
```
| `server/sample.config.json` | Production server config template. Serves the web bundle from `/static`, stores uploads under `/data/uploaded_files`, and points the database at the `postgres` service host. | Replace the `database` `url` / `username` / `password` (placeholders `TEST_DB` / `TEST_USERNAME` / `TEST_PASSWORD`), set `publicHost` to your real public address, set `openExchangeRatesAppId` if you use the currency feature, configure (or omit) the `email` block if you want SMTP-based test-email delivery, and review `enableRegistration`. Mount the finished file into the container at `/config.json`. |
```

### (c) `server/dev.config.json`

Verified: `dev.config.json`'s `plugins` array currently ends with
`"dev.inmo.wishlist.features.booking.server.JVMPlugin"` and is entirely missing
`"dev.inmo.wishlist.features.email.server.JVMPlugin"` (present in `sample.config.json`, positioned after
`booking.server.JVMPlugin`, before `deeplinks.server.JVMPlugin` — `deeplinks` itself is a pre-existing,
unrelated gap in `dev.config.json` that predates this branch and stays out of scope). Do **not** add a
top-level `"email"` key — its absence is intentional (disables real SMTP sends in local dev, matches the
feature's documented "omit the key to disable" contract) — and do not touch `server/local.config.json`
(git-ignored, personal) or `server/sample.config.json` (already correct).

Before (`plugins` array, current lines 13-23):
```json
  "plugins": [
    "dev.inmo.wishlist.features.common.server.JVMPlugin",
    "dev.inmo.wishlist.features.sample.server.JVMPlugin",
    "dev.inmo.wishlist.features.users.server.JVMPlugin",
    "dev.inmo.wishlist.features.auth.server.JVMPlugin",
    "dev.inmo.wishlist.features.wishlist.server.JVMPlugin",
    "dev.inmo.wishlist.features.files.server.JVMPlugin",
    "dev.inmo.wishlist.features.admin.server.JVMPlugin",
    "dev.inmo.wishlist.features.currency.server.JVMPlugin",
    "dev.inmo.wishlist.features.booking.server.JVMPlugin"
  ],
```

After:
```json
  "plugins": [
    "dev.inmo.wishlist.features.common.server.JVMPlugin",
    "dev.inmo.wishlist.features.sample.server.JVMPlugin",
    "dev.inmo.wishlist.features.users.server.JVMPlugin",
    "dev.inmo.wishlist.features.auth.server.JVMPlugin",
    "dev.inmo.wishlist.features.wishlist.server.JVMPlugin",
    "dev.inmo.wishlist.features.files.server.JVMPlugin",
    "dev.inmo.wishlist.features.admin.server.JVMPlugin",
    "dev.inmo.wishlist.features.currency.server.JVMPlugin",
    "dev.inmo.wishlist.features.booking.server.JVMPlugin",
    "dev.inmo.wishlist.features.email.server.JVMPlugin"
  ],
```

## 6. Untestable functionality

None. Every change in this cleanup is: two test deletions (each fully redundant with an existing
`DisabledEmailFeatureTest` case, verified above), one test rework (constructor-argument swap plus a
rename, exercising a real, already-existing code path), one dead-function deletion with its two
now-pointless tests, four KDoc corrections (doc-only, code unchanged), and three doc/config text edits.
There is no new production behavior, no new interface, and no new branch of logic that would need a new
test — every surviving/reworked test stub is specified in full above (§1, §2). Per `agents/ARCHITECTURE.md`
this is stated explicitly rather than left implicit: **nothing here is genuinely untestable; nothing
requires operator escalation.**

## 7. One tangential observation (not a plan gap, not adding scope)

While independently re-running `grep -rn "requireRoot"` across the whole repo (not just `features/email/`)
to verify §5(a)'s Root-guard README fix, I found one more hit outside the email feature entirely:
`features/ui/adminPanel/src/commonMain/kotlin/ui/AdminPanelViewModel.kt:52` — a comment reading
"...authorization is enforced by the server's `requireRoot` guard." This function does not exist anywhere
in the repo either (same finding as the email README's stale bullet), so this comment is independently
stale. This is **outside** this task's scope: it is not part of `features/email/`, was not touched by
this branch (`features/ui/adminPanel` changes on this branch are unrelated, per `001-planning.md` §2),
and `002-planning.md` did not flag it (its sweep was correctly scoped to email-related docs per the
Root's own candidate list in `PROMPT.md`). I am not adding it to Coding's file list — flagging only so
the operator has visibility if they want a separate follow-up task for it. This does not change my
"spec is ready" conclusion below.

## 8. Final concrete file list for Coding

1. `features/email/server/src/commonTest/kotlin/services/EmailFeatureServiceTest.kt` — apply §1 (full
   file content given).
2. `features/email/server/src/commonTest/kotlin/PluginTest.kt` — apply §2 (full file content given).
3. `features/email/server/src/commonMain/kotlin/Plugin.kt` — apply §3 (delete `selectEmailFeature` +
   its KDoc; fix two KDoc paragraphs). The `setupDI`/`startPlugin` bodies (operator's hand-edit) stay
   untouched.
4. `features/email/server/src/commonMain/kotlin/services/EmailFeatureService.kt` — apply §4 (three
   KDoc blocks only). All executable code (operator's hand-edit) stays untouched.
5. `features/email/README.md` — apply §5(a)'s four edits. Do not touch `## Operator Notes` (empty).
6. `README.md` (root) — apply §5(b)'s two edits.
7. `server/dev.config.json` — apply §5(c)'s one-line array addition.

Model-tier guidance (per `agents/SHORTCUTS.md`, restated from `002-planning.md` §6, unchanged): files 5
and 6 are pure `.md` content → **haiku** tier. Files 1-4 are `.kt` → normal Coding-role tier
(`sonnet`/`opus`/`fable` priority). File 7 is JSON, not `.md` — the haiku mandate doesn't strictly apply;
trivial one-line addition, no design judgment, safe to fold into either pass.

After the `.kt` edits (files 1-4), Coding must run `ast-index rebuild` per `agents/ALL.md` (not needed
for files 5-7, `.md`/JSON only). Per `agents/CODING.md`, after this coding session run
`./gradlew :wishlist.features.email.server:build` (or the closest matching module build task covering
both `commonMain` and `commonTest`) and fix one cycle of any resulting errors. Per `agents/GIT.md`,
Coding commits both its own step report and all seven changed files above (never `git add -A`); per this
task's `PROMPT.md`, the operator explicitly asked for commit **and** push, so push is in scope for
Coding/the end of this cycle here (unlike the default Orchestrator-only push rule).

## 9. Conclusion

`002-planning.md` is READY and, on independent re-verification of every cited file and grep, **fully
accurate** — no material gap, no missed compile error, no test disposition that fails to hold up. This
report transcribes its dispositions into concrete, copy-pasteable test stubs and file contents so Coding
has zero remaining design decisions. The spec is complete and ready for Coding.
