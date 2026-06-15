# Feature: UI / Users

## Operator Notes

<!-- Human operator writes here. Agents MUST read and respect before making any changes. Agents MUST NOT modify this section. -->

## Overview

User-facing screens for browsing and managing user profiles. Three screens share one navigation
chain (the scaffold main slot):

- **Users list** — main page content; the global list of registered users from the public
  `UsersFeature.getAll()`. Each row shows the user's avatar (circular thumbnail, placeholder when
  unset) as leading content alongside the username. Selecting a row pushes that user's
  `UserWishlistsViewConfig(userId)` (their all-items view). A **My profile** button (visible only
  when logged in) opens the caller's own profile.
- **User profile view** (`UserViewConfig(userId)`) — public, readable by anyone (anonymous
  included). Shows the username and avatar (when set). Shows an **Edit** button only to the profile
  owner and `root`.
- **User profile edit** (`UserEditViewConfig(userId)`) — reachable by the owner and `root`. A
  non-root owner has **no editable text fields** but may upload an avatar; `root` may edit the
  username, set a new password (with a confirmation field that must match), upload an avatar, and
  **delete** the user. The user id is never editable. User *creation* is not done here (admin panel).

No auth required to view the users list or a profile.

## Routes

None — client-only UI feature. Consumes `features/users/client` (public read), `features/auth/client`
(current caller + root check), `features/admin/client` (root-only username/password/delete) and
`features/files/client` (avatar storage).

## Models

| Type | Description |
|------|-------------|
| `UsersListViewConfig` | Empty `@Serializable class` — main slot root identifier |
| `UserViewConfig` | `data class(userId: UserId)` — public profile detail |
| `UserEditViewConfig` | `data class(userId: UserId)` — profile edit (owner/root) |
| `UsersModel` | Single feature model (renamed from `UsersListModel`). Wraps `UsersFeature.getAll()`, auth "me" `StateFlow<RegisteredUser?>` from `features/auth/client` `Scope.meStateFlow` (`getCurrentUserId`, `isCurrentUserRoot`), admin `AdminFeature.usersManagement` (`updateUsername`, `setPassword`, `deleteUser`), and `FilesClientService` (`getAvatar`, `uploadAvatar`, `imageUrl`, `loadImageBytes`); `getUser(id)` resolves from the public list |
| `UsersListViewInteractor` | `onUserSelected(node, userId)` (→ user's all-items view), `onOpenProfile(node, userId)` (→ profile view) |
| `UserViewInteractor` | `onBack(node)`, `onEditUser(node)` (→ edit) |
| `UserEditViewInteractor` | `onNavigateBack(node)`, `onSaved(node)`, `onDeleted(node)` |
| `UsersListViewModel` | `usersState`, `avatarsState` (`Map<UserId, FileId>`), `loadingState`, `currentUserIdState`; `onUserSelected`, `onMyProfile`, `imageUrl`/`loadImageBytes` |
| `UserViewModel` | `userState`, `avatarIdState`, `canEditState`, `loadingState`; auto-`onBack` when the user is gone after reload |
| `UserEditViewModel` | `isRootState`, `usernameState`, `passwordState`, `confirmPasswordState`, `avatarIdState`, `uploadingAvatarState`, `passwordMismatchState`, `canSaveState`, discard/delete dialog states |

## Architecture Notes

- All views use the shared `ScreenTitle` / `BackButton` / `ListRow` components from `features/common/client` (`ui.components`).
- All three screens' interactors are implemented in `client/ClientPlugin` (intra-feature push/pop). `onOpenProfile`/`UserViewInteractor.onEditUser` push `UserViewConfig`/`UserEditViewConfig` onto `node.chain`.
- `build.gradle` deps: `features/auth/client` (`ClientAuthFeature`), `features/admin/client` (`AdminFeature`), `features/files/client` (`FilesClientService`).
- **Single model**: `UsersListModel` was renamed to `UsersModel` and expanded to back all three screens (matching the one-model-per-UI-feature convention used by `wishlist`/`adminPanel`).
- **Root detection** is client-side (`me.value?.username?.string == "root"`); the server still enforces root on every admin endpoint (`403`) and owner-or-root on the avatar `PUT` (`403`).
- **My profile**: `UsersListViewModel` loads `currentUserIdState` (= `me.value?.id`); the header button is shown only when non-null and pushes `UserViewConfig(currentUserId)`.
- **Profile edit gating** (`UserEditViewModel`):
  - `isRootState` gates the editable username/password fields, the delete button, and `canSaveState`. Non-root owners see read-only username + a "no editable fields" note + the avatar uploader.
  - `canSaveState` = root && username non-blank && not loading && (password blank or password == confirm). `passwordMismatchState` drives the inline error. `onSave` calls `updateUsername` always and `setPassword` only when a new password was entered.
  - **Avatar upload** (owner or root): the image picker is the feature's own `utils/pickImageFile` (`expect`/`actual`; JS hidden input, JVM `JFileChooser`, Android `AvatarImagePicker` registered by `MainActivity`). `onAvatarPicked` → `model.uploadAvatar(userId, file)` (finalize + associate) → refresh `avatarIdState`. Avatar changes persist immediately and do not set the dirty flag.
  - **Delete** (root only) was **moved here from the users list** (per the requirement). A single confirmation dialog → `model.deleteUser(id)` → `interactor.onDeleted(node)` pops the edit screen; `UserViewModel` then reloads, finds the user gone, and auto-`onBack`s.
- Avatar rendering: JS uses `<img src=imageUrl>`; JVM/Android use a feature-local `RemoteImage` composable (Skia / `BitmapFactory`), mirroring the wishlist feature. The **users list** loads each user's avatar id via `UsersModel.getAvatar` into `avatarsState` during `loadUsers` and renders it as the `ListRow` `leading` slot (circular 48dp thumbnail, neutral placeholder box when none), mirroring the `UserWishlistsView` item-avatar pattern.
- JS uses Bootstrap modals; JVM uses Material v2 `AlertDialog`; Android uses Material3 `AlertDialog`.
- **Default avatar placeholder (issue #39):** when a user has no uploaded photo (`avatarId == null`),
  every avatar render site (UsersListView 48dp circular thumbnail, UserView and UserEditView 160dp
  previews) shows a Compose-drawn gray profile silhouette instead of a neutral box / nothing. The
  placeholder is the per-platform `UserAvatarPlaceholder` composable
  (`src/{jsMain,jvmMain,androidMain}/kotlin/ui/UserAvatarPlaceholder.kt`): JS renders an inline-SVG
  `data:` URI through `Img` (params `sizePx`, `circle`, `alt`); JVM/Android draw the silhouette with
  `androidx.compose.foundation.Canvas` (`drawRect` background + `drawCircle` head + `drawPath`
  shoulders, neutral `Color` literals so the same body compiles under both material v2 and material3),
  taking a `Modifier`+`contentDescription` mirroring `RemoteImage`'s call shape. No static assets and
  no image-loader dependency. New localized string `UsersListStrings.avatarPlaceholderAlt` (EN+RU)
  supplies the `alt`/`contentDescription`.
- **Contextual Back navigation (issue #43):** `UserView` Back is now REPLACE semantics: `UserViewInteractor.onBack` does `node.chain.replaceLastOrBackUntil(UserWishlistsViewConfig(userId))` (navigate to that user's all-items screen, popping back to it if already in the chain) instead of pop. `UserViewModel` exposes `backLabelState: StateFlow<String?>` derived from the loaded `userState` (= `username.string`); the view renders `backLabel ?: UsersListStrings.backButton.translation()`.
