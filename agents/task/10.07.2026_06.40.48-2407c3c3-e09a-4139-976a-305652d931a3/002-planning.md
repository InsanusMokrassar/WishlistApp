Model: Sonnet 5 (claude-sonnet-5)
Execution time: ~1500 (this step only, estimate — not exposed to this agent)
Tokens used: not exposed to this agent
Changed files: agents/task/10.07.2026_06.40.48-2407c3c3-e09a-4139-976a-305652d931a3/002-planning.md

# 002 — Planning (finalization)

## Tooling note

`ast-index` (v3.45.0) is on `PATH`. Used `ast-index usages SmtpEmailService` and `ast-index search
"sendTestEmail"` in this step to re-verify call sites before finalizing the plan (per `agents/ALL.md`
— grep is not to be used when ast-index can do the job; two earlier lookups in this step were
initially done with `grep` and were re-run through `ast-index` for compliance, with matching
results). **Index-staleness note:** `ast-index usages SmtpEmailService` reports
`Plugin.kt:34` as `single<EmailFeature> { EmailFeatureService(get<SmtpEmailService>(), get<UsersRepo>()) }`,
but a direct `Read` of the current `Plugin.kt` shows line 34 is actually
`single<EmailFeature> { EmailFeatureService(get(), get<UsersRepo>()) }` (the uncommitted WIP's
version). The index appears to predate the uncommitted WIP edit and was not rebuilt (Planning makes
no source edits, so no rebuild obligation fell on this or the prior step). All conclusions in this
report are based on direct `Read` of current file contents, not the stale index output — flagging
this only so Coding knows to run `ast-index rebuild` after making its edits, and doesn't trust a
`usages`/`search` result over the file itself if the two ever disagree again.

## Task understanding (unchanged from 001, restated briefly)

Redesign the email feature's SMTP-configured/disabled state from a runtime `config.smtp != null`
value check into a DI-graph-shape fact: `EmailConfig`/`SmtpEmailService`/`EmailsService` simply do
not exist in Koin when SMTP isn't configured, `EmailConfig.smtp` is non-null whenever those *do*
exist, and `EmailFeature`'s single substitutes a dedicated no-op implementation when
`EmailsService` is absent from the graph, instead of branching on a boolean at call time.

## Open questions asked (001) and operator's answers (this step) — for the record

**Q1 — literal schema change or colloquial reference?**
Operator confirmed **literal schema change (reading b)**: a genuinely new, nested top-level
`"email"` JSON key. `EmailConfig` is decoded from `config["email"]` (the nested `JsonObject`/
`JsonElement`), **not** the whole root config object — this is a deliberate divergence from the
`CurrencyConfig` root-flat-key pattern, gating the whole conditional `single` block on
`config["email"]` being non-null via `?.let { }`/`if` directly on the accessor.

**Q2 — does "absent" include "present-but-null"?**
Operator confirmed: `"email"` will never appear at the config **root** in the general case — its
absence is the documented way to disable the feature. `server/sample.config.json` MUST show the
feature **enabled** (a fully-populated `"email": { "smtp": { ...all required SmtpConfig fields... } }`
block). `server/local.config.json` (gitignored, untracked) is the real-world "omitted" example and
is read-only for this task (confirmed: it neither has an `"email"`/`"smtp"` key nor loads the email
plugin at all — see the config-file verification section below). Resolution adopted: guard on
`config["email"]` being Kotlin-`null` (key absent) **or** `JsonNull` (key present but literally
`null`) — both treated as "disabled" — since the schema is never documented to require an explicit
`null` value; this is defensive belt-and-braces, not a case any tracked config file will actually
exercise going forward.

**Q3 — does `EmailFeatureService.emailsService` ever actually receive `null`, and how does
`isFeatureEnabled()` still report `false` when disabled?**
Operator's correction (verbatim): *"I meant that it should be `DisabledEmailFeature` instead of
`DisabledEmailsService` — it is supposed to replace `EmailFeature` in case when emails service is
not available."* This corrects the **target type** of points 6/7 (stub implements `EmailFeature`,
not `EmailsService`), not the mechanism itself. Root's synthesis, adopted as settled:
- `EmailFeatureService`'s constructor keeps `emailsService: EmailsService?` **nullable**, exactly as
  point 2 literally states — this is for direct-construction/unit-test flexibility, not because the
  DI wiring ever actually passes `null` through it.
