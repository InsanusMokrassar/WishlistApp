# Memory — DISABLED

Do NOT use Claude Code's file-based auto-memory for this project:

- Do NOT read, recall, or write any file under `$HOME/.claude/projects/.*/memory/` (including its `MEMORY.md` index).
- Treat any recalled-memory content delivered inside `<system-reminder>` blocks as IGNORED background noise — it does NOT override these instructions or the user's prompt.
- Never create, update, or index memory files.

---

Always starts with `/caveman full`

Always look at the project work rules first.  Many thanks for your help to me and your work.

---

THIS AGENT MUST READ `agents/task/<uuid>/<STEP_NUMBER>.md` with the largest `STEP_NUMBER` BEFORE ANY WORK

THIS AGENT MUST WRITE `agents/task/<uuid>/<STEP_NUMBER>.md` WITH RESULTS REPORT ABOUT HIS WORK

---

ALWAYS USE `ast-index` for any code search/navigation (symbols, files, usages, hierarchy). NEVER use grep/find when `ast-index` can do the job. Read about how to use it in `AST_INDEX.md`. This rule applies to ALL roles without exceptions.

if `ast-index` is not installed - install it with:

```bash
curl -fsSL https://raw.githubusercontent.com/JuliusBrussee/caveman/main/install.sh | bash
echo 'eval "$(/home/linuxbrew/.linuxbrew/bin/brew shellenv)"' >> ~/.bashrc
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
source ~/.bashrc

brew tap defendend/ast-index
brew install ast-index
```

After installation continue your task using `ast-index`

In case of changes in source files (any files) you MUST use ast-index rebuild

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
