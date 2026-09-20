# Step File and Expert Council Protocol

Version: `council-v2`. The five stages remain Planning → Architecturing → Coding → Verification → Validating. [Architecture](ARCHITECTURE.md) coordinates an agents-only council within Architecturing; the council does not replace the other stages.

## Task folder and immutable steps

`TASK_ID_FORMAT` is `dd.MM.yyyy_HH.mm.ss-<uuid4>`, using UTC and a UUID v4, for example `07.07.2026_14.30.12-f47ac10b-58cc-4372-a567-0e02b2c3d479`. No slashes or spaces. Root creates `agents/task/<TASK_ID>/` and writes the source prompt once to `PROMPT.md`. Later answers receive new operator-input reports; never overwrite the prompt.

`STEP_NUMBER_FORMAT` is `<NNN>-<artifact-name>`; files end in `.md`. Numbers are task-global, strictly increasing allocations from 001 through 999 and never reset or get reused. Workflow names remain `planning`, `architecturing`, `coding`, `verification`, `validating`, `orchestrator`, and `operator-input`. For ordinary latest-step reads, sort numbered files excluding PROMPT and take the largest allocation. Council workers instead read only their assigned packet.

Root delegates exclusive number allocation to Architecture for the duration of the council stage; root and other stages must not allocate concurrently. Architecture reserves every wave's distinct output names before launching agents. Parallel completion order need not match allocation order. Each output has one writer; failed allocations remain gaps with recorded reasons. Once returned as a completed output, an artifact is immutable. Corrections get a new number and explicit supersession. Stop at number exhaustion and ask root for a linked new task; never wrap or overwrite.

## Agents-only execution boundary

Agents perform discovery, planning, collation, voting, outcome recording and validation using the host's native subagent, file and research tools. No custom script, executable fixture, parser, hash checker, automation service, or code-based evaluator is required to execute the council. Decisions come from recorded agent reasoning and votes, not from a script-produced certificate.

This restriction concerns the council process, not the application's normal build/test commands, repository navigation tools, or Git operations. [Verification](VERIFICATION.md) remains the mechanical project build/test stage. [Validation](VALIDATOR.md) performs the substantive requirements and council-comment audit by reading evidence.

## Roster and capability

The entire `agents/council/roles/` directory is the voting roster. Architecture recursively reads **every Markdown file**, records its path and declared `Role ID`, and loads its complete instructions. Each file defines one role; IDs must be unique lowercase letters/digits/hyphens beginning with a letter. The directory must be nonempty. Non-role instructions or an invalid/duplicate ID are a process error, never a reason to silently skip a file. Keep README, shared instructions and checklists outside this directory.

The shipped roles are examples of the initial roster, not an allow-list. Adding a valid role file automatically adds a voter without changes to routing or a script. There are no optional, nonvoting or not-applicable roles in this directory: a role with no domain-specific concern must still review the complete plan and cast an explicit vote. Architecture is the nonvoting coordinator outside the directory.

Architecture re-enumerates the directory at the start of **every** voting cycle and compares paths and instruction content with the brief. If any role is added, removed or changed, invalidate the active attempt, preserve its evidence, and start a new brief with the whole current roster and fresh proposals. Recheck before final publication as well; a changed roster can never inherit previous votes. Changes do not extend the outer task budget.

Every council role, in every mode and cycle, prefers **HL**, with **ML** only as the lower-priority fallback. Record the actual model and why ML was necessary. Never select LL for council planning or voting, and never omit a role because HL is unavailable. Missing adequate HL/ML capability is `PROCESS_FAILURE`.

## Common inputs and independent parallel waves

A brief records accepted Planning and source-prompt paths; requirements/acceptance IDs; confirmed answers, constraints, exclusions and unknowns; retained repository/research evidence; the full roster and frozen instruction contents; model policy; and a positive finite maximum voting-cycle count. The default is three cycles. Freeze the brief before proposals; role definitions and evidence are identical for every participant apart from active role and assigned output.

Architecture launches one **fresh independent subagent per role in parallel**, both for initial proposals and for **every voting cycle**. Launch the entire roster before waiting for results. Do not batch the roster sequentially, simulate multiple roles in one agent, reuse a previous-cycle agent's private conversation, or stop launching because an early result disagrees. Reserve enough concurrent subagent capacity for the whole wave. If the host cannot provide it, stop with `PROCESS_FAILURE` and ask root to resolve capacity; there is no sequential fallback.

Each agent receives the explicit frozen packet in a clean task context, not a fork of Architecture's conversation. Give it only the packet plus its private output assignment; use the host's native input/tool restrictions where available. Current-wave sibling outputs, messages, memory and task-history searches are not allowed. No private live research during a wave: missing evidence is requested through the result so Architecture can distribute it equally in a new brief. Prior completed waves may be disclosed identically in later cycles. Record actual spawn identities, chosen models, supplied inputs, allocated outputs, and available launch/completion evidence. Report host restrictions and visibility limits honestly; this agents-only protocol does not require a custom access-tracing system or claim filesystem sandboxing. If the host cannot provide independent task contexts, or peer-input exposure is observed, return `PROCESS_FAILURE`.

All workers write only their own allocated artifacts and do not commit, stage, switch branches, spawn other agents, or edit source. Architecture waits for all outputs, then serializes persistence under [GIT](GIT.md). A process breach may abort the wave with an explicit failure record; ordinary disagreement does not justify ignoring unfinished voters.

