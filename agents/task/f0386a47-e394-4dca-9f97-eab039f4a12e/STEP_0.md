# STEP_0 — Planning

task=item_link_editing_changes; scope=web client (JS) only — WishlistItemEditView

source_requests:
1. add-link form: reorder inputs so the TITLE input precedes the URL input (title first).
2. duplicate links: when the links list contains repeated urls, the Save button MUST be disabled.

styling_constraint: every raw style MUST live in CalmStudioStyleSheet OR a per-view `object <ViewNameWithoutViewSuffix>StyleSheet : StyleSheet()`. The links editor used inline `style { }` blocks -> migrate the touched ones into a per-view sheet.

files:
- features/ui/wishlist/src/jsMain/kotlin/ui/WishlistItemEditView.kt (add-link row order, save-disable, style migration)
- features/ui/wishlist/src/commonMain/kotlin/ui/WishlistItemEditViewModel.kt (defense-in-depth onSave guard)
- features/ui/wishlist/src/commonMain/kotlin/WishlistStrings.kt (duplicate hint string)
- NEW features/ui/wishlist/src/jsMain/kotlin/ui/WishlistItemEditStyleSheet.kt (per-view stylesheet)

duplicate definition: two WishlistItemLink with the same trimmed url; title differences do not matter.
operator notes: features/ui/wishlist/README.md "## Operator Notes" is EMPTY -> no constraint violated.
