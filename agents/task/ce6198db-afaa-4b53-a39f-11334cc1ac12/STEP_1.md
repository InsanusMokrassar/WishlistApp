# STEP_1 — Architecturing

decision: gate at the view layer (visual disable) + defense-in-depth at the viewmodel (no-op clicks). No new types, no enum change, no DI change.

files:
1. features/ui/sidebar/src/jsMain/kotlin/ui/SidebarView.kt
   - NavItem(): add param enabled:Boolean=true; when !enabled apply Compose-HTML attribute disabled() (import org.jetbrains.compose.web.attributes.disabled). Matches existing project idiom (CalmButtons.kt: `if (disabled) disabled()`).
   - onDraw(): derive `val signedIn = currentUserId != null`; pass enabled=signedIn to MyLists, Reserved, Settings NavItems. Discover unchanged (public).
2. features/common/client/src/jsMain/kotlin/ui/CalmStudioStyleSheet.kt
   - navitem: add `self + disabled style { opacity .5; cursor default; self+hover neutralized }`. Mirrors existing `.btn:disabled` rule. `.btn` already had it; `.navitem` did not.
3. features/ui/sidebar/src/commonMain/kotlin/ui/SidebarViewModel.kt
   - onSelectMyLists()/onSelectReserved(): early-return when currentUserIdFlow.value==null. Mirrors pre-existing onSelectSettings()/onOpenProfile() guards. Redundant behind the disabled button but keeps the VM internally consistent.

rationale: the disabled HTML attribute removes pointer events (click cannot fire) AND CSS :disabled dims the row; the VM guard covers any non-UI invocation path.
no server/route/model changes. i18n unchanged (labels still shown, just disabled).
