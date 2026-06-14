# STEP_2 Coding — Issue #38 (link titles)

ENTITY:
entity_id=issue_38; type=feature_request; state=implemented_built
CONTEXT: task_id=42baba56-8975-4ca8-ae3f-3553a809ccb0; prev_step=STEP_1(architected, read)

## Files changed

CREATED:
- features/wishlist/common/src/commonMain/kotlin/models/WishlistItemLink.kt
  - `@Serializable data class WishlistItemLink(val url: String, val title: String? = null)`
  - `val WishlistItemLink.displayText: String` = title(non-blank) ?: url  (centralized display rule)

MODIFIED (model):
- features/wishlist/common/src/commonMain/kotlin/models/WishlistItem.kt
  - links type ×3: `List<String>` → `List<WishlistItemLink>` (interface + NewWishlistItem + RegisteredWishlistItem); KDocs updated.
- features/wishlist/common/src/commonMain/kotlin/models/WishlistItemCopy.kt — NO edit (type-agnostic, compiled clean).

MODIFIED (server storage):
- features/wishlist/common/src/jvmMain/kotlin/repo/ExposedWishlistItemRepo.kt
  - WishlistItemsLinks table: NEW `titleColumn = text("title").nullable()`; PK unchanged (item_id, link).
  - linksFor(): returns List<WishlistItemLink> { url=link, title=title }.
  - update() + InsertStatement.asObject(): write stmt[link]=link.url, stmt[title]=link.title.
  - KDoc updated (title column, backward-compat).

MODIFIED (edit ViewModel):
- features/ui/wishlist/src/commonMain/kotlin/ui/WishlistItemEditViewModel.kt
  - _linksState: List<WishlistItemLink>; NEW _newLinkTitleState/newLinkTitleState/onNewLinkTitleChanged.
  - onAddLink() builds WishlistItemLink(url, title.ifBlank->null); clears both inputs; no-op on blank url.

MODIFIED (edit views ×3): added optional title input + per-link displayText render:
- features/ui/wishlist/src/jsMain/kotlin/ui/WishlistItemEditView.kt (Bootstrap Input; muted url line when title present)
- features/ui/wishlist/src/jvmMain/kotlin/ui/WishlistItemEditView.kt (OutlinedTextField)
- features/ui/wishlist/src/androidMain/kotlin/ui/WishlistItemEditView.kt (OutlinedTextField material3)

MODIFIED (read views ×3): text=displayText, href=url(js only):
- features/ui/wishlist/src/jsMain/kotlin/ui/WishlistItemView.kt (A href=link.url, Text=link.displayText)
- features/ui/wishlist/src/jvmMain/kotlin/ui/WishlistItemView.kt (Text=link.displayText)
- features/ui/wishlist/src/androidMain/kotlin/ui/WishlistItemView.kt (Text=link.displayText)

MODIFIED (strings):
- features/ui/wishlist/src/commonMain/kotlin/WishlistStrings.kt — NEW linkTitlePlaceholder (EN "Title (optional)" / RU "Название (необязательно)").

MODIFIED (docs):
- features/wishlist/README.md — Models (WishlistItemLink, links type), Architecture Notes (title col, backward-compat, wire-shape note).
- features/ui/wishlist/README.md — Item Edit row, new Architecture Note (issue #38), linkTitlePlaceholder.

## Model design chosen
`@Serializable data class WishlistItemLink(url: String, title: String? = null)` + `displayText` extension. data class (2 fields, equality preserved for hasSameContentAs + cache diff). Default null title = minimal churn.

## Backward-compat decision
- DB: reuse existing `link` TEXT column as url (no rename, no data migration). Add nullable `title` TEXT column → auto-added by SchemaUtils on startup. Pre-#38 rows: title=NULL → WishlistItemLink(url, null) → bare-url display. NO data loss.
- Wire/JSON: shape of `links` changed `["url"]` → `[{url,title}]`. Atomic server+client deploy assumed (template app, no external API consumers) → NO dual-format KSerializer shim added. Mixed-version-client deployment would require one. Flagged in README + final report.

## Build result
COMMAND: ./gradlew :wishlist.features.wishlist.common:build :wishlist.features.ui.wishlist:build
RESULT: BUILD SUCCESSFUL (1m 30s). All targets compiled (commonMain/jvmMain/jsMain/androidMain). No fix cycle needed (zero errors on first build).

## ast-index
`ast-index update` run after edits: 11 changed files re-indexed.

## Blockers
NONE.

REPETITION OF RESULT:
entity_id=issue_38; new_state=implemented_built; build=SUCCESSFUL; stored_in=agents/task/.../STEP_2.md; status=available