## Council artifacts

Council filenames are prefixed by the same task-global `NNN`. Within each brief, voting-cycle labels are `i01`, `i02`, and so on. A new candidate consumes one cycle, even if rejected. The brief's unique path identifies the attempt; never count votes from another brief.

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

All artifacts begin with the usual Model and Changed files headers and identify task, writer, assigned invocation, input paths, cycle where relevant, and any superseded output. Distinct immutable paths and retained contents identify the exact packet; no self-referential digest, hash chain or schema parser is required. All participants must receive the same unchanged candidate and issues contents. Any in-place change is a process failure, not a new version. An agent copies relevant source excerpts when a link alone would not preserve the evidence.

Each comment/issue keeps its ID, origin role, original text, violated requirement/invariant if applicable, concrete failure, evidence, proposed mitigation or operator question, and disposition evidence. Status is OPEN, RESOLUTION_PROPOSED, RESOLVED, WITHDRAWN_BY_ORIGINATOR, or NONBLOCKING_NOTE. Keep rejected alternatives and nonblocking notes; deduplication retains every originator. Architecture records statuses from the roles' cited dispositions and does not dismiss technical objections itself.

## Voting cycle and convergence

1. After the complete independent proposal wave, Architecture assembles the candidate report and issues snapshot. Conflicting proposals stay explicit. Any proposed synthesis is attributed and remains provisional until all voters accept it.
2. Freeze that cycle's candidate and issues. Launch the whole discovered roster as independent parallel voting subagents on that identical packet, with completed prior-cycle evidence supplied equally.
3. Each role reviews the **whole** candidate and returns `AGREE`, `AGREE_WITH_NOTES`, or `OBJECT`. An accepting vote is explicit final consent to implement this exact candidate without further changes. `AGREE_WITH_NOTES` is unconditional acceptance with optional notes. A requested prerequisite change is `OBJECT`, not conditional acceptance.
4. Gather all votes before deriving the outcome. Missing, malformed, duplicate, impersonated or stale responses are `PROCESS_FAILURE`, never silent agreement. A vote from another role or brief/candidate cannot substitute. Inspect the complete set even if an early vote objects.
5. Record every comment. Closing a blocking objection requires its originator's explicit evidence-backed acceptance of the candidate's mitigation or withdrawal; merged objections require all originators. Each fresh voter receives its role's prior issues and must disposition them. Unsupported taste is not a technical veto, but Architecture cannot manufacture assent: ask for a properly grounded vote or return a process failure if the response remains invalid.
6. If any valid objection, unresolved alternative or conditional acceptance remains, revise the candidate with attributed mitigations and carry all comments forward. Allocate a new candidate/issues pair and repeat **all roles in parallel** with fresh agents; never re-poll only objectors. Every changed implementation or test instruction invalidates old acceptance.
7. If every role unconditionally accepts the same candidate and all blocking comments have originator-backed closure, record `CONSENSUS`. This is the council's planning outcome, not a substitute for the later independent requirements/implementation Validation.
8. Publish the final `<NNN>-architecturing.md` **after** all council artifacts, making it the stage's normal latest report. Copy the candidate's complete implementation, test, research, risk and README sections unchanged. Add only administrative outcome/evidence references. Any new design choice or editorial change to implementation instructions requires a new candidate and a full parallel vote before publication.

Only Architecture collates outcomes; it has no tie-breaking or additional vote. Model identity or vote count cannot overrule objections. The published report must not require Coding to read votes, ledgers or a separate reference seal to discover implementation instructions. Council internals are audit evidence for Validation.

## Terminal states and re-entry

- `CONSENSUS`: complete accepting votes from the entire discovered roster for one unchanged candidate, no blocking issues; final Architecturing report may go to Coding.
- `NEEDS_INFORMATION`: a precise operator answer or missing evidence is necessary. Return the question through root; do not spend remaining cycles guessing.
- `IRRECONCILABLE`: valid incompatible objections remain at the configured cycle limit. Include the alternatives, evidence, attempted mitigations and required decision.
- `PROCESS_FAILURE`: missing/invalid records, capability, independent parallel execution, input isolation, role coverage or immutable packet identity. Include the repair needed; never claim consensus.

A non-consensus terminal report includes the last candidate/cycle, complete roster, all available votes/comments, missing responses, reasons and `Coding allowed: no`. Exhaustion never forces a result or raises the limit silently. Changed requirements/evidence return through Planning; a changed role roster or repaired process needs a new brief and fresh full-roster proposals. Published outcomes stay immutable. Coding design concerns stop implementation and return through root to Architecture for a new attempt, not a silent change to the accepted plan.

## Validation and historical records

[Validation](VALIDATOR.md) owns the formal audit of requirements, all council comments/objections, the final implementation and the evidence for independent parallel voting. It reads the complete active chain, including references predating a previous validation and rejected/superseded artifacts, and uses the agent-readable [CHECKS](council/CHECKS.md). It does not delegate this judgment to Verification or an executable fixture.

Historical `agents/task/**` reports remain unchanged and are judged against their recorded protocol. Do not relabel old results as v2 votes or fabricate retrospective council evidence. New Architecturing entries use this protocol. Root's separate maximum of ten full workflow cycles remains in force.
