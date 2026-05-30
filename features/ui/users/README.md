# Feature: UI / Users

## Operator Notes

<!-- Human operator writes here. Agents MUST read and respect before making any changes. Agents MUST NOT modify this section. -->

## Overview

Main page content. Renders the global list of registered users fetched from the
public `features/users/client` `UsersFeature.getAll()` endpoint. Selecting a
user delegates to `UsersListViewInteractor.onUserSelected` which pushes
`WishlistsListViewConfig(userId)` onto the same navigation chain (the scaffold's
main slot chain), replacing the users list with that user's wishlists.

No auth required to view the list — anonymous users can browse. The `root` user
additionally sees a per-row danger **Delete** button (see below).

## Routes

None — client-only UI feature.

## Models

| Type | Description |
|------|-------------|
| `UsersListViewConfig` | Empty `@Serializable class` — main slot root identifier |
| `UsersListModel` | Wraps `UsersFeature.getAll()`, `ClientAuthFeature` (`isCurrentUserRoot()`), and admin `AdminFeature.usersManagement.delete` (`deleteUser(id)`) |
| `UsersListViewInteractor` | `onUserSelected(node, userId)` |
| `UsersListViewModel` | Holds `usersState`, `loadingState`, `isRootState`, `deleteTargetState`, `deleteStepState`; loads users + root flag on init and on resume |

## Architecture Notes

- Interactor implementation lives in `client/ClientPlugin` (intra-feature push pattern).
- Interactor pushes `WishlistsListViewConfig(userId)` onto `node.chain` —
  the same chain instance that owns the users list, so wishlist navigation
  (open wishlist → item → edit) layers on top of the users list naturally.
- View loads users on init AND on `node.onResumeFlow` so returning to the list
  after a sub-screen pops refreshes the data.
- **Root-only user deletion:**
  - `build.gradle` adds `api project` deps on `features/auth/client` (for `ClientAuthFeature`) and `features/admin/client` (for `AdminFeature`).
  - Root detection is client-side: `UsersListModel.isCurrentUserRoot()` = `authFeature.getMe()?.username?.string == "root"`. The delete button renders only when `isRootState` is `true`; the server still enforces root via `403` on the admin endpoint.
  - Deletion uses **two** sequential confirmation dialogs (`deleteStepState`: `0` none / `1` first / `2` final). Flow: `onDeleteUserRequest(user)` → step 1 → `onConfirmDeleteFirst()` → step 2 → `onConfirmDeleteSecond()` → `model.deleteUser(id)` → reload list. `onCancelDelete()` aborts at any stage.
  - `deleteUser` delegates to `AdminFeature.usersManagement.delete`, which cascades server-side (wishlists, items, password, sessions — see `features/admin`).
  - JS uses Bootstrap modals (private `ConfirmModal` composable); JVM/Android use `AlertDialog` (private `ConfirmDialog` composable) with an error-colored confirm button.
