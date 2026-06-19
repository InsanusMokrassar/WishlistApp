# STEP 1 — Calm Studio redesign, phase 3/5: core content screens (web/JS only)

## Scope
Rebuilt the web client (`jsMain`) core screens to render Calm Studio markup, reusing the existing
ViewModels / data layer (no new data faked). Android/Desktop (Material 3) untouched. Class names mirror
the design skill `ui_kits/calm-studio` reference so the phase-1 shell CSS (`client/.../css/calm-studio.css`)
styles them.

## Files changed
New:
- `features/common/client/.../ui/components/CalmIcons.kt` — shared Lucide-style icon set (`CalmIcon`,
  `CalmIcons`) + `tintClass(seed)` helper, so wishlist/users views share one icon set without depending
  on the sidebar module.
- `features/ui/wishlist/.../ui/WishlistItemRow.kt` — `.row` list-item composable.

Rebuilt (Bootstrap → Calm Studio classes):
- wishlist jsMain: `WishlistsListView` (My Lists / profile `.listgrid`), `WishlistView` (list detail:
  Share + owner Edit/Add item / visitor Copy, `.toolbar` sort+grid/list, `.grid`/`.rows`),
  `WishlistItemView` (`.detail` gallery/pill/price/links + owner Edit / visitor Copy + booking providers),
  `WishlistItemEditView` (`.form`/`.fieldset`/`.priopts`, delete + discard `.scrim` modals),
  `WishlistEditView` (list rename/delete form), `WishlistItemCopyView` (`.listgrid` target picker),
  `UserWishlistsView` (all-items grid/rows + grouped section heads), `WishlistItemCard`, `PriorityBadge`
  (`.pill`), `ViewModeSelector` (`.seg`), `WishlistSortSelector`/`CurrencySelector` (`.select`),
  `WishlistSelectorsRow` (`.toolbar`), `PriceUnitsSelector` (`.fieldset`), `CreateWishlistButton`.
- users jsMain: `UsersListView` (Discover `.people`/`.person`), `UserView` (profile `.pagehead` header).
- `WishlistStrings`: added `shareButton`, `priorityHelp` (EN + RU).
- READMEs: wishlist + users overviews updated.

## Data-binding honesty (no faked data)
The reference shows per-list item/reserved counts, list **visibility** chips, and an All/Available/Reserved
filter on the list detail. The data layer exposes none of these: `Wishlist`/`WishlistItem` have no
visibility flag, reservation is the separate **booking** feature surfaced only via item-detail
`WishlistAdditionalConfigsProvider`s, and `WishlistsListViewModel` does not load item counts. So those
were intentionally omitted (cards show cover tint + title; list detail keeps sort + grid/list only).
"Share" is a view-side clipboard copy of the page URL (no VM/store change).

## Navigation
Per-screen Back buttons dropped on JS — the persistent sidebar + top-bar breadcrumb (phase-2 shell) own
back navigation. ViewModel bindings (state flows + intent methods) are unchanged.

## Verification
`./gradlew :wishlist.features.ui.wishlist:compileKotlinJs :wishlist.features.ui.users:compileKotlinJs` →
BUILD SUCCESSFUL (transitively compiles `features.common.client` JS too).

## Out of scope / follow-ups
- `UserEditView` (profile edit) and `features/ui/adminPanel` JS views still use Bootstrap.
- Reserved screen + global search results are owned by sidebar/topBar features (earlier phases).
- A real list visibility model + reservation-aware list filter would be data-layer work for a later phase.
