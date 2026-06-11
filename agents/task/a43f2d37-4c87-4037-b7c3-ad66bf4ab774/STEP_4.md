# STEP_4 — Documentation

task_id=pr31-review-fixes; agent_id=documentation; protocol=AML-HIP; branch=issue/29-book-functionality; pr=31
memory_ref=[STEP_0.md, STEP_1.md, STEP_2.md, STEP_3.md]; scope=README.md update; constraints=[no-Operator-Notes-edits, preserve existing style/density, AML-HIP report output]

---

## STATE ON ARRIVAL

entity=features/ui/wishlist/README.md; state=PRE-STEP_4-CONTENT
STEP_2 + STEP_3 (coding agents A+B) COMPLETE; all code changes implemented + build SUCCESS; 21 files modified per git stat
README: stale — references old `isOwnerState = wishlist.userId == currentUserId` pattern, no sort clamping docs, no CreateWishlistButton docs, no T4/T5 chainId/WishlistItemAdditionalConfigView docs

---

## EDITS APPLIED (5 sections)

### EDIT 1: Screens table — All Items + Wishlist Detail rows
location=lines 18-19 (table)
changes=[
  "All Items" row: APPEND bold sentence `**Sort selector is hidden when total items across all sections < 2**, and sort mode is forced to \`None\` in that case (sorting fewer than two items is meaningless).` (end of Description),
  "Wishlist Detail" row: APPEND bold sentence `**sort selector is hidden when the wishlist holds < 2 items**, and sort mode is forced to \`None\` in that case.` (mid-Description, before "Edit controls")
]
rationale=T1 feature (sort visibility + clamping) MUST be visible in screens table per STEP_1

### EDIT 2: Models table — WishlistsModel row
location=line 28 (Models section)
changes=REPLACE sparse `WishlistsModel` row description with comprehensive method list: PREPEND `suspend fun getCurrentUserId(): UserId?` (resolves caller's user id), `suspend fun isOwner(userId: UserId?): Boolean` (single ownership predicate; routes through all 4 VM sites; null userId = caller's own context)
density=explicit entity_id (WishlistsModel.isOwner), route-through list (UserWishlistsViewModel.isOwnerState, WishlistsListViewModel.isOwnerState, WishlistViewModel.isOwnerState, WishlistItemViewModel.isOwnerState)
rationale=T2 feature (isOwner centralization) MUST document interface + routing behavior per STEP_1

