# Feature: UI / Sidebar

## Operator Notes

<!-- Human operator writes here. Agents MUST read and respect before making any changes. Agents MUST NOT modify this section. -->

## Overview

Calm Studio left sidebar for the **web client only**. Rendered in the scaffold's
`leftConfig` slot (wired on JS in `client/ClientJSPlugin`). Provides the app's
primary navigation and the signed-in caller's context:

- Primary items: **My Lists**, **Discover**, **Reserved** (with a live count), **Settings**, and
  (root-only) **Admin Panel**.
- The caller's own wishlists pinned below, plus a **New list** affordance.
- A bottom profile row when signed in; the inline login widget (`features/ui/auth`) when anonymous.

The Android and Desktop (Material 3) clients do **not** use this feature — they keep
their own navigation chrome. The module ships `commonMain` + `jsMain` only.

## Routes

None — client-only UI feature.

## Models

| Type | Description |
|------|-------------|
| `SidebarViewConfig` | Empty `@Serializable class` placed in the scaffold left slot |
| `SidebarSection` | Enum of primary destinations used for the active-item highlight |
| `SidebarModel` | Composes `WishlistsModel` + `BookingModel`: current user, my lists, reserved count, user name |
| `SidebarViewModel` | Holds caller/lists/reserved-count/active-section state; delegates navigation to the interactor |
| `SidebarViewInteractor` | Drives the scaffold **main** chain (My Lists / Discover / Reserved / Settings / wishlist / new list / profile / admin panel (root-only)) |

## Architecture Notes

- The sidebar lives in its own navigation chain (the scaffold left slot) but every action
  navigates the **main** chain, located by `MainNavigationChainId`. Every sidebar action
  **resets** the main chain to a single node holding the target (via `resetToSingleNode`), so
  however deep the user had navigated a click leaves exactly one node — a single top-bar
  breadcrumb crumb. Clicking the already-active section is a no-op.
- `SidebarViewInteractor` is implemented in the top-level `client/ClientPlugin` (it needs the
  configs of destinations the feature itself cannot see). Section destinations:
  My Lists → `WishlistsListViewConfig()`, Discover → `UsersListViewConfig()`,
  Reserved → `MyPresentsBooksViewConfig()`, Settings → `UserEditViewConfig(me)`,
  profile row → `UserViewConfig(me)`, New list → `WishlistEditViewConfig(null)`.
- `SidebarModel` is registered in this feature's `Plugin` and resolves `WishlistsModel` /
  `BookingModel` lazily from Koin — no cross-feature `setupDI` delegation.
- **Feature Interface Return Model Rule:** `SidebarModel.getMyWishlists(): List<WishlistsFeatureWishlist>`
  (was `List<RegisteredWishlist>`) — this feature is a genuine V4 (`features/wishlist`) consumer via
  `WishlistsModel.getMyWishlists()`, confirmed to have zero `RegisteredUser`/`meStateFlow`/`AuthFeature`
  references (not a V1/auth consumer). `SidebarViewModel.myListsState` retyped to match.
- `SidebarViewModel` reloads pinned lists, reserved count, and the active section on first show,
  on login/logout (`currentUserIdFlow`), and on every navigation change (`changesInSubTreeFlow`).
- Nav glyphs are inline [Lucide](https://lucide.dev) SVGs (`LucideIcons` / `LucideIcon`), injected
  via a Compose-HTML `ref` because Compose-HTML has no SVG DOM builder. Class names match the
  Calm Studio shell CSS (`.sidebar`, `.navsec`, `.navitem`, `.me`, …).
- Register only `JSPlugin` (in `client/Main.kt` JS plugin list); there is no Android/JVM view.
- Added a 5th primary item, **Admin** (root-only), gated by `SidebarModel.isCurrentUserRootFlow` →
  `SidebarViewModel.isRootState`, which delegates to the already-existing
  `UsersModel.isCurrentUserRootFlow` (`Username.string == "root"` check, same mechanism
  `UserEditViewModel.isRootState` already uses) — no new `"root"` string literal, no new module
  dependency needed for the root check itself (the feature already depended on `ui.users`).
- Unlike the other primary items (which stay visible-but-disabled while signed out), the Admin item is
  fully **absent** — not disabled — for non-root/anonymous callers, per the issue's wording ("If user is
  root, left panel must include item").
- `onSelectAdminPanel()` navigates to `AdminPanelViewConfig` via the existing `navigateSection` /
  `resetToSingleNode` mechanism, same as the other four primary items — one click always leaves the main
  chain with exactly one node.
- `SidebarSection` gained an `Admin` case; `SidebarViewModel`'s active-section resolver logic was
  extracted into a pure, package-internal `resolveActiveSectionForStack(configs: List<ViewConfig>):
  SidebarSection` function (in `SidebarViewModel.kt`) specifically so it could be unit-tested without a
  live `NavigationNode`/`NavigationChain` — the first test coverage in this module (and in any
  `features/ui/*` module). See `features/ui/sidebar/src/commonTest/kotlin/ui/SidebarViewModelTest.kt`.
- New dependency: `features/ui/sidebar/build.gradle` now also depends on `:wishlist.features.ui.adminPanel`
  (needed for `AdminPanelViewConfig`, used both by the interactor call site in `ClientPlugin.kt` and by
  the new `SidebarSection.Admin` mapping).
- Security note: this only closes a UX/discoverability gap. Client-side hiding is not a new
  authorization boundary — every admin-panel server call is still independently gated by
  `AdminRoutingsConfigurator`'s server-side SuperAdmin-role check (via `simpleRoles.server`'s
  `SimpleRolesFeature.isSuperAdmin`, issue #68 — replaced the previous inline `rootUsername`
  comparison), matching the pattern already relied on by `UserEditViewModel.isRootState`.
