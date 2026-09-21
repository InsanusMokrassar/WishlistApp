Model: GPT-6 Astra (HL)
Changed files: agents/task/20.09.2026_08.29.44-18128984-e5c6-4d02-bc47-7f401b72fbe7/001-planning.md

# Planning result: Accepted for Architecture

GPT-6 Astra is the assigned high-capability model and satisfies the Planning role's HL-first priority in `agents/SHORTCUTS.md`. The role-specific priority and explicit assignment govern this planning report; the general low-capability documentation preference does not turn Planning into a separate documentation role. Caveman full was used only for internal working style. This report uses normal English under `AGENTS.md` communication precedence.

## Task understanding

Issue #86 asks for a documentation-defined replacement for the existing single Architecturing stage. The replacement is a portable, iterative expert council that turns an accepted Planning result into one implementation-ready contract accepted by every required specialist. The task is to change the active repository-owned framework instructions, not to install or implement an agent runner or change WishlistApp application behavior.

The default perspectives are Architect, Programmer, Security specialist, and Designer. The Facilitator administers artifacts and iterations without selecting a technical design or resolving technical disagreements by authority. Projects may change the specialist set only through explicit configuration accompanied by an evidence-based applicability decision. Required specialists must independently propose from identical permitted evidence, review the same material plan revision, and unanimously consent before Coding receives the sealed plan.

The source of requirements is `agents/task/20.09.2026_08.29.44-18128984-e5c6-4d02-bc47-7f401b72fbe7/PROMPT.md`, copied from https://github.com/InsanusMokrassar/WishlistApp/issues/86. No earlier step report exists in this task folder. The inspected starting commit is `6f39ec99c94173c7e5164184ac32074e5f9a2164`, and the worktree was clean before this report was added.

## Operator questions and answers

No operator questions remain. No questions were asked and no additional answers were needed. The issue already defines the required perspectives, consent rule, non-authoritative facilitation, portability boundary, failure states, preservation requirement, and documentation-only verification scope.

The issue deliberately leaves the exact role-file layout, artifact schema details, and positive iteration-limit configuration to implementation design. Those choices belong to Architecture; Planning does not preselect a solution. The suggested artifact names are examples, while monotonic numbering, iteration qualification, immutable instances, and single-writer ownership are mandatory. The Facilitator is an administrative role rather than an additional technical vote: unanimous technical consent belongs to the configured required specialists, and Facilitator completeness is a separate process obligation.

## Repository evidence and existing mechanisms

`agents/ORCHESTRATOR.md:1` mandates one sequential subagent for each of five stages and prohibits root from doing role work. The state table at `agents/ORCHESTRATOR.md:10` makes one Architecturing result the entry condition for Coding. The same file's Cycle Limit section already bounds full Planning-to-Validating cycles at ten; that outer retry limit is not the issue's council-local iteration limit. Its escalation examples embed GitHub commands and the WishlistApp repository name. The new portable mechanism must address these existing integration points rather than introduce an unrelated parallel workflow.

`agents/PLAN.md` already requires resolving operator questions and producing an Architecture handoff, but does not define a frozen evidence brief or prohibit preselecting the design. Its GitHub question-delivery command is an existing repository-specific delivery concern.

`agents/PROTOCOL.md:24` restricts step suffixes to the existing five roles. Its monotonic numbering and no-overwrite rules provide useful persistence guarantees, but it has no council iteration, brief identity, proposal, review, consent, or terminal-state schemas. Its latest-step lookup at line 39 is insufficient to select a specific brief or accepted plan revision.

`agents/SHORTCUTS.md` routes only the current roles, requires reading the latest step before work, and loads `agents/ALL.md` globally. An unconditional latest-step read would expose an earlier specialist's proposal to a later blind proposer. `agents/ALL.md` also defines existing write restrictions, task-report headers, memory restrictions, feature README duties, and tool-specific search requirements. Blind-round isolation must account for inherited context and automatic startup reads as well as intentional repository reads.

`agents/MODELS.md:3` defines capability levels through vendor/model examples. `agents/GIT.md:17` names only the existing roles in commit ownership rules, and its commit trailer names a particular vendor's assistant. These are concrete coupling points to consider when separating portable contracts from repository or runner bindings. The current Planning commit still follows the current required trailer; changing active framework policy belongs to implementation, not this report.

