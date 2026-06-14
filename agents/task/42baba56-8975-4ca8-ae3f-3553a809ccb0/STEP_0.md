# STEP_0 Planning — Issue #38 (link titles)

ENTITY:
entity_id=issue_38; type=feature_request; state=planned

CONTEXT:
- task_id=42baba56-8975-4ca8-ae3f-3553a809ccb0; agent_id=root(orchestrator)
- branch=issue/38-link-titles; constraints=[no_branch_switch, no_commit, no_push, code+report_only]
- memory_ref=[CLAUDE.md, AGENTS.md, SHORTCUTS.md(AML-HIP), ALL.md, local.ALL.md, ORCHESTRATOR.md, PLAN.md, ARCHITECTURE.md, CODING.md, local.CODING.md, AST_INDEX.md]

## Requirement (issue #38)

requirement=add_optional_title_per_link; relation: link → (url, title?)
display_rule:
- condition=title_present → render title as clickable text, href=url
- condition=title_absent → render bare url as clickable text, href=url

## Investigated touch sites (verified via grep/ast-index + Read)

ENTITY entity_id=model; type=kotlin_models; location=features/wishlist/common/src/commonMain/kotlin/models
- WishlistItem.kt: `val links: List<String>` on sealed interface WishlistItem + NewWishlistItem + RegisteredWishlistItem (lines 36, 68, 96)
- WishlistItemCopy.kt: `links = links` (toNewItem, line 22), `links == other.links` (hasSameContentAs, line 43)

ENTITY entity_id=server_storage; type=exposed_repo; location=features/wishlist/common/src/jvmMain/kotlin/repo/ExposedWishlistItemRepo.kt
- private inner table WishlistItemsLinks(`wishlist_item_links`): cols `item_id`(FK CASCADE) + `link`(text); PK(item_id, link)
- linksFor(itemId): List<String> (line 121)
- asObject: `links = linksFor(id)` (line 146)
- update(): deleteWhere + per-link insert (lines 180-186)
- InsertStatement.asObject(): per-link insert (lines 206-212)

ENTITY entity_id=edit_vm; type=viewmodel; location=features/ui/wishlist/src/commonMain/kotlin/ui/WishlistItemEditViewModel.kt
- `_linksState: List<String>` (line ~70), `_newLinkState: String`
- onNewLinkChanged, onAddLink (trim+append string), onRemoveLink(index)
- init load: `_linksState.value = item.links` (line 130)
- onSave: `links = _linksState.value` (line 344)

ENTITY entity_id=edit_views; type=compose_views; platforms=[js, jvm, android]
- js: WishlistItemEditView.kt lines 216-250 (link list + add input)
- jvm: WishlistItemEditView.kt lines ~66, ~193
- android: WishlistItemEditView.kt lines ~71, ~198

ENTITY entity_id=read_views; type=compose_views; platforms=[js, jvm, android]
- js: WishlistItemView.kt lines 126-141 (A href=link text=link)
- jvm: WishlistItemView.kt lines 120-127 (Text(link) — NOT currently clickable)
- android: WishlistItemView.kt lines 125-132 (Text(link) — NOT currently clickable)

ENTITY entity_id=strings; type=string_resources; location=features/ui/wishlist/src/commonMain/kotlin/WishlistStrings.kt
- existing: linksLabel(24), addLinkButton(25), newLinkPlaceholder(26), noLinks(69)
- new needed: link title input placeholder/label

## Backward-compatibility analysis

fact=links stored in separate table `wishlist_item_links` (one row per link, col `link` TEXT), NOT JSON-serialized in items row.
fact=model `links: List<String>` is also wire-format (JSON over Ktor) AND serialization of NewWishlistItem/RegisteredWishlistItem.

decision=introduce `@Serializable data class WishlistItemLink(val url: String, val title: String? = null)`.
model_change: `links: List<String>` → `links: List<WishlistItemLink>`.

DB backward-compat:
- option_chosen=add nullable `title` TEXT column to `wishlist_item_links` table.
- existing rows: `title` is NULL → maps to WishlistItemLink(url, title=null) → display bare url. PRESERVES existing data.
- SchemaUtils.createMissingTablesAndColumns adds the nullable column automatically on startup (same pattern as `amount` default column documented in README).
- PK currently (item_id, link). Keep PK on (item_id, link/url) — title is not part of identity. url column stays the same physical `link` column name to avoid data loss. NEW nullable `title` column added.

Wire/JSON backward-compat:
- OLD client sending `links: ["http://..."]` (array of strings) vs NEW `links: [{url, title}]` — INCOMPATIBLE JSON shape.
- assessment=template app, single repo, server+client deployed together; no external API consumers. Breaking wire shape ACCEPTABLE (atomic deploy). No custom serializer shim required.
- NOTE for report: if zero-downtime mixed-version clients were a requirement a custom KSerializer accepting both string and object would be needed. Not implemented (out of scope; atomic deploy assumed). Will flag in final report.

## Plan of changes (ordered)

1. model: add WishlistItemLink.kt (new file, KDoc). Change WishlistItem.kt links type ×3. Update KDoc.
2. model: WishlistItemCopy.kt — `links = links` still valid (same type); `links == other.links` still valid (data class equality). NO logic change, but verify compiles.
3. server: ExposedWishlistItemRepo.kt — add `title` nullable column to WishlistItemsLinks; linksFor returns List<WishlistItemLink>; update/asObject write url+title.
4. edit VM: `_linksState: List<WishlistItemLink>`; add `_newLinkTitleState`; onAddLink builds WishlistItemLink(url, title.ifBlank{null}); onRemoveLink unchanged; onSave links pass-through.
5. edit views ×3: add optional title input next to url input; show title (or url) in the per-link list row.
6. read views ×3: render `link.title ?: link.url` as the clickable text, href=link.url. jvm/android currently render plain Text — keep as Text showing (title or url); url clickability on jvm/android is out-of-scope unless trivial (they were never clickable). Display rule = show title when present else url.
7. strings: add `linkTitlePlaceholder` (EN+RU). Delegate *.md/string-doc to haiku if used.
8. build modules: :wishlist.features.wishlist.common, :wishlist.features.ui.wishlist. one fix cycle.
9. ast-index update.
10. update both feature README ## Architecture Notes / ## Models.

## Open questions / blockers

blocker=NONE. ambiguity=jvm/android read views currently render links as non-clickable Text; issue says "shown as text with link". Decision: preserve existing platform behavior (jvm/android = Text label = title-or-url; js = anchor). Display-rule (title-or-url) applied on all 3. No new clickable-link infra introduced on jvm/android (avoids scope creep). Will note in report.

VERIFICATION:
- check=model compiles with List<WishlistItemLink>; expected=pass
- check=existing DB rows (title NULL) read as WishlistItemLink(url, null); expected=pass
- check=display shows title when present else url; expected=pass

REPETITION OF RESULT:
- entity_id=issue_38; new_state=planned; stored_in=agents/task/42baba56-8975-4ca8-ae3f-3553a809ccb0/STEP_0.md; status=available
