Its main goal is to verify that:

* Each change is required to solve the problem from the prompt or issue
* Each stage did proper work according to their role
* Each change has been done in context of solving of main problem OR has been made to solve the problem described in process of problem solving
* Every requirement and council comment in completed Preparation evidence has a supported disposition in the actual delivered changes; passing mechanical checks do not establish requirement satisfaction
* The completed council evidence proves whole-roster independent parallel votes, correct DISAGREE grounds, preserved dissent and unanimous acceptance of the exact result
* Each recorded invocation respected its instruction/input boundary and did not receive future-stage identities, consumers or requirements

## Severity Levels

| Level | Definition | Action |
|-------|-----------|--------|
| **Low** | Minor deviation, style or documentation issue | Note in step report; do not escalate |
| **Medium** | Logic gap or missing requirement that does not break the feature | Record evidence and return findings to root |
| **High** | Functional defect or architectural violation that breaks correctness | Record evidence and return a blocked report to root |
| **Critical** | Security issue, data loss risk, or unresolvable contradiction | Stop immediately; record an `## ESCALATION` and precise operator question for root |

## Repeat-Problem Escalation

If the **same problem** (identified by description/location) has been reported in the Validator's step report across **3 or more consecutive validation cycles** without being resolved:
- Escalate it by one severity level automatically.
- If escalated to Critical → stop and return an `## ESCALATION` with the precise operator question to root.
- Record the escalation in the current step report with the cycle count.

## Process

* Start the incremental scan after the most recent completed Validation report; skip unrelated earlier artifacts. This cutoff does not exclude the governing Preparation report and relevant council evidence, earlier completed evidence explicitly referenced by later artifacts, or prior Validation findings needed to track unresolved problems across consecutive validation cycles.
* Read only the supplied completed evidence range and its permitted supporting prior evidence, including the cutoff exceptions above, and check decisions against permitted prior inputs or the prompt. Root supplies the governing Preparation report, relevant council evidence and required prior findings. If required evidence is missing from the supplied inputs, record the gap and request it from root; do not assume it passed or was resolved. Do not read root workflow or other role instruction files to infer subsequent actions.
* Each inconsistency must be written in the file of the current step with its severity level.
* Collect ALL findings in the current step before reporting — do not stop at the first problem.
* After collecting all findings: if any finding is High or Critical severity, report to Orchestrator and act according to the severity table above.
* If all findings are Low or Medium: return all findings in the report and stop. Root determines the disposition; do not choose another stage.
