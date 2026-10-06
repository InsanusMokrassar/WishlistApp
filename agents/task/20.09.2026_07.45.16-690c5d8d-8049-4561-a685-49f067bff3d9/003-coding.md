Model: GPT-5.6 Codex (ML; selected for implementation per `agents/SHORTCUTS.md`)
Changed files: `.github/workflows/build.yml`, `build.gradle`, `settings.gradle`, `gradle/libs.versions.toml`, `browser-tests.gradle`, `browserTests/build.gradle`, `browserTests/src/test/kotlin/dev/inmo/wishlist/browser/BrowserFixture.kt`, `browserTests/src/test/kotlin/dev/inmo/wishlist/browser/ServedWebSmokeTest.kt`

## Implementation

Added the `:wishlist.browserTests` JVM module with JUnit 5 and pinned Playwright 1.52.0. The root `browserTest` task builds the development Web distribution, provisions the Playwright-managed Chromium cache, generates a fresh loopback server config with SQLite and uploads under the test task directory, starts the real server on an ephemeral port, waits for its served HTML shell, and runs the JVM smoke suite. Cleanup destroys the server process and descendants, removes only the generated suite directory, and preserves each invocation's `server.log` under `browserTests/build/artifacts/`.

The test suite uses independent Chromium contexts. It confirms Compose mounts the application shell and wishlist heading, then registers a unique disposable user and verifies the authenticated `Log out` and `New Wishlist` controls. Failure captures trace ZIP and full-page screenshot. Headed debugging is available through `-PbrowserHeaded=true`. The one known anonymous bootstrap console condition is filtered narrowly: the current client logs the expected API 401 plus `NoTransformationFoundException` before authentication; any other page or console error fails the test.

CI now installs Chromium runtime libraries, runs `./gradlew browserTest`, and uploads browser artifacts on failure. The aggregate `build` keeps its normal scope: its direct browser test task is skipped because only the `browserTest` gate owns server setup.

## Verification

`./gradlew browserTest --console=plain` passed twice after the cache was provisioned. Each run exited 0 and ran 2 tests: `rendersApplication` and `registersAndReachesAuthenticatedUi`. The first passing run took 28 seconds; the second took 23 seconds. Both runs used fresh ephemeral ports and disposable SQLite state; server logs were retained under `browserTests/build/artifacts/20260920-140411-965/` and `browserTests/build/artifacts/20260920-140441-734/`. No failure screenshot or trace was produced on either pass.

`./gradlew build --console=plain` exited 0 in 5 minutes 3 seconds with 4,608 actionable tasks (4,055 executed, 553 up-to-date).

`XDG_CACHE_HOME=/tmp/issue85-ast-cache ast-index rebuild` completed successfully: 805 files and 50 modules indexed. `git diff --check` passed.

## Setup limitation

Initial browser provisioning reported the local OS as unsupported and selected Playwright's Ubuntu 20.04 fallback build. The resulting cached Chromium and headless shell ran both smoke tests successfully. CI declares the required Ubuntu runtime libraries; a clean CI run remains the final proof for that runner.
