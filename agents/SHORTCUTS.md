All `local.*` files can be missed, but you MUST check their availability if you need them. If a `local.*` file conflicts with its base file, the `local.*` file wins.

Before any work, read in this order (priorities placed from the best for role to the worst, always must be used the better available model according to priority; choice of model must be argued in step file in the beginning):

1. `agents/ALL.md` + (`agents/local.ALL.md` (if exists))
2. Task-specific:
    * root (if there is no direct role specified) → `agents/ORCHESTRATOR.md` + (`agents/local.ORCHESTRATOR.md` (if exists)) (priorities of agents: `sonnet` / `opus`). **ROOT IS THE MAIN SESSION, NEVER A SUBAGENT — full root rule: `agents/ORCHESTRATOR.md`.**
    * issue-executor → `agents/ISSUES_EXECUTION.md` (`agents/local.ISSUES_EXECUTION.md` (if exists)) (priorities of agents: `sonnet` / `opus`)
    * planning → `agents/PLAN.md` + (`agents/local.PLAN.md` (if exists)) (priorities of agents: `fable` / `opus` / `sonnet`)
    * coding → `agents/CODING.md` + (`agents/local.CODING.md` (if exists)) + ONLY the pattern file(s) selected per the `Pattern Library` section of `agents/CODING.md` (priorities of agents: `sonnet` / `opus` / `fable`)
    * architecture → `agents/ARCHITECTURE.md` + (`agents/local.ARCHITECTURE.md` (if exists)) (priorities of agents: `fable` / `opus` / `sonnet`)
    * verification → `agents/VERIFICATION.md` + (`agents/local.VERIFICATION.md` (if exists)) (priorities of agents: `sonnet` / `opus`)
    * validator → `agents/VALIDATOR.md` + (`agents/local.VALIDATOR.md` (if exists)) (priorities of agents: `fable` / `opus` / `sonnet`)
3. The feature's own `README.md` (especially `## Operator Notes`) before touching its code (rule: `agents/ALL.md`)
4. All fillings of documentations and other *.md files must be done with `haiku` agent
