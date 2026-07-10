Model: Claude Sonnet 5 (claude-sonnet-5), assigned by the orchestrator for this invocation. Per
`agents/SHORTCUTS.md` the Coding-role model priority is `sonnet` / `opus` / `fable`; Sonnet is the
priority-1 choice, used throughout including for the two `.md`-only edits (step 5, root `README.md`
and `features/email/README.md`). Per `agents/SHORTCUTS.md` rule 4 those edits should ideally run at
`haiku` tier, but this agent has no ability to switch model tier mid-invocation for a sub-step — noting
this here as the task's own instructions anticipated, rather than spawning a separate subagent for two
small, exact-text README edits where a revert-risk precedent already exists in this project's history.

Execution time: ~25 minutes wall-clock for this step (reading `agents/ALL.md`/`local.ALL.md`/
`CODING.md`/`GIT.md`/`PROTOCOL.md`/`SHORTCUTS.md`, the task folder's `PROMPT.md`/`001-planning.md`/
`002-planning.md`/`003-architecturing.md`, all seven target files at their current state, applying all
edits, `ast-index rebuild`, and the Gradle build).

Tokens used: not precisely instrumented from inside this agent.

Changed files:
- `features/email/server/src/commonTest/kotlin/services/EmailFeatureServiceTest.kt`
- `features/email/server/src/commonTest/kotlin/PluginTest.kt`
- `features/email/server/src/commonMain/kotlin/Plugin.kt`
- `features/email/server/src/commonMain/kotlin/services/EmailFeatureService.kt`
- `features/email/README.md`
- `README.md` (root)
- `server/dev.config.json`
- `agents/task/10.07.2026_07.55.45-816cb326-39a3-4582-8f48-3c5a24b0eac8/004-coding.md` (this file)

---

# Coding: fallout cleanup after the operator's hand-edit (fix/44-email)

## Summary

Implemented `003-architecturing.md`'s spec exactly, with no re-derivation of design. The operator's
hand-edit to `EmailFeatureService.kt`/`Plugin.kt` (non-nullable `emailsService`, hardcoded
`isFeatureEnabled() = true`, inlined `EmailFeature` selection in `Plugin.kt`) was left completely
untouched — all edits in this step are either test-file rewrites to match the now-non-nullable
constructor, dead-code deletion, or KDoc/doc-text corrections.

## What changed, file by file

1. **`EmailFeatureServiceTest.kt`** — replaced with the architecture spec's full file content verbatim.
   `isFeatureEnabledFalseWhenEmailsServiceNull` and
   `sendTestEmailReturnsFalseWhenEmailsServiceNullEvenForRootCaller` (both passed `null` as the first
   constructor arg, no longer compilable) were deleted — each is fully redundant with an existing
   `DisabledEmailFeatureTest` case. `setMyEmailPersistsViaUsersRepoRegardlessOfEmailsService` was
   reworked into `setMyEmailPersistsViaUsersRepoForFoundUser` (constructs with `FakeEmailsService()`
   instead of `null`) so `EmailFeatureService.setMyEmail`'s found-user-succeeds path stays covered.
   `isFeatureEnabledTrueWhenEmailsServiceNonNull` was renamed to `isFeatureEnabledAlwaysReturnsTrue`
   (body unchanged — it never passed `null`). Net: 9 → 7 test methods, all passing.

2. **`PluginTest.kt`** — replaced with the spec's full file content verbatim. Deleted
   `selectEmailFeatureReturnsDisabledEmailFeatureWhenEmailsServiceNull` and
   `selectEmailFeatureReturnsEmailFeatureServiceWhenEmailsServicePresent` (and the
   `// --- selectEmailFeature ---` section header), since `selectEmailFeature` itself is now deleted from
   `Plugin.kt`. Removed the 7 now-unused imports
   (`DisabledEmailFeature`/`EmailFeatureService`/`FakeEmailsService`/`FakeUsersRepo`/`runTest`/
   `assertIs`/`assertTrue`) and rewrote the class-level KDoc to describe only `emailConfigElementOrNull`.
   Net: 5 → 3 test methods, all passing.

