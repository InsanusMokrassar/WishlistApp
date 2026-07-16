Model: claude-sonnet-5
Execution time: ~900
Tokens used: not instrumented (approx. mid-to-high tens of thousands including tool outputs)
Changed files: agents/task/10.07.2026_11.49.17-f6128a2c-4a1d-4c43-984c-d5feaae2cebd/001-planning.md

# 001-planning — Issue #66 "In left panel add item for admin panel"

## 1. Task understanding

Add a reachable entry point to `AdminPanelViewConfig` (the existing, already-wired-but-unreachable
`features/ui/adminPanel` root screen) from the web client's left sidebar, visible **only** when the
signed-in caller is `root`. Web-only per the issue's wording ("left panel") and per explicit
constraints in `agents/task/.../PROMPT.md`.

## 2. Investigation (ast-index-grounded findings)

### 2.1 Root-check mechanism — confirmed, and a superior client-side one already exists

Server-side, four features independently repeat the exact same pattern (no shared constant):
`private val rootUsername = "root"` + `caller.username.string != rootUsername`/`== rootUsername`:
- `features/auth/server/src/jvmMain/kotlin/JVMPlugin.kt:20,44,49` (root bootstrap)
- `features/files/server/src/commonMain/kotlin/configurators/FilesRoutingsConfigurator.kt:50,54`
- `features/email/server/src/commonMain/kotlin/services/EmailFeatureService.kt:32,54` (the exact
  lead cited in PROMPT.md)
- `features/admin/server/src/commonMain/kotlin/configurators/AdminRoutingsConfigurator.kt:68,73`

Client-side, this exact check is **already surfaced and reused** — I found it while investigating:
`features/ui/users/src/commonMain/kotlin/ui/UsersModel.kt:58` declares
`val isCurrentUserRootFlow: StateFlow<Boolean>`, implemented in
`features/ui/users/src/commonMain/kotlin/Plugin.kt:76-78`:
```kotlin
override val isCurrentUserRootFlow: StateFlow<Boolean> =
    meState.map { it?.username?.string == "root" }
        .stateIn(scope, SharingStarted.Eagerly, meState.value?.username?.string == "root")
```
(`meState` = `meStateFlow: StateFlow<RegisteredUser?>` from `features/auth/client/.../Me.kt`, kept
live by `AuthCredentialsStorage.userAuthorised`.) This flow is already consumed by
`UserEditViewModel.isRootState`, `UserViewModel`, and all three `UserEditView` platforms
(`jsMain`/`jvmMain`/`androidMain`) to gate root-only UI (edit username/password, delete user).

**`features/ui/sidebar` already depends on `features/ui/users`**
(`features/ui/sidebar/build.gradle` commonMain → `api project(":wishlist.features.ui.users")`), so
`SidebarModel`'s existing implementation can inject `UsersModel` and delegate to
`usersModel.isCurrentUserRootFlow` — no new `"root"` string literal anywhere, no new module
dependency needed for this part of the change.

### 2.2 Target screen — confirmed unreachable, confirmed fully wired otherwise

`features/ui/adminPanel/src/commonMain/kotlin/ui/AdminPanelViewConfig.kt`: bare
`@Serializable class AdminPanelViewConfig : ViewConfig` (no constructor args).

Already registered end-to-end on **all three** client platforms:
- Polymorphic serializer + `NavigationNodeFactory` in `features/ui/adminPanel/{commonMain,jsMain,jvmMain,androidMain}`.
- Plugin registered in `client/src/jsMain/kotlin/Main.kt:33`, `client/src/jvmMain/kotlin/Main.kt:29`,
  `client/android/src/main/kotlin/MainActivity.kt:44`.
- `AdminPanelViewInteractor` (`onOpenUsers`/`onOpenWishlists`) already implemented in
  `client/src/commonMain/kotlin/ClientPlugin.kt:457-466`, pushing `AdminUsersListViewConfig()` /
  `AdminWishlistsListViewConfig()` onto `node.chain`.

