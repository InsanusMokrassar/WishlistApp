# Pattern: Bearer Auth (example: `features/auth`)

> Read together with the hard rules in `agents/CODING.md`.

The `features/auth` feature implements end-to-end bearer-token authentication on top of Ktor's server bearer plugin (`io.ktor.server.auth.bearer`) and client bearer plugin (`io.ktor.client.plugins.auth.providers.bearer`). Reuse this pattern for any feature that needs token-based auth.

## Common module (`features/auth/common/commonMain`)

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

## Server module (`features/auth/server`)

- **`PasswordsRepo : KeyValueRepo<Username, Password>`** in `commonMain/repo/`. The Exposed implementation in `jvmMain/repo/ExposedPasswordsRepo` wraps an `ExposedKeyValueRepo<String, String>` (table `users_passwords`, columns `username` text + `password_hash` text) via the `withMapper` extension from `dev.inmo.micro_utils.repos.mappers` to expose typed `KeyValueRepo<Username, Password>` to consumers.
- **`AuthFeatureService`** in `commonMain/services/` implements the common `AuthFeature`. Hashes passwords with `org.mindrot.jbcrypt.BCrypt` (`gensalt`/`hashpw`/`checkpw`), generates tokens via `com.benasher44.uuid.uuid4()`, and keeps two in-memory `MapKeyValueRepo` instances (`Token → Username`, `RefreshToken → Username`). Exposes a server-only `authenticate(token)` for the bearer validator and a server-only `setPassword(username, password)` for provisioning. Keep the in-memory token stores private to the service unless persistence is required.
- **`AuthRoutingsConfigurator : ApplicationRoutingConfigurator.Element`** registers `POST /<prefix>/login` and `POST /<prefix>/refresh`, returning `401 Unauthorized` when `AuthFeature` returns null.
- **`BearerAuthenticationConfigurator : ApplicationAuthenticationConfigurator.Element`** installs a Ktor `bearer(Constants.bearerAuthName) { authenticate { ... } }` block that maps a successful `authenticate(Token)` to `UserIdPrincipal(username.string)`. Protect routes with `authenticate(Constants.bearerAuthName) { ... }`.
- The common `Plugin.kt` registers the service, routing element, and bearer authentication element via `singleWithRandomQualifier`. The JVM `Plugin.kt` adds the Exposed-backed `PasswordsRepo` and re-uses the shared `Database` from `features/common/server`.
- `features/auth/server/build.gradle` adds `api libs.bcrypt`. `ktor-server-auth` is already transitive through `features/common/server`.

## Client module (`features/auth/client`)

- **`AuthCredentialsStorage`** is a tiny suspend-based `get`/`save` interface in `commonMain` with platform-specific implementations following the `ServerUrlStorage` pattern (see `agents/patterns/local-storage.md`): `LocalStorageAuthCredentialsStorage` (JS), `PreferencesAuthCredentialsStorage` (JVM), `SharedPreferencesAuthCredentialsStorage` (Android). Each impl JSON-encodes `AuthCredentials` and guards with `SmartRWLocker`. Each platform plugin registers `single<AuthCredentialsStorage> { ... }`.
- **`KtorAuthFeature : AuthFeature`** wraps the shared `HttpClient`, posts JSON to `Constants.prefixPathPart/Constants.loginPathPart` and `.../Constants.refreshPathPart`, persists the resulting `AuthCredentials` via `AuthCredentialsStorage`, and returns `null` on non-success status.
- **`BearerAuthHttpClientConfigurator : HttpClientConfigurator`** installs Ktor's `Auth` plugin with a `bearer { ... }` provider:
  - `loadTokens` reads the storage and returns `BearerTokens(token, refreshToken)` (or null on first run).
  - `refreshTokens` posts `RefreshRequest` to the refresh endpoint **using the inner `client` parameter** (no Auth plugin → no recursion), persists the new credentials, and returns fresh `BearerTokens`. On non-success status, the storage is cleared and `null` is returned so the caller sees a 401 instead of looping.
  - `sendWithoutRequest` skips preemptive auth for the login/refresh endpoints (matches against `request.url.encodedPathSegments.joinToString("/")` ending with the auth paths).
- The common `Plugin.kt` registers `BearerAuthHttpClientConfigurator` as `singleWithRandomQualifier<HttpClientConfigurator>` so `features/common/client` picks it up when building the shared `HttpClient`. `KtorAuthFeature` is bound as the client-side `AuthFeature`.
- `features/auth/client/build.gradle` adds `api libs.ktor.client.auth`.

## Auto-refresh end-to-end

1. UI calls `AuthFeature.login(username, password)` (the `KtorAuthFeature` implementation). On success, credentials are persisted and the user is logged in.
2. Subsequent requests through the shared `HttpClient` get the bearer header attached automatically (Ktor calls `loadTokens` once and caches the result).
3. When the server returns `401 Unauthorized`, Ktor invokes `refreshTokens`, which hits `/auth/refresh` with the stored refresh token, persists new credentials, and retries the original request transparently.
4. If refresh fails, storage is wiped and the user must `login` again.

## Root-user bootstrap

`features/auth/server/JVMPlugin.startPlugin` runs a one-time bootstrap **after** delegating to `features/users/common/JVMPlugin` (so `UsersRepo` is in the DI graph): if `UsersRepo.count() == 0L`, it generates a 24-character alphanumeric password using `java.security.SecureRandom`, calls `UsersRepo.create(listOf(NewUser(Username("root"))))`, calls `AuthFeatureService.setPassword(...)` to bcrypt-hash the password, and prints the plaintext password once via the KSLog logger (`logger.i(...)`). On subsequent startups (any user already exists) the bootstrap is a no-op. This is why `auth/server/JVMPlugin` calls `with(features.users.common.JVMPlugin) { setupDI(config) }` and `users.common.JVMPlugin.startPlugin(koin)` itself — the auth feature owns the wiring of users into the server graph and a separate `users.server.JVMPlugin` entry in `sample.config.json` is not required (loading both is harmless because Koin de-duplicates `single` registrations within the same module).
