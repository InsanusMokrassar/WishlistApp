# STEP_0 — Planning

task_id=pr31-review-fixes; uuid=undefined; role=planning; protocol=AML-HIP; branch=issue/29-book-functionality; pr=31

## 1. Task restatement

GOAL: plan fixes for 5 unresolved PR #31 review threads (T1..T5) in module=features/ui/wishlist; operator decisions BINDING.
CONSTRAINTS: no commit/push/stash/revert; preserve uncommitted if/else→when restructure in features/ui/wishlist/src/jsMain/kotlin/ui/UserWishlistsView.kt; no edits to `## Operator Notes`; KDoc rules (agents/CODING.md); README.md update after code changes (haiku agent per agents/ALL.md item 4); build cmd=`./gradlew :wishlist.features.ui.wishlist:build`; `ast-index update` after source changes.

## 2. Investigation findings

### 2.1 T1 surface (sort selector / empty wishlists)
- UserWishlistsViewModel.kt:166-194 sortedItemsState=combine(_sectionsState,_sortModeState,_ratesState,_currencyEnabledState); mode!=None+all-sections-empty → emptyList → views render flat empty area (no headers, no placeholder).
- UserWishlistsViewModel.kt:152-156 costSortAvailableState; isCostSortAvailable(emptyList(), enabled)=true → meaningless Cost option when zero priced items.
- WishlistSelectorsRow (jsMain-ONLY, features/ui/wishlist/src/jsMain/kotlin/ui/WishlistSelectorsRow.kt) shared by JS WishlistView (call site WishlistView.kt:76, noneLabel=sortDefault) + JS UserWishlistsView (call site UserWishlistsView.kt:92). JVM/Android UserWishlistsView (jvm:105-120, android:108-123) + JVM/Android WishlistView (jvm:92-109, android:94-111) render WishlistSortSelector/CurrencySelector/ViewModeSelector individually → hide-sort rule MUST be ViewModel-state-driven to reach JVM/Android.
- Views branch grouped-vs-flat on `sortMode == WishlistSortMode.None` (js UserWishlistsView:104, jvm:127+142, android:130+145) → forcing None only inside sortedItemsState is insufficient; public sortModeState must be clamped too, else stale mode=Cost + forced-empty sortedItems → flat empty render bug.
- WishlistViewModel (detail) has identical sortedItemsState/costSortAvailableState shape (WishlistViewModel.kt:98-130).

### 2.2 T2 surface (4 ownership expressions)
- site1: UserWishlistsViewModel.kt:220 `model.getCurrentUserId() == node.config.userId`.
- site2: WishlistsListViewModel.kt:95 `currentUserId != null && (targetUserId == null || targetUserId == currentUserId)`.
- site3: WishlistViewModel.kt:61-63 combine(_wishlistState,_currentUserIdState) → `wishlist != null && userId != null && wishlist.userId == userId`; _currentUserIdState set only at loadWishlist (line 171), used ONLY for ownership.
- site4: WishlistItemViewModel.kt:60-62 same combine shape; _wishlistState (line 53) private, used ONLY for ownership; _currentUserIdState (line 54) ditto.
- WishlistsModel interface=features/ui/wishlist/src/commonMain/kotlin/ui/WishlistsModel.kt; anonymous impl=features/ui/wishlist/src/commonMain/kotlin/Plugin.kt:87-156; getCurrentUserId=authFeature.getMe()?.id.
- AuthFeatureService.getMe (features/auth/client/src/commonMain/kotlin/AuthFeatureService.kt:38-43): HTTP call per invocation; short-circuit null only when storage.userAuthorised==false. Consequence: isOwner() internally re-fetching getMe adds 1 HTTP GET per load in WishlistsListViewModel (loadWishlists still needs getCurrentUserId for profileUserId line 92).
- RegisteredWishlist.userId type=UserId non-null → sites 3/4 pass non-null arg.

### 2.3 T3 surface (6 duplicated owner-gated New-Wishlist buttons)
- WishlistsListView: js:72-79 (`btn btn-primary`), jvm:85-89 (material.Button), android:88-92 (material3.Button).
- UserWishlistsView: js:67-74 (`btn btn-primary`), jvm:89-93, android:92-96.
- Markup identical per platform across both screens → single per-platform composable safe; strings key=WishlistStrings.createWishlistButton; android needs translation(LocalResources.current).

