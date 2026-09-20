# Git Rules

`.gitignore` files are prohibited to be touched UNLESS other said directly

## Before Branching

Always sync with master before creating a new branch:
```bash
git checkout master && git pull origin master
```

## Commit Rules

Every role MUST make a git commit with the result of its work, UNLESS the prompt gives other direction.

**Commit scope:** commit only the allocated report and any additional changed paths expressly authorized by your invocation. Do not infer another role's permissions.

**What no role may stage:** other roles' step reports, unrelated files, build artifacts, files in gitignore.

**Commit message format:**
- Normal prose per the communication policy supplied in the invocation.
- One-line summary describing the work result.
- End the commit body with:
  ```
  Co-Authored-By: Claude <noreply@anthropic.com>
  ```

## Push Rules

- Do NOT push during individual role work — pushing stays with the Orchestrator.
- Delivery authorization is supplied only to root; a role stops after its committed result.
