# Pattern: MVVM (Client UI)

> Read together with the hard rules in `agents/CODING.md`. Reference implementations: `features/ui/wishlist` (multi-screen list + edit), `features/ui/sample` (scenario consuming a full-stack feature).

## Model

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

## Interactor

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

### Intra-feature navigation interactors (multi-screen feature)

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

Key rules:

- Each screen in a multi-screen feature gets its own `XxxViewInteractor` interface — one interactor per ViewModel, not one per feature.
- Method names describe the **user intent** (`onItemSelected`, `onCreateItem`, `onNavigateBack`, `onSaved`), not the widget action.
- The `node` parameter is always the **current screen's node** (`NavigationNode<ThisViewConfig, ViewConfig>`). Push new screens onto `node.chain`; pop via `node.chain.pop()`.
- For simple push/pop navigation, no Koin `get()` calls are needed inside the `single { }` block — the anonymous object body is stateless.
- Reserve reactive / stateful interactors (injecting `NavigationChain`, `CoroutineScope`, `MutableRedeliverStateFlow`) for cross-cutting concerns like auth overlay management — see `agents/patterns/auth-ui.md` for that pattern.

## ViewModel

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
- **Refresh-on-resume rule:** any data that can become stale while the user is away on another screen (pushed a child screen, opened an edit/create form, etc.) MUST be (re)loaded from a `node.onResumeFlow` subscription, NOT only in `init`. Combine `flowOf(Unit)` with `node.onResumeFlow` so the load runs on first show and on every resume:
  ```kotlin
  init {
      merge(flowOf(Unit), node.onResumeFlow).subscribeLoggingDropExceptions(scope) {
          _loadingState.value = true
          try {
              _itemsState.value = model.getItems()
          } finally {
              _loadingState.value = false
          }
      }
  }
  ```
  Data that never changes behind the screen's back (one-time config, static currency lists) may stay in a plain `init { scope.launchLoggingDropExceptions { ... } }`. Reference: `WishlistItemViewModel` / `WishlistItemCopyViewModel` `init` blocks.
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
- Registered in `Plugin.kt` as a Koin `factory` (not `single`) so each navigation node gets its own instance.

## ViewConfig

- A `@Serializable class` implementing `ViewConfig` (interface lives in `features/common/client/src/commonMain/kotlin/models/ViewConfig.kt`).
- Carries only the data needed to identify and initialize the screen (navigation parameters).

## View (platform-specific)

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

## Koin Registration Checklist (per UI feature)

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

**4. Interactor singleton** — NOT in the feature's `Plugin.kt`; bound in the top-level `client/` module's `ClientPlugin` (commonMain by default; platform-specific source set only when the implementation requires platform APIs) — see the Interactor section above.

**5. NavigationNodeFactory** — in each platform plugin (`JSPlugin.kt`, `JVMPlugin.kt`, `AndroidPlugin.kt`), which also delegates to the common `Plugin`:

```kotlin
singleWithRandomQualifier<NavigationNodeFactory<ViewConfig>> {
    NavigationNodeFactory.Typed<MyViewConfig, ViewConfig> { chain, config ->
        MyView(chain, config)
    }
}
```

**6. ComposeView** — no DI registration needed. Instantiated by the factory; injects the ViewModel lazily (see View section above).

## Localized Strings

Define all user-visible text in a `MyStrings` object using `dev.inmo.micro_utils.strings.buildStringResource`:

```kotlin
object MyStrings {
    val title = buildStringResource("English text") {
        IetfLang.Russian("Русский текст")
    }
}
```

Access in Compose: `MyStrings.title.translation()` (JS/JVM) or `MyStrings.title.translation(LocalResources.current)` (Android).

## Connecting a UI Scenario to a Full-Stack Feature

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
