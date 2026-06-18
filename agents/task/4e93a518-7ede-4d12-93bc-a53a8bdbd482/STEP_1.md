# STEP_1 — Architecturing

new helper (features/common/client/src/commonMain/kotlin/utils/NavigationPushOrBackUpTo.kt):
- suspend inline fun NavigationChain<T>.resetToSingleNode(config, filter): drops every node above the bottom one (top-down, awaiting each removal), then replace(bottom, config) => the chain holds exactly one node. No-op when the chain is already a single node satisfying filter. Empty chain => push(config).
- contrast: replaceLastOrBackUntil swaps only the top node; resetToSingleNode resets the whole chain.

ClientPlugin SidebarViewInteractor: swap every replaceLastOrBackUntil for resetToSingleNode:
- navigateSection(...) (My Lists / Discover / Reserved / Settings) keeps the type-based filter (so it can no-op on the already-active section).
- onSelectWishlist / onCreateList / onOpenProfile use the default config-equality filter.
keep the replaceLastOrBackUntil import (still used by ~12 Back interactors in ClientPlugin).

doc: features/ui/sidebar/README.md architecture note updated to the reset-to-single semantics.
