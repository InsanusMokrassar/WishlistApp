# STEP_2 — Coding

status=DONE; build=SUCCESS

changes applied (git diff --stat):
- features/common/client/src/jsMain/kotlin/ui/CalmStudioStyleSheet.kt  (+6/-1)
    navitem: added `self + disabled` rule (opacity .5; cursor default; hover neutralized). KDoc updated to list the disabled state.
- features/ui/sidebar/src/jsMain/kotlin/ui/SidebarView.kt  (+17/-3)
    import org.jetbrains.compose.web.attributes.disabled
    NavItem(... enabled:Boolean=true ...) { if (!enabled) disabled() }
    onDraw: val signedIn = currentUserId != null
    enabled=signedIn on MyLists, Reserved, Settings; Discover untouched.
- features/ui/sidebar/src/commonMain/kotlin/ui/SidebarViewModel.kt  (+6/-2)
    onSelectMyLists()/onSelectReserved(): early-return when anonymous.

verification:
- gradle: :wishlist.features.common.client:compileKotlinJs + :wishlist.features.ui.sidebar:compileKotlinJs => BUILD SUCCESSFUL
- warning SidebarViewModel.kt:74 is PRE-EXISTING library @Warning on changesInSubTreeFlow (not from this change)
- ast-index rebuilt after edits
- git status: exactly 3 tracked files modified, no collateral changes

behavior: an anonymous caller now sees My Lists / Reserved / Settings dimmed and non-clickable; Discover stays active; signing in re-enables them reactively via currentUserIdState.

out of scope: no automated UI test added (feature has no existing test harness); manual visual check in the running JS client recommended.
