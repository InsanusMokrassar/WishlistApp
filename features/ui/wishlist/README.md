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
| Wishlists List | `WishlistsListViewConfig` | `WishlistsListViewModel` | Home screen; lists caller's wishlists; no back button |
| Wishlist Detail | `WishlistViewConfig(wishlistId)` | `WishlistViewModel` | Shows items; edit controls visible only to owner |
| Wishlist Edit | `WishlistEditViewConfig(wishlistId?)` | `WishlistEditViewModel` | Create (null id) or edit; back triggers discard modal if dirty |
| Item Edit | `WishlistItemEditViewConfig(itemId?, wishlistId)` | `WishlistItemEditViewModel` | Create (null id) or edit item; back triggers discard modal if dirty |

## Models

| Type | Description |
|------|-------------|
| `WishlistsModel` | Single interface consumed by all four ViewModels; wraps `WishlistsFeature`, `WishlistsItemsFeature`, `ClientAuthFeature` |

## Architecture Notes

- All four ViewModels push/pop directly via `node.chain`: no `ViewInteractor` needed (all navigation is intra-feature).
- `WishlistViewModel.isOwnerState` = `wishlist.userId == currentUserId`; derived via `combine`.
- `WishlistsModel.getWishlist(id)` resolves by calling `getMyWishlists().find { it.id == id }` — no dedicated server endpoint.
- `WishlistItemEditViewModel` loads item by calling `getWishlistItems(wishlistId).find { it.id == itemId }` — requires wishlistId in config.
- Price stored as double string in `priceState`; parsed to `Amount(double)` on save; blank = `null` price.
- JS modal rendered via Bootstrap classes (`modal d-block` + `modal-backdrop`); JVM/Android use `AlertDialog`.
- JS view packages: `dev.inmo.wishlist.features.ui.wishlist.ui.js`
- JVM view packages: `dev.inmo.wishlist.features.ui.wishlist.ui.jvm`
- Android view packages: `dev.inmo.wishlist.features.ui.wishlist.ui`
- `WishlistsListViewConfig` is the JS application root: always inserted as base config by `UrlParametersNavigationConfigsRepo` decoder in `ClientJSPlugin` (not injected via `ClientPlugin`).
- **ViewModel reload patterns:**
  - `WishlistViewModel`, `WishlistsListViewModel`: reload on every resume — `merge(flowOf(Unit), node.onResumeFlow).subscribeLoggingDropExceptions(scope)`.
  - `WishlistEditViewModel`, `WishlistItemEditViewModel`: load on first resume only — same pattern with `.takeWhile { inited == false }`.
- `WishlistsListViewModel.loadWishlists()` is `private suspend fun`; not callable externally.
- **JS URL navigation scheme** (encoded by `UrlParametersNavigationConfigsRepo` in `ClientJSPlugin`):
  - `?wishlist=<id>` → `WishlistViewConfig(id)`
  - `?wishlist=<id>&edit=true` → `WishlistViewConfig(id)` + `WishlistEditViewConfig(id)`
  - `?wishlist=<id>&wishlist_item=<id>&edit=true` → `WishlistViewConfig(id)` + `WishlistItemEditViewConfig(itemId, wishlistId)`
  - `?edit=true` (no wishlist) → `WishlistEditViewConfig(null)` (create mode)
  - `WishlistsListViewConfig` always present as root regardless of URL state.
