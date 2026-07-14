# Verification

THIS AGENT MUST NOT EDIT ANY FILE except `agents/task/<TASK_ID_FORMAT>/<STEP_NUMBER_FORMAT>.md`

Verification runs after Coding and before Validating. Its sole purpose is to confirm the build compiles and tests pass.

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
