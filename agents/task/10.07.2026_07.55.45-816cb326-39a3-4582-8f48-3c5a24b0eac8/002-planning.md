Model: Claude Sonnet 5 (claude-sonnet-5), assigned by the orchestrator for this invocation. Per `agents/SHORTCUTS.md` the Planning-role model priority is `fable` / `opus` / `sonnet`; Sonnet is the priority-3 fallback listed there (same as the previous round).
Execution time: ~15 minutes (~900s) wall-clock (previous step `001-planning.md` written 14:04, this round began reading immediately after and this report is written ~14:20, same day).
Tokens used: not precisely instrumented from inside this agent. Rough estimate: ~70k-90k tokens on this round's investigation thread (re-reading `agents/*.md`, both hand-edited source files, all three affected test files, both READMEs, both config files, plus `ast-index`/`grep` cross-checks).
Changed files: `agents/task/10.07.2026_07.55.45-816cb326-39a3-4582-8f48-3c5a24b0eac8/002-planning.md` (this file) only. No source, config, or other doc file was edited by this step — Planning is not authorized to edit them.

---

# Planning round 2: fallout cleanup after the operator's hand-edit (fix/44-email)

## 1. Task understanding (this round)

Round 1 (`001-planning.md`) found two uncommitted, unstaged working-tree edits
(`EmailFeatureService.kt`, `Plugin.kt`) that did not compile against the existing test suite and
blocked a docs-only "update docs, commit and push" pass, and asked the operator whether to discard
them or finish them. The Orchestrator (Root) has now resolved this: the edits are the operator's own
deliberate, complete-in-intent change (Option B) — `EmailFeatureService`'s `emailsService` becomes
non-nullable, `isFeatureEnabled()` becomes a hardcoded `true`, and `Plugin.kt`'s `single<EmailFeature>`
inlines the selection logic instead of calling `selectEmailFeature`. Root's instruction: keep that
code edit exactly as written, do not revert or redesign it; my job this round is only to plan the
resulting fallout cleanup (broken tests, dead code, stale docs/config) so the tree is consistent and
compiles/tests green, and to fold in the three independent doc/config fixes round 1 already identified.

