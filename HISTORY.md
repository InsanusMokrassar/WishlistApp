# WishlistApp — Agent History

## Format

Each entry: date | prompt_summary | actions | outcome | notes

---

## 2026-05-26

### Session: remote-control init

prompt=remote-control session start; action=read AGENTS.md+SHORTCUTS.md+ALL.md+local.ALL.md+AST_INDEX.md; outcome=context loaded, HISTORY.md created; notes=ast-index available at /home/linuxbrew/.linuxbrew/bin/ast-index

### Session: WishlistItemView + public read routes

prompt="WishlistItem view (like WishlistItemEdit but read-only) + wishlists/wishlist/items without authentication"

#### Changes made

**Server (make read routes public):**
- `Constants.kt` — added `wishlistGetByIdPathPart = "getById"`
- `WishlistService.kt` — added `getById(id): RegisteredWishlist?`
- `WishlistRoutingsConfigurator.kt` — split: `getByUserId/{userId}` + `getById/{id}` now public (outside `authenticate {}`); `getMy`/`create`/`update`/`delete` still require bearer
- `WishlistItemRoutingsConfigurator.kt` — split: `getByWishlistId/{wishlistId}` now public; mutations still require bearer

**Client (new public endpoint):**
- `WishlistsFeature.kt` — added `getById(id: WishlistId): RegisteredWishlist?`
- `KtorWishlistFeature.kt` — implemented `getById` via `GET /wishlist/getById/{id}`

**UI (WishlistItemView):**
- Created `WishlistItemViewConfig.kt` — `(wishlistItemId, wishlistId) : ViewConfig`
- Created `WishlistItemViewModel.kt` — loads item by id, exposes `itemState`/`loadingState`/`onBack()`
- Created `jsMain/ui/WishlistItemView.kt` — read-only Bootstrap view (title, description, price, links)
- Created `jvmMain/ui/WishlistItemView.kt` — read-only Desktop Compose view
- Created `androidMain/ui/WishlistItemView.kt` — read-only Material3 view
- `WishlistViewModel.kt` — added `onViewItem(itemId)` → pushes `WishlistItemViewConfig`
- `WishlistView.kt` (JS/JVM/Android) — items always clickable: owner → edit, non-owner → view
- `Plugin.kt` (ui/wishlist) — added serializer + factory for `WishlistItemViewConfig`/`WishlistItemViewModel`; fixed `getWishlist(id)` to use public `getById` instead of `getMyWishlists().find{}`
- `JSPlugin.kt` / `JVMPlugin.kt` / `AndroidPlugin.kt` — added `NavigationNodeFactory` for `WishlistItemView`
- `WishlistStrings.kt` — added `viewItemTitle`, `noPrice`, `noLinks`

outcome=all files created/modified; ast-index rebuilt (259 files, 21 modules)

### Session: WishlistItemView — view-first navigation

prompt="same logic as wishlist — open view first, then edit via button"

Changes:
- `WishlistItemViewModel.kt` — added `isOwnerState` (loads current user + parent wishlist, compares userId); added `onEditItem()` → pushes `WishlistItemEditViewConfig`
- `WishlistItemView.kt` (JS/JVM/Android) — added Edit button visible only when `isOwner`
- `WishlistViewModel.kt` — removed `onEditItem(itemId)` (now dead); `onViewItem` is the single item click handler for all users
- `WishlistView.kt` (JS/JVM/Android) — item click always calls `onViewItem` (no owner branch)

outcome=navigation flow: WishlistView → WishlistItemView (always) → WishlistItemEditView (owner only via Edit button)

### Session: Extract navigation into interactors

prompt="extract all actions related to work with nodes in wishlists into interactors"

Created 5 interactor interfaces in `features/ui/wishlist/src/commonMain/kotlin/ui/`:
- `WishlistsListViewInteractor` — onWishlistSelected, onCreateWishlist
- `WishlistViewInteractor` — onBack, onEditWishlist, onViewItem, onAddItem
- `WishlistEditViewInteractor` — onNavigateBack, onSaved
- `WishlistItemEditViewInteractor` — onNavigateBack, onSaved
- `WishlistItemViewInteractor` — onBack, onEditItem

Updated 5 ViewModels — added `interactor` constructor param, all node.chain.* calls replaced with interactor delegation:
- `WishlistsListViewModel`, `WishlistViewModel`, `WishlistEditViewModel`, `WishlistItemEditViewModel`, `WishlistItemViewModel`

Updated `Plugin.kt` — factory `(it.get(), get(), get())` for all 5 ViewModels.

Implemented all 5 interactors in `client/src/commonMain/kotlin/ClientPlugin.kt` (simple chain.push/pop delegates).

outcome=ast-index rebuilt (264 files)

