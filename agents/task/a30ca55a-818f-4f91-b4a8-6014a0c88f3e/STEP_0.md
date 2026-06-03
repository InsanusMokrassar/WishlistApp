# STEP_0 Planning

task_id=issue#17; entity=feature:features/ui/wishlist; role=ORCHESTRATOR->PLAN; state=planning_done

## ENTITY
entity_id=issue#17; type=ui_enhancement; title="Add opportunity to see items of wishlists as cards"; state=planned

## CONTEXT
- constraints=[no_commit, no_push, no_pr, no_modify_operator_notes, KDocs_on_all_new_kt, JS_uses_Bootstrap, JS_view_stylesheet_rule, MVVM_rules]
- target_views=[WishlistView, UserWishlistsView]; platforms=[jsMain, jvmMain, androidMain]
- operator_notes=EMPTY (features/ui/wishlist/README.md `## Operator Notes` empty) -> no operator constraints to respect/break

## TASK DECOMPOSITION (from issue#17)
1. component=WishlistItemCard; per_platform=[js,jvm,android]; presents single RegisteredWishlistItem as Bootstrap-style card.
   - media=item.imageIds.firstOrNull() picture (if present)
   - title=item.title
   - subtitle=wishlist title the item belongs to (passed in by caller)
   - content=item.description
   - footer=item.approximatePrice + item.priceUnits (only when approximatePrice != null)
   - priority_badge=top-right corner of card (decision: top-right overlay chosen; see STEP_1 rationale). PriorityBadge composable already exists per-platform.
2. component=ViewModeSelector (list|grid); per_platform=[js,jvm,android]; mirrors existing WishlistSortSelector pattern (button group, selected highlighted).
3. integrate ViewModeSelector into WishlistView + UserWishlistsView (both screens, all platforms).
4. grid mode -> grid layout of WishlistItemCard. list mode -> existing ListRow rendering (unchanged).

## EXISTING-CODE INVESTIGATION (facts)
- item-row rendering: WishlistView (js/jvm/android) renders sortedItems via ListRow (title + PriorityBadge inline + price + description). No leading image in WishlistView currently.
- UserWishlistsView (js/jvm/android) renders private ItemRow(item, wishlistTitle?) via ListRow WITH leading thumbnail (image or placeholder box). Custom-sort appends "(wishlistTitle)" to title.
- shared ListRow: features/common/client `ui.components.ListComponents.kt` (js=Bootstrap list-group-item; jvm/android=Material Card). Has overload with leading/trailing/content slots.
- PriorityBadge: per-platform exists (js=Bootstrap badge rounded-pill; jvm/android=Material3 Badge). Reuse as-is.
- RemoteImage: jvm (Skia) + android (BitmapFactory) exist; js uses <Img src=imageUrl>. Reuse for card media on jvm/android; js uses Img.
- WishlistSortSelector: per-platform exists; canonical pattern for new ViewModeSelector (js Bootstrap btn-group; jvm Material v2 Button/outlined; android Material3 Button/OutlinedButton).
- ViewModels (WishlistViewModel, UserWishlistsViewModel) expose: imageUrl(id) (js), loadImageBytes(id) (jvm/android). WishlistViewModel currently MISSING imageUrl/loadImageBytes -> MUST ADD (card media needs them in WishlistView).
  - VERIFIED: WishlistViewModel has NO imageUrl/loadImageBytes. UserWishlistsViewModel HAS both.
- WishlistsModel exposes imageUrl(FileId):String and loadImageBytes(FileId):ByteArray?.
- RegisteredWishlistItem fields: id, wishlistId, title, approximatePrice(Amount?), priceUnits(String), links, description, priority(Priority), imageIds(List<FileId>).
- Subtitle source:
  - WishlistView: all items share the screen wishlist -> subtitle = wishlistState.title (single value). 
  - UserWishlistsView grouped(None) mode: subtitle = section.wishlist.title. custom-sort mode: subtitle = sorted.wishlistTitle.

## VIEW STATE NEEDED FOR VIEW-MODE
- new enum WishlistViewMode { List, Grid } in commonMain (next to WishlistSortMode).
- WishlistViewModel + UserWishlistsViewModel each gain: `_viewModeState=MutableRedeliverStateFlow(WishlistViewMode.List)`, `viewModeState`, `onViewModeSelected(mode)`. Default=List (preserves current UX).
- WishlistViewModel also gains imageUrl(id) + loadImageBytes(id) passthrough (parity with UserWishlistsViewModel) for card media.

