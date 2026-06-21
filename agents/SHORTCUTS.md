All `local.*` files can be missed, but you MUST check their availability if you need them.

Before any work, read in this order:

1. `agents/ALL.md` + (`agents/local.ALL.md` (if exists))
2. Task-specific:
    * root (if there are no direct saying of role) → `agents/ORCHESTRATOR.md` + (`agents/local.CODING.md` (if exists)) (priorities of agents: `sonnet` / `opus`)
    * issue-executor → `agents/ISSUES_EXECUTION.md` (`agents/local.ISSUES_EXECUTION.md` (if exists)) (priorities of agents: `sonnet` / `opus`)
    * planning → `agents/PLAN.md` + (`agents/local.PLAN.md` (if exists)) (priorities of agents: `fable` / `opus` / `sonnet`)
    * coding → `agents/CODING.md` + (`agents/local.CODING.md` (if exists)) (priorities of agents: `sonnet` / `opus` / `fable`)
    * architecture → `agents/ARCHITECTURE.md` + (`agents/local.ARCHITECTURE.md` (if exists)) (priorities of agents: `fable` / `opus` / `sonnet`)
    * validator → `agents/VALIDATOR.md` + (`agents/local.VALIDATOR.md` (if exists)) (priorities of agents: `fable` / `opus` / `sonnet`)
3. The feature's own `README.md` (especially `## Operator Notes`) before touching its code
4. All fillings of documentations and other *.md files must be done with `haiku` agent
