Model: GPT-5.6 Codex (ML; selected for Coding per `agents/SHORTCUTS.md`)
Changed files: `browser-tests.gradle`

## Fix

The browser-test server now opens its generated, per-invocation SQLite database through a test-only JDBC URI with a 30-second busy timeout, WAL journal mode, and `IMMEDIATE` transactions. Concurrent feature startup can therefore wait for schema-lock ownership instead of failing while Exposed creates the first tables. Each URL still names `browserTests/build/tmp/test/<invocation>/browser.db`; no operator database is involved.

The browser suite directory and server process are now registered for cleanup before server readiness is checked. A readiness failure therefore follows the existing cleanup path and removes the generated temporary suite after forcibly stopping the child server.

## Verification

Three consecutive fresh `./gradlew browserTest --console=plain` invocations passed, taking 25 seconds, 23 seconds, and 23 seconds. Each invocation ran the served browser suite with 2 tests; the final JUnit XML reports 2 tests, 0 failures, and 0 errors for `rendersApplication` and `registersAndReachesAuthenticatedUi`.

A controlled server-readiness failure exercised the cleanup path. `browserTests/build/tmp/test/20260920-142709-888` was absent after cleanup, and the server process had stopped.

`./gradlew build --console=plain` passed in 1 minute 7 seconds: 4,597 actionable tasks, 181 executed, and 4,416 up-to-date. Gradle emitted its existing deprecation notice but no failures.

`XDG_CACHE_HOME=/tmp/issue85-ast-cache ast-index rebuild` completed successfully with 805 files and 50 modules indexed. `git diff --check` passed.
