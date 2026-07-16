Model: claude-sonnet-5
Execution time: ~2100
Tokens used: not instrumented (approx. high tens of thousands including tool outputs)
Changed files: agents/task/10.07.2026_11.49.17-f6128a2c-4a1d-4c43-984c-d5feaae2cebd/002-architecturing.md

# 002-architecturing — Issue #66 "In left panel add item for admin panel"

Scope confirmed unchanged from Planning: web-only, `features/ui/sidebar` (commonMain + jsMain) +
`client/src/commonMain/kotlin/ClientPlugin.kt`. No source files were edited by this step (Architecturing
is design + test-stub authorship only, per `agents/ARCHITECTURE.md`'s file-edit restriction) — everything
below is a concrete spec for Coding to implement verbatim. Grounded via `ast-index` (search/usages/deps)
and direct reads of every touched file plus the `dev.inmo.*` library sources under
`/home/aleksey/projects/own` (per `agents/local.ALL.md`) for `ViewModel`, `NavigationNode`, and
`resetToSingleNode`.

## 1. Trade-off decisions (resolving Planning's two open items)

### 1.1 Active-highlight tracking for the new Admin item — **DECIDED: implement it**

Verified via `ast-index deps`/`grep` that `features/ui/adminPanel/build.gradle` depends on
`common.client`, `ui.topBar`, `admin.client`, `users.common`, `wishlist.common`, `auth.client`,
`email.client` — **none of which is `ui.sidebar`**, and a repo-wide `grep` for `"ui.sidebar"` across
every `build.gradle` shows only `client/build.gradle` depends on it. So `features/ui/sidebar` →
`features/ui/adminPanel` introduces **no Gradle circular dependency**. Given the cost is one
`build.gradle` line + one enum case + one `when` branch, and the alternative (an item that never gets
the `.on` highlight while every sibling item does) is a visible UI inconsistency, the diff-size argument
for skipping it is weak. **Decision: add the dependency, add `SidebarSection.Admin`, extend the
active-section resolver.** Detailed below (§2, files 3/4/9).

### 1.2 Test coverage for `features/ui/sidebar` (first `features/ui/*` tests in the repo) — **DECIDED**

Confirmed Planning's finding independently: repo-wide `find … -name "*Test.kt"` returns only
`features/email/server/**` and `features/users/common/**` — zero `features/ui/*` test source sets exist.
However, `defaultProject.gradle` (the base of every module's Gradle template chain, including
`mppJvmJsAndroidWithCompose` used by `features/ui/sidebar`) **already declares a `commonTest` source
set** (`kotlin('test-common')`, `kotlin('test-annotations-common')`, `libs.kotlin.coroutines.test`) —
this is wired at the template level for every module, `features/ui/sidebar` included. **No
`build.gradle` change is needed to add a test source set** — Coding only needs to add files under
`features/ui/sidebar/src/commonTest/kotlin/`.

Direct precedent found for the exact pattern used below: `features/email/server/src/commonMain/kotlin/Plugin.kt`
declares `internal fun emailConfigElementOrNull(...)` (a pure decision function, no Koin/DB/IO), and
`features/email/server/src/commonTest/kotlin/PluginTest.kt` unit-tests it directly by name from the
`commonTest` source set — confirming `internal` friend-path visibility from `commonTest` → `commonMain`
already works in this Gradle setup, with zero extra wiring.

**Decision, by function/change (see §3 for the Test Planning Requirement stubs):**

- `SidebarViewModel.resolveActiveSection()` is genuinely modified by this task (new `AdminPanelViewConfig`
  branch) and its logic is pure (a `List<ViewConfig> → SidebarSection` mapping) once separated from the
  `NavigationChain` lookup. **Architecture extracts this pure mapping into a new `internal fun
  resolveActiveSectionForStack(configs: List<ViewConfig>): SidebarSection`** (§2 file 4), mirroring the
  `emailConfigElementOrNull` precedent exactly. This becomes the **first unit-tested logic in any
  `features/ui/*` module** — full test stub in §3.1.
- `SidebarModel.isCurrentUserRootFlow` (Plugin.kt delegation) and `SidebarViewModel.isRootState` /
  `onSelectAdminPanel()` are **not** given dedicated unit tests. Reasoning, stated explicitly per the
  Test Planning Requirement rather than silently skipped: unlike `resolveActiveSectionForStack`, these
  are one-line delegations/guards that require constructing a real or fake `NavigationNode`/
  `NavigationChain` to instantiate `SidebarViewModel` at all (`ViewModel`'s base class binds `scope` to
  `node.onDestroyFlow`, and `SidebarViewModel` itself derives `rootChain = node.chain.rootChain()` in its
  primary constructor path) — this repo has **no existing navigation test-double infrastructure**
  anywhere, for any ViewModel, so building one from scratch is a disproportionate lift for two
  one-line, no-branching statements that are structurally identical to four already-untested sibling
  members in the same class (`onSelectDiscover`, `onSelectReserved`'s guard, etc.). This is a scoped
  engineering trade-off, not a "cannot be tested" carve-out, and I am not escalating it — the parent
  instruction for this step explicitly delegates this call to Architecture's judgment. Coding should
  follow the class's existing convention for these two members (no test).
- The new Compose-HTML `NavItem` conditional render in `SidebarView.onDraw()` (jsMain) **is** the
  `agents/ARCHITECTURE.md` "platform-specific UI rendering... no stub possible" carve-out — flagged here
  explicitly as required, matching the zero JS-view test coverage that already exists project-wide (not
  introduced or worsened by this task). Per the parent instruction's explicit delegation ("escalate only
  if genuinely blocking/ambiguous"), I judge this not blocking — it is the same, obvious, repo-wide
  convention every other Compose-HTML view already follows — and do not raise it to the operator.

## 2. Concrete file-level changes (final — supersedes Planning §4 where a decision above changed it)

### File 1 — `features/ui/sidebar/src/commonMain/kotlin/ui/SidebarModel.kt`

Add one property to the interface, after `currentUserIdFlow`:

```kotlin
    /**
     * Reactive flag: `true` while the authenticated caller is the `root` user — the only identity
     * permitted to see the sidebar's admin-panel entry point.
     *
     * Delegates to [dev.inmo.wishlist.features.ui.users.ui.UsersModel.isCurrentUserRootFlow]; `false`
     * while anonymous or not yet resolved.
     */
    val isCurrentUserRootFlow: StateFlow<Boolean>
```

Update the interface's class KDoc ("Composes the existing wishlist and booking UI models...") to also
mention the users model, e.g. append: "...and the users UI model (for the root-only admin check)."

### File 2 — `features/ui/sidebar/src/commonMain/kotlin/Plugin.kt`

New import: `dev.inmo.wishlist.features.ui.users.ui.UsersModel`.

In the existing `single<SidebarModel> { ... }` block, resolve `UsersModel` and delegate:

```kotlin
        single<SidebarModel> {
            val wishlistsModel = get<WishlistsModel>()
            val bookingModel = get<BookingModel>()
            val usersModel = get<UsersModel>()
            object : SidebarModel {
                override val currentUserIdFlow: StateFlow<UserId?> = wishlistsModel.currentUserIdFlow
                override val isCurrentUserRootFlow: StateFlow<Boolean> = usersModel.isCurrentUserRootFlow
                override suspend fun getMyWishlists(): List<RegisteredWishlist> = wishlistsModel.getMyWishlists()
                override suspend fun getReservedCount(): Int = bookingModel.myPresentsBooks().size
                override suspend fun getUserName(userId: UserId): String? = wishlistsModel.getUserName(userId)
            }
        }
```

No new Gradle dependency needed here — `UsersModel` is already reachable (`features/ui/sidebar/build.gradle`
already has `api project(":wishlist.features.ui.users")`). Update the `Plugin` object's KDoc
("...that composes the already-registered `WishlistsModel` and `BookingModel` singletons...") to also
name `UsersModel`.

### File 3 — `features/ui/sidebar/src/commonMain/kotlin/ui/SidebarSection.kt`

Add a new enum case after `Settings`, before `None`:

```kotlin
    /** Root-only admin panel dashboard. */
    Admin,

    /** No primary section owns the current main-chain screen. */
    None
```

(i.e. insert `Admin` immediately above the existing `None` entry; `None` stays the last/sentinel value.)

### File 4 — `features/ui/sidebar/src/commonMain/kotlin/ui/SidebarViewModel.kt`

New import: `dev.inmo.wishlist.features.ui.adminPanel.ui.AdminPanelViewConfig`.

Add `isRootState`, right after `currentUserIdState`:

```kotlin
    /** Reactive id of the signed-in caller, or `null` when anonymous. */
    val currentUserIdState: StateFlow<UserId?> = model.currentUserIdFlow

    /**
     * Reactive flag: `true` while the signed-in caller is `root`. Gates the sidebar's admin-panel
     * entry point, which is fully absent (not merely disabled) for non-root/anonymous callers.
     */
    val isRootState: StateFlow<Boolean> = model.isCurrentUserRootFlow
```

Replace `resolveActiveSection()`'s body with a call into the new pure function, adding the `Admin`
mapping there instead of inline:

```kotlin
    /**
     * Maps the main chain's current stack to the primary section that owns it, scanning from the
     * topmost node down so a pushed detail resolves to the section it was opened from.
     */
    private fun resolveActiveSection(): SidebarSection {
        val mainChain: NavigationChain<ViewConfig> = rootChain.findInSubTree(MainNavigationChainId)
            ?: return SidebarSection.None
        return resolveActiveSectionForStack(mainChain.stackFlow.value.map { it.config })
    }
```

Add `onSelectAdminPanel()` after `onSelectSettings()` (mirrors the existing anonymous-guard convention
used by every other conditionally-visible item in this class, e.g. `onSelectReserved`/`onSelectSettings`,
even though the View will also never render the button for a non-root caller — defense in depth,
consistent with the class's existing pattern):

```kotlin
    /** Opens the root-only admin panel dashboard; no-op when the caller is not root. */
    fun onSelectAdminPanel() {
        if (!isRootState.value) return
        scope.launchLoggingDropExceptions { interactor.onSelectAdminPanel(node) }
    }
```

Add the new pure, test-only-reachable-by-package top-level function at the bottom of the file (outside
the class), replacing the mapping logic that used to live inline in `resolveActiveSection()`:

```kotlin
/**
 * Pure mapping from a main-chain stack (as ordered by [NavigationChain.stackFlow], bottom-most first)
 * to the [SidebarSection] that owns the topmost recognized screen.
 *
 * Scans from the topmost entry down so a pushed detail (e.g. a screen reached from elsewhere) still
 * resolves to the section it was opened from. Returns [SidebarSection.None] when no entry in the stack
 * maps to a primary section. Extracted as a standalone, [NavigationNode]-free function so the mapping
 * can be unit-tested with plain [ViewConfig] fixtures instead of a live navigation chain.
 *
 * @param configs Main-chain [ViewConfig]s, bottom-most first.
 * @return The [SidebarSection] the topmost recognized entry belongs to, or [SidebarSection.None].
 */
internal fun resolveActiveSectionForStack(configs: List<ViewConfig>): SidebarSection {
    for (cfg in configs.asReversed()) {
        val section = when (cfg) {
            is WishlistsListViewConfig -> if (cfg.userId == null) SidebarSection.MyLists else null
            is UsersListViewConfig -> SidebarSection.Discover
            is MyPresentsBooksViewConfig -> SidebarSection.Reserved
            is UserEditViewConfig -> SidebarSection.Settings
            is AdminPanelViewConfig -> SidebarSection.Admin
            else -> null
        }
        if (section != null) return section
    }
    return SidebarSection.None
}
```

This is a behavior-preserving refactor: identical traversal order and matching rules, just parameterized
on `List<ViewConfig>` instead of reading `NavigationNode.config` inline.

### File 5 — `features/ui/sidebar/src/commonMain/kotlin/ui/SidebarViewInteractor.kt`

Add, after `onOpenProfile` (or anywhere in the interface — order is not semantically significant, but
placing it last keeps the four `navigateSection`-style primary items grouped before it... actually place
it after `onSelectSettings` to mirror the View's visual order of primary items):

```kotlin
    /**
     * Opens the root-only admin panel dashboard in the content area.
     *
     * @param node Navigation node hosting the sidebar.
     */
    suspend fun onSelectAdminPanel(node: NavigationNode<SidebarViewConfig, ViewConfig>)
```

### File 6 — `client/src/commonMain/kotlin/ClientPlugin.kt`

No new imports needed — `AdminPanelViewConfig` (line 26) and `SidebarViewInteractor`/`SidebarViewConfig`
(lines 48-49) are already imported. Inside the existing `single<SidebarViewInteractor> { ... }` block,
add a new override right after `onSelectSettings` (line ~217), reusing the existing private
`navigateSection` helper exactly like the other primary-item overrides:

```kotlin
                override suspend fun onSelectAdminPanel(node: NavigationNode<SidebarViewConfig, ViewConfig>) {
                    navigateSection(AdminPanelViewConfig()) { it is AdminPanelViewConfig }
                }
```

(Verified `navigateSection(target: ViewConfig, isSection: (ViewConfig) -> Boolean)` — defined at line
192 in this file — calls `mainChain.resetToSingleNode(target) { node, _ -> isSection(node.config) }`;
`resetToSingleNode` itself lives in
`features/common/client/src/commonMain/kotlin/utils/NavigationPushOrBackUpTo.kt` and collapses the main
chain to one node holding `target`, no-op if already there — exactly the same "one click, one crumb"
behavior as the other four sidebar destinations.)

### File 7 — `features/ui/sidebar/src/commonMain/kotlin/SidebarStrings.kt`

Add, after `settings` and before `yourLists` (keeps the four-then-new primary items grouped, matching
render order):

```kotlin
    /** Primary item: root-only admin panel dashboard. */
    val adminPanel = buildStringResource("Admin Panel") {
        IetfLang.Russian("Панель администратора")
    }
```

(English text intentionally mirrors `AdminPanelStrings.title` for consistency, per Planning §4 item 6.)

### File 8 — `features/ui/sidebar/src/jsMain/kotlin/ui/LucideIcons.kt`

Add, after `settings` and before `plus` (keeps the four-then-new primary-item icons grouped):

```kotlin
    /** "Admin Panel" item (root-only). */
    val shield = """<path d="M12 3 4 6v6c0 5 3.5 8.5 8 9 4.5-.5 8-4 8-9V6z"/>"""
```

A simple closed shield silhouette, consistent stroke/viewBox contract with the other `LucideIcons`
constants (rendered through the existing shared `LucideIcon(inner: String)` composable — no changes
needed there).

### File 9 — `features/ui/sidebar/src/jsMain/kotlin/ui/SidebarView.kt`

In `onDraw()`, collect the new state right after `currentUserId`:

```kotlin
        val currentUserId by viewModel.currentUserIdState.collectAsState()
        val isRoot by viewModel.isRootState.collectAsState()
        val userName by viewModel.userNameState.collectAsState()
```

In the primary `Nav(attrs = { classes(CalmStudioStyleSheet.navsec) })` block, add a 5th item right after
the Settings `NavItem` call, rendered only when root (fully absent otherwise, no `enabled` param needed
since default is `true` and the item's *presence* is the gate, per the issue's plain wording and
Planning §2.4):

```kotlin
                NavItem(
                    icon = LucideIcons.settings,
                    label = SidebarStrings.settings.translation(),
                    active = activeSection == SidebarSection.Settings,
                    enabled = signedIn
                ) { viewModel.onSelectSettings() }
                if (isRoot) {
                    NavItem(
                        icon = LucideIcons.shield,
                        label = SidebarStrings.adminPanel.translation(),
                        active = activeSection == SidebarSection.Admin
                    ) { viewModel.onSelectAdminPanel() }
                }
```

This is a single binary `if` with no `else` — compliant with `agents/CODING.md`'s Control Flow rule
("A single binary `if`/`else` (no `else if`) stays allowed"). No raw CSS, no new component, no
`CalmStudioStyleSheet` change: the row reuses the existing `NavItem` private composable and existing
`.navitem`/`.on` classes verbatim — compliant with the Design System Rule (app-shell chrome, `.navitem`
already documented as staying raw / having no dedicated shared component).

### File 10 — `features/ui/sidebar/build.gradle`

Add to the `commonMain.dependencies` block (after the existing four `api project(...)` lines):

```groovy
                api project(":wishlist.features.ui.adminPanel")
```

Verified against `settings.gradle:51` (`":features:ui:adminPanel"`) and the identical reference already
used in `client/build.gradle:34` (`api project(":wishlist.features.ui.adminPanel")`) — string is correct
and this dependency edge does not exist in reverse (checked `features/ui/adminPanel/build.gradle` and a
repo-wide `grep` for `"ui.sidebar"` in every `build.gradle`), so no cycle.

## 3. Test Planning Requirement — stubs/specs for every planned change

### 3.1 `resolveActiveSectionForStack` (new, `internal`, pure) — full test file to add

**New file:** `features/ui/sidebar/src/commonTest/kotlin/ui/SidebarViewModelTest.kt` (new `commonTest`
source set for `features/ui/sidebar` — no `build.gradle` change needed, see §1.2). This is the first
test file in any `features/ui/*` module.

```kotlin
package dev.inmo.wishlist.features.ui.sidebar.ui

import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.ui.adminPanel.ui.AdminPanelViewConfig
import dev.inmo.wishlist.features.ui.booking.ui.MyPresentsBooksViewConfig
import dev.inmo.wishlist.features.ui.users.ui.UserEditViewConfig
import dev.inmo.wishlist.features.ui.users.ui.UsersListViewConfig
import dev.inmo.wishlist.features.ui.wishlist.ui.WishlistsListViewConfig
import dev.inmo.wishlist.features.users.common.models.UserId
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Verifies [resolveActiveSectionForStack] — the pure main-chain-stack-to-[SidebarSection] mapping
 * `SidebarViewModel.resolveActiveSection()` delegates to. No [dev.inmo.navigation.core.NavigationNode]
 * or [dev.inmo.navigation.core.NavigationChain] involved: fixtures are plain [ViewConfig] instances.
 */
class SidebarViewModelTest {

    /** An empty stack (no navigation yet, or main chain not found) resolves to [SidebarSection.None]. */
    @Test
    fun emptyStackReturnsNone() {
        assertEquals(SidebarSection.None, resolveActiveSectionForStack(emptyList()))
    }

    /** [WishlistsListViewConfig] with a `null` `userId` (the caller's own lists) maps to [SidebarSection.MyLists]. */
    @Test
    fun ownWishlistsListMapsToMyLists() {
        val stack = listOf<ViewConfig>(WishlistsListViewConfig(userId = null))
        assertEquals(SidebarSection.MyLists, resolveActiveSectionForStack(stack))
    }

    /**
     * [WishlistsListViewConfig] with a non-null `userId` (someone else's lists, reached from Discover)
     * is deliberately NOT [SidebarSection.MyLists] — with nothing else on the stack it falls through to
     * [SidebarSection.None].
     */
    @Test
    fun otherUsersWishlistsListDoesNotMapToMyLists() {
        val stack = listOf<ViewConfig>(WishlistsListViewConfig(userId = UserId(1L)))
        assertEquals(SidebarSection.None, resolveActiveSectionForStack(stack))
    }

    /** [UsersListViewConfig] maps to [SidebarSection.Discover]. */
    @Test
    fun usersListMapsToDiscover() {
        val stack = listOf<ViewConfig>(UsersListViewConfig())
        assertEquals(SidebarSection.Discover, resolveActiveSectionForStack(stack))
    }

    /** [MyPresentsBooksViewConfig] maps to [SidebarSection.Reserved]. */
    @Test
    fun myPresentsBooksMapsToReserved() {
        val stack = listOf<ViewConfig>(MyPresentsBooksViewConfig())
        assertEquals(SidebarSection.Reserved, resolveActiveSectionForStack(stack))
    }

    /** [UserEditViewConfig] maps to [SidebarSection.Settings]. */
    @Test
    fun userEditMapsToSettings() {
        val stack = listOf<ViewConfig>(UserEditViewConfig(UserId(1L)))
        assertEquals(SidebarSection.Settings, resolveActiveSectionForStack(stack))
    }

    /**
     * [AdminPanelViewConfig] maps to [SidebarSection.Admin] — the mapping added by issue #66. Covers the
     * one genuinely new branch this task adds to the resolver.
     */
    @Test
    fun adminPanelMapsToAdmin() {
        val stack = listOf<ViewConfig>(AdminPanelViewConfig())
        assertEquals(SidebarSection.Admin, resolveActiveSectionForStack(stack))
    }

    /**
     * Scans from the topmost entry down: a [UserEditViewConfig] pushed on top of [UsersListViewConfig]
     * resolves to [SidebarSection.Settings] (the screen actually showing), not [SidebarSection.Discover]
     * (the screen underneath). Regression guard for the refactor that extracted this function out of
     * `resolveActiveSection()` — the reversed-scan order must be preserved exactly.
     */
    @Test
    fun topmostEntryWinsOverEntriesBelowIt() {
        val stack = listOf<ViewConfig>(UsersListViewConfig(), UserEditViewConfig(UserId(1L)))
        assertEquals(SidebarSection.Settings, resolveActiveSectionForStack(stack))
    }

    /**
     * Same top-wins semantics for the new [AdminPanelViewConfig] branch: pushed above
     * [MyPresentsBooksViewConfig], it resolves to [SidebarSection.Admin], not [SidebarSection.Reserved].
     */
    @Test
    fun adminPanelOnTopWinsOverReservedBelowIt() {
        val stack = listOf<ViewConfig>(MyPresentsBooksViewConfig(), AdminPanelViewConfig())
        assertEquals(SidebarSection.Admin, resolveActiveSectionForStack(stack))
    }
}
```

Edge cases covered: empty stack, each of the five recognized configs in isolation (including the new
Admin one), the "recognized type but excluded by a field guard" case (`WishlistsListViewConfig` with a
non-null `userId`), and topmost-wins ordering for both a pre-existing branch and the new Admin branch
(regression coverage for the extraction refactor itself).

### 3.2 `SidebarModel.isCurrentUserRootFlow` (interface addition + `Plugin.kt` delegation)

Not unit tested — see §1.2 rationale. Spec for what Coding must preserve if this changes later: the
property must be a pure `= usersModel.isCurrentUserRootFlow` delegation with no additional logic (no
new `"root"` string literal, no re-derivation from `meStateFlow`), so it stays provably equivalent to
`UsersModel.isCurrentUserRootFlow`'s own (separately maintained) correctness.

### 3.3 `SidebarViewModel.isRootState` / `onSelectAdminPanel()`

Not unit tested — see §1.2 rationale (requires `NavigationNode`/`NavigationChain` test doubles that do
not exist anywhere in this codebase yet; disproportionate for two one-line members structurally
identical to four already-untested siblings). Behavioral contract Coding must satisfy (verifiable by
manual/`/verify`-skill smoke test since there is no automated harness for it): `isRootState` mirrors
`model.isCurrentUserRootFlow` exactly (no transformation); `onSelectAdminPanel()` is a no-op (does not
call the interactor) when `isRootState.value` is `false`, and calls
`interactor.onSelectAdminPanel(node)` on `scope` otherwise.

### 3.4 `SidebarViewInteractor.onSelectAdminPanel` / `ClientPlugin`'s implementation

Not unit tested — same rationale as 3.3 (this is the interactor implementation itself, one line calling
the already-shared `navigateSection` helper). Behavioral contract: navigating away and back to the admin
item is idempotent (no-op) once already on `AdminPanelViewConfig`, matching `resetToSingleNode`'s
documented no-op-if-already-there semantics — identical guarantee already relied on by the four existing
`navigateSection` call sites, so no new risk surface is introduced.

### 3.5 View-layer change (`SidebarView.kt` new `NavItem` conditional + `LucideIcons.shield` +
`SidebarStrings.adminPanel`)

**Explicitly flagged per the Test Planning Requirement's carve-out** ("platform-specific UI rendering...
no stub possible"): Compose-HTML rendering in this codebase has zero automated test coverage anywhere
(no snapshot/DOM test harness exists for any `jsMain` view, in any feature, today), so an automated
assertion that the 5th `NavItem` renders/hides correctly is not achievable without introducing new,
unrelated test infrastructure. Per the parent instruction's explicit delegation of this judgment call to
Architecture, this is **not** escalated to the operator — it is the same, obvious, project-wide
convention every other conditional Compose-HTML render already follows (e.g. the four existing
`enabled = signedIn` guards on the other `NavItem` calls are equally untested today). Manual verification
path for Coding/Verification: sign in as `root`, confirm the 5th item appears and navigates to
`AdminPanelViewConfig`; sign in as a non-root user (or stay anonymous), confirm the item is fully absent
from the DOM (not merely disabled).

## 4. README updates (drafted here for Coding to apply — Architecture may not edit files other than
this report)

### `features/ui/sidebar/README.md` — Architecture Notes additions

Append these bullets to the existing "Architecture Notes" section:

- Added a 5th primary item, **Admin** (root-only), gated by `SidebarModel.isCurrentUserRootFlow` →
  `SidebarViewModel.isRootState`, which delegates to the already-existing
  `UsersModel.isCurrentUserRootFlow` (`Username.string == "root"` check, same mechanism
  `UserEditViewModel.isRootState` already uses) — no new `"root"` string literal, no new module
  dependency needed for the root check itself (the feature already depended on `ui.users`).
- Unlike the other primary items (which stay visible-but-disabled while signed out), the Admin item is
  fully **absent** — not disabled — for non-root/anonymous callers, per the issue's wording ("If user is
  root, left panel must include item").
- `onSelectAdminPanel()` navigates to `AdminPanelViewConfig` via the existing `navigateSection` /
  `resetToSingleNode` mechanism, same as the other four primary items — one click always leaves the main
  chain with exactly one node.
- `SidebarSection` gained an `Admin` case; `SidebarViewModel`'s active-section resolver logic was
  extracted into a pure, package-internal `resolveActiveSectionForStack(configs: List<ViewConfig>):
  SidebarSection` function (in `SidebarViewModel.kt`) specifically so it could be unit-tested without a
  live `NavigationNode`/`NavigationChain` — the first test coverage in this module (and in any
  `features/ui/*` module). See `features/ui/sidebar/src/commonTest/kotlin/ui/SidebarViewModelTest.kt`.
- New dependency: `features/ui/sidebar/build.gradle` now also depends on `:wishlist.features.ui.adminPanel`
  (needed for `AdminPanelViewConfig`, used both by the interactor call site in `ClientPlugin.kt` and by
  the new `SidebarSection.Admin` mapping).
- Security note: this only closes a UX/discoverability gap. Client-side hiding is not a new
  authorization boundary — every admin-panel server call is still independently gated by
  `AdminRoutingsConfigurator`'s server-side `rootUsername` check, matching the pattern already relied on
  by `UserEditViewModel.isRootState`.

Also update the Overview bullet list of primary items ("My Lists, Discover, Reserved, Settings") to add
"and (root-only) Admin Panel", and the Models table's `SidebarViewInteractor` row ("My Lists / Discover /
Reserved / Settings / wishlist / new list / profile") to append "/ admin panel (root-only)".

### `features/ui/adminPanel/README.md` — correct the stale claim

Planning confirmed (§2.2 of `001-planning.md`) this line is stale: *"`AdminPanelViewConfig` is the root
screen pushed by `InjectNavigationNode` inside `ClientPlugin.startPlugin`."* Replace it with:

> `AdminPanelViewConfig` is reachable from the web client via the sidebar's root-only Admin item
> (`features/ui/sidebar`, issue #66) — it is not the navigation root; the actual root pushed by
> `InjectNavigationNode` in `ClientPlugin.startPlugin` is `mainScaffoldConfig`
> (`ScaffoldViewConfig`).

## 5. Verification note for the next step

No `.kt` files were changed in this step, so per `agents/ALL.md` no `ast-index rebuild` was run (rule
only applies after source-code changes). Coding must run `ast-index rebuild` after applying §2/§3.

## 6. Open Questions

READY, no open questions.