### 2.4 T4/T5 surface (provider chainId + inline injection)
- WishlistAdditionalConfigsProvider.kt:27 `val chainId: NavigationChainId` (non-null abstract).
- BookingConfigsProvider.kt:17 override non-null `NavigationChainId("wishlistItemAdditionalConfig_booking")`; createConfig=BookingViewConfig(item.id,item.wishlistId); BookingViewConfig=@Serializable data class (features/ui/booking/src/commonMain/kotlin/ui/BookingViewConfig.kt:20) → structural equality usable for dedupe.
- Injection sites (identical on 3 platforms): jsMain/ui/WishlistItemView.kt:117-125, jvmMain/ui/WishlistItemView.kt:118-122, androidMain/ui/WishlistItemView.kt:123-127; pattern=`InjectNavigationChain<ViewConfig>(id=provider.chainId){ InjectNavigationNode(provider.createConfig(it)) }`.
- API verified (sources /home/aleksey/projects/own/navigation):
  - InjectNavigationChain(onDismiss=null, id: NavigationChainId?=null, beforeNodes) — compose/src/commonMain/kotlin/ComposeNavigationExtensions.kt:102.
  - InjectNavigationNode(config, onDismiss=null, additionalCodeInNodeContext=null) — same file:211.
  - NavigationChain.rootChain() — core/src/commonMain/kotlin/extensions/Chain.kt:46.
  - NavigationChain.findInSubTree(id: NavigationChainId): NavigationChain? — Chain.kt:91.
  - NavigationChain.push/drop/dropItself NON-suspend — core NavigationChain.kt:65/85/173.
  - ComposeNode exposes `override val chain` public (compose/src/commonMain/kotlin/ComposeNode.kt:19); ComposeView extends ComposeNode → view==NavigationNode, `chain` accessible in onDraw.
- commonMain composable feasible: features/ui/wishlist applies compose plugin module-wide; gradle/templates/addCompose.gradle adds compose.runtime to commonMain; features/common/client commonMain `api libs.navigation.mvvm`; navigation.mvvm commonMain `api project(":navigation.compose")` → InjectNavigationChain/InjectNavigationNode/rootChain/findInSubTree available in wishlist commonMain.
- Lifecycle fact (ComposeNavigationExtensions.kt:107-126, ComposeNode.kt:30-38): pause → drawerState=null → composition disposed → InjectNavigationChain DisposableEffect removes chain only from provider.chains; created subchain (rootNode.createEmptySubChain(id)) + pushed node REMAIN in navigation tree; resume → remember re-runs → NEW chain with SAME id created. Consequence: rootChain().findInSubTree(chainId) after ≥1 pause/resume cycle finds STALE self-injected chain → naive "found→push there" renders booking view into undrawn stale chain → compact view disappears on resume. Mitigation mandatory (see R2/D6).

### 2.5 Misc
- Uncommitted diff: jsMain/ui/UserWishlistsView.kt if/else→when (lines 84-162 current) — cosmetic; all T1/T3 edits build on top.
- agents/task/undefined/ had no prior reports (directory created this run).

## 3. Problems / risks

- R1 (T5): duplicate push into found external chain on re-composition/resume. Guard: push iff `target.stackFlow.value.none { it.config == config }` (BookingViewConfig data class structural equality) + remember pushed node + drop(node) in DisposableEffect onDispose.
- R2 (T5): stale self-injected chain (same id, leftover after pause/resume, see 2.4) found from rootChain → booking view vanishes. Guard: exclude found chain when ancestor-walk (`found.parentNode→.chain.parentNode→…`) hits the current item-view node; on exclusion call `found.dropItself()` (leak cleanup) then fall back to inline injection.
- R3 (T5): found EXTERNAL chain exists but no host draws it → compact view invisible. Accepted: host-contract responsibility; operator instruction binding; documented in KDoc.
- R4 (T2): WishlistsListViewModel.loadWishlists gains +1 getMe HTTP per load/resume (profileUserId needs getCurrentUserId; isOwner re-fetches internally). Accepted: binding instruction; getMe short-circuits when unauthorized. Rejected alternative: isOwner overload taking precomputed currentUserId (defeats single-predicate goal).
- R5 (T2): isOwner(null)=true-when-authenticated fits "own-context" callers (WishlistsListViewModel targetUserId=null); unknown-wishlist callers MUST pre-check null wishlist as not-owner BEFORE calling. Enforcement: KDoc on isOwner + call shape `wishlist != null && model.isOwner(wishlist.userId)`.
- R6 (T1): WishlistSelectorsRow signature shared with JS WishlistView detail screen → new param forces explicit decision at both call sites; detail screen receives same <2 rule (D1).
- R7 (T1): stale non-None _sortModeState while item count drops <2 (deletion elsewhere + resume) → views' `sortMode == None` branching desyncs from forced-empty sortedItemsState. Guard: public sortModeState becomes clamped derived flow (D3).
- R8: preserve uncommitted when-restructure in jsMain UserWishlistsView.
- R9 (T2): WishlistItemViewModel._wishlistState/_currentUserIdState and WishlistViewModel._currentUserIdState removal — verified zero other usages before removal (item VM: _wishlistState set :92 used :60 only; _currentUserIdState set :91 used :60 only; wishlist VM: _currentUserIdState set :171 used :61 only).

