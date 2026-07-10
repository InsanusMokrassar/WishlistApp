# Issues Execution Algorithm

Agent tasked with fixing GitHub issues MUST follow these steps in order.

Repo: `InsanusMokrassar/WishlistApp`.

## Steps

1. Run `/caveman full`.
2. Read `CLAUDE.md` (and follow `AGENTS.md` / `agents/SHORTCUTS.md` chain it points to).
3. List open issues: `gh issue list --repo InsanusMokrassar/WishlistApp --state open`.
4. Pick ONE issue that has NO open or merged PR. Check linked PRs by branch name pattern:
   ```
   gh pr list --repo InsanusMokrassar/WishlistApp --head "fix/issue-<N>-*" --state all
   ```
   - If a PR exists and was **merged** → skip this issue (already done).
   - If a PR exists but was **closed without merging** → treat the issue as open; a fresh attempt is allowed.
   - If no PR exists → proceed.
5. Sync with master: `git checkout master && git pull origin master`.
6. Create branch for fix: `git checkout -b fix/issue-<N>-<slug>`.
7. Resolve the issue: you MUST start subagent with `root` role to solve the issue. Wait for it. YOU MUST CONTROL THAT ALL SUBAGENTS FOLLOWING THEIR INSTRUCTIONS FROM `agents` FOLDER IF OTHER IS NOT SAID IN USER PROMPT
8. Push branch: `git push origin fix/issue-<N>-<slug>`.
9. Open PR linked to the issue (body must contain `Closes #<N>`):
   ```
   gh pr create --repo InsanusMokrassar/WishlistApp --base master --head fix/issue-<N>-<slug> --title "..." --body "Closes #<N> ..."
   ```
10. Set reviewer = repo owner (operator):
    ```
    gh pr edit <PR#> --repo InsanusMokrassar/WishlistApp --add-reviewer InsanusMokrassar
    ```
11. Get back onto `master` branch: `git checkout master`.

## Rules

- One subagent per issue. Subagents run SEQUENTIALLY, never in parallel (avoid branch/index conflicts).
- If issue is ambiguous or blocked → stop, ask operator. If no terminal access is available, post a question as a GitHub issue comment:
  ```
  gh issue comment <N> --repo InsanusMokrassar/WishlistApp --body "AGENT BLOCKED: <question>"
  ```
  Then terminate. On next run, read the issue comments for answers before proceeding.
- If operator left a review on the issue's PR → treat it as continuation of the same issue: read ALL review comments (`gh api repos/InsanusMokrassar/WishlistApp/pulls/<PR#>/comments`), fix every comment on the SAME branch, push. Do NOT open a new issue or PR. Continue the issue work where it was left off.
