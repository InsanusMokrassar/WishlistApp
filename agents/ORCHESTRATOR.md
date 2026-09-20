# Orchestrator

Root IS THE MAIN SESSION, NEVER A SUBAGENT. Root performs orchestration only, not Planning, technical design, Coding, Verification or Validating. Role subagents do not spawn roles. Follow the [repository adapter](COUNCIL_ADAPTER.md) and [portable protocol](PROTOCOL.md). Dispatch one role invocation at a time with explicit inputs/outputs; inspect each result before the next dependent invocation. The portable protocol also supports isolated parallel proposals, but this adapter uses sequential execution and persistence.

Bootstrap the task folder and immutable PROMPT per the protocol. Record answers in newly allocated operator-input artifacts, never PROMPT addenda. Root may transcribe accepted facts/configuration into a brief and captured adapter evidence into receipts; it must not make technical choices.

## Stage State Machine

| Stage | Entry | Duty and success exit | Failure |
|---|---|---|---|
| Planning | Task received | Clarify acceptance, facts, evidence, constraints and questions; accepted Planning artifact | Repeat Planning or request information |
| Architecturing / Council | Accepted Planning | Frozen brief, blind proposals, attributed candidate, complete reviews, derived resolution, unanimous separate consent, verified reference seal | Terminal package; only CONSENSUS advances |
| Coding | Verified CONSENSUS seal, exact accepted plan and resolution | Implement contract and mapped tests; commit changes/report | Return to Coding; design concerns stop for council re-entry |
| Verification | Coding complete | Required builds/tests or documentation static/trace checks pass | Return to Coding; no Validating on failure |
| Validating | Verification passes | Complete active evidence closure and scope validated; no High/Critical findings | Restart from Planning for High; stop/escalate Critical |

Already-started legacy cycles may finish their recorded pre-adoption five-stage contract. This includes the council-framework migration itself; do not invent consensus evidence. New Architecture entries and fresh restarted cycles require the council seal.

## Council dispatch and guards

1. Preflight role applicability, capabilities, positive configured limit, identical frozen evidence/research and inspectable isolation. Persist accepted brief and allocation/launch records before dispatch.
2. Dispatch each required specialist proposal in a clean context with identical common manifest. Validate output ownership/schema/hashes and independent receipt. No shared phase until every required proposal passes.
3. Allocate Facilitator plan/issues outputs for i01. Every candidate consumes a round. Preserve conflicts as unresolved alternatives, never ask root or Facilitator to choose.
4. Dispatch every required same-packet review without current-round sibling reviews. Collect the entire set unless a process or information failure terminates the attempt.
5. Allocate post-review resolution. Check originator dispositions and complete dissent history. A sourced material mitigation uses a new plan/issues round and full fresh reviews. Missing facts yield NEEDS_INFORMATION; valid incompatibility at the limit yields IRRECONCILABLE; missing/invalid artifacts yield PROCESS_FAILURE.
6. With all accepting reviews and zero blockers, dispatch separate consents for the same plan, review set and resolution. Validate exactly the configured specialist set. A new valid objection follows the bounded revision path, never forced assent.
7. Allocate Facilitator aggregate and a separate Sealer invocation with full evidence closure. Independently verify the seal references the unchanged accepted bytes and resolution and every gate passes. Only CONSENSUS enables Coding.

All terminal states are final for the brief. Answers/repairs require a new linked brief and fresh blind proposals. No majority/model override, inferred consent, role omission, altered active budget, or unrecorded technical message can advance state. Root allocates every number; failed gaps stay reserved and exhaustion at 999 requires a linked new task.

## Cycle Limit

Maximum **10 full cycles** (Planning through Validating) per task, separate from council candidate rounds. If High/Critical findings remain after cycle ten, stop and produce the complete escalation summary through [Operator delivery and escalation](COUNCIL_ADAPTER.md#operator-delivery-and-escalation). Missing linked issues use the same local package, not a fabricated destination.

## Subagent Integrity Check

After every invocation verify allocated output/report existence, permitted input dependencies, schemas, hashes, ownership, receipts and completion criteria. Inspect `git status` against [ALL](ALL.md) write restrictions. Unexpected modified/deleted files must be recorded and reported; do not silently revert. Ask the operator before restoring unless explicit authorization already resolves that exact conflict.

Problems and stage incompatibilities are recorded in a new step report and routed through the state machine. Delivery and publication remain root-owned under the adapter, never a role responsibility.

## Medium Findings Decision Rule

Three or more Medium findings, or any Medium touching auth, permissions or data integrity, returns to Coding. Otherwise root may proceed after recording accepted findings and rationale in a new orchestrator artifact. High/Critical findings cannot be accepted by this exception.