## 4. Decisions (operator unavailable; recorded with rationale)

- D1: T1 `<2 items → hide sort + force None` applied to BOTH screens (all-items + wishlist detail) on ALL 3 platforms. Rationale: WishlistSelectorsRow shared by both JS screens (signature change is operator-mandated → detail call site must pass something); sorting <2 items meaningless identically on detail screen; consistency.
- D2: WishlistSelectorsRow new param `showSortSelector: Boolean` WITHOUT default value. Rationale: forces every call site to decide explicitly; prevents silent regression.
- D3: clamping implemented as derived public `sortModeState` = combine(raw _sortModeState, items-source) { mode, src -> if (totalItems(src) < 2) None else mode }.stateIn(scope, Eagerly, None); raw private flow keeps user selection. Rationale: views' existing `sortMode == None` branching + sortedItemsState stay consistent from one source (fixes R7); zero view-side logic added.
- D4: T2 WishlistViewModel/WishlistItemViewModel switch from combine-derived isOwnerState to load-time assignment `_isOwnerState.value = (loaded != null) && model.isOwner(loaded.userId)`; _currentUserIdState removed both; WishlistItemViewModel._wishlistState removed (local val). Rationale: combine existed only to join two load-time values; load-time assignment routes through isOwner per binding fix with null-pre-check; less state.
- D5: T5 logic centralized in ONE shared commonMain composable `WishlistItemAdditionalConfigView` (new file). Rationale: commonMain compose availability verified (2.4); avoids triplicating non-trivial search/push/dedupe/cleanup logic across 3 platforms; identical behavior guaranteed.
- D6: T5 own-subtree exclusion + stale-chain `dropItself()` cleanup before inline fallback (R2). Rationale: without exclusion booking view disappears after first pause/resume — direct regression of PR #31 feature; operator intent = reuse EXTERNAL host chains, not own leftovers.
- D7: T5 dedupe push via config structural equality + drop pushed node on dispose (R1).
- D8: T3 file name=CreateWishlistButton.kt per operator suggestion; per-platform markup copied verbatim from existing 6 sites (`btn btn-primary` / material.Button / material3.Button); composable gates on isOwner internally (renders nothing when false).
- D9: T1 "overall amount of wishlists item" read as TOTAL item count across all sections (all-items screen) / items.size (detail screen). Rationale: literal reading; per-section counting would re-enable sort with 2 empty wishlists + 1 item.

## 5. Ordered change list (execution stages for coding role)

### Stage A — commonMain contracts
1. features/ui/wishlist/src/commonMain/kotlin/ui/WishlistsModel.kt: ADD `suspend fun isOwner(userId: UserId?): Boolean`; KDoc: returns true iff authenticated caller owns context of [userId]; `userId == null` means caller's own context (true when authenticated); semantics `currentUserId != null && (userId == null || userId == currentUserId)`; callers holding nullable wishlist MUST treat null wishlist as not-owner BEFORE calling.
2. features/ui/wishlist/src/commonMain/kotlin/Plugin.kt: implement in anonymous WishlistsModel object: `override suspend fun isOwner(userId: UserId?): Boolean { val currentUserId = getCurrentUserId(); return currentUserId != null && (userId == null || userId == currentUserId) }`.
3. features/ui/wishlist/src/commonMain/kotlin/ui/WishlistAdditionalConfigsProvider.kt: `val chainId: NavigationChainId` → `val chainId: NavigationChainId? get() = null`; KDoc update: null (default) = inline-only injection on item screen; non-null = stable id; item screen first searches root chain subtree for an externally hosted chain with the id and pushes the compact view there, falling back to inline injection.
4. features/ui/wishlist/src/commonMain/kotlin/ui/BookingConfigsProvider.kt: override type → `NavigationChainId?`, value unchanged; KDoc update.