I re-read the actual current (post-hand-edit) source of every affected file rather than trusting round
1's or the READMEs' prose, and cross-checked usages with both `grep` and `ast-index usages` (per
`agents/ALL.md`'s ast-index mandate). Findings below.

## 2. Verified current state of the hand-edited files

`features/email/server/src/commonMain/kotlin/services/EmailFeatureService.kt` — constructor is now
`EmailFeatureService(private val emailsService: EmailsService, private val usersRepo: UsersRepo)`
(non-nullable), `override suspend fun isFeatureEnabled(): Boolean = true` (hardcoded),
`sendTestEmail` calls `emailsService.sendText(...)` directly with no null-guard. This part is exactly
as Root described and is out of scope to change.

**However**, the class's KDoc (class-level doc, the `@param emailsService` doc, `isFeatureEnabled`'s
doc, and one clause of `sendTestEmail`'s doc) still describe the *old* nullable behavior verbatim and
are now factually false / self-contradictory against the code beneath them (e.g. "`[emailsService]`
stays nullable... per the operator's explicit instruction..." and "`@return `true` when [emailsService]
is non-null; `false` otherwise`" — impossible now that the parameter type is non-null and the method
body is a bare `= true`). This is not a redesign of the operator's edit — the code stays untouched —
it is squarely `agents/CODING.md`'s standing rule "when updating existing code that has KDocs — update
the KDocs to match," and leaving it as-is would ship an internally contradictory file. I'm including it
in the plan below (item A).

`features/email/server/src/commonMain/kotlin/Plugin.kt` — the `single<EmailFeature>` block is now:
```kotlin
single<EmailFeature> {
    getOrNull<EmailsService>() ?.let {
        EmailFeatureService(it, get<UsersRepo>())
    } ?: DisabledEmailFeature(get<UsersRepo>())
}
```
This matches Root's description. The `selectEmailFeature` function is still physically present
(unchanged) below it but is no longer called from here.

## 3. `EmailFeatureServiceTest.kt` — the 3 non-compiling `null` call sites (item 1 of the brief)

Confirmed via direct read and a `grep -rn "EmailFeatureService("` sweep of the whole feature: exactly
three call sites pass `null` as the first constructor argument, at lines 38, 46, 113 (matching round
1's line numbers exactly — nothing else in the repo constructs `EmailFeatureService`, so no other file
is affected by the constructor signature change). Disposition for each, decided by comparing against
`DisabledEmailFeatureTest.kt` (read in full):

- **Line 36-40, `isFeatureEnabledFalseWhenEmailsServiceNull`** — asserts `isFeatureEnabled()` is
  `false` when constructed with `null`. That behavior no longer exists in this class at all (it's now
  a hardcoded `true`), and the "disabled → `isFeatureEnabled()` false" case is already fully covered
  by `DisabledEmailFeatureTest.isFeatureEnabledReturnsFalse` (lines 28-32 there). **Delete this test
  method outright** — redundant, not reworkable (there is no `false` outcome left to assert against on
  this class).
- **Lines 42-49, `sendTestEmailReturnsFalseWhenEmailsServiceNullEvenForRootCaller`** (including its
  one-line doc comment above it) — asserts `sendTestEmail` returns `false` for a root caller when
  `emailsService` is `null`. Already fully covered by
  `DisabledEmailFeatureTest.sendTestEmailReturnsFalseForRootCaller` (lines 35-41 there), which tests
  exactly this scenario against the class that now owns it. **Delete this test method outright.**
- **Lines 109-120, `setMyEmailPersistsViaUsersRepoRegardlessOfEmailsService`** — this one is *not* the
  same shape as the other two, and the brief's framing (which describes only the
  `isFeatureEnabled`/`sendTestEmail` null-case pattern) doesn't map cleanly onto it — flagging this
  explicitly since it required an independent judgment call rather than a literal application of the
  brief. This test proves `setMyEmail` persists successfully for a *found* user. That capability still
  exists on `EmailFeatureService` (the method body is unchanged — it delegates to `updateStoredEmail`
  regardless of `emailsService`), and no other test in this file covers the found-user-succeeds path
  for `EmailFeatureService.setMyEmail` — the only other `setMyEmail` test in this file is
  `setMyEmailReturnsFalseWhenUserNotFound` (not-found path). `DisabledEmailFeatureTest` has its own
  equivalent (`setMyEmailPersistsViaUsersRepoWhenUserFound`), but that exercises a *different* class
  (`DisabledEmailFeature`), not `EmailFeatureService` — deleting this test would leave
  `EmailFeatureService.setMyEmail`'s success path completely untested. **Rework, don't delete**: rename
  to `setMyEmailPersistsViaUsersRepoForFoundUser`, replace `EmailFeatureService(null, repo)` with
  `EmailFeatureService(FakeEmailsService(), repo)` (the only constructible shape now), and update its
  doc comment from "Storage keeps working even when `emailsService` is `null` — proves the documented
  independence." to "A found user's stored email is updated and persisted via `UsersRepo`, exercising
  `EmailFeatureService.setMyEmail` directly (the SMTP-disabled path is covered separately by
  `DisabledEmailFeatureTest`)." Assertions (`assertTrue(result)`,
  `assertEquals(newEmail, repo.getById(plainUser.id)?.email)`) stay unchanged.

**Two more mechanical consistency fixes in the same file**, needed so the file doesn't ship misleading
comments after the above (both small, both within "keep the tree consistent" scope, not new design):

- Class-level KDoc (lines 13-18) currently says "`isFeatureEnabled` reports `emailsService != null`" —
  false now. Rewrite to: "Verifies `EmailFeatureService`: `isFeatureEnabled` always returns `true`
  (this class is only ever constructed with a real, non-null `EmailsService` — see
  `DisabledEmailFeatureTest` for the SMTP-disabled no-op path); `sendTestEmail` enforces the root-only
  guard before delegating exactly one `EmailsService.sendText` call; `setMyEmail` persists via
  `UsersRepo` for a found user."
- `isFeatureEnabledTrueWhenEmailsServiceNonNull` (lines 30-34) still compiles (it already passes
  `FakeEmailsService()`, never `null`) but its name/framing implies a `null`-vs-non-null contrast that
  no longer exists — there is only one constructible shape now. Rename to
  `isFeatureEnabledAlwaysReturnsTrue`; body unchanged.

Net effect: file goes from 9 test methods to 7 (2 deleted, 2 renamed/reworked in place, 5 untouched:
`sendTestEmailDelegatesToSendTextForRootCallerAndReturnsTrueResult`,
`...AndReturnsFalseResult`, `sendTestEmailReturnsFalseForNonRootCallerAndDoesNotCallSendText`,
`sendTestEmailReturnsFalseWhenCallerNotFound`, `setMyEmailReturnsFalseWhenUserNotFound` — all already
construct with a non-null `FakeEmailsService()`/`emailsService`, need no change).

## 4. `Plugin.kt`'s `selectEmailFeature` — dead code (item 2 of the brief)

Checked with both `grep -rn "selectEmailFeature"` and `ast-index usages selectEmailFeature` (the
mandated tool per `agents/ALL.md`) — both agree: the function has exactly **zero** production call
sites (the operator's inline edit replaced its only call site in `Plugin.kt`'s `setupDI`) and exactly
**two** test call sites, both in `PluginTest.kt`
(`selectEmailFeatureReturnsDisabledEmailFeatureWhenEmailsServiceNull`,
`selectEmailFeatureReturnsEmailFeatureServiceWhenEmailsServicePresent`). It is genuinely dead —
**delete it entirely**, along with its own KDoc block, and delete the two `PluginTest.kt` cases written
specifically against it (there is nothing left to test once the function is gone — the inline
`getOrNull<EmailsService>()?.let{...}?:...` expression it used to contain is exercised indirectly by
the fact `EmailFeature` resolves correctly, which isn't independently unit-testable without a Koin
harness, same as every other `single{}` block in this plugin — no new test is owed here).

`emailConfigElementOrNull` is confirmed **unaffected** and must be kept exactly as-is: `grep` shows it
is still called at `Plugin.kt:41` (`val emailConfigElement = emailConfigElementOrNull(config)`), inside
production code, and `ast-index usages emailConfigElementOrNull` independently confirms all three
`PluginTest.kt` call sites still reference it. (Note: `ast-index usages` did not surface the
`Plugin.kt:41` production call site itself — likely because it doesn't report same-file/self-object
usages — so I cross-checked with `grep` directly on the file and confirmed it by reading `Plugin.kt` in
full. Flagging this only so Coding doesn't rely on `ast-index usages` alone if it re-verifies this.)

Concrete edits:

- **`Plugin.kt`**: delete the `selectEmailFeature` function and its KDoc (current lines 73-87 in its
  entirety). Update the object-level KDoc's bullet describing `EmailFeature` registration (currently
  lines 27-29, which names `selectEmailFeature`) to describe the actual inline `single<EmailFeature>`
  block instead: "`EmailFeature`, always registered unconditionally via an inline `single<EmailFeature>`
  block that resolves `getOrNull<EmailsService>()`: present → `EmailFeatureService`; absent →
  `DisabledEmailFeature` (both wrapping `UsersRepo`, so per-user email storage keeps working even with
  SMTP disabled)." Update the paragraph explaining why helpers were extracted (currently lines 32-34,
  which claims *two* pure functions) to describe only `emailConfigElementOrNull`: "`emailConfigElementOrNull`
  is extracted as a pure, Koin-free top-level function specifically so the `\"email\"`
  config-key-presence decision is directly unit testable (see `PluginTest`) without needing a Koin test
  harness (none exists in this repo). The `EmailFeature`-implementation choice (present-`EmailsService`
  vs `DisabledEmailFeature`) is simple enough that it stays inline in the `single<EmailFeature>` block
  rather than being extracted to its own named helper." Leave `emailConfigElementOrNull` itself and the
  `setupDI`/`startPlugin` bodies untouched.
- **`PluginTest.kt`**: delete the `// --- selectEmailFeature ---` section header and both test methods
  under it (current lines 67-82). Update the class-level KDoc (currently lines 19-27, which frames the
  file as testing "the two pure decision functions") to describe only `emailConfigElementOrNull`: "Verifies
  `emailConfigElementOrNull` — the pure decision function `Plugin.kt` uses to drive its conditional Koin
  wiring for whether the `\"email\"` config block is present. Pure — no Koin container is constructed
  anywhere in this file. The `single { }` registration calls that wire this function's result (and the
  inline `EmailFeature`-selection logic) into Koin are intentionally not separately unit tested — see
  `agents/task/10.07.2026_06.40.48-2407c3c3-e09a-4139-976a-305652d931a3/003-architecturing.md`'s
  \"Testability decision\" section." Remove now-unused imports after the two test methods are deleted —
  I traced every remaining test body and confirmed none of them reference `DisabledEmailFeature`,
  `EmailFeatureService`, `FakeEmailsService`, `FakeUsersRepo`, `runTest`, `assertIs`, or `assertTrue`
  (only the three `emailConfigElementOrNull` tests remain, using `buildJsonObject`/`JsonNull`/
  `JsonPrimitive`/`put`/`putJsonObject`/`assertNull`/`assertEquals`/`Test`) — delete those 7 import
  lines. Final import block:
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
  ```

## 5. Doc/config fixes (item 3 of the brief — the three genuinely-stale items from round 1, re-verified against current post-hand-edit source, plus the two extra README spots the hand-edit itself now makes stale)

### (a) `features/email/README.md`

Read in full again against the actual current source (not the file's own existing text, not round 1's
prose). Four edits needed, all inside `## Models` / `## Architecture Notes` (never touching the empty
`## Operator Notes`):

