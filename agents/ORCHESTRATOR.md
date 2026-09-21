# Root Orchestrator

Root is the main session and only workflow controller. It receives the task, creates the task folder and `PROMPT.md`, and invokes one ordinary role per stage sequentially: preparation → coding → verification → validating. It reads the returned report between stages and never performs role work itself. Preparation alone may invoke the complete council roster as fresh independent parallel subagents; this exception does not permit other workers to delegate.

This file and `SHORTCUTS.md` are root-only. Root manages every transition, restart, handoff, operator relay and publication. Do not send this state machine, other role instructions, future consumers or future-stage requirements to workers. Each role receives only its own instruction bundle, explicit permitted repository evidence, already-completed input artifacts, output path and write scope. Use fresh contexts, not parent-conversation forks. Apply available tool restrictions and exclude root catalogs, other roles' instructions, active peer outputs and copies of excluded content in searches/history. Supply relevant excerpts of completed history when full records disclose future workflow, without editing the originals. If required evidence cannot be supplied within the boundary, stop for an explicit input decision; do not silently leak it.

Preparation receives the source task and any confirmed answers or prior completed evidence. Its invocation authorizes council-contract discovery as coordinator-only input and council-artifact writes; those contracts are not distributed across participants. Coding receives the complete successful Preparation result; only root establishes this handoff. Coding may edit required source/resources/feature READMEs in addition to its report. Verification receives the latest completed report and relevant existing test specifications. Validation receives the completed current evidence chain, including council evidence needed to audit requirements and comments. Workers report results to root and stop; they never select a subsequent stage.

Bootstrap the task folder and `PROMPT.md` per `agents/PROTOCOL.md` "## Task Folder Bootstrap".

## Stage State Machine

| Stage | Duty | Entry condition | Exit condition (success) | On failure |
|-------|------|-----------------|--------------------------|------------|
| 1. Preparation | Coordinate clarification, investigation and the council's concrete design, test specifications and README deltas | Task received | Self-contained preparation report; questions resolved; CONSENSUS | Stop for operator input or rerun Preparation with a new brief; never advance a blocked result |
| 2. Coding | Implement the accepted preparation result and specified tests | Successful Preparation report | Code + tests implemented, committed | Loop within Coding; design/requirement conflicts return to Preparation |
| 3. Verification | Mechanical build and test execution | Coding complete | Build passes, all tests pass | Return to Coding; no Validation until PASS |
| 4. Validating | Audit requirements, delivered changes, all council comments and completed role work | Verification passed | No High/Critical findings; apply Medium rule below | High: restart Preparation; Critical: stop and relay escalation |

## Cycle Limit

Maximum **10 full cycles** (Preparation → Validating) per task. If the 10th cycle ends with High/Critical findings still present, stop and post a summary on the GitHub issue:
```
gh issue comment <N> --repo InsanusMokrassar/WishlistApp --body "AGENT ESCALATION after 10 cycles: <summary of unresolved problems>"
```
Then terminate and wait for operator input. If the task has no linked issue, use the fallback in `## Escalation Without a Linked Issue` below.

After each stage the Orchestrator verifies that the role wrote its step report (duty defined in `agents/ALL.md`); step numbering rules are in `agents/PROTOCOL.md`.

If Preparation reports operator questions, relay every question to the operator (or the linked issue in headless mode), stop and await answers. Record answers as a new immutable artifact, then invoke Preparation with the confirmed answers. With no linked issue, surface its escalation directly. A changed requirement or design starts a new council brief and full proposals; never edit an accepted report in place. Root alone decides re-entry and passes any completed findings as evidence without disclosing a future stage.

Root pushes only after the complete cycle succeeds; issue execution may then publish the PR under its delivery instructions. Ordinary workers never push.

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
