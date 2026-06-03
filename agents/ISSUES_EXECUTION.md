# Issues Execution Algorithm

Agent tasked with fixing GitHub issues MUST follow these steps in order.

Repo: `InsanusMokrassar/WishlistApp`.

## Steps

1. Run `/caveman full`.
2. Read `CLAUDE.md` (and follow `AGENTS.md` / `agents/SHORTCUTS.md` chain it points to).
3. List open issues: `gh issue list --repo InsanusMokrassar/WishlistApp --state open`.
4. Pick ONE issue that has NO related (linked / referencing) PR. Check linked PRs:
   `gh issue view <N> --repo InsanusMokrassar/WishlistApp --json title,body,number` and search PRs referencing it
   `gh pr list --repo InsanusMokrassar/WishlistApp --search "<N> in:body" --state all`.
   Skip issues that already have a PR.
5. Create branch for fix: `git checkout -b issue/<N>-<slug>`.
6. Resolve the issue: you MUST start subagent with `root` role to solve the issue. Wait for it. YOU MUST CONTROL THAT ALL SUBAGENTS FOLLOWING THEIR INSTRUCTIONS FROM `agents` FOLDER IF OTHER IS NOT SAID IN USER PROMPT
7. Commit, push branch.
8. Open PR linked to the issue (body must contain `Closes #<N>`):
   `gh pr create --repo InsanusMokrassar/WishlistApp --base master --head fix/issue-<N>-<slug> --title "..." --body "Closes #<N> ..."`.
9. Set reviewer = repo owner (operator):
   `gh pr edit <PR#> --repo InsanusMokrassar/WishlistApp --add-reviewer InsanusMokrassar`.
10. Get back onto `master` branch

## Rules

- One subagent per issue. Subagents run SEQUENTIALLY, never in parallel (avoid branch/index conflicts).
- Update `agents/HISTORY.md` at end of work (haiku agent).
- If issue ambiguous or blocked → stop, ask operator.
