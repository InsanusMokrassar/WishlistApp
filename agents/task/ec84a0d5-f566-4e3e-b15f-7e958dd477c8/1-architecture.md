Model: claude-opus-4-8 (Opus 4.8)
Execution time: 360
Tokens used: ~95000
Changed files: agents/task/ec84a0d5-f566-4e3e-b15f-7e958dd477c8/1-architecture.md

Model choice vs priority (architecture priority: fable / opus / sonnet): I used **opus**. Fable is the top architecture priority but is a lightweight model of uncertain availability; this step requires exact cross-feature dependency reasoning, weighing a silently-rejected prior PR, precise reactive-operator semantics, and a deliberate in/out-of-scope decision for admin views — judgement work where Opus is the safe pick. Opus is the next priority after fable and is the correct choice here.

---

# Architecture / Design — Issue #53: "On logout each edit view must exit (replace with its non-edit view)"

This design builds directly on `0-planning.md`. I verified every claim in the plan against the actual source. The plan is sound; this step pins down exact signatures, operator chains, per-ViewModel wiring, Koin lines, and README text. **One correction to the plan is recorded under REPORT TO ORCHESTRATOR (the `WishlistItemEditViewModel` create-mode exit target).** No blockers.

## 0. Verified facts (source-confirmed)

- `AuthCredentialsStorage.userAuthorised: StateFlow<Boolean>` — `features/auth/client/src/commonMain/kotlin/AuthCredentialsStorage.kt:9`. `true` = logged in, `false` = logged out. This is the GLOBAL source of truth: `meStateFlow` (per `Me.kt:30`) is derived FROM `userAuthorised`, so keying on `userAuthorised` is strictly more fundamental than the prior PR's per-feature `currentUserIdFlow`.
- Gradle deps confirmed: all three UI features `api project(":wishlist.features.auth.client")` AND `api project(":wishlist.features.common.client")` (`features/ui/wishlist/build.gradle:15,18`; `features/ui/users/build.gradle:15,18`; `features/ui/adminPanel/build.gradle:15,20`). **No new gradle dependency needed.**
- `features/common/client/build.gradle` depends ONLY on `common.common` (line 15) — it does NOT and must NOT reference `auth.client`. => The shared helper MUST stay generic over `StateFlow<Boolean>` (never import the auth type).
- `dev.inmo.micro_utils.coroutines` is already imported inside `features/common/client` (`Plugin.kt:3`), so `subscribeLoggingDropExceptions` is reachable there. The prior closed branch placed the helper in this same `utils/` package — location is proven to compile.
- Every interactor already exposes the method each logout-exit needs. **NO new interactor method is required anywhere.** Confirmed by reading all six interactors.
- All four features' `## Operator Notes` are empty/no-constraint — no operator escalation required.

---

## 1. SHARED HELPER — exact specification

**File (CREATE):** `features/common/client/src/commonMain/kotlin/utils/SubscribeOnLoggedOut.kt`
**Package:** `dev.inmo.wishlist.features.common.client.utils`

**Function signature:**
```kotlin
fun StateFlow<Boolean>.subscribeOnLoggedOut(
    scope: CoroutineScope,
    action: suspend () -> Unit
)
```

