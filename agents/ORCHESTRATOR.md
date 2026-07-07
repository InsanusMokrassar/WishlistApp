Orchestrator generating a TASK_ID for current execution and creating folder for current task as described in `agents/PROTOCOL.md`.

Add `PROMPT.md` with source prompt or issue raw text there.

## Stage State Machine

| Stage | Entry condition | Exit condition (success) | On failure |
|-------|----------------|--------------------------|------------|
| 1. Planning | Task received | Plan + open questions resolved | Loop within Planning |
| 2. Architecturing | Plan complete | Architecture doc + test stubs written | Loop within Architecturing |
| 3. Coding | Architecture + test stubs ready | Code + tests implemented, committed | Loop within Coding |
| 4. Verification | Coding complete | Build passes, all tests pass | Return to Coding |
| 5. Validating | Verification passed | No High/Critical findings | Restart from Planning (stage 1) |

## Cycle Limit

Maximum **10 full cycles** (Planning → Validating) per task. If the 10th cycle ends with High/Critical findings still present, stop and post a summary on the GitHub issue:
```
gh issue comment <N> --repo InsanusMokrassar/WishlistApp --body "AGENT ESCALATION after 10 cycles: <summary of unresolved problems>"
```
Then terminate and wait for operator input.

## Roles Order

1. **Planning** — make a plan of work; clarify requirements and architecture questions with operator before finalizing
2. **Architecturing** — plan concrete code changes without actual coding; write test stubs/specs for every planned change
3. **Coding** — create required structures and write code, guided by the test stubs from step 2
4. **Verification** — run build and tests; block handoff to Validating on any failure
5. **Validating** — check that each role did proper work; if High/Critical issues found → restart full cycle from Planning (stage 1)

EACH STEP MUST BE FORCED TO MAKE REPORT ABOUT ITS RESULTS IN `agents/task/<TASK_ID_FORMAT>/<STEP_NUMBER_FORMAT>.md`. After each step its number increases by one (monotonically — never reset across restarts).

If some step has problems or other incompatibilities with real life — it must be reported in `agents/task/<TASK_ID_FORMAT>/<STEP_NUMBER_FORMAT>.md` and passed to the previous stage per the state machine above.

None of the steps must be wiped during work of some other step.
