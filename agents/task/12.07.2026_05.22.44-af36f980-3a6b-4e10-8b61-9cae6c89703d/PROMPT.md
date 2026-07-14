# Source Prompt

> For each open PR make review with fable model. You may left commentaries on github or in file in PR - this decision on you

## Orchestrator interpretation

- Open PRs at task start: #69 (fix/66-admin-panel-left-panel-item), #70 (fix/67-users-feature-model), #71 (fix/68-roles).
- Review model: fable (user-mandated; overrides SHORTCUTS.md role model priorities where they differ).
- Delivery decision (delegated to agent by user): reviews are posted on GitHub as PR review comments via `gh pr review` — no review files committed into PR branches.
- Task type: review-only. No production code changes. Stage state machine reduced to review (validating-role) steps, one per PR, executed in parallel by fable subagents.
- Step assignment: 001-validating → PR #69, 002-validating → PR #70, 003-validating → PR #71.
- Git: subagents do NOT commit/push (prompt-level override per agents/GIT.md "UNLESS the prompt gives other direction"); Orchestrator commits step reports after all reviews complete.
