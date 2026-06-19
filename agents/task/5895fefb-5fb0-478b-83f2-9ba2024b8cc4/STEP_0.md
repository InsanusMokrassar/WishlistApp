# STEP_0 — Planning

task=sidebar_nav_replace_instead_of_push

problem: the top-bar breadcrumb shows the full main-chain path; every sidebar click pushes a new node onto the scaffold main chain, so the breadcrumb keeps growing.
operator decision: sidebar clicks should REPLACE the current (top) node instead of pushing ("at least for now").

breadcrumb source: TopBarViewModel exposes every TopBarTitleProvider node on the main chain (MainNavigationChainId) stack, bottom->top; the view concatenates them. So breadcrumb length == main-chain stack length, and replacing the top node instead of pushing keeps it from growing.

target: client/src/commonMain/kotlin/ClientPlugin.kt — the single<SidebarViewInteractor> implementation (drives the main chain). No view/VM change needed (SidebarViewModel already delegates every action to this interactor).
helper already available: NavigationChain.replaceLastOrBackUntil (features/common/client utils/NavigationPushOrBackUpTo.kt) — replaces the last node with the target, or backs up to it if already present.
operator notes: features/ui/sidebar/README.md "## Operator Notes" EMPTY -> no constraint violated.
