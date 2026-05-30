All `local.*` files can be missed, but you MUST check their availability if you need them.


Before any work, read in this order:

1. `agents/ALL.md` + (`agents/local.ALL.md` (if exists))
2. Task-specific:
    * coding → `agents/CODING.md` + (`agents/local.CODING.md` (if exists)) (priorities of agents: `opus` / `sonnet`)
    * architecture → `agents/ARCHITECTURE.md` + (`agents/local.ARCHITECTURE.md` (if exists)) (priorities of agents: `opus` / `sonnet`)
3. The feature's own `README.md` (especially `## Operator Notes`) before touching its code
4. All fillings of documentations and other *.md files must be done with `haiku` agent
5. `agents/HISTORY.md` — long-term memory; update it at session end (using `haiku` agent)
