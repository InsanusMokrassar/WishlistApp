# STEP_2 — Coding

status=DONE; build=SUCCESS (:wishlist.client:compileKotlinJs)

changes:
- features/common/client/src/commonMain/kotlin/utils/NavigationPushOrBackUpTo.kt
    + resetToSingleNode(config, filter): collapse the chain to one node (drop everything above the bottom, then replace the bottom); no-op if already the single matching node; push when empty.
- client/src/commonMain/kotlin/ClientPlugin.kt (single<SidebarViewInteractor>)
    navigateSection + onSelectWishlist + onCreateList + onOpenProfile: replaceLastOrBackUntil -> resetToSingleNode. import added; the replaceLastOrBackUntil import kept (still used by Back interactors).
- features/ui/sidebar/README.md: architecture note -> reset-to-single semantics.

verification:
- :wishlist.client:compileKotlinJs => BUILD SUCCESSFUL; ast-index rebuilt.
- not run in a live browser; compile-verified only.

effect: from any depth (e.g. Discover > User > Wishlist), a sidebar click leaves the main chain with exactly one node => the breadcrumb shows a single crumb. Clicking the already-active section is a no-op.

note: the haiku md-writer subagent was unreliable (no-op on the previous task) -> reports + README written directly by the main thread.
