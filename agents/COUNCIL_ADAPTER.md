# WishlistApp Council Adapter

This repository binds the portable [protocol](PROTOCOL.md) and [roles](council/README.md). It does not claim an installed isolation runner. The main-session Orchestrator is root; role agents never spawn roles. Dispatch one role invocation at a time, with allocated outputs and serialized Git persistence. The portable protocol permits isolated parallel proposals, but this repository adapter remains sequential.

## Configuration and launch

Artifacts live in `agents/task/<TASK_ID>/`. Default specialists are architect, programmer, security and designer; default limit is **three candidate rounds**, explicitly copied into each brief. A task may configure another positive finite limit before freezing. The separate outer workflow limit remains ten full cycles. Never increase an active brief's budget to evade exhaustion.

Bind capabilities using [MODELS](MODELS.md), recording actual model and adequacy in launches/reports. Role-level technical reasoning outranks the generic LL documentation preference. Preserve the [Git policy](GIT.md), including its repository-specific trailer. Each role commits only allocated outputs; Coding additionally commits task-authorized implementation and documentation. Root checks status after each invocation and never silently restores unexpected changes.

Before any automatic latest-report read, identify council mode and its manifest. Freeze applicable AGENTS/ALL/tool/project instructions, every relevant local override and the entire portable role bundle into identical common inputs. Reject overrides that weaken immutability, isolation or unanimous consent. Include [ARCHITECTURE](ARCHITECTURE.md) only as identically supplied project evidence, not as an implicit specialist startup file.

Blind invocations require clean contexts, not a fork of root history that already contains proposals. Use an inspectable read-only snapshot/private-output boundary or equivalent demonstrable restriction. Deny non-manifest task files, history, Git diffs, indexes, memory, messages and live retrieval. Record actual context/input identities, successful/denied reads, writes and tool activity into launch/receipt artifacts. If the runner cannot expose and enforce that boundary, terminate PROCESS_FAILURE; a self-declaration or prompt instruction is insufficient. No runner installation is part of this framework.

Project navigation uses the frozen source revision. Existing ast-index rules apply only when the index is bounded to that source snapshot and cannot include sibling artifacts. Required current research is collected before freezing, retained with content/source/time/digest and distributed identically. Missing evidence uses intake and a new brief, never unilateral blind web searches.

## Serialization and integrity

The portable schema defines semantics. Structured fields here use complete AML-HIP blocks under root AGENTS.md; narrative remains normal prose. Put model and changed-file headers at the beginning of role reports; hash the entire finalized UTF-8 artifact including headers. No self-hash is embedded. All references retain bytes and complete digests. The adapter serializes publication to capture predecessor digests even if an alternative isolated runner completes invocations out of order.

This complete encoding example illustrates an allocation finding, not a fabricated successful council record. Concrete council artifacts additionally populate every field required by their protocol schema.

```text
ENTITY:
entity_id=allocation_example; type=allocation_finding; state=reserved
CONTEXT:
* task_id=07.07.2026_14.30.12-f47ac10b-58cc-4372-a567-0e02b2c3d479; agent_id=orchestrator; memory_ref=[]
* constraints=[immutable_outputs,single_writer]; protocol_version=council-v1
ACTION:
1. action=reserve; target=allocation_example; params={global_step:2,writer:architect,mode:proposal}
REASON:
* condition=accepted_brief; requirement=allocate_before_dispatch
EXPECTED RESULT:
* entity_id=allocation_example; new_state=reserved; location=task_artifacts
VERIFICATION:
* check=allocation_ownership; expected=one_writer
UNCERTAINTY:
* missing=none; ambiguity=none
REPETITION OF RESULT:
* entity_id=allocation_example; stored_in=shared_memory; status=available
COMMUNICATION:
* sender=orchestrator; receiver=architect; task_id=07.07.2026_14.30.12-f47ac10b-58cc-4372-a567-0e02b2c3d479; message_id=2423ef91-66a1-4f66-b893-e86f254463d5; protocol=AML-HIP
PERSISTENCE:
* local_memory=true; shared_memory=true; index_keys=[task_id,allocation_example]
VALIDATION:
* format_valid=true; no_pronouns=true
* entities_explicit=true; high_density=true
* causal_chain_present=true; ambiguity_detected=false
```

Persistence labels identify durable task artifacts, not disabled assistant auto-memory.

## Operator delivery and escalation

Roles write questions/findings/terminal packages; only the main Orchestrator communicates externally. For an already-authorized issue-execution task linked to an issue, publish the exact prepared package through the existing transport:

```sh
gh issue comment <N> --repo InsanusMokrassar/WishlistApp --body-file <prepared-file>
```

Prepare actual multiline text without interpolating untrusted artifact content into shell code. Otherwise surface the same package in the main-session response; never invent a destination. With no linked issue, include an `## ESCALATION` section in a newly allocated report. Transport failure is reported separately and never changes a council terminal state or enables Coding. Read issue answers on resumption and persist them as immutable operator-input artifacts before Planning/brief restart.

The ten-cycle escalation includes unresolved findings, positions, evidence, attempted mitigations and the exact operator decision needed. This policy also handles Planning questions, process failures and repeated-attempt issue escalation. It grants no additional permission to publish or push.

## Adoption and checks

Already-started legacy cycles, including issue #86, finish using their recorded pre-change contract. New Architecture entries and fresh restarted cycles use the council gate. Do not fabricate retrospective consensus. Run the portable [checks](council/CHECKS.md) and repository static preservation checks. Keep historical tasks untouched. Rollback is a reviewed framework-documentation revert preserving all reports; version changes require a new attempt, not recycled consent.
