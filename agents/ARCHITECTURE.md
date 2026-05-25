# Architecture

## Overview

This is a **Kotlin Multiplatform (KMP)** full-stack application template. The server runs on the JVM, and the client compiles to JavaScript (web), JVM (desktop), and Android. All three share common code via KMP source sets. The system is structured around a **plugin-based startup architecture** using the `dev.inmo:micro_utils.startup` library.

The root package is `dev.inmo.wishlist`. When forking this template, replace both `dev.inmo` and `wishlist` everywhere (package names, Gradle module names, config references).

---

## Technology Stack

| Layer | Technology |
|---|---|
| Language | Kotlin (Multiplatform) |
| Build system | Gradle (Groovy DSL) + TOML version catalog |
| Server framework | Ktor (Netty engine) |
| Database ORM | Exposed (JDBC) |
| Database | PostgreSQL |
| Web UI | Jetbrains Compose for Web (`compose.web.core`) |
| Desktop UI | Jetbrains Compose for Desktop |
| Android UI | Jetbrains Compose + Material 3 |
| Navigation | `dev.inmo:navigation.mvvm` |
| DI | Koin |
| Serialization | Kotlinx Serialization JSON |
| Logging | KSLog (multiplatform) + Logback (JVM) |
| Code generation | KSP (microutils generators: koin, sealed, classcasts) |
| JVM target | 17 |
| Docker base | Amazon Corretto 21 |

---

## Modules Structure

### Root

In the root there are three folders:

* `features` - multiplatform feature modules
* `server` - JVM-only server module. Launches whole app, contains files related to the entrypoint - sample config, `Dockerfile` for building server app image, etc.
* `client` - different clients

#### `features`

Contains features of project. On the start of project contains only `common` module.

##### `features/ui`

Contains UI-only feature modules. Each module under `features/ui/` implements a complete user scenario — a self-contained
screen or flow that users interact with directly. Unlike full-stack features, these modules have no server component: all
logic is client-side and expressed through the MVVM pattern (`ViewConfig`, `ViewModel`, `View`, and optionally a `Model`
for local or HTTP-based data access). Each module targets all three client platforms (`commonMain`, `jsMain`, `jvmMain`, `androidMain`)
in a single Gradle module.

Use `features/ui/` when a feature does not require new server routes and its purpose is entirely presentational or navigational.

Each module is split into three submodules:

* `common` - common things related to feature. Most part of logic must be placed here as soon as it is possible
* `server` - server realizations and bindings for feature to bring it up on the server
* `client` - clients-related things. For example, ktor client bindings

`common` and `client` modules contains next targets sources:

* `commonMain` must be used to contain as much logic as possible.
* `jvmMain` must contains jvm-specific realizations of actual functions and jvm-only logic. For example, exposed databases as variant of jvm-only logic
* `androidMain` must contains android-specific realizations of actual functions and android-only logic. For example, calls of some intents or permissions
* `jsMain` must contains js-specific realizations of actual functions and js-only logic. For example, bindings to browser or document

`server` module contains `commonMain` and `jvmMain` targets.

## Gradle Module Names

The root project is named `wishlist` in `settings.gradle`. Submodules use dotted paths. For example,
`features/common/common/` have name in gradle `:wishlist.features.common.common`

In `settings.gradle`, includes use the colon-path form (e.g. `:features:common:common`), which gets transformed to the dotted name automatically.

---

## Dependency Graph Between Modules

Each feature may depend on any other one. Modules of `features/common` CANNOT depend on any other internal module. `client`, `client/android`,
`server` should depend on each feature part. Dependencies rules:

* `client` -> `features/FEATURE_NAME/client`
* `server` -> `features/FEATURE_NAME/server`
* `client/android` -> `features/FEATURE_NAME/client`

---

## Feature adding rules

1. **Scaffold the feature** by running `./generate_feature.sh` and entering the feature name when prompted. AI AGENTS MUST RUN THIS SCRIPT WHEN THEY NEED TO CREATE FEATURE. This creates `features/FEATURE_NAME/common/`, `features/FEATURE_NAME/server/`, and `features/FEATURE_NAME/client/` with stub `build.gradle` and `Plugin.kt` files.

