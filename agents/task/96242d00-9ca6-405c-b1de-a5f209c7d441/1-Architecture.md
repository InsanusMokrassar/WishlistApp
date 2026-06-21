Model: claude-sonnet-4-6
Execution time: 480
Tokens used: ~52000
Changed files: agents/task/96242d00-9ca6-405c-b1de-a5f209c7d441/1-Architecture.md

---

ENTITY:
entity_id=task/96242d00; type=architecture_step; state=complete
agent_id=ARCHITECTURE; branch=issue/45-deeplinks-redo

CONTEXT:
* task_id=96242d00-9ca6-405c-b1de-a5f209c7d441; prior_step=0-Planning.md; step=1-Architecture.md
* constraint=THIS_AGENT_MUST_NOT_EDIT_ANY_FILE_EXCEPT_THIS_FILE
* constraint=server_only_feature; client_module=scaffold_stubs_only
* route_decision=STANDARD_ApplicationRoutingConfigurator.Element; final_path=/api/links/{deeplinkId}; reason=InternalApplicationRoutingConfigurator wraps ALL Element registrations under /api prefix; no separate public-routing hook exists in codebase; orchestrator-confirmed decision

---

# ARCHITECTURE: Issue #45 — Deeplinks Feature

## ROUTE-PATH DECISION (ORCHESTRATOR-CONFIRMED)

DECISION:
* path=route_via_standard_ApplicationRoutingConfigurator.Element
* final_server_path=GET /api/links/{deeplinkId}
* mechanism=InternalApplicationRoutingConfigurator(getAllDistinct<ApplicationRoutingConfigurator.Element>()) wraps every Element registration under route("api") block
* evidence=features/common/server/src/jvmMain/kotlin/configurators/InternalApplicationRoutingConfigurator.kt:18-19 uses apiPathPart ("api") wrapping
* PROMPT says "links/<deeplink_uuid>" without /api prefix — treated as relative path segment, not absolute
* no_separate_top-level_routing_hook=confirmed; ApplicationRoutingConfigurator (not InternalApplicationRoutingConfigurator) handles static files separately
* auth=NONE; deeplink UUID is capability token; route sits outside authenticate{} block

---

## 1. MODULE STRUCTURE

```
features/deeplinks/
├── common/          gradle_name=:wishlist.features.deeplinks.common
│   └── src/
│       ├── commonMain/kotlin/dev.inmo.wishlist.features.deeplinks.common/
│       │   ├── Constants.kt
│       │   ├── Plugin.kt
│       │   └── models/
│       │       └── DeepLinkId.kt
│       ├── jvmMain/kotlin/.../JVMPlugin.kt
│       ├── jsMain/kotlin/.../JSPlugin.kt
│       └── androidMain/kotlin/.../AndroidPlugin.kt
├── server/          gradle_name=:wishlist.features.deeplinks.server
│   └── src/
│       ├── commonMain/kotlin/dev.inmo.wishlist.features.deeplinks.server/
│       │   ├── DeepLinkHandler.kt
│       │   ├── DeepLinksFeature.kt
│       │   ├── Plugin.kt
│       │   ├── configurators/
│       │   │   └── DeepLinksRoutingConfigurator.kt
│       │   ├── models/
│       │   │   └── StoredDeepLink.kt
│       │   ├── repo/
│       │   │   └── DeepLinksRepo.kt
│       │   └── services/
│       │       └── DeepLinksService.kt
│       └── jvmMain/kotlin/dev.inmo.wishlist.features.deeplinks.server/
│           ├── JVMPlugin.kt
│           └── repo/
│               └── ExposedDeepLinksRepo.kt
└── client/          gradle_name=:wishlist.features.deeplinks.client
    └── src/
        ├── commonMain/kotlin/.../Plugin.kt          (scaffold stub only)
        ├── jvmMain/kotlin/.../JVMPlugin.kt          (scaffold stub only)
        ├── jsMain/kotlin/.../JSPlugin.kt            (scaffold stub only)
        └── androidMain/kotlin/.../AndroidPlugin.kt  (scaffold stub only)
```

---

## 2. KOTLIN SIGNATURES

### 2a. features/deeplinks/common/src/commonMain/kotlin/

**Constants.kt**
```kotlin
package dev.inmo.wishlist.features.deeplinks.common

/** Path segment used by server routing and any external link builder. */
object DeepLinksConstants {
    /** Relative path segment for deep-link resolution routes: `links`. */
    const val linksPrefixPathPart = "links"
    /** Route parameter name for the deep-link identifier. */
    const val deeplinkIdPathParam = "deeplinkId"
}
```

