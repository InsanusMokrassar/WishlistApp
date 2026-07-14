# Pattern: Per-Client Local Storage + Stored-URL HttpClient Plumbing

> Read together with the hard rules in `agents/CODING.md`. Reference implementation: `features/ui/serverAddress` (`ServerUrlStorage`).

## Single-platform local storage interface (e.g. `ServerUrlStorage`)

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

## `HttpClientConfigurator` driven by stored URL

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
