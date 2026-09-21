# Adopt a universal iterative expert council for architecture planning

Source: https://github.com/InsanusMokrassar/WishlistApp/issues/86

## Problem

The current agent workflow has one Architecturing stage responsible for converting an accepted plan into the implementation contract. This concentrates architecture, implementation feasibility, security, and user-experience reasoning in one perspective.

The framework under `agents/` is also reused outside WishlistApp. The replacement must therefore be repository-agnostic and tool-neutral: it must not depend on Claude teams, Codex collaboration features, AutoGen, CrewAI, LangGraph, or another runner-specific council implementation.

## Goal

Replace the single Architecturing stage with a reusable expert-council protocol that produces one implementation-ready plan accepted by every required perspective before Coding begins.

Default specialist roles:

- **Architect** — boundaries, dependencies, interfaces, data ownership, invariants, evolution, and rollback.
- **Programmer** — concrete implementation feasibility, affected files and symbols, dependency compatibility, operational complexity, and testability.
- **Security specialist** — trust boundaries, authentication, authorization, secrets, privacy, abuse cases, integrity, and recovery.
- **Designer** — end-to-end user/operator flows, accessibility, feedback, error states, recovery, consistency, and observable outcomes.
- **Facilitator** — administers artifacts and iterations but has no authority to choose a design, erase dissent, or resolve technical disputes.

Projects may add or omit specialist roles only through explicit configuration and an evidence-based applicability decision.

## Required council workflow

1. **Planning and frozen brief**
   - Planning resolves operator questions and records acceptance criteria, constraints, known facts, repository evidence, known unknowns, and excluded scope.
   - The Orchestrator creates an immutable council brief.
   - If material input changes, create a new brief and restart the blind proposal round; never mutate the active brief.

2. **Blind independent proposals**
   - Every required specialist receives the same brief and the same allow-listed repository evidence.
   - A first-round proposer must not read another proposal or review.
   - Each role records assumptions, evidence, recommended approach, alternatives, concrete impact, risks, tests, blockers, confidence, and excluded scope.

3. **Shared plan and issue ledger**
   - The non-authoritative Facilitator normalizes the proposals into a shared-plan candidate and an issue ledger.
   - The Facilitator may deduplicate and trace claims but may not decide disputed claims or suppress objections.

4. **Unanimous review**
   - Every required specialist reviews the same plan revision.
   - Allowed dispositions are `AGREE`, `AGREE_WITH_NOTES`, or a structured objection.
   - A blocking objection must identify a violated requirement or invariant, a concrete failure scenario, supporting evidence, and a feasible mitigation or a precise operator question.
   - Unsupported preference cannot block.

5. **Evidence-based convergence loop**
   - Resolve objections with evidence, mitigation, or operator clarification.
   - Revise the shared plan.
   - After every material revision, every required role reviews the new revision again.
   - Repeat until all required roles return `AGREE` or `AGREE_WITH_NOTES` and no blocking issue remains.
   - Do not use majority voting, model identity, or facilitator preference as decision criteria.

6. **Sealed implementation contract**
   - After unanimous consent, seal the accepted revision without substantive changes as the final architecturing plan consumed by Coding.
   - The final plan must include rationale, rejected alternatives, implementation order, affected components/files/symbols, API/data/auth/UI/migration/rollout/rollback effects, tests mapped to every change and acceptance criterion, assumptions, confidence, and a dissent register.
   - Any substantive change during sealing invalidates consent and requires another complete review round.

## Bounded termination

The frozen brief sets a positive iteration limit. Reaching the limit must never force agreement or handoff.

Terminal states:

- `CONSENSUS` — unanimous consent, no blocking issues, actionable sealed plan. Only this state may proceed to Coding.
- `NEEDS_INFORMATION` — a precise operator answer or missing evidence is required.
- `IRRECONCILABLE` — valid constraints or objections remain incompatible after the allowed rounds.
- `PROCESS_FAILURE` — a required artifact, role response, isolation guarantee, or iteration record is missing or invalid.

An unresolved run must produce an escalation package containing agreed sections, open issues, positions, evidence, attempted resolutions, and the exact decision or information required.

## Portability requirements

The portable council core must:

- define behavior through Markdown contracts and durable artifacts;
- make no assumption about a particular LLM vendor, CLI, subagent API, team API, or programming language;
- use a generic invocation contract: role instructions, allow-listed inputs, required output path, write scope, and completion criteria;
- support sequential or parallel blind proposals when the adapter can guarantee isolation;
- keep repository-specific architecture guidance and external-delivery commands outside the portable core;
- use iteration-qualified, monotonic artifact names so repeated reviews never overwrite history;
- remain auditable from task artifacts alone.

## Suggested artifacts

The exact global step numbers remain monotonic. Council-local iteration identifiers should distinguish repeated rounds, for example:

- `NNN-council-brief.md`
- `NNN-proposal-architect.md`
- `NNN-proposal-programmer.md`
- `NNN-proposal-security.md`
- `NNN-proposal-designer.md`
- `NNN-council-i01-plan.md`
- `NNN-council-i01-issues.md`
- `NNN-council-i01-review-architect.md`
- `NNN-council-i01-review-programmer.md`
- `NNN-council-i01-review-security.md`
- `NNN-council-i01-review-designer.md`
- `NNN-council-i02-plan.md`, followed by a complete new review set after revision
- `NNN-council-consent.md`
- `NNN-council-final.md`

Each artifact instance must have exactly one writer and must never be overwritten.

## Repository integration

Update the active framework documentation consistently:

- `agents/PROTOCOL.md` — portable council protocol, schemas, artifact naming, convergence, consent, and terminal states.
- Portable council role instructions — complete obligations for Architect, Programmer, Security, Designer, Facilitator, and final sealing/synthesis.
- `agents/ORCHESTRATOR.md` — replace the single Architecturing stage with the council state machine.
- `agents/PLAN.md` — produce the inputs for the frozen brief without preselecting a design.
- `agents/SHORTCUTS.md` and `agents/MODELS.md` — route roles by capability rather than vendor.
- `agents/ALL.md` and `agents/GIT.md` — enforce input isolation, write scopes, and artifact ownership.
- `agents/VALIDATOR.md` — verify blind-round isolation, complete review sets, dissent disposition, final consent, and sealed-plan identity.
- `agents/ARCHITECTURE.md` — retain WishlistApp-specific KMP and project architecture evidence, separate from the portable mechanics.

Preserve historical task artifacts unchanged.

## Acceptance criteria

- [ ] Council mechanics are reusable in a non-WishlistApp repository without semantic changes.
- [ ] Portable core contains no dependency on a particular agent framework or model vendor.
- [ ] Architect, Programmer, Security, Designer, and Facilitator have complete role contracts.
- [ ] First-round proposals are blind and receive identical allowed inputs.
- [ ] Every material plan revision is reviewed by every required role.
- [ ] Consensus requires unanimous consent and zero blocking issues; no majority override exists.
- [ ] Facilitator cannot select the design, erase dissent, or declare technical objections resolved.
- [ ] Iteration limit escalates unresolved work rather than forcing synthesis.
- [ ] Only `CONSENSUS` can hand off to Coding.
- [ ] Final Coding input is identical in substance to the unanimously accepted revision.
- [ ] Iteration-qualified artifacts are monotonic and never overwritten.
- [ ] WishlistApp-specific guidance remains available but separate from the portable protocol.
- [ ] Validator detects information-flow violations and incomplete consent.
- [ ] Documentation links, role names, stage names, and examples are internally consistent.
- [ ] `git diff --check` passes; application builds are unnecessary for documentation-only changes.


