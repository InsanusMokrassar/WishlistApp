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
- **Planning, Architecturing, Verification, Validating**: commit ONLY their step report file (`agents/task/<TASK_ID>/<STEP_NUMBER>.md`).
- **Coding**: commit BOTH the step report file AND all changed source code files.

**What no role may stage:** other roles' step reports, unrelated files, build artifacts, files in gitignore.

**Commit message format:**
- Use normal prose (not caveman-compressed).
- One-line summary describing the work result.
- End the commit body with:
  ```
  Co-Authored-By: Claude <noreply@anthropic.com>
  ```

## Push Rules

- Do NOT push during individual role work — pushing stays with the Orchestrator.
- Orchestrator pushes after all stages for a cycle complete successfully.
- Exception: issue-executor explicitly pushes the branch before opening a PR (see `ISSUES_EXECUTION.md`).
