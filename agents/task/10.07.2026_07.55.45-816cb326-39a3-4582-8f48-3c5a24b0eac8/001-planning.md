Model: Claude Sonnet 5 (claude-sonnet-5), assigned by the orchestrator for this invocation. Per `agents/SHORTCUTS.md` the Planning-role model priority is `fable` / `opus` / `sonnet`; Sonnet is the priority-3 fallback listed there.
Execution time: ~8 minutes (~480s) wall-clock (task folder created 13:55, this report written 14:03, same day).
Tokens used: not precisely instrumented from inside this agent. Rough estimate: ~90k-110k tokens on the main investigation thread (repo-wide greps, multiple full-file reads, `git log`/`git show` history reconstruction) plus 52,426 tokens reported by one background verification subagent ≈ 140k-165k total for this step.
Changed files: `agents/task/10.07.2026_07.55.45-816cb326-39a3-4582-8f48-3c5a24b0eac8/001-planning.md` (this file) only. No source, config, or other doc file was edited by this step.

---

# Planning: "update docs, commit and push" (fix/44-email)

## 1. Task understanding

The operator's verbatim prompt is terse: "update docs, commit and push." Per `agents/PLAN.md` and the
Root-supplied context in `PROMPT.md`, my job was to investigate the *full* set of changes accumulated
on `fix/44-email` relative to `master` (not just the immediately-preceding task), verify
`features/email/README.md` and any other repo documentation against the actual current source, and
produce either (a) a concrete file-by-file doc-update plan, or (b) an explicit "nothing is stale"
conclusion — plus surface any genuine open questions rather than guessing.

I did not confine the investigation to `features/email/README.md` in isolation: I also checked the root
`README.md`, `agents/*.md` (non-task), other feature READMEs that mention email, and both tracked
sample/example server config files, per the Root context's explicit candidate list.

## 2. Full branch diff (not just the latest task)

`git log master..fix/44-email --oneline` shows 20 commits back to `3417880 feat(email): base email
feature (#44)`, spanning the original feature-add, a client/server `EmailFeature` split, a
merge-conflict resolution against `master` (Calm Studio redesign), the `EmailsService`
send-methods task, and the most recent DI-graph-shape redesign task (`5b81a9e` + its KDoc follow-up
`37bc0d1`, plus the `agents/task/.../PROMPT.md` recording commit `141e139` and one more housekeeping
commit `523430d`). `git diff master...fix/44-email --stat` confirms the email feature's `common`/
`server`/`client` modules, `features/email/README.md`, `server/sample.config.json`,
`gradle/libs.versions.toml`, `settings.gradle`, plus small touch-ups to `features/ui/adminPanel` and
`features/users` (the `email` field on `User`) are the only non-`agents/` production changes on this
branch. No changes to root `README.md`, `agents/ARCHITECTURE.md`, or any other feature's README appear
anywhere in the branch diff.

## 3. Open question — MUST be resolved by the operator before Coding touches anything (blocking)

**There are two uncommitted, unstaged working-tree modifications that are not part of any commit on
this branch and are not mentioned in the task context:**

- `features/email/server/src/commonMain/kotlin/services/EmailFeatureService.kt`
- `features/email/server/src/commonMain/kotlin/Plugin.kt`

`git status` shows both as `modified` with no corresponding commit; `git diff <path>` (working tree vs
HEAD) shows they were hand-edited roughly 90–170 seconds *after* the branch's last commit (`523430d`,
13:51:49) and only ~1–3 minutes before this task's folder was created (13:55) — i.e. very likely a
manual, local edit by the operator immediately preceding the "update docs, commit and push" prompt, not
stray agent leftovers.

**What the edit does:** it removes the nullable-`EmailsService?` shape from `EmailFeatureService`
(constructor param becomes non-nullable `EmailsService`, `isFeatureEnabled()` becomes a hardcoded
`= true`, `sendTestEmail` drops its `?: return false` null-guard) and removes `Plugin.kt`'s use of the
`selectEmailFeature(...)` helper in the `single<EmailFeature>` registration, replacing it with an
inline `getOrNull<EmailsService>()?.let { EmailFeatureService(it, get()) } ?: DisabledEmailFeature(get())`.

