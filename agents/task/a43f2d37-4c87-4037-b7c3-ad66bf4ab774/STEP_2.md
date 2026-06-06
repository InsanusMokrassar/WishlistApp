# STEP_2 — Coding A (T1,T2,T3)

task_id=pr31-review-fixes; agent_id=coding-A; protocol=AML-HIP; branch=issue/29-book-functionality; pr=31
memory_ref=[STEP_0.md, STEP_1.md]; scope=T1+T2+T3; constraints=[no-commit/push/stash/revert, preserve when-restructure, no Operator Notes edits]

---

## STATE ON ARRIVAL (pre-existing partial work)

entity=commonMain files; state=COMPLETE-ON-ARRIVAL; detail=[
  WishlistsModel.kt:L128 isOwner() already present (T2 interface),
  Plugin.kt:L149-152 isOwner() impl already present (T2 impl),
  UserWishlistsViewModel.kt: T1+T2 fully done (sortModeState clamped L135-138, sortSelectorVisibleState L146-148, sortedItemsState effectiveMode L189-193, _isOwnerState.value=model.isOwner L238),
  WishlistsListViewModel.kt:L96 _isOwnerState.value=model.isOwner(targetUserId) done,
  WishlistViewModel.kt: T1+T2 fully done (isOwnerState MutableRedeliverStateFlow L55, sortModeState clamped L76-79, sortSelectorVisibleState L86-88, sortedItemsState effectiveMode L126-130, loadWishlist L185),
  WishlistItemViewModel.kt: T2 fully done (isOwnerState MutableRedeliverStateFlow L48, init load block L84-85),
  jsMain/WishlistSelectorsRow.kt: showSortSelector param L38+body-if L53 done,
  jsMain/UserWishlistsView.kt: CreateWishlistButton L68 + sortSelectorVisible L62 + showSortSelector=sortSelectorVisible L90 done,
  jsMain/WishlistView.kt: sortSelectorVisible L54 + showSortSelector=sortSelectorVisible L81 done,
  jsMain/WishlistsListView.kt: CreateWishlistButton L72 done,
  jvmMain/UserWishlistsView.kt: CreateWishlistButton L90 + sortSelectorVisible L74 + if(sortSelectorVisible) L102 done
]

---

## FILES CREATED (this agent)

file=features/ui/wishlist/src/jsMain/kotlin/ui/CreateWishlistButton.kt
  action=NEW; T3; @Composable fun CreateWishlistButton(isOwner,onClick); Bootstrap btn btn-primary; renders nothing when isOwner=false; KDoc complete

file=features/ui/wishlist/src/jvmMain/kotlin/ui/CreateWishlistButton.kt
  action=NEW; T3; @Composable fun CreateWishlistButton(isOwner,onClick); material.Button; renders nothing when isOwner=false; KDoc complete

file=features/ui/wishlist/src/androidMain/kotlin/ui/CreateWishlistButton.kt
  action=NEW; T3; @Composable fun CreateWishlistButton(isOwner,onClick); material3.Button; translation(LocalResources.current); renders nothing when isOwner=false; KDoc complete

---

## FILES EDITED (this agent)

file=features/ui/wishlist/src/jvmMain/kotlin/ui/WishlistsListView.kt
  change=T3; lines 85-89 inline isOwner-gated Button replaced with CreateWishlistButton(isOwner){viewModel.onCreateWishlist()}; removed freed import androidx.compose.material.Button (line 13)

file=features/ui/wishlist/src/jvmMain/kotlin/ui/WishlistView.kt
  change=T1; ADD val sortSelectorVisible by viewModel.sortSelectorVisibleState.collectAsState() (after costSortAvailable collect); wrapped WishlistSortSelector call in if(sortSelectorVisible){...}

