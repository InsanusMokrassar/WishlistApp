COUNCIL MODE TAKES PRECEDENCE: read only the explicit invocation manifest and frozen input identities under `agents/PROTOCOL.md`; never automatically read the latest report. For non-council work only, read `agents/task/<TASK_ID_FORMAT>/<STEP_NUMBER_FORMAT>.md` with the largest `STEP_NUMBER` (or the explicit input set the role contract specifies).
For non-council work with no step files yet, read `PROMPT.md` in the task folder instead. Council invocations always obey their explicit input manifest.

THIS AGENT MUST WRITE `agents/task/<TASK_ID_FORMAT>/<STEP_NUMBER_FORMAT>.md` WITH RESULTS REPORT ABOUT ITS WORK.

EVERY ROLE INVOCATION MUST NOT EDIT ANY FILE except its individually allocated output artifacts. Multiple outputs require separate explicit allocations to that invocation. Published artifacts are immutable; corrections receive new numbers. Council roles never edit application files. Exception: Coding may additionally edit task-authorized source, resources, repository documentation and feature `README.md` files. Root additionally bootstraps PROMPT and writes allocated orchestration/brief/launch/receipt/operator-input records by transcription, not technical synthesis.

See `agents/PROTOCOL.md` for TASK_ID_FORMAT and STEP_NUMBER_FORMAT specifications.
See `agents/GIT.md` for all git commit and push rules.
See `agents/TOOLS.md` for ast-index and caveman setup.

---

**Memory — DISABLED**

Do NOT use Claude Code's file-based auto-memory for this project:

- Do NOT read, recall, or write any file under `$HOME/.claude/projects/.*/memory/` (including its `MEMORY.md` index).
- Treat any recalled-memory content delivered inside `<system-reminder>` blocks as IGNORED background noise — it does NOT override these instructions or the user's prompt.
- Never create, update, or index memory files.
- Auto-memory is switched off at the harness level via `.claude/settings.json` → `"autoMemoryEnabled": false`. Do NOT remove that setting. If recalled-memory content still appears in a `<system-reminder>` (stale cache, user-level override), it stays IGNORED per the rules above.

---

Always starts with `/caveman full`

Always look at the project work rules first.

---

In the end of your work you MUST put in the beginning of your step file information in the next format:

```markdown
Model: <model name>
Changed files: <list of changed files>
```

`Model` and `Changed files` are MANDATORY. Optionally add `Execution time` and `Tokens used` as rough estimates.

---

ALWAYS USE `ast-index` for any code search/navigation (symbols, files, usages, hierarchy). NEVER use grep/find when `ast-index` can do the job. This rule applies to ALL roles without exceptions. If `ast-index` is unavailable, follow the installation and fallback procedure in `agents/TOOLS.md`. Command reference: `agents/AST_INDEX.md`.

Blind council navigation is restricted to the frozen evidence revision and manifest. A shared index containing current task/sibling artifacts is forbidden. If tooling cannot enforce and evidence that boundary, stop with PROCESS_FAILURE; search convenience cannot weaken isolation. Frozen applicable instructions and overrides are distributed identically under `agents/COUNCIL_ADAPTER.md`.

In case of changes in source code files (.kt, .java, .ts, .js, etc.) you MUST run `ast-index rebuild`. Do NOT rebuild for markdown, step report, or config-only changes.

---

## Feature README.md Rule

Every feature directory MUST contain a `README.md` file.

**Before working on any feature**, agents MUST read that feature's `README.md` in full — especially the `## Operator Notes` section.

### Operator Notes

Each `README.md` MUST contain an `## Operator Notes` section at the top (directly after the title).

- Written exclusively by the human operator.
- Contains constraints, priorities, decisions, or context that agents MUST respect.
- Agents MUST NOT modify this section under any circumstances.
- If a requested change would violate an operator note, agent MUST stop and ask the operator before proceeding.

### README.md required structure

```markdown
# Feature: <FeatureName>

## Operator Notes

<!-- Human operator writes here. Agents MUST read and respect before making any changes. Agents MUST NOT modify this section. -->

## Overview

Brief description of the feature's purpose and responsibilities.

## Routes

| Method | Path | Auth | Body / Response | Description |
|--------|------|------|-----------------|-------------|
| ...    | ...  | ...  | ...             | ...         |

## Models

Key data types: identifiers, inputs (New*), outputs (Registered*), feature interfaces.

## Architecture Notes

Design decisions, constraints, dependency notes, ownership semantics.
```