**Why this blocks a docs-only pass — this edit is demonstrably incomplete/broken, not a finished
design change:**
1. It directly contradicts the KDoc that already ships in the committed file: *"[emailsService] stays
   nullable so this class remains directly constructible ... per the operator's explicit instruction and
   for direct-construction testability."* That sentence is itself a record of a previous operator
   instruction to keep the nullable shape — the uncommitted edit reverses it silently, with no updated
   KDoc explaining why (the KDoc block above the class, and on `isFeatureEnabled`/`sendTestEmail`/the
   constructor param, still describes the *old* nullable behavior verbatim — it is now internally
   self-contradictory: comment says nullable/checks-null, code is non-nullable/hardcoded `true`).
2. It does not compile as-is: `features/email/server/src/commonTest/kotlin/services/EmailFeatureServiceTest.kt`
   calls `EmailFeatureService(null, ...)` at three call sites (lines 38, 46, 113) to test the
   disabled/null branch — those become type errors under the non-nullable constructor.
3. `Plugin.kt`'s `selectEmailFeature` function (and its class-level KDoc, and its two dedicated
   `PluginTest.kt` cases) are left in place but now unreferenced by production code — dead code with no
   indication whether that's intentional (keep for the unit tests only) or an oversight (forgot to
   delete along with its call site).

I did **not** touch either file — Planning is not authorized to edit source, and this is exactly the
kind of "unclear architecture decision" `agents/PLAN.md` step 3/4 says must go back to the operator
rather than be guessed at. Two possible resolutions, both requiring operator input:

- **(A) Discard.** `git checkout -- features/email/server/src/commonMain/kotlin/Plugin.kt
  features/email/server/src/commonMain/kotlin/services/EmailFeatureService.kt` restores the clean,
  fully-documented, fully-tested committed state. Under this resolution, `features/email/README.md`
  needs **no** changes on account of this diff (it already matches the committed/HEAD design exactly —
  see §4) — proceed straight to the concrete plan in §5.
- **(B) Finish it.** If the operator wants to actually retire the nullable-`EmailsService?` shape, that
  is a real code change (fix the KDoc in both files, decide the fate of `selectEmailFeature`/`PluginTest`
  dead code, rewrite the 3 null-argument `EmailFeatureServiceTest` cases, rebuild, re-run tests) — i.e. a
  full Architecture → Coding cycle, not a haiku-tier docs pass, and `features/email/README.md`'s Models
  table rows for `EmailFeatureService` (currently "wraps a nullable `EmailsService?`... `isFeatureEnabled()`
  reports `emailsService != null`") and the "DI-graph-shape disabled state" architecture bullet (currently
  references `selectEmailFeature`) would then need rewriting too. I have deliberately not pre-specified
  that redesign's exact diff — it's a new coding decision, not a docs-parity fix, and speccing it now
  would be exactly the "manufactured busywork" the Root's own prompt warned against guessing into.

