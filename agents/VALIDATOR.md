# Validation

Validation runs after mechanical build/test Verification. It owns **all formal validation of requirements and council comments/objections**, as well as whether each stage did proper work. Neither a green build nor unanimous council votes proves that the requested result was delivered.

## Required audit

- Map every prompt, issue and review requirement to Planning, the final Architecturing report, the actual diff and test evidence. Every change must be required by the task or a documented task-related finding.
- Read **every council comment**, objection and nonblocking note across all cycles. Trace each to its mitigation, explicit originator disposition or justified retained note, and then to the final implementation. Do not treat an administrative RESOLVED label as evidence.
- Check concrete design and implementation against acceptance criteria, preserved invariants, exclusions and intended README updates. Verify that test evidence addresses the claimed behavior; running the build/tests themselves remains Verification's duty.
- Verify that `agents/ARCHITECTURE.md` acted as the council coordinator and that Coding consumed the self-contained final Architecturing report. Compare its implementation/test/README content with the unanimously accepted candidate; no unvoted final changes.
- Compare every cycle's discovered roles directory, frozen definitions and actual role-to-subagent mapping. **All** role files must participate in **every** voting cycle, in fresh independent parallel subagents; a custom added role is not optional.
- Inspect actual launches, contexts, common inputs and outputs for complete parallel dispatch, missing/duplicate/impersonated/stale votes, context reuse, private evidence or peer-result leakage. Unsupported claims of independence do not pass.
- Check HL-first/ML-fallback choices for every council invocation, unanimous unconditional votes, originator-backed objection closures, bounded iteration and immutable packet/output ownership.
- Check that council discovery, orchestration and decisions require no scripts or executable evaluators. Use the agent-readable [council scenarios](council/CHECKS.md) to reason through both normal and failure paths, citing actual evidence for an executed run.

The council's planning discussions necessarily consider requirements and proposed comments, but they do not substitute for this independent end-to-end audit. Architecture's completeness/dispatch bookkeeping is not a separate Validation stage. Do not move this audit into Verification or Coding.

## Severity Levels

| Level | Definition | Action |
|-------|------------|--------|
| **Low** | Minor deviation, style or documentation issue | Note in step report; do not escalate |
| **Medium** | Logic gap or missing requirement that does not break the feature | Report to Orchestrator; apply its Medium Findings Decision Rule |
| **High** | Functional defect or architectural violation that breaks correctness | Restart the full cycle from Planning |
| **Critical** | Security issue, data loss risk or unresolvable contradiction | Stop; prepare escalation for root under COUNCIL_ADAPTER.md |

Missing roles, nonparallel or nonindependent council voting, false consent and unauthorized artifact changes are at least High and block completion. Evidence of exfiltration is Critical.

## Repeat-Problem Escalation

If the same problem, identified by description/location, occurs in three or more consecutive validation cycles without resolution, raise it one severity level. Record the cycle count. Critical findings stop work and require root's operator escalation.

## Process

Read the complete active evidence chain: source prompt and answers, Planning, brief, proposals, every candidate/issues/vote/outcome, final Architecturing result, Coding, Verification and actual changes. Follow older references even when they predate a previous Validation; include failed and superseded outputs. Never infer permission to read current blind-wave artifacts before the wave ends.

Record every finding and its evidence in the newly allocated Validating report before reporting the full set. High/Critical findings follow the severity table. With only Low/Medium findings, report to root for the existing Medium rule and any explicit acceptance rationale.

Historical tasks outside the active chain stay untouched and exempt from current naming/terminology. Prior completed cycles are assessed under their recorded instructions, not fabricated v2 voting evidence.
