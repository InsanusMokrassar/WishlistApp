# wishlist

## Usage of template

### Opportunities out-of-the-box

* **Fully working server** — Ktor (Netty, 32 threads) wired to **PostgreSQL** via Exposed ORM. Schema versioning (`tables_versions`) is included. A `docker-compose.yml` spins up the database for local development with a single command.
* **Three ready-to-launch clients** — Web (Compose HTML), Desktop (Compose for Desktop / JVM), and Android (Jetpack Compose + Material 3) all share one KMP codebase and compile from the same `client/` module.
* **Multiplatform MVVM architecture** — navigation (`dev.inmo:navigation.mvvm`), Koin DI, and the `ViewConfig / ViewModel / View / Model / Interactor` (in fact, MVVM) pattern are pre-wired across all three client targets.
* **Bearer-token authorization** — end-to-end login/refresh flow out of the box: BCrypt password hashing on the server, auto-refreshing bearer plugin on the client, platform-specific credential storage (`localStorage` / `Preferences` / `SharedPreferences`), and a root-user bootstrap on first startup.
* **Polymorphic serialization** — `kotlinx.serialization` is configured to collect all `SerializersModule` instances registered in Koin, so every feature's `@Serializable` types are automatically included without central registration.
* **Extendable server configurators** — add routes, auth providers, session types, error handlers, and cache headers by registering `ApplicationRoutingConfigurator.Element`, `ApplicationAuthenticationConfigurator.Element`, etc. into Koin; the server picks them all up automatically.
* **Extendable client HTTP configurators** — register `HttpClientConfigurator` instances into Koin to modify the shared `HttpClient` (base URL, auth headers, timeouts, logging) without rebuilding the client.
* **Plugin-based startup** — both server and clients boot via `StartLauncherPlugin`; features are isolated `StartPlugin` objects with two lifecycle phases (`setupDI` / `startPlugin`), making it straightforward to add, remove, or replace any feature.

## Run server with static distribution

> It is required to have installed:
> * JDK 17+
> * Docker engine
> * Docker compose (if you have old docker, for new one it is included in docker engine)

For running server you must build it:

```bash
#!/bin/bash

# In root of project
./gradlew :wishlist.server:build
```

Then in other terminal start postgres database:

```bash
#!/bin/bash

# In root of project
cd server
# or `sudo docker-compose up` for elder version od docker engine
sudo docker compose up
```

And then run server:

```bash
#!/bin/bash

# In root of project
# It is better to use full path to file with config
./gradlew :wishlist.server:run --args="sample.config.json"
```

Then you may open http://127.0.0.1:8196 to access web client
