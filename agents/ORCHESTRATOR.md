# Orchestrator

Root coordinates the existing five stages: Planning → Architecturing → Coding → Verification → Validating. It creates the task folder and immutable PROMPT, invokes one stage agent at a time, and reads the completed report before the next stage. Root does not do the stage's substantive work itself.

[Architecture](ARCHITECTURE.md) is the explicit delegation exception: that stage agent is a small council orchestrator and launches the entire [roles directory](council/roles/) as independent parallel subagents for proposals and every voting cycle. Root must not flatten the council into sequential root-owned role calls. Council workers do not spawn agents. If the host cannot support the required nested independent parallel wave, record PROCESS_FAILURE and stop for a capability/capacity fix.

## Stage State Machine

| Stage | Duty | Entry | Success exit | Failure |
|---|---|---|---|---|
| Planning | Clarify requirements, constraints, evidence and questions | Task received | Accepted Planning report | Repeat Planning or request operator input |
| Architecturing | Coordinate council planning and all-role parallel voting | Planning complete | Self-contained Architecturing report with design, test specifications, README delta and CONSENSUS | Loop within the bounded council or return a blocked outcome |
| Coding | Implement the Architecturing result and its test specifications | Successful Architecturing report | Implementation and tests committed | Return to Coding; design concerns go back to Architecture |
| Verification | Mechanically run the project build and tests | Coding complete | Build passes and all tests pass | Return to Coding; do not advance on failure |
| Validating | Audit requirements, every council comment/objection, implementation scope and proper stage work | Verification passed | No High/Critical findings; Medium rule applied | Restart Planning for High; stop/escalate Critical |

Do not add a root-owned council gate, separate reference seal, or extra requirements audit to Verification. Architecture returns the same kind of implementation-ready report as the old Architecturing stage. Coding consumes that report, while Validation can traverse council internals.

## Artifact ownership and integrity

Bootstrap and allocate steps under [PROTOCOL](PROTOCOL.md). During Architecturing, delegate exclusive allocation to Architecture and do not launch other stage writers. During council waves every voter writes only its unique assigned output; Architecture joins the entire wave before committing outputs serially. Sequential stages and Git persistence do not imply sequential council voting.

After each stage inspect the returned report, allowed output scope and repository status. Unexpected modified/deleted files must be recorded and reported; never silently restore user work. Ask before restoration unless an explicit operator instruction already resolves that exact conflict. Root handles operator questions and authorized publication via [COUNCIL_ADAPTER](COUNCIL_ADAPTER.md#operator-delivery-and-escalation).

Architecture's roster/vote-completeness checks are orchestration bookkeeping. Formal validation of requirements and council-comment dispositions, including the evidence for parallelism and independence, belongs to [VALIDATOR](VALIDATOR.md).

## Cycle Limit

Maximum **10 full cycles** (Planning through Validating) per task, separate from the council's finite voting-cycle limit. If High/Critical findings remain at cycle ten, stop and deliver a summary of unresolved findings, evidence, attempted mitigations and the exact operator decision needed. Without a linked issue, use a new report's `## ESCALATION` section and surface it directly; never invent a destination.

## Medium Findings Decision Rule

Three or more Medium findings, or any Medium touching auth, permissions or data integrity, returns to Coding. Otherwise root may proceed only after recording accepted findings and rationale in a new orchestrator report. High/Critical findings cannot be accepted by this exception.

Historical cycles are judged against their recorded instructions; no retrospective council votes are fabricated. New Architecturing entries use the current council contract.
