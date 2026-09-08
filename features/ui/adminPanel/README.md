# Feature: AdminPanel UI

## Operator Notes

<!-- Human operator writes here. Agents MUST read and respect before making any changes. Agents MUST NOT modify this section. -->

## Overview

Admin panel UI scenario. Full CRUD for users, wishlists, and wishlist items via a `List → View → Edit` canvas. The dashboard loads a live registered-user list with loading, empty, and retryable error states, and provides a root-only SMTP test form only when delivery is enabled. Accessible only to authenticated admins.

## Routes

No server routes — UI-only feature. Consumes `features/admin/client` via `AdminFeature` and `features/email/client` via `EmailFeature`.

## Models

| Type | Description |
|---|---|
| `AdminPanelViewConfig` | Dashboard entry point — live registered-user list plus navigation actions |
| `AdminUsersListViewConfig` | List of all users |
| `AdminUserViewConfig(userId)` | User detail with inline wishlists list |
| `AdminUserEditViewConfig(userId?)` | Create/edit user form; `null` = create mode |
| `AdminWishlistsListViewConfig` | List of all wishlists |
| `AdminWishlistViewConfig(wishlistId)` | Wishlist detail with inline items list |
| `AdminWishlistEditViewConfig(wishlistId?, preselectedUserId?)` | Create/edit wishlist; owner dropdown; `null` wishlistId = create mode |
| `AdminWishlistItemEditViewConfig(itemId?, wishlistId)` | Create/edit wishlist item |
| `AdminPanelModel.getAllUsers(): List<AdminUser>` / `getUserById(): AdminUser?` / `createUser(): AdminUser?` | Delegate to `AdminFeature.usersManagement`; return the `admin.common` feature model, not `RegisteredUser` |
| `AdminPanelModel.getAllWishlists(): List<AdminWishlist>` / `getWishlistsByUser()` / `getWishlistById()` / `createWishlist()` | Delegate to `AdminFeature.wishlists`; return `AdminWishlist`, not `RegisteredWishlist` |
| `AdminPanelModel.getItemsByWishlist(): List<AdminWishlistItem>` / `createWishlistItem()` | Delegate to `AdminFeature.wishlistItems`; return `AdminWishlistItem`, not `RegisteredWishlistItem` |
| `AdminPanelModel.sendTestEmail(recipient: Email): Boolean` | Delegates to `EmailFeature.sendTestEmail` |
| `AdminPanelModel.isEmailFeatureEnabled(): Boolean` | Delegates to `EmailFeature.isFeatureEnabled` |
| `AdminPanelModel.updateUsername(id, username)` | Delegates to the safe username-only admin endpoint, preserving email approval state |

## Architecture Notes

- All 8 views use the shared `ScreenTitle` / `BackButton` / `ListRow` components from `features/common/client` (`ui.components`) for titles, back buttons, and list rows. List rows with badges/prices use the `ListRow(onSelect, trailing) { content }` custom-primary overload; item edit/delete buttons are passed via the `trailing` slot.
- Single `AdminPanelModel` interface consumed by all 8 ViewModels; implemented in `Plugin.kt` as anonymous object wrapping `AdminFeature`.
- All 8 `*ViewInteractor` interfaces are implemented in `client/src/commonMain/kotlin/ClientPlugin.kt` as stateless anonymous objects (push/pop on `node.chain`).
- `AdminPanelViewConfig` is reachable from the web client via the sidebar's root-only Admin item
  (`features/ui/sidebar`, issue #66) — it is not the navigation root; the actual root pushed by
  `InjectNavigationNode` in `ClientPlugin.startPlugin` is `mainScaffoldConfig`
  (`ScaffoldViewConfig`).
- Android `AndroidPlugin` registers all 8 `NavigationNodeFactory` entries (same as JS and JVM plugins).
- JS views use Bootstrap CSS classes. JVM and Android use Compose Desktop / Material3.
- Wishlist create screen (`AdminWishlistEditViewConfig(null, preselectedUserId)`) pre-selects the owner dropdown when `preselectedUserId` is non-null — used when "Add Wishlist" is tapped from a user detail screen.
- **Dashboard users:** `AdminPanelViewModel` loads `AdminPanelModel.getAllUsers()` on initial display, resume, and authenticated-session restoration. It clears stale data on logout, ignores older in-flight responses with a request version, and exposes separate loading, empty, and retryable failure states. Selecting a row uses the dashboard interactor to push `AdminUserViewConfig(userId)` directly.
- **Email section (added in issue #44):** Dashboard (`AdminPanelView`) probes `EmailFeature.isFeatureEnabled()` and hides the complete SMTP test form when delivery is unavailable. When enabled, every platform labels the field **SMTP test recipient**, explains that the entered address receives only the test and does not alter any account email, validates with `Email.parse(...)`, and disables duplicate sends while the request is active. `AdminPanelViewModel.sendTestEmailState: StateFlow<Boolean?>` holds the result (`null` = not yet attempted). Real authorization is server-side (`requireRoot` on `POST /api/email/sendTest`). Requires `api project(":wishlist.features.email.client")` in `build.gradle`.
- **Username-only admin edits:** `AdminUserEditViewModel` calls `AdminPanelModel.updateUsername` for existing users rather than building a partial `NewUser`; this preserves the server-owned email and approval fields.
- **Note:** The "JS views use Bootstrap CSS classes" bullet in the original notes is stale — JS views use Calm Studio components only.
- **Feature Interface Return Model Rule:** `AdminPanelModel` and its ViewModels' state flows now hold `AdminUser`/`AdminWishlist`/`AdminWishlistItem` (from `admin.common.models`) instead of the `users`/`wishlist` features' `RegisteredUser`/`RegisteredWishlist`/`RegisteredWishlistItem` persistence entities — `Plugin.kt`'s anonymous `AdminPanelModel` impl and `AdminUsersListViewModel`/`AdminUserViewModel`/`AdminWishlistsListViewModel`/`AdminWishlistViewModel`/`AdminWishlistEditViewModel`'s state flows were retyped accordingly. Field access (`.username`, `.title`, `.email`, etc.) is unchanged since the new models mirror the old ones' display fields.
- **Logout exits open admin editors (issue #53):** `AdminUserEditViewModel`, `AdminWishlistEditViewModel`, and `AdminWishlistItemEditViewModel` route logout-exit through `AdminPanelModel.userAuthorisedState` and subscribe `model.userAuthorisedState.subscribeOnLoggedOut(scope) { interactor.onNavigateBack(node) }` (helper in `features/common/client` `utils/`) in `init`. On logout each admin editor pops to its read/list view, **bypassing the dirty-changes confirm dialog**. The model exposes `userAuthorisedState: StateFlow<Boolean>` so edit ViewModels exit via the model layer (MVVM boundary) instead of importing auth storage directly. Reuses existing `interactor.onNavigateBack` surface — no new interactor methods.
