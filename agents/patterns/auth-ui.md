# Pattern: Auth UI / Gate Screen (example: `features/ui/auth`)

> Read together with the hard rules in `agents/CODING.md` and `agents/patterns/mvvm.md`.

The `features/ui/auth` UI feature combines the full-stack `features/auth` feature and the `features/ui/serverAddress` storage into a single login screen that is the root of the navigation chain. Reuse this pattern for any "gate" view that must run before the user reaches the rest of the app.

## Module dependencies (`features/ui/auth/build.gradle`)

```groovy
api project(":wishlist.features.common.client")
api project(":wishlist.features.auth.client")          // AuthFeature, AuthCredentialsStorage
api project(":wishlist.features.ui.serverAddress")     // ServerUrlStorage
```

UI features are allowed to depend on other UI features when one wraps the other's storage/interactors. Here `ui/auth` reuses `ServerUrlStorage` so the user can set the server address and credentials in one form.

## MVVM layout

- **`AuthViewConfig`** — empty `@Serializable class AuthViewConfig : ViewConfig`; NOT the root node. `client/ClientPlugin` initializes navigation with `InjectNavigationNode(SampleViewConfig())` as root. Auth is an overlay: when `userLoggedIn=false`, `AuthViewConfig` is pushed on top of the root chain; when `userLoggedIn=true`, auth nodes are dropped from the root chain via `dropNodesInSubTree { it.config is AuthViewConfig }`, revealing the pre-existing `SampleViewConfig` node.
- **`AuthModel`** (commonMain interface) exposes `isAlreadyLoggedIn()`, `getServerAddress()`/`saveServerAddress(address)` (delegating to `ServerUrlStorage`), and `login(username, password): Boolean` (delegating to `AuthFeature.login` and treating `null` as failure). The single anonymous implementation lives in the feature's common `Plugin.kt` — it composes `AuthCredentialsStorage`, `ServerUrlStorage`, and `AuthFeature`.
- **`AuthViewInteractor`** has two methods: `suspend fun onUserLoggedIn(node: NavigationNode<AuthViewConfig, ViewConfig>)` and `suspend fun onUserLoggedOut()`. The implementation lives in the top-level `client/ClientPlugin`, which injects the root `NavigationChain<ViewConfig>` and a `CoroutineScope`. The implementation is stateful and reactive — it tracks a `userLoggedIn: MutableRedeliverStateFlow(true)` flag and merges it with `rootChain.changesInSubTreeFlow()` to reactively manage the auth overlay:
  ```kotlin
  single<AuthViewInteractor> {
      val rootChain = get<NavigationChain<ViewConfig>>()
      val scope = get<CoroutineScope>()
      object : AuthViewInteractor {
          private val userLoggedIn = MutableRedeliverStateFlow(true)

          init {
              merge(
                  userLoggedIn,
                  rootChain.changesInSubTreeFlow()
              ).conflate().subscribeLoggingDropExceptions(scope) {
                  when {
                      userLoggedIn.value -> rootChain.dropNodesInSubTree { it.config is AuthViewConfig }
                      else -> {
                          if (rootChain.stackFlow.value.lastOrNull()?.config !is AuthViewConfig) {
                              rootChain.push(AuthViewConfig())
                          }
                      }
                  }
              }
          }

          override suspend fun onUserLoggedIn(node: NavigationNode<AuthViewConfig, ViewConfig>) {
              userLoggedIn.value = true
          }

          override suspend fun onUserLoggedOut() {
              userLoggedIn.value = false
          }
      }
  }
  ```
  `onUserLoggedIn` sets `userLoggedIn=true` → reactive subscription calls `dropNodesInSubTree { config is AuthViewConfig }` on the root chain. `onUserLoggedOut` sets `userLoggedIn=false` → reactive subscription pushes `AuthViewConfig` onto the root chain if not already present. Navigation target is the injected root chain (`get<NavigationChain<ViewConfig>>()`), NOT `node.chain`.
- **`AuthViewModel`** holds three input states (`usernameState`, `passwordState`, `addressState`), plus `loadingState` and `errorState`. The button-availability flow `loginEnabledState: StateFlow<Boolean>` is derived via `combine(usernameState, passwordState, addressState, loadingState).stateIn(scope, Eagerly, false)` and uses `checkFields(...)` to guarantee all three inputs are non-blank and no request is in flight. The same `checkFields` is reused inside `onAuthorize()` to gate the network call. `init { ... }` reads the saved server address (falling back to `defaultServerUrl()` — see below), and **calls `interactor.onUserLoggedIn(node)` immediately if `model.isAlreadyLoggedIn()`** so a returning user skips the form.
- **`onAuthorize`** is the single entry point for "log in" actions. It (a) re-validates fields, (b) flips `loadingState` to true under a `try/finally`, (c) saves the server address via the model, (d) calls `model.login(...)`, (e) on success calls `interactor.onUserLoggedIn(node)`, on failure flips `errorState` to true. Each input handler resets `errorState` to false so a stale error disappears as soon as the user edits anything.
- **Plugin.kt logout subscription** — the feature's common `Plugin.kt` also wires the logout path: in `startPlugin` it collects the `AuthCredentialsStorage.userAuthorised` flow and, when it emits `false`, calls `interactor.onUserLoggedOut()`. This means any code that invalidates credentials (token refresh failure, server-side logout, manual clear) automatically triggers the interactor without the ViewModel having to poll.

## Platform-specific views

The view layer differs by platform on **which fields are rendered**, not on logic:

| Platform | Fields shown | Default address fallback |
|---|---|---|
| JS | username, password (no address field) | `window.location.origin` (via `defaultServerUrl()` actual) |
| JVM (Desktop) | server address, username, password | `""` |
| Android | server address, username, password | `""` |

The JS view skips the address input because the browser already knows its origin. To keep the shared `AuthViewModel.loginEnabledState` valid on JS without an address input, the feature exports a multiplatform `expect fun defaultServerUrl(): String` in `commonMain/utils/`, with platform actuals in `jsMain/utils/DefaultServerUrl.js.kt` (`window.location.origin`), `jvmMain/utils/DefaultServerUrl.jvm.kt` (`""`), `androidMain/utils/DefaultServerUrl.android.kt` (`""`). The VM seeds `addressState` with `savedAddress.ifBlank { defaultServerUrl() }`, so JS first-time users get a non-blank address automatically and the `loginEnabledState` flow turns true once username/password are filled. Use this `expect/actual` pattern for any other "platform default" the VM needs without forking the model.

Each platform view reads the same flows and binds them to platform-appropriate widgets:

| Platform | Text field | Password masking | Button disabled binding |
|---|---|---|---|
| JS | `Input(type = InputType.Text)`, `Input(type = InputType.Password)` | native via `InputType.Password` | `if (!loginEnabled) disabled()` |
| JVM | `OutlinedTextField` with `PasswordVisualTransformation()` and `KeyboardOptions(KeyboardType.Password)` | manual | `enabled = loginEnabled` |
| Android | same as JVM but with `material3` | manual | `enabled = loginEnabled` |

The button always reads `loginEnabledState` for its disabled binding — no view-side validation logic.
