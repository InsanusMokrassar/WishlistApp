# Coding Rules

## Feature README.md

See `ALL.md` for the full rule. Short version:
- Read the feature's `README.md` (especially `## Operator Notes`) before touching any code.
- After every coding session that changes a feature: update its `README.md` (routes, models, behavior, deps).
- Never modify the `## Operator Notes` section.

### AFTER ANY CODE CHANGE

Update the feature `README.md`:

- New/removed/changed routes (path, method, auth, behavior)
- Changed models or data types
- Changed ownership/auth semantics
- New dependencies between modules

---

After you are done with changes - you must run required compilation task. In most cases, it is `./gradlew :<MODULE_NAME>:build`. If there are erorrs in output, you must try to fix it. Do only one cycle of fixing, in new cycles do not fix same (fully same) issues again but add in report

---

## JS Stylesheet Rule

Every JS (`jsMain`) view that needs custom CSS **MUST** define its styles in a dedicated
`object <ViewName>Stylesheet : StyleSheet()` placed in the same `ui/` package as the view class.

- Class name: `<ViewName>Stylesheet` (e.g. `ScaffoldViewStylesheet` for `ScaffoldView`).
- Base class: `org.jetbrains.compose.web.css.StyleSheet`.
- Apply in `onDraw()` with `Style(<ViewName>Stylesheet)` before rendering DOM elements.
- Keep all CSS declarations (flex, grid, sizing, overflow, etc.) inside the stylesheet object — do not use inline `style { }` blocks in DOM elements for anything that belongs to the layout skeleton.

---

## KDoc Requirements

Priority of selecting model for KDocs fills: `haiku` / `sonnet` / `opus`

**ALL created `.kt` files MUST contain valid KDocs.**

Rules:
- Every `class`, `interface`, `object`, `fun`, `val`/`var` at class/interface level must have a KDoc comment.
- KDocs must describe purpose, not restate the name.
- When updating existing code that has KDocs — update the KDocs to match.
- Constructor parameters documented via `@param` tags.
- Return values documented via `@return` tag when non-obvious.
- No placeholder or empty KDoc blocks (`/** */`).

---

## Plugin Composition Pattern

Each feature has platform-specific plugin objects that delegate to shared ones:

```kotlin
// Pattern used in every platform plugin (JVM, JS, Android)
object JVMPlugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        with(ParentPlugin) { setupDI(config) }   // delegate to parent
        with(Plugin) { setupDI(config) }          // delegate to common
        // ... platform-specific registrations
    }
    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
        ParentPlugin.startPlugin(koin)
        Plugin.startPlugin(koin)
    }
}
```

### Plugin Registration

- **Server**: plugin FQCNs listed in `config.json` `"plugins"` array, loaded by reflection.
- **Client (JS/JVM/Android)**: plugin instances hardcoded in `Main.kt` / `MainActivity.kt`.

---

## Plugins (`StartPlugin` inheritors) note

You MUST NOT add new `setupDI` and `startPlugin` methods calls in plugins for the other plugins outside of feature. It is
permitted to call `setupDI` and `startPlugin` methods only within the same feature and only to the plugin with greater commonized meaning.
For example, `JVMPlugin` can't call `JSPlugin.setupDI`, but must call `Plugin.setupDI`.

---

## Koin Named Qualifiers

When a Koin `named("...")` qualifier is used more than once within a plugin (e.g. registering a dependency under a qualifier and resolving it elsewhere with `get(qualifier = ...)`), extract the qualifier into a single `private val` on the plugin object instead of repeating the inline `named("...")` literal. This keeps the qualifier string defined once and prevents drift between registration and resolution sites.

```kotlin
object Plugin : StartPlugin {
    private val myDependencyQualifier = named("myDependency")

    override fun Module.setupDI(config: JsonObject) {
        single(qualifier = myDependencyQualifier) { /* ... */ }
        single { Consumer(dep = get(qualifier = myDependencyQualifier)) }
    }
}
```

### Typed definition & accessor helpers

When a qualified dependency is registered in one plugin and resolved at other call sites, do not repeat raw `single<T>(qualifier = ...)` / `get<T>(qualifier = ...)` calls. Instead, define typed helpers in a dedicated file (one per logical dependency) so the qualifier, the type, and the resolution rule are declared exactly once:

- one `Module.singleXxx(...)` registration extension wrapping `single(qualifier, createdAtStart, definition)`;
- typed read accessors `Koin.xxx` and `Scope.xxx` wrapping `get(qualifier = ...)` — `Scope` accessor lets other `single { }`/`factory { }` blocks resolve the dependency without naming the qualifier.

Visibility mirrors the dependency's public surface: expose the read-only/public type with public helpers; keep any mutable backing dependency `private`/`internal` (private qualifier + `internal` helpers and accessors).

```kotlin
// MyThing.kt — declares qualifier, registration helper, and accessors once
val myThingQualifier: StringQualifier = named("myThing")

internal fun Module.singleMyThing(
    createdAtStart: Boolean = false,
    definition: Definition<MyThing>,
): KoinDefinition<MyThing> = single(myThingQualifier, createdAtStart, definition)

val Koin.myThing: MyThing
    get() = get(qualifier = myThingQualifier)

val Scope.myThing: MyThing
    get() = get(qualifier = myThingQualifier)
```

```kotlin
// Plugin.kt — registration and resolution stay free of raw qualifier/type literals
override fun Module.setupDI(config: JsonObject) {
    singleMyThing { MyThing(/* ... */) }
    single { Consumer(dep = myThing) } // Scope.myThing accessor
}
```