**models/DeepLinkId.kt**
```kotlin
package dev.inmo.wishlist.features.deeplinks.common.models

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/**
 * Type-safe identifier for a deep link. Wraps a UUID string used as primary key in storage
 * and as the path segment in `/api/links/{deeplinkId}`.
 *
 * @param string UUID string value.
 */
@Serializable
@JvmInline
value class DeepLinkId(val string: String)
```

**Plugin.kt**
```kotlin
package dev.inmo.wishlist.features.deeplinks.common

import dev.inmo.micro_utils.startup.plugin.StartPlugin
import kotlinx.serialization.json.JsonObject
import org.koin.core.module.Module

/**
 * Common plugin for the deeplinks feature. No DI bindings — shared constants and models only.
 */
object Plugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {}
}
```

### 2b. features/deeplinks/common/src/jvmMain/kotlin/JVMPlugin.kt
```kotlin
package dev.inmo.wishlist.features.deeplinks.common

import dev.inmo.micro_utils.startup.plugin.StartPlugin
import kotlinx.serialization.json.JsonObject
import org.koin.core.Koin
import org.koin.core.module.Module

/**
 * JVM platform plugin for deeplinks common module. Delegates to [Plugin].
 */
object JVMPlugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        with(Plugin) { setupDI(config) }
    }
    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
        Plugin.startPlugin(koin)
    }
}
```

### 2c. features/deeplinks/common/src/jsMain/kotlin/JSPlugin.kt
```kotlin
package dev.inmo.wishlist.features.deeplinks.common

import dev.inmo.micro_utils.startup.plugin.StartPlugin
import kotlinx.serialization.json.JsonObject
import org.koin.core.Koin
import org.koin.core.module.Module

/**
 * JS platform plugin for deeplinks common module. Delegates to [Plugin].
 */
object JSPlugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        with(Plugin) { setupDI(config) }
    }
    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
        Plugin.startPlugin(koin)
    }
}
```

### 2d. features/deeplinks/common/src/androidMain/kotlin/AndroidPlugin.kt
```kotlin
package dev.inmo.wishlist.features.deeplinks.common

import dev.inmo.micro_utils.startup.plugin.StartPlugin
import kotlinx.serialization.json.JsonObject
import org.koin.core.Koin
import org.koin.core.module.Module

/**
 * Android platform plugin for deeplinks common module. Delegates to [Plugin].
 */
object AndroidPlugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        with(Plugin) { setupDI(config) }
    }
    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
        Plugin.startPlugin(koin)
    }
}
```

### 2e. features/deeplinks/server/src/commonMain/kotlin/

**DeepLinkHandler.kt**
```kotlin
package dev.inmo.wishlist.features.deeplinks.server

import dev.inmo.wishlist.features.deeplinks.common.models.DeepLinkId

/**
 * Handler for a specific deep link type. Consuming features register implementations via
 * `singleWithRandomQualifier<DeepLinkHandler>` in their Koin module.
 *
 * Each handler is called in sequence when a deep link is resolved. Return `true` to signal
 * the link was handled (stops iteration); return `false` to defer to the next handler.
 */
fun interface DeepLinkHandler {
    /**
     * Attempts to handle the deep link.
     *
     * @param deeplinkId Identifier of the resolved deep link.
     * @param handlerInfo Deserialized handler-info object. Handler should check type before processing.
     * @return `true` if handled and processing should stop; `false` to pass to next handler.
     */
    suspend fun tryHandle(deeplinkId: DeepLinkId, handlerInfo: Any): Boolean
}
```

**DeepLinksFeature.kt**
```kotlin
package dev.inmo.wishlist.features.deeplinks.server

import dev.inmo.wishlist.features.deeplinks.common.models.DeepLinkId

/**
 * Server-only capability contract for the deeplinks feature.
 * Not exposed to client modules.
 */
interface DeepLinksFeature {
    /**
     * Persists a new deep link with the given handler info and returns its identifier.
     *
     * @param handlerInfo Serializable handler-info object; must be registered in the shared
     *   [kotlinx.serialization.json.Json]'s `SerializersModule` before calling.
     * @return Newly created [DeepLinkId] (UUID string).
     */
    suspend fun createDeepLink(handlerInfo: Any): DeepLinkId

    /**
     * Resolves a deep link by invoking registered [DeepLinkHandler]s in order.
     *
     * @param deeplinkId Identifier of the deep link to resolve.
     * @return `true` if any handler reported success; `false` if unknown or no handler matched.
     */
    suspend fun resolveDeepLink(deeplinkId: DeepLinkId): Boolean
}
```

