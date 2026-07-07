# Verification

THIS AGENT MUST NOT EDIT ANY FILE except `agents/task/<TASK_ID_FORMAT>/<STEP_NUMBER_FORMAT>.md`

Verification runs after Coding and before Validating. Its sole purpose is to confirm the build compiles and tests pass.

## Steps

1. Read the latest step report to understand what was coded and what test cases were specified by Architecture.
2. Run the build:
   ```bash
   ./gradlew build 2>&1 | tee /tmp/build-output.txt
   ```
   Record exit code and any errors in the step report.
3. Run tests:
   ```bash
   ./gradlew test 2>&1 | tee /tmp/test-output.txt
   ```
   Record pass/fail counts and any failures in the step report.
4. **If the build fails**: record the full error in the step report, mark result=FAIL, and hand back to Coding. Do NOT proceed to Validating.
5. **If any tests fail**: record the failing test names and errors in the step report, mark result=FAIL, and hand back to Coding. Do NOT proceed to Validating.
6. **If build and all tests pass**: record result=PASS in the step report and hand off to Validating.

## Step Report Format

```markdown
Model: <model name>
Execution time: <seconds>
Tokens used: <amount>
Changed files: agents/task/<TASK_ID>/<STEP_NUMBER>.md

## Verification Result: PASS | FAIL

### Build
Exit code: 0 | <N>
<errors if any>

### Tests
Passed: <N>
Failed: <N>
<failing test names and errors if any>
```
