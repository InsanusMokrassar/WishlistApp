# Root-only instruction routing

This catalog is read by root only, never passed to workers. Root supplies only the selected role's instructions and applicable shared sections, not this catalog or its conversation. Check local overrides for the same input boundaries. All `local.*` files can be missed, but you MUST check their availability if you need them. If a `local.*` file conflicts with its base file, the `local.*` file wins, except it cannot broaden the invocation's input boundary.

## Explicit root startup

Start the main session with `agents/ROOT_PROMPT.md` as the initial prompt. The prompt labels the invocation as root before loading this catalog. `AGENTS.md` contains shared guidance and must not route an unassigned invocation into this catalog.

From the repository root, start a new interactive session with:

```sh
codex "$(cat agents/ROOT_PROMPT.md)"
```

For a fresh non-interactive main session, supply the same prompt on standard input:

```sh
codex exec - < agents/ROOT_PROMPT.md
```

Add the operator's task to the startup prompt when launching a non-interactive task. Do not use resume, fork or a previous conversation for a startup-isolation check. After changing automatically loaded instructions, start a fresh main session before creating workers and inspect a fresh worker's initial context before assigning substantive work.

## Execution classes

Root must label each invocation as root, an ordinary stage role or a council participant before loading role instructions. Root supplies each worker only its own instruction bundle, permitted repository evidence and completed input artifacts, not this routing catalog or its private conversation. The automatically loaded shared `AGENTS.md` is permitted for every invocation.

- **Root**: use this catalog to select the instruction bundle. Only root reads the workflow order and transition rules in `agents/ORCHESTRATOR.md`.
- **Ordinary stage roles**: receive `agents/ALL.md`, applicable shared instructions and only their assigned role instructions. Root supplies the shared communication policy from `AGENTS.md` without this routing catalog. Do not read other role instructions or root workflow files.
- **Council participants**: follow `agents/council/COMMON.md`, `agents/council/PROTOCOL.md`, `agents/council/MODELS.md`, their assigned role file, and the frozen task packet, together with the automatically loaded shared guidance. Participants may inspect repository evidence and permitted completed task history, but not other participants' current-wave outputs or ordinary role/root workflow instructions, including copies in searches/history. Do not supply this catalog as participant instructions.

Preparation is the coordinator defined in `agents/PREPARATION.md`. Root grants it the explicit exception to launch all council roles as fresh parallel subagents and persist allocated council evidence with its final report after all workers stop. Root alone manages stage order, transitions and handoffs. Workers receive no future-stage identities, consumers or requirements; workers must not discover those through repository searches or history. Root filters instruction files and historical reports accordingly, providing relevant completed evidence excerpts where necessary without altering the original records.

## Instruction bundle selection

Use `MODELS.md` file to get abbreviations `LL`/`ML` / `HL` meanings.

Before any work, read in this order (priorities placed from the best for role to the worst, always must be used the better available model according to priority; choice of model must be argued in step file in the beginning):

1. `agents/ALL.md` + (`agents/local.ALL.md` (if exists))
2. Task-specific:
    * root (if there is no direct role specified) → `agents/ORCHESTRATOR.md` + (`agents/local.ORCHESTRATOR.md` (if exists)) (priorities of agents: ML / HL). **ROOT IS THE MAIN SESSION, NEVER A SUBAGENT — full root rule: `agents/ORCHESTRATOR.md`.**
    * issue-executor (root delivery mode, not a worker stage) → `agents/ISSUES_EXECUTION.md` (`agents/local.ISSUES_EXECUTION.md` (if exists)) (priorities of agents: ML / HL)
    * preparation → `agents/PREPARATION.md` + (`agents/local.PREPARATION.md` (if exists)) (priorities of agents: HL / ML)
    * coding → `agents/CODING.md` + (`agents/local.CODING.md` (if exists)) + ONLY the pattern file(s) selected per the `Pattern Library` section of `agents/CODING.md` (priorities of agents: ML / HL)
    * verification → `agents/VERIFICATION.md` + (`agents/local.VERIFICATION.md` (if exists)) (priorities of agents: ML / HL)
    * validator → `agents/VALIDATOR.md` + (`agents/local.VALIDATOR.md` (if exists)) (priorities of agents: HL / ML)
3. The feature's own `README.md` (especially `## Operator Notes`) before touching its code (rule: `agents/ALL.md`)
4. LL may fill purely mechanical documentation when root explicitly allocates it. Preparation and council reasoning, votes and substantive reports retain HL / ML priority; this does not authorize workers to delegate.
