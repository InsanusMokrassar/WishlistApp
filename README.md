# WishlistApp

A multiplatform wishlist application: users create wishlists, fill them with items
(price, priority, images), and share them so other people can see what to gift.
Server and all three clients (Web, Desktop, Android) are built from a single Kotlin
Multiplatform codebase.

## Functionality

- **Accounts** — registration and login with bearer-token auth (auto-refreshing tokens,
  BCrypt password hashing on the server). A `root` user is bootstrapped on first server
  start; its generated password is printed once to the server log.
- **Wishlists** — each user owns wishlists. Owners can create, rename, and delete their
  own wishlists; edit controls are hidden for non-owners.
- **Items** — every wishlist holds items with a title, description, price, a priority
  (Low / Medium / High / Custom weight), and one or more images. Owners create, edit, and
  delete items.
- **Browsing other users** — wishlists are publicly readable. You can open another user's
  profile, view their wishlists, and view an aggregated "all items" screen across all of
  their wishlists.
- **Sorting** — items on a wishlist (and on the all-items screen) can be sorted by Cost,
  Priority, or Title; the default keeps the stored order.
- **Images** — items support image upload/preview; files are stored on the server and
  served back to all clients.
- **Admin panel** — the `root` user gets an admin panel for CRUD over users and wishlists.

## Supported platforms

| Platform | UI toolkit | How it ships |
|----------|------------|--------------|
| Web      | Compose HTML (Kotlin/JS) | Static JS bundle served by the server |
| Desktop  | Compose for Desktop (JVM) | Run from the `:wishlist.client` JVM target |
| Android  | Jetpack Compose + Material 3 | Installed APK (`:wishlist.client.android`) |
| Server   | Ktor (Netty) + PostgreSQL via Exposed | JVM application |

## Architecture (short)

- **Backend** — Ktor on Netty, PostgreSQL accessed through the Exposed ORM.
- **Plugin-based startup** — server and clients boot via `StartLauncherPlugin`; each feature
  is an isolated `StartPlugin` with `setupDI` / `startPlugin` lifecycle phases. Server plugins
  are listed in the config JSON and loaded by reflection; client plugins are listed in each
  client's `Main` entry point.
- **Client MVVM** — `dev.inmo:navigation.mvvm` + Koin DI, with the
  `ViewConfig / ViewModel / View / Model / Interactor` pattern shared across all three clients.

See `agents/CODING.md` for the full coding conventions and feature patterns.

## Prerequisites

- JDK 17+
- Docker engine + Docker Compose (for the PostgreSQL database)

## Running the server (with the web client)

The server also serves the compiled web client as static files, so a single `run` brings up
both the API and the Web UI.

1. Start the PostgreSQL database (config expects it on `127.0.0.1:8501`):

   ```bash
   # from the project root
   cd server
   docker compose up        # use `docker-compose up` on older Docker
   ```

2. Run the server. The `run` task automatically builds the web client's development bundle
   first (the config serves it from `../client/build/dist/js/developmentExecutable`):

   ```bash
   # from the project root
   ./gradlew :wishlist.server:run --args="sample.config.json"
   ```

3. Open <http://127.0.0.1:8196> for the Web client.

On first start, watch the server log for the generated `root` password.

### Server configuration

The server takes a single argument: the path to a config JSON (working directory is the
`server/` module, so `sample.config.json` resolves to `server/sample.config.json`). Key fields
in `server/sample.config.json`:

| Field | Meaning |
|-------|---------|
| `host` / `port` | bind address and port (default `8196`) |
| `publicHost` | host advertised to clients |
| `staticFolders` | static content roots (serves the web client bundle) |
| `database` | JDBC `url`, `username`, `password` for PostgreSQL |
| `plugins` | fully-qualified server feature plugins loaded by reflection |
| `filesFolder` | directory for uploaded item images |
| `tokenTtl` / `refreshTokenTtl` | bearer / refresh token lifetimes (ISO-8601 durations) |
| `enableRegistration` | allow new-user registration |

## Running the Desktop client

The desktop client is the JVM target of `:wishlist.client`. Its entry point is
`dev.inmo.wishlist.client.MainKt` (`client/src/jvmMain/kotlin/Main.kt`), launched with the
Compose for Desktop runtime — run it from your IDE's run configuration for that `main`.
Set the server address in the client's login screen.

## Running the Android client

```bash
# from the project root, with a device/emulator connected
./gradlew :wishlist.client.android:installDebug
```

Then launch the installed app and set the server address on the login screen.