**models/StoredDeepLink.kt**
```kotlin
package dev.inmo.wishlist.features.deeplinks.server.models

import dev.inmo.wishlist.features.deeplinks.common.models.DeepLinkId
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Persisted record for a deep link. Stores handler info as opaque [JsonElement] so the
 * server module has no compile-time knowledge of concrete handler-info types.
 *
 * @param id Unique identifier of this deep link.
 * @param handlerInfo Polymorphic JSON payload produced by `Json.encodeToJsonElement(PolymorphicSerializer(Any::class), ...)`.
 */
@Serializable
data class StoredDeepLink(
    val id: DeepLinkId,
    val handlerInfo: JsonElement
)
```

**repo/DeepLinksRepo.kt**
```kotlin
package dev.inmo.wishlist.features.deeplinks.server.repo

import dev.inmo.micro_utils.repos.KeyValueRepo
import dev.inmo.wishlist.features.deeplinks.common.models.DeepLinkId
import dev.inmo.wishlist.features.deeplinks.server.models.StoredDeepLink

/**
 * Persistence contract for deep links. Key = [DeepLinkId], Value = [StoredDeepLink].
 * JVM implementation: [ExposedDeepLinksRepo].
 */
interface DeepLinksRepo : KeyValueRepo<DeepLinkId, StoredDeepLink>
```

**services/DeepLinksService.kt**
```kotlin
package dev.inmo.wishlist.features.deeplinks.server.services

import com.benasher44.uuid.uuid4
import dev.inmo.wishlist.features.deeplinks.common.models.DeepLinkId
import dev.inmo.wishlist.features.deeplinks.server.DeepLinkHandler
import dev.inmo.wishlist.features.deeplinks.server.DeepLinksFeature
import dev.inmo.wishlist.features.deeplinks.server.models.StoredDeepLink
import dev.inmo.wishlist.features.deeplinks.server.repo.DeepLinksRepo
import dev.inmo.micro_utils.repos.set
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.json.Json

/**
 * Default implementation of [DeepLinksFeature]. Encodes handler info using
 * [PolymorphicSerializer] with `Any::class` so the consuming feature's serializer
 * (registered in the shared [Json]'s `SerializersModule`) handles type discrimination.
 *
 * @param json Shared [Json] instance from `features/common/common`; must include the
 *   consuming feature's [kotlinx.serialization.modules.SerializersModule].
 * @param repo Persistence layer for stored deep links.
 * @param handlers All registered [DeepLinkHandler]s collected via `getAllDistinct()`.
 */
class DeepLinksService(
    private val json: Json,
    private val repo: DeepLinksRepo,
    private val handlers: List<DeepLinkHandler>
) : DeepLinksFeature {

    override suspend fun createDeepLink(handlerInfo: Any): DeepLinkId {
        val id = DeepLinkId(uuid4().toString())
        val encoded = json.encodeToJsonElement(PolymorphicSerializer(Any::class), handlerInfo)
        repo.set(id, StoredDeepLink(id, encoded))
        return id
    }

    override suspend fun resolveDeepLink(deeplinkId: DeepLinkId): Boolean {
        val stored = repo.get(deeplinkId) ?: return false
        val handlerInfo = json.decodeFromJsonElement(PolymorphicSerializer(Any::class), stored.handlerInfo)
        return handlers.any { it.tryHandle(deeplinkId, handlerInfo) }
    }
}
```

