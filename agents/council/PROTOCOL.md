# Council Protocol

Preparation coordinates the council; the council [Architect](roles/ARCHITECT.md) performs substantive investigation, architecture research, design and test planning. The outer coordinator collates participant-authored content and manages the process, not its own technical solution. Participants apply only [COMMON](COMMON.md), this protocol, [MODELS](MODELS.md), and their assigned role contract. Role links identify responsibilities, not permission to load a peer's contract.

## Allocated artifacts

Preparation receives the source task, confirmed answers, permitted completed evidence and an allocated final Preparation report path from root. Store council evidence in a new `council/<attempt-id>/` subdirectory of that report's task folder, using a unique attempt ID. Do not place council artifacts among the ordinary top-level stage reports or consume their step numbers.

Within an attempt, use monotonically increasing `NNN-<artifact-name>.md` names from 001 through 999. Preparation reserves every wave's distinct output paths before launching agents. Parallel completion order need not match allocation order. Each output has one writer; failed allocations remain gaps with recorded reasons. Once returned as a completed output, an artifact is immutable. Corrections get a new allocation and explicit supersession. Stop at number exhaustion; never wrap or overwrite. Participants identify the active decision inputs from their assigned packet, never a latest-step lookup; repository and task-history reads are permitted under the isolation rules below.

## Agents-only execution boundary

Agents perform discovery, planning, collation, voting, outcome recording and validation using the host's native subagent, file and research tools. No custom script, executable fixture, parser, hash checker, automation service, or code-based evaluator is required to execute the council. Decisions come from recorded agent reasoning and votes, not from a script-produced certificate.

This restriction concerns the council process, not repository navigation or native research tools. Proposed application test commands may be specified as part of the design; a council vote does not claim their execution.

## Roster and capability

The entire `agents/council/roles/` directory is the voting roster. Preparation recursively reads **every Markdown file**, records its path and declared `Role ID`, and loads its complete instructions. Each file defines one role; IDs must be unique lowercase letters/digits/hyphens beginning with a letter. The directory must be nonempty. Non-role instructions or an invalid/duplicate ID are a process error, never a reason to silently skip a file. Keep README, shared instructions and checklists outside this directory.

The shipped roles are examples of the initial roster, not an allow-list. Adding a valid role file automatically adds a voter without changes to routing or a script. There are no optional, nonvoting or not-applicable roles in this directory: a role with no domain-specific concern must still review the complete plan and cast an explicit vote. Preparation is the nonvoting coordinator outside the directory.

Preparation re-enumerates the directory at the start of **every** voting cycle and compares paths and instruction content with the brief. If any role is added, removed or changed, invalidate the active attempt, preserve its evidence, and start a new brief with the whole current roster and fresh proposals. Recheck before final publication as well; a changed roster can never inherit previous votes. Changes do not extend the outer task budget.

Every council role, in every mode and cycle, prefers **HL**, with **ML** only as the lower-priority fallback. Record the actual model and why ML was necessary. Never select LL for council planning or voting, and never omit a role because HL is unavailable. Missing adequate HL/ML capability is `PROCESS_FAILURE`.

## Common inputs and independent parallel waves

A brief records source-prompt and permitted completed-input paths; task understanding and requirements/acceptance IDs; confirmed answers, constraints, exclusions and unknowns; repository revision, evidence paths and retained research; the full roster IDs/paths; model policy; and a positive finite maximum voting-cycle count. Preparation separately freezes the full role contracts for roster-integrity checks, but each participant receives only its own role contract plus shared instructions, never other role instruction contents. The default is five cycles. Freeze the brief before proposals; the common packet and evidence-access rules are identical apart from active role contract and assigned output. Participants may independently inspect repository source/configuration/documentation and permitted completed task history beyond named evidence paths. Exclude root workflow, other role instructions and copies that disclose future-stage identities, consumers or requirements. Relevant completed evidence may be supplied as excerpts without altering historical records.

Preparation launches one **fresh independent subagent per role in parallel**, both for initial proposals and for **every voting cycle**. Launch the entire roster before waiting for results. Do not batch the roster sequentially, simulate multiple roles in one agent, reuse a previous-cycle agent's private conversation, or stop launching because an early result disagrees. Reserve enough concurrent subagent capacity for the whole wave. If the host cannot provide it, stop with `PROCESS_FAILURE` and ask root to resolve capacity; there is no sequential fallback.

