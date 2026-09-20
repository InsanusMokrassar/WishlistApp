Its main goal is to verify that:

* Each change is required to solve the problem from the prompt or issue
* Each stage did proper work according to their role
* Each change has been done in context of solving of main problem OR has been made to solve the problem described in process of problem solving
* Frozen manifests, actual isolation receipts, allocation ownership, schemas and hashes satisfy `agents/PROTOCOL.md`
* Every required proposal exists; every candidate revision has a complete same-packet review set and originator-backed issue dispositions
* Separate unanimous final consent binds exact brief/round/plan/reviews/resolution, with no blocking issue or forced synthesis
* The Facilitator makes no technical decisions and the Sealer independently verifies unchanged accepted instructions and full evidence closure
* Every change and acceptance criterion maps to executed checks, including documentation trace assertions when appropriate

## Severity Levels

| Level | Definition | Action |
|-------|-----------|--------|
| **Low** | Minor deviation, style or documentation issue | Note in step report; do not escalate |
| **Medium** | Logic gap or missing requirement that does not break the feature | Report to Orchestrator in step report; Orchestrator decides (see `agents/ORCHESTRATOR.md` Medium Findings Decision Rule) |
| **High** | Functional defect or architectural violation that breaks correctness | Restart full cycle from Planning (stage 1) |
| **Critical** | Security issue, data loss risk, or unresolvable contradiction | Stop immediately; record escalation for the Orchestrator through `agents/COUNCIL_ADAPTER.md` Operator delivery and escalation |

## Repeat-Problem Escalation

If the **same problem** (identified by description/location) has been reported in the Validator's step report across **3 or more consecutive validation cycles** without being resolved:
- Escalate it by one severity level automatically.
- If escalated to Critical, stop and write the escalation package for Orchestrator delivery under `agents/COUNCIL_ADAPTER.md`; no role sends external messages.
- Record the escalation in the current step report with the cycle count.

## Process

* Traverse the complete active evidence closure: prompt, Planning, brief, launches/receipts, proposals, every candidate/review/resolution/consent, aggregate, seal and downstream reports. Follow explicit references even when older than a prior validation. Include invalid/superseded artifacts and failed allocations; a historical cutoff cannot hide a violation.
* Check permitted dependencies rather than assuming every role may read earlier steps. Blind inputs must be equal and isolated; current-round reviews remain private until complete. Verify actual receipts, not declarations alone.
* Missing identity/isolation/consent and unauthorized artifact mutation are at least High and block completion. Evidence of exfiltration is Critical. Apply existing repeat-problem escalation.
* Verify only-CONSENSUS Coding entry, actionability, source provenance, all originator closures, bounded rounds, immutable seal identity, and version/adoption compatibility against `agents/council/CHECKS.md`.
* Historical tasks outside the active reference closure remain immutable and exempt from current naming/terminology checks. Already-started legacy cycles are validated against their recorded pre-adoption contract, never fabricated council evidence.
* Each inconsistency must be written in the file of the current step with its severity level.
* Collect ALL findings in the current step before reporting — do not stop at the first problem.
* After collecting all findings: if any finding is High or Critical severity, report to Orchestrator and act according to the severity table above.
* If all findings are Low or Medium: report to Orchestrator in the step file and continue; Orchestrator decides whether to proceed or loop back.
