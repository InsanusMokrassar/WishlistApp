# Pattern: Full-Stack Feature Implementation

> Read together with the hard rules in `agents/CODING.md`. Scaffolding/registration steps: see `agents/ARCHITECTURE.md` "Feature adding rules".

After scaffolding and registering the modules, write the actual implementation following `features/sample/` as a reference:

## Common module (`features/FEATURE_NAME/common/`)

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

## Server module (`features/FEATURE_NAME/server/`)

### Common part (`commonMain/kotlin`)

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

### JVM part (`jvmMain/kotlin`)

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

## Client module (`features/FEATURE_NAME/client/`)

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

> **Ktor realization rule** applies — see `agents/CODING.md` "Ktor Client Realization Rule": `KtorXxxFeature` classes only call HTTP endpoints; any extra logic lives in a wrapping service.

## Cross-cutting feature inside `features/common`

When a feature is a tiny cross-cutting helper rather than a domain-level capability (example: an `echo`/`status` ping endpoint plus client wrapper), prefer hosting it as a sub-package inside `features/common/{common,client,server}` instead of creating a new top-level feature module. Concretely:

- Put shared constants/path parts under `features/common/common/src/commonMain/kotlin/<topic>/` (e.g. `echo/Constants.kt`).
- Put server interface, services and routing configurator under `features/common/server/src/commonMain/kotlin/<topic>/` and register them inside `features/common/server/.../Plugin.kt` (the existing one — extend it, don't create a parallel plugin).
- Put client interface and Ktor-based implementation under `features/common/client/src/commonMain/kotlin/<topic>/` and register them inside `features/common/client/.../Plugin.kt`.

Because `features/common` is loaded by every consumer, no extra wiring in `settings.gradle` / `Main.kt` / `sample.config.json` is needed.
