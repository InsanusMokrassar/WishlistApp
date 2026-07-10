Model: Sonnet 5 (claude-sonnet-5)
Execution time: ~35 minutes (this step only, wall-clock estimate)
Tokens used: not exposed to this agent
Changed files: see "Concrete file list" below

# 004 — Coding

## Summary

Implemented `003-architecturing.md` verbatim: the email feature's SMTP-configured/disabled state is
now a DI-graph-shape fact instead of a runtime `config.smtp != null` check. `EmailConfig.smtp` is
non-nullable and decoded from a new nested `"email"` JSON key (`config["email"]`, not the whole root
config object); `EmailConfig`/`SmtpEmailService`/`EmailsService` are registered together in Koin only
when that key is present and non-null; `EmailFeature` is always registered and resolves to either
`EmailFeatureService` (SMTP available) or the new `DisabledEmailFeature` no-op stub (SMTP absent),
selected via the new pure helper `selectEmailFeature`. `SmtpEmailService.isFeatureEnabled()` was
removed entirely. The uncommitted WIP present at task start (`EmailFeatureService.kt`'s
non-compiling double-send in `sendTestEmail`, `Plugin.kt`'s `get<SmtpEmailService>()` → `get()`
change) is fully superseded — both files now match the architecture spec exactly, with
`sendTestEmail` issuing exactly one `sendText(...)` call.

No deviation from `003-architecturing.md`'s file-by-file plan or its concrete Kotlin. The only
addition beyond what the architecture draft's Kotlin snippets literally showed is KDoc coverage: the
architecture draft's test-file snippets omitted KDocs on a few class-level `val`s/`var`s and on the
new test doubles' overridden interface methods (`FakeUsersRepo.nextId`, `FakeUsersRepo`'s three
overrides, `FakeEmailsService`'s three overrides, `EmailConfigTest.json`, `DisabledEmailFeatureTest`'s
and `EmailFeatureServiceTest`'s `rootUser`/`plainUser`/`recipient` vals) — added KDoc to every one of
these per this task's explicit instruction ("class-level private vals need KDocs... per a finding
from the prior task's Validating step") and `agents/CODING.md`'s KDoc Requirements section. All
behavior is otherwise identical to the architecture spec's Kotlin.

## Files changed

### Modified (production)
- `features/email/server/src/commonMain/kotlin/EmailConfig.kt` — `smtp` non-nullable; KDoc rewritten
  for the nested-key decode pattern and DI-graph-shape disabled state.
- `features/email/server/src/commonMain/kotlin/services/SmtpEmailService.kt` — removed
  `isFeatureEnabled()`; `send()`'s guard drops the dead `smtp == null` half (keeps blank-host check);
  KDoc updated.
- `features/email/server/src/commonMain/kotlin/services/EmailFeatureService.kt` — constructor takes
  nullable `emailsService: EmailsService?`; `isFeatureEnabled()` = `emailsService != null`;
  `sendTestEmail` short-circuits on null `emailsService` then issues exactly one `sendText(...)` call
  (WIP's erroneous second `sendTestEmail(recipient)` call, which doesn't exist on `EmailsService`,
  dropped); `setMyEmail` now delegates to the new shared `updateStoredEmail` helper.
- `features/email/server/src/commonMain/kotlin/Plugin.kt` — rewritten: `EmailConfig` +
  `SmtpEmailService` + `EmailsService` binding registered together, conditionally, gated by the new
  `emailConfigElementOrNull(config)` pure helper; `EmailFeature` always registered via the new
  `selectEmailFeature(emailsService, usersRepo)` pure helper (`getOrNull<EmailsService>()` →
  `EmailFeatureService` or `DisabledEmailFeature`).
- `server/sample.config.json` — replaced flat `"smtp": null` with a fully-populated nested
  `"email": { "smtp": {...} }` block (feature shown enabled, per the finalized plan).
- `features/email/README.md` — Overview, Models table, and Architecture Notes updated for the
  nested-key config pattern, `DisabledEmailFeature`, the `updateStoredEmail` shared helper, and the
  `emailConfigElementOrNull`/`selectEmailFeature` pure-function testability design. `## Operator
  Notes` untouched (it was already empty and remains so).

### New (production)
- `features/email/server/src/commonMain/kotlin/services/DisabledEmailFeature.kt` — no-op
  `EmailFeature`; `isFeatureEnabled`/`sendTestEmail` return `false`; `setMyEmail` still persists via
  `updateStoredEmail`.
