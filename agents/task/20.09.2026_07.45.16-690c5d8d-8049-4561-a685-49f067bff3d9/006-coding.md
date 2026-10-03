Model: GPT-5.6 Codex (ML; selected for coding per `agents/SHORTCUTS.md`)
Changed files: `browserTests/src/test/kotlin/dev/inmo/wishlist/browser/ServedWebSmokeTest.kt`

## Fix

Replaced the ambiguous exact-text `Log out` assertion with the first exact-name button locator. The rendered authenticated page currently contains two `Log out` buttons, so Playwright strict mode correctly rejected the former locator even though registration succeeded. The locator now matches the same button semantic as the existing Register and New Wishlist checks and remains bounded by the 15-second visible wait.

## Verification

`./gradlew browserTest --console=plain` exited 0 in 29 seconds. The cached Playwright 1.52.0 Chromium installer was skipped; the JUnit XML reports 2 tests, 0 failures, and 0 errors: `rendersApplication` and `registersAndReachesAuthenticatedUi`. The generated server state was isolated and cleaned as part of the gate.

The aggregate `./gradlew build` is not repeated because Verification step 005 already completed it successfully after the browser harness changes (exit 0, 4,597 actionable tasks), and this correction only changes the JVM test locator. The browser gate recompiles and runs the changed test source together with the served server and Web bundle.

`XDG_CACHE_HOME=/tmp/issue85-ast-cache ast-index rebuild` completed successfully: 805 files and 50 modules indexed. `git diff --check` passed.
