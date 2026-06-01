# Feature: UI / Wishlist

## Operator Notes

<!-- Human operator writes here. Agents MUST read and respect before making any changes. Agents MUST NOT modify this section. -->

## Overview

Client-only UI feature implementing four MVVM screens for wishlist management.
Navigation is chain-based: all screens share one chain, back = `node.chain.pop()`.
JS views use Bootstrap CSS classes via Compose HTML. JVM uses Material v2, Android uses Material3.

## Screens

| Screen | ViewConfig | ViewModel | Description |
|--------|-----------|-----------|-------------|
| Wishlists List | `WishlistsListViewConfig(userId: UserId? = null)` | `WishlistsListViewModel` | Home screen; when `userId` non-null, lists that user's wishlists; when null, lists caller's wishlists; when `userId` non-null, shows "Grid view" button to switch to card grid layout |
| User Wishlists Grid | `UserWishlistsViewConfig(userId: UserId)` | `UserWishlistsViewModel` | Card-grid layout showing a concrete user's wishlists; tapping a card opens the wishlist detail; reachable via "Grid view" button on `WishlistsListView` when viewing a concrete user |
| Wishlist Detail | `WishlistViewConfig(wishlistId)` | `WishlistViewModel` | Shows items; edit controls visible only to owner; auto-pops on resume when wishlist no longer exists |
| Wishlist Edit | `WishlistEditViewConfig(wishlistId?)` | `WishlistEditViewModel` | Create (null id) or edit; back triggers discard modal if dirty; edit mode shows danger Delete (confirm modal → delete → back) |
| Item Read | `WishlistItemViewConfig(itemId, wishlistId)` | `WishlistItemViewModel` | Read-only item view showing priority and other fields; auto-pops on resume when item no longer exists |
| Item Edit | `WishlistItemEditViewConfig(itemId?, wishlistId)` | `WishlistItemEditViewModel` | Create (null id) or edit item; priority selector (4 options: Low/Medium/High/Custom) plus custom weight text field (shown when Custom selected); back triggers discard modal if dirty; edit mode shows danger Delete (confirm modal → delete → back) |

## Models

| Type | Description |
|------|-------------|
| `WishlistsModel` | Single interface consumed by all ViewModels; wraps `WishlistsFeature`, `WishlistsItemsFeature`, `ClientAuthFeature`; method: `suspend fun getUserWishlists(userId: UserId): List<RegisteredWishlist>` — fetches any user's wishlists (public read) |
| `UserWishlistsViewConfig` | `data class(userId: UserId)` — config for the grid-view screen; specifies which user's wishlists to display |
| `UserWishlistsViewModel` | ViewModel for grid-view screen; exposes `wishlistsState: StateFlow<List<RegisteredWishlist>>`, `onWishlistSelected(wishlist)`, `onBack()` |
| `UserWishlistsViewInteractor` | Interactor interface with `suspend fun onWishlistSelected(node, wishlist)` (impl: push WishlistViewConfig) and `suspend fun onBack(node)` (impl: pop) |

## Architecture Notes

- All views (`WishlistsListView`, `UserWishlistsView`, `WishlistView`, `WishlistItemView`, `WishlistEditView`, `WishlistItemEditView`) use the shared `ScreenTitle` / `BackButton` / `ListRow` components from `features/common/client` (`ui.components`) for the screen title, back button, and list rows instead of hand-rolling them per platform.
- All ViewModels push/pop directly via `node.chain`: `ViewInteractor` pattern used only for grid-view screen (UserWishlistsViewInteractor) where interactor bridges navigation between configs. Intra-feature views (List/Detail/Edit) push/pop directly in ViewModel.
- `WishlistViewModel.isOwnerState` = `wishlist.userId == currentUserId`; derived via `combine`.
- `WishlistsModel.getWishlist(id)` resolves by calling `getMyWishlists().find { it.id == id }` — no dedicated server endpoint.
- `WishlistItemEditViewModel` loads item by calling `getWishlistItems(wishlistId).find { it.id == itemId }` — requires wishlistId in config.
- Price stored as double string in `priceState`; parsed to `Amount(double)` on save; blank = `null` price.
- **Priority display and editing:**
  - `WishlistItemView` reads `RegisteredWishlistItem.priority` and displays the priority label (e.g., "Low", "Medium", "High", or "Custom (weight)" for custom priorities).
  - `WishlistItemEditViewModel` exposes `priorityState: StateFlow<Priority>`, `onPrioritySelected(Priority)`, `onCustomWeightChanged(String)`. Edit view renders a 4-option selector (Low/Medium/High/Custom) plus a custom weight text field shown only when Custom is selected.
  - `WishlistStrings` includes: `priorityLabel`, `prioritySmall`, `priorityMedium`, `priorityHigh`, `priorityCustom`, `priorityCustomWeightLabel`.
  - `Priority.labelResource()` helper returns the localized string key for display.