1. **Models table, `EmailFeatureService` row** (currently line 41) — currently says "wraps a nullable
   `EmailsService?`... `isFeatureEnabled()` reports `emailsService != null`". Rewrite to: "Server
   `EmailFeature` impl; wraps a non-nullable `EmailsService` + `UsersRepo`. `isFeatureEnabled()` always
   returns `true` — this class is only ever constructed by `Plugin` when a real `EmailsService` exists;
   `sendTestEmail` enforces root-only access before delegating; `setMyEmail` persists the caller's email
   address. See `DisabledEmailFeature` for the substituted no-op implementation used when SMTP is not
   configured."
2. **Architecture Notes, "DI-graph-shape 'disabled' state" bullet** (currently line 51) — currently ends
   "...it resolves `getOrNull<EmailsService>()` via another pure helper, `selectEmailFeature(emailsService,
   usersRepo)`, and picks `EmailFeatureService` (present) or `DisabledEmailFeature` (absent)." Rewrite
   the ending to: "...it resolves `getOrNull<EmailsService>()` directly inside an inline
   `single<EmailFeature>` block and picks `EmailFeatureService` (present) or `DisabledEmailFeature`
   (absent)."
3. **Architecture Notes, "Root guard" bullet** (currently line 55) — the one concrete inaccuracy round 1
   found, independent of the hand-edit. Currently: "`POST /email/sendTest` uses `requireRoot()` —
   mirrors `AdminRoutingsConfigurator.requireAdmin()`. Responds 401 (no token) or 403 (non-root). `PUT
   /email/myEmail` uses only `getCallerUserIdOrAnswerUnauthorized()` (self-service)." I re-read
   `EmailRoutingsConfigurator.kt` directly to re-confirm: there is no `requireRoot()` anywhere in the
   repo; both routes call only `getCallerUserIdOrAnswerUnauthorized()`; both failure branches (root
   check inside `EmailFeatureService.sendTestEmail`, or `feature.setMyEmail` returning `false`) respond
   `HttpStatusCode.InternalServerError` (500), never 403. Rewrite to: "Both `POST /email/sendTest` and
   `PUT /email/myEmail` use only `getCallerUserIdOrAnswerUnauthorized()` at the routing layer
   (self-service auth — 401 on missing/invalid bearer token). Root-only enforcement for `sendTest`
   happens inside `EmailFeatureService.sendTestEmail` by comparing `caller.username.string` against the
   literal `\"root\"`; on failure there — whether the caller isn't root or the SMTP send itself failed —
   the route responds `500 Internal Server Error` (the two failure modes are indistinguishable at the
   HTTP layer)."
