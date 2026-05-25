# Feature: UI / Auth

## Operator Notes

<!-- Human operator writes here. Agents MUST read and respect before making any changes. Agents MUST NOT modify this section. -->

## Overview

Login/auth gate screen. Overlays the navigation stack when the user is not authenticated; disappears (pops itself) on successful login. Combines `features/auth` (credentials) and server URL storage into a single form. Depends on `features/auth/client` and `features/ui/serverAddress` (via `ServerUrlStorage`).

## Routes

None. Client-only feature; no server component.

## Models

| Type | Description |
|------|-------------|
| `AuthViewConfig` | Empty `@Serializable class` — pushed onto root chain when `userLoggedIn=false` |
| `AuthModel` | Interface: `isAlreadyLoggedIn()`, `getServerAddress()`, `saveServerAddress()`, `login(username, password): Boolean` |
| `AuthViewInteractor` | Interface: `onUserLoggedIn(node)`, `onUserLoggedOut()` — implemented in `client/ClientPlugin` |
| `AuthViewModel` | Holds `usernameState`, `passwordState`, `addressState`, `loadingState`, `errorState`, `loginEnabledState` |

## Architecture Notes

- `AuthViewConfig` is **not** the root node. `ClientPlugin` pushes it onto the root chain reactively when `userLoggedIn=false`; on login success it drops all `AuthViewConfig` nodes via `dropNodesInSubTree`.
- `AuthViewInteractor` implementation lives in `client/ClientPlugin` (not in this feature's `Plugin.kt`) — it needs access to the root `NavigationChain<ViewConfig>`.
- `loginEnabledState` is a derived `StateFlow<Boolean>` from `combine(usernameState, passwordState, addressState, loadingState)` — all inputs non-blank and no request in flight.
- Auto-login on startup: `AuthViewModel.init` calls `interactor.onUserLoggedIn(node)` immediately if `model.isAlreadyLoggedIn()`.
- Logout path: `Plugin.startPlugin` collects `AuthCredentialsStorage.userAuthorised` flow; `false` emission → `interactor.onUserLoggedOut()`.
- JS platform hides the address field (uses `window.location.origin` as default via `expect fun defaultServerUrl()`); JVM/Android show it.
- `errorState` resets to `false` on any input change so stale error banners clear as the user types.