- `features/email/server/src/commonMain/kotlin/services/UpdateStoredEmail.kt` — `internal suspend fun
  updateStoredEmail(usersRepo, callerId, email)`, shared by `EmailFeatureService.setMyEmail` and
  `DisabledEmailFeature.setMyEmail`.

### Rewritten (test)
- `features/email/server/src/commonTest/kotlin/services/SmtpEmailServiceDisabledTest.kt` — removed
  the 4 null-`smtp`/`isFeatureEnabled` cases (no longer compile / no longer exist); kept and extended
  the blank-host coverage to all three send methods (`sendText`, `sendHtml`,
  `sendTextWithAttachments` including the "must not invoke attachment provider" assertion).

### New (test)
- `features/email/server/src/commonTest/kotlin/EmailConfigTest.kt` — 4 cases: full decode, defaults
  applied, missing `smtp` key fails, explicit `smtp: null` fails.
- `features/email/server/src/commonTest/kotlin/PluginTest.kt` — 5 cases covering
  `emailConfigElementOrNull` (absent/JsonNull/present) and `selectEmailFeature`
  (null→`DisabledEmailFeature`, present→`EmailFeatureService` reporting enabled).
- `features/email/server/src/commonTest/kotlin/services/DisabledEmailFeatureTest.kt` — 6 cases:
  `isFeatureEnabled` always false, `sendTestEmail` false for root/non-root/missing caller,
  `setMyEmail` persists/clears/fails-when-not-found.
- `features/email/server/src/commonTest/kotlin/services/EmailFeatureServiceTest.kt` — 9 cases:
  `isFeatureEnabled` true/false by nullability, `sendTestEmail` null-service/root-success/root-failure
  /non-root/caller-not-found (with `FakeEmailsService` call-count assertions), `setMyEmail`
  persists-regardless-of-emailsService / not-found.
- `features/email/server/src/commonTest/kotlin/services/FakeUsersRepo.kt` — new in-memory `UsersRepo`
  test double, `MapCRUDRepo`-backed (no new `build.gradle` dependency needed — confirmed transitively
  available via `features/common/common`'s `api libs.microutils.repos.cache` →
  `micro_utils.repos.inmemory`).
- `features/email/server/src/commonTest/kotlin/services/FakeEmailsService.kt` — new call-recording
  `EmailsService` test double.

### Committed alongside
- `agents/task/10.07.2026_06.40.48-2407c3c3-e09a-4139-976a-305652d931a3/004-coding.md` (this file).

Not staged/committed: `agents/task/10.07.2026_06.40.48-2407c3c3-e09a-4139-976a-305652d931a3/PROMPT.md`
was found untracked at the start of this step (pre-existing, not created or modified by Coding) — left
alone per this task's explicit "do not stage or commit anything outside what this task touched"
instruction. `agents/security-review-2026-07-09.md` and the two unrelated `agents/task/06-07-2026...`/
`agents/task/24-06-2026...` folders visible in `git status` are unrelated prior work and were not
touched.

## Tooling

`ast-index rebuild` run after all `.kt` changes: 679 files indexed, 46 modules, 0 errors. Not rerun for
the README/config-JSON/step-report-only edits, per `agents/ALL.md`.

## Build result

`./gradlew :wishlist.features.email.server:build` — **PASS** (`BUILD SUCCESSFUL in 20s`, exit code 0).

Test summary (all 7 suites in the module, all green, 0 failures/errors):

| Suite | Tests |
|---|---|
| `EmailConfigTest` | 4 |
| `PluginTest` | 5 |
| `DisabledEmailFeatureTest` | 6 |
| `EmailFeatureServiceTest` | 9 |
| `SmtpEmailServiceDisabledTest` | 3 |
| `EmailAttachmentTest` (pre-existing, untouched) | 2 |
| `EmailAttachmentDataSourceTest` (pre-existing, untouched) | 3 |

No fix cycle was needed — the module compiled and all tests passed on the first build run.

## Known remaining issues

None identified. Every file `003-architecturing.md` listed was implemented; the module build and its
full test suite pass. The architecture doc's own "Testability decision" and "Genuinely untestable
functionality" sections (Koin `single { }` wiring glue, and `SmtpEmailService`'s live-SMTP
`Transport.send` success path) are pre-existing, documented, non-blocking gaps — not something this
step introduced or was asked to close.