## NEW STRINGS (WishlistStrings)
- viewModeLabel="View" / "Вид"
- viewModeList="List" / "Список"
- viewModeGrid="Grid" / "Плитка"
- helper WishlistViewMode.labelResource() -> mapping.

## POSSIBLE PROBLEMS / RISKS
- problem=WishlistViewModel lacks image accessors. resolution=add passthrough methods (model already has them). low risk.
- problem=JS Stylesheet Rule: grid layout + card overlay badge => custom CSS. resolution=add object stylesheets `WishlistItemCardStylesheet`/grid styles per JS_Stylesheet_Rule (StyleSheet object in same ui/ package), Style(...) applied in card composable. Use Bootstrap card classes for skeleton; use stylesheet only for grid container + badge absolute-position overlay.
  - NOTE: existing JS views (WishlistView/UserWishlistsView) do NOT currently define a *Stylesheet object and use inline style{} for image sizing. Rule says custom CSS MUST be in stylesheet. New card component introduces grid + absolute overlay -> create WishlistItemCardStylesheet. Keep minimal.
- problem=priority badge top-right overlay positioning on jvm/android. resolution=Box with badge aligned TopEnd over media (or over whole card top). Material Card supports Box overlay.
- problem=Material v2 (jvm) Card import = androidx.compose.material.Card; Material3 (android) = androidx.compose.material3.Card. keep per-platform.
- ambiguity=issue offers OR-choice for badge position (top-right corner OR right of title). DECISION=top-right corner overlay (cleaner on card with media). Reasonable design decision per task permission.
- ambiguity=grid columns count. DECISION=js Bootstrap responsive `row-cols-1 row-cols-sm-2 row-cols-md-3`; jvm/android `LazyVerticalGrid(GridCells.Adaptive(minSize=180.dp/160.dp))`. Reasonable.

## PLAN OF CHANGES (files)
commonMain:
- ui/WishlistViewMode.kt (NEW): enum WishlistViewMode { List, Grid } + KDoc.
- ui/WishlistViewModel.kt (EDIT): add viewModeState/onViewModeSelected + imageUrl/loadImageBytes passthrough.
- ui/UserWishlistsViewModel.kt (EDIT): add viewModeState/onViewModeSelected.
- WishlistStrings.kt (EDIT): add viewMode* strings + WishlistViewMode.labelResource().
jsMain:
- ui/WishlistItemCard.kt (NEW) + WishlistItemCardStylesheet (same file/package).
- ui/ViewModeSelector.kt (NEW).
- ui/WishlistView.kt (EDIT): add ViewModeSelector; grid branch renders cards.
- ui/UserWishlistsView.kt (EDIT): add ViewModeSelector; grid branch renders cards (grouped + custom-sort).
jvmMain:
- ui/WishlistItemCard.kt (NEW).
- ui/ViewModeSelector.kt (NEW).
- ui/WishlistView.kt (EDIT), ui/UserWishlistsView.kt (EDIT).
androidMain:
- ui/WishlistItemCard.kt (NEW).
- ui/ViewModeSelector.kt (NEW).
- ui/WishlistView.kt (EDIT), ui/UserWishlistsView.kt (EDIT).
README.md (EDIT): Architecture Notes (not Operator Notes) — document card + view-mode.

## BUILD TASK
- `./gradlew :wishlist.features.ui.wishlist:build` after coding. one fix cycle per CODING.md.

## VERIFICATION
- check=all_new_kt_have_KDoc; expected=true
- check=js_custom_css_in_stylesheet_object; expected=true
- check=operator_notes_unmodified; expected=true
- check=gradle_build; expected=PASS

## UNCERTAINTY
- missing=none_blocking
- ambiguity=badge_position+grid_cols -> resolved via reasonable OR-choices above (task permits).

## RESULT REPETITION
- entity_id=issue#17; new_state=planned; stored_in=agents/task/a30ca55a-818f-4f91-b4a8-6014a0c88f3e/STEP_0.md; status=available
- next_step=STEP_1 Architecturing
