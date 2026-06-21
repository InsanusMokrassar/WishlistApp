Model: claude-opus-4-8 (Opus 4.8)
Execution time: 300
Tokens used: ~70000
Changed files: agents/task/ec84a0d5-f566-4e3e-b15f-7e958dd477c8/3-validation.md

Model choice vs priority (validator priority: fable / opus / sonnet): I used **opus**. Fable is the top validator priority but is a lightweight model of uncertain availability; this validation requires careful cross-step consistency checking, reasoning about reactive-flow semantics (the `distinctUntilChanged` removal), Koin DI-by-type correctness, and an enumerate-and-prove "no edit view missed" audit — judgement work where Opus is the safe pick. Opus is the next priority after fable and the correct choice here.

---

# Validation — Issue #53: "On logout each edit view must exit (replace with its non-edit view)"

I read PROMPT.md and all three prior steps (0-planning, 1-architecture, 2-coding), then independently verified the working tree with `ast-index`/grep + full reads of every changed source file, and ran the build. Findings below. Verdict at the end.

## 1. Requirement coverage — edit-view enumeration (PASS)

Enumerated every `*EditViewModel` in `features/` (grep `EditViewModel` over `*.kt`). Exactly six ViewModel classes exist, and ALL SIX have logout-exit wiring (ctor param + `init` subscription + correct exit call):

| # | ViewModel | ctor param added | init subscription | exit call |
|---|---|---|---|---|
| 1 | `WishlistEditViewModel` (`features/ui/wishlist/.../ui/WishlistEditViewModel.kt:40,106`) | ✔ | ✔ | `when { isCreating -> onNavigateBack(node); else -> onNavigateBackToParent(node) }` |
| 2 | `WishlistItemEditViewModel` (`.../WishlistItemEditViewModel.kt:50,182`) | ✔ | ✔ | `onNavigateBackToParent(node)` (both modes) |
| 3 | `UserEditViewModel` (`features/ui/users/.../UserEditViewModel.kt:52,146`) | ✔ | ✔ | `onNavigateBack(node)` |
| 4 | `AdminUserEditViewModel` (`features/ui/adminPanel/.../AdminUserEditViewModel.kt:39,85`) | ✔ | ✔ | `onNavigateBack(node)` |
| 5 | `AdminWishlistEditViewModel` (`.../AdminWishlistEditViewModel.kt:41,94`) | ✔ | ✔ | `onNavigateBack(node)` |
| 6 | `AdminWishlistItemEditViewModel` (`.../AdminWishlistItemEditViewModel.kt:37,97`) | ✔ | ✔ | `onNavigateBack(node)` |

