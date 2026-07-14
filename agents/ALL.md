THIS AGENT MUST READ `agents/task/<TASK_ID_FORMAT>/<STEP_NUMBER_FORMAT>.md` with the largest `STEP_NUMBER` BEFORE ANY WORK (or the full step range the agent's role file specifies — e.g. the Validator reads all steps since the last validation).
If no step files exist yet, read `PROMPT.md` in the task folder instead.

THIS AGENT MUST WRITE `agents/task/<TASK_ID_FORMAT>/<STEP_NUMBER_FORMAT>.md` WITH RESULTS REPORT ABOUT ITS WORK.

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

`Model` and `Changed files` are MANDATORY.

---

ALWAYS USE `ast-index` for any code search/navigation (symbols, files, usages, hierarchy). NEVER use grep/find when `ast-index` can do the job. Read about how to use it in `agents/AST_INDEX.md`. See `agents/TOOLS.md` for installation instructions. This rule applies to ALL roles without exceptions.

In case of changes in source code files (.kt, .java, .ts, .js, etc.) you MUST run `ast-index rebuild`. Do NOT rebuild for markdown or step report changes.

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
