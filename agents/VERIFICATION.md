# Verification

Verification runs after Coding and before Validating. It confirms that the build
compiles and the applicable tests pass, including the served Web browser gate for
changes that can affect the Web UI.

## Steps

1. Read the latest step report to understand what was coded and what test cases were specified by Architecture.
2. Run the build (compiles AND runs `check`, which includes every test task on all KMP targets):
   ```bash
   set -o pipefail
   ./gradlew build 2>&1 | tee /tmp/build-output.txt
   echo "build_exit=$?"
   ```
   `set -o pipefail` is MANDATORY — without it the recorded exit code is `tee`'s (always 0), not Gradle's. Record the real exit code and any errors in the step report.
3. Parse test results from the build output (`/tmp/build-output.txt`): record pass/fail counts and failing test names. Only if the build output shows that NO test tasks were executed, run tests explicitly (`allTests`, not `test`, so all KMP targets are covered):
   ```bash
   set -o pipefail
   ./gradlew allTests 2>&1 | tee /tmp/test-output.txt
   echo "test_exit=$?"
   ```
4. **If the build fails**: record the full error in the step report, mark result=FAIL, and hand back to Coding. Do NOT proceed to Validating.
5. **If any tests fail**: record the failing test names and errors in the step report, mark result=FAIL, and hand back to Coding. Do NOT proceed to Validating.
6. **If build and all tests pass**: record result=PASS in the step report and hand off to Validating.

## Served Web browser gate

Run the real-browser gate in addition to `build` and the KMP test coverage when the
change affects Web UI code, Web resources, client routing, or server/API behavior
that the Web client uses:

```bash
set -o pipefail
./gradlew browserTest --console=plain 2>&1 | tee /tmp/browser-test-output.txt
echo "browser_exit=$?"
```

The command must run on a supported Linux runner with JDK 17 and the documented
Chromium runtime libraries. It builds the Web bundle, provisions the pinned
Playwright Chromium revision when needed, starts the application with a fresh
loopback SQLite database and temporary uploads directory, runs the served page
through Chromium, and cleans up the server and temporary state. The current suite
contains two tests: `rendersApplication` and
`registersAndReachesAuthenticatedUi`. Headed inspection is available with
`./gradlew browserTest -PbrowserHeaded=true`; CI uses headless mode.

The step report must include the exact browser command, its real exit status, the
Chromium version or pinned Playwright version, passed and failed test counts, the
artifact directory, and any setup limitation. Record relevant paths for
`server.log`, `failure.png`, and `trace.zip` when present. A required browser gate
that fails, cannot start, lacks required OS libraries, or is unavailable on the
runner is `FAIL` and must return to Coding; do not report `PASS` based on a bundle
build or partial setup.

For changes unrelated to the Web UI and unrelated to server/API behavior consumed
by the Web client, record why the browser gate is not applicable and continue with
the normal build and relevant tests. `jsBrowserTest` verifies Kotlin/JS browser-unit
behavior, while `browserTest` verifies the served application in managed Chromium;
`jsBrowserTest` cannot substitute for a required served-application gate.

## Step Report Format

```markdown
Model: <model name>
Changed files: agents/task/<TASK_ID>/<STEP_NUMBER>.md

## Verification Result: PASS | FAIL

### Build
Exit code: 0 | <N>   (real Gradle exit code via pipefail — see Steps)
<errors if any>

### Tests
Passed: <N>
Failed: <N>
<failing test names and errors if any>
```

(`Execution time` / `Tokens used` are optional rough estimates per `agents/ALL.md`.)
