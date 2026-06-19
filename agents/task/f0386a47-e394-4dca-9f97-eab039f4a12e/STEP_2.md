# STEP_2 — Coding

status=DONE; build=SUCCESS (:wishlist.features.ui.wishlist:compileKotlinJs)

changes:
- NEW features/ui/wishlist/src/jsMain/kotlin/ui/WishlistItemEditStyleSheet.kt
    object WishlistItemEditStyleSheet : StyleSheet(); classes linkRow/linkText/addLinkRow/dupError; init registers into StyleSheetsAggregator.
- features/ui/wishlist/src/jsMain/kotlin/ui/WishlistItemEditView.kt
    add-link row inputs reordered: title first, url second (Add button unchanged).
    inline link-editor styles replaced with WishlistItemEditStyleSheet classes.
    hasDuplicateLinks computed from the collected links; dupError hint rendered when true.
    Save disabled when loading || title.isBlank() || hasDuplicateLinks.
- features/ui/wishlist/src/commonMain/kotlin/ui/WishlistItemEditViewModel.kt
    onSave(): early-return when links contain duplicate trimmed urls.
- features/ui/wishlist/src/commonMain/kotlin/WishlistStrings.kt
    new duplicateLinksHint (EN+RU).

verification:
- gradle :wishlist.features.ui.wishlist:compileKotlinJs => BUILD SUCCESSFUL
- ast-index rebuilt after edits
- styling rule satisfied: no new inline raw styles; touched styles live in the per-view StyleSheet.

scope notes:
- JVM/Android item-edit views NOT changed (request targets the web links editor).
- pre-existing unrelated inline styles elsewhere in WishlistItemEditView (images, action bar) left as-is (out of scope).
- not run in a live browser; compile-verified only.