For a dependency with a public read-only view backed by a private mutable instance, declare two pairs of helpers in the same file: public `singleXxx` + `Koin.xxx`/`Scope.xxx` for the read-only type, and `internal` `singleSecretXxxMutable` + `internal` `Koin.secretXxxMutable`/`Scope.secretXxxMutable` (with a `private` qualifier) for the mutable backing instance.

---

## Full-Stack Feature Implementation

After scaffolding and registering the modules, write the actual implementation following `features/sample/` as a reference:

### Common module (`features/FEATURE_NAME/common/`)

Define shared URL path constants used by both server and client to avoid out-of-sync strings:

```kotlin
// Constants.kt
object FEATURE_NAMEConstants {
    const val prefixPathPart = "myFeature"
    const val myActionPathPart = "myAction"
}
```

Replace `FEATURE_NAME` here with name of feature in UpperCamelCase.

The common `Plugin.kt` is typically empty at the start (only inherits the parent's `setupDI`).

### Server module (`features/FEATURE_NAME/server/`)

#### Common part (`commonMain/kotlin`)

**Declare the feature interface** — the business capability the server exposes:

> **Note** Feature can be placed in `features/FEATURE_NAME/common/commonMain/kotlin` in case if it have fully identical
> structure in server and client.

```kotlin
// SampleFeature.kt
interface MyFeature {
    suspend fun myAction(): String
}
```

**Implement the feature**, for example, in a service class under `services/`:

```kotlin
// services/MyFeatureService.kt
class MyFeatureService : MyFeature {
    override suspend fun myAction(): String = "result"
}
```

**Fill `Plugin.kt`** — register the service, bind the interface:

```kotlin
// Plugin.kt
object Plugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        single { MyFeatureService() }
        single<MyFeature> { get<MyFeatureService>() }
    }
}
```

#### JVM part (`jvmMain/kotlin`)

**Register routes** by implementing `ApplicationRoutingConfigurator.Element` under `configurators/`:

```kotlin
// configurators/MyRoutingsConfigurator.kt
class MyRoutingsConfigurator(
    private val feature: MyFeature
) : ApplicationRoutingConfigurator.Element {
    override fun Route.invoke() {
        route(Constants.prefixPathPart) {
            get(Constants.myActionPathPart) {
                call.respondText(feature.myAction())
            }
        }
    }
}
```

**Fill `JvmPlugin.kt`** — register the routing configurator with a random qualifier so Ktor picks it up automatically:

```kotlin
// JvmPlugin.kt
object JvmPlugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        /* here old code, do not change it */
        singleWithRandomQualifier<ApplicationRoutingConfigurator.Element> {
            MyRoutingsConfigurator(get())
        }
    }
}
```

### Client module (`features/FEATURE_NAME/client/`)

**Mirror the interface if it is not in common module of feature**:

```kotlin
// SampleFeature.kt
interface MyFeature {
    suspend fun myAction(): String
}
```

**Implement via Ktor HTTP** using constants from the common module:

```kotlin
// KtorMyFeature.kt
class KtorMyFeature(private val client: HttpClient) : MyFeature {
    private val path = "${Constants.prefixPathPart}/${Constants.myActionPathPart}"
    override suspend fun myAction(): String = client.get(path).bodyAsText()
}
```

**Wire in `Plugin.kt`**:

```kotlin
// Plugin.kt
object Plugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        single { KtorMyFeature(get()) }
        single<MyFeature> { get<KtorMyFeature>() }
    }
}
```

> **Ktor realization rule:** `KtorXxxFeature` classes must **only** call HTTP endpoints and return the result. They must not implement any additional logic — no storage writes, no caching, no business rules. If such logic is needed, wrap `KtorXxxFeature` in a service class (e.g. `MyFeatureService`) that holds the storage and delegates HTTP calls to the Ktor realization. Register the service as the `MyFeature` binding in Koin, not the Ktor class directly.

---

## UI MVVM Rules

### Model

- Declared as an **interface** in `commonMain`.
- Its purpose is to **talk to the outside world**: HTTP client requests, database queries, WebSocket streams, device APIs, or any other external I/O. It must not hold UI state or contain presentation logic.
- The concrete implementation is registered in `Plugin.kt` as a Koin `single`:
  ```kotlin
  single<MyModel> {
      object : MyModel {
          // HTTP calls, DB access, etc.
      }
  }
  ```
- For non-trivial implementations, extract to a named class and register the same way:
  ```kotlin
  single<MyModel> { MyModelImpl(get()) }
  ```
- The interface must expose:
  - **Global outside-world state** as `val ...: StateFlow<T>`. The implementation backs it with a `MutableRedeliverStateFlow`:
    ```kotlin
    interface MyModel {
        val items: StateFlow<List<Item>>
    }
    class MyModelImpl : MyModel {
        private val _items = MutableRedeliverStateFlow<List<Item>>(emptyList())
        override val items: StateFlow<List<Item>> = _items
    }
    ```
  - **Parameterized outside-world state** (state that depends on an argument) as a plain `fun` returning `Flow<T>`:
    ```kotlin
    fun itemById(id: Long): Flow<Item?> // nullability is optional and depend on each case separately
    ```
  - **Mutations** (any action that changes outside-world state) as `suspend fun`:
    ```kotlin
    suspend fun addItem(item: Item)
    suspend fun deleteItem(id: Long)
    ```

### Interactor

- Declared as an **interface** in the feature's `commonMain`, named `<FeatureName>ViewInteractor`, placed next to the `ViewModel` (e.g. `features/ui/myScreen/src/commonMain/kotlin/ui/MyScreenViewInteractor.kt`).
- Its purpose is to expose a **side-effecting capability** that the ViewModel needs but cannot own itself: things that depend on the surrounding application or platform (navigation between features, opening other screens, triggering app-level dialogs, switching themes, etc.). Unlike `Model`, it does not represent outside-world data — it represents outside-world *behavior* that the ViewModel delegates to.
- The ViewModel takes the interactor as a constructor parameter named `interactor` and resolves it from Koin in the factory with a plain `get()`:
  ```kotlin
  class MyViewModel(
      private val node: NavigationNode<MyViewConfig, ViewConfig>,
      private val model: MyModel,
      private val interactor: MyViewInteractor
  ) : ViewModel<ViewConfig>(node)

  // in the feature's common Plugin.kt:
  factory { MyViewModel(it.get(), get(), get()) }
  ```
- The feature's `Plugin.kt` does **not** register an implementation. The interface is published from the feature and bound somewhere the feature itself cannot see (it would create a cyclic dependency).
- **Implementations live in the top-level `client/` module by default**, registered as `single<MyViewInteractor> { ... }` inside `ClientPlugin` (`client/src/commonMain/kotlin/ClientPlugin.kt`). Prefer `commonMain` so all three platforms share one implementation. Drop into a platform-specific source set (`client/src/jsMain`, `client/src/jvmMain`, `client/src/androidMain`) only when the interactor's body genuinely requires it (e.g. it needs `Context`, `window`, or a JVM-only API).

#### Intra-feature navigation interactors (multi-screen feature)

When all navigation is within a single feature's own chain (push a sibling screen, pop back), the interactor implementation needs no injected dependencies — only the `node` parameter passed at call time. Register a stateless anonymous object in `ClientPlugin`.

The example below is a generic two-screen feature: a list screen (`ItemsListView`) that opens a detail (`ItemView`) or pushes a create/edit form (`ItemEditView`), and the edit screen that pops back. Replace `Item`/`ItemId` and the `Xxx*ViewConfig` names with your feature's own types.

```kotlin
// client/src/commonMain/kotlin/ClientPlugin.kt — setupDI:
single<ItemsListViewInteractor> {
    object : ItemsListViewInteractor {
        override suspend fun onItemSelected(
            node: NavigationNode<ItemsListViewConfig, ViewConfig>,
            itemId: ItemId
        ) {
            node.chain.push(ItemViewConfig(itemId))
        }
        override suspend fun onCreateItem(
            node: NavigationNode<ItemsListViewConfig, ViewConfig>
        ) {
            node.chain.push(ItemEditViewConfig(null))
        }
    }
}

single<ItemEditViewInteractor> {
    object : ItemEditViewInteractor {
        override suspend fun onNavigateBack(node: NavigationNode<ItemEditViewConfig, ViewConfig>) {
            node.chain.pop()
        }
        override suspend fun onSaved(node: NavigationNode<ItemEditViewConfig, ViewConfig>) {
            node.chain.pop()
        }
    }
}
```

> **Reference implementation:** `features/ui/wishlist` (list + edit screens) follows this exact shape.

Key rules:

- Each screen in a multi-screen feature gets its own `XxxViewInteractor` interface — one interactor per ViewModel, not one per feature.
- Method names describe the **user intent** (`onItemSelected`, `onCreateItem`, `onNavigateBack`, `onSaved`), not the widget action.
- The `node` parameter is always the **current screen's node** (`NavigationNode<ThisViewConfig, ViewConfig>`). Push new screens onto `node.chain`; pop via `node.chain.pop()`.
- For simple push/pop navigation, no Koin `get()` calls are needed inside the `single { }` block — the anonymous object body is stateless.
- Reserve reactive / stateful interactors (injecting `NavigationChain`, `CoroutineScope`, `MutableRedeliverStateFlow`) for cross-cutting concerns like auth overlay management — see the Auth UI Pattern section below for that pattern.

### ViewModel

- Extends `ViewModel<ViewConfig>` from `dev.inmo.navigation.mvvm`.
- Constructor receives `NavigationNode<MyViewConfig, ViewConfig>` (passed as a Koin factory parameter), the `Model` singleton, and the `Interactor` singleton.
- Must contain **all UI state** — every state is declared as a private/public pair:
  ```kotlin
  private val _itemsState = MutableRedeliverStateFlow<List<Item>>(emptyList())
  val itemsState = _itemsState.asStateFlow()

  private val _loadingState = MutableRedeliverStateFlow(false)
  val loadingState = _loadingState.asStateFlow()
  ```
- Must contain **all MVVM logic**: reacting to user events, calling `Model` methods, updating state flows, and triggering navigation.
- The `View` must be as dumb as possible — it only observes state and forwards user actions to the ViewModel.
- **User interaction handlers** are named by their meaning (what the user intends), not by the UI action that triggered them:
  ```kotlin
  fun onSubmitForm(data: FormData)   // good — describes intent
  fun onButtonClick()                // bad  — describes widget action
  ```
- Each interaction handler launches work via `scope.launchLoggingDropExceptions`:
  ```kotlin
  fun onSubmitForm(data: FormData) {
      scope.launchLoggingDropExceptions {
          // all logic for this interaction
      }
  }
  ```
- For long-running interactions (server requests, heavy computations) the ViewModel **must** use a `loadingState` and the View must reflect it:
  ```kotlin
  fun onSubmitForm(data: FormData) {
      scope.launchLoggingDropExceptions {
          _loadingState.value = true
          try {
              model.submitForm(data)
          } finally {
              _loadingState.value = false
          }
      }
  }
  ```
- Registered in `Plugin.kt` as a Koin `factory` (not `single`) so each navigation node gets its own instance:
  ```kotlin
  factory { MyViewModel(it.get(), get(), get()) }
  // it.get() — resolves the NavigationNode passed as a parameter by the View
  // get()    — resolves the Model singleton
  // get()    — resolves the Interactor singleton (bound in client/ClientPlugin)
  ```

### ViewConfig

- A `@Serializable class` implementing `ViewConfig`.
- Carries only the data needed to identify and initialize the screen (navigation parameters).
- Registered in `Plugin.kt` with a polymorphic `SerializersModule`:
  ```kotlin
  singleWithRandomQualifier {
      SerializersModule {
          polymorphic(Any::class, MyViewConfig::class, MyViewConfig.serializer())
          polymorphic(ViewConfig::class, MyViewConfig::class, MyViewConfig.serializer())
      }
  }
  ```

### View (platform-specific)

- One `class` per platform (`jsMain`, `jvmMain`, `androidMain`), each extending `ComposeView<MyViewConfig, ViewConfig, MyViewModel>`.
- The ViewModel is always injected lazily:
  ```kotlin
  override val viewModel: MyViewModel by inject(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
      parametersOf(this@MyView)
  }
  ```
- `onDraw()` is a `@Composable` override. Use platform-specific Compose APIs:

  | Platform | Text API | String translation |
  |---|---|---|
  | JS | `org.jetbrains.compose.web.dom.Text` | `str.translation()` |
  | JVM | `androidx.compose.material.Text` | `str.translation()` |
  | Android | `androidx.compose.material3.Text` | `str.translation(LocalResources.current)` |

- Each platform plugin (`JSPlugin`, `JVMPlugin`, `AndroidPlugin`) delegates to the common `Plugin` and registers a `NavigationNodeFactory` that instantiates the platform `View`:
  ```kotlin
  singleWithRandomQualifier<NavigationNodeFactory<ViewConfig>> {
      NavigationNodeFactory.Typed<MyViewConfig, ViewConfig> { chain, config ->
          MyView(chain, config)
      }
  }
  ```

### Localized Strings

Define all user-visible text in a `MyStrings` object using `buildStringResource`:

```kotlin
object MyStrings {
    val title = buildStringResource("English text") {
        IetfLang.Russian("Русский текст")
    }
}
```

---

## Adding Server Routes

Routes are not hardcoded. Feature plugins register `ApplicationRoutingConfigurator.Element` instances into Koin using `singleWithRandomQualifier`. The common server plugin collects all of them and installs their routes into Ktor's routing tree.

```kotlin
// In a feature's JVMPlugin.setupDI:
singleWithRandomQualifier<ApplicationRoutingConfigurator.Element> {
    ApplicationRoutingConfigurator.Element {
        // `this` is Routing
        get("/api/my-endpoint") {
            call.respondText("Hello")
        }
    }
}
```

### Server Configurator Extension Points

Each configurator type has its own `Element` fun interface:

| Configurator | Element interface | What it configures |
|---|---|---|
| `ApplicationRoutingConfigurator` | `Routing.() -> Unit` | HTTP routes |
| `ApplicationAuthenticationConfigurator` | `AuthenticationConfig.() -> Unit` | Auth providers |
| `ApplicationSessionsConfigurator` | (from microutils) | Session types |
| `StatusPagesConfigurator` | (from microutils) | Error handlers |
| `ApplicationCachingHeadersConfigurator` | (from microutils) | Cache headers |

Register elements with `singleWithRandomQualifier<ConfiguratorType.Element>` in `setupDI`.

---

## Navigation / MVVM (Client) — Koin Registration Pattern

Each UI feature registers these things in its plugin's `setupDI`:

**1. Polymorphic serializer** — in common `Plugin.kt`:

```kotlin
singleWithRandomQualifier {
    SerializersModule {
        polymorphic(Any::class, MyViewConfig::class, MyViewConfig.serializer())
        polymorphic(ViewConfig::class, MyViewConfig::class, MyViewConfig.serializer())
    }
}
```

**2. ViewModel factory** — in common `Plugin.kt`:

```kotlin
factory { MyViewModel(it.get(), get(), get()) }
```

`it.get()` resolves the `NavigationNode` passed as a parameter; the first `get()` resolves the Model; the second `get()` resolves the Interactor.

**3. Model singleton** — in common `Plugin.kt`:

```kotlin
single<MyModel> { MyModelImpl(/* deps */) }
```

**4. Interactor singleton** — NOT in the feature's `Plugin.kt`. The feature only declares the `MyViewInteractor` interface. The implementation is bound in the top-level `client/` module's `ClientPlugin` (commonMain by default; platform-specific source set only when the implementation requires platform APIs):

```kotlin
// in client/src/commonMain/kotlin/ClientPlugin.kt — setupDI:
single<MyViewInteractor> {
    object : MyViewInteractor {
        // delegate to navigation, other features, etc.
    }
}
```

**5. NavigationNodeFactory** — in each platform plugin (`JSPlugin.kt`, `JVMPlugin.kt`, `AndroidPlugin.kt`):

```kotlin
singleWithRandomQualifier<NavigationNodeFactory<ViewConfig>> {
    NavigationNodeFactory.Typed<MyViewConfig, ViewConfig> { chain, config ->
        MyView(chain, config)
    }
}
```

**6. ComposeView** — no DI registration needed. Instantiated by the factory. Injects ViewModel lazily:

```kotlin
override val viewModel: MyViewModel by inject(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
    parametersOf(this@MyView)
}
```

### Platform-specific View Differences

The `ComposeView.onDraw()` is implemented per platform with different Compose APIs:

| Platform | Text import | `onDraw()` |
|---|---|---|
| JS (Web) | `org.jetbrains.compose.web.dom.Text` | `Text(string.translation())` |
| JVM (Desktop) | `androidx.compose.material.Text` | `Text(string.translation())` |
| Android | `androidx.compose.material3.Text` | `Text(string.translation(LocalResources.current))` |

### Localization (Strings)

Use `dev.inmo.micro_utils.strings.buildStringResource` for localizable strings:

```kotlin
object MyStrings {
    val title = buildStringResource("English default") {
        IetfLang.Russian("Русский текст")
    }
}
```

Access in Compose: `MyStrings.title.translation()` (JS/JVM) or `MyStrings.title.translation(LocalResources.current)` (Android).

### Connecting a UI Scenario to a Full-Stack Feature

A UI scenario consumes a full-stack feature through its `Model` interface. The ViewModel never imports the feature client interface directly — the Model wraps it. This is exactly what `features/ui/sample/` does with `features/sample/client`.

**Step 1 — Add the client module as a dependency** in the UI feature's `build.gradle`:

```groovy
api project(":wishlist.features.FEATURE_NAME.client")
```

**Step 2 — Declare the Model interface** to expose only what the scenario needs (not the full feature surface):

```kotlin
// SampleModel.kt
interface SampleModel {
    suspend fun getSampleText(): String
    fun serverStatusFlow(): Flow<String?>
}
```

**Step 3 — Create an anonymous Model implementation in `Plugin.kt`** that delegates to the injected feature:

```kotlin
single<SampleModel> {
    val feature = get<SampleFeature>()  // from features/sample/client
    val echoFeature = get<EchoFeature>()
    object : SampleModel {
        override suspend fun getSampleText(): String = feature.getSampleText()
        override fun serverStatusFlow(): Flow<String?> = flow {
            while (true) {
                val result = runCatchingLogging { echoFeature.getEcho() }
                emit(result.getOrNull())
                delay(1.seconds)
            }
        }
    }
}
```

> **Note** in case of need you may create some `utils` `expect`ed kotlin functions in module. For
> example, if you need to add function like `getSampleText` which will work differently on different platforms, you must:
>
> * Check or create `utils` package in scenario commonMain
> * Create common file (for `getSampleText` it will be `GetSampleText.kt`, take care about correct package in there)
> * Put there `expect` function - like `expect fun getSampleText(): String`
> * In each platform check or create `utils` package
> * Create there file with infix `PLATFORM_NAME`. For example, in jvm - `GetSampleText.jvm.kt`
> * Put there `actual` function - like `actual fun getSampleText(): String = "Sample text for JVM"`

**Step 4 — Use the Model in the ViewModel** — never inject the feature interface directly into the ViewModel:

```kotlin
class SampleViewModel(
    private val node: NavigationNode<SampleViewConfig, ViewConfig>,
    private val model: SampleModel
) : ViewModel<ViewConfig>(node) {
    private val _textState = MutableRedeliverStateFlow("Loading...")
    val textState = _textState.asStateFlow()

    init {
        scope.launchLoggingDropExceptions {
            val basePart = model.getSampleText()
            model.serverStatusFlow().collect { status ->
                _textState.value = "$basePart\n\n - Server status answer: $status"
            }
        }
    }
}
```

---

## Notes (added during Server Address feature)

### Cross-cutting feature inside `features/common`

When a feature is a tiny cross-cutting helper rather than a domain-level capability (example: an `echo`/`status` ping endpoint plus client wrapper), prefer hosting it as a sub-package inside `features/common/{common,client,server}` instead of creating a new top-level feature module. Concretely:

- Put shared constants/path parts under `features/common/common/src/commonMain/kotlin/<topic>/` (e.g. `echo/Constants.kt`).
- Put server interface, services and routing configurator under `features/common/server/src/commonMain/kotlin/<topic>/` and register them inside `features/common/server/.../Plugin.kt` (the existing one — extend it, don't create a parallel plugin).
- Put client interface and Ktor-based implementation under `features/common/client/src/commonMain/kotlin/<topic>/` and register them inside `features/common/client/.../Plugin.kt`.

Because `features/common` is loaded by every consumer, no extra wiring in `settings.gradle` / `Main.kt` / `sample.config.json` is needed.

### Single-platform local storage interface (e.g. `ServerUrlStorage`)

For UI features that need very small, per-client persistent state (like a saved server URL), use this pattern:

- Declare a `suspend`-based interface in `commonMain`:
  ```kotlin
  interface ServerUrlStorage {
      suspend fun getServerUrl(): String?
      suspend fun saveServerUrl(url: String?)
  }
  ```
  All accessor methods are `suspend` — the caller is always inside a coroutine (ViewModel scope, request pipeline, etc.) so there's no reason to expose blocking IO.
- Provide one implementation per platform under `jsMain` / `jvmMain` / `androidMain`:
  - JS: `kotlinx.browser.localStorage`.
  - JVM (Desktop): `java.util.prefs.Preferences.userRoot().node(...)`.
  - Android: `Context.getSharedPreferences(...)` — pull `Context` from Koin (already registered by `ClientAndroidPlugin`).
- Each implementation MUST guard its mutable backing store with a `dev.inmo.micro_utils.coroutines.SmartRWLocker`, using `withReadAcquire { }` for reads and `withWriteLock { }` for writes. This keeps concurrent suspending readers from stepping on a writer (e.g. a HTTP request reading the saved URL while the user clicks "Save").
  ```kotlin
  private val locker = SmartRWLocker()
  override suspend fun getServerUrl(): String? = locker.withReadAcquire { /* read */ }
  override suspend fun saveServerUrl(url: String?) {
      locker.withWriteLock { /* write */ }
  }
  ```
- Register the platform impl as `single<ServerUrlStorage>` inside that platform's `JSPlugin` / `JVMPlugin` / `AndroidPlugin`.
- The common `Model` consumes the interface; the common `Plugin` does NOT register it (no expect/actual indirection needed).
- The `ViewModel` cannot read the initial value synchronously in its constructor — `init { scope.launchLoggingDropExceptions { ...read storage... } }` is the canonical pattern; the input field starts empty for ~one frame, then populates.

### `HttpClientConfigurator` driven by stored URL

`features/common/client/.../Plugin.kt` builds `HttpClient` once, iterating all `HttpClientConfigurator`s registered via `singleWithRandomQualifier`. The configurator's `configure()` is **not** suspend, so it cannot directly call a suspend storage. Instead, install a per-request Ktor client plugin via `io.ktor.client.plugins.api.createClientPlugin` — its `onRequest { request, _ -> ... }` lambda IS suspend, so it can call `storage.getServerUrl()` and apply the URL fresh on every outgoing request:

```kotlin
override fun HttpClientConfig<*>.configure() {
    val storage = storage
    val plugin = createClientPlugin("DefaultServerUrlPlugin") {
        onRequest { request, _ ->
            val currentUrl = storage.getServerUrl() ?: return@onRequest
            val newUrlBuilder = URLBuilder(currentUrl)
            request.url.fillAbsentPartsWith(newUrlBuilder)
        }
    }
    install(plugin)
}
```

`fillAbsentPartsWith` (defined in `features/common/client/src/commonMain/kotlin/utils/MergeUrlBuilders.kt`) fills only the absent/empty parts of the request URL from the stored default — protocol, host, port, path, credentials, query, and fragment are each filled only if not already set. This means: (a) saving a new URL takes effect on the next request, no `HttpClient` rebuild needed; (b) requests that already specify a host (or any other URL component) keep their own values and are not overridden. Register the configurator from the feature's common `Plugin.kt` with `singleWithRandomQualifier<HttpClientConfigurator>`.

---

## Exposed repositories notes

> **IMPORTANT**: Each exposed database must contains `init` block with calling of `initTable()` (package `dev.inmo.micro_utils.repos.exposed`) to init table

---

## CRUD Repository Pattern

This section documents the canonical pattern for adding a persistent, cache-backed CRUD repository to a feature. Names use a generic `Item` entity — replace `Item`/`ItemId`/`ItemName` and `FEATURE_NAME` with your feature's own types.

> **Reference implementation:** `features/users` (`User`/`UserId`/`Username`) follows this exact pattern end to end.

### Model layer (`features/FEATURE_NAME/common/commonMain`)

Define three model types:

- **`Id`** — inline value class wrapping a primitive (used as the primary key):
  ```kotlin
  @Serializable @JvmInline value class ItemId(val long: Long)
  ```
- **`NewObject`** — data sent on create (no id yet):
  ```kotlin
  @Serializable data class NewItem(override val name: ItemName) : Item
  ```
- **`RegisteredObject`** — stored entity returned after create/read (carries the id):
  ```kotlin
  @Serializable data class RegisteredItem(val id: ItemId, override val name: ItemName) : Item
  ```

A sealed `Item` interface must be used as the shared base for `NewItem` and `RegisteredItem`; fields common to both variants are declared there.

Auxiliary value types (e.g. `ItemName`) should also be inline value classes so they carry type-safety with zero runtime overhead:

```kotlin
@Serializable @JvmInline value class ItemName(val string: String)
```

### Repository interfaces (`features/FEATURE_NAME/common/commonMain`)

Split the repository into three interfaces:

```kotlin
// ReadItemsRepo.kt
interface ReadItemsRepo : ReadCRUDRepo<RegisteredItem, ItemId>

// WriteItemsRepo.kt
interface WriteItemsRepo : WriteCRUDRepo<RegisteredItem, ItemId, NewItem>

// ItemsRepo.kt
interface ItemsRepo : ReadItemsRepo, WriteItemsRepo, CRUDRepo<RegisteredItem, ItemId, NewItem>
```

- `ReadCRUDRepo`, `WriteCRUDRepo`, and `CRUDRepo` are from `dev.inmo.micro_utils.repos`.
- Splitting read and write allows consumers that only need read access to depend only on `ReadItemsRepo`.

### Cache repository (`features/FEATURE_NAME/common/commonMain`)

Wrap the real repo with `FullCRUDCacheRepo` from `dev.inmo.micro_utils.repos.cache.full`:

```kotlin
class CacheItemsRepo(
    parentRepo: ItemsRepo,
    scope: CoroutineScope,
    kvCache: KeyValueRepo<ItemId, RegisteredItem> = MapKeyValueRepo(),
    locker: SmartRWLocker = SmartRWLocker()
) : ItemsRepo, FullCRUDCacheRepo<RegisteredItem, ItemId, NewItem>(
    crudRepo = parentRepo,
    kvCache = kvCache,
    scope = scope,
    skipStartInvalidate = false,
    locker = locker,
    idGetter = RegisteredItem::id
)
```

- `kvCache` defaults to an in-memory `MapKeyValueRepo`; swap for a persistent implementation if needed.
- `skipStartInvalidate = false` causes the cache to pre-fill from the DB on startup.
- `idGetter` is a function reference pointing to the id property of the registered type.

### Exposed (JVM) implementation (`features/FEATURE_NAME/common/jvmMain`)

Extend `AbstractExposedCRUDRepo` from `dev.inmo.micro_utils.repos.exposed`:

```kotlin
class ExposedItemsRepo(
    override val database: Database
) : ItemsRepo, AbstractExposedCRUDRepo<RegisteredItem, ItemId, NewItem>(tableName = "items") {

    private val idColumn = long("id").autoIncrement()
    private val nameColumn = text("name").uniqueIndex()

    override val primaryKey = PrimaryKey(idColumn)

    override val ResultRow.asObject: RegisteredItem
        get() = RegisteredItem(
            id = ItemId(get(idColumn)),
            name = ItemName(get(nameColumn))
        )

    override val ResultRow.asId: ItemId
        get() = ItemId(get(idColumn))

    override val selectById: (ItemId) -> Op<Boolean> = { idColumn.eq(it.long) }

    override fun update(id: ItemId?, value: NewItem, it: UpdateBuilder<Int>) {
        it[nameColumn] = value.name.string
    }

    override fun InsertStatement<Number>.asObject(value: NewItem): RegisteredItem =
        RegisteredItem(
            id = ItemId(this[idColumn]),
            name = value.name
        )

    init { initTable() }
}
```

- `initTable()` (from `dev.inmo.micro_utils.repos.exposed`) runs `SchemaUtils.createMissingTablesAndColumns` inside a transaction on `init`.
- The `update` function is called for both insert-fill and explicit update paths; `id` is `null` during insert.
- `InsertStatement<Number>.asObject` constructs the registered object from the auto-generated id returned by the insert statement.

### DI wiring (`features/FEATURE_NAME/common/jvmMain — JVMPlugin`)

Register `ExposedItemsRepo` as a plain `single`, then wrap it in `CacheItemsRepo` and bind both `ReadItemsRepo` and `WriteItemsRepo` via `singleWithBinds`:

```kotlin
object JVMPlugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        with(Plugin) { setupDI(config) }

        single { ExposedItemsRepo(get()) }           // raw DB repo; only needed for cache wiring
        singleWithBinds<ItemsRepo> {
            CacheItemsRepo(parentRepo = get<ExposedItemsRepo>(), scope = get())
        }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
        Plugin.startPlugin(koin)
    }
}
```

- `singleWithBinds` registers the `CacheItemsRepo` as `ItemsRepo`, `ReadItemsRepo`, and `WriteItemsRepo` simultaneously — any consumer that injects any of those three interfaces gets the cache-backed instance.
- `ExposedItemsRepo` is registered separately so it can be retrieved by type when constructing `CacheItemsRepo`; consumers should never inject it directly.

### Server plugin wiring (`features/FEATURE_NAME/server/jvmMain — JVMPlugin`)

The server plugin delegates to the common JVM plugin so the repo is available in the DI graph:

```kotlin
object JVMPlugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        with(features.FEATURE_NAME.common.JVMPlugin) { setupDI(config) }
        with(Plugin) { setupDI(config) }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
        features.FEATURE_NAME.common.JVMPlugin.startPlugin(koin)
        Plugin.startPlugin(koin)
    }
}
```

The `Database` singleton required by `ExposedItemsRepo` is provided by `features.common.server.JVMPlugin` (which connects to some database), so `features/FEATURE_NAME/server` must be loaded after (or alongside) `features/common/server` in the plugin list.

---

## Bearer Auth Pattern (example: `features/auth`)

The `features/auth` feature implements end-to-end bearer-token authentication on top of Ktor's server bearer plugin (`io.ktor.server.auth.bearer`) and client bearer plugin (`io.ktor.client.plugins.auth.providers.bearer`). Reuse this pattern for any feature that needs token-based auth.

### Common module (`features/auth/common/commonMain`)

- Value classes for tokens and passwords:
  ```kotlin
  @Serializable @JvmInline value class Password(val string: String)
  @Serializable @JvmInline value class Token(val string: String)
  @Serializable @JvmInline value class RefreshToken(val string: String)
  ```
- Wire-format DTOs: `LoginRequest(username, password)`, `RefreshRequest(refreshToken)`, `AuthCredentials(token, refreshToken)` — used as JSON bodies on both sides.
- Shared interface: `AuthFeature.login(username, password)` and `AuthFeature.refresh(refreshToken)`. The same interface is implemented by `AuthFeatureService` (server) and `KtorAuthFeature` (client).
- Path constants (`Constants.prefixPathPart`, `Constants.loginPathPart`, `Constants.refreshPathPart`, `Constants.bearerAuthName`) shared by both routing and HTTP-client code.
- Depends on `features/users/common` for the `Username` value class.

### Server module (`features/auth/server`)

- **`PasswordsRepo : KeyValueRepo<Username, Password>`** in `commonMain/repo/`. The Exposed implementation in `jvmMain/repo/ExposedPasswordsRepo` wraps an `ExposedKeyValueRepo<String, String>` (table `users_passwords`, columns `username` text + `password_hash` text) via the `withMapper` extension from `dev.inmo.micro_utils.repos.mappers` to expose typed `KeyValueRepo<Username, Password>` to consumers.
- **`AuthFeatureService`** in `commonMain/services/` implements the common `AuthFeature`. Hashes passwords with `org.mindrot.jbcrypt.BCrypt` (`gensalt`/`hashpw`/`checkpw`), generates tokens via `com.benasher44.uuid.uuid4()`, and keeps two in-memory `MapKeyValueRepo` instances (`Token → Username`, `RefreshToken → Username`). Exposes a server-only `authenticate(token)` for the bearer validator and a server-only `setPassword(username, password)` for provisioning. Keep the in-memory token stores private to the service unless persistence is required.
- **`AuthRoutingsConfigurator : ApplicationRoutingConfigurator.Element`** registers `POST /<prefix>/login` and `POST /<prefix>/refresh`, returning `401 Unauthorized` when `AuthFeature` returns null.
- **`BearerAuthenticationConfigurator : ApplicationAuthenticationConfigurator.Element`** installs a Ktor `bearer(Constants.bearerAuthName) { authenticate { ... } }` block that maps a successful `authenticate(Token)` to `UserIdPrincipal(username.string)`. Protect routes with `authenticate(Constants.bearerAuthName) { ... }`.
- The common `Plugin.kt` registers the service, routing element, and bearer authentication element via `singleWithRandomQualifier`. The JVM `Plugin.kt` adds the Exposed-backed `PasswordsRepo` and re-uses the shared `Database` from `features/common/server`.
- `features/auth/server/build.gradle` adds `api libs.bcrypt`. `ktor-server-auth` is already transitive through `features/common/server`.

### Client module (`features/auth/client`)

- **`AuthCredentialsStorage`** is a tiny suspend-based `get`/`save` interface in `commonMain` with platform-specific implementations following the `ServerUrlStorage` pattern: `LocalStorageAuthCredentialsStorage` (JS), `PreferencesAuthCredentialsStorage` (JVM), `SharedPreferencesAuthCredentialsStorage` (Android). Each impl JSON-encodes `AuthCredentials` and guards with `SmartRWLocker`. Each platform plugin registers `single<AuthCredentialsStorage> { ... }`.
- **`KtorAuthFeature : AuthFeature`** wraps the shared `HttpClient`, posts JSON to `Constants.prefixPathPart/Constants.loginPathPart` and `.../Constants.refreshPathPart`, persists the resulting `AuthCredentials` via `AuthCredentialsStorage`, and returns `null` on non-success status.
- **`BearerAuthHttpClientConfigurator : HttpClientConfigurator`** installs Ktor's `Auth` plugin with a `bearer { ... }` provider:
  - `loadTokens` reads the storage and returns `BearerTokens(token, refreshToken)` (or null on first run).
  - `refreshTokens` posts `RefreshRequest` to the refresh endpoint **using the inner `client` parameter** (no Auth plugin → no recursion), persists the new credentials, and returns fresh `BearerTokens`. On non-success status, the storage is cleared and `null` is returned so the caller sees a 401 instead of looping.
  - `sendWithoutRequest` skips preemptive auth for the login/refresh endpoints (matches against `request.url.encodedPathSegments.joinToString("/")` ending with the auth paths).
- The common `Plugin.kt` registers `BearerAuthHttpClientConfigurator` as `singleWithRandomQualifier<HttpClientConfigurator>` so `features/common/client` picks it up when building the shared `HttpClient`. `KtorAuthFeature` is bound as the client-side `AuthFeature`.
- `features/auth/client/build.gradle` adds `api libs.ktor.client.auth`.

### Auto-refresh end-to-end

1. UI calls `AuthFeature.login(username, password)` (the `KtorAuthFeature` implementation). On success, credentials are persisted and the user is logged in.
2. Subsequent requests through the shared `HttpClient` get the bearer header attached automatically (Ktor calls `loadTokens` once and caches the result).
3. When the server returns `401 Unauthorized`, Ktor invokes `refreshTokens`, which hits `/auth/refresh` with the stored refresh token, persists new credentials, and retries the original request transparently.
4. If refresh fails, storage is wiped and the user must `login` again.

### Root-user bootstrap

`features/auth/server/JVMPlugin.startPlugin` runs a one-time bootstrap **after** delegating to `features/users/common/JVMPlugin` (so `UsersRepo` is in the DI graph): if `UsersRepo.count() == 0L`, it generates a 24-character alphanumeric password using `java.security.SecureRandom`, calls `UsersRepo.create(listOf(NewUser(Username("root"))))`, calls `AuthFeatureService.setPassword(...)` to bcrypt-hash the password, and prints the plaintext password once via the KSLog logger (`logger.i(...)`). On subsequent startups (any user already exists) the bootstrap is a no-op. This is why `auth/server/JVMPlugin` calls `with(features.users.common.JVMPlugin) { setupDI(config) }` and `users.common.JVMPlugin.startPlugin(koin)` itself — the auth feature owns the wiring of users into the server graph and a separate `users.server.JVMPlugin` entry in `sample.config.json` is not required (loading both is harmless because Koin de-duplicates `single` registrations within the same module).

---

## Auth UI Pattern (example: `features/ui/auth`)

The `features/ui/auth` UI feature combines the full-stack `features/auth` feature and the `features/ui/serverAddress` storage into a single login screen that is the root of the navigation chain. Reuse this pattern for any "gate" view that must run before the user reaches the rest of the app.

### Module dependencies (`features/ui/auth/build.gradle`)

```groovy
api project(":wishlist.features.common.client")
api project(":wishlist.features.auth.client")          // AuthFeature, AuthCredentialsStorage
api project(":wishlist.features.ui.serverAddress")     // ServerUrlStorage
```

UI features are allowed to depend on other UI features when one wraps the other's storage/interactors. Here `ui/auth` reuses `ServerUrlStorage` so the user can set the server address and credentials in one form.

### MVVM layout

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

### Platform-specific views

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