- `EmailFeatureService.isFeatureEnabled() = emailsService != null` — correct in all cases, including
  direct construction with a `null` argument.
- In the DI wiring, `single<EmailFeature>` resolves `getOrNull<EmailsService>()`; when `null`, it
  constructs `DisabledEmailFeature(get<UsersRepo>())` **instead of** `EmailFeatureService` at all
  (not `EmailFeatureService(null, ...)`) — so in production `EmailFeatureService.emailsService` is
  never actually null; the nullable type is deliberately retained anyway per point 2's literal
  wording and for testability.

**Q4 — "EmailsFeature" typo, and does the conditional wrap exclude `EmailFeature`'s single?**
Operator confirmed: `"EmailsFeature"` was a typo for `EmailsService`. The conditional block wraps
`EmailConfig`'s single, `SmtpEmailService`'s single, and `single<EmailsService> { get<SmtpEmailService>() }`
together (gated on `config["email"]`, per Q1/Q2). `single<EmailFeature> { ... }` and the routing
configurator's `singleWithRandomQualifier` stay **unconditional** — `EmailFeature` always exists in
the graph; only its *implementation* switches.

No further ambiguity surfaced while finalizing — see "Remaining open questions" at the end.

## Final plan — file by file

### 1. `features/email/server/src/commonMain/kotlin/EmailConfig.kt` — MODIFIED

- `EmailConfig.smtp: SmtpConfig? = null` → `smtp: SmtpConfig` (non-nullable, no default — point 3).
- `SmtpConfig` itself is **unchanged** (still has nullable `username`/`password`, defaults for
  `port`/`useTls`/`useSsl`).
- KDoc rewrite for `EmailConfig`: drop the "disabled/no-op mode" framing (that concept now lives
  entirely at the DI-graph-shape / `DisabledEmailFeature` level, not on this data class). Replace
  the "same config-slice pattern as `CurrencyConfig`" claim — it is **no longer** the same pattern:
  document that `EmailConfig` is decoded from the **nested** `config["email"]` JSON object (not the
  whole root config object), by `Plugin`, only when that key is present and non-null.

Final shape:
```kotlin
/**
 * Email-feature config slice, decoded from the nested `"email"` object in the server config JSON
 * (`config["email"]`) — NOT the whole root config object (unlike the root-flat-key pattern used by
 * e.g. [dev.inmo.wishlist.features.currency.server.CurrencyConfig]).
 *
 * [dev.inmo.wishlist.features.email.server.Plugin] only registers this class's Koin `single` (and,
 * together with it, [dev.inmo.wishlist.features.email.server.services.SmtpEmailService] and the
 * [EmailsService] binding) when the `"email"` key is present and non-null in the root config. When
 * absent, none of the three exist in the DI graph and [EmailFeature] resolves to
 * [dev.inmo.wishlist.features.email.server.services.DisabledEmailFeature] instead — "disabled" is a
 * DI-graph-shape fact, not a value carried on this class.
 *
 * @property smtp SMTP delivery settings. Always present whenever an [EmailConfig] instance exists.
 */
@Serializable
data class EmailConfig(
    val smtp: SmtpConfig
)
```
(`SmtpConfig`'s KDoc `@property host` line referencing "A blank value has the same effect as `null`
on the parent `EmailConfig.smtp`" is now slightly stale phrasing — reword to "A blank value still
disables delivery via the guard in `SmtpEmailService.send`" since `EmailConfig.smtp` itself can no
longer be `null`.)

### 2. `features/email/server/src/commonMain/kotlin/services/SmtpEmailService.kt` — MODIFIED

- **Delete** the `isFeatureEnabled()` member entirely (point 1) — lines 44–50 in the current file.
- Constructor **unchanged**: keep `class SmtpEmailService(private val config: EmailConfig) : EmailsService`.
  (Decision, not operator-specified: simplifying to take `SmtpConfig` directly was considered and
  rejected — keeping `EmailConfig` minimizes the diff, and nothing in the 7 points asks for this
  signature change.)