**Confirmed genuinely unreachable**: `ast-index usages "AdminPanelViewConfig"` shows no call site
that constructs/pushes `AdminPanelViewConfig()` anywhere in `client/` or any feature except the
admin panel's own internal navigation (which only fires once you're already on that screen).
`ClientPlugin.startPlugin` (line ~583-605) pushes `mainScaffoldConfig` as the actual root node — a
`ScaffoldViewConfig`, not `AdminPanelViewConfig`. **`features/ui/adminPanel/README.md`'s Architecture
Notes line "`AdminPanelViewConfig` is the root screen pushed by `InjectNavigationNode` inside
`ClientPlugin.startPlugin`" is stale/incorrect** and should be corrected once this issue lands
(Architecture/Coding should fix it per the "AFTER ANY CODE CHANGE" README rule).

### 2.3 "Left panel" = web Sidebar chain — confirmed, and confirmed genuinely web-only

`features/ui/sidebar` ships **`commonMain` + `jsMain` only** (`find` on `src/` shows no
`jvmMain`/`androidMain` dirs). Its own `README.md` states explicitly: *"The Android and Desktop
(Material 3) clients do **not** use this feature — they keep their own navigation chrome."*
Confirmed in code: `client/src/commonMain/kotlin/ClientPlugin.kt:102-107`'s default
`mainScaffoldConfigProvider` (used as-is by JVM/Android) has no `leftConfig` at all; only
`client/src/jsMain/kotlin/ClientJSPlugin.kt:31-37` overrides it to add
`leftConfig = SidebarViewConfig()`. `SidebarViewConfig()` is constructed nowhere else in the
codebase. So this issue is fully satisfiable inside `features/ui/sidebar` (commonMain + jsMain) plus
the `SidebarViewInteractor` implementation in `client/src/commonMain/kotlin/ClientPlugin.kt` — no
JVM/Android/Desktop changes needed. This matches and confirms the PROMPT's lead; not treated as
settled fact until this independent verification, per PROMPT's own instruction.

`SidebarViewModel`'s navigation model (`features/ui/sidebar/src/commonMain/kotlin/ui/SidebarViewModel.kt`)
confirms every sidebar action drives the **main** chain (located via `MainNavigationChainId`) via
`resetToSingleNode` — the interactor implementation in `client/ClientPlugin.kt:183-236`
(`single<SidebarViewInteractor>`) has a private `navigateSection(target, isSection)` helper doing
exactly this for the four existing items (My Lists / Discover / Reserved / Settings).

### 2.4 Existing sidebar structure (for reference)

- `SidebarSection` enum (`MyLists`, `Discover`, `Reserved`, `Settings`, `None`) drives the active-item
  highlight; `SidebarViewModel.resolveActiveSection()` maps main-chain top-of-stack `ViewConfig`s to
  sections (only "list"-level configs are matched — e.g. `WishlistViewConfig` detail screens are not,
  so highlight-tracking is already partial/best-effort in the existing code, not exhaustive).
- `SidebarView.kt` (JS) renders primary items via a private `NavItem(icon, label, active, enabled, count, onSelect)`
  composable inside a raw `Nav(classes(CalmStudioStyleSheet.navsec))` block — this is app-shell chrome
  per `agents/CODING.md`'s Design System Rule (`.navitem` etc. "have no component and stay raw"), so a
  5th `NavItem` call is consistent with the existing pattern, not a rule violation.
- Icons come from a feature-local `features/ui/sidebar/src/jsMain/kotlin/ui/LucideIcons.kt` object
  (inline SVG path strings) — not one of the frozen `features/common/client/.../ui/components/`
  composables, so adding a new icon constant here does not trigger the component-freeze rule.
- Existing items (My Lists/Reserved/Settings) render **always visible but `enabled=false`** while
  anonymous, because they're meaningful destinations for any authenticated user, merely gated on
  login state. The Admin item is different: it is root-exclusive, and the issue's wording ("If user
  is root, left panel must include item") reads as conditional *presence*, not conditional
  *enablement*. Recommendation carried into the plan below: render it only `if (isRoot)`, i.e. fully
  absent for non-root/anonymous users, not merely disabled.

### 2.5 Test coverage baseline (relevant to Architecture's Test Planning Requirement)