2. **Register modules in `settings.gradle`** — add the three generated submodules to the `includes` array:
   ```groovy
   ":features:FEATURE_NAME:common",
   ":features:FEATURE_NAME:server",
   ":features:FEATURE_NAME:client",
   ```

3. **Add dependencies** to each consumer module:
   - `server/build.gradle` → `api project(":wishlist.features.FEATURE_NAME.server")`
   - `client/build.gradle` (commonMain) → `api project(":wishlist.features.FEATURE_NAME.client")`
   - `client/android/build.gradle` (commonMain) → `api project(":wishlist.features.FEATURE_NAME.client")`

4. **Register the server plugin** — add the fully-qualified plugin object name to the `"plugins"` array in `server/sample.config.json` (or whichever config is in use):
   ```json
   "dev.inmo.wishlist.features.FEATURE_NAME.server.JVMPlugin"
   ```

5. **Register client plugins** — add the platform-specific plugin object to the plugin list in each client entry point:
   - `client/src/jsMain/kotlin/Main.kt` → `dev.inmo.wishlist.features.FEATURE_NAME.client.JSPlugin`
   - `client/src/jvmMain/kotlin/Main.kt` → `dev.inmo.wishlist.features.FEATURE_NAME.client.JVMPlugin`
   - `client/android/src/main/kotlin/MainActivity.kt` → `dev.inmo.wishlist.features.FEATURE_NAME.client.AndroidPlugin`

### Plugins (`StartPlugin` inheritors) note

You MUST NOT add new `setupDI` and `startPlugin` methods calls in plugins for the other plugins outside of feature. It is
permitted to call `setupDI` and `startPlugin` methods only within the same feature and only to the plugin with greater commonized meaning.
For example, `JVMPlugin` can't call `JSPlugin.setupDI`, but must call `Plugin.setupDI`.

### Full-Stack Feature Implementation

After scaffolding and registering the modules, write the actual implementation following `features/sample/` as a reference:

#### Common module (`features/FEATURE_NAME/common/`)

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

#### Server module (`features/FEATURE_NAME/server/`)

##### Common part (`commonMain/kotlin`)

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

##### JVM part (`jvmMain/kotlin`)

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

#### Client module (`features/FEATURE_NAME/client/`)

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

## UI Feature Adding Rules

1. **Scaffold the UI feature** by running `../generate_scenario.sh`. AI AGENTS MUST RUN THIS SCRIPT WHEN THEY NEED TO CREATE UI FEATURE.
It will prompt for two inputs:
   - **Module name** — starts with a lowercase letter (e.g. `myScreen`); becomes the directory name under `features/ui/`
   - **MVVM title** — starts with a capital letter (e.g. `MyScreen`); used as the class name prefix for `ViewModel`, `ViewConfig`, `View`, etc.

   This creates `features/ui/MODULE_NAME/` with `build.gradle` and stub Kotlin sources for `commonMain`, `jvmMain`, `jsMain`, and `androidMain`.

2. **Register the module in `settings.gradle`** — add to the `includes` array:
   ```groovy
   ":features:ui:MODULE_NAME",
   ```

3. **Add dependency in `client/build.gradle`** (commonMain only — Android gets it transitively through `client`):
   ```groovy
   api project(":wishlist.features.ui.MODULE_NAME")
   ```

4. **Register client plugins** — add the platform-specific plugin object to the plugin list in each client entry point:
   - `client/src/jsMain/kotlin/Main.kt` → `dev.inmo.wishlist.features.ui.MODULE_NAME.JSPlugin`
   - `client/src/jvmMain/kotlin/Main.kt` → `dev.inmo.wishlist.features.ui.MODULE_NAME.JVMPlugin`
   - `client/android/src/main/kotlin/MainActivity.kt` → `dev.inmo.wishlist.features.ui.MODULE_NAME.AndroidPlugin`

### UI MVVM Rules

#### Model

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

#### Interactor

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

#### ViewModel

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

#### ViewConfig

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

#### View (platform-specific)

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

#### Localized Strings

Define all user-visible text in a `MyStrings` object using `buildStringResource`:

