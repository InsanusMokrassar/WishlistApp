# STEP_0 — Planning

task=restore_sidebar_on_page_reload

bug: on page refresh with a deep URL (e.g. /ui/wishlist/<id>), the left sidebar is missing. The URL-restored navigation hierarchy has no left/sidebar chain.

root cause: client/jsMain UrlNavigationConfigsRepo.parsePath rebuilds the scaffold skeleton with only the Top (TopNavigationChainId) and Main (MainNavigationChainId) sub-chains. The Left slot (Calm Studio sidebar, LeftNavigationChainId) was added in the redesign but parsePath was never updated, so a restored scaffold has no left chain. ScaffoldView reattaches a restored chain to a slot by matching the slot's stable NavigationChainId — with no left chain present the restored deep link comes back sidebar-less.

fix target: parsePath — also recreate the Left chain, seeded from ClientPlugin.mainScaffoldConfig.leftConfig.
operator notes: features/ui/sidebar/README.md + features/ui/scaffold "## Operator Notes" EMPTY -> no constraint violated.
