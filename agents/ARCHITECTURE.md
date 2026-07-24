# Architecture

## Test Planning Requirement

After planning all architectural changes, this agent MUST produce test stubs or test specifications for every planned change:

- For each new or modified function/class/endpoint: describe what test cases must pass (inputs, expected outputs, edge cases).
- Test stubs must be written into the current step report file (`agents/task/<TASK_ID_FORMAT>/<STEP_NUMBER_FORMAT>.md`) so the Coding agent can implement them alongside the production code.
- If any planned functionality **cannot** be covered by automated tests (e.g. platform-specific UI rendering, external service integration with no stub possible): this MUST be explicitly flagged in the step report and raised with the operator **before proceeding to the Coding step**. Do not hand off to Coding until the operator has confirmed how untestable functionality should be handled.

## Feature README.md

See `ALL.md` for the full rule. Role-specific additions:
- This agent MUST NOT edit the README itself (file-edit restriction in `agents/ALL.md`). Instead: write the intended `## Architecture Notes` delta into the current step report, in a dedicated `## README updates` section — design decisions and their rationale, constraints or invariants, dependency notes. The Coding agent applies that delta together with the code changes.
- Never specify changes to the `## Operator Notes` section.

---

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

### DI Aggregation Across Features

A feature may contribute typed entries into a cross-feature aggregator without a direct module dependency, via the Koin `singleWithRandomQualifier` + `getAllDistinct` pattern (see `agents/CODING.md` "DI Aggregation"). This allows decoupled contributions: contributors register their entries with random qualifiers to avoid collisions, and the aggregator collects them all. Today this pattern powers two aggregators: `InternalApplicationRoutingConfigurator` for routes (in `features/common/server`) and `MapFeatureRolesRegistry` for role requirements (in `features/roles/common`).

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
            │    └─ Static file serving at site root `/` (from config.staticFolders)
            ├─ creates EmbeddedServer<Netty> with 32-thread pool
            │    └─ installs all KtorApplicationConfigurator instances
            │    └─ installs InternalApplicationRoutingConfigurator (wraps all feature routes under `/api`)
            │    └─ installs CallLogging (TRACE in debug, WARN in prod)
            └─ starts server (blocking)
```

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

---

## Access & Layering Rule (Full-Stack Feature)

Every full-stack feature is accessed through a single top-to-bottom chain of layers. Each layer talks only to the next one down. Syntax key: `[Notes] Name of part (place of registration in DI)`.

1. View (Client, Per target, target Plugin) →
2. ViewModel (Client, Common Plugin) →
3. Model (Client, Common Plugin) →
4. Client Feature (Client, Common Plugin) →
5. Ktor Client Feature (Client, Common Plugin) →
6. Server Ktor Routings Configurator (Server, JVM Plugin) →
7. Server Feature (Server, Common Plugin) →
8. Realization of Server Feature (Server, Common or JVM Plugin (depend on existence of platform-specific dependencies)) →
9. [if and which required] Repositories (Server, Common Plugin) →
10. (for each repository) [Optionally, but preferred] Fully cached repository (Server, Common Plugin) →
11. (for each repository) Exposed repository (Server, JVM Plugin)

**Each part MUST be declared in the same module where it is registered, always.**

### Exceptions & clarifications

- UI-only features (`features/ui/*`) have no server half (see the `features/ui` description above): the chain stops at Model, and the Model itself is optional.
- When the client and server feature interfaces are structurally identical, they may collapse into ONE interface placed in `features/FEATURE_NAME/common/commonMain` instead of two separate Client Feature / Server Feature hops (per `agents/patterns/full-stack-feature.md`).
- Ktor Client Feature is HTTP-only — no storage, no caching, no business rules (per `agents/CODING.md` "Ktor Client Realization Rule"). Any client-side caching/logic lives in the layer that wraps it: the Client Feature service bound as the feature interface.
- The fully cached repository (step 10) is the preferred default for CRUD repos — cache over Exposed, per `agents/patterns/crud-repo.md`. A non-CRUD / key-value repository may bind its Exposed repo directly, with no cache layer.

---

## Database

- **PostgreSQL** accessed via **Exposed** library  with JDBC driver.
- Connection established eagerly in `DatabaseConfig` data class constructor.
- A `VersionsRepo<Database>` tracks schema versions in a `tables_versions` key-value table (`table_name: String → version: Int`).
- For local dev, `server/docker-compose.yml` runs Postgres on port **8201** (user: `test`, password: `test`, db: `test`).
