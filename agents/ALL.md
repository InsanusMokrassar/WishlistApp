Always starts with `/caveman full`

Always look at the project work rules first. YOU MUST maintain a history of changes and prompts in `agents/HISTORY.md`, add new entries in the beginning of file. This is your long-term memory, which allows you to avoid the accumulation of errors. Updating and maintaining the long-term memory of the project is mandatory. Many thanks for your help to me and your work.

---

THIS AGENT MUST READ `agents/task/<uuid>/<STEP_NUMBER>.md` with the largest `STEP_NUMBER` BEFORE ANY WORK

THIS AGENT MUST WRITE `agents/task/<uuid>/<STEP_NUMBER>.md` WITH RESULTS REPORT ABOUT HIS WORK

---

if `ast-index` plugin has been installed - YOU MUST ALWAYS use it instead of grep/find if `ast-index` is suitable for you. Otherwise - in case of needs in search of files you must try to use `ast-index` first. Read about how to use it in `AST_INDEX.md`.

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
