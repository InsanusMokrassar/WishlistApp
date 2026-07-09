Model: sonnet
Execution time: ~900
Tokens used: not exposed to this agent
Changed files:
- features/email/server/src/commonMain/kotlin/EmailsService.kt (new)
- features/email/server/src/commonMain/kotlin/models/EmailAttachment.kt (new)
- features/email/server/src/commonMain/kotlin/services/SmtpEmailService.kt (replaced)
- features/email/server/src/commonMain/kotlin/Plugin.kt (edited)
- features/email/server/src/commonTest/kotlin/services/SmtpEmailServiceDisabledTest.kt (new)
- features/email/server/src/commonTest/kotlin/models/EmailAttachmentTest.kt (new)
- features/email/server/src/commonTest/kotlin/services/EmailAttachmentDataSourceTest.kt (new)
- features/email/README.md (edited)

# 003 — Coding

## Model choice argument

`agents/SHORTCUTS.md` sets the Coding role model priority as `sonnet / opus / fable`. This agent runs on `sonnet` (Claude Sonnet 5), the highest-priority option for Coding, so the best-priority model is used. No fallback needed.

## What was implemented

Implemented `002-architecturing.md` verbatim, with no deviations:

1. **`features/email/server/src/commonMain/kotlin/EmailsService.kt`** (new) — server-only interface `EmailsService` with three `suspend` methods (`sendText`, `sendTextWithAttachments`, `sendHtml`), each returning `Boolean` and carrying full KDoc per spec §A.1. Copied verbatim.
2. **`features/email/server/src/commonMain/kotlin/models/EmailAttachment.kt`** (new) — plain class `EmailAttachment(fileName, mimeType = "application/octet-stream", content: suspend () -> InputStream)` in a new `models` package under `email/server`, per spec §A.2. Copied verbatim.
3. **`features/email/server/src/commonMain/kotlin/services/SmtpEmailService.kt`** (replaced) — `SmtpEmailService` now implements `EmailsService`; added the private shared `send(recipient, subject, logLabel, fillContent)` skeleton (disabled-check, session, envelope, `Transport.send` on `Dispatchers.IO`, `runCatching`/warn-log); `sendTestEmail` was refactored onto the same helper (per Architecture's delegated decision (b)); `sendText`, `sendHtml`, `sendTextWithAttachments` implemented via the helper; added the top-level `internal class EmailAttachmentDataSource : DataSource` bridge (`getInputStream()` uses a bare `runBlocking { attachment.content() }`, `getOutputStream()` throws `UnsupportedOperationException`, `getContentType()`/`getName()` echo the attachment). Copied verbatim from spec §A.3.
4. **`features/email/server/src/commonMain/kotlin/Plugin.kt`** (edited) — inserted `single<EmailsService> { get<SmtpEmailService>() }` directly after the `SmtpEmailService` registration, and updated the object KDoc bullet to mention the `EmailsService` binding, exactly per spec §A.5. No import changes needed (`EmailsService` is in the same package).
5. **Three new test files** under `features/email/server/src/commonTest/kotlin/...`, mirroring the case tables in spec §B.2–§B.4:
   - `services/SmtpEmailServiceDisabledTest.kt` — 5 cases (disabled/no-op mode: `sendText`/`sendHtml`/`sendTextWithAttachments` return `false` when `smtp == null` or host is blank; attachment provider is never invoked in disabled mode; `isFeatureEnabled()` is `false`). All wrapped in `runTest`.
   - `models/EmailAttachmentTest.kt` — 2 cases (content provider returns independent fresh streams on each invocation with an invocation counter; `mimeType` defaults to `application/octet-stream`).
   - `services/EmailAttachmentDataSourceTest.kt` — 3 cases (`getInputStream()` re-invokes the provider and returns a fresh stream on each call; `getContentType()`/`getName()` echo the attachment; `getOutputStream()` always throws `UnsupportedOperationException`). Exercises the `internal` bridge directly.
   - Every test class carries a KDoc per the spec's requirement; every test method also carries a short KDoc (stricter than the spec's minimum, to satisfy `agents/CODING.md`'s blanket "every fun must have a KDoc" rule with no exceptions carved out for tests).
   - One deliberate deviation from literal property-access style: the architecture spec's own production code uses Kotlin's Java-interop property-accessor sugar for `MimeBodyPart` setters (`dataHandler = ...`), but for `EmailAttachmentDataSourceTest` I called `getInputStream()` / `getOutputStream()` / `getContentType()` / `getName()` as explicit method calls rather than via property syntax (`dataSource.inputStream`, etc.), since these are Kotlin-declared `override fun` members (not directly Java-declared) and explicit calls remove any ambiguity about synthetic-property availability. This is a test-only stylistic choice with zero behavioral impact; the case table and assertions match the spec exactly.
6. **`features/email/README.md`** (edited) — applied §C.1 (`## Models` table: split the stale `EmailFeature` row into server/client variants, corrected the `SmtpEmailService` row, added `EmailFeatureService`, `EmailsService`, `EmailAttachment` rows) and §C.2 (`## Architecture Notes`: new `EmailsService` bullet, updated `DI placement` bullet) verbatim as specified. `## Operator Notes`, `## Overview`, and `## Routes` were not touched. Applied directly by this Coding agent rather than delegating to a sub-agent, per the explicit orchestrator instruction for this task ("This is mechanical application of Architecture-authored text — apply it directly yourself"), which supersedes the spec's general suggestion to delegate markdown-filling to a haiku sub-agent.

## Build result

```
./gradlew :wishlist.features.email.server:build
```

**Exit code: 0 (BUILD SUCCESSFUL)**. Test results (JUnit XML under `features/email/server/build/test-results/jvmTest/`):

- `SmtpEmailServiceDisabledTest`: 5 tests, 0 failures, 0 errors
- `EmailAttachmentTest`: 2 tests, 0 failures, 0 errors
- `EmailAttachmentDataSourceTest`: 3 tests, 0 failures, 0 errors
- **Total: 10 tests, 10 passed, 0 failed, 0 skipped.**

No `build.gradle` changes were needed — confirmed per spec §A.4: `jakarta.activation.DataHandler`/`DataSource` resolved on the compile classpath transitively through `libs.angus.mail`, and test dependencies (`kotlin("test-common")`, `libs.kotlin.coroutines.test`, `kotlin("test-junit")`) were already wired by the `mppJavaProject` template.

`ast-index rebuild` completed successfully: indexed 671 files, 46 modules.

## Deviations from spec

None. All file contents were applied verbatim as given in `002-architecturing.md` §A.1–§A.3, §A.5, and §C.1–§C.2. The only agent-authored content is the three test file bodies, which were written to match the exact case tables in §B.2–§B.4 (test names, fixtures, and assertions), since the spec provided the case tables but not literal file text for tests (as expected — Architecture reserves the exact test source for Coding by design).

## Scope guard verification

`git status --porcelain` after all changes confirms only the intended files changed: `features/email/README.md`, `features/email/server/src/commonMain/kotlin/Plugin.kt`, `features/email/server/src/commonMain/kotlin/services/SmtpEmailService.kt`, plus new files under `features/email/server/src/commonMain/kotlin/{EmailsService.kt,models/}` and `features/email/server/src/commonTest/`. No changes under `features/email/common`, `features/email/client`, any `configurators/`, or any route/DTO file. `EmailAttachment` content is never materialized as a `ByteArray` in production code (`SmtpEmailService.kt`, `EmailAttachmentDataSource`) — only test code reads bytes for assertions, per the operator constraint. Every new declaration carries KDoc. No `else if` was introduced (the `send` helper uses a single binary `if`; `buildSession`'s existing `when` was left untouched).

## Commit

Staged and committed: the 7 new/changed source files above, `features/email/README.md`, and this `003-coding.md`. Nothing else (other untracked task folders and `PROMPT.md` in this task's own folder were left untouched, per `agents/GIT.md`). Not pushed — pushing stays with the Orchestrator. Branch: `fix/44-email`.