`find … -name "*Test.kt" -not -path "*/build/*"` across the whole repo returns only
`features/email/server/**` and `features/users/common/**` test files. **No `features/ui/*` module has
any test source set at all** — this is a pre-existing, project-wide gap, not something introduced or
skipped by this task. Architecture should note this explicitly when producing test stubs per
`agents/ARCHITECTURE.md`: there is no existing ViewModel-unit-test convention in any UI feature to
extend, and the one genuinely untestable piece (the conditional Compose-HTML `NavItem` render itself)
falls under the "platform-specific UI rendering" carve-out — but the state/delegation logic
(`isCurrentUserRootFlow` plumbing, `onSelectAdminPanel` delegating to the interactor,
`resolveActiveSection`'s new branch if implemented) is plain Kotlin and in principle unit-testable the
same way `UserEditViewModel`/`UsersModel` would be if *they* had tests — which they also don't.
Architecture must decide (and, if ambiguous, escalate before Coding per the Test Planning
Requirement) whether this task is expected to introduce the first ViewModel-level tests in `features/ui/*`
or whether it's acceptable to follow the codebase's existing (untested) convention for this class of
change.

## 3. Open/unclear points considered, and why none rose to "ask the operator"

- **Hidden vs. disabled for non-root**: resolved by reading the issue text plainly (conditional
  presence) — not an ambiguity needing escalation, stated as a recommendation above.
- **Active-highlight for the new item** (extending `SidebarSection` + `resolveActiveSection`, which
  requires adding a new `features/ui/adminPanel` dependency to `features/ui/sidebar/build.gradle`):
  a normal implementation trade-off within ordinary engineering latitude (consistency vs. minimal
  diff), not a decision only the operator can make — recommended below, left for Architecture to
  finalize.
- **Test coverage for a previously-untested module**: flagged above for Architecture's Test Planning
  Requirement gate (per `agents/ARCHITECTURE.md`, Architecture — not Planning — must raise this to the
  operator if it decides automated coverage isn't achievable/appropriate here).
- **Icon/label choice**: cosmetic, has direct prior art (`AdminPanelStrings.title` = "Admin Panel" /
  "Панель администратора"), left to Architecture/Coding.

None of the above are "genuine open/ambiguous design questions that only a human operator can
answer" per `agents/PLAN.md` step 3/4 — they are normal implementation decisions with clear
defensible answers grounded in existing codebase conventions.

## 4. Plan handed off to Architecture

**Root-check to reuse**: `UsersModel.isCurrentUserRootFlow` (already exists, already the
`Username.string == "root"` pattern) — do not re-derive from `meStateFlow` directly in the sidebar
feature; delegate through `UsersModel` the same way `SidebarModel.getUserName` already delegates
through `WishlistsModel`.

**Target navigation**: `mainChain()?.resetToSingleNode(AdminPanelViewConfig()) { it is AdminPanelViewConfig }`
inside the existing `single<SidebarViewInteractor>` block in `client/src/commonMain/kotlin/ClientPlugin.kt`
(reuse the private `navigateSection` helper already there) — `AdminPanelViewConfig` is already
imported in that file (line 26).

**Scope**: web only (`features/ui/sidebar` commonMain + jsMain, plus
`client/src/commonMain/kotlin/ClientPlugin.kt`'s `SidebarViewInteractor` implementation). No
JVM/Android/Desktop changes.

**Concrete file-level changes for Architecture to detail:**

1. `features/ui/sidebar/src/commonMain/kotlin/ui/SidebarModel.kt` — add
   `val isCurrentUserRootFlow: StateFlow<Boolean>` to the interface (KDoc mirroring
   `UsersModel.isCurrentUserRootFlow`'s wording).
2. `features/ui/sidebar/src/commonMain/kotlin/Plugin.kt` — inject `val usersModel = get<UsersModel>()`
   in the existing `single<SidebarModel> { ... }` block; implement the new property as
   `usersModel.isCurrentUserRootFlow`. New import: `dev.inmo.wishlist.features.ui.users.ui.UsersModel`.
3. `features/ui/sidebar/src/commonMain/kotlin/ui/SidebarViewModel.kt` —
   - `val isRootState: StateFlow<Boolean> = model.isCurrentUserRootFlow` (mirrors
     `UserEditViewModel.isRootState`'s naming/shape).
   - `fun onSelectAdminPanel() { scope.launchLoggingDropExceptions { interactor.onSelectAdminPanel(node) } }`
     — no anonymous-guard needed (item won't render for non-root; server independently enforces
     `requireRoot` regardless of what the client shows/sends).
   - Recommended: extend `SidebarSection` with `Admin`, and `resolveActiveSection()`'s `when` with
     `is AdminPanelViewConfig -> SidebarSection.Admin`. This needs `AdminPanelViewConfig` imported
     here, which needs a **new Gradle dependency**:
     `features/ui/sidebar/build.gradle` commonMain → add `api project(":wishlist.features.ui.adminPanel")`.
     Flag this explicitly — it's the one non-trivial wiring change beyond the sidebar module's
     current dependency set (`common.client`, `ui.wishlist`, `ui.users`, `ui.booking`, `ui.auth`).
4. `features/ui/sidebar/src/commonMain/kotlin/ui/SidebarViewInteractor.kt` — add
   `suspend fun onSelectAdminPanel(node: NavigationNode<SidebarViewConfig, ViewConfig>)`, documented
   like the other five methods.
5. `client/src/commonMain/kotlin/ClientPlugin.kt` — in the existing `single<SidebarViewInteractor>`
   block (~line 183-236), add:
   ```kotlin
   override suspend fun onSelectAdminPanel(node: NavigationNode<SidebarViewConfig, ViewConfig>) {
       navigateSection(AdminPanelViewConfig()) { it is AdminPanelViewConfig }
   }
   ```
6. `features/ui/sidebar/src/commonMain/kotlin/SidebarStrings.kt` — add a new label string (e.g.
   `adminPanel`, EN "Admin Panel" / RU "Панель администратора" — reuse `AdminPanelStrings.title`'s
   wording for consistency), following the existing `buildStringResource(...) { IetfLang.Russian(...) }`
   pattern.
7. `features/ui/sidebar/src/jsMain/kotlin/ui/LucideIcons.kt` — add a new icon constant (e.g. `shield`)
   for the admin nav glyph. Not a frozen shared component; editing this file does not violate
   `agents/CODING.md`'s Design System Rule 5.
8. `features/ui/sidebar/src/jsMain/kotlin/ui/SidebarView.kt` — in `onDraw()`, collect
   `val isRoot by viewModel.isRootState.collectAsState()`; conditionally render a 5th `NavItem` only
   `if (isRoot)` (fully absent otherwise — see §2.4 recommendation), placed in the existing primary
   `Nav(classes(CalmStudioStyleSheet.navsec))` block after Settings:
   ```kotlin
   if (isRoot) {
       NavItem(
           icon = LucideIcons.shield,
           label = SidebarStrings.adminPanel.translation(),
           active = activeSection == SidebarSection.Admin
       ) { viewModel.onSelectAdminPanel() }
   }
   ```
9. `features/ui/sidebar/build.gradle` — add `api project(":wishlist.features.ui.adminPanel")` to
   `commonMain.dependencies` (only if step 3's active-highlight is implemented; otherwise this step
   and the `SidebarSection.Admin` piece of step 3 can be dropped for a smaller diff — Architecture's
   call).
10. README updates (owned by Architecture/Coding per the "AFTER ANY CODE CHANGE" rule, not Planning):
    - `features/ui/sidebar/README.md` Architecture Notes — document the new root-only Admin item, the
      `isCurrentUserRootFlow` delegation to `UsersModel`, the new `onSelectAdminPanel` interactor
      method, and (if implemented) the new `ui.adminPanel` dependency + `SidebarSection.Admin`.
    - `features/ui/adminPanel/README.md` — correct the stale claim (§2.2) that `AdminPanelViewConfig`
      is pushed as the navigation root in `ClientPlugin.startPlugin`; note it is now reachable via the
      sidebar's root-only Admin item.

**Security note (for Architecture/Coding to carry forward, not a gap to close)**: client-side hiding
is UX-only, matching the codebase's existing security model — e.g. `UserEditViewModel.isRootState`
already gates root-only fields client-side while `AdminRoutingsConfigurator`'s own `rootUsername`
check independently enforces authorization server-side. A non-root user forcing
`AdminPanelViewConfig` onto their chain (e.g. via devtools) gains no real capability: every
`AdminFeature`/`EmailFeature` call the admin screens make is still rejected server-side. This task
only closes the UX/discoverability gap; it introduces no new authorization surface.

## 5. Open Questions

READY, no open questions.