**configurators/DeepLinksRoutingConfigurator.kt**
```kotlin
package dev.inmo.wishlist.features.deeplinks.server.configurators

import dev.inmo.micro_utils.ktor.server.configurators.ApplicationRoutingConfigurator
import dev.inmo.wishlist.features.deeplinks.common.DeepLinksConstants
import dev.inmo.wishlist.features.deeplinks.server.DeepLinksFeature
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

/**
 * Registers the deep link resolution route as an [ApplicationRoutingConfigurator.Element].
 * Final server path (via [dev.inmo.wishlist.features.common.server.configurators.InternalApplicationRoutingConfigurator]):
 * `GET /api/links/{deeplinkId}`
 *
 * No authentication required — the UUID itself is a capability token.
 * Missing/blank `deeplinkId` → 400 Bad Request.
 * Handler matched → 200 OK.
 * Not found or no handler matched → 404 Not Found.
 *
 * @param feature Business logic delegate.
 */
class DeepLinksRoutingConfigurator(
    private val feature: DeepLinksFeature
) : ApplicationRoutingConfigurator.Element {
    override fun Route.invoke() {
        route(DeepLinksConstants.linksPrefixPathPart) {
            get("{${DeepLinksConstants.deeplinkIdPathParam}}") {
                val rawId = call.parameters[DeepLinksConstants.deeplinkIdPathParam]
                    .takeUnless { it.isNullOrBlank() }
                    ?: run {
                        call.respond(HttpStatusCode.BadRequest)
                        return@get
                    }
                val id = dev.inmo.wishlist.features.deeplinks.common.models.DeepLinkId(rawId)
                val handled = feature.resolveDeepLink(id)
                call.respond(if (handled) HttpStatusCode.OK else HttpStatusCode.NotFound)
            }
        }
    }
}
```

**Plugin.kt** (server/commonMain)
```kotlin
package dev.inmo.wishlist.features.deeplinks.server

import dev.inmo.micro_utils.koin.getAllDistinct
import dev.inmo.micro_utils.koin.singleWithRandomQualifier
import dev.inmo.micro_utils.ktor.server.configurators.ApplicationRoutingConfigurator
import dev.inmo.micro_utils.startup.plugin.StartPlugin
import dev.inmo.wishlist.features.deeplinks.server.configurators.DeepLinksRoutingConfigurator
import dev.inmo.wishlist.features.deeplinks.server.services.DeepLinksService
import kotlinx.serialization.json.JsonObject
import org.koin.core.Koin
import org.koin.core.module.Module

/**
 * Server-common plugin for the deeplinks feature. Registers [DeepLinksService],
 * [DeepLinksFeature] binding, and [DeepLinksRoutingConfigurator].
 *
 * Requires [dev.inmo.wishlist.features.deeplinks.server.repo.DeepLinksRepo] to be
 * registered before use (supplied by [JVMPlugin] for JVM targets).
 */
object Plugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        single {
            DeepLinksService(
                json = get(),
                repo = get(),
                handlers = getAllDistinct()
            )
        }
        single<DeepLinksFeature> { get<DeepLinksService>() }
        singleWithRandomQualifier<ApplicationRoutingConfigurator.Element> {
            DeepLinksRoutingConfigurator(get())
        }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
    }
}
```

### 2f. features/deeplinks/server/src/jvmMain/kotlin/

**repo/ExposedDeepLinksRepo.kt**
```kotlin
package dev.inmo.wishlist.features.deeplinks.server.repo

import dev.inmo.micro_utils.repos.KeyValueRepo
import dev.inmo.micro_utils.repos.exposed.keyvalue.ExposedKeyValueRepo
import dev.inmo.micro_utils.repos.mappers.withMapper
import dev.inmo.wishlist.features.deeplinks.common.models.DeepLinkId
import dev.inmo.wishlist.features.deeplinks.server.models.StoredDeepLink
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.jdbc.Database

/**
 * Builds the [KeyValueRepo] delegate backed by table `deeplinks` (columns: `deeplink_id` TEXT,
 * `data_json` TEXT). Uses [ExposedKeyValueRepo] + [withMapper] pattern from
 * [dev.inmo.wishlist.features.files.common.repo.ExposedFilesMetaInfoRepo].
 *
 * [ExposedKeyValueRepo] → [ExposedReadKeyValueRepo] calls `initTable()` in its own `init {}`;
 * no additional `initTable()` call needed in this class.
 *
 * @param database Exposed [Database] from Koin (provided by `features/common/server`).
 * @param json Shared [Json] for encoding/decoding [StoredDeepLink].
 */
private fun createDelegate(database: Database, json: Json): KeyValueRepo<DeepLinkId, StoredDeepLink> =
    ExposedKeyValueRepo<String, String>(
        database = database,
        keyColumnAllocator = { text("deeplink_id") },
        valueColumnAllocator = { text("data_json") },
        tableName = "deeplinks"
    ).withMapper<DeepLinkId, StoredDeepLink, String, String>(
        keyFromToTo = { string },
        valueFromToTo = { json.encodeToString(StoredDeepLink.serializer(), this) },
        keyToToFrom = { DeepLinkId(this) },
        valueToToFrom = { json.decodeFromString(StoredDeepLink.serializer(), this) }
    )

/**
 * JVM Exposed JDBC implementation of [DeepLinksRepo]. Stores each [StoredDeepLink] as
 * a JSON string in the `deeplinks` table. Schema-flexible: adding fields to [StoredDeepLink]
 * or the embedded [kotlinx.serialization.json.JsonElement] requires no table migration.
 *
 * @param database Exposed [Database] injected from Koin.
 * @param json Shared [Json] injected from Koin.
 */
class ExposedDeepLinksRepo(
    database: Database,
    json: Json
) : DeepLinksRepo, KeyValueRepo<DeepLinkId, StoredDeepLink> by createDelegate(database, json)
```