```kotlin
object MyStrings {
    val title = buildStringResource("English text") {
        IetfLang.Russian("Русский текст")
    }
}
```

---

## Gradle Templates System

The `gradle/templates/` directory contains 24 reusable `.gradle` scripts. The `extensions.gradle` file at the root loads every template file path onto `project.ext` (key = filename without `.gradle` extension). Submodules reference them as `apply from: "$templateName"`.

### Key Templates

| Template | What it does |
|---|---|
| `defaultProject` | Base: stdlib, test deps, Java 17, `-Xcontext-parameters` compiler flag |
| `defaultProjectWithSerialization` | Applies `defaultProject` + adds `kotlinx-serialization-json` |
| `enableMPPJvm` | Adds JVM target (Java 17) |
| `enableMPPJs` | Adds JS/IR target (browser + Node) |
| `enableMPPAndroid` | Adds Android target + `defaultAndroidSettings` |
| `addCompose` | Adds `compose.runtime` to commonMain |
| `addComposeForJs` | Adds `compose.web.core` to jsMain |
| `addComposeForDesktop` | Adds `compose.desktop.currentOs` to jvmMain |
| `addComposeForAndroid` | Adds `compose.uiTest` to androidUnitTest |
| `mppJvmJs` | `defaultProjectWithSerialization` + JVM + JS |
| `mppJvmJsAndroid` | `defaultProjectWithSerialization` + JVM + JS + Android |
| `mppJvmJsAndroidWithCompose` | All 3 targets + Compose for all platforms |
| `mppJvmJsWithCompose` | JVM + JS + Compose for desktop and web |
| `mppJavaProject` | `defaultProject` + JVM only (server modules use this) |

### Choosing the Right Template

- **Feature `common/` module** (shared models, no UI): `mppJvmJsAndroid` (add `com.android.library` plugin)
- **Feature `server/` module** (JVM only, Ktor routes, Exposed tables): `mppJavaProject`
- **Feature `client/` module** (shared UI, Compose): `mppJvmJsAndroidWithCompose` (add `com.android.library` + compose plugins)
- **UI-only feature** (views, viewmodels, Compose): `mppJvmJsAndroidWithCompose` (add `com.android.library` + compose plugins)

---

## Plugin System

The entire application (both client and server) is initialized via `StartLauncherPlugin` from `dev.inmo:micro_utils.startup.launcher`. Each plugin implements `StartPlugin` with two lifecycle phases:

1. **`Module.setupDI(config: JsonObject)`** — registers dependencies into the Koin DI module.
2. **`suspend startPlugin(koin: Koin)`** — async startup using the built DI container.

### Plugin Composition Pattern

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

## Server Architecture

### Entry Point

The server has **no Kotlin source files** of its own. It uses `dev.inmo.micro_utils.startup.launcher.MainKt` as its main class (configured in `server/build.gradle`). This reads a JSON config file (first CLI argument), instantiates plugins by reflection, and runs them.

### Startup Flow

```
MainKt (microutils launcher)
  └─ reads config.json (first CLI arg)
  └─ instantiates plugins from "plugins" array by FQCN
  └─ For each plugin: setupDI(config), then startPlugin(koin)
       └─ features.common.server.JVMPlugin
            ├─ features.common.common.JVMPlugin → Plugin
            │    └─ registers: Json, CoroutineScope, ContentType
            ├─ features.common.server.Plugin (empty)
            ├─ parses Config & KtorConfig from JSON
            ├─ connects to PostgreSQL via Exposed
            ├─ creates VersionsRepo (tables_versions table)
            ├─ registers Ktor configurators:
            │    ├─ WebSockets (with JSON converter)
            │    ├─ ContentNegotiation (JSON)
            │    ├─ GZip compression (min 1024 bytes)
            │    ├─ Authentication (extensible)
            │    ├─ Sessions (extensible)
            │    ├─ StatusPages (extensible)
            │    ├─ CachingHeaders (extensible)
            │    └─ Static file serving (from config.staticFolders)
            ├─ creates EmbeddedServer<Netty> with 32-thread pool
            │    └─ installs all KtorApplicationConfigurator instances
            │    └─ installs ApplicationRoutingConfigurator
            │    └─ installs CallLogging (TRACE in debug, WARN in prod)
            └─ starts server (blocking)
```