Each agent receives the explicit frozen packet in a clean task context, not a fork of Preparation's conversation, plus its private output assignment and equal read access to the repository and task history. Use the host's native input/tool restrictions where available. Participants must not read other participants' proposal, review or vote artifacts from the current proposal wave or voting cycle, including copies in task history, Git history, search results or messages. Exclude those artifacts from searches before reading results. Private sibling messages and persistent agent memory are also forbidden. Permitted completed task reports and prior-cycle artifacts may be read as evidence within the instruction boundary; permitted history access is not permission to inherit a prior agent's private conversation or substitute an old vote.

Participants obtain architecture and project facts directly from repository evidence and cite the paths/revisions and relevant task reports used. Do not read ordinary root-role files, other role contracts or root workflow instructions, including copies in task history or searches. Include independently discovered evidence in the allocated proposal/vote for Preparation to reconcile; do not alter the frozen brief/candidate/issues. Participants perform external research using available native tools under the same access policy; Preparation coordinates distribution of their cited findings in subsequent common packets, not the substantive research itself. Evidence that changes requirements or the common brief requires a new brief and full proposals; a candidate-only revision requires the next complete voting cycle. Record actual spawn identities, chosen models, supplied inputs, allocated outputs, cited evidence, and available launch/completion evidence. Report host restrictions and visibility limits honestly; this agents-only protocol does not require a custom access-tracing system or claim filesystem sandboxing. If the host cannot provide independent task contexts, or forbidden current-wave peer-input exposure is observed, return `PROCESS_FAILURE`.

All workers write only their own allocated artifacts and do not commit, stage, switch branches, spawn other agents, or edit source. Preparation waits for all outputs, then persists only its allocated council evidence and final report together, without staging unrelated files. A process breach may abort the wave with an explicit failure record; ordinary disagreement does not justify ignoring unfinished voters.

## Council artifacts

Council filenames are prefixed by the attempt-local `NNN`. Within each brief, voting-cycle labels are `i01`, `i02`, and so on. A new candidate consumes one cycle, even if rejected. The brief's unique path identifies the attempt; never count votes from another brief.

| Suffix | Writer | Required contents |
|---|---|---|
| `council-brief` | Preparation | Frozen requirements/evidence/shared instructions, complete discovered roster IDs/paths, maximum cycles, model policy and prior-attempt link if any; role-contract snapshots stay coordinator-only |
| `council-launch` | Preparation | Phase/cycle, role-file-to-subagent mapping, actual model, shared packet paths, independent context setup and unique output allocations |
| `council-proposal-<role>` | Assigned role | Brief and role identity, evidence, recommendations, alternatives, concrete impacts, tests, assumptions, confidence and blockers |
| `council-i<cycle>-candidate` | Preparation | Complete proposed Preparation implementation report; source attribution, unresolved alternatives, planned changes, test specifications and README delta |
| `council-i<cycle>-issues` | Preparation | Candidate/brief paths; every comment and objection with stable ID, originator, source, proposed response and status |
| `council-i<cycle>-vote-<role>` | Assigned role | Role/file/subagent identity, model, exact brief/candidate/issues paths, vote, evidence, comments, originating-issue dispositions and explicit final acceptance or objection |
| `council-i<cycle>-outcome` | Preparation | Complete expected/received roster comparison, actual invocation evidence, all votes, comment dispositions with citations, current outcome and next action |
| `preparation` | Preparation | Self-contained final accepted report, or blocked terminal report; complete requirement/comment dispositions and evidence references |

All artifacts begin with the Model and Changed files headers defined in [COMMON](COMMON.md) and identify task, writer, assigned invocation, input paths, cycle where relevant, and any superseded output. Distinct immutable paths and retained contents identify the exact packet; no self-referential digest, hash chain or schema parser is required. All participants must receive the same unchanged candidate and issues contents. Any in-place change is a process failure, not a new version. An agent copies relevant source excerpts when a link alone would not preserve the evidence.

Each comment/issue keeps its ID, origin role, original text, violated requirement/invariant if applicable, concrete failure, evidence, proposed mitigation or operator question, and disposition evidence. Status is OPEN, RESOLUTION_PROPOSED, RESOLVED, WITHDRAWN_BY_ORIGINATOR, or NONBLOCKING_NOTE. Keep rejected alternatives and nonblocking notes; deduplication retains every originator. Preparation records statuses from the roles' cited dispositions and does not dismiss technical objections itself.