**JVMPlugin.kt** (server/jvmMain)
```kotlin
package dev.inmo.wishlist.features.deeplinks.server

import dev.inmo.micro_utils.startup.plugin.StartPlugin
import dev.inmo.wishlist.features.deeplinks.server.repo.ExposedDeepLinksRepo
import kotlinx.serialization.json.JsonObject
import org.koin.core.Koin
import org.koin.core.module.Module
import dev.inmo.wishlist.features.deeplinks.server.repo.DeepLinksRepo

/**
 * JVM entry-point plugin for the deeplinks server module. Adds [ExposedDeepLinksRepo]
 * binding on top of the common [Plugin] registrations.
 *
 * Requires `features/common/server.JVMPlugin` to be loaded first (provides [Database] + [Json]).
 * Requires `features/deeplinks/common.JVMPlugin` to be loaded first (provides common models).
 */
object JVMPlugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        with(dev.inmo.wishlist.features.deeplinks.common.JVMPlugin) { setupDI(config) }
        with(Plugin) { setupDI(config) }
        single<DeepLinksRepo> { ExposedDeepLinksRepo(get(), get()) }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
        dev.inmo.wishlist.features.deeplinks.common.JVMPlugin.startPlugin(koin)
        Plugin.startPlugin(koin)
    }
}
```

### 2g. features/deeplinks/client/src/ — SCAFFOLD STUBS ONLY

All four files (Plugin.kt, JSPlugin.kt, JVMPlugin.kt, AndroidPlugin.kt) follow empty-delegation pattern. No business logic. No additional imports beyond StartPlugin pattern. Coding agent verifies scaffold output matches; manual creation only if generate_feature.sh fails.

---

## 3. BUILD.GRADLE CHANGES

### features/deeplinks/common/build.gradle
```groovy
plugins {
    id "org.jetbrains.kotlin.multiplatform"
    id "org.jetbrains.kotlin.plugin.serialization"
    id "com.android.library"
}

apply from: "$mppJvmJsAndroid"

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api project(":wishlist.features.common.common")
            }
        }
    }
}
```

### features/deeplinks/server/build.gradle
```groovy
plugins {
    id "org.jetbrains.kotlin.multiplatform"
    id "org.jetbrains.kotlin.plugin.serialization"
}

apply from: "$mppJavaProject"

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api project(":wishlist.features.deeplinks.common")
                api project(":wishlist.features.common.server")
            }
        }
    }
}
```
NOTE: `uuid4` transitive via `features/common/common`; `ExposedKeyValueRepo`/`withMapper` transitive via `features/common/server`; no extra explicit deps needed.

### features/deeplinks/client/build.gradle
* template=mppJvmJsAndroidWithCompose (scaffold output from generate_feature.sh)
* commonMain deps: `api project(":wishlist.features.deeplinks.common")`, `api project(":wishlist.features.common.client")`
* Coding agent verifies compose plugins present in scaffold output

---

## 4. SETTINGS.GRADLE CHANGES

FILE: settings.gradle
INSERT after `:features:booking:client` line:
```groovy
":features:deeplinks:common",
":features:deeplinks:server",
":features:deeplinks:client",
```

---

## 5. SERVER/BUILD.GRADLE CHANGES

FILE: server/build.gradle
ADD in dependencies block:
```groovy
api project(":wishlist.features.deeplinks.server")
```

---

## 6. SERVER/SAMPLE.CONFIG.JSON CHANGES

FILE: server/sample.config.json
ADD as last element in `"plugins"` array:
```json
"dev.inmo.wishlist.features.deeplinks.server.JVMPlugin"
```
REASON: must load AFTER `features/common/server.JVMPlugin` (provides `Database` + `Json`); append-last satisfies ordering.

---

## 7. CLIENT WIRING