3. **`Plugin.kt`** — deleted the `selectEmailFeature` function and its KDoc entirely (it had zero
   production call sites once the operator's inline `single<EmailFeature>` block replaced its only
   caller). Updated the object-level KDoc's `EmailFeature`-registration bullet to describe the actual
   inline `getOrNull<EmailsService>()?.let{...}?:...` block instead of the now-deleted helper, and
   updated the "why helpers were extracted" paragraph to describe only `emailConfigElementOrNull` (kept
   exactly as-is — still has 3 production/test call sites, unaffected). `setupDI`/`startPlugin` bodies
   (the operator's hand-edit) were not touched.

4. **`EmailFeatureService.kt`** — KDoc-only fix, no executable-code changes: rewrote the class-level KDoc
   (removed the now-false "stays nullable... per the operator's explicit instruction" language),
   `isFeatureEnabled()`'s doc (now documents the hardcoded `true`), and `sendTestEmail`'s doc (dropped
   the now-nonexistent null-guard clause). `setMyEmail`'s doc and the `rootUsername` val's doc were
   already accurate and left untouched.

5. **`features/email/README.md`** — four edits inside `## Models`/`## Architecture Notes` (the empty
   `## Operator Notes` section was not touched): the `EmailFeatureService` Models-table row now
   describes the non-nullable constructor and hardcoded `isFeatureEnabled()`; the "DI-graph-shape
   disabled state" bullet's closing clause now describes the inline selection instead of
   `selectEmailFeature`; the "Root guard" bullet was corrected — there is no `requireRoot()` anywhere in
   the repo, both `sendTest` and `myEmail` use only `getCallerUserIdOrAnswerUnauthorized()`, and failures
   (non-root caller or SMTP failure) both yield `500`, never `403`; the "DI placement" bullet was updated
   to match (inline selection, `emailConfigElementOrNull` is now the only extracted pure helper).

6. **Root `README.md`** — added an `email` row to the "Server configuration" Key Fields table
   (documents the nested `{ smtp: {...} }` shape and the omit-to-disable contract), and extended the
   `server/sample.config.json` row's "What to change before use" cell in the Production deployment table
   with a clause about configuring or omitting the `email` block.

7. **`server/dev.config.json`** — added
   `"dev.inmo.wishlist.features.email.server.JVMPlugin"` as the new last entry in the `plugins` array
   (same relative position as in `sample.config.json`, after `booking.server.JVMPlugin`). No top-level
   `"email"` key was added — its absence is intentional and disables real SMTP sends in local dev, per
   the feature's documented "omit the key to disable" contract. `server/local.config.json` (git-ignored)
   and `server/sample.config.json` (already correct) were not touched.

## Verification

- Ran `ast-index rebuild` (four `.kt` files changed) — completed cleanly, 679 files indexed.
- Ran `./gradlew :wishlist.features.email.server:build` — **BUILD SUCCESSFUL** on the first attempt, no
  fix cycle needed. Confirmed via the generated JUnit XML reports:
  `PluginTest[jvm]`: `tests="3" failures="0" errors="0"`;
  `EmailFeatureServiceTest[jvm]`: `tests="7" failures="0" errors="0"` — matching the spec's expected
  test counts exactly. All other pre-existing test classes in the module
  (`EmailConfigTest`, `EmailAttachmentTest`, `DisabledEmailFeatureTest`, `EmailAttachmentDataSourceTest`,
  `SmtpEmailServiceDisabledTest`) were unaffected and still pass.
- `git status` after all edits shows exactly the 7 intended files as `modified`, nothing else touched;
  the pre-existing unrelated untracked items (`agents/security-review-2026-07-09.md`, this task's own
  `PROMPT.md`, and the newer, unrelated `agents/task/10.07.2026_14.11.08-...` folder) were left alone
  and are not staged in this step's commit.

## Deviation from `003-architecturing.md`'s push instruction

`003-architecturing.md` §8 states push is in scope for this Coding step (since the operator's original
"update docs, commit and push" prompt named push directly). However, the orchestrator's direct
instructions for this specific invocation explicitly said **do not push**. Per that explicit
instruction taking precedence for this invocation, this step commits but does not push; pushing is left
to whoever the orchestrator designates next.
