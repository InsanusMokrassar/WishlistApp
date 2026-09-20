# Git Rules

`.gitignore` files are prohibited to be touched UNLESS other said directly

## Before Branching

Always sync with master before creating a new branch:
```bash
git checkout master && git pull origin master
```

## Commit Rules

Every role MUST make a git commit with the result of its work, UNLESS the prompt gives other direction.

**What each role commits:**
- **Planning, council proposal/review/consent, Facilitator, Sealer, Verification, Validating**: commit ONLY outputs explicitly allocated to that invocation. Legacy Architecturing cycles retain their allocated report under the adoption exception.
- **Coding**: commit BOTH the step report AND all task-authorized source/resource/documentation changes.

Repository invocations and commits remain sequential to prevent index races. A portable parallel adapter must isolate outputs and serialize persistence. A published/committed artifact is immutable; corrections use new allocations and explicit supersession. Git history supplements embedded provenance and retained evidence, never replaces those records.

**What no role may stage:** other roles' step reports, unrelated files, build artifacts, files in gitignore.

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
