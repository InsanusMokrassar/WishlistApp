ENTITY:
entity_id=task_issue22; type=plan; state=complete

CONTEXT:
* task_id=e5f164bf-85f4-403a-b0d5-63a2b253d080; agent_id=root_orchestrator; memory_ref=[agents/CODING.md, agents/local.CODING.md, features/ui/wishlist/README.md]
* constraints=[branch=issue/22-modes-dropdowns, no_commit, no_push, no_PR, scope=JS_Compose_HTML_client_only, Bootstrap_styling_required, keep_public_API]

REASON:
* condition=GitHub_issue#22 "Rework modes as dropdowns" → requirement=convert button-group selectors WishlistSortSelector+ViewModeSelector into Bootstrap dropdown controls

INVESTIGATION:
1. entity=WishlistSortSelector; locations=jsMain+jvmMain+androidMain ui/WishlistSortSelector.kt; jsMain=button-group(btn-group btn-group-sm, btn-primary/btn-outline-primary per mode)
2. entity=ViewModeSelector; locations=jsMain+jvmMain+androidMain ui/ViewModeSelector.kt; jsMain=button-group same pattern
3. entity=local.CODING.md; rule="JS views MUST use Bootstrap classes"; scope_marker=JS Compose-HTML client → only jsMain files target
4. precedent=CurrencySelector.kt(jsMain)+PriceUnitsSelector.kt(jsMain) use native <select class=form-select> as "dropdown"
5. no_existing usage of Bootstrap dropdown-menu/dropdown-toggle in features/
6. enums: WishlistSortMode @ commonMain/ui/UserWishlistsViewModel.kt:46; WishlistViewMode @ commonMain/ui/WishlistViewMode.kt; labels via labelResource()+translation()

DECISION:
* decision=convert jsMain WishlistSortSelector+ViewModeSelector from btn-group → Bootstrap dropdown (button.dropdown-toggle + ul.dropdown-menu). Rationale: issue literal term "dropdowns"; true Bootstrap dropdown component vs native <select>; keeps localized labelResource() rendering + active highlight; Compose controls open/close via local state (no Bootstrap JS), consistent with modal pattern in local.CODING.md.
* scope_limit=jsMain only. jvmMain+androidMain are Material (not Bootstrap, not "simple buttons groups as dropdowns" target per local.CODING.md JS-Bootstrap rule); public API (params) unchanged so callers WishlistView+UserWishlistsView untouched.

UNCERTAINTY:
* ambiguity=dropdown form (native <select> vs dropdown-menu). resolution=dropdown-menu component chosen as truer "dropdown" + preserves per-mode localized labels and active state highlight; documented for operator review.

EXPECTED RESULT:
* entity_id=WishlistSortSelector.kt(jsMain); new_state=Bootstrap_dropdown
* entity_id=ViewModeSelector.kt(jsMain); new_state=Bootstrap_dropdown

VERIFICATION:
* check=gradle compile :wishlist.features.ui.wishlist js target; expected=BUILD_SUCCESSFUL
