# STEP_1 Architecturing — Issue #38 (link titles)

ENTITY:
entity_id=issue_38; type=feature_request; state=architected
CONTEXT: task_id=42baba56-8975-4ca8-ae3f-3553a809ccb0; prev_step=STEP_0(planned, read)

## Type design (chosen)

new_type: location=features/wishlist/common/src/commonMain/kotlin/models/WishlistItemLink.kt
```kotlin
@Serializable
data class WishlistItemLink(
    val url: String,
    val title: String? = null,
)
```
rationale:
- `data class` (NOT inline value) because two fields (url + optional title).
- `title: String? = null` default → minimal call-site churn; absent title = bare-url display.
- `@Serializable` → wire format (Ktor JSON) + persisted model serialization.
- equality (data class) → preserves `hasSameContentAs` semantics (WishlistItemCopy.kt line 43) AND FullCRUDCacheRepo idGetter/diff correctness.

helper (display rule centralization):
```kotlin
val WishlistItemLink.displayText: String get() = title?.takeIf { it.isNotBlank() } ?: url
```
location=same file. Used by all 3 read views + edit list rows → single source of the title-or-url rule. KDoc mandatory.

## Model change

WishlistItem.kt:
- interface field: `val links: List<WishlistItemLink>` (was List<String>)
- NewWishlistItem.links: List<WishlistItemLink> = emptyList()
- RegisteredWishlistItem.links: List<WishlistItemLink> = emptyList()
- KDoc updated: "External links (url + optional title) related to the item."

WishlistItemCopy.kt: NO source edit required (`links = links` and `links == other.links` are type-agnostic). Verify-compile only.

## Server storage architecture (ExposedWishlistItemRepo.kt)

table WishlistItemsLinks(`wishlist_item_links`):
- existing cols: `item_id` BIGINT FK CASCADE, `link` TEXT — `link` column REUSED as url (no rename → no data migration).
- NEW col: `title` = text("title").nullable()  → stores WishlistItemLink.title.
- PK stays (item_id, link). url is identity; title not part of PK. (Two links with same url but different titles on one item = disallowed; acceptable — url is the natural key.)

backward-compat: SchemaUtils.createMissingTablesAndColumns (run by initTable on init) ADDS nullable `title` column to existing table. Existing rows → title NULL → WishlistItemLink(url, null). NO data loss. Mirrors documented `amount`/default-column compat pattern.

methods:
- linksFor(itemId): List<WishlistItemLink> = select map { WishlistItemLink(it[link], it[title]) }
- update(): per-link insert sets stmt[link]=link.url, stmt[title]=link.title
- InsertStatement.asObject(): per-link insert sets url+title; returned RegisteredWishlistItem.links = value.links (pass-through, already correct type)

## Edit ViewModel architecture (WishlistItemEditViewModel.kt)

- `_linksState: MutableRedeliverStateFlow<List<WishlistItemLink>>` (was List<String>)
- `_newLinkState: String` (url input) — KEEP
- NEW `_newLinkTitleState: String` (optional title input) + `newLinkTitleState` exposed + `onNewLinkTitleChanged(v)`
- onAddLink(): build WishlistItemLink(url=trimmedUrl, title=newLinkTitle.trim().ifBlank{null}); clear BOTH inputs; no-op when url blank (title alone insufficient).
- onRemoveLink(index): unchanged (index-based).
- init load: `_linksState.value = item.links` (type now matches).
- onSave: `links = _linksState.value` (type matches).

## Edit Views architecture (×3 platforms)

per-link list row: show `link.displayText` (title-or-url). Optionally show url as secondary muted text when title present (js: small muted span). Keep remove button.
add-row: TWO inputs — url (existing newLinkState) + NEW title (newLinkTitleState, optional). Add button gated on url non-blank (unchanged gate, reads newLinkState).
- js: WishlistItemEditView.kt — add second Input for title in the input-group/stacked; bind onNewLinkTitleChanged. Bootstrap classes.
- jvm: OutlinedTextField for title beside url.
- android: OutlinedTextField (material3) for title beside url.

## Read Views architecture (×3 platforms)

display rule: text = link.displayText (title-or-url); href = link.url.
- js: `A(href = link.url) { Text(link.displayText) }` (was href=link, Text(link)).
- jvm: `Text(link.displayText)` (preserve existing non-clickable label behavior; only the shown text changes to title-or-url). url not hyperlinked on desktop — pre-existing limitation, out-of-scope.
- android: `Text(link.displayText)` (same as jvm; material3).

decision=DO NOT introduce clickable-anchor infra on jvm/android (they never had it). Scope=display-rule only. Flag in final report.

## Strings (WishlistStrings.kt)

new: `linkTitlePlaceholder = buildStringResource("Title (optional)") { IetfLang.Russian("Название (необязательно)") }`
existing newLinkPlaceholder reused for url input.
doc/string authoring may be delegated to haiku per ALL.md; simple enough to write inline.

## Module build targets

- :wishlist.features.wishlist.common:build (model + exposed repo)
- :wishlist.features.ui.wishlist:build (vm + views + strings)
one fix cycle per CODING.md.

## README updates (post-code, STEP_2)

- features/wishlist/README.md: Models table (links type), Architecture Notes (wishlist_item_links gains nullable `title` col; backward-compat note); WishlistItemLink new model row.
- features/ui/wishlist/README.md: Item Edit row (optional link title input); Architecture Notes (display rule title-or-url, new string key).

## Blockers
NONE. ambiguity resolved in STEP_0 (jvm/android non-clickable preserved).

REPETITION OF RESULT:
entity_id=issue_38; new_state=architected; stored_in=agents/task/.../STEP_1.md; status=available
