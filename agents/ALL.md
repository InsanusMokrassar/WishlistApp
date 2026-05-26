Always look at the project work rules first. Update them and maintain a history of changes and prompts in `HISTORY.md`. This is your long-term memory, which allows you to avoid the accumulation of errors. Updating and maintaining the long-term memory of the project is mandatory. Many thanks for your help to me and your work.

---

In case of needs in search of files you must try to use `ast-index` first. Read about how to use it in `AST_INDEX.md`

In case of changes in source files (any files) you MUST call `ast-index rebuild`

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
