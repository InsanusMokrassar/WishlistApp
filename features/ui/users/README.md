# Feature: UI / Users

## Operator Notes

<!-- Human operator writes here. Agents MUST read and respect before making any changes. Agents MUST NOT modify this section. -->

## Overview

User-facing screens for browsing and managing user profiles. Three screens share one navigation
chain (the scaffold main slot). On JS the list + profile views render **Calm Studio** markup (the
Discover people grid `.people`/`.person`, the profile `.pagehead` header; class names mirror the
design skill's `ui_kits/calm-studio` reference so the phase-1 shell CSS styles them); the profile
**edit** screen uses existing Calm Studio form components on JS and Material fields on JVM/Android.

- **Users list** (Discover) — main page content; the global list of registered users from the public
  `UsersFeature.getAll()`, shown as a Calm Studio `.people` grid of `.person` cards (avatar circle —
  the uploaded photo or a deterministic tint when unset — over the username). Selecting a card pushes
  that user's `UserWishlistsViewConfig(userId)` (their all-items view). A **My profile** button
  (visible only when logged in) opens the caller's own profile.
- **User profile view** (`UserViewConfig(userId)`) — public, readable by anyone (anonymous
  included). Shows the username and avatar (when set). Shows an **Edit** button only to the profile
  owner and a SuperAdmin.
- **User profile edit** (`UserEditViewConfig(userId)`) — reachable by the owner and a SuperAdmin. A
  non-root owner cannot edit administrator-managed username/password fields but may upload an avatar
  and store or replace a missing, pending, or approved private email. The authoritative saved email
  and approval status remain separate from the editable replacement draft. SMTP enables verification
  delivery, but disabled SMTP still permits storage. A SuperAdmin may edit the username, set a new
  password (with a confirmation field that must match), upload an avatar, and **delete** the user.
  The user id is never editable. User *creation* is not done here (admin panel).

No auth required to view the users list or a profile.

## Routes

None — client-only UI feature. Consumes `features/users/client` (public read), `features/auth/client`
(current caller + private own-record read), `features/email/client` (owner email storage and
verification request), `features/admin/client` (root-only username/password/delete) and
`features/files/client` (avatar storage).

## Models

| Type | Description |
|------|-------------|
| `UsersListViewConfig` | Empty `@Serializable class` — main slot root identifier |
| `UserViewConfig` | `data class(userId: UserId)` — public profile detail |
| `UserEditViewConfig` | `data class(userId: UserId)` — profile edit (owner/root) |
| `UsersModel` | Single feature model (renamed from `UsersListModel`). Wraps `UsersFeature.getAll()` (returns `UsersFeatureUser` — no email, see `features/users/README.md`), auth's private `ClientAuthFeature.getMe()` for the owner record (the four private lifecycle fields are retained), `EmailFeature` (`isEmailFeatureEnabled` delivery capability, `setMyEmail` storage/replacement, `requestMyEmailVerification` delivery), admin `AdminFeature.usersManagement` (`updateUsername`, `setPassword`, `deleteUser`), and `FilesClientService` (`getAvatar`, `uploadAvatar`, `imageUrl`, `loadImageBytes`). The public list is never used to read email or approval state. |
| `UsersListViewInteractor` | `onUserSelected(node, userId)` (→ user's all-items view), `onOpenProfile(node, userId)` (→ profile view) |
| `UserViewInteractor` | `onBack(node)`, `onEditUser(node)` (→ edit) |
| `UserEditViewInteractor` | `onNavigateBack(node)`, `onSaved(node)`, `onDeleted(node)` |
| `UsersListViewModel` | `usersState`, `avatarsState` (`Map<UserId, FileId>`), `loadingState`, `currentUserIdState`; `onUserSelected`, `onMyProfile`, `imageUrl`/`loadImageBytes` |
| `UserViewModel` | `userState`, `avatarIdState`, `canEditState`, `loadingState`; auto-`onBack` when the user is gone after reload |
| `UserEditViewModel` | Existing profile/avatar/admin-edit state plus owner-only saved-email profile, replacement draft, shared storage/delivery eligibility, loading/busy/error state, saved confirmation, and explicit verification result state |

## Architecture Notes

- All views use the shared `ScreenTitle` / `BackButton` / `ListRow` components from `features/common/client` (`ui.components`).
- All three screens' interactors are implemented in `client/ClientPlugin` (intra-feature push/pop). `onOpenProfile`/`UserViewInteractor.onEditUser` push `UserViewConfig`/`UserEditViewConfig` onto `node.chain`.
- `build.gradle` deps: `features/auth/client` (`ClientAuthFeature`), `features/admin/client` (`AdminFeature`), `features/files/client` (`FilesClientService`).
- **Single model**: `UsersListModel` was renamed to `UsersModel` and expanded to back all three screens (matching the one-model-per-UI-feature convention used by `wishlist`/`adminPanel`).
- **Model implementation:** `DefaultUsersModel` implements `UsersModel` in the common UI package and is registered as an interface `single` in `Plugin.kt`. Its private constructor dependencies are `UsersFeature`, `ClientAuthFeature`, `EmailFeature`, the auth `meStateFlow`, `AdminFeature`, `FilesClientService`, `CoroutineScope`, `AuthCredentialsStorage`, and `RolesFeature`.
- **Superadmin/functionality detection is client-side**, via `roles/client` (issue #68) — replaces the
  previous `me.value?.username?.string == "root"` comparison. `UsersModel.isCurrentUserRootFlow` is
  backed by `roles/client` `RolesFeature.isFunctionalityAvailable(adminPanelFunctionalityId)` over
  `meStateFlow` (built with `meState.mapLatest { ... }.stateIn(...)` in this feature's `Plugin`),
  evaluated reactively as the caller identity changes. Similarly, `UsersModel.canChangeAvatarForOthersFlow`
  is backed by `RolesFeature.isFunctionalityAvailable(avatarChangeForOthersFunctionalityId)`.
  `UserEditViewModel.canUploadAvatarState` = (caller is the profile owner) OR `canChangeAvatarForOthersFlow`;
  the avatar uploader button in all three `UserEditView`s (JS/JVM/Android) is shown only when this state
  is true; `onAvatarPicked` no-ops otherwise. The server still enforces independently on every admin
  endpoint (`403`) and on the avatar `PUT` (`403`) — unchanged, just re-worded mechanism.
- **My profile**: `UsersListViewModel` loads `currentUserIdState` (= `me.value?.id`); the header button is shown only when non-null and pushes `UserViewConfig(currentUserId)`.
- **Profile edit gating** (`UserEditViewModel`):
  - `isRootState` gates the editable username/password fields, the delete button, and `canSaveState`. Non-root owners see read-only username + a "no editable fields" note + the avatar uploader.
  - `canSaveState` = root && username non-blank && not loading && (password blank or password == confirm). `passwordMismatchState` drives the inline error. `onSave` requires a confirmed username result before it attempts an optional password change and navigates only after every requested mutation succeeds. Username and password failures render separate feedback; a password failure after a successful username is intentionally a visible partial commit rather than a rollback.
  - **Avatar upload** (owner or root): shown only when `canUploadAvatarState` is true (owner OR has `avatarChangeForOthers` functionality). The image picker is the feature's own `utils/pickImageFile` (`expect`/`actual`; JS hidden input, JVM `JFileChooser`, Android `AvatarImagePicker` registered by `MainActivity`). `onAvatarPicked` → `model.uploadAvatar(userId, file)` (finalize + associate) → refresh `avatarIdState`. Avatar changes persist immediately and do not set the dirty flag.
  - **Owner email storage and verification:** the editor derives owner-only visibility from the current authorized caller, immutable bound user id, and the actual rendered navigation node. Each renderer collects that node's `configState` and also compares its immediate raw target before emitting private controls, feedback, status, or interruption copy; a stale owner collector therefore cannot expose an old target during retargeting. A matching private profile under `Enabled` or `Disabled` permits storage; SMTP is a delivery capability, not a storage prerequisite. Root editing another account never sees private controls or state. Each owner operation captures caller identity, selected target, and an owner generation, is cancelled on identity/session loss or retargeting, and rechecks ownership after every suspension so an obsolete PUT/POST/reconciliation cannot publish or issue a later request. The private profile snapshot contains four lifecycle fields: approved current `email`, `emailApproved`, pending candidate `pendingEmail`, and persisted UTC `emailChangeAllowedAt`; the editable draft remains separate. The retained approved current remains visible while a replacement is pending. Pending/current status, restriction deadline, Refresh behavior, expiry, and candidate resend are rendered privately. Active cooldown disables Save and mutation admission, including the synchronous IME callback; exact expiry is re-evaluated on Refresh and re-enables editing. A well-formed typed cooldown is authoritative feedback; malformed/uncertain errors remain generic. Completed saved/delivery feedback is bound to the exact checked email-and-approval snapshot, so a later address or approval transition retires old claims while a current negative outcome or uncertain storage failure remains visible. Raw trimmed draft dirtiness is independent from parsing: malformed nonblank input is immediately invalid, survives refresh/resume, and triggers Back confirmation. Replacement approval reset is owned by the repository. Enabled save confirms the exact refreshed address before optional verification; resend targets only the saved pending address. A false, 409, or transport-uncertain Boolean result is reported generically as unconfirmed storage, preserves the draft, and never posts automatically; refresh may later reveal a committed write. POST failures become the distinct `DeliveryFailed` result, while storage and delivery feedback remain separate. Confirmed `Disabled` capability displays the existing delivery-unavailable copy once while retaining save-only storage. The JS, JVM, and Android implementations use the same state and controls; shared tests cover owner privacy, lifecycle transitions, cooldown expiry, and cross-platform rendering contracts.
  - **Owner lifecycle confinement:** owner generations, refresh versions, mutation tokens/jobs, authorization checks, and private-state publications run in a `Dispatchers.Main.immediate` child scope that retains the navigation ViewModel lifecycle job. UI callbacks enter on that same UI dispatcher; tests inject one shared serial dispatcher. This prevents a worker that passed an old ownership check from publishing after invalidation while retaining node-destruction cancellation.
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
  - **Logout exits the open editor (issue #53):** `UserEditViewModel` routes logout-exit through `UsersModel.userAuthorisedState` and subscribes `model.userAuthorisedState.subscribeOnLoggedOut(scope) { interactor.onNavigateBack(node) }` (helper in `features/common/client` `utils/`) in `init`. On logout the edit screen pops to the underlying profile (read) view, **bypassing the dirty-changes confirm dialog**. The model exposes `userAuthorisedState: StateFlow<Boolean>` so the edit ViewModel exits via the model layer (MVVM boundary) instead of importing auth storage directly. Reuses the existing `onNavigateBack` surface — no new interactor method.
- **Test coverage and limits:** focused shared state tests cover replacement, later approval reset, immediate malformed-input feedback, raw partial-draft preservation/Back confirmation, disabled-SMTP storage, Boolean 409/transport ambiguity, typed cooldown feedback, active cooldown and exact expiry, owner/caller/live-target boundaries, cancellation, and stale-publication suppression. `UserEditEmailRenderTest` uses the existing desktop Compose raster host to render the production `OwnerEmailEditor`; it proves retained-current plus pending/draft widgets, active UTC cooldown with disabled mutation controls and captured IME behavior, confirmed Disabled copy, and immediate private-semantics removal after live retargeting. Pure ViewModel cases use one controlled scheduler; the desktop privacy case deliberately separates ViewModel, composition, and test-body schedulers. HTTP client/route tests cover exact PUT payloads, bearer ownership, typed 429/409 mapping; existing SQLite tests cover repository invariants. Run `./gradlew --no-parallel :wishlist.features.ui.users:jvmTest --tests '*UserEditViewModelEmailTest'`, `./gradlew --no-parallel :wishlist.features.ui.users:jvmTest --tests '*UserEditEmailRenderTest'`, `./gradlew --no-parallel :wishlist.features.email.client:jvmTest --tests '*KtorEmailFeatureTest'`, and `./gradlew --no-parallel :wishlist.features.email.server:jvmTest --tests '*EmailRoutingsConfiguratorTest'` for focused proof. The Boolean contract cannot identify duplicate causes; a lost response may be followed by a refresh that reveals a committed write; same-owner credential epochs are not observable; accepted PUTs have no client rollback. Existing platform compiles and state tests do not claim live SMTP, browser DOM, Android device, or physical Android IME execution. No browser/device renderer harness or dependency is introduced.
