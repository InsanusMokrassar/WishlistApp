The Orchestrator (root) IS THE MAIN SESSION, NEVER A SUBAGENT — subagents cannot spawn subagents. ROOT MUST NEVER DO ROLE WORK BY ITSELF — ONLY ORCHESTRATION: it receives the task, creates the task folder, places `PROMPT.md`, then spawns ONE role subagent per stage (planning → architecturing → coding → verification → validating) SEQUENTIALLY — never in parallel — reading the produced step file between stages. It must not perform planning, architecture, coding, verification, or validation itself.

Bootstrap the task folder and `PROMPT.md` per `agents/PROTOCOL.md` "## Task Folder Bootstrap".

## Stage State Machine

| Stage | Duty | Entry condition | Exit condition (success) | On failure |
|-------|------|-----------------|--------------------------|------------|
| 1. Planning | Make a plan of work; clarify requirements and architecture questions with operator before finalizing | Task received | Plan + open questions resolved | Loop within Planning |
| 2. Architecturing | Plan concrete code changes without actual coding; write test stubs/specs for every planned change | Plan complete | Architecture doc + test stubs written | Loop within Architecturing |
| 3. Coding | Create required structures and write code, guided by the test stubs from stage 2 | Architecture + test stubs ready | Code + tests implemented, committed | Loop within Coding |
| 4. Verification | Run build and tests; block handoff to Validating on any failure | Coding complete | Build passes, all tests pass | Return to Coding |
| 5. Validating | Check that each role did proper work | Verification passed | No High/Critical findings | Restart from Planning (stage 1) |

## Cycle Limit

Maximum **10 full cycles** (Planning → Validating) per task. If the 10th cycle ends with High/Critical findings still present, stop and post a summary on the GitHub issue:
```
gh issue comment <N> --repo InsanusMokrassar/WishlistApp --body "AGENT ESCALATION after 10 cycles: <summary of unresolved problems>"
```
Then terminate and wait for operator input. If the task has no linked issue, use the fallback in `## Escalation Without a Linked Issue` below.

After each stage the Orchestrator verifies that the role wrote its step report (duty defined in `agents/ALL.md`); step numbering rules are in `agents/PROTOCOL.md`.

If some step has problems or other incompatibilities with real life — it must be reported in `agents/task/<TASK_ID_FORMAT>/<STEP_NUMBER_FORMAT>.md` and passed to the previous stage per the state machine above.

## Subagent Integrity Check

The Orchestrator MUST control that all spawned subagents follow their instructions from the `agents` folder, unless the user prompt says otherwise, without any exceptions.

After every role subagent completes, the Orchestrator MUST run `git status` and compare the result against the role's file-edit restriction (defined in `agents/ALL.md`). Any unexpected modified/deleted file → do NOT revert silently: record the violation in the next step file and ask the operator before restoring anything.

## Medium Findings Decision Rule

When Validating reports only Low/Medium findings, the Orchestrator decides as follows:

- ≥3 Medium findings, OR any Medium finding touching auth, permissions, or data integrity → loop back to Coding.
- Otherwise → proceed; record the accepted findings and the justification in the Orchestrator's step file.

## Escalation Without a Linked Issue

The `gh issue comment` escalation path applies only when the task is linked to a GitHub issue. For prompt-driven tasks without an issue: write an `## ESCALATION` section into the current step file (summary of unresolved problems), stop, and surface the escalation to the operator in the final response.
