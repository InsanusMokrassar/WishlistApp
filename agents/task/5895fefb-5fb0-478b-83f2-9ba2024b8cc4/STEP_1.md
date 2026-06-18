# STEP_1 — Architecturing

change all 7 SidebarViewInteractor navigations from push to replace:
- navigateSection(target, isSection): pushOrBackUntil -> replaceLastOrBackUntil (My Lists / Discover / Reserved / Settings; type-based filter preserved so an existing section is reused).
- onSelectWishlist(id): push(WishlistViewConfig) -> replaceLastOrBackUntil(WishlistViewConfig(id)) (default config-equality filter: back to it if already open, else replace top).
- onCreateList(): push(WishlistEditViewConfig(null)) -> replaceLastOrBackUntil(...).
- onOpenProfile(userId): push(UserViewConfig(userId)) -> replaceLastOrBackUntil(...).
- remove the now-unused import pushOrBackUntil; replaceLastOrBackUntil is already imported.

behavior: at the root level the main chain stays length 1 (one section), so the breadcrumb shows a single crumb.
accepted limitation ("for now"): when a deeper detail stack exists (e.g. deep-linked), replace only swaps the TOP node; ancestors below remain as crumbs. Full reset-to-section is out of scope per operator.
doc: the features/ui/sidebar/README.md architecture note is updated to describe the replace semantics.