### client/build.gradle
ADD in commonMain dependencies:
```groovy
api project(":wishlist.features.deeplinks.client")
```

### client/src/jsMain/kotlin/Main.kt
ADD after `dev.inmo.wishlist.features.booking.client.JSPlugin`:
```kotlin
dev.inmo.wishlist.features.deeplinks.client.JSPlugin,
```

### client/src/jvmMain/kotlin/Main.kt
ADD after `dev.inmo.wishlist.features.booking.client.JVMPlugin`:
```kotlin
dev.inmo.wishlist.features.deeplinks.client.JVMPlugin,
```

### client/android/src/main/kotlin/MainActivity.kt
ADD after `dev.inmo.wishlist.features.booking.client.AndroidPlugin`:
```kotlin
dev.inmo.wishlist.features.deeplinks.client.AndroidPlugin,
```

---

## 8. DI WIRING SUMMARY

| Binding | Type | Registered by | Collected/resolved by |
|---|---|---|---|
| `DeepLinksRepo` | `single<DeepLinksRepo>` | `deeplinks.server.JVMPlugin` (jvmMain) | `DeepLinksService` constructor via `get()` |
| `DeepLinksService` | `single` | `deeplinks.server.Plugin` (commonMain) | — (implementation only) |
| `DeepLinksFeature` | `single<DeepLinksFeature>` | `deeplinks.server.Plugin` | consuming features via `get<DeepLinksFeature>()` |
| `ApplicationRoutingConfigurator.Element` (routing) | `singleWithRandomQualifier` | `deeplinks.server.Plugin` | `InternalApplicationRoutingConfigurator(getAllDistinct())` in `features/common/server.JVMPlugin` |
| `DeepLinkHandler` (0..N) | `singleWithRandomQualifier` | consuming feature plugins | `DeepLinksService(handlers = getAllDistinct())` |
| `SerializersModule` (handler-info types) | `singleWithRandomQualifier` | consuming feature plugins | `features/common/common.Plugin` Json builder via `getAllDistinct<SerializersModule>()` |
| `Json` | `single` | `features/common/common.Plugin` | `DeepLinksService` constructor via `get()` |
| `Database` | `single` | `features/common/server.JVMPlugin` | `ExposedDeepLinksRepo` constructor via `get()` |

---

## 9. POLYMORPHIC HANDLER-INFO REGISTRATION (CONSUMING FEATURE GUIDE)

RULE — consuming feature registers handler-info type + handler in its own Koin plugin:

```kotlin
// In consuming feature's Plugin.setupDI:
singleWithRandomQualifier {
    SerializersModule {
        polymorphic(Any::class, MyHandlerInfo::class, MyHandlerInfo.serializer())
    }
}
singleWithRandomQualifier<DeepLinkHandler> {
    MyDeepLinkHandler(/* deps */)
}
```

MECHANISM:
* `features/common/common.Plugin` builds shared `Json` with `useArrayPolymorphism = true`
* `Json.serializersModule` aggregates ALL `singleWithRandomQualifier<SerializersModule>` instances via `getAllDistinct<SerializersModule>().forEach { include(it) }`
* `DeepLinksService.createDeepLink`: encodes via `PolymorphicSerializer(Any::class)` → produces `["dev.foo.MyHandlerInfo", { ... }]` (array format per `useArrayPolymorphism=true`)
* `DeepLinksService.resolveDeepLink`: decodes same array via `PolymorphicSerializer(Any::class)` → calls each `DeepLinkHandler.tryHandle(id, decoded)`
* handler checks type: `if (handlerInfo !is MyHandlerInfo) return false`

RISK R3 (from plan): consuming feature MUST register `SerializersModule` before `Json` is built (Koin lazy eval handles ordering naturally). If not registered → `SerializationException` at runtime on encode/decode.

---

## 10. EXPOSED TABLE SCHEMA

Table name: `deeplinks`
Columns:
* `deeplink_id` TEXT — PK; stores [DeepLinkId.string] (UUID format, e.g. `"550e8400-e29b-41d4-a716-446655440000"`)
* `data_json` TEXT — stores JSON-encoded [StoredDeepLink] (full record incl. `id` + `handlerInfo` JsonElement)

Primary key: `PrimaryKey(keyColumn, valueColumn)` — inherited from `ExposedReadKeyValueRepo`; both columns form composite PK.
`initTable()` called automatically in `ExposedReadKeyValueRepo.init {}` — no separate call needed in `ExposedDeepLinksRepo`.

---