`agents/ARCHITECTURE.md` combines general role obligations at the beginning with WishlistApp-specific architecture from its Overview onward. The general duties include current best-practice research and test specifications. The project material includes KMP targets, Gradle module naming, dependencies, feature scaffolding, plugin registration, MVVM, layer ownership, and database conventions. The task requires keeping that project guidance available while removing its role as the container for portable council mechanics.

`agents/CODING.md:34` consumes Architecture README deltas and later mandates compilation after changes. `agents/VERIFICATION.md:7` reads only the latest report and unconditionally requires a Gradle build. Those downstream consumers are relevant even though the issue's integration list does not explicitly name both files: active entry conditions and documentation-only checks must agree with the sealed-plan workflow. Existing Kotlin and feature conventions should otherwise remain intact.

`agents/VALIDATOR.md` validates role-by-role causality and severity, but its Process section skips artifacts through the previous validation report. Council validation will need the complete evidence chain for the active brief and sealed revision, including older referenced artifacts when necessary. It currently has no explicit checks for blind-input isolation, full same-revision consent, preservation of dissent, or sealing identity.

`AGENTS.md` defines communication precedence and AML-HIP for structured step-file blocks. `CLAUDE.md` is an existing runner-specific entry point, and `agents/ISSUES_EXECUTION.md` is an existing repository-specific GitHub delivery workflow. Their existence is not a reason to put runner or delivery requirements into the portable core. `README.md` points to project coding conventions and confirms the application scope. No relevant `local.*` overrides were present in the worktree root, `agents/`, or this task folder.

The investigation searched and read Markdown instructions only; no application code search or navigation was needed. `ast-index` is available, but rebuilding an application index is neither required nor appropriate for this report-only change.

## Acceptance criteria

### Portable contracts and specialist coverage

The council core must be expressible through Markdown contracts and durable task artifacts, reusable in a non-WishlistApp repository without changing its semantics. It must not require a particular vendor, CLI, agent framework, subagent API, programming language, or application stack. Invocation contracts must identify role instructions, allowed inputs, required output path, write scope, and completion criteria. Repository guidance and external-delivery commands must remain outside that core. Architect, Programmer, Security specialist, Designer, Facilitator, and the final sealing responsibility must have complete obligations and capability-based routing. Omissions or additions to the specialist set require explicit configuration and applicability evidence.

### Frozen inputs and blind proposals

Planning must supply acceptance criteria, constraints, known facts, repository evidence, known unknowns, and excluded scope without choosing a design. The Orchestrator must create an immutable brief with a positive iteration limit. A material input change requires a new brief and a restarted blind proposal round. Every required specialist must receive the same brief and permitted repository evidence and must not receive another proposal or review before producing its first-round proposal. Sequential and parallel execution are both valid only when the adapter can guarantee that isolation. Proposals must cover assumptions, evidence, recommended approach, alternatives, concrete impact, risks, tests, blockers, confidence, and excluded scope.

### Shared plan, objections, and unanimous review

The Facilitator must preserve provenance and objections while normalizing proposals into a candidate plan and issue ledger. It may deduplicate claims but may not select disputed technical positions, erase dissent, or declare objections resolved by preference. All required specialists must review one identified revision using `AGREE`, `AGREE_WITH_NOTES`, or a structured objection. A blocking objection must specify the violated requirement or invariant, concrete failure scenario, evidence, and a feasible mitigation or precise operator question. Unsupported preference must not block. Every material revision requires a complete fresh review set; consent from an older revision cannot substitute for current review. Neither majority vote, model identity, nor facilitator preference can override a valid objection.

### Bounded outcomes and sealed implementation input

The council must terminate as `CONSENSUS`, `NEEDS_INFORMATION`, `IRRECONCILABLE`, or `PROCESS_FAILURE` under the issue's definitions. Exhausting the positive iteration limit must escalate unresolved work rather than fabricate agreement. Missing or invalid required roles, artifacts, isolation guarantees, or iteration records must be process failures. Any unresolved outcome must produce agreed sections, open issues, positions, evidence, attempted resolutions, and the exact decision or information required. Only `CONSENSUS`, with unanimous current-revision consent and zero blocking issues, may enter Coding.

