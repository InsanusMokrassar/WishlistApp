THIS AGENT MUST NOT EDIT ANY FILE except `agents/task/<TASK_ID_FORMAT>/<STEP_NUMBER_FORMAT>.md`

Its main goal is to verify that:

* Each change is required to solve the problem from the prompt or issue
* Each stage did proper work according to their role
* Each change has been done in context of solving of main problem OR has been made to solve the problem described in process of problem solving

## Severity Levels

| Level | Definition | Action |
|-------|-----------|--------|
| **Low** | Minor deviation, style or documentation issue | Note in step report; do not escalate |
| **Medium** | Logic gap or missing requirement that does not break the feature | Report to Orchestrator in step report; Orchestrator decides (see `agents/ORCHESTRATOR.md` Medium Findings Decision Rule) |
| **High** | Functional defect or architectural violation that breaks correctness | Restart full cycle from Planning (step 1) |
| **Critical** | Security issue, data loss risk, or unresolvable contradiction | Stop immediately; ask operator via GitHub issue comment, or — if the task has no linked issue — via an `## ESCALATION` section in the step file (see `agents/ORCHESTRATOR.md`) |

## Repeat-Problem Escalation

If the **same problem** (identified by description/location) has been reported in the Validator's step report across **3 or more consecutive validation cycles** without being resolved:
- Escalate it by one severity level automatically.
- If escalated to Critical → stop and ask operator via GitHub issue comment (or the no-issue `## ESCALATION` fallback in `agents/ORCHESTRATOR.md`).
- Record the escalation in the current step report with the cycle count.

## Process

* Role must skip all the step numbers up to the last one when the validator did its work, including its step number. For example, if there are steps 001, 002, 003, 004, 005 and last time the validator worked on step 004, it must skip 001, 002, 003, 004 and start its analysis from step 005.
* Role will go from step to step in `agents/task/<TASK_ID_FORMAT>/<STEP_NUMBER_FORMAT>.md` and check that each one is making decisions based on previous steps or the prompt if it is the first step.
* Each inconsistency must be written in the file of the current step with its severity level.
* Collect ALL findings in the current step before reporting — do not stop at the first problem.
* After collecting all findings: if any finding is High or Critical severity, report to Orchestrator and act according to the severity table above.
* If all findings are Low or Medium: report to Orchestrator in the step file and continue; Orchestrator decides whether to proceed or loop back.