4. **Architecture Notes, "DI placement" bullet** (currently line 57) — currently: "...always registers
   `EmailFeature` (`EmailFeatureService` or `DisabledEmailFeature`, selected by `selectEmailFeature`)...
   The `\"email\"`-key-presence check and the `EmailFeature`-implementation choice are each implemented
   as a small `internal` pure function (`emailConfigElementOrNull`, `selectEmailFeature`) in `Plugin.kt`
   specifically so they can be unit-tested (`PluginTest.kt`) without a Koin test harness — this repo has
   none, and these two conditionals are the only branchy logic in the plugin." Rewrite to: "...always
   registers `EmailFeature` (`EmailFeatureService` or `DisabledEmailFeature`, selected inline by a
   `getOrNull<EmailsService>()` check in the `single<EmailFeature>` block)... The `\"email\"`-key-presence
   check is implemented as a small `internal` pure function (`emailConfigElementOrNull`) in `Plugin.kt`
   specifically so it can be unit-tested (`PluginTest.kt`) without a Koin test harness — this repo has
   none. The `EmailFeature`-implementation choice
   (`getOrNull<EmailsService>()?.let { EmailFeatureService(it, get()) } ?: DisabledEmailFeature(get())`)
   is simple enough that it stays inline rather than being extracted to its own testable helper."

