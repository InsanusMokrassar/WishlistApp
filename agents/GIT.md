# Git Rules

`.gitignore` files are prohibited to be touched UNLESS other said directly

## Before Branching

Always sync with master before creating a new branch:
```bash
git checkout master && git pull origin master
```

## Commit Rules

Every stage MUST commit the result of its work, UNLESS the prompt gives other direction. Parallel council workers are the explicit exception: they do not touch the shared Git index or commit; Architecture persists their completed outputs after joining the whole wave.

**What each role commits:**
- **Planning, Verification, Validating**: commit ONLY the report allocated to that invocation.
- **Architecture**: commit its coordination/final reports and completed council outputs allocated to its wave, after all workers have stopped writing. Never stage unrelated files.
- **Council workers**: write only the uniquely assigned proposal/vote; do not stage, commit or switch branches.
- **Coding**: commit BOTH the step report AND all task-authorized source/resource/documentation changes.

Outer stages and Git commits remain sequential. Council proposals and **every voting cycle run all discovered roles in parallel**, with one independent subagent and output allocation per role. Architecture serializes Git persistence only after the wave completes; it must not serialize voting to avoid index races. Published artifacts remain immutable; corrections use new allocations and explicit supersession.

**What no role may stage:** unrelated reports/files, build artifacts, or ignored files. Architecture's explicitly allocated completed council outputs are the only cross-role staging exception.

**Commit message format:**
- Normal prose per `AGENTS.md` "Communication Protocol Precedence".
- One-line summary describing the work result.
- End the commit body with:
  ```
  Co-Authored-By: Claude <noreply@anthropic.com>
  ```

## Push Rules

- Do NOT push during individual role work — pushing stays with the Orchestrator.
- Orchestrator pushes after all stages for a cycle complete successfully.
- Exception: issue-executor explicitly pushes the branch before opening a PR (see `ISSUES_EXECUTION.md`).
