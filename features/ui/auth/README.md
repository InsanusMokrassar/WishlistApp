# Feature: UI / Auth

## Operator Notes

<!-- Human operator writes here. Agents MUST read and respect before making any changes. Agents MUST NOT modify this section. -->

## Overview

Compact login/register widget rendered inline within the top navigation bar. When logged in, displays a "Log out" button. When logged out and collapsed, displays "Log in" (and "Register" when server has `enableRegistration=true`). When expanded in login mode: username/password + "Submit" + "Cancel". When expanded in register mode: username/password + "Create account" + "Cancel". Server URL storage is owned by `features/ui/serverUrl`. Depends on `features/auth/client`.

## Routes

None. Client-only feature; no server component.

## Models

| Type | Description |
|------|-------------|
| `AuthViewConfig` | Empty `@Serializable class` — embedded in top navigation bar via `InjectNavigationChain` |
| `AuthModel` | Interface: `isAlreadyLoggedIn()`, `login(username, password): Boolean`, `logout()`, `userAuthorisedState: StateFlow<Boolean>`, `isRegistrationEnabled(): Boolean`, `register(username, password): Boolean` |
| `AuthViewInteractor` | Interface: `onUserLoggedIn(node)`, `onUserLoggedOut()` — implemented in `client/ClientPlugin` |
| `AuthViewModel` | Holds `usernameState`, `passwordState`, `loadingState`, `errorState`, `formExpandedState`, `registerModeState`, `registrationEnabledState`, `loggedInState`, `loginEnabledState`; methods: `onToggleForm()`, `onToggleRegisterForm()`, `onCancelForm()`, `onAuthorize()`, `onRegister()`, `onLogout()` |

## Architecture Notes

- **View embedding:** `features/ui/topBar` embeds `AuthViewConfig` via `InjectNavigationChain<ViewConfig> { InjectNavigationNode(AuthViewConfig()) }`, replacing the old "auth overlay on root chain" pattern.
- **Inline widget behavior:** `AuthView` per platform (JS/JVM/Android) is a single compact widget that renders conditionally based on `loggedInState` and `formExpandedState`.
- **Model interface:** `AuthModel.userAuthorisedState: StateFlow<Boolean>` mirrors login state; `logout()` method clears credentials. `getServerAddress()`/`saveServerAddress()` removed — delegated to `features/ui/serverUrl`.
- **ViewModel:** `loggedInState` derived from `model.userAuthorisedState`; `formExpandedState` tracks collapse/expand toggle; `onToggleForm()` toggles collapse state; `onLogout()` calls `model.logout()`.
- `loginEnabledState` is a derived `StateFlow<Boolean>` from `combine(usernameState, passwordState, loadingState)` — all inputs non-blank and no request in flight. Shared for both login and register submit buttons.
- `registrationEnabledState` is loaded async in VM `init` from `model.isRegistrationEnabled()` (defaults to `false` until resolved).
- `registerModeState` distinguishes expanded-login from expanded-register; `onToggleRegisterForm()` sets it to `true`; `onToggleForm()` sets it to `false`; `onCancelForm()` collapses and resets both.
- `AuthViewInteractor` implementation lives in `client/ClientPlugin` (not in this feature's `Plugin.kt`) — it needs access to the root `NavigationChain<ViewConfig>`.
- `errorState` resets to `false` on any input change so stale error banners clear as the user types.
- **Registration config:** `AuthModel.isRegistrationEnabled()` calls `AuthFeature.isRegistrationAvailable()` via `runCatchingLogging` (defaults to `false` on network error).
