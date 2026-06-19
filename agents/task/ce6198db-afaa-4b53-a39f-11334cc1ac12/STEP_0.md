# STEP_0 — Planning

task=disable_sidebar_items_for_unauthorized_user
scope=web client (Calm Studio sidebar) only; Android/Desktop unaffected (feature ships commonMain+jsMain only)

requirement: primary sidebar nav points unavailable to an anonymous caller MUST render disabled (dimmed, non-clickable). Operator-named examples: Settings, "books" (=Reserved).

classification of primary items (features/ui/sidebar):
- MyLists  -> WishlistsListViewConfig(userId=null) = caller's own lists -> REQUIRES_AUTH -> disable when anonymous
- Discover -> UsersListViewConfig()                = browse other people -> PUBLIC       -> stays enabled
- Reserved("books") -> MyPresentsBooksViewConfig() = caller's reservations -> REQUIRES_AUTH -> disable when anonymous
- Settings -> UserEditViewConfig(me)              = account settings     -> REQUIRES_AUTH -> disable when anonymous

anonymous detection: SidebarViewModel.currentUserIdState (==null => anonymous), already collected in SidebarView.onDraw.

already anonymous-safe (no change needed): "Your lists" + "New list" block (rendered only when currentUserId!=null), bottom profile row (login widget shown when anonymous), onSelectSettings/onOpenProfile (already guarded no-op when anonymous).

operator notes: features/ui/sidebar/README.md "## Operator Notes" section is EMPTY -> no constraint violated.