### Stage B — ViewModels
5. features/ui/wishlist/src/commonMain/kotlin/ui/UserWishlistsViewModel.kt:
   - line 220 → `_isOwnerState.value = model.isOwner(node.config.userId)`.
   - rename existing `val sortModeState = _sortModeState.asStateFlow()` to derived clamp: `val sortModeState: StateFlow<WishlistSortMode> = combine(_sortModeState, _sectionsState) { mode, sections -> if (sections.sumOf { it.items.size } < 2) WishlistSortMode.None else mode }.stateIn(scope, Eagerly, None)`.
   - ADD `val sortSelectorVisibleState: StateFlow<Boolean> = _sectionsState.map { it.sumOf { s -> s.items.size } >= 2 }.stateIn(scope, Eagerly, false)` (combine/map import as needed).
   - sortedItemsState: add identical <2 clamp to effectiveMode computation (totalItems < 2 → None).
   - KDocs updated (sortModeState/sortSelectorVisibleState/sortedItemsState/isOwnerState).
6. features/ui/wishlist/src/commonMain/kotlin/ui/WishlistsListViewModel.kt: line 95 → `_isOwnerState.value = model.isOwner(targetUserId)`; keep currentUserId fetch for profileUserId; KDoc note on isOwner routing.
7. features/ui/wishlist/src/commonMain/kotlin/ui/WishlistViewModel.kt:
   - REMOVE _currentUserIdState; isOwnerState → `MutableRedeliverStateFlow(false)` + asStateFlow; in loadWishlist: `_isOwnerState.value = loaded != null && model.isOwner(loaded.userId)`.
   - sortModeState → clamped derived (combine(_sortModeState, _itemsState), items.size < 2 → None).
   - ADD sortSelectorVisibleState (= _itemsState.map { it.size >= 2 }.stateIn(...)).
   - sortedItemsState: add <2 clamp to effectiveMode.
   - KDocs updated; drop unused UserId import if freed.
8. features/ui/wishlist/src/commonMain/kotlin/ui/WishlistItemViewModel.kt: REMOVE _wishlistState + _currentUserIdState; isOwnerState → MutableRedeliverStateFlow(false)+asStateFlow; in init load block: `val wishlist = model.getWishlist(node.config.wishlistId); _isOwnerState.value = wishlist != null && model.isOwner(wishlist.userId)`; KDocs updated; prune unused imports (combine/SharingStarted/stateIn/UserId/RegisteredWishlist if freed).

### Stage C — new shared composables
9. NEW features/ui/wishlist/src/commonMain/kotlin/ui/WishlistItemAdditionalConfigView.kt:
   `@Composable fun WishlistItemAdditionalConfigView(provider: WishlistAdditionalConfigsProvider, item: RegisteredWishlistItem, viewNode: NavigationNode<*, ViewConfig>)`.
   - `val config = remember(provider, item) { provider.createConfig(item) }`; `val chainId = provider.chainId`.
   - chainId == null → `InjectNavigationChain<ViewConfig> { InjectNavigationNode(config) }`.
   - chainId != null → `var searched by remember(chainId) { mutableStateOf(false) }; var found by remember(chainId) { mutableStateOf<NavigationChain<ViewConfig>?>(null) }`; `LaunchedEffect(chainId) { val candidate = viewNode.chain.rootChain().findInSubTree(chainId); found = candidate?.takeUnless { it.isInSubTreeOf(viewNode) ?.also stale-cleanup }; ... ; searched = true }` — own-subtree test = ancestor walk `generateSequence(candidate) { it.parentNode?.chain }.any { it.parentNode === viewNode }`; stale own chain → `candidate.dropItself()`, treat as not found.
   - render: `!searched → nothing (single frame)`; `found != null → DisposableEffect(found, config) { val pushed = if (found.stackFlow.value.none { it.config == config }) found.push(config) else null; onDispose { pushed?.let { found.drop(it) } } }`; `else → InjectNavigationChain<ViewConfig>(id = chainId) { InjectNavigationNode(config) }`.
   - Full KDoc incl. R1/R2/R3 behavior contract.
10. NEW features/ui/wishlist/src/jsMain/kotlin/ui/CreateWishlistButton.kt: `@Composable fun CreateWishlistButton(isOwner: Boolean, onClick: () -> Unit)`; `if (!isOwner) return`; `Button({ classes("btn","btn-primary"); onClick { onClick() } }) { Text(WishlistStrings.createWishlistButton.translation()) }`; KDoc.
11. NEW features/ui/wishlist/src/jvmMain/kotlin/ui/CreateWishlistButton.kt: material.Button equivalent; KDoc.
12. NEW features/ui/wishlist/src/androidMain/kotlin/ui/CreateWishlistButton.kt: material3.Button + translation(LocalResources.current); KDoc.