file=features/ui/wishlist/src/androidMain/kotlin/ui/UserWishlistsView.kt
  change=T1+T3
  T1: ADD val sortSelectorVisible by viewModel.sortSelectorVisibleState.collectAsState(); wrapped WishlistSortSelector call in if(sortSelectorVisible){...}
  T3: lines 92-96 inline isOwner-gated Button replaced with CreateWishlistButton(isOwner){viewModel.onCreateWishlist()}
  note: Button import kept (still used for profile/addItem/openWishlist buttons)

file=features/ui/wishlist/src/androidMain/kotlin/ui/WishlistView.kt
  change=T1; ADD val sortSelectorVisible by viewModel.sortSelectorVisibleState.collectAsState(); wrapped WishlistSortSelector call in if(sortSelectorVisible){...}

file=features/ui/wishlist/src/androidMain/kotlin/ui/WishlistsListView.kt
  change=T3; lines 88-92 inline isOwner-gated Button replaced with CreateWishlistButton(isOwner){viewModel.onCreateWishlist()}; removed freed import androidx.compose.material3.Button (line 13)

---

## VERIFICATION CHECKS

check=createWishlistButton.translation occurrences outside CreateWishlistButton.kt; method=grep
  jsMain: 0 outside CreateWishlistButton.kt
  jvmMain: 0 outside CreateWishlistButton.kt
  androidMain: 0 outside CreateWishlistButton.kt
  result=PASS

check=getCurrentUserId() == in ViewModels; expected=0
  UserWishlistsViewModel.kt: model.isOwner(node.config.userId) at L238
  WishlistsListViewModel.kt: model.isOwner(targetUserId) at L96
  WishlistViewModel.kt: model.isOwner(loaded.userId) at L185
  WishlistItemViewModel.kt: model.isOwner(wishlist.userId) at L85
  result=PASS; all 4 sites routed through WishlistsModel.isOwner

check=when-restructure in jsMain/UserWishlistsView.kt; expected=intact
  result=PASS; when block at lines 78-157 preserved; additive changes only (L62 sortSelectorVisible collect added; L68 CreateWishlistButton replaces old inline if-block; L90 showSortSelector arg added)

check=sortSelectorVisibleState wired to all 6 view sites; expected=true
  jsMain UserWishlistsView: WishlistSelectorsRow showSortSelector=sortSelectorVisible PASS
  jsMain WishlistView: WishlistSelectorsRow showSortSelector=sortSelectorVisible PASS
  jvmMain UserWishlistsView: if(sortSelectorVisible) wrapping WishlistSortSelector PASS
  jvmMain WishlistView: if(sortSelectorVisible) wrapping WishlistSortSelector PASS (THIS AGENT)
  androidMain UserWishlistsView: if(sortSelectorVisible) wrapping WishlistSortSelector PASS (THIS AGENT)
  androidMain WishlistView: if(sortSelectorVisible) wrapping WishlistSortSelector PASS (THIS AGENT)

---

## BUILD RESULT

command=./gradlew :wishlist.features.ui.wishlist:build
result=BUILD SUCCESSFUL in 55s; 1257 actionable tasks: 82 executed, 1175 up-to-date
errors=0; warnings=Deprecated Gradle features (pre-existing, unrelated to this change)

---

## AST-INDEX UPDATE

command=ast-index update
result=Updated 19 files (19 changed, 0 deleted); exit=0

---

## DEVIATIONS

deviation=NONE; all STEP_1 AGENT_A decisions honored exactly; no files outside AGENT_A scope touched

---

## REMAINING (AGENT_B scope, not touched)

scope=T4+T5; files=[WishlistAdditionalConfigsProvider.kt, BookingConfigsProvider.kt, WishlistItemAdditionalConfigView.kt (NEW), 3x WishlistItemView.kt]

---

## REPETITION OF RESULT

entity_id=STEP_2; stored_in=agents/task/undefined/STEP_2.md; status=available
build=SUCCESS; T1=COMPLETE; T2=COMPLETE; T3=COMPLETE; deviations=0
