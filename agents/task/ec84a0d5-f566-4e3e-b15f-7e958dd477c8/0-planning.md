Model: claude-opus-4-8 (Opus 4.8)
Execution time: 300
Tokens used: ~78000
Changed files: agents/task/ec84a0d5-f566-4e3e-b15f-7e958dd477c8/0-planning.md

Model choice vs priority list (planning priority: fable / opus / sonnet): I used **opus**. Fable is the top planning priority but is a lightweight/uncertain-availability model; this task requires careful cross-feature dependency reasoning, weighing a rejected prior PR, and an explicit in/out-of-scope architecture decision — exactly the kind of judgement Opus is the safe choice for. Opus is the next priority after fable and is the appropriate pick here.

---

# Implementation Plan — Issue #53: "On logout each edit view must implement its exit (replace with its non-edit view)"

## 1. Summary of the chosen design

On logout, every edit ViewModel must call the same contextual "back / exit" interactor method it already calls on a plain Back, so the edit view is replaced by its non-edit (read) view (or popped in create mode). Detection of logout is done by subscribing to the **global** `AuthCredentialsStorage.userAuthorised: StateFlow<Boolean>` (`features/auth/client`), reacting only to a genuine `true → false` (logged-in → logged-out) transition.

This replaces the prior PR's per-feature `currentUserIdFlow` approach, which could not reach the admin feature (`AdminPanelModel` has no auth/me flow) and therefore had to exclude admin — the likely reason for the silent rejection.

**Admin edit views ARE in scope** (see §4).

## 2. Enumeration of every edit view

| # | Edit view / ViewModel | Feature | Model | Interactor | Exit method to use on logout | Resulting non-edit view (EDIT mode) | Resulting view (CREATE mode) |
|---|---|---|---|---|---|---|---|
| 1 | `WishlistEditViewModel` (`features/ui/wishlist/.../ui/WishlistEditViewModel.kt:28`) | ui/wishlist | `WishlistsModel` | `WishlistEditViewInteractor` | `onNavigateBackToParent` (edit) / `onNavigateBack` (create) | wishlist detail (read) view | pop the create form |
| 2 | `WishlistItemEditViewModel` (`features/ui/wishlist/.../ui/WishlistItemEditViewModel.kt:37`) | ui/wishlist | `WishlistsModel` | `WishlistItemEditViewInteractor` | `onNavigateBackToParent` (both modes) | item read view | containing wishlist detail view |
| 3 | `UserEditViewModel` (`features/ui/users/.../ui/UserEditViewModel.kt:42`) | ui/users | `UsersModel` | `UserEditViewInteractor` | `onNavigateBack` | pops to underlying profile (read) view | same pop |
| 4 | `AdminUserEditViewModel` (`features/ui/adminPanel/.../ui/AdminUserEditViewModel.kt:29`) | ui/adminPanel | `AdminPanelModel` | `AdminUserEditViewInteractor` | `onNavigateBack` | admin user read/list view (pop) | same pop |
| 5 | `AdminWishlistEditViewModel` (`features/ui/adminPanel/.../ui/AdminWishlistEditViewModel.kt:31`) | ui/adminPanel | `AdminPanelModel` | `AdminWishlistEditViewInteractor` | `onNavigateBack` | admin wishlist read/list view (pop) | same pop |
| 6 | `AdminWishlistItemEditViewModel` (`features/ui/adminPanel/.../ui/AdminWishlistItemEditViewModel.kt:27`) | ui/adminPanel | `AdminPanelModel` | `AdminWishlistItemEditViewInteractor` | `onNavigateBack` | admin item read view (pop) | same pop |

Notes:
- The three admin interactors expose only `onNavigateBack` + `onSaved` (no `onNavigateBackToParent`). So for admin views the logout exit reuses `onNavigateBack`, identical to their existing non-dirty Back path. No new interactor surface needed anywhere.
- For #1, replicate the existing `navigateBack()` branch logic (create → `onNavigateBack`, edit → `onNavigateBackToParent`).
- IMPORTANT — bypass the dirty-confirm dialog. On logout the view MUST exit unconditionally. Do NOT call `onBack()` (which shows the discard-changes dialog when dirty). Call the underlying interactor method directly inside `scope.launchLoggingDropExceptions { ... }`, exactly like `onConfirmBack()` does. A logged-out user must never be trapped behind a confirm modal.

## 3. Dependency-graph verification (build.gradle inspection)

`AuthCredentialsStorage` lives in `:wishlist.features.auth.client` (`features/auth/client/src/commonMain/kotlin/AuthCredentialsStorage.kt:8`) and is registered as a Koin `single<AuthCredentialsStorage>` per platform (android/js/jvm Plugins).

