# WishlistApp

A multiplatform wishlist application: users create wishlists, fill them with items
(price, priority, images), and share them so other people can see what to gift.
Server and all three clients (Web, Desktop, Android) are built from a single Kotlin
Multiplatform codebase.

## Functionality

- **Accounts** — registration and login with bearer-token auth (auto-refreshing tokens,
  BCrypt password hashing on the server). When required-email registration is enabled, new
  accounts receive a verification invite and remain `NewUser` until the deeplink is opened;
  verified accounts receive the `User` role. A `root` user is bootstrapped on first server
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

### Timestamp rules

Application timestamps representing instants use `korlibs.time.DateTime`. Serializable DateTime
properties use `dev.inmo.micro_utils.common.DateTimeSerializer`. Durations, monotonic elapsed
measurements, IDs, counters, sizes, and monetary values retain their appropriate existing types.
Primitive epoch values are permitted only at explicit checked storage or external-format boundaries.
JSON timestamps remain numeric epoch milliseconds; the serializer writes Double numbers, so consumers
must not depend on integer lexical formatting. Existing nullable history stays null. Durable email
lifecycle instants are finite whole milliseconds in the inclusive range
`[-4503599627370496, 4503599627370496]`; conversion outside that range fails without rewriting data.

See `agents/CODING.md` for the full coding conventions and feature patterns.

## Prerequisites