- **User wishlists grid view:**
  - `UserWishlistsView*` (JS/JVM/Android) renders the same user's wishlists as a card grid. JS/JVM layout uses responsive Bootstrap grid; Android uses 2 cards per row.
  - Tapping a card calls `interactor.onWishlistSelected(node, wishlist)`, which is implemented in `ClientPlugin` to push `WishlistViewConfig(wishlistId)`.
  - "Grid view" button on `WishlistsListView` is shown only when `WishlistsListViewModel.targetUserId != null` (concrete user). Calls `onShowGrid()` which pushes `UserWishlistsViewConfig(userId)`.
  - `WishlistsListViewInteractor` gained `onShowUserWishlists(node, userId)` method (impl in ClientPlugin pushes `UserWishlistsViewConfig(userId)`).
  - Grid view is reachable from list view when viewing a concrete user's wishlists; back from grid view pops back to the list view.
- JS modal rendered via Bootstrap classes (`modal d-block` + `modal-backdrop`); JVM/Android use `AlertDialog`.
- **Deletion** (owner only; server enforces ownership, returning `403`/`false` for non-owners):
  - `WishlistEditViewModel` / `WishlistItemEditViewModel` expose `canDelete` (= `!isCreating`), `showDeleteDialogState`, and `onDelete()` / `onConfirmDelete()` / `onCancelDelete()`. `onConfirmDelete()` calls `model.deleteWishlist(id)` / `model.deleteWishlistItem(id)` then delegates to the same `interactor.onNavigateBack(node)` as a plain back. The edit views render a danger Delete button (shown only when `canDelete`) plus a dedicated delete confirmation modal.
  - `WishlistViewModel` / `WishlistItemViewModel` reload on every resume and call `interactor.onBack(node)` automatically when the loaded wishlist / item is `null` (deleted here or elsewhere), so a reopened read screen for a removed entity navigates back.
- JS view packages: `dev.inmo.wishlist.features.ui.wishlist.ui.js`
- JVM view packages: `dev.inmo.wishlist.features.ui.wishlist.ui.jvm`
- Android view packages: `dev.inmo.wishlist.features.ui.wishlist.ui`
- `WishlistsListViewConfig` is the JS application root: always inserted as base config by `UrlParametersNavigationConfigsRepo` decoder in `ClientJSPlugin` (not injected via `ClientPlugin`).
- **ViewModel reload patterns:**
  - `WishlistViewModel`, `WishlistItemViewModel`, `WishlistsListViewModel`: reload on every resume — `merge(flowOf(Unit), node.onResumeFlow).subscribeLoggingDropExceptions(scope)`. `WishlistViewModel`/`WishlistItemViewModel` also auto-`onBack` when the entity is gone after reload.
  - `WishlistEditViewModel`: load on first resume only — same pattern with `.takeWhile { inited == false }`. (`WishlistItemEditViewModel` likewise loads once.)
- `WishlistsListViewModel.loadWishlists()` is `private suspend fun` that branches on `node.config.userId`: calls `model.getUserWishlists(userId)` if non-null, otherwise calls `model.getMyWishlists()`; not callable externally.
- **JS URL navigation scheme** (encoded by `UrlParametersNavigationConfigsRepo` in `ClientJSPlugin`):
  - `?wishlist=<id>` → `WishlistViewConfig(id)`
  - `?wishlist=<id>&edit=true` → `WishlistViewConfig(id)` + `WishlistEditViewConfig(id)`
  - `?wishlist=<id>&wishlist_item=<id>&edit=true` → `WishlistViewConfig(id)` + `WishlistItemEditViewConfig(itemId, wishlistId)`
  - `?edit=true` (no wishlist) → `WishlistEditViewConfig(null)` (create mode)
  - `WishlistsListViewConfig` always present as root regardless of URL state.
