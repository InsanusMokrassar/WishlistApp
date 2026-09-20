Use `MODELS.md` for HL / ML / LL meanings. Select the best available adequate model in the role's priority order and record the choice at the start of its report.

Identify a **council worker** before any automatic latest-step read. Such workers use only Architecture's supplied frozen packet, including applicable common/local instructions. All other stages follow their ordinary input contracts. Local overrides may refine repository policy but cannot weaken independent parallel voting, full-directory participation or immutable outputs.

Before work, read:

1. `agents/ALL.md` and applicable `agents/local.ALL.md`.
2. The task-specific route:
   - root (no other role specified) → `agents/ORCHESTRATOR.md` and applicable `agents/local.ORCHESTRATOR.md` (ML / HL).
   - issue-executor → `agents/ISSUES_EXECUTION.md` and applicable local override (ML / HL).
   - planning → `agents/PLAN.md` and applicable `agents/local.PLAN.md` (HL / ML).
   - architecture / architecturing → `agents/ARCHITECTURE.md`, `agents/PROTOCOL.md`, `agents/COUNCIL_ADAPTER.md` and applicable `agents/local.ARCHITECTURE.md` (HL / ML). This stage agent coordinates the council and may spawn its voting subagents.
   - council role → `agents/council/COMMON.md`, `agents/PROTOCOL.md`, `agents/MODELS.md` and the allocated role file discovered under `agents/council/roles/` (HL / ML). There is no hard-coded role routing subset: **every role file** participates in **every parallel voting cycle**. Supply the entire frozen role bundle as common context.
   - coding → `agents/CODING.md`, applicable `agents/local.CODING.md`, and only matching Pattern Library files (ML / HL). Consume the final Architecturing report, not council internals.
   - verification → `agents/VERIFICATION.md` and applicable `agents/local.VERIFICATION.md` (ML / HL). Mechanical project build/tests only.
   - validator → `agents/VALIDATOR.md` and applicable `agents/local.VALIDATOR.md` (HL / ML). Owns requirements and council-comment validation.
3. The feature's `README.md`, especially Operator Notes, before feature work. Council workers receive relevant content in their common packet, not through unscheduled private discovery.
4. LL may fill already-decided mechanical documentation outside council work. Architecture and every council role prefer HL and only fall back to ML; generic documentation preferences do not change that.

`agents/PROJECT_ARCHITECTURE.md` contains repository facts, not a council role. Administrative collation belongs to Architecture, not extra nonvoting role files. Historical reports remain immutable; new Architecturing entries use the current protocol.
