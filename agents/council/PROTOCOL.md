# Council Protocol

Architecture coordinates the council inside Architecturing. Participants apply only [COMMON](COMMON.md), this protocol, [MODELS](MODELS.md), and their assigned role contract.

## Allocated artifacts

Architecture receives the accepted Planning report and an allocated final Architecturing report path from root. Store council evidence in a new `council/<attempt-id>/` subdirectory of that report's task folder, using a unique attempt ID. Do not place council artifacts among the ordinary top-level stage reports or consume their step numbers.

Within an attempt, use monotonically increasing `NNN-<artifact-name>.md` names from 001 through 999. Architecture reserves every wave's distinct output paths before launching agents. Parallel completion order need not match allocation order. Each output has one writer; failed allocations remain gaps with recorded reasons. Once returned as a completed output, an artifact is immutable. Corrections get a new allocation and explicit supersession. Stop at number exhaustion; never wrap or overwrite. Participants identify the active decision inputs from their assigned packet, never a latest-step lookup; repository and task-history reads are permitted under the isolation rules below.

## Agents-only execution boundary

Agents perform discovery, planning, collation, voting, outcome recording and validation using the host's native subagent, file and research tools. No custom script, executable fixture, parser, hash checker, automation service, or code-based evaluator is required to execute the council. Decisions come from recorded agent reasoning and votes, not from a script-produced certificate.

This restriction concerns the council process, not the application's normal build/test commands, repository navigation tools, or Git operations. Verification remains the mechanical project build/test stage. Validation performs the substantive requirements and council-comment audit by reading evidence.

## Roster and capability

The entire `agents/council/roles/` directory is the voting roster. Architecture recursively reads **every Markdown file**, records its path and declared `Role ID`, and loads its complete instructions. Each file defines one role; IDs must be unique lowercase letters/digits/hyphens beginning with a letter. The directory must be nonempty. Non-role instructions or an invalid/duplicate ID are a process error, never a reason to silently skip a file. Keep README, shared instructions and checklists outside this directory.

The shipped roles are examples of the initial roster, not an allow-list. Adding a valid role file automatically adds a voter without changes to routing or a script. There are no optional, nonvoting or not-applicable roles in this directory: a role with no domain-specific concern must still review the complete plan and cast an explicit vote. Architecture is the nonvoting coordinator outside the directory.

Architecture re-enumerates the directory at the start of **every** voting cycle and compares paths and instruction content with the brief. If any role is added, removed or changed, invalidate the active attempt, preserve its evidence, and start a new brief with the whole current roster and fresh proposals. Recheck before final publication as well; a changed roster can never inherit previous votes. Changes do not extend the outer task budget.

Every council role, in every mode and cycle, prefers **HL**, with **ML** only as the lower-priority fallback. Record the actual model and why ML was necessary. Never select LL for council planning or voting, and never omit a role because HL is unavailable. Missing adequate HL/ML capability is `PROCESS_FAILURE`.

## Common inputs and independent parallel waves

A brief records accepted Planning and source-prompt paths; requirements/acceptance IDs; confirmed answers, constraints, exclusions and unknowns; repository revision, evidence paths and retained research; the full roster and frozen instruction contents; model policy; and a positive finite maximum voting-cycle count. The default is three cycles. Freeze the brief before proposals; the common packet and evidence-access rules are identical for every participant apart from active role and assigned output. Participants may independently inspect repository source/configuration/documentation and permitted task history beyond the named evidence paths; no separate project-explaining file is required.

Architecture launches one **fresh independent subagent per role in parallel**, both for initial proposals and for **every voting cycle**. Launch the entire roster before waiting for results. Do not batch the roster sequentially, simulate multiple roles in one agent, reuse a previous-cycle agent's private conversation, or stop launching because an early result disagrees. Reserve enough concurrent subagent capacity for the whole wave. If the host cannot provide it, stop with `PROCESS_FAILURE` and ask root to resolve capacity; there is no sequential fallback.