### Stage D — views
13. features/ui/wishlist/src/jsMain/kotlin/ui/WishlistSelectorsRow.kt: ADD param `showSortSelector: Boolean` (no default, placed after onSortModeSelected or before noneLabel — keep named-arg call sites); wrap WishlistSortSelector in `if (showSortSelector)`; KDoc @param added.
14. features/ui/wishlist/src/jsMain/kotlin/ui/UserWishlistsView.kt (preserve when-restructure): collect sortSelectorVisibleState; pass `showSortSelector = sortSelectorVisible` to WishlistSelectorsRow; replace lines 67-74 owner-gated Button with `CreateWishlistButton(isOwner) { viewModel.onCreateWishlist() }`.
15. features/ui/wishlist/src/jsMain/kotlin/ui/WishlistView.kt: collect sortSelectorVisibleState; pass showSortSelector to WishlistSelectorsRow.
16. features/ui/wishlist/src/jsMain/kotlin/ui/WishlistsListView.kt: replace lines 72-79 with CreateWishlistButton.
17. features/ui/wishlist/src/jvmMain/kotlin/ui/UserWishlistsView.kt: wrap WishlistSortSelector (lines 105-109) in `if (sortSelectorVisible)`; replace lines 89-93 with CreateWishlistButton.
18. features/ui/wishlist/src/jvmMain/kotlin/ui/WishlistView.kt: wrap WishlistSortSelector (lines 92-97) in `if (sortSelectorVisible)`.
19. features/ui/wishlist/src/jvmMain/kotlin/ui/WishlistsListView.kt: replace lines 85-89 with CreateWishlistButton.
20. features/ui/wishlist/src/androidMain/kotlin/ui/UserWishlistsView.kt: wrap WishlistSortSelector (lines 108-112) in `if (sortSelectorVisible)`; replace lines 92-96 with CreateWishlistButton.
21. features/ui/wishlist/src/androidMain/kotlin/ui/WishlistView.kt: wrap WishlistSortSelector (lines 94-99) in `if (sortSelectorVisible)`.
22. features/ui/wishlist/src/androidMain/kotlin/ui/WishlistsListView.kt: replace lines 88-92 with CreateWishlistButton.
23. features/ui/wishlist/src/{jsMain,jvmMain,androidMain}/kotlin/ui/WishlistItemView.kt: replace provider forEach injection block with `viewModel.additionalConfigsProviders.forEach { provider -> WishlistItemAdditionalConfigView(provider, it, this@WishlistItemView) }` (js keeps wrapper Div with flex classes); remove freed imports (InjectNavigationChain/InjectNavigationNode); update inline comment.

### Stage E — docs + verification
24. features/ui/wishlist/README.md (haiku agent; NOT `## Operator Notes`): update Screens table + Architecture Notes: (a) sort selector hidden + None forced when total items < 2 on both sorting screens; (b) WishlistsModel.isOwner + new isOwnerState derivations; (c) CreateWishlistButton shared per-platform composable; (d) chainId nullable default-null + search-root-then-push-else-inline drawing semantics + WishlistItemAdditionalConfigView; booking provider keeps non-null id. Mention removed states (_currentUserIdState combines).
25. `export PATH="/home/linuxbrew/.linuxbrew/bin:$PATH" && ast-index update`.
26. `cd /home/aleksey/projects/own/WishlistApp && ./gradlew :wishlist.features.ui.wishlist:build`; one fix cycle max per CODING.md; unresolved repeats → report.

## 6. Verification

- check=build :wishlist.features.ui.wishlist; expected=SUCCESS.
- check=jsMain UserWishlistsView when-restructure intact in diff; expected=true.
- check=zero remaining occurrences of duplicated create-button markup (`createWishlistButton.translation` outside CreateWishlistButton.kt); expected=0 per platform.
- check=zero remaining direct `getCurrentUserId() ==`-style ownership comparisons in 4 ViewModels; expected=all route via model.isOwner.
- check=InjectNavigationChain/InjectNavigationNode imports absent from 3 WishlistItemView files; expected=true.
- manual scenario (T1): user with 2 wishlists, 0 items → no sort selector, grouped headers + emptyItems placeholders visible; 1 item → same; 2 items → selector visible.
- manual scenario (T5): open item view → booking button inline; push edit → back → booking button still visible (R2 guard); no duplicate booking nodes.

## 7. Result

entity=STEP_0; state=COMPLETE; next=coding stage executes Stage A→E; stored_in=agents/task/undefined/STEP_0.md
