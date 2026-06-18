# STEP_1 — Architecturing

per-view stylesheet:
- object WishlistItemEditStyleSheet : StyleSheet() in jsMain/ui; classes linkRow, linkText, addLinkRow, dupError.
- self-registers via init { StyleSheetsAggregator.addStyleSheet(this) }; StyleSheetsAggregator.draw() in ClientJSPlugin is reactive (StateFlow + debounce) so a sheet added on first class-name access renders without a per-call Style(...). var(--cs-danger) resolves against the CalmStudioStyleSheet :root block.

view (WishlistItemEditView.kt):
- existing-link rows: replace inline flex styles with classes(WishlistItemEditStyleSheet.linkRow) + linkText.
- add-link row: classes(addLinkRow); ORDER swapped -> title Input (newLinkTitle) first, url Input (newLink) second, Add button last. Add button still disabled when newLink.isBlank().
- compute `val hasDuplicateLinks = links.size != links.distinctBy { it.url.trim() }.size` from the collected links.
- render the dupError hint (WishlistStrings.duplicateLinksHint) when hasDuplicateLinks.
- Save button: disabled when loading || title.isBlank() || hasDuplicateLinks (previously loading || title.isBlank()).

viewmodel (WishlistItemEditViewModel.kt):
- onSave(): after the title-blank guard, early-return when _linksState.value contains duplicate trimmed urls (mirrors the existing guard pattern; redundant behind the disabled button but covers any non-UI path).

strings:
- new key duplicateLinksHint: EN "Remove duplicate links to save" / RU "Удалите повторяющиеся ссылки, чтобы сохранить".

no model/route/server change. duplicate predicate kept identical in view and VM (one line each).
