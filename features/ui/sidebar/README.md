# Feature: UI / Sidebar

## Operator Notes

<!-- Human operator writes here. Agents MUST read and respect before making any changes. Agents MUST NOT modify this section. -->

## Overview

Calm Studio left sidebar for the **web client only**. Rendered in the scaffold's
`leftConfig` slot (wired on JS in `client/ClientJSPlugin`). Provides the app's
primary navigation and the signed-in caller's context:

- Primary items: **My Lists**, **Discover**, **Reserved** (with a live count), **Settings**.
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
| `SidebarViewInteractor` | Drives the scaffold **main** chain (My Lists / Discover / Reserved / Settings / wishlist / new list / profile) |

## Architecture Notes

- The sidebar lives in its own navigation chain (the scaffold left slot) but every action
  navigates the **main** chain, located by `MainNavigationChainId`. Every sidebar action
  **replaces** the current top node instead of pushing (via `replaceLastOrBackUntil`) so it does
  not grow the main chain / top-bar breadcrumb; if the target is already on the stack the chain
  backs up to it.
- `SidebarViewInteractor` is implemented in the top-level `client/ClientPlugin` (it needs the
  configs of destinations the feature itself cannot see). Section destinations:
  My Lists → `WishlistsListViewConfig()`, Discover → `UsersListViewConfig()`,
  Reserved → `MyPresentsBooksViewConfig()`, Settings → `UserEditViewConfig(me)`,
  profile row → `UserViewConfig(me)`, New list → `WishlistEditViewConfig(null)`.
- `SidebarModel` is registered in this feature's `Plugin` and resolves `WishlistsModel` /
  `BookingModel` lazily from Koin — no cross-feature `setupDI` delegation.
- `SidebarViewModel` reloads pinned lists, reserved count, and the active section on first show,
  on login/logout (`currentUserIdFlow`), and on every navigation change (`changesInSubTreeFlow`).
- Nav glyphs are inline [Lucide](https://lucide.dev) SVGs (`LucideIcons` / `LucideIcon`), injected
  via a Compose-HTML `ref` because Compose-HTML has no SVG DOM builder. Class names match the
  Calm Studio shell CSS (`.sidebar`, `.navsec`, `.navitem`, `.me`, …).
- Register only `JSPlugin` (in `client/Main.kt` JS plugin list); there is no Android/JVM view.