Rationale for `StateFlow<Boolean>` receiver (not the prior `Flow<T?>`): the design keys on the boolean `userAuthorised` flow. Keeping the receiver generic over `StateFlow<Boolean>` means `common/client` never imports the auth type (it can't — no dependency). The prior `Flow<T?>` variant is **replaced**, not kept — nothing else uses the nullable-id form.

**Exact reactive operator chain (this is load-bearing — keep verbatim):**
```kotlin
fun StateFlow<Boolean>.subscribeOnLoggedOut(
    scope: CoroutineScope,
    action: suspend () -> Unit
) {
    distinctUntilChanged()
        .drop(1)
        .filter { authorised -> !authorised }
        .subscribeLoggingDropExceptions(scope) { action() }
}
```

Required imports:
```kotlin
import dev.inmo.micro_utils.coroutines.subscribeLoggingDropExceptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
```

**Why each operator (and why the initial value MUST NOT fire):**
- `distinctUntilChanged()` — collapse redundant identical emissions so only real transitions survive.
- `drop(1)` — discard the StateFlow's replayed CURRENT value. A `StateFlow` always re-emits its current value to every new collector; without `drop(1)` a ViewModel created while already-logged-out (`false`) would fire `action()` immediately and instantly close a freshly-opened editor. `drop(1)` guarantees only a value that ARRIVES AFTER subscription can fire. This is the single most important line; the prior PR's silent rejection was NOT about this (its drop logic was correct) but this invariant must be preserved.
- `filter { !authorised }` — fire only on the logged-OUT value, i.e. a genuine `true → false` transition (a `false → true` login is ignored).
- `subscribeLoggingDropExceptions(scope) { action() }` — ties the subscription to the ViewModel `scope` (dies when the node/ViewModel is torn down) and swallow-logs exceptions, matching every other reactive subscription in this codebase (e.g. `auth/client/Plugin.kt:53`).

**Full KDoc intent (Coding must write a KDoc of this substance; CODING.md requires KDoc on the function):**
> Subscribes [scope] to "the authenticated caller just logged out" transitions of this login-state flow and runs [action] on each one. The flow carries `true` while a user is logged in and `false` when logged out (it is `AuthCredentialsStorage.userAuthorised`). A logout is the transition from `true` to `false`. The flow's initial/replayed value is dropped via `drop(1)`, so neither a cold start that begins logged-out nor the StateFlow's initial replay fires [action] — only a genuine `true → false` change while the subscriber is alive does. Used by edit screens so that, on logout, each one exits itself (replacing the edit view with its non-edit/read view) instead of leaving an orphaned editor open for a now-anonymous user. `@param scope` Coroutine scope tied to the subscriber's lifecycle (the ViewModel scope). `@param action` Side effect to run on each logout transition (a navigation exit).

---

## 2. PER-VIEWMODEL WIRING

Common pattern for all six ViewModels:
- Add a constructor parameter `authCredentialsStorage: AuthCredentialsStorage` (type `dev.inmo.wishlist.features.auth.client.AuthCredentialsStorage`) — passed the WHOLE storage single (cleaner Koin `get()` than threading the raw flow).
- Add an `import dev.inmo.wishlist.features.auth.client.AuthCredentialsStorage` and `import dev.inmo.wishlist.features.common.client.utils.subscribeOnLoggedOut`.
- In the EXISTING `init { ... }` block, AFTER the existing load subscription, add the logout subscription.
- The interactor methods are already `suspend`; call them directly inside `action` (no extra `launchLoggingDropExceptions` — `subscribeLoggingDropExceptions` already provides scope + error handling).
- **The logout exit MUST BYPASS the dirty-changes confirm dialog.** Do NOT call `onBack()` (it sets `_showConfirmDialogState.value = true` when dirty). Call the interactor method directly — exactly what `onConfirmBack()` does, minus the dialog toggle. A logged-out user must never be trapped behind a confirm modal.

### 2.1 `WishlistEditViewModel` (`features/ui/wishlist/src/commonMain/kotlin/ui/WishlistEditViewModel.kt`)
- Param position: 4th (after `interactor`): `private val authCredentialsStorage: AuthCredentialsStorage`.
- `init` addition:
  ```kotlin
  authCredentialsStorage.userAuthorised.subscribeOnLoggedOut(scope) {
      if (isCreating) {
          interactor.onNavigateBack(node)
      } else {
          interactor.onNavigateBackToParent(node)
      }
  }
  ```
- Modes: CREATE → `onNavigateBack(node)` (pop the create form — there is no parent entity). EDIT → `onNavigateBackToParent(node)` (replace with the wishlist detail/read view `WishlistViewConfig`). This mirrors the existing private `navigateBack()` exactly.
- Methods confirmed on `WishlistEditViewInteractor`: `onNavigateBack`, `onNavigateBackToParent`, `onSaved`. ✔ No new method.

### 2.2 `WishlistItemEditViewModel` (`features/ui/wishlist/src/commonMain/kotlin/ui/WishlistItemEditViewModel.kt`)
- Param position: 4th (after `interactor`): `private val authCredentialsStorage: AuthCredentialsStorage`.
- `init` addition:
  ```kotlin
  authCredentialsStorage.userAuthorised.subscribeOnLoggedOut(scope) {
      interactor.onNavigateBackToParent(node)
  }
  ```
- Modes: BOTH modes → `onNavigateBackToParent(node)`. Per the interactor's own KDoc (`WishlistItemEditViewInteractor.kt:20-30`), `onNavigateBackToParent` already handles both modes from `node.config`: EDIT → item read view (`WishlistItemViewConfig`); CREATE → containing wishlist detail (`WishlistViewConfig`). This is precisely the existing `onBack()` non-dirty branch (line 308) and `onConfirmBack()` (line 315). ✔ No new method.
- NOTE (plan correction): the planning doc's table said create-mode target is "containing wishlist detail view" and that is CORRECT, but it is reached via `onNavigateBackToParent` (NOT `onNavigateBack`). Both modes use the single `onNavigateBackToParent` call. See REPORT TO ORCHESTRATOR.

### 2.3 `UserEditViewModel` (`features/ui/users/src/commonMain/kotlin/ui/UserEditViewModel.kt`)
- Param position: 4th (after `interactor`): `private val authCredentialsStorage: AuthCredentialsStorage`.
- `init` addition:
  ```kotlin
  authCredentialsStorage.userAuthorised.subscribeOnLoggedOut(scope) {
      interactor.onNavigateBack(node)
  }
  ```
- Both modes (the screen is edit-only; there is no create mode): `onNavigateBack(node)` — pops to the underlying profile (read) view, identical to the existing non-dirty Back path (line 196). ✔ Methods on `UserEditViewInteractor`: `onNavigateBack`, `onSaved`, `onDeleted`. No new method.

### 2.4 `AdminUserEditViewModel` (`features/ui/adminPanel/src/commonMain/kotlin/ui/AdminUserEditViewModel.kt`)
- Param position: 4th (after `interactor`): `private val authCredentialsStorage: AuthCredentialsStorage`.
- `init` addition:
  ```kotlin
  authCredentialsStorage.userAuthorised.subscribeOnLoggedOut(scope) {
      interactor.onNavigateBack(node)
  }
  ```
- Both modes (create/edit) → `onNavigateBack(node)` — pops to the admin user read/list view, identical to existing non-dirty Back (line 108). ✔ `AdminUserEditViewInteractor` exposes `onNavigateBack`, `onSaved`. No new method.

### 2.5 `AdminWishlistEditViewModel` (`features/ui/adminPanel/src/commonMain/kotlin/ui/AdminWishlistEditViewModel.kt`)
- Param position: 4th (after `interactor`): `private val authCredentialsStorage: AuthCredentialsStorage`.
- `init` addition:
  ```kotlin
  authCredentialsStorage.userAuthorised.subscribeOnLoggedOut(scope) {
      interactor.onNavigateBack(node)
  }
  ```
- Both modes → `onNavigateBack(node)` — pops to admin wishlist read/list view (line 117). ✔ `AdminWishlistEditViewInteractor` exposes `onNavigateBack`, `onSaved`. No new method.

### 2.6 `AdminWishlistItemEditViewModel` (`features/ui/adminPanel/src/commonMain/kotlin/ui/AdminWishlistItemEditViewModel.kt`)
- Param position: 4th (after `interactor`): `private val authCredentialsStorage: AuthCredentialsStorage`.
- `init` addition:
  ```kotlin
  authCredentialsStorage.userAuthorised.subscribeOnLoggedOut(scope) {
      interactor.onNavigateBack(node)
  }
  ```
- Both modes → `onNavigateBack(node)` — pops to admin item read view (line 124). ✔ `AdminWishlistItemEditViewInteractor` exposes `onNavigateBack`, `onSaved`. No new method.

**Admin scope decision: INCLUDE all three admin views.** The issue says "EACH edit view"; the prior PR explicitly excluded admin (its `currentUserIdFlow` could not reach `AdminPanelModel`) and was silently closed. Switching to the global `userAuthorised` dissolves that blocker — admin already depends on `auth.client`. Root-gating controls ENTRY, not an already-open editor; a root user editing an admin form who logs out is exactly issue #53's orphaned-editor case. Including admin is consistent and low-risk (same one-line pattern). This is the most plausible correction to the rejection.

**The Model layer is NOT touched.** Because `AuthCredentialsStorage` is a Koin `single`, each ViewModel factory supplies it via an extra `get()`. `WishlistsModel`, `UsersModel`, `AdminPanelModel` stay unchanged.

---

## 3. KOIN FACTORY CHANGES (exact current → new lines)

Each affected `factory { ... }` line gains ONE extra trailing `get()` (resolving the `AuthCredentialsStorage` single).

**File `features/ui/wishlist/src/commonMain/kotlin/Plugin.kt`:**
- Line 92 — current: `factory { WishlistEditViewModel(it.get(), get(), get()) }`
  → new: `factory { WishlistEditViewModel(it.get(), get(), get(), get()) }`
- Line 93 — current: `factory { WishlistItemEditViewModel(it.get(), get(), get()) }`
  → new: `factory { WishlistItemEditViewModel(it.get(), get(), get(), get()) }`

**File `features/ui/users/src/commonMain/kotlin/Plugin.kt`:**
- Line 56 — current: `factory { UserEditViewModel(node = it.get(), model = get(), interactor = get()) }`
  → new: `factory { UserEditViewModel(node = it.get(), model = get(), interactor = get(), authCredentialsStorage = get()) }`
  (named args are used at this site — keep the named style and add `authCredentialsStorage = get()`.)

**File `features/ui/adminPanel/src/commonMain/kotlin/Plugin.kt`:**
- Line 76 — current: `factory { AdminUserEditViewModel(it.get(), get(), get()) }`
  → new: `factory { AdminUserEditViewModel(it.get(), get(), get(), get()) }`
- Line 79 — current: `factory { AdminWishlistEditViewModel(it.get(), get(), get()) }`
  → new: `factory { AdminWishlistEditViewModel(it.get(), get(), get(), get()) }`
- Line 80 — current: `factory { AdminWishlistItemEditViewModel(it.get(), get(), get()) }`
  → new: `factory { AdminWishlistItemEditViewModel(it.get(), get(), get(), get()) }`

No new imports are needed in the Plugin.kt files (the extra `get()` resolves by type; `AuthCredentialsStorage` is referenced only in the ViewModel files). `AuthCredentialsStorage` is already registered as a Koin `single` per platform in `auth/client` plugins, so each plugin's DI graph already contains it transitively.

---

## 4. EXACT FILE CHANGE LIST (for Coding)

CREATE (1):
1. `features/common/client/src/commonMain/kotlin/utils/SubscribeOnLoggedOut.kt` — the generic `StateFlow<Boolean>.subscribeOnLoggedOut(scope, action)` helper (chain + KDoc per §1).

MODIFY — ViewModels (6) — add `authCredentialsStorage: AuthCredentialsStorage` ctor param (4th position), two imports, and the `init` logout subscription (§2). Update each class KDoc with an `@param authCredentialsStorage Login-state source; on logout this screen exits to its non-edit view.` line:
2. `features/ui/wishlist/src/commonMain/kotlin/ui/WishlistEditViewModel.kt`
3. `features/ui/wishlist/src/commonMain/kotlin/ui/WishlistItemEditViewModel.kt`
4. `features/ui/users/src/commonMain/kotlin/ui/UserEditViewModel.kt`
5. `features/ui/adminPanel/src/commonMain/kotlin/ui/AdminUserEditViewModel.kt`
6. `features/ui/adminPanel/src/commonMain/kotlin/ui/AdminWishlistEditViewModel.kt`
7. `features/ui/adminPanel/src/commonMain/kotlin/ui/AdminWishlistItemEditViewModel.kt`

MODIFY — Plugins (3) — add one extra `get()` to the six factory lines (§3):
8. `features/ui/wishlist/src/commonMain/kotlin/Plugin.kt` (lines 92, 93)
9. `features/ui/users/src/commonMain/kotlin/Plugin.kt` (line 56)
10. `features/ui/adminPanel/src/commonMain/kotlin/Plugin.kt` (lines 76, 79, 80)

MODIFY — READMEs (4) — append the §5 text to each `## Architecture Notes` (NEVER touch `## Operator Notes`). This README update is performed by Coding in a LATER step, not by Architecture:
11. `features/common/README.md`
12. `features/ui/wishlist/README.md`
13. `features/ui/users/README.md`
14. `features/ui/adminPanel/README.md`

After source changes: run `ast-index rebuild` (mandatory per ALL.md).

---

## 5. README `## Architecture Notes` TEXT (Coding appends; do NOT touch Operator Notes)

**Append to `features/common/README.md` `## Architecture Notes`:**
> - **Logout-exit helper (`utils/SubscribeOnLoggedOut.kt`, issue #53):** `fun StateFlow<Boolean>.subscribeOnLoggedOut(scope, action)` runs `action` exactly once per genuine logged-in→logged-out transition of a login-state flow. Chain: `distinctUntilChanged().drop(1).filter { !it }.subscribeLoggingDropExceptions(scope) { action() }`. `drop(1)` discards the StateFlow's replayed current value so neither a cold start that begins logged-out nor the initial replay fires `action` — only a `true→false` change after subscription does. The helper stays generic over `StateFlow<Boolean>` (common/client has no dependency on `auth.client`); callers pass `AuthCredentialsStorage.userAuthorised`. Consumed by all six edit ViewModels (`ui/wishlist`, `ui/users`, `ui/adminPanel`) to exit an open editor to its read view on logout.

**Append to `features/ui/wishlist/README.md` `## Architecture Notes`:**
> - **Logout exits open editors (issue #53):** `WishlistEditViewModel` and `WishlistItemEditViewModel` each take an `AuthCredentialsStorage` (4th ctor param, injected via `get()` in `Plugin.kt`) and subscribe `userAuthorised.subscribeOnLoggedOut(scope) { ... }` (helper in `features/common/client` `utils/`) in `init`. On logout the editor exits to its non-edit view, **bypassing the dirty-changes confirm dialog** (logout is forced, not user-cancelable): `WishlistEditViewModel` → `onNavigateBack` (CREATE) / `onNavigateBackToParent` (EDIT, → wishlist detail); `WishlistItemEditViewModel` → `onNavigateBackToParent` (EDIT → item read view, CREATE → containing wishlist). No new interactor methods; reuses the existing contextual-Back surface. The subscription is scoped to the ViewModel, so it dies when the node is removed.

**Append to `features/ui/users/README.md` `## Architecture Notes`:**
> - **Logout exits the open editor (issue #53):** `UserEditViewModel` takes an `AuthCredentialsStorage` (4th ctor param, injected via `get()` in `Plugin.kt`) and subscribes `userAuthorised.subscribeOnLoggedOut(scope) { interactor.onNavigateBack(node) }` (helper in `features/common/client` `utils/`) in `init`. On logout the edit screen pops to the underlying profile (read) view, **bypassing the dirty-changes confirm dialog**. Reuses the existing `onNavigateBack` surface — no new interactor method.

**Append to `features/ui/adminPanel/README.md` `## Architecture Notes`:**
> - **Logout exits open admin editors (issue #53):** `AdminUserEditViewModel`, `AdminWishlistEditViewModel`, and `AdminWishlistItemEditViewModel` each take an `AuthCredentialsStorage` (4th ctor param, injected via `get()` in `Plugin.kt`) and subscribe `userAuthorised.subscribeOnLoggedOut(scope) { interactor.onNavigateBack(node) }` (helper in `features/common/client` `utils/`) in `init`. On logout each admin editor pops to its read/list view, **bypassing the dirty-changes confirm dialog**. Admin views are deliberately IN scope (issue #53 says "each edit view"): keying on the global `AuthCredentialsStorage.userAuthorised` reaches the admin feature, which the prior per-feature-`currentUserIdFlow` approach could not. Root-gating controls entry, not an already-open editor. No new interactor methods.

---

## 6. BUILD COMMANDS to verify (and ast-index)

After Coding's source changes, run `ast-index rebuild` FIRST (mandatory per ALL.md), then build the four affected modules:
```
./gradlew :wishlist.features.common.client:build \
          :wishlist.features.ui.wishlist:build \
          :wishlist.features.ui.users:build \
          :wishlist.features.ui.adminPanel:build
```
Or individually:
```
./gradlew :wishlist.features.common.client:build
./gradlew :wishlist.features.ui.wishlist:build
./gradlew :wishlist.features.ui.users:build
./gradlew :wishlist.features.ui.adminPanel:build
```
Definition of Done requires `BUILD SUCCESSFUL`. Module path names are the dotted gradle names per the Architecture doc convention (`features/ui/wishlist` → `:wishlist.features.ui.wishlist`).

---

## 7. Risks / edge cases

- **R1 — double-navigation:** If logout fires while the node is already being torn down (e.g. user logged out via a different path that also pops), the interactor's `replaceLastOrBackUntil` / `pop` could be a no-op or run twice. Mitigated: `subscribeLoggingDropExceptions` swallow-logs any exception, and the subscription is scoped to the dying ViewModel. The `actionOrBackUntil`/`replaceLastOrBackUntil` helpers are idempotent-ish (back-up-to-existing-node), so a second call is harmless. No code action; Validation should confirm no crash on rapid logout.
- **R2 — initial-value firing (critical):** `userAuthorised` is a `StateFlow` and always replays its current value to a new collector. WITHOUT `drop(1)` a ViewModel created while logged-out would instantly close itself. `drop(1)` is mandatory and is enforced in §1. Validation must check the chain keeps `drop(1)`.
- **R3 — subscription lifecycle (scope death on node removal):** the subscription uses the ViewModel `scope` (from `ViewModel<ViewConfig>(node)`), which is cancelled when the node leaves the chain. So the logout handler cannot fire for a removed editor — correct.
- **R4 — create-mode vs edit-mode exit targets:** handled per §2. Wishlist edit splits CREATE (`onNavigateBack`) vs EDIT (`onNavigateBackToParent`); wishlist-item edit uses the single mode-aware `onNavigateBackToParent`; users + all admin views are pop-to-read (`onNavigateBack`) for both modes. All targets are the SAME as each screen's existing non-dirty Back, so behavior is consistent and already-validated navigation paths are reused.
- **R5 — confirm-dialog bypass (esp. admin):** every edit ViewModel routes Back through `onBack()` which raises a discard modal when dirty. The logout handler MUST NOT call `onBack()`; it calls the interactor directly (like `onConfirmBack()` without the dialog toggle). This is explicit in §2 and called out in every README note. Validation must confirm a dirty editor logs out WITHOUT showing/awaiting the confirm modal.
- **R6 — `WishlistsModel.currentUserIdFlow` left unused for this purpose:** the model still exposes `currentUserIdFlow` for ownership logic; it is intentionally NOT used for logout detection (global `userAuthorised` replaces it). No removal required — out of scope.

---

## REPORT TO ORCHESTRATOR

One correction and one confirmation for the previous (planning) stage; neither is a blocker:

1. **`WishlistItemEditViewModel` create-mode exit target — interactor method correction.** Planning §2 table row 2 lists the exit method as `onNavigateBackToParent` for both modes, which is CORRECT, but planning §5's per-ViewModel action list also says "`interactor.onNavigateBackToParent(node)`" — consistent. However planning §2 table's "CREATE mode" column phrasing ("containing wishlist detail view") could be misread as needing `onNavigateBack`. CLARIFIED here: BOTH modes call the single `onNavigateBackToParent(node)`, which internally routes CREATE → containing wishlist (`WishlistViewConfig`) and EDIT → item read view (`WishlistItemViewConfig`) from `node.config` (per the interactor's own KDoc). Coding must use `onNavigateBackToParent` for both modes of the item editor — do NOT split it. This matches the existing `onBack()`/`onConfirmBack()` non-dirty path exactly.

2. **Helper receiver type finalized as `StateFlow<Boolean>` (plan left this as a recommendation).** Decision: replace the prior `Flow<T?>` helper entirely with the `StateFlow<Boolean>` variant. `common/client` has no dependency on `auth.client`, so the helper must stay generic over the boolean flow; callers pass `AuthCredentialsStorage.userAuthorised`. The ViewModels receive the whole `AuthCredentialsStorage` single (not the raw flow) for the cleanest Koin `get()`.

No gaps in the plan block implementation. Design is code-ready.