**Recommendation:** ask the operator directly (per `agents/PLAN.md` step 4's terminal path) which of A/B
they intend, before Coding stages anything. Given the KDoc explicitly documents a prior operator
instruction to keep the nullable shape, and the working-tree edit is unfinished/non-compiling, (A)
discard is the safer default reading of "update docs, commit and push" (a docs-only ask), but this is
the operator's call, not mine to make silently.

## 4. `features/email/README.md` — verified against current HEAD-committed source

I read the file in full (66 lines) and verified every Routes-table row, every Models-table row, and
every Architecture Notes bullet against `git show HEAD:<path>` for each real source file (bypassing the
two uncommitted files above, read at HEAD instead of working tree, precisely because of the open
question in §3) — both directly and via an independent read-only subagent cross-check. Result:

**One concrete, unambiguous inaccuracy, independent of §3's open question:**

- Architecture Notes, "Root guard" bullet (currently line 55) reads: *"`POST /email/sendTest` uses
  `requireRoot()` — mirrors `AdminRoutingsConfigurator.requireAdmin()`. Responds 401 (no token) or 403
  (non-root)."* This is false. I read
  `features/email/server/src/commonMain/kotlin/configurators/EmailRoutingsConfigurator.kt` directly:
  there is no `requireRoot()` function anywhere in the email feature or the wider repo (confirmed via
  repo-wide grep — `requireAdmin()` exists only in `AdminRoutingsConfigurator.kt`, nothing analogous
  exists for email). Both `sendTest` and `myEmail` routes use only
  `getCallerUserIdOrAnswerUnauthorized()` (401 on missing/invalid token). The actual root-only
  enforcement happens *inside* `EmailFeatureService.sendTestEmail` (`caller.username.string !=
  rootUsername`, i.e. `"root"`), and on failure — whether "not root" or "SMTP send failed" — the route
  returns **`HttpStatusCode.InternalServerError` (500)**, not 403; the two failure modes are
  indistinguishable at the HTTP layer.

Everything else checked out exactly: both other Routes rows, all 12 Models-table rows (constructor
shapes, defaults — `port` 587/`useTls` true/`useSsl` false, nullability, method signatures), and every
other Architecture Notes bullet (`Email` ownership/dependency direction, nested-key config-slice
pattern, DI-graph-shape disabled-state wiring via `emailConfigElementOrNull`/`selectEmailFeature` +
`PluginTest`, storage-vs-sending independence via the shared `updateStoredEmail` helper, Angus Mail
`org.eclipse.angus:angus-mail:2.0.3` JVM-only usage confirmed in `libs.versions.toml` and
`email/server/build.gradle`, the `EmailsService`/`EmailAttachmentDataSource` streaming-attachment
bridge, the public `GET /enabled` placement outside `authenticate{}`, DI placement/ordering, client
transport-only notes, and the sample-config JSON block — byte-for-byte identical to
`server/sample.config.json`'s `"email"` object) all matched the current committed code precisely. So:
**`features/email/README.md` is fully current except for the one Root-guard sentence above** — this is
not a "manufacture busywork" situation; there genuinely is one small, real fix needed, plus (per §5
below) two adjacent repo docs that were never updated when the email feature landed.

## 5. Repo-wide stale-reference sweep (routes, config-key rename, naming-mistake trace)

- **Old flat `"smtp"` config-key convention:** no live documentation anywhere references a flat
  root-level `"smtp"` key. The only `smtp` hits outside `features/email/` are (a) the *nested* `smtp`
  object inside `server/sample.config.json`'s new `"email"` block — correct, matches the documented
  nested-key design — and (b) `agents/security-review-2026-07-09.md` (see below, out of scope).
- **`DisabledEmailsService` (the corrected naming mistake):** zero occurrences in any live doc or source
  file. Every hit is inside the historical, already-committed step reports of the prior task
  (`agents/task/10.07.2026_06.40.48-.../001-planning.md`, `002-planning.md`, `PROMPT.md`, `006-validating.md`)
  that document the mistake being made and then corrected mid-task to `DisabledEmailFeature`. Per
  `agents/PROTOCOL.md` these step files are an immutable audit trail (numbering never resets, no step
  file is ever overwritten) — they are correctly left as-is; there is nothing to fix here.
- **`isFeatureEnabled` / `EmailConfig`:** grepped repo-wide outside `agents/task/`. Every live hit is a
  correct, current reference (the interface method itself in `EmailFeature.kt`/`KtorEmailFeature.kt`/
  `EmailFeatureService.kt`/`DisabledEmailFeature.kt`/tests, plus `features/ui/adminPanel/README.md:28`'s
  `AdminPanelModel.isEmailFeatureEnabled()` row, which I spot-checked against
  `features/ui/adminPanel/src/commonMain/kotlin/Plugin.kt:138` — matches). No stale `EmailConfig`
  reference exists in any `.md` file outside `features/email/` and `agents/task/`.
- **`agents/ARCHITECTURE.md` and other `agents/*.md`:** grepped for `email`/`Email`/`smtp`/`SMTP` —
  zero hits. No precedent/example text there references the email feature at all, so nothing there is
  stale.
- **Other feature READMEs referencing email:** only `features/users/README.md` (the `email: Email? =
  null` field added to `User`/`NewUser`/`RegisteredUser`, and the note that self-service email update
  lives under `/api/email/myEmail`) and `features/ui/adminPanel/README.md` (the send-test-email form,
  `AdminPanelModel.sendTestEmail`/`isEmailFeatureEnabled`). I spot-checked both against current source —
  both are accurate and were already updated by this branch's earlier commits (visible in the diff
  --stat). No action needed on either.
- **Root `README.md`:** never touched by this branch (absent from `git diff master...fix/44-email
  --stat`). It has no "Functionality" bullet for email — but it also has none for currency, deeplinks,
  or files-as-a-feature, so that omission is consistent with the README's existing non-exhaustive style
  and is *not* a regression. However, its "Server configuration" **Key Fields table** (the one place
  that *does* try to be an exhaustive list of top-level `config.json` keys) is missing the new `email`
  key entirely, and the "Production deployment" table's `sample.config.json` row lists what to
  configure (`database`, `publicHost`, `openExchangeRatesAppId` "if you use the currency feature",
  `enableRegistration`) without any mention of `email` — this is a genuine parity gap against
  `server/sample.config.json`'s actual current shape (§6 below).
- **`agents/security-review-2026-07-09.md`:** untracked (never `git add`-ed), dated one day before
  today, a point-in-time security-audit snapshot. Its email-related code excerpts are stale relative to
  the current DI-graph-shape design — e.g. it cites a constructor
  `EmailFeatureService(get<SmtpEmailService>(), get<UsersRepo>(), get<RootAccessService>())` and a
  `smtpEmailService.sendTestEmail(recipient)` call, neither of which exist post-`5b81a9e`; no
  `RootAccessService` exists anywhere in the repo. This is an **audit artifact, not a living doc surface**
  (not a feature README, not enumerated in the Root's candidate list) — I'm treating it as out of scope
  for "update docs." Flagging only so Coding does not sweep it into the commit incidentally (per
  `agents/GIT.md`, staging must be explicit per-file, never `git add -A`, so this is a process reminder
  rather than a plan item).

## 6. Sample/example config files (`server/sample.config.json` vs `server/dev.config.json`)

There are exactly two **tracked** config files in `server/`: `sample.config.json` (production template)
and `dev.config.json` (local-dev config, used by `./gradlew :wishlist.server:run --args="dev.config.json"`
per root `README.md`). A third file, `server/local.config.json`, exists on disk but is git-ignored
(`.gitignore:14: local.*`) and contains what looks like the operator's real personal
`openExchangeRatesAppId` — it is out of scope entirely (untracked, personal, must not be touched or
staged).

Comparing the two tracked files' `plugins` arrays: every one of the 9 feature-server `JVMPlugin` entries
in `dev.config.json` also appears in `sample.config.json`, **except** `dev.config.json` is missing
`dev.inmo.wishlist.features.email.server.JVMPlugin` entirely (`sample.config.json` has it, positioned
after `booking`, before `deeplinks`). `dev.config.json` also lacks the top-level `"email"` key, which is
*correct and intentional* — omitting the key is the documented way to disable SMTP delivery, and running
without real SMTP in local dev is clearly desirable. But the **plugin itself** not being registered means
the entire feature (including the SMTP-independent `PUT /email/myEmail` storage route and `GET
/email/enabled`) is unreachable in local dev — nobody can exercise even the no-op/disabled-mode behavior
locally without hand-editing their config.

(`deeplinks` server plugin is also present in `sample.config.json` but absent from `dev.config.json` —
I checked and this predates the branch entirely, `features/deeplinks/` does not appear anywhere in
`git diff master...fix/44-email --stat`, so it is a pre-existing, unrelated gap and out of scope here.)

## 7. Conclusion — is this "find a few stale references" or "already fully current"?

Neither extreme. `features/email/README.md` is **almost entirely current** — not a "just touched
recently, unverified" situation, it genuinely matches HEAD-committed source in nearly every detail I
checked — but it is not 100% current: there is one concrete, real inaccuracy (§4's Root-guard bullet),
plus two adjacent, real gaps outside the feature README itself (§5's root `README.md` config-table
omission, §6's `dev.config.json` missing plugin registration) that were never addressed by any of the
prior tasks on this branch. This is a small, precisely-scoped fix list, not manufactured busywork — and
it is gated on resolving §3's open question first, since that uncommitted diff (if kept) would reopen
two of the Models-table rows I just verified as correct.

## 8. Concrete plan for Architecture/Coding (applies once §3 is resolved; unaffected by which way §3
resolves — these three items stand on their own)

All three items below are small, mechanical, low-risk edits with no remaining ambiguity. Per
`agents/SHORTCUTS.md` rule 4 / the Root's own constraint, items (1) and (2) are pure `.md` content and
**must run at `haiku` tier**. Item (3) is a one-line JSON array addition (not `.md` content, so the
haiku mandate does not strictly apply to it) — trivial enough to fold into the same Coding pass
regardless of tier; Architecture should decide whether to keep it in the same haiku-tier step or split
it out, but there is no design judgment involved either way.

1. **`features/email/README.md`**, Architecture Notes, "Root guard" bullet (line 55): rewrite to state
   that both `sendTest` and `myEmail` use only `getCallerUserIdOrAnswerUnauthorized()` at the routing
   layer (self-service auth, 401 on missing/invalid token); root-only enforcement for `sendTest` happens
   inside `EmailFeatureService.sendTestEmail` by comparing `caller.username.string` against the literal
   `"root"`; failure there (non-root caller *or* SMTP send failure — indistinguishable) yields **500
   Internal Server Error**, not 403. Do not touch the `## Operator Notes` section (currently empty,
   per `agents/CODING.md` convention).
2. **Root `README.md`**:
   - "Server configuration" Key Fields table (lines 86-98): add a row for the `email` key, documenting
     it as the nested SMTP-config object (`smtp: {host, port, username?, password?, from, useTls,
     useSsl}`) that enables the email feature; omitting the key (or setting it to JSON `null`) disables
     SMTP delivery while leaving per-user email-address storage (`PUT /email/myEmail`) working.
   - "Production deployment" table, `server/sample.config.json` row (line 109), "What to change before
     use" cell: add a clause analogous to the existing `openExchangeRatesAppId` one, e.g. "...and
     configure (or omit) the `email` block if you want SMTP-based test-email delivery."
3. **`server/dev.config.json`**: add `"dev.inmo.wishlist.features.email.server.JVMPlugin"` to the
   `plugins` array (same relative position as in `sample.config.json`, i.e. immediately after the
   `...currency.server.JVMPlugin`/`...booking.server.JVMPlugin` entries). Do **not** add a top-level
   `"email"` key — its absence is intentional (disables real SMTP sends in local dev) and matches the
   feature's documented "omit the key to disable" contract. Do not touch `server/local.config.json`
   (git-ignored, personal, out of scope) or `server/sample.config.json` (already correct).

No other file needs a doc change. Once §3 is resolved (and, if resolution A/discard, the two stray
working-tree files are reverted), Coding should: apply items 1-3 above, run `ast-index rebuild` only if
any `.kt`/config file with actual code semantics changed (item 3 is JSON, not `.kt`, but note
`agents/ALL.md`'s rebuild trigger is scoped to `.kt`/`.java`/`.ts`/`.js` — a config JSON edit does not
require it), then commit (per `agents/GIT.md`, Coding stages the step report *and* every changed source
file it touched — here: `features/email/README.md`, `README.md`, `server/dev.config.json`, plus its own
step report) and push per the operator's explicit "commit and push" instruction in this task's prompt
(unlike the default Orchestrator-only push rule, the operator named push directly here, so it is in
scope for this cycle's Coding/final step — see `PROMPT.md`'s own constraints section).