### Adding Server Routes

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

### Server Configuration Model

`server/sample.config.json` (ready for local dev):

```json
{
  "host": "0.0.0.0",
  "port": 8196,
  "publicHost": "0.0.0.0",
  "staticFolders": {
    "": "../client/build/dist/js/developmentExecutable"
  },
  "database": {
    "url": "jdbc:postgresql://127.0.0.1:8201/test",
    "username": "test",
    "password": "test"
  },
  "plugins": [
    "dev.inmo.wishlist.features.common.server.JVMPlugin"
  ]
}
```

| Field | Type | Purpose |
|---|---|---|
| `host` | String | Ktor bind address (default `"0.0.0.0"`) |
| `port` | Int | Ktor bind port (default `8082` in model, `8196` in sample) |
| `publicHost` | String | Public hostname for link generation |
| `wss` | Boolean | Use `wss://` scheme for WebSocket links |
| `staticFolders` | Map<String, String> | URL path prefix → local directory |
| `database.url` | String | JDBC URL |
| `database.username` | String | DB username |
| `database.password` | String | DB password |
| `plugins` | List<String> | FQCNs of `StartPlugin` objects, loaded by reflection |

`DEBUG=true` environment variable switches Ktor into development mode and raises log verbosity to TRACE.

---

## Client Architecture (Web / Desktop / Android)

### Three Client Targets

The `client/` module is a KMP library targeting JS, JVM, and Android. Each target has its own entry point and platform plugin, but they all share `ClientPlugin` which initializes navigation.

| Target | Entry point | Platform plugin | Renders into |
|---|---|---|---|
| JS (Web) | `client/src/jsMain/kotlin/Main.kt` | `ClientJSPlugin` | `<div id="content">` via `renderComposable` |
| JVM (Desktop) | `client/src/jvmMain/kotlin/Main.kt` | `ClientJVMPlugin` | `Window` via Compose Desktop `application` |
| Android | `client/android/.../MainActivity.kt` | `ClientAndroidPlugin` | `setContent {}` via Jetpack Compose |

### Client Startup Flow (JS example)

```
Main.kt (JS)
  └─ window.addEventListener("load") → coroutine
       └─ StartLauncherPlugin.start(Config(listOf(
            ClientJSPlugin,                          // platform-specific client
            features.common.common.JSPlugin,         // Json, CoroutineScope
            features.common.client.JSPlugin,         // base navigation factories
            features.ui.sample.JSPlugin,             // SampleView factory
          )))
            └─ setupDI for each → builds Koin container
            └─ startPlugin for each:
                 └─ ClientPlugin.startPlugin(koin)
                      └─ initNavigation<ViewConfig>(EmptyConfig(), ...)
                      └─ InjectNavigationChain → InjectNavigationNode(SampleViewConfig())
                 └─ ClientJSPlugin.startPlugin(koin)
                      └─ renderComposable("content") { currentDrawingBlock }
```

### Plugin Chain Order (all platforms)

When adding a new feature, its client plugin must be added to the plugin list in **all three** entry points:

**JS** — `client/src/jsMain/kotlin/Main.kt`:
```kotlin
Config(listOf(
    ClientJSPlugin,
    dev.inmo.wishlist.features.common.common.JSPlugin,
    dev.inmo.wishlist.features.common.client.JSPlugin,
    dev.inmo.wishlist.features.sample.client.JSPlugin,
    dev.inmo.wishlist.features.auth.client.JSPlugin,

    dev.inmo.wishlist.features.ui.sample.JSPlugin,
    dev.inmo.wishlist.features.ui.serverAddress.JSPlugin,
    dev.inmo.wishlist.features.ui.auth.JSPlugin,
    // ADD NEW UI FEATURE JS PLUGINS HERE
))
```