### EDIT 3: Architecture Notes — ownership predicate paragraph
location=line 39 (after "All ViewModels push/pop..." bullet)
changes=REPLACE `WishlistViewModel.isOwnerState = ...` 1-liner with NEW 4-line **Ownership predicate** block
content=[
  single `suspend fun isOwner(userId: UserId?): Boolean` replaces scattered `currentUserId == ...`,
  returns `true` iff authenticated AND (userId null OR userId == caller's id),
  all 4 ownership sites route through it (list VM, list model, detail VM, detail item VM),
  removed: _currentUserIdState flows from WishlistViewModel + WishlistItemViewModel
]
rationale=T2 refactor consolidates ownership logic; reader MUST know all 4 sites + removed flows per STEP_1

### EDIT 4: Architecture Notes — Wishlist detail sorting paragraph
location=lines 66-70 (existing "Wishlist detail item sorting" subsection)
changes=[
  sortModeState KDoc: APPEND `(clamped to \`None\` when fewer than 2 items)`,
  sortedItemsState: APPEND `(also clamped)`,
  ADD new sortSelectorVisibleState line: `sortSelectorVisibleState: StateFlow<Boolean>` (true when 2+ items),
  ADD new **Sort clamping** block: explains auto-clamp when <2 items, visibility hidden, mode preserves user selection privately,
  WishlistSortSelector signature: EXPAND with full 11-parameter form; highlight NEW `showSortSelector: Boolean` (no default, operator-mandated); change JS/JVM/Android behavior per showSortSelector,
  sortedItemsState derive: REPLACE formula to show clamped `combine(_sortModeState, _itemsState)` pattern
]
density=explicit state flow transitions, visibility-hiding semantics, parameter changes
rationale=T1 sort-visibility feature MUST document VM flows + selector behavior + wiring per STEP_1 §T1

### EDIT 5: Architecture Notes — All-items sorting paragraph
location=lines 74-80 (existing "Item sorting:" sublist inside "All-items view")
changes=[
  ADD NEW **Sort clamping** bullet: explains <2 item threshold, visibility hidden, selection preserved,
  Sort selector line: APPEND `**Selector hidden when total items < 2.**`,
  sortModeState derive line: REPLACE single-line description with explicit formula showing `combine(_sortModeState, _sectionsState)` clamp pattern
]
density=explicit count semantics (sections.sumOf { it.items.size }), visibility gate, clamping formula
rationale=T1 sort-visibility feature MUST document all-items screen clamp behavior per STEP_1 §T1

### EDIT 6: Architecture Notes — NEW CreateWishlistButton bullet
location=before "**All-items view:**" (new bullet)
content=**Create Wishlist button (PR #31 T3):** line describing [
  shared per-platform CreateWishlistButton(isOwner, onClick) composable,
  3 platform files (jsMain/jvmMain/androidMain),
  renders nothing when isOwner=false (non-owners never see it),
  platform markup: JS=Bootstrap btn btn-primary, JVM=Material Button, Android=Material3 Button,
  call sites: WishlistsListView header + UserWishlistsView header
]
rationale=T3 shared button consolidation (replaces 6 duplicates) MUST be documented per STEP_1 §T3

### EDIT 7: Architecture Notes — Additional configs provider section (T4+T5)
location=lines 120-125 (existing "Additional configs provider..." subsection)
changes=[
  WishlistAdditionalConfigsProvider interface: chainId type `NavigationChainId` → `NavigationChainId?` (nullable, default null); NEW bold sentence explaining null=inline, non-null=search-push-fallback,
  BookingConfigsProvider: declared type widened to nullable, value unchanged; short property KDoc explaining non-null stable id (host pre-creates chain or inline default),
  ADD NEW **Shared WishlistItemAdditionalConfigView composable** block documenting [
    commonMain composable new file location,
    2-branch behavior: chainId==null → inline; chainId!=null → LaunchedEffect search-root-then-push-else-inline,
    safeguards: stale-self exclusion (retry loop), duplicate-push (structural equality check), cleanup-on-dispose (drop on unmount),
    3 WishlistItemView call sites (jsMain/jvmMain/androidMain): WishlistItemAdditionalConfigView(provider, item, this@WishlistItemView),
    removed: direct InjectNavigationChain/InjectNavigationNode imports from view files
  ],
  WishlistItemViewModel: APPEND "Views no longer directly import InjectNavigationChain/InjectNavigationNode (now internal to WishlistItemAdditionalConfigView).",
  booking note: booking view renders inline UNLESS host pre-creates chain with id "wishlistItemAdditionalConfig_booking"
]
density=explicit composable behavior states, safeguard names, file locations, removed imports
rationale=T4+T5 features (nullable chainId, shared view router) MUST document new composable + call semantics per STEP_1 §T4+T5

---

## CHANGES SUMMARY

| section | change_type | T | detail |
|---------|-------------|---|--------|
| Screens table (All Items row) | APPEND bold sentence | T1 | sort selector hidden <2 items |
| Screens table (Wishlist Detail row) | APPEND bold sentence | T1 | sort selector hidden <2 items |
| Models (WishlistsModel row) | REPLACE description | T2 | document isOwner() method + routing |
| Architecture (ownership predicate) | NEW block | T2 | single predicate replaces scattered comparisons + 4 sites |
| Architecture (sort detail) | EXPAND + NEW blocks | T1 | sortSelectorVisibleState, clamping logic, WishlistSelectorsRow signature |
| Architecture (sort all-items) | EXPAND + NEW bullets | T1 | clamping threshold <2, visibility flag, formula |
| Architecture (CreateWishlistButton) | NEW bullet | T3 | shared composable across platforms, replaces 6 duplicates |
| Architecture (additional configs) | EXPAND + NEW blocks | T4+T5 | nullable chainId, WishlistItemAdditionalConfigView search-push-fallback, safeguards, view call sites |

---

## VERIFICATION CHECKS

check=Operator Notes untouched; method=inspect file line 3-5
  result=PASS; "## Operator Notes" section lines 3-5 UNCHANGED (empty placeholder)

check=Screens table rows updated; method=grep "Sort selector is hidden"
  All Items row: PRESENT (bold sentence)
  Wishlist Detail row: PRESENT (bold sentence)
  result=PASS; both table rows document sort visibility behavior

check=Models.WishlistsModel description updated; method=grep "isOwner"
  result=PASS; row contains `suspend fun isOwner(userId: UserId?): Boolean` with routing documentation

check=Architecture isOwner block present; method=grep "Ownership predicate"
  result=PASS; NEW **Ownership predicate** block lines 39-42 document single predicate + 4 sites + removed flows

check=T1 sort clamping documented 2x (detail + all-items); method=grep "Sort clamping (PR #31 T1)"
  Wishlist detail section: PRESENT (NEW block)
  All-items section: PRESENT (NEW bullet)
  result=PASS; both screens document <2 item threshold + visibility behavior

check=WishlistSelectorsRow signature updated; method=grep showSortSelector
  result=PASS; signature expanded with 11 parameters including NEW showSortSelector: Boolean

check=CreateWishlistButton documented; method=grep "Create Wishlist button (PR #31 T3)"
  result=PASS; NEW bullet documents shared composable + 3 platforms + call sites

check=T4 chainId nullable documented; method=grep "chainId: NavigationChainId?"
  result=PASS; interface description + BookingConfigsProvider property KDoc updated

check=T5 WishlistItemAdditionalConfigView documented; method=grep "WishlistItemAdditionalConfigView composable"
  result=PASS; NEW block documents commonMain composable, 2-branch behavior, safeguards, 3 view call sites

check=View import cleanup documented; method=grep "Views no longer directly import"
  result=PASS; line appended to WishlistItemViewModel paragraph

check=File structure: density + style preservation; method=manual review
  result=PASS; edits preserve bullet/sub-bullet hierarchy, AML-HIP explicit entity/method names, existing density (1-2 sentences per bullet), all cross-references explicit

---

## STYLE CONFORMANCE

style_base=existing README.md patterns: bold **Feature Name (PR #issue T#)**, explicit method signatures, dense 1-2 sentence descriptions, sub-bullets for detail
edited_sections_comply=true
density_check=each new block maintains 1-3 fact lines per bullet (method names, platform differences, removed flows, etc.)
aml_hip_check=[
  entities_explicit=true (isOwner method, chainId field, WishlistItemAdditionalConfigView composable, 3 call sites),
  key_value_pairs=true (chainId: NavigationChainId?; default null; null=inline, non-null=search),
  causality=true (when <2 items → mode clamped to None; when found external chain → push into it),
  no_pronouns=true (verified; all references explicit — "isOwner predicate", "sort mode", "view", not "it", "they", "its")
]

---

## REPETITION OF RESULT

entity_id=STEP_4; stored_in=agents/task/undefined/STEP_4.md; status=available
action=README.md update; target=features/ui/wishlist/README.md
scope=[
  (a) T2 WishlistsModel.isOwner() documented + 4 routing sites,
  (b) T1 sort clamping + sortSelectorVisibleState both screens + WishlistSelectorsRow signature,
  (c) T3 CreateWishlistButton shared composable replacement,
  (d) T4 chainId: NavigationChainId? default null,
  (e) T5 WishlistItemAdditionalConfigView search-root-then-push-else-inline + safeguards
]
changes_total=7 edits (Screens table ×2, Models ×1, Architecture ×5); lines_affected=~40 insertions/replacements
operator_notes=UNTOUCHED; style=PRESERVED; aml_hip=COMPLIANT
remaining=NONE (task complete; build, index update, PR staging deferred to orchestrator)
