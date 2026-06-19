# STEP_3 — Coding (follow-up: full raw-style migration)

trigger: operator review — inline `style { }` blocks were still present in WishlistItemEditView after STEP_2. STEP_2 migrated only the link-editor styles and left other pre-existing inline styles as "out of scope", which violates the styling rule (ALL raw styles MUST live in CalmStudioStyleSheet or a per-view StyleSheet). Correction: migrate every remaining inline style in the view.

migrated (all static) -> new classes in WishlistItemEditStyleSheet:
- custom-priority weight input margin-top       -> customWeight
- attached-images wrapping grid                 -> imageGrid
- per-image positioning cell                    -> imageCell
- image thumbnail (96px square, object-fit cover) -> imageThumb
- per-image remove button (absolute top/right)  -> imageRemove (appended to the existing btn/danger/sm classes)
- bottom action bar (flex / gap / margin-top)   -> actions
- action-bar flex spacer                         -> actionsSpacer

result: grep "style {" WishlistItemEditView.kt => 0 inline element styles. Only the StyleSheet DSL `by style { }` delegates remain (in WishlistItemEditStyleSheet / CalmStudioStyleSheet), which are the rule-compliant home for styles.

note: no dynamic (data-derived) inline styles existed in this view, so all were migratable to a static sheet.
build: :wishlist.features.ui.wishlist:compileKotlinJs => BUILD SUCCESSFUL. ast-index rebuilt.