**JVM** — `client/src/jvmMain/kotlin/Main.kt`:
```kotlin
Config(listOf(
    ClientJVMPlugin(appJob),
    dev.inmo.wishlist.features.common.common.JVMPlugin,
    dev.inmo.wishlist.features.common.client.JVMPlugin,
    dev.inmo.wishlist.features.sample.client.JVMPlugin,
    dev.inmo.wishlist.features.auth.client.JVMPlugin,

    dev.inmo.wishlist.features.ui.sample.JVMPlugin,
    dev.inmo.wishlist.features.ui.serverAddress.JVMPlugin,
    dev.inmo.wishlist.features.ui.auth.JVMPlugin,
    // ADD NEW UI FEATURE JVM PLUGINS HERE
))
```

**Android** — `client/android/src/main/kotlin/MainActivity.kt`:
```kotlin
Config(listOf(
    clientAndroidPlugin,
    dev.inmo.wishlist.features.common.common.AndroidPlugin,
    dev.inmo.wishlist.features.common.client.AndroidPlugin,
    dev.inmo.wishlist.features.sample.client.AndroidPlugin,
    dev.inmo.wishlist.features.auth.client.AndroidPlugin,

    dev.inmo.wishlist.features.ui.sample.AndroidPlugin,
    dev.inmo.wishlist.features.ui.serverAddress.AndroidPlugin,
    dev.inmo.wishlist.features.ui.auth.AndroidPlugin,
    // ADD NEW UI FEATURE ANDROID PLUGINS HERE
))
```

---

## Navigation / MVVM (Client)

The client uses a navigation stack from `dev.inmo.navigation.mvvm`.

| Type | Role |
|---|---|
| `ViewConfig` | Interface — serializable screen identifier. Lives in `features/common/client/src/commonMain/kotlin/models/ViewConfig.kt`. Every screen defines a `@Serializable class` implementing this. |
| `NavigationChain<ViewConfig>` | Navigation stack managed by the library |
| `NavigationNode<C, ViewConfig>` | One screen instance in the chain |
| `ViewModel<C>` | Business logic layer; extends `dev.inmo.navigation.mvvm.ViewModel<ViewConfig>`, exposes `StateFlow`s |
| `ComposeView<C, ViewConfig, VM>` | Compose UI per platform; extends `dev.inmo.navigation.mvvm.compose.ComposeView`, observes ViewModel |
| `SampleModel` | Interface for data access / business rules; injected into ViewModel |

### Koin Registration Pattern (UI Feature)

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

`SampleViewModel` calls `model.getSampleText()` once in `init`, then collects `model.serverStatusFlow()` in the same coroutine, merging both into a single `_textState` update on every emission.

**Dependency chain:**

```
features/ui/SCENARIO
  └─ depends on: features/FEATURE_NAME/client  (client interface + impl)
  └─ depends on: features/common/client        (base navigation, HttpClient)
       └─ builds HttpClient used by KtorMyFeature
```

---

## Database

- **PostgreSQL** accessed via **Exposed** library  with JDBC driver.
- Connection established eagerly in `DatabaseConfig` data class constructor.
- A `VersionsRepo<Database>` tracks schema versions in a `tables_versions` key-value table (`table_name: String → version: Int`).
- For local dev, `server/docker-compose.yml` runs Postgres on port **8201** (user: `test`, password: `test`, db: `test`).

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

## CRUD Repository Pattern (example: `features/users`)

This section documents the canonical pattern for adding a persistent, cache-backed CRUD repository to a feature, using `features/users` as the reference implementation.

### Model layer (`features/FEATURE_NAME/common/commonMain`)

Define three model types:

- **`Id`** — inline value class wrapping a primitive (used as the primary key):
  ```kotlin
  @Serializable @JvmInline value class UserId(val long: Long)
  ```
- **`NewObject`** — data sent on create (no id yet):
  ```kotlin
  @Serializable data class NewUser(override val username: Username) : User
  ```
- **`RegisteredObject`** — stored entity returned after create/read (carries the id):
  ```kotlin
  @Serializable data class RegisteredUser(val id: UserId, override val username: Username) : User
  ```

A sealed `User` interface must be used as the shared base for `NewUser` and `RegisteredUser`; fields common to both variants are declared there.

Auxiliary value types (e.g. `Username`) should also be inline value classes so they carry type-safety with zero runtime overhead:

```kotlin
@Serializable @JvmInline value class Username(val string: String)
```

### Repository interfaces (`features/FEATURE_NAME/common/commonMain`)

Split the repository into three interfaces:

```kotlin
// ReadUsersRepo.kt
interface ReadUsersRepo : ReadCRUDRepo<RegisteredUser, UserId>

// WriteUsersRepo.kt
interface WriteUsersRepo : WriteCRUDRepo<RegisteredUser, UserId, NewUser>

// UsersRepo.kt
interface UsersRepo : ReadUsersRepo, WriteUsersRepo, CRUDRepo<RegisteredUser, UserId, NewUser>
```

- `ReadCRUDRepo`, `WriteCRUDRepo`, and `CRUDRepo` are from `dev.inmo.micro_utils.repos`.
- Splitting read and write allows consumers that only need read access to depend only on `ReadUsersRepo`.

### Cache repository (`features/FEATURE_NAME/common/commonMain`)

Wrap the real repo with `FullCRUDCacheRepo` from `dev.inmo.micro_utils.repos.cache.full`:

```kotlin
class CacheUsersRepo(
    parentRepo: UsersRepo,
    scope: CoroutineScope,
    kvCache: KeyValueRepo<UserId, RegisteredUser> = MapKeyValueRepo(),
    locker: SmartRWLocker = SmartRWLocker()
) : UsersRepo, FullCRUDCacheRepo<RegisteredUser, UserId, NewUser>(
    crudRepo = parentRepo,
    kvCache = kvCache,
    scope = scope,
    skipStartInvalidate = false,
    locker = locker,
    idGetter = RegisteredUser::id
)
```

- `kvCache` defaults to an in-memory `MapKeyValueRepo`; swap for a persistent implementation if needed.
- `skipStartInvalidate = false` causes the cache to pre-fill from the DB on startup.
- `idGetter` is a function reference pointing to the id property of the registered type.

### Exposed (JVM) implementation (`features/FEATURE_NAME/common/jvmMain`)

Extend `AbstractExposedCRUDRepo` from `dev.inmo.micro_utils.repos.exposed`:

```kotlin
class ExposedUsersRepo(
    override val database: Database
) : UsersRepo, AbstractExposedCRUDRepo<RegisteredUser, UserId, NewUser>(tableName = "users") {

    private val idColumn = long("id").autoIncrement()
    private val usernameColumn = text("username").uniqueIndex()

    override val primaryKey = PrimaryKey(idColumn)

    override val ResultRow.asObject: RegisteredUser
        get() = RegisteredUser(
            id = UserId(get(idColumn)),
            username = Username(get(usernameColumn))
        )

    override val ResultRow.asId: UserId
        get() = UserId(get(idColumn))

    override val selectById: (UserId) -> Op<Boolean> = { idColumn.eq(it.long) }

    override fun update(id: UserId?, value: NewUser, it: UpdateBuilder<Int>) {
        it[usernameColumn] = value.username.string
    }

    override fun InsertStatement<Number>.asObject(value: NewUser): RegisteredUser =
        RegisteredUser(
            id = UserId(this[idColumn]),
            username = value.username
        )

    init { initTable() }
}
```

- `initTable()` (from `dev.inmo.micro_utils.repos.exposed`) runs `SchemaUtils.createMissingTablesAndColumns` inside a transaction on `init`.
- The `update` function is called for both insert-fill and explicit update paths; `id` is `null` during insert.
- `InsertStatement<Number>.asObject` constructs the registered object from the auto-generated id returned by the insert statement.

### DI wiring (`features/FEATURE_NAME/common/jvmMain — JVMPlugin`)

Register `ExposedUsersRepo` as a plain `single`, then wrap it in `CacheUsersRepo` and bind both `ReadUsersRepo` and `WriteUsersRepo` via `singleWithBinds`:

```kotlin
object JVMPlugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        with(Plugin) { setupDI(config) }

        single { ExposedUsersRepo(get()) }           // raw DB repo; only needed for cache wiring
        singleWithBinds<UsersRepo> {
            CacheUsersRepo(parentRepo = get<ExposedUsersRepo>(), scope = get())
        }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
        Plugin.startPlugin(koin)
    }
}
```