Each agent receives the explicit frozen packet in a clean task context, not a fork of Architecture's conversation, plus its private output assignment and equal read access to the repository and task history. Use the host's native input/tool restrictions where available. Participants must not read other participants' proposal, review or vote artifacts from the current proposal wave or voting cycle, including copies in task history, Git history, search results or messages. Exclude those artifacts from searches before reading results. Private sibling messages and persistent agent memory are also forbidden. Ordinary task reports and completed prior-cycle artifacts may be read as evidence; permitted history access is not permission to inherit a prior agent's private conversation or substitute an old vote.

Participants obtain architecture and project facts directly from repository evidence and cite the paths/revisions and relevant task reports used. Read ordinary root-role files only as evidence, not as participant instructions. Include independently discovered evidence in the allocated proposal/vote for Architecture to reconcile; do not alter the frozen brief/candidate/issues. New external research remains coordinated through Architecture. Evidence that changes requirements or the common brief requires a new brief and full proposals; a candidate-only revision requires the next complete voting cycle. Record actual spawn identities, chosen models, supplied inputs, allocated outputs, cited evidence, and available launch/completion evidence. Report host restrictions and visibility limits honestly; this agents-only protocol does not require a custom access-tracing system or claim filesystem sandboxing. If the host cannot provide independent task contexts, or forbidden current-wave peer-input exposure is observed, return `PROCESS_FAILURE`.

All workers write only their own allocated artifacts and do not commit, stage, switch branches, spawn other agents, or edit source. Architecture waits for all outputs, then persists only its allocated council evidence and final report together, without staging unrelated files. A process breach may abort the wave with an explicit failure record; ordinary disagreement does not justify ignoring unfinished voters.

## Council artifacts

Council filenames are prefixed by the attempt-local `NNN`. Within each brief, voting-cycle labels are `i01`, `i02`, and so on. A new candidate consumes one cycle, even if rejected. The brief's unique path identifies the attempt; never count votes from another brief.

| Suffix | Writer | Required contents |
|---|---|---|
| `council-brief` | Architecture | Frozen requirements/evidence/instructions, complete discovered roster, maximum cycles, model policy and prior-attempt link if any |
| `council-launch` | Architecture | Phase/cycle, role-file-to-subagent mapping, actual model, shared packet paths, independent context setup and unique output allocations |
| `council-proposal-<role>` | Assigned role | Brief and role identity, evidence, recommendations, alternatives, concrete impacts, tests, assumptions, confidence and blockers |
| `council-i<cycle>-candidate` | Architecture | Complete proposed Architecturing implementation report; source attribution, unresolved alternatives, planned changes, test specifications and README delta |
| `council-i<cycle>-issues` | Architecture | Candidate/brief paths; every comment and objection with stable ID, originator, source, proposed response and status |
| `council-i<cycle>-vote-<role>` | Assigned role | Role/file/subagent identity, model, exact brief/candidate/issues paths, vote, evidence, comments, originating-issue dispositions and explicit final acceptance or objection |
| `council-i<cycle>-outcome` | Architecture | Complete expected/received roster comparison, actual invocation evidence, all votes, comment dispositions with citations, current outcome and next action |
| `architecturing` | Architecture | Self-contained final accepted report, or blocked terminal report; council evidence references for Validation |

All artifacts begin with the Model and Changed files headers defined in [COMMON](COMMON.md) and identify task, writer, assigned invocation, input paths, cycle where relevant, and any superseded output. Distinct immutable paths and retained contents identify the exact packet; no self-referential digest, hash chain or schema parser is required. All participants must receive the same unchanged candidate and issues contents. Any in-place change is a process failure, not a new version. An agent copies relevant source excerpts when a link alone would not preserve the evidence.

Each comment/issue keeps its ID, origin role, original text, violated requirement/invariant if applicable, concrete failure, evidence, proposed mitigation or operator question, and disposition evidence. Status is OPEN, RESOLUTION_PROPOSED, RESOLVED, WITHDRAWN_BY_ORIGINATOR, or NONBLOCKING_NOTE. Keep rejected alternatives and nonblocking notes; deduplication retains every originator. Architecture records statuses from the roles' cited dispositions and does not dismiss technical objections itself.