### (b) Root `README.md`

Two edits, both re-verified against the actual current `server/sample.config.json` (read in full this
round):

1. **"Server configuration" Key Fields table** (lines 86-98) — add a new row after the last existing
   row (`openExchangeRatesRefreshTTLMillis`, line 98), before the table's trailing blank line:
   `| \`email\` | Nested SMTP config object (\`{ smtp: { host, port, username?, password?, from, useTls,
   useSsl } }\`) that enables the email feature's SMTP test-email delivery; omit the key (or set it to
   JSON \`null\`) to disable SMTP while per-user email-address storage (\`PUT /email/myEmail\`) keeps
   working |`
2. **"Production deployment" table, `server/sample.config.json` row, "What to change before use" cell**
   (line 109) — insert a clause between the existing `openExchangeRatesAppId` clause and `enableRegistration`
   clause. New cell text: "Replace the `database` `url` / `username` / `password` (placeholders
   `TEST_DB` / `TEST_USERNAME` / `TEST_PASSWORD`), set `publicHost` to your real public address, set
   `openExchangeRatesAppId` if you use the currency feature, configure (or omit) the `email` block if
   you want SMTP-based test-email delivery, and review `enableRegistration`. Mount the finished file
   into the container at `/config.json`."

### (c) `server/dev.config.json`

