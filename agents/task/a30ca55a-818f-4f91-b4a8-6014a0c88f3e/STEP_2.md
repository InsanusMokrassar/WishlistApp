# STEP_2 Coding

task_id=issue#17; role=ORCHESTRATOR->CODING; state=coding_done; input=STEP_1.md

## ENTITY
entity_id=issue#17; type=ui_enhancement; state=implemented_build_pass

## CONTEXT
- constraints_satisfied=[KDocs_all_new_kt, JS_Bootstrap_classes, JS_Stylesheet_Rule(WishlistItemCardStylesheet), MVVM_state_in_VM, no_operator_notes_edit, no_plugin/DI/settings/Main.kt_changes]
- build_task=`./gradlew :wishlist.features.ui.wishlist:build`; result=PASS (after 1 fix cycle)

## FILES CHANGED
### commonMain (4)
- ui/WishlistViewMode.kt (NEW): enum WishlistViewMode{List,Grid}; KDoc per entry.
- ui/WishlistViewModel.kt (EDIT): +viewModeState/_viewModeState(default List), +onViewModeSelected(mode), +imageUrl(id), +loadImageBytes(id); +import FileId.
- ui/UserWishlistsViewModel.kt (EDIT): +viewModeState/_viewModeState(default List), +onViewModeSelected(mode).
- WishlistStrings.kt (EDIT): +viewModeLabel/viewModeList/viewModeGrid (EN+RU); +WishlistViewMode.labelResource(); +import WishlistViewMode.

### jsMain (4)
- ui/ViewModeSelector.kt (NEW): Bootstrap btn-group selector; mirrors WishlistSortSelector.
- ui/WishlistItemCard.kt (NEW): Bootstrap card (media/title/subtitle/content/footer) + top-right PriorityBadge overlay; WishlistItemCardStylesheet:StyleSheet (clickable cursor + media height/object-fit) applied via Style(...).
- ui/WishlistView.kt (EDIT): collect wishlist+viewMode; render ViewModeSelector; Grid branch=Bootstrap row row-cols-1/sm-2/md-3 g-3 of WishlistItemCard(wishlistTitle=wishlist?.title); List branch unchanged.
- ui/UserWishlistsView.kt (EDIT): collect viewMode; render ViewModeSelector; +private ItemsGrid(entries) Bootstrap row/col; Grid branch for grouped(None)=header+ItemsGrid(section), custom-sort=ItemsGrid(sortedItems); List branch unchanged.

### jvmMain (4)
- ui/ViewModeSelector.kt (NEW): Material v2 Button/outlined selector.
- ui/WishlistItemCard.kt (NEW): Material v2 Card; RemoteImage media when image present; Box align TopEnd PriorityBadge overlay; clickable.
- ui/WishlistView.kt (EDIT): collect wishlist+viewMode; ViewModeSelector; Grid=LazyVerticalGrid(GridCells.Adaptive(180dp)) via gridItems alias (avoids items() collision); List unchanged.
- ui/UserWishlistsView.kt (EDIT): collect viewMode; ViewModeSelector; +import verticalScroll/rememberScrollState; +private ItemCardsGrid(entries) chunked 2-col Rows (last row padded weighted Box) inside verticalScroll Column; Grid branch grouped(None)=header+Divider+ItemCardsGrid, custom-sort=ItemCardsGrid; List branch unchanged (LazyColumn).

### androidMain (4)
- ui/ViewModeSelector.kt (NEW): Material3 Button/OutlinedButton selector; translation(LocalResources.current).
- ui/WishlistItemCard.kt (NEW): Material3 Card; RemoteImage media; Box TopEnd PriorityBadge; clickable.
- ui/WishlistView.kt (EDIT): mirror jvm; Grid=LazyVerticalGrid(GridCells.Adaptive(160dp)) gridItems alias.
- ui/UserWishlistsView.kt (EDIT): mirror jvm; +verticalScroll/rememberScrollState; +ItemCardsGrid(entries).

### docs (1)
- features/ui/wishlist/README.md (EDIT): added "Item cards and view mode" bullet group under Architecture Notes. Operator Notes UNCHANGED.

## DESIGN DECISIONS REALIZED
- badge_position=top-right overlay (issue OR-choice).
- default_view_mode=List (preserves prior UX).
- grid: js=Bootstrap responsive row-cols; jvm/android flat WishlistView=LazyVerticalGrid (top-level scroller, legal); jvm/android section UserWishlistsView=chunked Row grid (2 col) inside verticalScroll Column (prevents nested same-axis lazy-scroller crash).
- card media accessors split per-platform (js=imageUrl lambda, jvm/android=loadImageBytes suspend lambda) to avoid unused params.
- subtitle source: WishlistView=wishlist?.title (single wishlist); UserWishlistsView grouped=section.wishlist.title, custom-sort=sorted.wishlistTitle.

## FIX CYCLE (1, per CODING.md)
- error1=jsMain WishlistItemCard.kt: `Unresolved reference 'property'` + `Unresolved reference 'Style'`.
  - cause=wrong imports: `property` is a StyleScope member (no import needed inside style{}); `Style` composable import missing.
  - fix=removed `import ...css.property`; added `import org.jetbrains.compose.web.css.Style`.
  - rebuild=BUILD SUCCESSFUL.

## POST-BUILD
- ast-index rebuild executed (indexed 476 files, 32 modules) per ALL.md source-change rule.

## VERIFICATION RESULTS
- check=gradle_build :wishlist.features.ui.wishlist:build; expected=PASS; actual=PASS
- check=all_new_kt_have_KDoc; actual=true (8 new files, every public symbol documented)
- check=js_custom_css_in_stylesheet_object; actual=true (WishlistItemCardStylesheet)
- check=operator_notes_unmodified; actual=true
- check=no_plugin/DI/settings/Main.kt_changes; actual=true (existing VM factories already build WishlistViewModel/UserWishlistsViewModel; no new ViewConfig)
- check=no_nested_same_axis_lazy_scrollers; actual=true (chunked Row grid inside verticalScroll for sectioned grid)

## UNRESOLVED ISSUES
- none. All three platforms compile; both target screens (WishlistView, UserWishlistsView) integrate the view-mode selector + card grid.

## RESULT REPETITION
- entity_id=issue#17; new_state=implemented_build_pass; stored_in=agents/task/a30ca55a-818f-4f91-b4a8-6014a0c88f3e/STEP_2.md; status=available
- build=`./gradlew :wishlist.features.ui.wishlist:build`=PASS