- `singleWithBinds` registers the `CacheUsersRepo` as `UsersRepo`, `ReadUsersRepo`, and `WriteUsersRepo` simultaneously — any consumer that injects any of those three interfaces gets the cache-backed instance.
- `ExposedUsersRepo` is registered separately so it can be retrieved by type when constructing `CacheUsersRepo`; consumers should never inject it directly.

### Server plugin wiring (`features/FEATURE_NAME/server/jvmMain — JVMPlugin`)

The server plugin delegates to the common JVM plugin so the repo is available in the DI graph:

```kotlin
object JVMPlugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        with(features.users.common.JVMPlugin) { setupDI(config) }
        with(Plugin) { setupDI(config) }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
        features.users.common.JVMPlugin.startPlugin(koin)
        Plugin.startPlugin(koin)
    }
}
```

The `Database` singleton required by `ExposedUsersRepo` is provided by `features.common.server.JVMPlugin` (which connects to some database), so `features/users/server` must be loaded after (or alongside) `features/common/server` in the plugin list.

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

### Wiring outside the feature

- `settings.gradle` includes the three submodules (`:features:auth:common`, `:features:auth:server`, `:features:auth:client`).
- `server/build.gradle` adds `api project(":wishlist.features.auth.server")` and `server/sample.config.json` adds `"dev.inmo.wishlist.features.auth.server.JVMPlugin"` to the `plugins` array (after `features.common.server.JVMPlugin`, which provides the `Database` singleton).
- `client/build.gradle` adds `api project(":wishlist.features.auth.client")`. Each client entry point (`client/src/jsMain/kotlin/Main.kt`, `client/src/jvmMain/kotlin/Main.kt`, `client/android/src/main/kotlin/MainActivity.kt`) appends the corresponding `features.auth.client.{JS,JVM,Android}Plugin` to its `Config(listOf(...))`.

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

### Wiring outside the feature

- `settings.gradle` adds `:features:ui:auth`.
- `client/build.gradle` adds `api project(":wishlist.features.ui.auth")`.
- Each client entry point (`client/src/jsMain/kotlin/Main.kt`, `client/src/jvmMain/kotlin/Main.kt`, `client/android/src/main/kotlin/MainActivity.kt`) appends `features.ui.auth.{JS,JVM,Android}Plugin` to its `Config(listOf(...))`.
- `client/src/commonMain/kotlin/ClientPlugin.kt` registers the `AuthViewInteractor` implementation (pushes `SampleViewConfig` onto the chain) and replaces the root `InjectNavigationNode(ServerAddressViewConfig())` with `InjectNavigationNode(AuthViewConfig())`.

### Login lifecycle

1. App boots → `ClientPlugin.startPlugin` initializes navigation with `InjectNavigationNode(SampleViewConfig())` as the root node. The reactive `AuthViewInteractor` subscription starts with `userLoggedIn=true`; no auth overlay is pushed immediately.
2. `AuthViewModel.init` checks `model.isAlreadyLoggedIn()`. If credentials are absent or invalid, `interactor.onUserLoggedOut()` is called → `userLoggedIn=false` → `AuthViewConfig` is pushed on top of `SampleViewConfig` in the root chain; the auth form becomes visible.
3. If credentials already exist, `interactor.onUserLoggedIn(node)` is called → `userLoggedIn=true` → auth nodes are dropped from the root chain; `SampleViewConfig` node (already present) is the visible screen; the auth form never renders.
4. Otherwise the user fills the visible fields. `loginEnabledState` controls the button.
5. On click, `onAuthorize` saves the server URL (so `DefaultUrlHttpClientConfigurator` and `BearerAuthHttpClientConfigurator` both pick it up), calls `AuthFeature.login`, persists the returned credentials via `AuthCredentialsStorage`, then calls `interactor.onUserLoggedIn(node)` → `userLoggedIn=true` → auth node dropped from root chain → `SampleViewConfig` becomes visible.
6. On any subsequent app start with stored credentials, step 3 short-circuits the form.
7. **Logout** — when `AuthCredentialsStorage.userAuthorised` becomes `false` (e.g. token refresh fails or credentials are cleared), the subscription started in `Plugin.startPlugin` calls `interactor.onUserLoggedOut()` → `userLoggedIn=false` → `AuthViewConfig` pushed onto root chain.
