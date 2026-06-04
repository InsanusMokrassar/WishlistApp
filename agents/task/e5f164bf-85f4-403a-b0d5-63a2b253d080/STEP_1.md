ENTITY:
entity_id=task_issue22; type=architecture; state=complete

CONTEXT:
* task_id=e5f164bf-85f4-403a-b0d5-63a2b253d080; agent_id=root_orchestrator
* constraints=[Bootstrap_dropdown, no_Bootstrap_JS_dependency, Compose_state_driven_open_close, keep_public_API]

DESIGN:
component=Bootstrap_dropdown(state_driven). pattern_per_selector:
1. local_state: var expanded by remember{mutableStateOf(false)}
2. outer Div classes=("d-flex","align-items-center","mb-3","gap-2","flex-wrap") — unchanged caption row
3. Span caption classes=("text-muted","small") — unchanged label
4. Div classes=("dropdown") wrapping:
   a. Button classes=("btn","btn-outline-primary","btn-sm","dropdown-toggle"); onClick→expanded=!expanded; text=current selected mode label
   b. Div classes=("dropdown-menu") + conditional class "show" when expanded; style override not needed (show class toggles display)
      - per mode: Button(type=Button) classes=("dropdown-item") + "active" when mode==selected; onClick→{ callback(mode); expanded=false }; text=mode label
relation: expanded(state)→dropdown-menu.show(class)→visible. Bootstrap CSS only, no Bootstrap JS (matches modal pattern local.CODING.md).

API_PRESERVED:
* WishlistSortSelector(selected, onSortModeSelected, noneLabel, availableModes) — signature unchanged
* ViewModeSelector(selected, onViewModeSelected) — signature unchanged
* callers WishlistView.kt+UserWishlistsView.kt(jsMain) — no edit needed

LABEL_LOGIC_REUSE:
* sort: mode==None→noneLabel.translation() else mode.labelResource().translation() (same as old btn loop, reused for toggle text + items)
* viewmode: mode.labelResource().translation()

STYLESHEET_RULE:
* no custom CSS needed (Bootstrap utility/dropdown classes only) → no <Name>Stylesheet object required (rule applies only when custom CSS needed)

KDOC:
* update existing KDoc on each fun: replace "button group" description with "Bootstrap dropdown" description; keep @param tags

README_UPDATE:
* features/ui/wishlist/README.md Architecture Notes: update ViewModeSelector line + WishlistSortSelector line to state jsMain now renders Bootstrap dropdown (was button-per-mode). Do NOT modify Operator Notes.

VERIFICATION:
* check=./gradlew :wishlist.features.ui.wishlist:compileKotlinJs (or :build); expected=BUILD_SUCCESSFUL
