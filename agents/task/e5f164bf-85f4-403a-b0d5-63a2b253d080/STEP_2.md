ENTITY:
entity_id=task_issue22; type=coding; state=complete

CONTEXT:
* task_id=e5f164bf-85f4-403a-b0d5-63a2b253d080; agent_id=root_orchestrator
* constraints=[Bootstrap_dropdown, jsMain_only, keep_public_API, no_commit/push/PR]

ACTIONS:
1. action=rewrite; target=features/ui/wishlist/src/jsMain/kotlin/ui/WishlistSortSelector.kt; params={old=btn-group(btn-primary/btn-outline-primary per mode), new=Bootstrap dropdown(div.dropdown > button.btn.btn-outline-primary.btn-sm.dropdown-toggle[text=selected label] + div.dropdown-menu[+show when expanded] > button.dropdown-item[+active when mode==selected] per availableModes); state=expanded via remember{mutableStateOf(false)}; label helper reuses None→noneLabel else labelResource(); KDoc updated to describe dropdown}
2. action=rewrite; target=features/ui/wishlist/src/jsMain/kotlin/ui/ViewModeSelector.kt; params={old=btn-group; new=Bootstrap dropdown same shape over WishlistViewMode.entries; KDoc updated}
3. action=edit; target=features/ui/wishlist/README.md; params={section=Architecture Notes; updated ViewModeSelector line + WishlistSortSelector line to document JS Bootstrap dropdown + unchanged public API; Operator Notes NOT modified}
4. action=rebuild; target=ast-index; result=507 files indexed exit=0

API_PRESERVED:
* WishlistSortSelector(selected, onSortModeSelected, noneLabel, availableModes) unchanged → callers WishlistView.kt+UserWishlistsView.kt(jsMain) untouched
* ViewModeSelector(selected, onViewModeSelected) unchanged
scope_excluded=jvmMain+androidMain (Material, not Bootstrap; local.CODING.md JS-Bootstrap rule targets JS only)

VERIFICATION:
* check=./gradlew :wishlist.features.ui.wishlist:compileKotlinJs; result=BUILD_SUCCESSFUL in 20s; exit=0
* check=no_unused_imports/compile_errors; result=pass (build clean)

UNCERTAINTY:
* missing=none
* note=dropdown form = Bootstrap dropdown-menu component (not native <select>); flagged in STEP_0 for operator review

REPETITION OF RESULT:
* entity_id=WishlistSortSelector.kt(jsMain); new_state=Bootstrap_dropdown; status=compiled
* entity_id=ViewModeSelector.kt(jsMain); new_state=Bootstrap_dropdown; status=compiled
* build=BUILD_SUCCESSFUL
