# Feature: UI / Auth

## Operator Notes

<!-- Human operator writes here. Agents MUST read and respect before making any changes. Agents MUST NOT modify this section. -->

## Overview

Compact login/register widget rendered within the top navigation bar. When logged in, displays a "Log out" button. When logged out and collapsed, displays "Log in" (and "Register" when server has `enableRegistration=true`). When expanded, the credentials form appears in a modal dialog — on JS as a Bootstrap modal overlay with header (title + close button), body (inputs + error), and footer (Cancel + Submit); on JVM/Android as a `androidx.compose.ui.window.Dialog` wrapping a Surface-based form with Cancel/Submit buttons. Server URL storage is owned by `features/ui/serverUrl`. Depends on `features/auth/client`.

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
- **Modal dialog behavior:** `AuthView` per platform (JS/JVM/Android) renders log in/register trigger buttons in the navbar. When `formExpandedState` is true, the credentials form renders inside a modal dialog (Bootstrap modal overlay on JS; `androidx.compose.ui.window.Dialog` + Surface on JVM/Android), dismissable via `onCancelForm`. The navbar buttons remain visible while the dialog is open.
- **Model interface:** `AuthModel.userAuthorisedState: StateFlow<Boolean>` mirrors login state; `logout()` method clears credentials. `getServerAddress()`/`saveServerAddress()` removed — delegated to `features/ui/serverUrl`.
- **ViewModel:** `loggedInState` derived from `model.userAuthorisedState`; `formExpandedState` tracks collapse/expand toggle; `onToggleForm()` toggles collapse state; `onLogout()` calls `model.logout()`.
- `loginEnabledState` is a derived `StateFlow<Boolean>` from `combine(usernameState, passwordState, loadingState)` — all inputs non-blank and no request in flight. Shared for both login and register submit buttons.
- `registrationEnabledState` is loaded async in VM `init` from `model.isRegistrationEnabled()` (defaults to `false` until resolved).
- `registerModeState` distinguishes expanded-login from expanded-register; `onToggleRegisterForm()` sets it to `true`; `onToggleForm()` sets it to `false`; `onCancelForm()` collapses and resets both.
- **Enter-to-submit (classic form behavior):** no dedicated VM method — each platform view dispatches Enter to the existing `onRegister()`/`onAuthorize()` per `registerModeState`. JS wraps the credentials inputs in a `Form` whose `onSubmit` (triggered by the `type=submit` primary button or Enter) calls `preventDefault()` then the mode-appropriate method; the cancel button is `type=button`. JVM/Android wire `KeyboardOptions(imeAction = ImeAction.Done)` + `KeyboardActions(onDone = { if (registerMode) onRegister() else onAuthorize() })` on both fields. On JS the `Form.onSubmit` handler early-returns when `loginEnabledState` is `false`, so Enter mirrors the disabled submit button (ignored while a request is in flight or any field is blank); JVM/Android `onDone` still rely on `onAuthorize()`/`onRegister()` self-guarding blank credentials.
- `AuthViewInteractor` implementation lives in `client/ClientPlugin` (not in this feature's `Plugin.kt`) — it needs access to the root `NavigationChain<ViewConfig>`.
- `errorState` resets to `false` on any input change so stale error banners clear as the user types.
- **Registration config:** `AuthModel.isRegistrationEnabled()` calls `AuthFeature.isRegistrationAvailable()` via `runCatchingLogging` (defaults to `false` on network error).