## 11. SYMBOL VERIFICATION (micro_utils)

| Symbol | Package | Verified source path |
|---|---|---|
| `ExposedKeyValueRepo` | `dev.inmo.micro_utils.repos.exposed.keyvalue` | `MicroUtils/repos/exposed/src/jvmMain/kotlin/dev/inmo/micro_utils/repos/exposed/keyvalue/ExposedKeyValueRepo.kt` |
| `withMapper` (KeyValueRepo) | `dev.inmo.micro_utils.repos.mappers` | `MicroUtils/repos/common/src/commonMain/kotlin/dev/inmo/micro_utils/repos/mappers/KeyValueMappers.kt:246` |
| `KeyValueRepo` | `dev.inmo.micro_utils.repos` | `MicroUtils/repos/common/src/commonMain/kotlin/dev/inmo/micro_utils/repos/KeyValueRepo.kt` |
| `set(k, v)` extension | `dev.inmo.micro_utils.repos` | `MicroUtils/repos/common/src/commonMain/kotlin/dev/inmo/micro_utils/repos/KeyValueRepo.kt:144` |
| `singleWithRandomQualifier` | `dev.inmo.micro_utils.koin` | `MicroUtils/koin/src/commonMain/kotlin/SingleWithRandomQualifier.kt` |
| `getAllDistinct` | `dev.inmo.micro_utils.koin` | `MicroUtils/koin/src/commonMain/kotlin/GetAllDistinct.kt` |
| `ApplicationRoutingConfigurator.Element` | `dev.inmo.micro_utils.ktor.server.configurators` | `MicroUtils/ktor/server/src/jvmMain/kotlin/dev/inmo/micro_utils/ktor/server/configurators/ApplicationRoutingConfigurator.kt:13` |
| `StartPlugin` | `dev.inmo.micro_utils.startup.plugin` | `MicroUtils/startup/plugin/src/commonMain/kotlin/StartPlugin.kt` |
| `ExposedKeyValueRepo` constructor param | `keyColumnAllocator: ColumnAllocator<Key>` | verified signature in ExposedKeyValueRepo.kt:17-24 |

NOTE: `ExposedKeyValueRepo` constructor uses named params `keyColumnAllocator`/`valueColumnAllocator` (NOT `keyCol`/`valueCol` as plan stated). Corrected in architecture signatures above.

---

## 12. PLAN FLAWS FOUND

FLAW F1 (MINOR — CORRECTED HERE):
* location=0-Planning.md §2e `features/deeplinks/server/src/commonMain/kotlin/configurators/DeepLinksRoutingsConfigurator.kt`
* plan_said=`route_pattern=GET links/{deeplinkId} (NOT under /api — it is a public route)`
* actual=route IS under /api via InternalApplicationRoutingConfigurator; orchestrator confirmed STANDARD Element; plan body contradicts itself (recommendation said to use ApplicationRoutingConfigurator.Element which does go under /api)
* architecture_decision=use STANDARD ApplicationRoutingConfigurator.Element; final path=GET /api/links/{deeplinkId}; corrected in 1-Architecture.md

FLAW F2 (MINOR — CORRECTED HERE):
* location=0-Planning.md §2e ExposedDeepLinksRepo
* plan_said=`keyCol={ text("deeplink_id") }` and `valueCol={ text("data_json") }`
* actual=constructor params named `keyColumnAllocator` and `valueColumnAllocator` (verified in ExposedKeyValueRepo.kt:17-24)
* architecture_decision=use correct param names in ExposedDeepLinksRepo.createDelegate()

FLAW F3 (MINOR — NOTE ONLY):
* location=0-Planning.md §2e DeepLinksRoutingsConfigurator file name
* plan_said=`DeepLinksRoutingsConfigurator` (plural "Routings")
* architecture_decision=renamed to `DeepLinksRoutingConfigurator` (singular "Routing") to match existing pattern: `SampleRoutingsConfigurator` uses plural but this is inconsistent across codebase; CODING agent may use either — note for consistency.
* NOTE: plural "Routings" matches sample feature convention; CODING agent should use `DeepLinksRoutingsConfigurator` to match.

FLAW F4 (MINOR — CORRECTED):
* location=0-Planning.md §2b DeepLinksService imports
* plan_said=`import dev.inmo.micro_utils.repos.set`
* actual=`set(key, value)` extension is in package `dev.inmo.micro_utils.repos` (verified KeyValueRepo.kt:144); import is correct; no correction needed

