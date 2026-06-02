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
| Wishlists List | `WishlistsListViewConfig(userId: UserId? = null)` | `WishlistsListViewModel` | Home screen; when `userId` non-null, lists that user's wishlists; when null, lists caller's wishlists; when `userId` non-null, shows "All items" button opening the all-items screen. "New Wishlist" button shown only when the caller owns the displayed list (`isOwnerState`). Title shows `${userName}'s Wishlists` (`userWishlistsTitleFormat`), falling back to the generic `wishlistsTitle` when no user is resolved (anonymous own list) |
| All Items | `UserWishlistsViewConfig(userId: UserId)` | `UserWishlistsViewModel` | Items across all of a user's wishlists, grouped by wishlist with a separator header (the wishlist title) above each group (shared `ListRow`, aggregated client-side; wishlists with no items omitted); tapping a row opens that item's detail; reachable via "All items" button on `WishlistsListView` when viewing a concrete user. The screen supports sorting items by Cost, Priority, or Title; when a custom sort is active, wishlist group headers are hidden and items appear as a single flat list with each item title followed by its wishlist title in brackets. Title shows `${userName}'s wishes` (`userWishesTitleFormat`), falling back to `allItemsTitle`. Also reachable directly from the users list (selecting a user pushes `UserWishlistsViewConfig`) |
| Wishlist Detail | `WishlistViewConfig(wishlistId)` | `WishlistViewModel` | Shows items; edit controls visible only to owner; auto-pops on resume when wishlist no longer exists |
| Wishlist Edit | `WishlistEditViewConfig(wishlistId?)` | `WishlistEditViewModel` | Create (null id) or edit; back triggers discard modal if dirty; edit mode shows danger Delete (confirm modal → delete → back) |
| Item Read | `WishlistItemViewConfig(itemId, wishlistId)` | `WishlistItemViewModel` | Read-only item view showing priority and other fields; auto-pops on resume when item no longer exists |
| Item Edit | `WishlistItemEditViewConfig(itemId?, wishlistId)` | `WishlistItemEditViewModel` | Create (null id) or edit item; priority selector (4 options: Low/Medium/High/Custom) plus custom weight text field (shown when Custom selected); back triggers discard modal if dirty; edit mode shows danger Delete (confirm modal → delete → back) |

## Models

