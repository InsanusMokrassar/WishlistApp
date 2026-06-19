# STEP_1 — Architecturing

parsePath (client/src/jsMain/kotlin/UrlNavigationConfigsRepo.kt):
- read scaffoldConfig = ClientPlugin.mainScaffoldConfig once (instance reused for node config + leftConfig).
- build subchains via buildList: Top chain (TopBarViewConfig, TopNavigationChainId); IF scaffoldConfig.leftConfig != null -> Left chain (single node = leftConfig, LeftNavigationChainId); Main chain (mainStack, MainNavigationChainId).
- the sidebar carries no deep state, so a single-node left chain seeded from leftConfig suffices; ScaffoldView reattaches it by matching LeftNavigationChainId (SubchainsHost) instead of falling back to a fresh InjectNavigationChain.
- guard on leftConfig == null keeps non-web shells (no left slot) safe.

imports: add LeftNavigationChainId. No SidebarViewConfig import needed (reuse scaffoldConfig.leftConfig).
no view/model change.