---

## 13. REPORT TO ORCHESTRATOR

ENTITY:
entity_id=1-Architecture.md; type=step_output; state=complete; location=agents/task/96242d00-9ca6-405c-b1de-a5f209c7d441/1-Architecture.md

FINALIZED_FILE_LIST:
* features/deeplinks/common/build.gradle — template=mppJvmJsAndroid; dep=wishlist.features.common.common
* features/deeplinks/common/src/commonMain/kotlin/Constants.kt — object DeepLinksConstants; linksPrefixPathPart="links"; deeplinkIdPathParam="deeplinkId"
* features/deeplinks/common/src/commonMain/kotlin/models/DeepLinkId.kt — @Serializable @JvmInline value class DeepLinkId(val string: String)
* features/deeplinks/common/src/commonMain/kotlin/Plugin.kt — object Plugin : StartPlugin (empty)
* features/deeplinks/common/src/jvmMain/kotlin/JVMPlugin.kt — delegates to Plugin
* features/deeplinks/common/src/jsMain/kotlin/JSPlugin.kt — delegates to Plugin
* features/deeplinks/common/src/androidMain/kotlin/AndroidPlugin.kt — delegates to Plugin
* features/deeplinks/server/build.gradle — template=mppJavaProject; deps=deeplinks.common+common.server
* features/deeplinks/server/src/commonMain/kotlin/DeepLinkHandler.kt — fun interface; tryHandle(DeepLinkId, Any): Boolean
* features/deeplinks/server/src/commonMain/kotlin/DeepLinksFeature.kt — interface; createDeepLink(Any): DeepLinkId; resolveDeepLink(DeepLinkId): Boolean
* features/deeplinks/server/src/commonMain/kotlin/models/StoredDeepLink.kt — @Serializable data class; id: DeepLinkId; handlerInfo: JsonElement
* features/deeplinks/server/src/commonMain/kotlin/repo/DeepLinksRepo.kt — interface : KeyValueRepo<DeepLinkId, StoredDeepLink>
* features/deeplinks/server/src/commonMain/kotlin/services/DeepLinksService.kt — class(json, repo, handlers); implements DeepLinksFeature
* features/deeplinks/server/src/commonMain/kotlin/configurators/DeepLinksRoutingsConfigurator.kt — implements ApplicationRoutingConfigurator.Element; route=links/{deeplinkId}; no auth
* features/deeplinks/server/src/commonMain/kotlin/Plugin.kt — registers DeepLinksService+DeepLinksFeature+DeepLinksRoutingsConfigurator
* features/deeplinks/server/src/jvmMain/kotlin/repo/ExposedDeepLinksRepo.kt — class(Database, Json); delegates to createDelegate(); table=deeplinks
* features/deeplinks/server/src/jvmMain/kotlin/JVMPlugin.kt — adds single<DeepLinksRepo> on top of common Plugin
* features/deeplinks/client/* — scaffold stubs only (Plugin+JSPlugin+JVMPlugin+AndroidPlugin)
* settings.gradle — add 3 submodule entries after booking:client
* server/build.gradle — add deeplinks.server dep
* server/sample.config.json — add deeplinks.server.JVMPlugin as last plugin
* client/build.gradle — add deeplinks.client dep
* client/src/jsMain/kotlin/Main.kt — add JSPlugin after booking.client.JSPlugin
* client/src/jvmMain/kotlin/Main.kt — add JVMPlugin after booking.client.JVMPlugin
* client/android/src/main/kotlin/MainActivity.kt — add AndroidPlugin after booking.client.AndroidPlugin

FLAWS_FOUND:
* F1=route_path_plan_self-contradiction; corrected=STANDARD_Element_path=/api/links/{deeplinkId}; blocker=false
* F2=ExposedKeyValueRepo_constructor_param_names_wrong_in_plan; corrected_in_architecture; blocker=false
* F3=file_name_singular/plural_inconsistency; recommendation=use_plural_DeepLinksRoutingsConfigurator_to_match_SampleRoutingsConfigurator; blocker=false
* F4=no_actual_flaw_in_import; cleared

NEXT_STEP:
* action=pass_to_CODING_agent; input=1-Architecture.md; step_file_to_write=2-Coding.md
* coding_agent_must_run=generate_feature.sh FIRST; then implement per §2-§7 above

VALIDATION:
* format_valid=true; no_pronouns=true; entities_explicit=true; high_density=true; causal_chain_present=true; ambiguity_detected=false
