# Verification

Verification runs after Coding and before Validating. Confirm the explicit accepted contract's mapped checks pass, including builds/tests for application changes and static/trace assertions for documentation-only work.

## Steps

1. Read the Coding report, explicit final seal, aggregate, referenced accepted plan and resolution/test mapping. Verify artifact identities and CONSENSUS; do not infer authority from the latest filename. Already-started legacy cycles use their recorded Architecture contract under the adoption exception.
2. For documentation-only changes, run the specified static and representative trace assertions (council changes use `agents/council/CHECKS.md`), record command/exit/result evidence, and mark application build and ast-index rebuild not applicable. Failed checks return to Coding; all mapped checks must pass before Validating. For application/source/build-affecting changes, run the build (compiles AND runs `check`, which includes every test task on all KMP targets):
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
6. **If build and all tests pass**, or all required documentation-only checks pass: record result=PASS in the step report and hand off to Validating. Do not replace evaluated trace assertions with a narrative claim that scenarios look correct.

## Step Report Format

```markdown
Model: <model name>
Changed files: agents/task/<TASK_ID>/<STEP_NUMBER>.md

## Verification Result: PASS | FAIL

### Contract and mapped checks
<seal/plan/resolution identities, or explicit legacy contract>
<acceptance IDs, check IDs, commands, real exit codes, PASS/FAIL, failing assertions>

### Build
Exit code: 0 | <N> | not applicable (documentation only)
<real Gradle exit code via pipefail for application changes>
<errors if any>

### Tests
Passed: <N>
Failed: <N>
<failing test names and errors if any>
```

(`Execution time` / `Tokens used` are optional rough estimates per `agents/ALL.md`.)
