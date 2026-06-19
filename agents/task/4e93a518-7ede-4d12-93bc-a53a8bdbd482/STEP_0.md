# STEP_0 — Planning

task=sidebar_nav_reset_chain_to_single_node

problem: the previous change made sidebar clicks replace only the TOP node (replaceLastOrBackUntil). When the user had gone deep (e.g. Discover > User > someone's Wishlist), a sidebar click replaced just the top node and left the lower ancestors as breadcrumb crumbs.
operator decision: a sidebar click must ALWAYS leave the main chain holding a single node (the target) — "contain one chain and then replace it" => reset the whole chain to one node, so the breadcrumb is a single crumb regardless of prior depth.

target: client ClientPlugin single<SidebarViewInteractor>; new helper added in features/common/client utils.
no view/VM change (SidebarViewModel delegates every action to the interactor).
operator notes: features/ui/sidebar/README.md "## Operator Notes" EMPTY -> no constraint violated.
