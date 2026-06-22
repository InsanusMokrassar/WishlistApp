# Task: GitHub Issue #53

## Issue
Title: "On logout each edit view must implement its exit (it must be changed with its non-edit view)"
Body: (empty)

## Interpretation
When the user logs out while any edit/editor view is open, that edit view must exit itself and be
replaced by its corresponding non-edit (read) view. Apply this to EVERY user-facing edit screen.
Design the cleanest shared mechanism.

## Context
- Prior PR #58 (branch `issue/53-logout-exit-edit-views`, commit 13a39b0) attempted this and was
  CLOSED by the operator WITHOUT comments (silent rejection). Fresh max-effort implementation
  required; do NOT assume the prior approach was correct. Inspect for reference only.
- Prior PR approach: a per-ViewModel helper `Flow<T?>.subscribeOnLoggedOut` subscribed to each
  feature's `currentUserIdFlow`; edit ViewModels called their contextual Back interactor on a
  non-null→null transition. It EXPLICITLY EXCLUDED admin-panel edit views, arguing
  `AdminPanelModel` exposes no auth/me flow and admin is root-gated separately.
- The issue says "EACH edit view" — reconsider whether the admin exclusion is why the PR was
  rejected. Decide deliberately and justify.

## Edit views in repo (from ast-index)
User-facing:
- features/ui/wishlist: WishlistEditViewModel, WishlistItemEditViewModel
- features/ui/users: UserEditViewModel
Admin-panel:
- features/ui/adminPanel: AdminUserEditViewModel, AdminWishlistEditViewModel, AdminWishlistItemEditViewModel

## Candidate shared mechanism (for the design roles to evaluate)
`AuthCredentialsStorage.userAuthorised: StateFlow<Boolean>` (features/auth/client) is the global,
platform-shared, single source of truth for login state (true→false == logout). Unlike per-feature
`currentUserIdFlow`, it is available to the admin feature too, so a single shared logout-detection
helper keyed on this flow can cover ALL edit views including admin — removing the prior PR's
exclusion.

## Git constraints
- Already on branch `fix/53-logout-exit-edit-views`. Do NOT create/switch/commit/push/stash
  branches or touch git history. Only modify working-tree files (code + task step files).

## Definition of Done
- Logging out while on any edit view exits that view to its non-edit/read view. Cover ALL
  user-facing edit views; justify any deliberately-excluded view in the validation step.
- Affected module(s) BUILD successfully (gradle BUILD SUCCESSFUL).
- Validation role confirms correctness.
