Model: GPT-5.6 Codex (ML; selected for Coding per `agents/SHORTCUTS.md`)
Changed files: `browser-tests.gradle`, `browserTests/src/test/kotlin/dev/inmo/wishlist/browser/ChromiumProbe.kt`, `browserTests/src/test/kotlin/dev/inmo/wishlist/browser/BrowserResponseClassifier.kt`, `browserTests/src/test/kotlin/dev/inmo/wishlist/browser/BrowserResponseClassifierTest.kt`, `browserTests/src/test/kotlin/dev/inmo/wishlist/browser/ServedWebSmokeTest.kt`

## Implementation

`installBrowserChromium` now probes the pinned Playwright 1.52.0 executable with downloads disabled and launches headless Chromium before treating the private cache as healthy. A failed probe derives the pinned Chromium and headless-shell directories from Playwright's executable path, removes only that pair, uses the pinned CLI to install Chromium, and repeats the no-download launch probe. Other Chromium revisions are temporarily preserved around the CLI because Playwright's installer otherwise removes unfamiliar revisions. Provisioning remains scoped to `browserTests/build/playwright`; the test JVM keeps `PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1`.

The served smoke tests now collect Playwright responses before navigation. Only same-origin `GET /api/wishlist/getMy` with HTTP 401 during anonymous bootstrap is tolerated. The phase advances immediately before account creation and after authenticated UI appears, so every later 401 is recorded as a test failure. Generic Chromium console messages are deferred until the matching response is known, and source locations such as `.../getMy:0:0` are normalized before exact endpoint matching. The focused classifier tests prove the allowed response, exact path and origin constraints, and rejection after authentication begins.

All server termination paths use the same bounded process-tree routine. The routine repeatedly snapshots the live descendant tree, signals leaves before the parent, waits five seconds, force-stops survivors, waits another five seconds, and removes only the invocation suite after all captured handles exit. A cleanup failure retains the suite and log and reports surviving PIDs. Readiness failure, task finalization, and Gradle build finalization all call this routine.

## Verification

`./gradlew browserTest --console=plain` passed with a complete cache in 27 seconds, reporting a pinned headless-launch cache hit. The final JUnit XML reports 5 tests, 0 failures, and 0 errors: 2 served smoke tests and 3 focused classifier tests. The latest generated suite `browserTests/build/tmp/test/20260920-151211-685` was removed after teardown.

Private-cache scenarios all used only `browserTests/build/playwright`:

- Empty cache: repaired, launch-validated, and passed in 54 seconds.
- Wrong-revision-only cache containing `chromium-999999`: repaired and passed in 38 seconds; the unrelated directory remained present.
- Partial pinned cache with its executable removed: removed only dynamically derived `chromium-1169` and `chromium_headless_shell-1169`, repaired and passed in 41 seconds; the executable and unrelated revision were present afterward.

`./gradlew help --console=plain` passed in 13 seconds and showed no browser provisioning action. `./gradlew build --console=plain` passed in 1 minute 7 seconds with 4,597 actionable tasks, 181 executed, and 4,416 up-to-date; `installBrowserChromium` was skipped. Existing Gradle deprecation warnings remained non-fatal.

`./gradlew :wishlist.browserTests:testClasses --console=plain` passed after the final source update. `XDG_CACHE_HOME=/tmp/issue85-ast-cache ast-index rebuild` indexed 808 files and 50 modules. `git diff --check` passed.

No operator database was accessed: the generated browser server configuration continues to use an invocation-specific SQLite database below `browserTests/build/tmp/test`. A fresh external CI checkout has not been run during this local Coding step; that remains for the orchestrator after push.
