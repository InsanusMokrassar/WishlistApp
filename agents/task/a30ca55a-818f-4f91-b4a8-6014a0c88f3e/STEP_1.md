# STEP_1 Architecturing

task_id=issue#17; role=ORCHESTRATOR->ARCHITECTURE; state=architecture_done; input=STEP_0.md

## ENTITY
entity_id=issue#17; type=ui_enhancement; state=architectured

## CONTEXT
- constraints=[no_file_edits_except_this_report, MVVM_rules, KDocs, JS_Bootstrap, JS_Stylesheet_Rule, plugin_composition_unchanged]
- decision_badge_position=top-right overlay (Box align TopEnd / Bootstrap position-absolute top-0 end-0)
- decision_default_view_mode=List (preserve current UX)
- decision_grid_cols=js responsive row-cols; jvm/android LazyVerticalGrid Adaptive

## 1. COMMON LAYER

### 1.1 NEW file commonMain/ui/WishlistViewMode.kt
```
enum class WishlistViewMode { List, Grid }
```
- KDoc on enum + each entry. List=existing list/row rendering; Grid=card grid.

### 1.2 EDIT commonMain/ui/WishlistViewModel.kt
add:
```
private val _viewModeState = MutableRedeliverStateFlow(WishlistViewMode.List)
val viewModeState = _viewModeState.asStateFlow()
fun onViewModeSelected(mode: WishlistViewMode) { _viewModeState.value = mode }
fun imageUrl(id: FileId): String = model.imageUrl(id)
suspend fun loadImageBytes(id: FileId): ByteArray? = model.loadImageBytes(id)
```
- import FileId (dev.inmo.wishlist.features.files.common.models.FileId).
- KDoc on each new member.

### 1.3 EDIT commonMain/ui/UserWishlistsViewModel.kt
add:
```
private val _viewModeState = MutableRedeliverStateFlow(WishlistViewMode.List)
val viewModeState = _viewModeState.asStateFlow()
fun onViewModeSelected(mode: WishlistViewMode) { _viewModeState.value = mode }
```
- imageUrl/loadImageBytes already present. KDoc on new members.

### 1.4 EDIT commonMain/WishlistStrings.kt
add strings:
```
val viewModeLabel = buildStringResource("View") { IetfLang.Russian("Вид") }
val viewModeList   = buildStringResource("List") { IetfLang.Russian("Список") }
val viewModeGrid   = buildStringResource("Grid") { IetfLang.Russian("Плитка") }
```
add helper:
```
fun WishlistViewMode.labelResource() = when (this) {
    WishlistViewMode.List -> WishlistStrings.viewModeList
    WishlistViewMode.Grid -> WishlistStrings.viewModeGrid
}
```
- import WishlistViewMode. KDoc on helper.

## 2. PER-PLATFORM COMPONENTS

### 2.1 ViewModeSelector (mirrors WishlistSortSelector exactly)
signature (all platforms):
```
@Composable fun ViewModeSelector(selected: WishlistViewMode, onViewModeSelected: (WishlistViewMode) -> Unit)
```
- iterate WishlistViewMode.entries; selected=filled button, other=outlined.
- caption=WishlistStrings.viewModeLabel.
- JS: Bootstrap d-flex + btn-group btn-group-sm; btn-primary/btn-outline-primary. translation() no resources.
- JVM: Material v2 Row + Button(buttonColors)/Button(outlinedButtonColors). translation().
- ANDROID: Material3 Row + Button/OutlinedButton. translation(LocalResources.current).
- KDoc full.

### 2.2 WishlistItemCard
signature (all platforms):
```
@Composable fun WishlistItemCard(
    item: RegisteredWishlistItem,
    wishlistTitle: String?,        // subtitle; null hides subtitle line
    imageUrl: (FileId) -> String,             // JS only path (used by js card)
    loadImageBytes: suspend (FileId) -> ByteArray?, // JVM/Android path
    onSelect: () -> Unit
)
```
RATIONALE for two media accessors: js renders <Img src=imageUrl(id)>; jvm/android use RemoteImage(loader=loadImageBytes(id)). To keep ONE common call-site shape across platforms each platform card takes BOTH lambdas but uses only the relevant one. SIMPLER ALTERNATIVE adopted to avoid unused-param noise: each platform card declares ONLY the accessor it needs.
=> FINAL per-platform signatures:
- JS:   WishlistItemCard(item, wishlistTitle: String?, imageUrl: (FileId)->String, onSelect: ()->Unit)
- JVM:  WishlistItemCard(item, wishlistTitle: String?, loadImageBytes: suspend (FileId)->ByteArray?, onSelect: ()->Unit)
- ANDROID: WishlistItemCard(item, wishlistTitle: String?, loadImageBytes: suspend (FileId)->ByteArray?, onSelect: ()->Unit)

