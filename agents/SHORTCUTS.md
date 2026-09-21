# Root-only instruction routing

This catalog is read by root only, never passed to workers. Root supplies only the selected role's instructions and applicable shared sections, not this catalog or its conversation. Check local overrides for the same input boundaries. All `local.*` files can be missed, but you MUST check their availability if you need them. If a `local.*` file conflicts with its base file, the `local.*` file wins, except it cannot broaden the invocation's input boundary.

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