## Voting cycle and convergence

1. After the complete independent proposal wave, Preparation assembles the candidate report and issues snapshot from participant-authored sections, with the council Architect supplying the complete architecture/research/implementation/test/README contract. Conflicting proposals and missing substantive sections stay explicit for participants to resolve; the coordinator must not invent a technical resolution or fill those sections itself. Any proposed synthesis is attributed and remains provisional until all voters accept it.
2. Freeze that cycle's candidate and issues. Launch the whole discovered roster as independent parallel voting subagents on that identical packet, with completed prior-cycle evidence supplied equally.
3. Each role reviews the **whole** candidate and returns `AGREE`, `AGREE_WITH_NOTES`, or `DISAGREE`. An accepting vote is explicit final consent to implement this exact candidate without further changes. `AGREE_WITH_NOTES` is unconditional acceptance with optional notes. A requested prerequisite change is `DISAGREE`, not conditional acceptance.
4. Gather all votes before deriving the outcome. Missing, malformed, duplicate, impersonated or stale responses are `PROCESS_FAILURE`, never silent agreement. A vote from another role or brief/candidate cannot substitute. Inspect the complete set even if an early vote objects.
5. Record every comment. Every `DISAGREE` must identify the violated requirement/invariant, exact problem and failure mode, supporting evidence, and feasible correction or precise operator question. Unsupported preference is nonblocking; a `DISAGREE` missing any required grounds is invalid and causes `PROCESS_FAILURE` after inspecting the complete wave. Never convert it to agreement or let it operate as a veto. Closing a valid blocking disagreement requires its originator's explicit evidence-backed acceptance of the candidate's mitigation or withdrawal; merged issues require all originators. Each fresh voter receives its role's prior issues and must disposition them. Preparation cannot manufacture assent.
6. If any valid objection, unresolved alternative or conditional acceptance remains, revise the candidate with attributed mitigations and carry all comments forward. Allocate a new candidate/issues pair and repeat **all roles in parallel** with fresh agents; never re-poll only objectors. Every changed implementation or test instruction invalidates old acceptance.
7. If every role unconditionally accepts the same candidate and all blocking comments have originator-backed closure, record `CONSENSUS`. This records acceptance of the design, not proof of implementation or test execution.
8. Publish the root-allocated final `<NNN>-preparation.md` **after** all council artifacts, making it the stage's normal latest report. Copy the candidate's complete implementation, test, research, risk and README sections unchanged. Add only administrative outcome/evidence references. Any new design choice or editorial change to implementation instructions requires a new candidate and a full parallel vote before publication.

Only Preparation collates outcomes; it has no tie-breaking or additional vote. Model identity or vote count cannot overrule disagreements. The published report contains complete implementation instructions without relying on votes, ledgers or a separate reference seal. Retain council internals as supporting evidence.

## Terminal states and re-entry

- `CONSENSUS`: complete accepting votes from the entire discovered roster for one unchanged candidate, no blocking issues; publish a successful self-contained Preparation report.
- `NEEDS_INFORMATION`: a precise operator answer or missing evidence is necessary. Return the question through root; do not spend remaining cycles guessing.
- `IRRECONCILABLE`: valid incompatible objections remain at the configured cycle limit. Include the alternatives, evidence, attempted mitigations and required decision.
- `PROCESS_FAILURE`: missing/invalid records, capability, independent parallel execution, input isolation, role coverage or immutable packet identity. Include the repair needed; never claim consensus.

A non-consensus terminal report includes the last candidate/cycle, complete roster, all available votes/comments, missing responses, reasons and `Result: BLOCKED`. Exhaustion never forces a result or raises the limit silently. Changed requirements/evidence, a changed role roster or a repaired process need a new brief and fresh full-roster proposals. Published outcomes stay immutable. Report unresolved operator questions to root and stop; a new invocation uses confirmed answers and permitted completed evidence without changing an accepted report.

## Completion evidence

Preparation checks the complete attempt using [CHECKS](CHECKS.md) and includes the observed result, requirement coverage, every comment disposition and supporting paths in its self-contained final report. Carry forward relevant permitted completed evidence explicitly. Stop after returning the allocated result; do not select or infer a subsequent workflow.

Historical `agents/task/**` reports remain unchanged and are judged against their recorded protocol. Do not relabel old results as v2 votes or fabricate retrospective council evidence. Root's task-cycle budget remains in force.
