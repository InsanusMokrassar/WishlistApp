Model: GPT-5.6 Codex (ML; selected for Coding per `agents/SHORTCUTS.md`)
Changed files: `browserTests/src/test/kotlin/dev/inmo/wishlist/browser/BrowserResponseClassifier.kt`, `browserTests/src/test/kotlin/dev/inmo/wishlist/browser/BrowserResponseClassifierTest.kt`

## Fix

Console suppression now uses bounded allowances derived from observed exact anonymous bootstrap responses. A generic Chromium 401 console message is accepted only when its HTTP source location normalizes to same-origin `GET /api/wishlist/getMy` and one exact response remains available. An unlocated generic 401 always fails.

The served client logs its anonymous `NoTransformationFoundException` from `webpack-internal:///./kotlin/kslog.js`, which cannot identify an HTTP endpoint. That message is therefore treated as unlocated and may consume at most one allowance from an observed exact bootstrap response. An unlocated transformation error without such a response, and every extra unlocated transformation error beyond the response count, fails. This preserves the actual served-client diagnostic path without accepting unrelated anonymous errors absent an exact bootstrap response.

Focused tests now cover an attributed generic console 401, an unlocated generic 401 after a valid response, an unlocated transformation error without a valid response, and the observed webpack-internal transformation diagnostic with one valid response.

## Verification

`./gradlew browserTest --console=plain` passed in 29 seconds. The healthy private Playwright cache completed the pinned headless launch probe without installation work. The JUnit XML reports 8 tests, 0 failures, and 0 errors: 2 served application smoke tests and 6 classifier tests.

The browser gate recompiles and runs both changed Kotlin test files together with the served application. The aggregate build is not repeated because the change is limited to browser-test source and the aggregate build passed after the harness implementation in step 013 (1 minute 7 seconds, 4,597 actionable tasks); the focused served-browser gate provides direct compilation and execution coverage for this correction.

`XDG_CACHE_HOME=/tmp/issue85-ast-cache ast-index rebuild` completed successfully with 808 files and 50 modules indexed. `git diff --check` passed. The fixture continues to use invocation-specific SQLite state below `browserTests/build/tmp/test`; no operator database was accessed.