## Voting cycle and convergence

1. After the complete independent proposal wave, Architecture assembles the candidate report and issues snapshot. Conflicting proposals stay explicit. Any proposed synthesis is attributed and remains provisional until all voters accept it.
2. Freeze that cycle's candidate and issues. Launch the whole discovered roster as independent parallel voting subagents on that identical packet, with completed prior-cycle evidence supplied equally.
3. Each role reviews the **whole** candidate and returns `AGREE`, `AGREE_WITH_NOTES`, or `OBJECT`. An accepting vote is explicit final consent to implement this exact candidate without further changes. `AGREE_WITH_NOTES` is unconditional acceptance with optional notes. A requested prerequisite change is `OBJECT`, not conditional acceptance.
4. Gather all votes before deriving the outcome. Missing, malformed, duplicate, impersonated or stale responses are `PROCESS_FAILURE`, never silent agreement. A vote from another role or brief/candidate cannot substitute. Inspect the complete set even if an early vote objects.
5. Record every comment. Closing a blocking objection requires its originator's explicit evidence-backed acceptance of the candidate's mitigation or withdrawal; merged objections require all originators. Each fresh voter receives its role's prior issues and must disposition them. Unsupported taste is not a technical veto, but Architecture cannot manufacture assent: ask for a properly grounded vote or return a process failure if the response remains invalid.
6. If any valid objection, unresolved alternative or conditional acceptance remains, revise the candidate with attributed mitigations and carry all comments forward. Allocate a new candidate/issues pair and repeat **all roles in parallel** with fresh agents; never re-poll only objectors. Every changed implementation or test instruction invalidates old acceptance.
7. If every role unconditionally accepts the same candidate and all blocking comments have originator-backed closure, record `CONSENSUS`. This is the council's planning outcome, not a substitute for the later independent requirements/implementation Validation.
8. Publish the root-allocated final `<NNN>-architecturing.md` **after** all council artifacts, making it the stage's normal latest report. Copy the candidate's complete implementation, test, research, risk and README sections unchanged. Add only administrative outcome/evidence references. Any new design choice or editorial change to implementation instructions requires a new candidate and a full parallel vote before publication.

Only Architecture collates outcomes; it has no tie-breaking or additional vote. Model identity or vote count cannot overrule objections. The published report must not require Coding to read votes, ledgers or a separate reference seal to discover implementation instructions. Council internals are audit evidence for Validation.

## Terminal states and re-entry

- `CONSENSUS`: complete accepting votes from the entire discovered roster for one unchanged candidate, no blocking issues; final Architecturing report may go to Coding.
- `NEEDS_INFORMATION`: a precise operator answer or missing evidence is necessary. Return the question through root; do not spend remaining cycles guessing.
- `IRRECONCILABLE`: valid incompatible objections remain at the configured cycle limit. Include the alternatives, evidence, attempted mitigations and required decision.
- `PROCESS_FAILURE`: missing/invalid records, capability, independent parallel execution, input isolation, role coverage or immutable packet identity. Include the repair needed; never claim consensus.

A non-consensus terminal report includes the last candidate/cycle, complete roster, all available votes/comments, missing responses, reasons and `Coding allowed: no`. Exhaustion never forces a result or raises the limit silently. Changed requirements/evidence return through Planning; a changed role roster or repaired process needs a new brief and fresh full-roster proposals. Published outcomes stay immutable. Coding design concerns stop implementation and return through root to Architecture for a new attempt, not a silent change to the accepted plan.

## Completion evidence

Architecture checks the complete attempt using [CHECKS](CHECKS.md) and includes the observed result, requirement coverage, every comment disposition and supporting paths in its self-contained final report. Carry forward relevant evidence from prior attempts explicitly. Council participants do not perform the later Validation stage or change its reading rules. Verification remains the mechanical build/test stage.

Historical `agents/task/**` reports remain unchanged and are judged against their recorded protocol. Do not relabel old results as v2 votes or fabricate retrospective council evidence. Root's task-cycle budget remains in force.
