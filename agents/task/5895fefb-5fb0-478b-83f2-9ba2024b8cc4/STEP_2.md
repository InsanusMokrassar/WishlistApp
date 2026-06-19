# STEP_2 — Coding

status=DONE; build=SUCCESS (:wishlist.client:compileKotlinJs)

changes (client/src/commonMain/kotlin/ClientPlugin.kt, single<SidebarViewInteractor>):
- navigateSection(): mainChain.pushOrBackUntil(...) -> mainChain.replaceLastOrBackUntil(...) (sections My Lists / Discover / Reserved / Settings).
- onSelectWishlist / onCreateList / onOpenProfile: mainChain()?.push(cfg) -> mainChain()?.replaceLastOrBackUntil(cfg).
- removed the unused import pushOrBackUntil (replaceLastOrBackUntil already imported).
- doc: features/ui/sidebar/README.md architecture note updated (sidebar actions now replace the top node instead of pushing).

verification:
- :wishlist.client:compileKotlinJs => BUILD SUCCESSFUL; ast-index rebuilt.
- not run in a live browser; compile-verified only.

effect: clicking a sidebar item replaces the current top node on the main chain, so the breadcrumb no longer accumulates a crumb per click. Deep ancestor stacks (deep-link) keep their lower crumbs ("for now" scope).

note: the haiku md-writer subagent no-op'd (claimed done, 0 tool calls); reports + README written directly by the main thread.
