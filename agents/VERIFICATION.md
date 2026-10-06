# Verification

Mechanical verification confirms the build compiles and the applicable specified checks execute and pass. Record results and artifacts under the [feature acceptance contract](FEATURE_ACCEPTANCE.md); execution does not approve requirements, visual intent or baselines.

## Steps

1. Read the supplied latest completed report and governing accepted plan to identify what was coded, the bounded required transition/criterion/check IDs, applicable platforms, commands, setup and evidence requirements. Missing required specifications are a reported gap, not permission to substitute generic smoke tests.
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
4. **If the build fails**: record the full error in the step report, mark result=FAIL, and return the report to root.
5. **If any tests fail**: record the failing test names and errors in the step report, mark result=FAIL, and return the report to root.
6. **If build and all applicable required mechanical checks pass with the specified evidence**: record result=PASS in the step report and return it to root. A failed, unavailable, unjustifiably skipped or unverified required mechanical check is FAIL. Preserve any supplied operator exception and the actual check outcome; never report an unexecuted check as passed. Root handles exceptions through the accepted-scope decision process.

## Feature check execution and evidence

Execute each applicable specified unit/integration/served-browser/visual check, using the plan's fixtures, deterministic conditions and cleanup. Inspect reports from the current invocation for actual assertions, counts and failing names; distinguish cached/UP-TO-DATE tasks from fresh execution and prior XML files. Record delivered source revision and working-tree delta, environment/browser/viewport/native identity, exact commands and exit statuses, artifacts and limitations against the stable IDs. Record required, checked, failed, skipped, unavailable and unverified sets with reasons; do not infer native or cross-browser results from Chromium.

Collect approved manual inspection evidence only under its recorded handling decision and identify reviewer, date, exact criteria, revision/environment and observations. Missing manual or visual approval evidence remains explicit and unverified even when mechanical checks pass. Failure screenshots/traces and captured DOM are diagnostics; a specified comparison or approved inspection is still required for visual criteria. Do not approve initial/new baselines or update expectations to make a failed comparison pass.

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
Chromium runtime libraries. It builds the Web bundle, checks the pinned Chromium
cache by launching the headless browser, repairs an empty or partial pinned cache
when needed, starts the application with a fresh loopback SQLite database and
temporary uploads directory, runs the served page through Chromium, and cleans up
the server and temporary state. The current suite contains eight tests: two served
smoke tests (`rendersApplication` and `registersAndReachesAuthenticatedUi`) plus
six focused browser-response classifier tests. The collector strictly fails on
every console error, page exception, and HTTP 401. The anonymous smoke path must
emit zero Playwright `onRequest` events for same-origin `GET /api/wishlist/getMy`,
including attempts that fail before an HTTP response; public browsing by an explicit
owner remains available anonymously. ViewModel list-load errors are logged per
trigger while the auth/resume collector remains active for later retries. Headed
inspection is available with
`./gradlew browserTest -PbrowserHeaded=true`; CI uses headless mode.

The step report must include the exact browser command, its real exit status, the
Chromium version or pinned Playwright version, cache probe or repair result,
passed and failed test counts, the artifact directory, and any setup limitation.
Record relevant paths for
`server.log`, `failure.png`, and `trace.zip` when present. A required browser gate
that fails, cannot start, lacks required OS libraries, or is unavailable on the
runner is `FAIL` and must return its report to root; do not report `PASS` based on
a bundle build or partial setup.

For changes unrelated to the Web UI and unrelated to server/API behavior consumed
by the Web client, record why the browser gate is not applicable and continue with
the normal build and relevant tests. `jsBrowserTest` verifies Kotlin/JS browser-unit
behavior, while `browserTest` verifies the served application in managed Chromium;
`jsBrowserTest` cannot substitute for a required served-application gate.

## Step Report Format

Begin with the mandatory `Model` and `Changed files` headers, then a
`## Verification Result: PASS` or `## Verification Result: FAIL` heading. Describe
build/test outcomes and limitations in normal prose, including real exit statuses,
actual counts/failing names, revision/environment identity and evidence paths. Keep
criterion/transition/check execution records in full AML-HIP blocks with the shared
AGENTS.md self-check; do not replace those blocks with ad hoc tables or YAML.

(`Execution time` / `Tokens used` are optional rough estimates per `agents/ALL.md`.)