card structure (Bootstrap mapping from issue):
- media   = item.imageIds.firstOrNull() image (if present); else omit media OR placeholder. DECISION: render media block only when image present (cards without image start at title), matches "if present".
- title   = item.title (card-title)
- subtitle= wishlistTitle (card-subtitle text-muted) when non-null
- content = item.description (card-text) when non-blank
- footer  = price+units (card-footer) when item.approximatePrice != null
- priority badge = top-right overlay via PriorityBadge(item.priority)

JS card (Bootstrap):
- outer Div classes("card","h-100") + position-relative for badge overlay.
- badge overlay: Div classes("position-absolute","top-0","end-0","m-2") { PriorityBadge(item.priority) } -- Bootstrap utilities, no custom CSS needed for overlay.
- media: Img classes("card-img-top") with object-fit cover + fixed height -> object-fit/height = custom CSS => place in WishlistItemCardStylesheet (JS_Stylesheet_Rule). Img src=imageUrl(firstImage).
- body: Div classes("card-body") { H5 card-title=title; H6 card-subtitle mb-2 text-muted=wishlistTitle; P card-text=description }
- footer: Div classes("card-footer","text-muted","small") { price+units } when price!=null
- click: onClick onSelect on card; style cursor pointer => cursor in stylesheet (custom CSS) per rule.
- WishlistItemCardStylesheet : StyleSheet() in SAME file/package (dev.inmo.wishlist.features.ui.wishlist.ui). holds: cardClickable(cursor pointer), cardMedia(height 180px, object-fit cover, width 100%). Applied via Style(WishlistItemCardStylesheet) at start of card composable.
- grid container styles: see 2.3.

JVM card (Material v2 Card):
- Card(Modifier.fillMaxWidth()) { Box { Column { media?; title(subtitle1); subtitle(caption,muted) if wishlistTitle!=null; description(caption) if notBlank; price(caption) if price!=null } ; PriorityBadge over Box align TopEnd with padding } }
- media = RemoteImage(key=firstImage.string, loader={loadImageBytes(firstImage)}, contentDescription=null, modifier=fillMaxWidth height 160dp clip) when imageIds nonEmpty.
- clickable: Card modifier .clickable { onSelect() }.
- badge overlay: wrap Card content in Box; Box(align=TopEnd modifier padding 8dp){ PriorityBadge }.

ANDROID card (Material3 Card): same as JVM but material3 Card/typography (titleMedium/bodySmall), colorScheme; translation(LocalResources.current) for any localized text (none in card except none). price text uses item fields (no translation). 

### 2.3 Grid layout
JS:
- grid container Div classes("row","row-cols-1","row-cols-sm-2","row-cols-md-3","g-3").
- each card wrapped in Div classes("col"){ WishlistItemCard(...) }.
- (Bootstrap grid utilities -> no custom CSS for the grid skeleton; complies with local.CODING Bootstrap grid rule.)
JVM/Android:
- LazyVerticalGrid(columns = GridCells.Adaptive(minSize = 180.dp [jvm] / 160.dp [android]), verticalArrangement spacedBy 8dp, horizontalArrangement spacedBy 8dp, contentPadding). import androidx.compose.foundation.lazy.grid.{LazyVerticalGrid,GridCells,items}.

## 3. INTEGRATION INTO VIEWS

### 3.1 WishlistView (js/jvm/android)
- collect viewMode = viewModel.viewModeState.collectAsState().
- after WishlistSortSelector, render ViewModelSelector(viewMode, viewModel::onViewModeSelected). (place ViewModeSelector below sort selector)
- subtitle source = wishlistState.title (collect wishlist). NOTE WishlistView already builds title; collect wishlistState for subtitle string (wishlist?.title).
- branch:
  - viewMode==List -> existing ListRow rendering (UNCHANGED).
  - viewMode==Grid -> grid of WishlistItemCard(item=sortedItem, wishlistTitle=wishlist?.title, imageUrl/loadImageBytes=viewModel::..., onSelect={viewModel.onViewItem(item.id)}).
- JS grid uses Bootstrap row/col; jvm/android use LazyVerticalGrid (replace LazyColumn in grid branch).

### 3.2 UserWishlistsView (js/jvm/android)
- collect viewMode.
- render ViewModeSelector after WishlistSortSelector.
- grouped(None)+List -> existing rendering UNCHANGED.
- Grid mode:
  - when sortMode==None: render per-section header (same header as now) then a grid of cards for section.items with wishlistTitle=section.wishlist.title.
  - when sortMode!=None: render single grid of cards from sortedItems with wishlistTitle=sorted.wishlistTitle.
  - List mode (existing): unchanged (grouped headers + ListRow / flat ListRow).