- `sendTestEmail(recipient: Email): Boolean` (the extra, non-interface, class-only member) —
  **kept as-is, unchanged.** Decision, recorded for the record: after point 2's constructor-type
  change, `EmailFeatureService` can no longer call this method (it only holds an `EmailsService`
  interface reference, which doesn't declare it), so this method becomes *currently uncalled* within
  the module. It is intentionally **not** deleted: point 1 named exactly one member to remove
  (`isFeatureEnabled`) and did not ask for this one to go; deleting more than requested would be
  unrequested scope creep. It remains public API on the concrete class, reachable via
  `get<SmtpEmailService>()` if ever needed directly in the future.
- Private `send(...)` skeleton: replace
  ```kotlin
  val smtp = config.smtp
  if (smtp == null || smtp.host.isBlank()) {
  ```
  with
  ```kotlin
  val smtp = config.smtp
  if (smtp.host.isBlank()) {
  ```
  (the `smtp == null` half is dead code once `EmailConfig.smtp` is non-nullable; the blank-host
  guard is kept as the sole remaining no-op trigger inside this class — nothing in the 7 points asks
  to remove it, and it is real defensive behavior against a configured-but-empty host).
- KDoc updates: class doc's "When `EmailConfig.smtp` is `null` or the host is blank... and
  `isFeatureEnabled` returns `false`" → "When the configured host is blank, all delivery methods are
  no-ops" (drop the null-smtp and `isFeatureEnabled` clauses — both gone). `@param config` doc: "Full
  email config slice decoded from the server config JSON" → "Email config slice decoded from the
  nested `\"email\"` object in the server config JSON (see [EmailConfig])."

### 3. New file: `features/email/server/src/commonMain/kotlin/services/DisabledEmailFeature.kt` — NEW

Implements `EmailFeature` (not `EmailsService` — Q3's naming correction), substituted for
`EmailFeatureService` at the `single<EmailFeature>` site when no `EmailsService` is registered.

```kotlin
package dev.inmo.wishlist.features.email.server.services

import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.email.server.EmailFeature
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.users.common.repo.UsersRepo

/**
 * No-op [EmailFeature] implementation used when SMTP is not configured (no [dev.inmo.wishlist.features.email.server.EmailsService]
 * is registered in the DI graph).
 *
 * Substituted for [EmailFeatureService] by [dev.inmo.wishlist.features.email.server.Plugin]'s
 * `single<EmailFeature>` definition whenever `getOrNull<dev.inmo.wishlist.features.email.server.EmailsService>()`
 * returns `null`. [isFeatureEnabled] and [sendTestEmail] are pure no-ops — there is no SMTP
 * transport to send through. [setMyEmail] is deliberately NOT a no-op: per `features/email/README.md`,
 * per-user email-address storage is intentionally independent of SMTP configuration, so this class
 * still persists the caller's address via [usersRepo], identically to [EmailFeatureService.setMyEmail]
 * (both delegate to the shared [updateStoredEmail] helper).
 *
 * @param usersRepo User repository used to persist the caller's stored email address.
 */
class DisabledEmailFeature(
    private val usersRepo: UsersRepo
) : EmailFeature {

    /**
     * Always returns `false` — SMTP is not configured.
     *
     * @return `false`.
     */
    override suspend fun isFeatureEnabled(): Boolean = false

    /**
     * Always returns `false` — there is no SMTP transport to send a test message through.
     *
     * @param callerId Ignored.
     * @param recipient Ignored.
     * @return `false`.
     */
    override suspend fun sendTestEmail(callerId: UserId, recipient: Email): Boolean = false

    /**
     * Updates or clears the stored email address for [callerId].
     *
     * Delegates to [updateStoredEmail] — identical behavior to [EmailFeatureService.setMyEmail],
     * since storage is independent of SMTP configuration.
     *
     * @param callerId User whose record is updated.
     * @param email New address to store, or `null` to clear the current address.
     * @return `true` when the update was persisted; `false` when the user was not found.
     */
    override suspend fun setMyEmail(callerId: UserId, email: Email?): Boolean =
        updateStoredEmail(usersRepo, callerId, email)
}
```

### 4. New file: `features/email/server/src/commonMain/kotlin/services/UpdateStoredEmail.kt` — NEW

Shared helper extracted so `EmailFeatureService.setMyEmail` and `DisabledEmailFeature.setMyEmail`
implement the documented "storage is always independent of SMTP" behavior from exactly one place,
instead of duplicating three lines verbatim in two files (duplication was considered acceptable
given the small size, but a shared helper removes any chance of the two drifting apart later — this
is the free implementation choice flagged for Architecture in 001, decided here).

```kotlin
package dev.inmo.wishlist.features.email.server.services

import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.users.common.models.NewUser
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.users.common.repo.UsersRepo

/**
 * Updates or clears the stored email address for [callerId] via [usersRepo].
 *
 * Shared by [EmailFeatureService.setMyEmail] and [DisabledEmailFeature.setMyEmail] — per-user
 * email-address storage is intentionally independent of SMTP configuration (see
 * `features/email/README.md`), so both [dev.inmo.wishlist.features.email.server.EmailFeature]
 * implementations must persist through this identical path.
 *
 * @param usersRepo User repository used to look up and update the caller's record.
 * @param callerId User whose record is updated.
 * @param email New address to store, or `null` to clear the current address.
 * @return `true` when the update was persisted; `false` when the user was not found.
 */
internal suspend fun updateStoredEmail(usersRepo: UsersRepo, callerId: UserId, email: Email?): Boolean {
    val user = usersRepo.getById(callerId) ?: return false
    return usersRepo.update(callerId, NewUser(user.username, email)) != null
}
```

### 5. `features/email/server/src/commonMain/kotlin/services/EmailFeatureService.kt` — MODIFIED

```kotlin
package dev.inmo.wishlist.features.email.server.services

import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.email.server.EmailFeature
import dev.inmo.wishlist.features.email.server.EmailsService
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.users.common.repo.UsersRepo

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
class EmailFeatureService(
    private val emailsService: EmailsService?,
    private val usersRepo: UsersRepo
) : EmailFeature {

    private val rootUsername = "root"

    /**
     * Returns whether an SMTP delivery service is available.
     *
     * @return `true` when [emailsService] is non-null; `false` otherwise.
     */
    override suspend fun isFeatureEnabled(): Boolean = emailsService != null

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
    override suspend fun sendTestEmail(callerId: UserId, recipient: Email): Boolean {
        val caller = usersRepo.getById(callerId) ?: return false
        if (caller.username.string != rootUsername) return false
        val service = emailsService ?: return false
        return service.sendText(
            recipient = recipient,
            subject = "Test email from WishlistApp",
            text = "This is a test email sent from WishlistApp to verify SMTP configuration."
        )
    }

    /**
     * Updates or clears the stored email address for [callerId].
     *
     * Delegates to [updateStoredEmail] — identical to [DisabledEmailFeature.setMyEmail].
     *
     * @param callerId User whose record is updated.
     * @param email New address to store, or `null` to clear the current address.
     * @return `true` when the update was persisted; `false` when the user was not found.
     */
    override suspend fun setMyEmail(callerId: UserId, email: Email?): Boolean =
        updateStoredEmail(usersRepo, callerId, email)
}
```

Note vs. the uncommitted WIP: the WIP's `sendTestEmail` called `emailsService.sendText(...)` **and
then also** `emailsService.sendTestEmail(recipient)` — the latter doesn't exist on the `EmailsService`
interface (won't compile) and was never asked for by any of the 7 points. This plan drops that
second call entirely; `sendTestEmail` issues exactly one `sendText(...)` call, as it did before the
WIP (confirmed as settled in 001, restated here for Coding's benefit).

### 6. `features/email/server/src/commonMain/kotlin/Plugin.kt` — MODIFIED

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
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import org.koin.core.Koin
import org.koin.core.module.Module

/**
 * Common (JVM) startup plugin for the email server feature.
 *
 * Registers in the shared DI graph:
 * - [EmailConfig], [SmtpEmailService], and the [SmtpEmailService]-backed [EmailsService] binding —
 *   ALL THREE registered together, and ONLY when a non-null `"email"` JSON object is present at
 *   `config["email"]` in the root server config. When the key is absent (or explicitly JSON `null`),
 *   none of the three are registered — "email delivery disabled" is a DI-graph-shape fact rather
 *   than a runtime value check.
 * - [EmailFeature], always registered unconditionally. Resolves [EmailsService] via `getOrNull`:
 *   present → wraps it (and [UsersRepo]) in [EmailFeatureService]; absent → substitutes
 *   [DisabledEmailFeature] (also wrapping [UsersRepo], so per-user email storage keeps working even
 *   with SMTP disabled).
 * - [EmailRoutingsConfigurator], registered with a random qualifier so Ktor picks it up automatically.
 *
 * The email server module targets JVM only (`mppJavaProject`), so Jakarta Mail references are safe
 * in this `commonMain` source set — the same approach used by `currency/server` with OkHttp.
 */
object Plugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        val emailConfigElement = config["email"]
        if (emailConfigElement != null && emailConfigElement != JsonNull) {
            single { get<Json>().decodeFromJsonElement(EmailConfig.serializer(), emailConfigElement) }
            single { SmtpEmailService(get<EmailConfig>()) }
            single<EmailsService> { get<SmtpEmailService>() }
        }
        single<EmailFeature> {
            val emailsService = getOrNull<EmailsService>()
            when (emailsService) {
                null -> DisabledEmailFeature(get<UsersRepo>())
                else -> EmailFeatureService(emailsService, get<UsersRepo>())
            }
        }
        singleWithRandomQualifier<ApplicationRoutingConfigurator.Element> {
            EmailRoutingsConfigurator(get())
        }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
    }
}
```

Notes on this file:
- `config["email"]` works directly because `kotlinx.serialization.json.JsonObject` implements
  `Map<String, JsonElement>` — `get("email")` (the `[]` operator) returns `null` when the key is
  absent. The `emailConfigElement != JsonNull` check additionally treats an explicit `"email": null`
  the same as absent (Q2's defensive belt-and-braces; no tracked config file is expected to ever
  actually carry this).
- `emailConfigElement` is a local `val`, so Kotlin smart-casts it to non-null `JsonElement` inside
  the `if` block and across the `single { }` lambda captures without an extra `!!` or a second local.
- This `if (...) { ... }` with no `else` branch is a single binary `if`, allowed under
  `agents/CODING.md`'s control-flow rule (the ban is on `else if` chains). The `single<EmailFeature>`
  block already uses `when` per the operator's own snippet, also compliant.
- `getOrNull<EmailsService>()` is evaluated lazily on first `get<EmailFeature>()`, by which point the
  whole Koin container (all plugins' `setupDI`) has already run — no registration-ordering hazard.
- `features/email/server/src/jvmMain/kotlin/JVMPlugin.kt` (the thin JVM entry-point delegator) needs
  **no change** — confirmed by reading it: it only delegates to `email.common.JVMPlugin` and this
  `Plugin`, with no email-specific logic of its own.

### 7. Files confirmed UNCHANGED (read and verified, no edits needed)

- `features/email/server/src/commonMain/kotlin/EmailFeature.kt` — server capability interface,
  untouched by all 7 points; confirmed by re-reading.
- `features/email/server/src/commonMain/kotlin/EmailsService.kt` — server-only send interface,
  untouched (still exactly `sendText`/`sendTextWithAttachments`/`sendHtml`, no `isFeatureEnabled`/
  `sendTestEmail`).
- `features/email/server/src/commonMain/kotlin/configurators/EmailRoutingsConfigurator.kt` — depends
  only on the `EmailFeature` interface, indifferent to which concrete implementation backs it.
- `features/email/server/src/commonMain/kotlin/models/EmailAttachment.kt` and its two existing test
  files (`EmailAttachmentTest.kt`, `EmailAttachmentDataSourceTest.kt`) — unrelated to this task.
- `features/email/client/**`, `features/email/common/**` — unrelated (server-only change; `ast-index
  usages` confirmed no other feature/module references `EmailsService`/`SmtpEmailService`/
  `EmailFeatureService`).
- `features/email/server/src/jvmMain/kotlin/JVMPlugin.kt` — see point 6 above.

### 8. `features/email/server/src/commonTest/kotlin/services/SmtpEmailServiceDisabledTest.kt` — REWRITTEN

All 5 existing tests construct `SmtpEmailService(EmailConfig(smtp = null))`, which won't compile
once `smtp` is non-nullable; one calls the now-deleted `isFeatureEnabled()`. Rewrite to keep coverage
of `SmtpEmailService`'s one remaining no-op trigger (blank host), extended to all three send methods
(the current file only covers blank-host for `sendText`):

- **Remove:** `sendTextReturnsFalseWhenSmtpIsNull`, `sendHtmlReturnsFalseWhenSmtpIsNull`,
  `sendTextWithAttachmentsReturnsFalseWhenSmtpIsNullAndDoesNotInvokeProvider`,
  `isFeatureEnabledFalseWhenSmtpIsNull` — all four either don't compile or test a deleted method.
- **Keep/adapt:** `sendTextReturnsFalseWhenHostIsBlank` — still constructs
  `EmailConfig(smtp = SmtpConfig(host = "", from = Email("noreply@example.com")))`, unaffected by the
  nullability change.
- **Add (new, to restore 1:1 breadth against the removed null-smtp cases, now keyed on blank host
  instead):**
  - `sendHtmlReturnsFalseWhenHostIsBlank` — same shape as the kept `sendText` test, for `sendHtml`.
  - `sendTextWithAttachmentsReturnsFalseWhenHostIsBlankAndDoesNotInvokeProvider` — mirrors the
    removed null-smtp attachments test: blank host, assert `false` return AND that the attachment's
    `content` provider is never invoked (reuse the existing invocation-counter pattern from the
    removed test).
- Update the class-level KDoc: drop "no SMTP server is configured" framing (that's now
  `DisabledEmailFeature`'s scope, not this class's), reframe as "Verifies `SmtpEmailService`'s
  remaining no-op trigger: a configured-but-blank SMTP host."

### 9. New test file: `features/email/server/src/commonTest/kotlin/services/DisabledEmailFeatureTest.kt`

Cases:
- `isFeatureEnabledReturnsFalse` — trivial, always `false`.
- `sendTestEmailReturnsFalseRegardlessOfCaller` — `false` for both a root caller and a non-root/
  missing caller (it's a hard no-op, no repo interaction required to prove this — if the chosen fake
  `UsersRepo` supports call counting, additionally assert it is never queried by this method).
- `setMyEmailPersistsViaUsersRepoWhenUserFound` — verify `usersRepo.update(...)` is actually invoked
  with the new email and the method returns `true`.
- `setMyEmailReturnsFalseWhenUserNotFound` — `usersRepo.getById` returns `null` → `false`, no update
  call.

### 10. New test file: `features/email/server/src/commonTest/kotlin/services/EmailFeatureServiceTest.kt`

No test file exists yet for this class (confirmed: only `SmtpEmailServiceDisabledTest.kt`,
`EmailAttachmentTest.kt`, `EmailAttachmentDataSourceTest.kt` exist under `commonTest`). Cases:
- `isFeatureEnabledTrueWhenEmailsServiceNonNull`
- `isFeatureEnabledFalseWhenEmailsServiceNull`
- `sendTestEmailReturnsFalseWhenEmailsServiceNullEvenForRootCaller`
- `sendTestEmailDelegatesToSendTextForRootCallerAndReturnsItsResult` — verify the fake `EmailsService`
  receives exactly one `sendText(recipient, subject, text)` call with the expected fixed subject/text,
  and that the method's return value is exactly what `sendText` returned (both `true` and `false`
  cases worth covering).
- `sendTestEmailReturnsFalseForNonRootCallerAndDoesNotCallSendText` — verify the fake `EmailsService`
  records zero invocations.
- `sendTestEmailReturnsFalseWhenCallerNotFound`
- `setMyEmailPersistsViaUsersRepo` — same fixture/assertions as `DisabledEmailFeatureTest`'s
  equivalent case; both exercise the same `updateStoredEmail` helper, so keeping the assertions
  parallel makes the shared-helper choice visible in the test suite itself.

**Test doubles needed (none exist yet in this module):** a hand-rolled fake `EmailsService` (records
call count/args, returns a configurable `Boolean`) and a fake/in-memory `UsersRepo` (no existing
fake was found anywhere in the repo via `ast-index`/grep — `CacheUsersRepo` and `ExposedUsersRepo`
are the only two implementations, neither test-oriented). Architecture should design a minimal
in-memory `UsersRepo` test double (e.g. backed by a `MutableMap`/`dev.inmo.micro_utils.repos.MapKeyValueRepo`,
matching whatever shape `UsersRepo`'s actual interface requires — read
`features/users/common/src/commonMain/kotlin/repo/UsersRepo.kt` directly when writing it) shared by
both new test files. This is a normal test-infrastructure task, not a blocker — flagged here so
Architecture doesn't have to rediscover that no such fake exists.

**No dedicated test file for `UpdateStoredEmail.kt`:** it's a pure `internal` function with no public
surface beyond what's already exercised through both callers' `setMyEmail` tests — covered
transitively.

### 11. `server/sample.config.json` — MODIFIED

Replace the current flat `"smtp": null,` line with a nested, fully-populated `"email"` block at the
same position (must show the feature **enabled**, per Q2). Using the same example values already
documented in the README's "how to enable" snippet, for continuity:

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

Every field on `SmtpConfig` that lacks a default (`host`, `from`) is present; the rest are shown
explicitly for clarity even though some have defaults, matching the README's existing example
formatting.

### 12. `server/dev.config.json` — NO CHANGE (verified)

Re-confirmed by reading the file directly: `dev.config.json`'s `"plugins"` array does **not** include
`dev.inmo.wishlist.features.email.server.JVMPlugin` at all (it stops at `currency.server.JVMPlugin`
and `booking.server.JVMPlugin` — no email entry), and the file has no `"smtp"`/`"email"` key. Since
the email plugin is never loaded against this config, `Plugin.setupDI` for email never runs, so
nothing about this task's schema change touches it. **Conclusion: no change needed.**

### 13. `server/local.config.json` — NOT EDITED (read-only per instructions, verified consistent)

Gitignored/untracked, read for context only. Confirmed: same as `dev.config.json`, its `"plugins"`
array does not include the email plugin and it has no `"smtp"`/`"email"` key — this is the intended
real-world "feature omitted" convention the operator referenced in the Q2 answer. Not edited, and
its absence of an email plugin entry is not a blocker for this task (the plugin's absence from the
list is exactly what "omitted" means here, at both the DI-registration and config-key level).

### 14. `features/email/README.md` — MODIFIED (Overview / Models / Architecture Notes; Operator Notes untouched)

- **Overview:** replace *"When no SMTP block is present in the server config (or `smtp` is `null`),
  the feature operates in no-op mode..."* with wording describing the new nested-key convention:
  *"When no `\"email\"` object is present in the server config (the key is entirely absent — or, if
  ever set, is JSON `null`), the feature operates in no-op mode: `GET /email/enabled` returns
  `false`, `POST /email/sendTest` returns `false` without attempting a connection, and
  `PUT /email/myEmail` (storage) keeps working normally — see Architecture Notes."*
- **Models table:**
  - `EmailFeatureService` row: *"wraps `SmtpEmailService` + `UsersRepo`"* →
    *"wraps a nullable `EmailsService?` + `UsersRepo`; `isFeatureEnabled()` reports
    `emailsService != null`. In the DI graph, only ever constructed with a non-null `EmailsService`
    — see `DisabledEmailFeature`."*
  - New row: `DisabledEmailFeature` | `email/server` | No-op `EmailFeature` substituted in DI
    (`Plugin.kt`) whenever no `EmailsService` is registered (SMTP unconfigured):
    `isFeatureEnabled`/`sendTestEmail` return `false`; `setMyEmail` still persists via `UsersRepo`
    (storage stays independent of SMTP, per the feature's own architecture rule). |
  - `EmailConfig` row: *"`smtp: SmtpConfig? = null`"* → *"`smtp: SmtpConfig` (non-nullable) — decoded
    from the nested `\"email\"` object (`config[\"email\"]`) in the server config JSON, not the whole
    root object."*
  - `SmtpEmailService` row: drop the (now-removed) `isFeatureEnabled` framing; note it "is only ever
    constructed by `Plugin` when the `\"email\"` key is present and non-null."
- **Architecture Notes:**
  - **Config-slice pattern paragraph** — rewrite: `EmailConfig` is decoded via
    `get<Json>().decodeFromJsonElement(EmailConfig.serializer(), config["email"])` — a **nested-key**
    pattern, deliberately different from `CurrencyConfig`'s root-flat-key pattern. This is the first
    feature in the codebase to gate `single` registrations on JSON-key presence (DI-graph-shape
    "disabled" state) rather than handling a null/absent value at runtime.
  - **DI placement paragraph** — rewrite: `EmailConfig` + `SmtpEmailService` + the `EmailsService`
    binding are registered together, conditionally, only when `config["email"]` is present and
    non-null. `EmailFeature` is always registered; it resolves `getOrNull<EmailsService>()` and picks
    `EmailFeatureService` (present) or `DisabledEmailFeature` (absent).
  - **New note:** `EmailFeatureService.setMyEmail` and `DisabledEmailFeature.setMyEmail` both
    delegate to a shared `internal` `updateStoredEmail(usersRepo, callerId, email)` helper, so the
    documented "storage is always independent of SMTP" guarantee is implemented in exactly one place.
  - **Sample config SMTP block** — replace the `"smtp": null` / flat `"smtp": {...}` example with the
    new nested `"email": { "smtp": {...} } }` example (same values as item 11 above); state that
    omitting the whole `"email"` key (not setting `"smtp"` to `null` within it) is now the documented
    way to disable.
  - **Storage vs sending paragraph** — kept, lightly reworded to note the separation is now also
    enforced structurally: `DisabledEmailFeature` proves `setMyEmail` cannot regress even when the
    `EmailFeature` implementation itself changes based on SMTP availability.

## Config-file verification (task item 3)

- `server/sample.config.json` — **needs an edit** (item 11 above): it currently has the old flat
  `"smtp": null` and lists the email plugin in `"plugins"`, so it must gain the new nested `"email"`
  block to stay bootable and to demonstrate the feature enabled (Q2's explicit requirement).
- `server/dev.config.json` — **no change needed.** Verified by direct read: it does not list the
  email plugin and has no smtp/email key at all, so this task's schema change is entirely inert
  against it.
- `server/local.config.json` — **not edited** (gitignored, read-only per instructions). Verified
  consistent with the "omitted" convention: no email plugin entry, no smtp/email key.
- **No new config file is required anywhere.** This task only changes the shape of an existing,
  already-referenced key (`"smtp"` → nested `"email".smtp`) in one already-tracked file.

## Remaining open questions

**None. The plan is READY for Architecture.** Every point flagged as blocked in `001-planning.md`
(Q1–Q4) has a concrete, operator-confirmed resolution reflected in the file-by-file plan above; the
two genuinely new implementation-detail decisions that surfaced while finalizing this round —
(a) keeping `SmtpEmailService.sendTestEmail(recipient)` even though it becomes currently-uncalled,
and (b) extracting the shared `updateStoredEmail` helper instead of duplicating `setMyEmail`'s three
lines — were both explicitly pre-authorized as "free implementation-detail choices for Architecture
to make and record" by the operator's Q3 answer, and are recorded here with rationale rather than
re-raised as blocking questions. Per `agents/ARCHITECTURE.md`, Architecture must still produce
concrete test stubs (the shapes/cases are sketched in items 8–10 above, but not written as literal
Kotlin) and flag anything it finds genuinely untestable before Coding starts — none of the planned
changes here look untestable to this agent (all are pure `suspend fun`s over injectable interfaces),
but that determination is Architecture's to make per its own role instructions, not Planning's to
preempt.

## Status

**READY for Architecture.** No source file was modified in this step; only this report was written,
and it will be committed alone (with `002-planning.md` only), per `agents/GIT.md`.
