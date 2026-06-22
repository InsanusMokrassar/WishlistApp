Model: claude-sonnet-4-6
Execution time: 630
Tokens used: ~85000
Changed files:
  features/common/client/src/commonMain/kotlin/utils/SubscribeOnLoggedOut.kt (CREATED)
  features/ui/wishlist/src/commonMain/kotlin/ui/WishlistEditViewModel.kt
  features/ui/wishlist/src/commonMain/kotlin/ui/WishlistItemEditViewModel.kt
  features/ui/users/src/commonMain/kotlin/ui/UserEditViewModel.kt
  features/ui/adminPanel/src/commonMain/kotlin/ui/AdminUserEditViewModel.kt
  features/ui/adminPanel/src/commonMain/kotlin/ui/AdminWishlistEditViewModel.kt
  features/ui/adminPanel/src/commonMain/kotlin/ui/AdminWishlistItemEditViewModel.kt
  features/ui/wishlist/src/commonMain/kotlin/Plugin.kt
  features/ui/users/src/commonMain/kotlin/Plugin.kt
  features/ui/adminPanel/src/commonMain/kotlin/Plugin.kt
  features/common/README.md
  features/ui/wishlist/README.md
  features/ui/users/README.md
  features/ui/adminPanel/README.md

---

## Model choice

Coding priority: sonnet / opus / fable. Used **sonnet** (claude-sonnet-4-6) — the highest-priority coding model. This task is straightforward mechanical implementation (one new utility file + 6 ViewModel edits + 3 Plugin edits + 4 README updates) with a fully-specified architecture doc. No ambiguity requiring opus-level reasoning.

---

## Summary of changes per file

### A. Created `features/common/client/src/commonMain/kotlin/utils/SubscribeOnLoggedOut.kt`

- Package `dev.inmo.wishlist.features.common.client.utils`
- `fun StateFlow<Boolean>.subscribeOnLoggedOut(scope, action)` with full KDoc
- Operator chain: `.drop(1).filter { !it }.subscribeLoggingDropExceptions(scope) { action() }`
- **Deviation from architecture spec**: `distinctUntilChanged()` was specified in 1-architecture.md but REMOVED in the fix cycle (see BUILD section below). `StateFlow` already deduplicates by contract; calling `distinctUntilChanged()` on a `StateFlow` is deprecated in Kotlin and the compiler treats it as an error (`'fun <T> StateFlow<T>.distinctUntilChanged(): Flow<T>' is deprecated`). Removing it is correct and semantically identical. KDoc updated to document this.

### B. ViewModels (6 files) — each received:
- Two new imports: `AuthCredentialsStorage` (from `auth.client`) + `subscribeOnLoggedOut` (from `common.client.utils`)
- New 4th constructor parameter `private val authCredentialsStorage: AuthCredentialsStorage`
- Updated class KDoc with `@param authCredentialsStorage Login-state source; on logout this screen exits to its non-edit view.` and a description of the logout behavior
- In `init { }` block: `authCredentialsStorage.userAuthorised.subscribeOnLoggedOut(scope) { <exit call> }` added after the existing load subscription

Logout exit calls per file:
- `WishlistEditViewModel`: `when { isCreating -> interactor.onNavigateBack(node); else -> interactor.onNavigateBackToParent(node) }`
- `WishlistItemEditViewModel`: `interactor.onNavigateBackToParent(node)` (both modes)
- `UserEditViewModel`: `interactor.onNavigateBack(node)`
- `AdminUserEditViewModel`: `interactor.onNavigateBack(node)`
- `AdminWishlistEditViewModel`: `interactor.onNavigateBack(node)`
- `AdminWishlistItemEditViewModel`: `interactor.onNavigateBack(node)`

All interactor method names verified against the actual source files before use. No new interactor methods required.

### C. Plugin.kt files (3 files)

- `features/ui/wishlist/src/commonMain/kotlin/Plugin.kt`: Added trailing `get()` to `WishlistEditViewModel` and `WishlistItemEditViewModel` factory lines (3 args → 4 args).
- `features/ui/users/src/commonMain/kotlin/Plugin.kt`: Added `authCredentialsStorage = get()` (named-arg style preserved) to `UserEditViewModel` factory line.
- `features/ui/adminPanel/src/commonMain/kotlin/Plugin.kt`: Added trailing `get()` to `AdminUserEditViewModel`, `AdminWishlistEditViewModel`, `AdminWishlistItemEditViewModel` factory lines (3 args → 4 args each).

### D. READMEs (4 files) — `## Architecture Notes` section only, `## Operator Notes` NOT touched

- `features/common/README.md`: Appended logout-exit helper note (before the Calm Studio entry)
- `features/ui/wishlist/README.md`: Appended logout-exits-editors note (at end of Architecture Notes)
- `features/ui/users/README.md`: Appended logout-exits-editor note (at end of Architecture Notes)
- `features/ui/adminPanel/README.md`: Appended logout-exits-admin-editors note (at end of Architecture Notes)

---

## Build

Command run:
```
./gradlew :wishlist.features.common.client:build :wishlist.features.ui.wishlist:build :wishlist.features.ui.users:build :wishlist.features.ui.adminPanel:build
```

**First run: BUILD FAILED** — one deprecation-as-error:
```
e: file:///...SubscribeOnLoggedOut.kt:38:5 'fun <T> StateFlow<T>.distinctUntilChanged(): Flow<T>'
   is deprecated. Applying 'distinctUntilChanged' to StateFlow has no effect.
```
Fix applied (one cycle): removed `distinctUntilChanged()` call and its import; updated KDoc.

**Second run: BUILD SUCCESSFUL in 1m 28s**
1611 actionable tasks: 321 executed, 1290 up-to-date

---

## ast-index rebuild

Run after all source changes: `ast-index rebuild` — completed successfully.
Output: `Indexed 619 files, 40 modules, 0 deps, 0 transitive, 1 XML usages, 4 resources`

---

## Deviations from 1-architecture.md

1. **`distinctUntilChanged()` removed** from the operator chain in `SubscribeOnLoggedOut.kt`. Architecture §1 specified it; Kotlin's compiler treats calling `distinctUntilChanged()` on a `StateFlow` as a deprecated error (`operator fusion` — StateFlow already guarantees value-change deduplication by contract). Removed in the fix cycle. Semantically identical behavior. The architecture's intent (no duplicate fires) is fully preserved by StateFlow's own guarantee.

No other deviations. All other design decisions in 1-architecture.md were implemented exactly as specified.