Direct `api project(":wishlist.features.auth.client")` dependency confirmed in all three UI features:
- `features/ui/wishlist/build.gradle:18`
- `features/ui/users/build.gradle:18`
- `features/ui/adminPanel/build.gradle:20`

=> All three features can import the `AuthCredentialsStorage` type and resolve it from Koin. **No new gradle dependency is required.** `features/common/client` does NOT depend on auth.client, so the shared helper must NOT reference the auth type (kept generic — see §5).

Each edit ViewModel is built in its feature's `commonMain/.../Plugin.kt` via a Koin `factory { ... }` line:
- `features/ui/wishlist/.../Plugin.kt:92` `factory { WishlistEditViewModel(it.get(), get(), get()) }`
- `features/ui/wishlist/.../Plugin.kt:93` `factory { WishlistItemEditViewModel(it.get(), get(), get()) }`
- `features/ui/users/.../Plugin.kt:56` `factory { UserEditViewModel(node = it.get(), model = get(), interactor = get()) }`
- `features/ui/adminPanel/.../Plugin.kt:76` `factory { AdminUserEditViewModel(it.get(), get(), get()) }`
- `features/ui/adminPanel/.../Plugin.kt:79` `factory { AdminWishlistEditViewModel(it.get(), get(), get()) }`
- `features/ui/adminPanel/.../Plugin.kt:80` `factory { AdminWishlistItemEditViewModel(it.get(), get(), get()) }`

Because `AuthCredentialsStorage` is a Koin `single`, each factory can supply it with an additional `get()`. **The signal does NOT need to be threaded through any Model.** This keeps `WishlistsModel`, `UsersModel`, and `AdminPanelModel` untouched.

## 4. Admin scope decision — INCLUDE admin views