The sealed contract must be substantively identical to the accepted revision and include rationale, rejected alternatives, implementation order, affected components/files/symbols, API/data/auth/UI/migration/rollout/rollback effects, tests mapped to all changes and acceptance criteria, assumptions, confidence, and a dissent register. Substantive edits during sealing invalidate consent and require another complete review round. Inapplicable impact areas must be accounted for explicitly rather than silently omitted.

### Persistence, integration, and verification

Each artifact instance must have one writer, a unique monotonic global step number, and sufficient council-local iteration identity to distinguish repeated rounds. No historical artifact may be overwritten or retroactively normalized. Task artifacts alone must support auditing the active brief, allowed inputs, proposals, issue history, review completeness, consent, and sealed-plan identity. The Validator must detect information-flow violations and incomplete or stale consent, not merely check whether a final file exists.

Active links, role names, stage names, examples, write scopes, and consumers must be mutually consistent. WishlistApp-specific architecture evidence must remain available separately. `git diff --check` must pass. Application builds are explicitly unnecessary for documentation-only changes under this issue; a passing build would not prove council correctness.

## Concrete work plan and constraints

Architecture should first define the portable-core boundary and how the current framework binds to it, using the existing files and mechanisms identified above. Architecture must then specify the council state transitions, immutable artifact contracts, allowed information flow, role applicability, objection disposition, review completeness, and sealed-plan identity checks. That design must account for the distinction between the existing outer workflow cycle limit and the new council-local limit.

The implementation plan must cover coordinated updates to `agents/PROTOCOL.md`, `agents/ORCHESTRATOR.md`, `agents/PLAN.md`, `agents/SHORTCUTS.md`, `agents/MODELS.md`, `agents/ALL.md`, `agents/GIT.md`, `agents/VALIDATOR.md`, and `agents/ARCHITECTURE.md`, together with the required portable role contracts. Architecture should identify the smallest necessary downstream changes to `agents/CODING.md`, `agents/VERIFICATION.md`, and other active entry points when an existing directive would contradict the new workflow. The exact new filenames, schema representation, adapter placement, and identity mechanism remain Architecture decisions.

Coding should apply the approved documentation contract and synchronized references, preserving unrelated application conventions and historical reports. Verification should check the documented scenarios and acceptance mapping specified by Architecture, cross-file consistency, preservation scope, and whitespace. Validating should independently assess the complete active council evidence chain and the implementation's adherence to the issue.

This task concerns repository-owned framework sources, not installed or Workshop-owned skills. No plugin installation, reusable-skill publication, application migration, dependency upgrade, UI implementation, runtime orchestration engine, or new external service is requested. Do not edit `.gitignore`, historical task artifacts, application sources, feature Operator Notes, or unrelated machine/workspace configuration. No push or external message is part of this Planning role.

## Known unknowns and design risks

No operator-owned requirement is missing. Architecture must resolve the exact artifact schemas and file organization; how material revision identity is established and verified; how adapters document and guarantee blind context; how evidence changes restart the brief; how role-configuration applicability is recorded; how the positive council limit is supplied; and how incomplete, duplicate, stale, or contradictory responses are classified. These are bounded design responsibilities, not assumptions authorizing weaker consent.

Important failure cases include sequential proposers inheriting earlier proposals through startup or shared history; late repository evidence silently changing one specialist's inputs; an older review being counted against a newer plan; facilitation turning deduplication into loss of dissent; an omitted role being treated as implicit consent; exhausted rounds falling through to Coding; a sealing rewrite changing meaning; and a Validator that cannot reconstruct the complete active history. The final architecture must include verification specifications for these failures and for successful first-round and revised-round consensus. Documentation checks should not claim to prove a runner's actual isolation unless the adapter supplies evidence for that guarantee.

## Architecture handoff

Planning is complete and accepted for the Architecture stage under the currently active workflow. This status is not council consensus and does not authorize Coding. The next role should read this report and the task prompt, perform the architecture research required by the current role contract, and produce a concrete documentation implementation contract with tests or check specifications mapped to every acceptance criterion and planned change. Preserve all mandatory council invariants while selecting the smallest coherent document and adapter design. If Architecture uncovers a genuinely missing operator constraint, report the precise question through the Orchestrator before Coding proceeds.

Only this Planning report was created. No framework instructions, application files, or historical task reports were changed.
