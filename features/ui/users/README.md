# Feature: UI / Users

## Operator Notes

<!-- Human operator writes here. Agents MUST read and respect before making any changes. Agents MUST NOT modify this section. -->

## Overview

Main page content. Renders the global list of registered users fetched from the
public `features/users/client` `UsersFeature.getAll()` endpoint. Selecting a
user delegates to `UsersListViewInteractor.onUserSelected` which pushes
`WishlistsListViewConfig(userId)` onto the same navigation chain (the scaffold's
main slot chain), replacing the users list with that user's wishlists.

No auth required to view the list — anonymous users can browse.

## Routes

None — client-only UI feature.

## Models

| Type | Description |
|------|-------------|
| `UsersListViewConfig` | Empty `@Serializable class` — main slot root identifier |
| `UsersListModel` | Wraps `UsersFeature.getAll()` |
| `UsersListViewInteractor` | `onUserSelected(node, userId)` |
| `UsersListViewModel` | Holds `usersState`, `loadingState`; loads on init and on resume |

## Architecture Notes

- Interactor implementation lives in `client/ClientPlugin` (intra-feature push pattern).
- Interactor pushes `WishlistsListViewConfig(userId)` onto `node.chain` —
  the same chain instance that owns the users list, so wishlist navigation
  (open wishlist → item → edit) layers on top of the users list naturally.
- View loads users on init AND on `node.onResumeFlow` so returning to the list
  after a sub-screen pops refreshes the data.