**No edit view was missed.** I additionally swept all `*ViewModel` files for names containing `edit`/`create` to find any non-`*Edit*`-named editor. The only borderline candidate is `WishlistItemCopyViewModel` (`features/ui/wishlist/.../WishlistItemCopyViewModel.kt`). I read it fully: it is a **copy-target picker** (loads the caller's wishlists and copies a source item into a chosen one) — it is NOT an editor of an entity's fields and is not a `*EditViewModel`. It is a legitimate exclusion (out of scope of "edit view"). All other `*ViewModel`s are read/list/auth/sidebar/topBar screens — none are editors.

Admin views (4,5,6) are deliberately INCLUDED and justified by planning §4 and architecture §2.6: keying on the global `AuthCredentialsStorage.userAuthorised` reaches the admin feature, dissolving the prior PR's exclusion. This is the most plausible correction to the prior silent rejection. Justification is sound and recorded — no omission to flag.

## 2. Shared helper correctness (PASS)

`features/common/client/src/commonMain/kotlin/utils/SubscribeOnLoggedOut.kt` read in full.
- Chain: `drop(1).filter { authorised -> !authorised }.subscribeLoggingDropExceptions(scope) { action() }`.
- `drop(1)` IS present → the StateFlow's initial replayed value does NOT fire `action`. Only a value arriving after subscription can fire. ✔ (This is the load-bearing invariant from architecture §1 / R2.)
- `filter { !authorised }` fires only on the `false` (logged-out) value → a genuine `true→false` transition; a `false→true` login is ignored. ✔
- Receiver stays generic `StateFlow<Boolean>` — `common/client` does NOT import the auth type (confirmed: imports are only micro_utils.coroutines + kotlinx.coroutines.*). ✔ Matches the architecture constraint that common/client must not depend on auth.client.
- Imports compile (build green, see §5).

**Coding deviation — removal of `distinctUntilChanged()` (SOUND).** Architecture §1 specified `distinctUntilChanged()`; Coding removed it because Kotlin deprecates (compiler-errors) calling it on a `StateFlow` ("applying distinctUntilChanged to StateFlow has no effect"). Verified reasoning: `StateFlow` by contract only emits when its value actually changes (conflated, value-equality-deduped), so `distinctUntilChanged()` is a strict no-op on it. Behavior is identical: between any two distinct emissions seen by the collector, consecutive duplicates cannot occur, so neither a spurious double `true→false` nor a redundant `false→false` can reach `filter`. Removal is correct and necessary (without it the module would not compile). KDoc documents the omission. This deviation is an improvement, not a regression.

## 3. Per-ViewModel correctness (PASS)

For each of the 6, confirmed by full read: (a) `private val authCredentialsStorage: AuthCredentialsStorage` is the 4th ctor param; (b) `authCredentialsStorage.userAuthorised.subscribeOnLoggedOut(scope) { ... }` added in the existing `init {}` after the load subscription; (c) the exit calls the correct interactor method (see table §1) — these are the SAME methods each screen's existing non-dirty Back uses, so the editor is replaced by the proper read/parent view; (d) the call **bypasses the dirty-confirm dialog** — it invokes the interactor method directly inside the helper's `action` (NOT `onBack()`, which would set `_showConfirmDialogState`), exactly mirroring `onConfirmBack()` minus the dialog toggle; (e) class KDoc updated with logout behavior + `@param authCredentialsStorage`; (f) `WishlistEditViewModel` uses `when { ... }` (not else-if) for its create/edit branch — the only VM that branches; the others are single-call.

Interactor methods all EXIST (grep over each interactor):
- `WishlistEditViewInteractor`: `onNavigateBack`, `onNavigateBackToParent`, `onSaved` ✔
- `WishlistItemEditViewInteractor`: `onNavigateBack`, `onNavigateBackToParent`, `onSaved` ✔ (logout uses `onNavigateBackToParent`, which routes EDIT→item read / CREATE→containing wishlist — matches architecture's plan-correction)
- `UserEditViewInteractor`: `onNavigateBack`, `onSaved`, `onDeleted` ✔
- `AdminUserEditViewInteractor` / `AdminWishlistEditViewInteractor` / `AdminWishlistItemEditViewInteractor`: each `onNavigateBack`, `onSaved` ✔ (no `onNavigateBackToParent` on admin — logout correctly reuses `onNavigateBack`)

All called methods are `suspend` and invoked directly inside the suspend `action`. No new interactor surface added anywhere. Consistent with planning §2/§5 and architecture §2.

## 4. Koin wiring (PASS)

Read the changed factory lines in all three Plugin.kt:
- `features/ui/wishlist/.../Plugin.kt:92` `factory { WishlistEditViewModel(it.get(), get(), get(), get()) }`
- `:93` `factory { WishlistItemEditViewModel(it.get(), get(), get(), get()) }`
- `features/ui/users/.../Plugin.kt:56` `factory { UserEditViewModel(node = it.get(), model = get(), interactor = get(), authCredentialsStorage = get()) }`
- `features/ui/adminPanel/.../Plugin.kt:76,79,80` each `EditViewModel(it.get(), get(), get(), get())`

Each factory supplies 4 args matching the 4 ctor params in order: NavigationNode via `it.get()` (parameterized injection), Model `get()`, Interactor `get()`, AuthCredentialsStorage `get()`. The new `get()` is in the trailing 4th position, matching the new 4th ctor param. The users site uses NAMED args (`authCredentialsStorage = get()`) so it is order-independent and unambiguous. Because `AuthCredentialsStorage`, the Model, and the Interactor are all distinct registered types, Koin resolves each `get()` by type — there is no wrong-type risk even though all are `get()`. Confirmed correct.

## 5. Build (PASS)

Independently ran:
```
./gradlew :wishlist.features.common.client:build :wishlist.features.ui.wishlist:build :wishlist.features.ui.users:build :wishlist.features.ui.adminPanel:build
```
Result: **BUILD SUCCESSFUL in 21s** (1611 actionable tasks: 63 executed, 1548 up-to-date). No errors; only generic Gradle-10 deprecation warnings unrelated to this change.

## 6. Conventions (PASS)

- KDoc present on the new helper and on every new/changed symbol (class KDoc + `@param authCredentialsStorage` on all 6 VMs). ✔
- No else-if chains introduced; the one branch (`WishlistEditViewModel`) uses `when`. ✔
- README changes: each of the 4 READMEs has exactly ONE appended line, all within `## Architecture Notes` / cross-cutting sections; `## Operator Notes` untouched (verified via `git diff` hunk contexts and one-line-each diff stat). ✔

## 7. Scope (PASS)

`git status --short` shows only intended changes: the 6 ViewModels, 3 Plugin.kt, 4 READMEs (all modified), 1 new helper file (untracked `SubscribeOnLoggedOut.kt`), and task step files. No stray reverts and no unrelated edits.

Two unrelated UNTRACKED items appear and are NOT part of this task's change-set (no action): `agents/task/897544f2-72b5-4335-a42a-3ccb07c562e4/` (a different task's folder, pre-existing untracked) and this task's own `agents/task/ec84a0d5-.../` folder. Neither touches source. No model/Interactor source files were modified beyond the planned set (WishlistsModel/UsersModel/AdminPanelModel untouched, as designed).

## Inconsistencies found

- **Minor (no fix needed): `distinctUntilChanged()` deviation between architecture §1 and coding.** Architecture mandated it "verbatim"; Coding removed it. This is CORRECT (compile-blocking deprecation on StateFlow; semantically a no-op). Coding documented it in 2-coding.md and the helper KDoc. Recorded here for traceability — not a defect.
- **Cosmetic (no fix needed): planning §2 table phrasing** for `WishlistItemEditViewModel` create-mode target could read as needing `onNavigateBack`; architecture §REPORT corrected this to the single `onNavigateBackToParent` for both modes, and coding implemented `onNavigateBackToParent`. Resolved across steps; correct in code.

No medium-or-higher inconsistency found.

## REPORT TO ORCHESTRATOR

**VERDICT: PASS.**

- Requirement fully met: all 6 edit ViewModels exit to their non-edit/read view on a genuine logout (`true→false`), bypassing the dirty-confirm dialog. No edit view was missed (`WishlistItemCopyViewModel` is a copy-target picker, correctly out of scope). Admin views are deliberately included and justified.
- Shared helper is correct: `drop(1)` prevents the StateFlow initial-value from firing; `filter { !it }` fires only on logout; receiver stays generic so common/client needs no auth dependency. The `distinctUntilChanged()` removal is sound (StateFlow dedupes by contract; the call is a deprecated no-op).
- Koin wiring is correct: the extra `get()` is the trailing 4th arg matching the new ctor param; resolution is by distinct type (users site uses named args).
- Build: **BUILD SUCCESSFUL** for all four affected modules.
- Conventions and scope clean; Operator Notes untouched; READMEs updated in Architecture Notes only.

No medium-or-higher problems. Nothing needs to be redone by a prior role. Implementation is ready.
