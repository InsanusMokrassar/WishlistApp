# Feature: AdminPanel UI

## Operator Notes

<!-- Human operator writes here. Agents MUST read and respect before making any changes. Agents MUST NOT modify this section. -->

## Overview

Admin panel UI scenario. Full CRUD for users, wishlists, and wishlist items via a `List → View → Edit` canvas. Accessible only to authenticated admins.

## Routes

No server routes — UI-only feature. Consumes `features/admin/client` via `AdminFeature`.

## Models

| Type | Description |
|---|---|
| `AdminPanelViewConfig` | Dashboard entry point — two navigation buttons |
| `AdminUsersListViewConfig` | List of all users |
| `AdminUserViewConfig(userId)` | User detail with inline wishlists list |
| `AdminUserEditViewConfig(userId?)` | Create/edit user form; `null` = create mode |
| `AdminWishlistsListViewConfig` | List of all wishlists |
| `AdminWishlistViewConfig(wishlistId)` | Wishlist detail with inline items list |
| `AdminWishlistEditViewConfig(wishlistId?, preselectedUserId?)` | Create/edit wishlist; owner dropdown; `null` wishlistId = create mode |
| `AdminWishlistItemEditViewConfig(itemId?, wishlistId)` | Create/edit wishlist item |

## Architecture Notes

- Single `AdminPanelModel` interface consumed by all 8 ViewModels; implemented in `Plugin.kt` as anonymous object wrapping `AdminFeature`.
- All 8 `*ViewInteractor` interfaces are implemented in `client/src/commonMain/kotlin/ClientPlugin.kt` as stateless anonymous objects (push/pop on `node.chain`).
- `AdminPanelViewConfig` is the root screen pushed by `InjectNavigationNode` inside `ClientPlugin.startPlugin`.
- Android `AndroidPlugin` registers all 8 `NavigationNodeFactory` entries (same as JS and JVM plugins).
- JS views use Bootstrap CSS classes. JVM and Android use Compose Desktop / Material3.
- Wishlist create screen (`AdminWishlistEditViewConfig(null, preselectedUserId)`) pre-selects the owner dropdown when `preselectedUserId` is non-null — used when "Add Wishlist" is tapped from a user detail screen.
