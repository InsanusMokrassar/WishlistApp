# WishlistApp Council Bindings

The outer workflow is [ORCHESTRATOR](ORCHESTRATOR.md). Its Architecturing stage is implemented by the small [Architecture coordinator](ARCHITECTURE.md), using [PROTOCOL](PROTOCOL.md) and every role in [council/roles/](council/roles/).

## Repository inputs

- Artifacts live in `agents/task/<TASK_ID>/`; all output names use the existing task-global numbering.
- [PROJECT_ARCHITECTURE](PROJECT_ARCHITECTURE.md) retains the project's architecture facts, module conventions and feature guidance. Supply relevant evidence, feature Operator Notes and applicable local overrides identically in the common packet.
- Read and freeze applicable repository instructions before dispatch. Local overrides may refine project policy but cannot authorize omitted voters, sequential voting, LL council work, mutable artifacts or forced agreement.
- Required best-practice research is collected and retained before the common brief freezes. Missing facts are returned through root for intake, not gathered privately by one voter.
- The default is three voting cycles per brief. Another positive finite limit must be declared before the attempt; do not enlarge an active budget to force completion.
- Bind models according to [MODELS](MODELS.md): Architecture and all council roles prefer HL, then adequate ML. Record actual choices and fallback reasons.

## Native agent execution and persistence

The host must provide independent clean-context subagents, enough capacity for the full roster concurrently, and native file tools for private outputs. Architecture dispatches **all discovered roles in parallel for every voting cycle**, and also for initial proposals. Start the whole wave before waiting. No custom runner, script, replay evaluator, schema parser or hashing service is required.

Each role receives the same frozen evidence/candidate packet, its own instructions/identity and a unique output path. No current-wave sibling reports, parent-history forks or private messages are allowed. Use available native access restrictions, retain available invocation evidence and state visibility limits; do not require a custom tracing system. If the host cannot provide independent task contexts or a peer-input violation is observed, stop with PROCESS_FAILURE rather than claim an unobserved guarantee.

Ordinary project navigation tools remain available for preparing the common evidence, and normal build/test commands remain in Verification. These tools do not orchestrate or decide the council. Voting workers neither modify application files nor perform Git operations. Architecture joins the complete wave and commits only allocated artifacts serially under [GIT](GIT.md); never serialize the voting itself to avoid an index race.

Reports keep Model and Changed files headers, explicit input/output paths and normal-prose reasoning. Structured fields follow AGENTS.md where applicable; no machine parser or cryptographic certificate is required. All final implementation guidance is present in the ordinary Architecturing report.

## Operator delivery and escalation

Roles return questions and findings to Architecture; Architecture returns blocked reports to root. Only root handles authorized external delivery. In an issue-linked task, post the prepared multiline report through the existing GitHub transport. In a prompt-only task, surface it directly and include an `## ESCALATION` section in the allocated report. Never guess another destination.

Record answers in new immutable operator-input reports. Changed requirements or evidence return through Planning and a new brief; do not append to PROMPT or reuse old votes. Transport failure does not alter a council result or permit Coding.

The ten-full-cycle and failed-issue-attempt escalation policies continue to apply. These bindings grant no extra permission to publish, push or merge.

## Audit and adoption

[Validation](VALIDATOR.md) reads requirements, all council comments and implementation evidence using the agent-readable [CHECKS](council/CHECKS.md). Verification remains a mechanical project build/test stage and does not inspect council voting certificates.

Preserve historical task files unchanged. New Architecturing entries use the current agent-only process. A future rollback preserves all reports and starts a new attempt under the selected instructions, never recycles votes.