- card onSelect = { viewModel.onItemSelected(item) }.
- DECISION: keep grouped section headers visible in Grid+None mode (header row identical), cards rendered in a grid under each header.
  - JS: for each section -> header Div (existing) + Div row row-cols... with col-wrapped cards.
  - JVM/Android: simplest correct approach = render headers + grids without nesting LazyVerticalGrid inside LazyColumn (nesting same-axis scrollers is illegal). USE: outer verticalScroll Column; for each section a header + a non-lazy grid. To avoid LazyVerticalGrid-in-Column height issues, use a simple FlowRow OR fixed-cell manual grid.
    - CHOSEN jvm/android grid impl that is safe inside a scrolling Column: `androidx.compose.foundation.layout.FlowRow` (ExperimentalLayoutApi) with maxItemsInEachRow + each card Modifier.weight... -> weight not allowed in FlowRow simply. 
    - SAFER CHOICE: build manual rows: chunk items into rows of N (N=2) and render Row{ each card Modifier.weight(1f) }. Deterministic, no experimental API, works inside vertical scroll Column. Adopt manual chunked grid for jvm/android in BOTH views to keep section headers + avoid nested lazy scrollers.
    - For WishlistView grid (single list, no section headers): can use LazyVerticalGrid directly (it is the only scroller). BUT to keep ONE grid helper shape, also use chunked-rows inside the existing Column for WishlistView? WishlistView currently uses LazyColumn(weight 1f). For grid, replace with LazyVerticalGrid(weight 1f) since it is top-level scroller -> allowed. 
    - DECISION FINALIZED:
      * WishlistView (single flat list): Grid = LazyVerticalGrid (top-level scroller, fillMaxWidth weight 1f). cells Adaptive.
      * UserWishlistsView (sections + headers, must interleave): Grid = wrap content in scrolling container.
        - JVM/Android: replace the LazyColumn with `Column(Modifier.verticalScroll(rememberScrollState()))`; for None mode emit header + chunked Rows of cards; for custom-sort emit chunked Rows of all cards. Chunk size=2. Each card Modifier.weight(1f); pad incomplete last row with Spacer(weight) to keep widths.
        - JS: Bootstrap row/col handles wrapping natively (no scroller concern).
  - RATIONALE: nested same-orientation lazy scrollers crash Compose; chunked Row grid inside verticalScroll Column is the standard safe pattern for "grid interleaved with headers".

## 4. KDOC / STYLE COMPLIANCE
- every NEW .kt: file has KDoc on each public composable/enum/member.
- JS custom CSS (card media height/object-fit, clickable cursor) -> WishlistItemCardStylesheet object (StyleSheet) in jsMain/ui, applied via Style(...). Bootstrap classes for all skeleton/layout. Grid skeleton uses Bootstrap row/col utilities (no custom CSS).
- No plugin/DI changes (no new ViewConfig/ViewModel registration; existing factories already build VMs). NavigationNodeFactory unchanged.
- No changes to settings.gradle / Main.kt / build.gradle (no new module, no new dependency: LazyVerticalGrid/FlowRow are in compose.foundation already used; verticalScroll in foundation).

## 5. README UPDATE (Architecture Notes only; NOT Operator Notes)
- add bullet group: "Item card + view mode":
  - WishlistItemCard per-platform component (media/title/subtitle/content/footer + top-right PriorityBadge overlay).
  - WishlistViewMode enum (List/Grid), default List; viewModeState + onViewModeSelected on both VMs; ViewModeSelector per-platform component; grid layout (js Bootstrap row-cols; jvm/android LazyVerticalGrid for flat WishlistView, chunked Row grid inside verticalScroll for UserWishlistsView sections).
  - WishlistViewModel gains imageUrl/loadImageBytes passthrough for card media parity with UserWishlistsViewModel.
  - new WishlistStrings: viewModeLabel/viewModeList/viewModeGrid + WishlistViewMode.labelResource().

## VERIFICATION
- check=no_nested_same_axis_lazy_scrollers; expected=true (chunked rows used in UserWishlistsView grid)
- check=WishlistViewModel_has_image_accessors_after_edit; expected=true
- check=js_card_custom_css_in_stylesheet; expected=true
- check=default_view_mode_List; expected=true

## UNCERTAINTY
- missing=none
- ambiguity=resolved (badge=top-right; grid impl=per above)

## RESULT REPETITION
- entity_id=issue#17; new_state=architectured; stored_in=agents/task/a30ca55a-818f-4f91-b4a8-6014a0c88f3e/STEP_1.md; status=available
- next_step=STEP_2 Coding