DECISION: admin edit views (#4, #5, #6) are **IN scope**.

Justification:
- The issue text says "EACH edit view" with no exclusion. The prior PR explicitly excluded admin and was silently closed — including admin is the most plausible correction.
- The previous exclusion rationale ("`AdminPanelModel` exposes no auth/me flow") is dissolved by switching to the global `AuthCredentialsStorage.userAuthorised`, which is available to the admin feature (it already depends on auth.client). There is no hard blocker.
- "admin is root-gated separately" gates *entry*, not *open editors*: a root user editing an admin form who logs out is exactly the orphaned-editor case #53 describes. Including admin is consistent and low-risk (same one-line pattern).

## 5. Shared helper specification

**Location:** `features/common/client/src/commonMain/kotlin/utils/SubscribeOnLoggedOut.kt` (same path the prior PR used; reuse it, do not invent a new module).

**Why common/client:** all four target features already `api project(":wishlist.features.common.client")` (confirmed in each build.gradle), so the helper is reachable everywhere with no new dependency. The helper must stay generic over a `StateFlow<Boolean>` (or `Flow<Boolean>`) so common/client need not depend on auth.client.

**Signature (new boolean-keyed variant):**
```kotlin
fun StateFlow<Boolean>.subscribeOnLoggedOut(
    scope: CoroutineScope,
    action: suspend () -> Unit
)
```
Receiver is the `userAuthorised` flow where `true == logged in`, `false == logged out`.

**Semantics (must drop the initial value):**
- `distinctUntilChanged()` to collapse redundant emissions.
- `drop(1)` to discard the initial/replayed current value, so a cold start that begins `false` (anonymous) does NOT fire, and a cold start that begins `true` does NOT fire on first emission.
- `filter { it == false }` so only a genuine `true → false` (logout) transition runs `action`.
- Subscribe with the existing `subscribeLoggingDropExceptions(scope) { action() }` so the subscription dies with the ViewModel scope and exceptions are swallow-logged.

This mirrors the prior helper's logic but is keyed on the boolean auth flow rather than `Flow<T?>` nullability. (If Architecture prefers, the prior generic `Flow<T?>.subscribeOnLoggedOut` may be kept and a thin boolean overload added; but since nothing else uses the `T?` form, a single `StateFlow<Boolean>` helper is cleanest. Recommend replacing, not keeping both.)

**How each ViewModel obtains the flow:** add a constructor parameter to each of the 6 edit ViewModels:
```kotlin
authCredentialsStorage: AuthCredentialsStorage   // or: userAuthorised: StateFlow<Boolean>
```
Recommend passing the **whole `AuthCredentialsStorage`** (single, already in Koin) and calling `authCredentialsStorage.userAuthorised.subscribeOnLoggedOut(scope) { ... }` in each `init {}` block, right after the existing load subscription. Passing the storage (vs. the raw flow) is marginally cleaner for Koin (`get()` resolves the single directly).

**Per-ViewModel exit action inside the subscription:**
- `WishlistEditViewModel`: `if (isCreating) interactor.onNavigateBack(node) else interactor.onNavigateBackToParent(node)`
- `WishlistItemEditViewModel`: `interactor.onNavigateBackToParent(node)`
- `UserEditViewModel`: `interactor.onNavigateBack(node)`
- `AdminUserEditViewModel`: `interactor.onNavigateBack(node)`
- `AdminWishlistEditViewModel`: `interactor.onNavigateBack(node)`
- `AdminWishlistItemEditViewModel`: `interactor.onNavigateBack(node)`

(`subscribeOnLoggedOut`'s `action` is already `suspend`, and the interactor calls are suspend — invoke them directly; no extra `launchLoggingDropExceptions` wrapper needed since `subscribeLoggingDropExceptions` provides the scope/error handling.)

## 6. Exact files Architecture + Coding will create / modify

CREATE (1):
1. `features/common/client/src/commonMain/kotlin/utils/SubscribeOnLoggedOut.kt` — the shared `StateFlow<Boolean>.subscribeOnLoggedOut` helper.

MODIFY ViewModels (6) — add `AuthCredentialsStorage` ctor param + `init` subscription:
2. `features/ui/wishlist/src/commonMain/kotlin/ui/WishlistEditViewModel.kt`
3. `features/ui/wishlist/src/commonMain/kotlin/ui/WishlistItemEditViewModel.kt`
4. `features/ui/users/src/commonMain/kotlin/ui/UserEditViewModel.kt`
5. `features/ui/adminPanel/src/commonMain/kotlin/ui/AdminUserEditViewModel.kt`
6. `features/ui/adminPanel/src/commonMain/kotlin/ui/AdminWishlistEditViewModel.kt`
7. `features/ui/adminPanel/src/commonMain/kotlin/ui/AdminWishlistItemEditViewModel.kt`

MODIFY Plugins (3) — add the extra `get()` to the 6 affected `factory { ... }` lines:
8. `features/ui/wishlist/src/commonMain/kotlin/Plugin.kt` (lines 92, 93)
9. `features/ui/users/src/commonMain/kotlin/Plugin.kt` (line 56)
10. `features/ui/adminPanel/src/commonMain/kotlin/Plugin.kt` (lines 76, 79, 80)

DOC (4) — append a brief note to the `## Architecture Notes` (NOT Operator Notes) of each touched feature README, matching the prior PR's documentation habit:
11. `features/common/README.md` (helper note)
12. `features/ui/wishlist/README.md`
13. `features/ui/users/README.md`
14. `features/ui/adminPanel/README.md`

(README edits are optional-but-recommended for consistency with project README rule; they must NOT touch `## Operator Notes`. All Operator Notes for these features are currently empty/no-constraint — verified.)

After any source change: run `ast-index rebuild` (per AGENTS rule).

## 7. Build / verify commands

Per-module builds (fast, targeted):
```
./gradlew :wishlist.features.common.client:build
./gradlew :wishlist.features.ui.wishlist:build
./gradlew :wishlist.features.ui.users:build
./gradlew :wishlist.features.ui.adminPanel:build
```
Or a single combined invocation:
```
./gradlew :wishlist.features.common.client:build :wishlist.features.ui.wishlist:build :wishlist.features.ui.users:build :wishlist.features.ui.adminPanel:build
```
Definition of Done requires `BUILD SUCCESSFUL`. (Module path names verified via `./gradlew projects`.)

## 8. Risks / open questions

- R1 (low): the navigation interactor `replace`/`pop` semantics — if an edit node is already being torn down when logout fires, a double-navigation could occur. Mitigated because `subscribeLoggingDropExceptions` swallows exceptions and the subscription is scoped to the ViewModel (dies on node destruction). No action needed; flag for the Validation step.
- R2 (low): `userAuthorised` is a `StateFlow`, so it always has a current value; `drop(1)` is essential to avoid firing the exit immediately on ViewModel creation. The helper spec enforces this. Architecture/Coding MUST keep `drop(1)`.
- R3 (design choice, no blocker): pass `AuthCredentialsStorage` vs. raw `StateFlow<Boolean>` into the ViewModel. Recommend `AuthCredentialsStorage` (cleaner Koin `get()`); leave final call to Architecture but keep common/client free of the auth type.
- R4 (none): all four `## Operator Notes` sections are empty — no operator constraint blocks this work. No operator escalation required.

No blockers require the operator. Plan is ready for the Architecture step.