- JDK 17 for development and the full Gradle build
- Android SDK for Android compilation (see [Development and test environment](#development-and-test-environment))
- Docker engine for the optional disposable PostgreSQL test path; Docker Compose for the local Mailpit SMTP inbox

The served Web browser gate supports Linux runners with JDK 17 and the Chromium
runtime libraries installed. Ubuntu is the supported CI baseline. On Ubuntu, install
the same libraries used by CI before running the gate: `libnss3`, `libnspr4`,
`libdbus-1-3`, `libatk1.0-0`, `libatk-bridge2.0-0`, `libatspi2.0-0`,
`libxcomposite1`, `libxdamage1`, `libxfixes3`, `libxrandr2`, `libgbm1`,
`libasound2t64`, `libcups2`, `libpango-1.0-0`, `libcairo2`, `libx11-6`, `libxcb1`,
`libxext6`, and `libxkbcommon0`.

## Development and test environment

### JDK, Android SDK, and browsers

From the repository root, select a JDK 17 installation in the current shell and check
that Gradle sees it:

```bash
export JAVA_HOME=/path/to/jdk-17
export PATH="$JAVA_HOME/bin:$PATH"
java -version
./gradlew --version
```

For Android compilation, install the Android SDK platform **37** and Build Tools
**37.0.0**, matching `android-compileSdk` and `android-buildTools` in
`gradle/libs.versions.toml`, and accept the SDK licenses. Set `ANDROID_HOME` to
that SDK in the current shell, or put `sdk.dir=/path/to/android-sdk` in an
untracked root `local.properties`. Check the installed packages with the SDK's
`sdkmanager --list_installed`; install missing packages with that SDK's
`sdkmanager` and run `sdkmanager --licenses` as needed. Use your own paths, not another developer's home directory.

The served [browser gate](#served-web-browser-verification) downloads and validates
its own Playwright-managed Chromium; it does not require `CHROME_BIN`. Kotlin/JS
`jsBrowserTest` is a separate browser-unit task and may require a locally installed
Chrome/Chromium and `CHROME_BIN` pointing to its executable. Passing browser-unit
tests is not a substitute for the served gate. See the Chromium runtime-library
prerequisites above and the browser cache/artifact guidance below.

### Disposable PostgreSQL for integration tests

Use a **separate, throwaway database**, never the application or production database.
PostgreSQL 18 is a known-working test version. The Docker path also requires a
PostgreSQL `psql` client installed on the host to verify the published TCP port.
If Docker is available, choose an unused localhost port (55488 is only an
example; check it before use), then start one temporary named container from
the repository root. Run this in Bash and keep the same shell for the URLs and
tests below:

```bash
PG_TEST_PORT=55488
PG_TEST_PASSWORD='wishlist_tests_only'
PG_TEST_CONTAINER="wishlist-tests-$$"
PG_TEST_READY=0
# Check that 127.0.0.1:$PG_TEST_PORT is unused before continuing.
start_docker_test_postgres() {
  command -v psql >/dev/null || { printf 'Install a host PostgreSQL psql client first.\n' >&2; return 1; }
  docker run --rm -d --name "$PG_TEST_CONTAINER" \
    -e POSTGRES_USER=wishlist_tests -e POSTGRES_PASSWORD="$PG_TEST_PASSWORD" \
    -e POSTGRES_DB=wishlist_tests -e POSTGRES_INITDB_ARGS='--auth-host=scram-sha-256' \
    -p "127.0.0.1:${PG_TEST_PORT}:5432" postgres:18 >/dev/null || return 1
  local attempt
  for attempt in {1..30}; do
    if [[ $(docker inspect -f '{{.State.Running}}' "$PG_TEST_CONTAINER" 2>/dev/null) != true ]]; then
      printf 'Test PostgreSQL container exited before readiness; do not run tests.\n' >&2
      return 1
    fi
    if docker exec "$PG_TEST_CONTAINER" pg_isready -h 127.0.0.1 -U wishlist_tests -d wishlist_tests >/dev/null 2>&1; then
      if PGPASSWORD="$PG_TEST_PASSWORD" psql -h 127.0.0.1 -p "$PG_TEST_PORT" \
        -U wishlist_tests -d wishlist_tests -c 'SELECT 1'; then
        PG_TEST_READY=1
        return 0
      fi
      printf 'Published TCP authentication failed; do not run tests.\n' >&2
      docker stop "$PG_TEST_CONTAINER" >/dev/null 2>&1 || true
      return 1
    fi
    sleep 1
  done
  printf 'Test PostgreSQL did not become ready in 30 attempts; do not run tests.\n' >&2
  docker stop "$PG_TEST_CONTAINER" >/dev/null 2>&1 || true
  return 1
}
start_docker_test_postgres || printf 'Fix the setup failure before exporting URLs or running Gradle.\n' >&2
```

`pg_isready` checks readiness, while the `psql` command proves an authenticated
TCP connection. Stop only this container when finished:
`docker stop "$PG_TEST_CONTAINER"` (`--rm` removes it). The example password is disposable;
do not reuse it for an application database.

If Docker is unavailable, use **already installed, compatible** `initdb`, `pg_ctl`,
`createdb`, `pg_isready`, and `psql` binaries. The following Bash commands keep
the cluster, socket, password file, and log in one private temporary directory;
choose and check an unused localhost port first. If the binaries are absent,
install PostgreSQL through your normal platform tooling before following this
path; this repository does not bundle them.

```bash
PG_TEST_PORT=55488
PG_TEST_PASSWORD='wishlist_tests_only'
PG_TEST_READY=0
start_local_test_postgres() {
  PG_TEST_TMP=$(mktemp -d) || return 1  # private, mode 0700
  mkdir "$PG_TEST_TMP/socket" || return 1
  printf '%s\n' "$PG_TEST_PASSWORD" > "$PG_TEST_TMP/password" || return 1
  chmod 600 "$PG_TEST_TMP/password" || return 1
  initdb -D "$PG_TEST_TMP/data" --username=wishlist_tests \
    --auth-local=scram-sha-256 --auth-host=scram-sha-256 \
    --pwfile="$PG_TEST_TMP/password" || return 1
  pg_ctl -D "$PG_TEST_TMP/data" \
    -o "-h 127.0.0.1 -p $PG_TEST_PORT -k $PG_TEST_TMP/socket" \
    -l "$PG_TEST_TMP/server.log" -w start || return 1
  PGPASSWORD="$PG_TEST_PASSWORD" createdb -h 127.0.0.1 -p "$PG_TEST_PORT" \
    -U wishlist_tests wishlist_tests || return 1
  pg_isready -h 127.0.0.1 -p "$PG_TEST_PORT" -U wishlist_tests -d wishlist_tests || return 1
  PGPASSWORD="$PG_TEST_PASSWORD" psql -h 127.0.0.1 -p "$PG_TEST_PORT" \
    -U wishlist_tests -d wishlist_tests -c 'SELECT 1' || return 1
  PG_TEST_READY=1
}
start_local_test_postgres || printf 'Fix the setup failure before exporting URLs or running Gradle.\n' >&2
```

This uses SCRAM authentication even on the private socket; localhost binding
alone would not restrict other local users. After the tests, stop the cluster
with `pg_ctl -D "$PG_TEST_TMP/data" -m fast -w stop`, then remove **only the
directory you created** at `PG_TEST_TMP` after verifying its
path. If startup fails after `pg_ctl`, stop only this cluster before cleaning
up its directory; do not continue to `createdb` or tests. Do not change system
services, socket permissions, or user groups for this test setup; no `sudo`
workaround is needed.

Set both test URLs in the same shell before the Gradle commands below. They may
point to the same disposable database. The test user needs `CREATE SCHEMA` and
`DROP SCHEMA` permissions; test fixtures own their generated schemas. The JDBC
URL must include an explicit `user` (omitting it caused a PostgreSQL startup
failure), and a `password` when SCRAM is used. URL-encode non-example credentials
when placing them in query parameters. Keep real URLs out of shared diagnostics.

```bash
unset WISHLIST_TEST_POSTGRES_URL WISHLIST_POSTGRES_TEST_JDBC_URL
if [[ ${PG_TEST_READY:-0} == 1 ]]; then
  export WISHLIST_TEST_POSTGRES_URL="jdbc:postgresql://127.0.0.1:${PG_TEST_PORT}/wishlist_tests?user=wishlist_tests&password=${PG_TEST_PASSWORD}"
  export WISHLIST_POSTGRES_TEST_JDBC_URL="$WISHLIST_TEST_POSTGRES_URL"
else
  printf 'PostgreSQL setup failed; do not run Gradle tests.\n' >&2
fi
```

`WISHLIST_TEST_POSTGRES_URL` is required by the deeplink and email password
PostgreSQL tests included in `build`.
`WISHLIST_POSTGRES_TEST_JDBC_URL` is required by the separate
`:wishlist.features.users.common:postgresEmailLifecycleTest` task, which
`build` does **not** run.

### Verification commands and evidence

Run these from the repository root after the environment is ready. This Bash
helper preserves each Gradle exit code when using `tee`; in Bash capture
`${PIPESTATUS[0]}` immediately after the pipeline (or capture `$?` immediately
with `pipefail`). In zsh, use its corresponding `pipestatus[1]` instead.
The `&&` sequence stops at the first failed gate, including a failed setup
guard; fix that failure before rerunning later commands. Do not infer success
from `tee` or an old XML file. The `local.*` log names are ignored by Git.

```bash
set -o pipefail
run_gradle() {
  local log_file=$1
  shift
  "$@" 2>&1 | tee "$log_file"
  local gradle_status=${PIPESTATUS[0]}
  printf 'Gradle exit status: %s\n' "$gradle_status"
  return "$gradle_status"
}
if [[ ${PG_TEST_READY:-0} == 1 && -n ${WISHLIST_TEST_POSTGRES_URL:-} && -n ${WISHLIST_POSTGRES_TEST_JDBC_URL:-} ]] &&
  run_gradle local.build.log ./gradlew --no-parallel build --console=plain &&
  run_gradle local.postgres-lifecycle.log ./gradlew --no-parallel \
    :wishlist.features.users.common:postgresEmailLifecycleTest --console=plain &&
  run_gradle local.deeplinks-postgres.log ./gradlew --no-parallel \
    :wishlist.features.deeplinks.common:jvmTest \
    --tests '*ExposedDeepLinksRepoTest.requiredPostgresIndependentConnectionsSelectOneWinnerAndPreserveReplacement' --console=plain &&
  run_gradle local.email-postgres.log ./gradlew --no-parallel \
    :wishlist.features.email.server:jvmTest --tests '*EmailPasswordChangePostgresTest' --console=plain &&
  run_gradle local.email-commit.log ./gradlew --no-parallel \
    :wishlist.features.email.server:jvmTest --tests '*EmailPasswordChangeCommitTest' --console=plain &&
  run_gradle local.browser.log ./gradlew browserTest --console=plain; then
  printf 'All requested gates passed.\n'
else
  printf 'Setup or a gate failed; later gates were not run.\n' >&2
fi
```

After stopping only the test container or cluster you started as described
above, clear both JDBC URLs and the disposable password from this shell:

```bash
unset WISHLIST_TEST_POSTGRES_URL WISHLIST_POSTGRES_TEST_JDBC_URL \
  PG_TEST_PASSWORD PGPASSWORD PG_TEST_READY
```

If `build` stops before any tests execute, run `./gradlew --no-parallel allTests
--console=plain` after fixing the prerequisite. `UP-TO-DATE` is not fresh test
execution; add `--rerun-tasks` selectively to a relevant integration task when
fresh proof is needed. A filtered `jvmTest` can replace that task's previous XML
results, so save any needed evidence locally before the next filtered run. Do
not describe a mixed tree of old and new XML as one fresh suite result. Logs
may contain a full JDBC URL, and browser `server.log` may contain a generated
root password; redact those before sharing. Tooling unpacked under `/tmp` is
not a durable developer installation.

### Dependency and setup failures

For npm, Yarn, or Maven HTTP 502 errors, check the proxy/network policy first.
Only if direct networking is permitted, try a **per-command** proxy bypass:

```bash
env -u HTTP_PROXY -u HTTPS_PROXY -u ALL_PROXY \
  -u http_proxy -u https_proxy -u all_proxy NO_PROXY='*' \
  ./gradlew --no-parallel build --console=plain
```

Gradle JVM settings and npm configuration can set proxies separately; inspect
them if the per-command environment is insufficient. Do not globally change
proxy settings, disable TLS, wipe caches, or blindly upgrade dependencies.
Missing JDBC variables fail the required PostgreSQL tests; a JDBC URL without
`user` can fail at connection startup. If Docker is unavailable, use the
installed-binary path above. For Android SDK version mismatches, compare with
the catalog values above. For Chromium runtime-library or cache failures, use
the prerequisites and cache-repair behavior in
[Served Web browser verification](#served-web-browser-verification), then
inspect its failure artifacts.

## Served Web browser verification

Run the real-browser smoke suite with the Gradle wrapper:

```bash
./gradlew browserTest
```

The gate builds the development Web bundle, provisions the Playwright-managed
Chromium revision pinned by the Gradle catalog (`com.microsoft.playwright:playwright`
1.52.0), starts the Ktor server on a loopback ephemeral port, and runs eight JUnit
tests: two served Chromium smoke tests and six focused browser-response classifier
tests. The smoke tests check the rendered application, then register a unique
disposable account and check authenticated wishlist controls.
The default is headless. Use `./gradlew browserTest -PbrowserHeaded=true` to watch
the run locally.

Every invocation creates its own temporary SQLite database, upload directory,
server configuration, and port. Cleanup stops the server and removes that temporary
state; the operator's database and files under `server/src` are not used. Browser
installation is cached at `browserTests/build/playwright`. Each gate invocation
launches the pinned headless browser to validate that cache before the test suite;
an empty or partial pinned cache is repaired automatically, while unrelated browser
revisions are preserved. Repeating the command does not reinstall a healthy matching
browser. Unrelated Gradle tasks do not provision a browser or start the test server.

The browser gate treats every console error, page exception, and HTTP 401 as a
failure. Anonymous startup must emit zero Playwright `onRequest` events for
same-origin `GET /api/wishlist/getMy`; counting request events also catches an
attempt that fails before receiving an HTTP response. Public browsing of an
explicitly selected owner's wishlists remains available anonymously.

On a failed browser test, inspect `browserTests/build/artifacts/<invocation>/` for
`server.log` and, for each failed test, `failure.png` and `trace.zip`. CI runs the
same `./gradlew browserTest` command after installing the Chromium runtime libraries
and uploads `browserTests/build/artifacts/**` when the job fails.

`jsBrowserTest` checks Kotlin/JS browser-unit behavior. `browserTest` checks the
served Web application through the real Ktor server and managed Chromium; both
checks cover different layers.

## Feature acceptance planning

Feature work uses the [acceptance contract](agents/FEATURE_ACCEPTANCE.md) to specify
observable behavior, UI states, applicable platforms, checks and required evidence
before implementation. The common [graph of navigation](NAVIGATION.md) at the
project root contains the shared Mermaid graph and links authoritative feature
records, source/symbol links or descriptive `TBD` entries, and separate test/evidence
references. Every change must consult and edit it; changes without navigation
impact still record their scope, reason and checks there. Detailed feature models
extend the common graph using the same IDs. Unmodeled journeys and unverified
transitions remain explicit.

The [worked wishlist example](agents/examples/wishlist-acceptance.md) shows
authenticated/anonymous paths, persistence, errors/retry and visual criteria. It is
a proposed example, not proof those paths already pass. New screenshot baselines
need recorded approval; screenshot capture and green smoke/build checks do not
establish feature or visual conformance. See the contract for setup/evidence,
baseline review, model maintenance, scoped exceptions and assurance limits.

## Running the server (with the web client)

The server also serves the compiled web client as static files, so a single `run` brings up
both the API and the Web UI.

1. Start Mailpit for development email delivery. `server/dev.config.json` uses
   a local SQLite database; PostgreSQL is not required for this server run.
   The PostgreSQL service in `server/docker-compose.yml` is commented out, so
   this Compose command starts **Mailpit only** (SMTP on 1025, UI on 8025).
   The disposable PostgreSQL instructions above are for integration tests, not
   the application database.

   ```bash
   # from the project root
   docker compose -f server/docker-compose.yml up
   ```

2. Run the server with the development config. The `run` task automatically builds the web
   client's development bundle first (`dev.config.json` serves it from
   `../client/build/dist/js/developmentExecutable`):

   ```bash
   # from the project root
   ./gradlew :wishlist.server:run --args="dev.config.json"
   ```

3. Open <http://127.0.0.1:8196> for the Web client.

On first start, watch the server log for the generated `root` password.

### Server configuration

The server takes a single argument: the path to a config JSON (working directory is the
`server/` module, so `dev.config.json` resolves to `server/dev.config.json`). For local
development use `server/dev.config.json`; `server/sample.config.json` is the production
template (see [Production deployment](#production-deployment)). Key fields:

| Field | Meaning |
|-------|---------|
| `host` / `port` | bind address and port (default `8196`) |
| `publicHost` | host advertised to clients |
| `publicHttpOrigin` | complete externally reachable HTTP(S) origin used in invite links; defaults to `http://{publicHost}:{port}` for compatibility; set an explicit reverse-proxy origin such as `https://wishlist.example` in production |
| `staticFolders` | static content roots (serves the web client bundle) |
| `database` | JDBC `url`, `username`, `password` for PostgreSQL |
| `plugins` | fully-qualified server feature plugins loaded by reflection |
| `filesFolder` | directory for uploaded item images |
| `useCache` | enable in-memory caching of repositories |
| `tokenTtl` / `refreshTokenTtl` | bearer / refresh token lifetimes (ISO-8601 durations) |
| `enableRegistration` | allow new-user registration |
| `requireEmailForRegistration` | require a valid email and successful verification invite; new accounts start with `NewUser` |
| `openExchangeRatesAppId` | Open Exchange Rates App ID enabling the currency feature (`null` disables it) |
| `openExchangeRatesRefreshTTLMillis` | currency-rates cache lifetime in milliseconds |
| `email` | Nested SMTP config object (`{ smtp: { host, port, username?, password?, from, useTls, useSsl } }`) that enables the email feature's SMTP test-email delivery; omit the key (or set it to JSON `null`) to disable SMTP while per-user email-address storage (`PUT /email/myEmail`) keeps working |

## Production deployment

Local development uses `server/dev.config.json` (SQLite) together with
`server/docker-compose.yml` (Mailpit at <http://127.0.0.1:8025>; its PostgreSQL
service is commented out). Registration invites are delivered to Mailpit through SMTP on
`127.0.0.1:1025`; opening the invite calls `/api/links/{deeplink_uuid}` and promotes the
account from `NewUser` to `User`. Production runs from a set of
**sample template files** in `server/` — copy each one, replace its placeholder values, and
never commit the result.

| File | Role | What to change before use |
|------|------|---------------------------|
| `server/sample.config.json` | Production server config template. Serves the web bundle from `/static`, stores uploads under `/data/uploaded_files`, and points the database at the `postgres` service host. | Replace the `database` `url` / `username` / `password` (placeholders `TEST_DB` / `TEST_USERNAME` / `TEST_PASSWORD`), set `publicHost` as needed by clients and `publicHttpOrigin` to the externally reachable reverse-proxy origin (for example `https://wishlist.example`, with no internal bind port), set `openExchangeRatesAppId` if you use the currency feature, configure the `email` SMTP block, and review both registration flags. Set `requireEmailForRegistration` to `true` only with working SMTP and deeplinks. Mount the finished file into the container at `/config.json`. |
| `server/sample.docker-compose.yml` | Production Docker Compose template. Runs the published `insanusmokrassar/wishlists` image plus a PostgreSQL service, mounts `./config.json:/config.json:ro` and `./data/uploaded_files/`, and publishes port `8196`. | Copy to `docker-compose.yml`, replace the `POSTGRES_USER` / `POSTGRES_PASSWORD` / `POSTGRES_DB` placeholders (match them to your config's `database` block), and provide your filled-in `config.json` next to it. |
| `server/Dockerfile` | Builds the server image (`amazoncorretto:21`). Unpacks the web production bundle into `/static` and the server distribution, and runs the entrypoint against `/config.json`. | Usually unchanged; used by `deploy.sh`. |
| `server/deploy.sh` | Build-and-publish script: packs the web `productionExecutable` bundle, then builds, tags, and pushes the Docker image to the registry. | Set `app` / `version` / `server` (registry account) to your own. Build the client (`./gradlew :wishlist.client:jsBrowserDistribution`) and server distribution tar first. |

Hardening notes for production:

- The server speaks plain HTTP — terminate TLS with a reverse proxy in front of it.
- Set `publicHttpOrigin` to that proxy's public HTTP(S) origin so verification messages do not expose the internal Ktor bind port.
- Replace every `TEST_*` placeholder; the sample files ship with placeholders only.
- On first start, watch the server log for the generated `root` password and store it.

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