| Type | Description |
|------|-------------|
| `WishlistsModel` | Single interface consumed by all ViewModels; wraps `WishlistsFeature`, `WishlistsItemsFeature`, `ClientAuthFeature`, `FilesClientService`, `UsersFeature`; methods: `suspend fun getUserWishlists(userId: UserId): List<RegisteredWishlist>` (fetches any user's wishlists, public read), `suspend fun getUserName(userId: UserId): String?` (resolves a user's display name via `UsersFeature.getAll()`, null when unknown), `suspend fun uploadImage(file: MPPFile): FileId?` (uploads and returns file id, null on failure), `fun imageUrl(id: FileId): String` (constructs image URL), `suspend fun loadImageBytes(id: FileId): ByteArray?` (downloads raw bytes for platform-specific rendering) |
| `UserWishlistsViewConfig` | `data class(userId: UserId)` — config for the all-items screen; specifies whose items to aggregate |
| `UserWishlistsViewModel` | ViewModel for the all-items screen; loads each wishlist with its items via `getUserWishlists(userId).map { UserWishlistsSection(it, getWishlistItems(it.id)) }.filter { it.items.isNotEmpty() }`; exposes `sectionsState: StateFlow<List<UserWishlistsSection>>` (each `UserWishlistsSection` = a `RegisteredWishlist` + its `List<RegisteredWishlistItem>`), `userNameState: StateFlow<String?>` (target user's name for the title), `sortModeState: StateFlow<WishlistSortMode>` (current sort selection), `sortedItemsState: StateFlow<List<SortedWishlistItem>>` (empty when mode is `None`; derived from `combine(sectionsState, sortModeState)`), `onItemSelected(item)`, `onSortModeSelected(mode)`, `onBack()` |
| `WishlistSortMode` | Enum with values `None`, `Cost`, `Priority`, `Title`; determines how items are sorted on the all-items screen. |
| `SortedWishlistItem` | Data class containing an item and its wishlist title; used when a custom sort is active to render each item with its origin wishlist shown in brackets. |
| `UserWishlistsViewInteractor` | Interactor interface with `suspend fun onItemSelected(node, itemId, wishlistId)` (impl: push `WishlistItemViewConfig`), `suspend fun onWishlistSelected(node, wishlistId)` (impl: push `WishlistViewConfig`), `suspend fun onBack(node)` (impl: pop), `suspend fun onOpenProfile(node, userId)` (impl: push `UserViewConfig`) |

## Architecture Notes

- All views (`WishlistsListView`, `UserWishlistsView`, `WishlistView`, `WishlistItemView`, `WishlistEditView`, `WishlistItemEditView`) use the shared `ScreenTitle` / `BackButton` / `ListRow` components from `features/common/client` (`ui.components`) for the screen title, back button, and list rows instead of hand-rolling them per platform.
- All ViewModels push/pop directly via `node.chain`: `ViewInteractor` pattern used only for the all-items screen (UserWishlistsViewInteractor) where interactor bridges navigation between configs. Intra-feature views (List/Detail/Edit) push/pop directly in ViewModel.
- `WishlistViewModel.isOwnerState` = `wishlist.userId == currentUserId`; derived via `combine`.
- `WishlistsModel.getWishlist(id)` resolves by calling `getMyWishlists().find { it.id == id }` — no dedicated server endpoint.
- `WishlistItemEditViewModel` loads item by calling `getWishlistItems(wishlistId).find { it.id == itemId }` — requires wishlistId in config.
- Price stored as double string in `priceState`; parsed to `Amount(double)` on save; blank = `null` price.
- **Priority display and editing:**
  - Priority is rendered through a shared per-platform `PriorityBadge(priority)` composable (`features/ui/wishlist/src/{jsMain,jvmMain,androidMain}/kotlin/ui/PriorityBadge.kt`): JS = Bootstrap `badge rounded-pill`; JVM/Android = Material3 `Badge` (secondary-container colours). It shows the localized `labelResource()` label plus `weightSuffix()` (the `" (<weight>)"` for `Priority.Custom`).
  - Item rows in `WishlistView` and `UserWishlistsView` (all platforms) show the `PriorityBadge` inline in the same row as the item title; the description stays below as supporting text.
  - `WishlistItemView` (detail) shows the same `PriorityBadge` under its "Priority" section header.
  - `WishlistItemEditViewModel` exposes `priorityState: StateFlow<Priority>`, `onPrioritySelected(Priority)`, `onCustomWeightChanged(String)`. Edit view renders a 4-option selector (Low/Medium/High/Custom) plus a custom weight text field shown only when Custom is selected.
  - `WishlistStrings` includes: `priorityLabel`, `prioritySmall`, `priorityMedium`, `priorityHigh`, `priorityCustom`, `priorityCustomWeightLabel`.
  - `Priority.labelResource()` helper returns the localized string key for display; `Priority.weightSuffix()` returns `" (<weight>)"` for `Priority.Custom` and an empty string for presets. Both are consumed by `PriorityBadge`.
- **Image support:**
  - `WishlistItemEditViewModel` exposes `imageIdsState: StateFlow<List<FileId>>`, `uploadingImageState: StateFlow<Boolean>`, `onAddImage(file: MPPFile)`, `onRemoveImage(index)`, `imageUrl(id: FileId): String`, `loadImageBytes(id: FileId): ByteArray?`. On `onSave()`, `NewWishlistItem` is built with current `imageIds`.
  - `WishlistItemViewModel` exposes `imageUrl(id)` and `loadImageBytes(id)` for read-only view to display item images.
  - Image picking is platform-specific: `expect suspend fun pickImageFile(): MPPFile?` in `commonMain/utils/PickImageFile.kt`; JS uses hidden `<input type="file">` element; JVM uses Swing `JFileChooser`; Android uses `ActivityResultContracts.GetContent` through `AndroidImagePicker`, which `MainActivity` registers in `onCreate()`.
  - Image preview: JS renders `<img src=imageUrl>` directly. JVM/Android use a `RemoteImage` composable that calls `loadImageBytes` and decodes via platform codec (Skia on desktop, BitmapFactory on Android). No third-party image-loader dependency.
  - New `WishlistStrings` keys: `imagesLabel`, `addImageButton`, `removeImageButton`, `uploadingImage`, `noImages` (English + Russian translations).
- **All-items view:**
  - `UserWishlistsView*` (JS/JVM/Android) renders the target user's items grouped by wishlist: a separator header showing the wishlist title precedes each group of items, each item built from the shared `ListRow` component (title + optional price + optional description), same item rendering as `WishlistView`. JS renders an `H6` header above a Bootstrap `list-group` per section; JVM/Android use a single `LazyColumn` with a header `item` (Material `Divider` / Material3 `HorizontalDivider`, keyed `header-<wishlistId>`) before each section's items. Sections are built client-side in `UserWishlistsViewModel` as `UserWishlistsSection`s; wishlists with no items are omitted.
  - **Item sorting:**
    - Sort selector renders buttons (one per mode: Grouped/Cost/Priority/Title); the active mode is highlighted.
    - When sort mode is `None` (Grouped), the list displays with wishlist group headers and section separators as default (per above).
    - When sort mode is `Cost`, `Priority`, or `Title`, the list flattens to a single plain list with no wishlist headers. Each row renders a shared private `ItemRow(item, wishlistTitle)` composable displaying the item title followed by the wishlist title in brackets, e.g., `Item title (Wishlist title)`.
    - Sort orders: `Cost` = ascending by `approximatePrice` (items without price appear last); `Priority` = descending by `priority.weight` (highest weight first); `Title` = case-insensitive ascending alphabetical.
    - `UserWishlistsViewModel.sortedItemsState` is derived by `combine(sectionsState, sortModeState)` then `.stateIn(scope, Eagerly, emptyList())` — returns an empty list when mode is `None` (in which case the view renders `sectionsState` instead).
    - New `WishlistStrings` keys: `sortLabel`, `sortNone`, `sortCost`, `sortPriority`, `sortTitle` (English + Russian). Helper `WishlistSortMode.labelResource()` returns the localized string key for display.
  - Tapping a row calls `interactor.onItemSelected(node, itemId, wishlistId)`, implemented in `ClientPlugin` to push `WishlistItemViewConfig(itemId, wishlistId)` (the item's `wishlistId` comes from `RegisteredWishlistItem.wishlistId`).
  - In the grouped (`None`) presentation, each wishlist group header shows the wishlist title next to an "Open" button (`WishlistStrings.openWishlistButton`); the title text itself is not clickable. The button calls `viewModel.onWishlistSelected(wishlist)` → `interactor.onWishlistSelected(node, wishlistId)`, implemented in `ClientPlugin` to push `WishlistViewConfig(wishlistId)` (opens the wishlist detail screen). The header is a space-between row on every platform (JS Bootstrap flex row; JVM/Android `Row` with the title weighted and a trailing `Button`).
  - "All items" button (`WishlistStrings.allItemsButton`) on `WishlistsListView` is shown only when `WishlistsListViewModel.targetUserId != null` (concrete user). Calls `onShowUserWishlists()` which pushes `UserWishlistsViewConfig(userId)`.
  - `WishlistsListViewInteractor` has `onShowUserWishlists(node, userId)` (impl in ClientPlugin pushes `UserWishlistsViewConfig(userId)`).
  - Reachable from list view when viewing a concrete user; back pops to the list view.
- **Profile button:** `WishlistsListView` shows a "Profile" button that opens the profile of the user whose wishlists are displayed. `WishlistsListViewModel.profileUserIdState` = `targetUserId ?: model.getCurrentUserId()` (the browsed owner, or the caller for the own list); the button is hidden when that resolves to `null` (anonymous viewing own list). `onShowProfile()` delegates to `WishlistsListViewInteractor.onShowUser(node, userId)`, implemented in `ClientPlugin` to push `UserViewConfig(userId)` from `features/ui/users`.
- **Personalized titles:** both list screens title themselves after the displayed user. `WishlistsListViewModel.userNameState` and `UserWishlistsViewModel.userNameState` are resolved via `WishlistsModel.getUserName(userId)` (backed by `UsersFeature.getAll().find { it.id == userId }?.username?.string`). Views build the title from a `{name}` placeholder string (`userWishlistsTitleFormat` → "{name}'s Wishlists"; `userWishesTitleFormat` → "{name}'s wishes") via `.translation().replace("{name}", name)`, falling back to the generic `wishlistsTitle` / `allItemsTitle` when the name is `null`. `features/ui/wishlist` gains an `api` dependency on `features/users/client` for `UsersFeature`.
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
- `WishlistsListViewModel.loadWishlists()` is `private suspend fun` that branches on `node.config.userId`: calls `model.getUserWishlists(userId)` if non-null, otherwise calls `model.getMyWishlists()`; not callable externally. It also fetches `getCurrentUserId()` once and sets `isOwnerState = currentUserId != null && (targetUserId == null || targetUserId == currentUserId)` — the views gate the "New Wishlist" button on this flag so non-owners (and anonymous viewers) never see it.
- **JS URL navigation scheme** (encoded by `UrlParametersNavigationConfigsRepo` in `ClientJSPlugin`):
  - `?wishlist=<id>` → `WishlistViewConfig(id)`
  - `?wishlist=<id>&edit=true` → `WishlistViewConfig(id)` + `WishlistEditViewConfig(id)`
  - `?wishlist=<id>&wishlist_item=<id>&edit=true` → `WishlistViewConfig(id)` + `WishlistItemEditViewConfig(itemId, wishlistId)`
  - `?edit=true` (no wishlist) → `WishlistEditViewConfig(null)` (create mode)
  - `WishlistsListViewConfig` always present as root regardless of URL state.