Read in full this round; confirmed `dev.config.json`'s `plugins` array has all 9 entries
`sample.config.json` has (through `booking.server.JVMPlugin`) but is missing
`"dev.inmo.wishlist.features.email.server.JVMPlugin"` entirely (`sample.config.json` has it positioned
after `booking.server.JVMPlugin`, before `deeplinks.server.JVMPlugin` — the latter is a pre-existing,
unrelated gap in `dev.config.json` that predates this branch and stays out of scope). Add
`"dev.inmo.wishlist.features.email.server.JVMPlugin"` as the new last entry in `dev.config.json`'s
`plugins` array (immediately after `"dev.inmo.wishlist.features.booking.server.JVMPlugin"`). Do **not**
add a top-level `"email"` key — its absence is intentional (disables real SMTP sends in local dev,
matches the feature's documented "omit the key to disable" contract) and must be preserved. Do not
touch `server/local.config.json` (git-ignored, personal, out of scope) or `server/sample.config.json`
(already correct, already has both the plugin entry and the `"email"` block).

## 6. Model-tier guidance per `agents/SHORTCUTS.md`

- **`.md`-only, haiku tier**: `features/email/README.md` (§5a), root `README.md` (§5b).
- **Code/config, normal Coding-role tier** (`sonnet`/`opus`/`fable` priority per `agents/SHORTCUTS.md`):
  `EmailFeatureServiceTest.kt` (§3), `Plugin.kt` (§4, including its KDoc), `PluginTest.kt` (§4),
  `EmailFeatureService.kt`'s KDoc-only fix (§2). `server/dev.config.json` (§5c) is JSON, not `.md`, so
  the haiku mandate does not strictly apply — it's a trivial one-line array addition, low-risk either
  way; Architecture/Coding can fold it into the same pass as the other JSON/`.kt` work or run it
  separately, no design judgment involved.
- After all `.kt` edits, Coding must run `ast-index rebuild` per `agents/ALL.md` (not needed for the
  `.md`/JSON-only edits).
- Per `agents/CODING.md`, after this coding session run `./gradlew :wishlist.features.email.server:build`
  (or the closest matching module build task) and fix one cycle of any resulting errors.

## 7. Recommendation on Architecture-step weight

This cleanup is small and mechanical: no new production behavior, no new interfaces or routes, no
architectural decision left open — it is exactly "delete 2 redundant tests + rework 1 test's
constructor argument, delete 1 dead function + its 2 dedicated tests + prune imports, fix 4 KDoc/README
paragraphs to match code that's already final, add 1 README row + 1 table clause + 1 JSON array entry."
I've already produced file-by-file, line-anchored before/after text above, detailed enough that Coding
could implement directly from this document. My recommendation: a full heavyweight Architecture pass
is not needed on the merits.

That said, I don't have the authority to waive the state machine in `agents/ORCHESTRATOR.md`, which
requires stage 2 (Architecturing) with test stubs written into its own step file before stage 3
(Coding) — `agents/PLAN.md` doesn't grant Planning a skip-to-Coding path, and `agents/ARCHITECTURE.md`'s
test-stub requirement is written as unconditional. So: I recommend the Orchestrator route this through
a **fast, low-effort Architecture pass** — largely transcribing §3/§4's test-method dispositions
(delete/rename/rework, exact resulting names and bodies already specified above) into its own step
file's required test-stub format — rather than either skipping Architecture outright or re-deriving the
plan from scratch. The actual Coding diff this implies is genuinely small (4 `.kt` files with mostly
deletions + KDoc text, 3 doc/config files with targeted paragraph/row edits), so I'd expect Architecture
and Coding together to be fast regardless of how the Orchestrator routes it.

## 8. Open questions

None remaining. The one place I exercised independent judgment beyond a literal reading of the brief —
§3's disposition of `setMyEmailPersistsViaUsersRepoRegardlessOfEmailsService` (rework instead of
delete, to avoid a real coverage gap on `EmailFeatureService.setMyEmail`'s success path) — is a
plan-level testing-strategy call within Planning's normal authority, not an architecture decision or a
product-behavior question, so I did not treat it as blocking. **READY.**
