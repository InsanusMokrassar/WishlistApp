# Add reproducible real-browser verification for the Web client

Source: https://github.com/InsanusMokrassar/WishlistApp/issues/85

## Goal

Make the served Web client testable in a real browser and make that check part of Verification for changes that affect the Web UI or its API. Kotlin/JS browser-unit tests and a successful bundle build do not by themselves prove that the running application renders and works.

The project is Kotlin-first and uses Gradle. The operator's local server uses SQLite under `server/src`; browser tests must not use or modify that working database.

## Proposed implementation

- Prefer Playwright's JVM library from Kotlin in a dedicated Gradle test module or source set. Pin its version in the project dependency management. Start with Playwright-managed Chromium; do not depend on a manually installed ChromeDriver or a separate TypeScript test project.
- Make a test-only server configuration with a disposable SQLite database, deterministic seed data, and a configurable local port. Build the Web bundle, start the server, wait until it is ready, run browser tests, and stop it reliably, including after failure. Do not require Docker or PostgreSQL for this suite.
- Add a small initial end-to-end smoke suite that opens the served application, verifies a meaningful rendered page, and exercises at least one authenticated user path. Check for unexpected page/console errors on those paths. Keep tests independent of the operator's accounts and data.
- Provide headless execution for CI and an optional headed/debug run for local inspection. Retain a useful screenshot and trace when a browser test fails.

## Recoverable setup on a new machine

- Provide one documented Gradle command that provisions the required browser if missing and runs the browser suite. The Gradle wrapper and pinned dependency must determine the Playwright/browser version; no global Playwright installation or manually managed driver path should be required.
- Keep bootstrap logic in a separate Groovy Gradle file applied from root `build.gradle`, following the existing `extensions.gradle` pattern. An idempotent Bash setup script may be used for initial machine setup and called by a dedicated Gradle task only when initialization is needed. Do not download or install anything during ordinary Gradle configuration or unrelated tasks.
- If Bash or OS-level browser packages are required on a supported platform, document the prerequisite and give an actionable failure when automatic setup cannot complete. Prefer a Gradle/JVM path where it avoids an unnecessary platform-specific dependency.
- Run the same documented command in CI on a clean checkout so installation and tests are proven recoverable, not only functional on one developer machine.

## Verification-stage instructions

- Update `agents/VERIFICATION.md` so Verification runs the real-browser suite for Web UI changes and server/API changes that affect the Web client, in addition to the existing build and KMP tests. For unrelated changes, record why the browser gate is not applicable.
- Require the step report to record the exact command and exit status, browser and test counts, relevant failure artifacts, and any setup limitation. A required browser check that fails or cannot run must not be reported as PASS.
- Document that `jsBrowserTest` and the new served-application browser suite prove different things; one must not silently substitute for the other.

## Acceptance criteria

1. On a clean supported machine or CI runner, a documented Gradle command installs/provisions the matching browser as needed, starts the isolated SQLite-backed application, runs the smoke suite, and cleans up its server and temporary data.
2. The suite tests the actual served Web page in Chromium and produces diagnostic artifacts on failure. It does not read, overwrite, or delete the operator's SQLite database under `server/src`.
3. Re-running setup and tests is safe and does not repeat installation when the required browser is already available. Unrelated Gradle commands do not trigger browser setup.
4. CI runs the same browser gate; Verification instructions require and report it for Web-affecting changes, and a failing/unavailable required gate blocks handoff to Validation.
5. README/setup documentation explains local and CI commands, supported platforms, prerequisites, headed mode, and where failure artifacts are found.

References: [Playwright Java with Gradle and JUnit](https://playwright.dev/java/docs/test-runners), [browser installation](https://playwright.dev/java/docs/browsers), [CI guidance](https://playwright.dev/java/docs/ci).

