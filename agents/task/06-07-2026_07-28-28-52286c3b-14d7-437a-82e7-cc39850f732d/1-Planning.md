Model: claude-fable-5
Execution time: ~180
Tokens used: ~50000
Changed files: agents/task/06-07-2026_07-28-28-52286c3b-14d7-437a-82e7-cc39850f732d/1-Planning.md

# 1-Planning — merge-conflict resolution plan for PR #63 (`fix/44-email` ← `origin/master`)

MODEL CHOICE ARGUMENT: role=planning; `agents/local.ALL.md` priority list for planning = `fable` > `opus` > `sonnet`; model `fable` (claude-fable-5) available → `fable` used.

CONTEXT:
- branch=`fix/44-email`; HEAD=`3417880` (`feat(email): base email feature (#44)`); MERGE_HEAD=`36d1d9e` (`origin/master`); merge-base=`37c520e`.
- `ours` (HEAD, marker `<<<<<<< HEAD`) = email feature: modules `features/email/{common,server,client}`, adminPanel test-email UI, `smtp` config key, angus-mail dep (inside `features/email/server`, NOT conflicted).
- `theirs` (marker `>>>>>>> origin/master`) = master: Calm Studio redesign, topbar-search-disable, deeplinks feature `features/deeplinks/{common,server,client}`, logout-exits-admin-editors (issue #53).
- Conflict count: 9 files, EXACTLY 1 conflict hunk per file. `features/ui/adminPanel/src/commonMain/kotlin/ui/AdminPanelModel.kt` (listed in PROMPT.md as 10th) is ALREADY auto-merged and staged: interface contains `userAuthorisedState: StateFlow<Boolean>` (line 28, theirs) AND `isEmailFeatureEnabled()` (line 149) AND `sendTestEmail(recipient: Email)` (line 159) (ours). Coder MUST NOT touch `AdminPanelModel.kt`.
- Working tree also contains ~40 auto-merged (`M`/`A`) files from master (deeplinks sources, Calm Studio files, `SubscribeOnLoggedOut.kt`). Coder MUST NOT edit auto-merged files.

GLOBAL RESOLUTION RULE (applies to files 1,2,3,4,7,8,9): both sides ADD independent registrations at the SAME insertion point (email vs deeplinks). Resolution = UNION. Line order convention = `email` line FIRST, `deeplinks` line SECOND (ours-then-theirs), IDENTICAL order in every file. No logical-line collision: email and deeplinks are independent plugins/modules; registration order between the two plugins has no runtime dependency.

FORBIDDEN FOR CODER UNTIL ALL 9 FILES RESOLVED: `git merge --abort`, editing non-conflicted files, reordering surrounding lines, reformatting.

---

## Per-file directives

### 1. `client/android/src/main/kotlin/MainActivity.kt` — UNION
Conflict hunk = lines 38–42 (working tree). Replace whole hunk (markers included) with:
```kotlin
                        dev.inmo.wishlist.features.email.client.AndroidPlugin,
                        dev.inmo.wishlist.features.deeplinks.client.AndroidPlugin,
```
Surrounding blank line (line 43) + `ui.*` plugin block stay unchanged. Indent = 24 spaces (match neighbours).

### 2. `client/build.gradle` — UNION
Conflict hunk = lines 29–33. Replace whole hunk with:
```groovy
                api project(":wishlist.features.email.client")
                api project(":wishlist.features.deeplinks.client")
```
Indent = 16 spaces (match `":wishlist.features.booking.client"` line above).

### 3. `client/src/jsMain/kotlin/Main.kt` — UNION
Conflict hunk = lines 27–31. Replace whole hunk with:
```kotlin
                        dev.inmo.wishlist.features.email.client.JSPlugin,
                        dev.inmo.wishlist.features.deeplinks.client.JSPlugin,
```
Blank line after hunk + `ui.*` block stay unchanged. Indent = 24 spaces.

### 4. `client/src/jvmMain/kotlin/Main.kt` — UNION
Conflict hunk = lines 23–27. Replace whole hunk with:
```kotlin
                dev.inmo.wishlist.features.email.client.JVMPlugin,
                dev.inmo.wishlist.features.deeplinks.client.JVMPlugin,
```
Blank line after hunk + `ui.*` block stay unchanged. Indent = 16 spaces.

### 5. `features/ui/adminPanel/README.md` — UNION (documentation, both bullet blocks additive)
Conflict hunk = lines 39–44 (tail of `## Architecture Notes`). Both sides append bullets to the same list end; zero same-line collision. Replace whole hunk with ours-block then theirs-block, VERBATIM:
```markdown
- **Email section (added in issue #44):** Dashboard (`AdminPanelView`) gains a `CalmTextField` + `CalmButton` row for sending a test email. Input validated via `Email.parse(...)` before calling `viewModel.onSendTestEmail(recipient)`. `AdminPanelViewModel.sendTestEmailState: StateFlow<Boolean?>` holds result (`null` = not yet attempted). Real authorization is server-side (`requireRoot` on `POST /api/email/sendTest`). Requires `api project(":wishlist.features.email.client")` in `build.gradle`.
- **Note:** The "JS views use Bootstrap CSS classes" bullet in the original notes is stale — JS views use Calm Studio components only.
- **Logout exits open admin editors (issue #53):** `AdminUserEditViewModel`, `AdminWishlistEditViewModel`, and `AdminWishlistItemEditViewModel` route logout-exit through `AdminPanelModel.userAuthorisedState` and subscribe `model.userAuthorisedState.subscribeOnLoggedOut(scope) { interactor.onNavigateBack(node) }` (helper in `features/common/client` `utils/`) in `init`. On logout each admin editor pops to its read/list view, **bypassing the dirty-changes confirm dialog**. The model exposes `userAuthorisedState: StateFlow<Boolean>` so edit ViewModels exit via the model layer (MVVM boundary) instead of importing auth storage directly. Reuses existing `interactor.onNavigateBack` surface — no new interactor methods.
```
Constraint: `## Operator Notes` section (lines 3–5) untouched (empty, no conflict). Known residual inconsistency: line 37 bullet "JS views use Bootstrap CSS classes" stays stale on BOTH parents; ours-side "stale" note documents the fact. Rewriting line 37 = OUT OF SCOPE for the merge commit (line 37 is not conflicted); optional follow-up task for operator, NOT a merge blocker.

### 6. `features/ui/adminPanel/src/commonMain/kotlin/Plugin.kt` — UNION (looks semantic, is additive)
Conflict hunk = lines 87–91, inside `single<AdminPanelModel> { ... }` before `object : AdminPanelModel {`. BOTH locals are REQUIRED by the already-auto-merged anonymous-object body: `credentialsStorage.userAuthorised` used at line 93 (theirs, #53), `email.isFeatureEnabled()` / `email.sendTestEmail(...)` used at lines 140–144 (ours, #44). Replace whole hunk with:
```kotlin
            val email = get<EmailFeature>()
            val credentialsStorage = get<AuthCredentialsStorage>()
```
Imports ALREADY merged outside hunk: line 6 `EmailFeature`, line 7 `Email`, line 9 `AuthCredentialsStorage` — do not edit imports. DI prerequisite satisfied by file-1/3/4 unions: `features.email.client` plugin registers `EmailFeature`; `features.auth.client` plugin (registered on both parents) provides `AuthCredentialsStorage`.

### 7. `server/build.gradle` — UNION
Conflict hunk = lines 24–28. Replace whole hunk with:
```groovy
    api project(":wishlist.features.email.server")
    api project(":wishlist.features.deeplinks.server")
```
Indent = 4 spaces. angus-mail dep lives in `features/email/server/build.gradle` (not conflicted) — no action.

### 8. `server/sample.config.json` — UNION + JSON-syntax fix (comma)
Conflict hunk = lines 23–27, tail of `"plugins"` array. Each parent's line was array-last → neither has trailing comma. Union REQUIRES comma after email line; deeplinks line becomes array-last WITHOUT trailing comma:
```json
    "dev.inmo.wishlist.features.email.server.JVMPlugin",
    "dev.inmo.wishlist.features.deeplinks.server.JVMPlugin"
```
`"smtp": null` key (line 29) already auto-merged from ours — keep, do not duplicate. MANDATORY post-edit check: JSON parses (`python3 -m json.tool server/sample.config.json > /dev/null`).

### 9. `settings.gradle` — UNION
Conflict hunk = lines 40–48, inside `includes` array between `booking` group and `ui` group. Replace whole hunk with both module triples, blank line between groups (file style = blank line per feature group):
```groovy
    ":features:email:common",
    ":features:email:server",
    ":features:email:client",

    ":features:deeplinks:common",
    ":features:deeplinks:server",
    ":features:deeplinks:client",
```
Blank line after hunk (before `":features:ui:sample"`) already present — do not duplicate. Both `features/email/` and `features/deeplinks/` directories confirmed present on disk.

---

## Coder execution order

1. action=edit; targets=9 files above; per-file directive as specified; markers `<<<<<<< HEAD` / `=======` / `>>>>>>> origin/master` fully removed.
2. check=`grep -rn '^<<<<<<<\|^=======$\|^>>>>>>>' client features server settings.gradle`; expected=empty output.
3. check=`python3 -m json.tool server/sample.config.json > /dev/null`; expected=exit 0.
4. action=`ast-index rebuild` (source files changed — mandatory per `agents/ALL.md`).
5. action=`git add` the 9 resolved paths (allowed for Coder step, NOT for Planning).
6. build verification (minimum set):
   - `./gradlew :wishlist.server:compileKotlin`
   - `./gradlew :wishlist.client:compileKotlinJvm :wishlist.client:compileKotlinJs`
   - optional Android: `./gradlew :wishlist.client.android:compileDebugKotlin` (module id per settings.gradle transform = `wishlist.client.android`; verify exact task name via `./gradlew :wishlist.client.android:tasks` if first form fails).
7. runtime-surface sanity (no execution required for merge): confirm `features/ui/adminPanel/src/commonMain/kotlin/ui/AdminPanelModel.kt` still contains `userAuthorisedState` + `isEmailFeatureEnabled` + `sendTestEmail` (already true; regression guard).
8. action=`git commit` (concludes merge; keep default merge message `Merge remote-tracking branch 'origin/master' into fix/44-email` or equivalent normal-style message; end body with `Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>`); never stage `agents/task/`.
9. NO push (Orchestrator scope). NO edits to auto-merged files.

## Risk register

- risk=missed comma in `server/sample.config.json` → server config parse failure at startup; mitigation=step 3 check.
- risk=dropping one of two `val` locals in `Plugin.kt` → compile error (`unresolved reference: email` or `credentialsStorage`); mitigation=directive 6 keeps both.
- risk=editing auto-merged Calm Studio / deeplinks files → feature regression; mitigation=explicit prohibition; `git status` diff review before commit.
- risk=stale "Bootstrap" bullet at `features/ui/adminPanel/README.md:37` remains after merge; severity=doc-only; decision=leave as-is; operator MAY order follow-up cleanup task.

## Operator judgement required

NONE. All 9 conflicts are additive dual-feature insertions; zero same-logical-line semantic collisions. Single doc inconsistency (README `Bootstrap` bullet, item above) is pre-existing on both parents and non-blocking.
