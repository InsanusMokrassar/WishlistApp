Identify council mode BEFORE any automatic latest-report read. Council invocations read only their explicit manifest, including frozen copies of applicable instructions. Inspect applicable `local.*` overrides before freezing and distribute identically. Overrides may refine repository policy but cannot weaken immutable artifacts, isolation or unanimous consent; reject conflicting overrides before dispatch. Outside council mode, applicable `local.*` files override base files.

Use `MODELS.md` file to get abbreviations `LL`/`ML` / `HL` meanings.

Before any work, read in this order (priorities placed from the best for role to the worst, always must be used the better available model according to priority; choice of model must be argued in step file in the beginning):

1. `agents/ALL.md` + (`agents/local.ALL.md` (if exists))
2. Task-specific:
    * root (if there is no direct role specified) → `agents/ORCHESTRATOR.md` + (`agents/local.ORCHESTRATOR.md` (if exists)) (priorities of agents: ML / HL). **ROOT IS THE MAIN SESSION, NEVER A SUBAGENT — full root rule: `agents/ORCHESTRATOR.md`.**
    * issue-executor → `agents/ISSUES_EXECUTION.md` (`agents/local.ISSUES_EXECUTION.md` (if exists)) (priorities of agents: ML / HL)
    * planning → `agents/PLAN.md` + (`agents/local.PLAN.md` (if exists)) (priorities of agents: HL / ML)
    * coding → `agents/CODING.md` + (`agents/local.CODING.md` (if exists)) + ONLY the pattern file(s) selected per the `Pattern Library` section of `agents/CODING.md` (priorities of agents: ML / HL)
    * architecture / architecturing (umbrella stage for new work) → dispatch the council through `agents/COUNCIL_ADAPTER.md`; never invoke a standalone Architecture worker as the new Coding gate.
    * council architect / programmer / security / designer → `agents/council/COMMON.md` + `agents/PROTOCOL.md` + the matching `agents/council/ARCHITECT.md`, `PROGRAMMER.md`, `SECURITY.md`, or `DESIGNER.md` (priorities: HL / explicitly adequate ML).
    * council facilitator / sealer → `agents/PROTOCOL.md` + the matching `agents/council/FACILITATOR.md` or `SEALER.md` (priorities: ML / HL).
    * `agents/ARCHITECTURE.md` and applicable local architecture guidance remain project evidence, included identically through the brief, not a standalone role contract. Historical `architecturing` reports remain readable but cannot substitute for a new council seal.
    * verification → `agents/VERIFICATION.md` + (`agents/local.VERIFICATION.md` (if exists)) (priorities of agents: ML / HL)
    * validator → `agents/VALIDATOR.md` + (`agents/local.VALIDATOR.md` (if exists)) (priorities of agents: HL / ML)
3. The feature's own `README.md` (especially `## Operator Notes`) before touching its code (rule: `agents/ALL.md`)
4. LL handles mechanical documentation filling only. Assigned Planning, architectural, Coding and validation reasoning takes precedence over the generic LL preference; do not delegate technical decisions to an LL formatter.

Already-started legacy cycles may finish their recorded pre-adoption contract, including issue #86. New Architecture entries after adoption and fresh restarted cycles use the council. Every specialist receives the complete portable bundle identically; selected routing identifies its active perspective, not a different evidence allowance.
