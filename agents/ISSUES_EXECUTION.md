# Issues Execution Algorithm

Agent tasked with fixing GitHub issues MUST follow these steps in order.

Repo: `InsanusMokrassar/WishlistApp`.

## Steps

1. Run `/caveman full` (rule: `agents/ALL.md`).
2. Start with the explicit root invocation in `agents/ROOT_PROMPT.md`, read the shared `AGENTS.md`, and explicitly load `agents/SHORTCUTS.md`. Follow `CLAUDE.md` guidance without using an automatically loaded file as a routing catalog.
3. List open issues: `gh issue list --repo InsanusMokrassar/WishlistApp --state open`.
4. Pick ONE issue that has NO merged and NO open linked PR. Check linked PRs via the GitHub API — a branch-name pattern alone is NOT sufficient:
   ```
   gh pr list --repo InsanusMokrassar/WishlistApp --state all --search "<N> in:body" --json number,state,mergedAt,headRefName,title
   gh pr list --repo InsanusMokrassar/WishlistApp --head "fix/issue-<N>-*" --state all
   ```
   Treat a PR as linked if its body/title references the issue (`Closes #<N>`, `#<N>`) or its branch matches the pattern.
   - If a linked PR was **merged** → skip this issue (already done).
   - If a linked PR is **open** → skip this issue (in progress).
   - If linked PRs were **closed without merging** → count them as failed attempts. **3 or more** failed attempts → post `gh issue comment <N> --repo InsanusMokrassar/WishlistApp --body "AGENT ESCALATION: 3 attempts failed, manual intervention required"` and skip the issue. Fewer than 3 → a fresh attempt is allowed.
   - If no linked PR exists → proceed.
5. Sync with master per `agents/GIT.md` "## Before Branching".
6. Create branch for fix: `git checkout -b fix/issue-<N>-<slug>`.
7. Resolve the issue: act as `root` (Orchestrator) yourself per `agents/ORCHESTRATOR.md` — the MAIN SESSION is the root role; do NOT spawn a `root` subagent.
8. Apply the root [mandatory feature acceptance gate](ORCHESTRATOR.md#mandatory-feature-acceptance-gate) before publication, including an actual update to the common [graph of navigation](../NAVIGATION.md) for every change. A no-navigation-impact fix still records its scope, affected IDs or explicit empty set, reason and checks there. A green build cannot waive a missing graph update or required transition/visual/platform evidence. Carry every scoped operator-approved limitation and actual outcome into the final report and PR; never claim unconditional acceptance with an exception. Push branch: `git push origin fix/issue-<N>-<slug>`.
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

- One issue at a time. Stage subagents run sequentially per `agents/ORCHESTRATOR.md` (avoids branch/index conflicts).
- If issue is ambiguous or blocked → stop, ask operator. If no terminal access is available, post a question as a GitHub issue comment:
  ```
  gh issue comment <N> --repo InsanusMokrassar/WishlistApp --body "AGENT BLOCKED: <question>"
  ```
  Then terminate. On next run, read the issue comments for answers before proceeding.
- If operator left a review on the issue's PR → treat it as continuation of the same issue: read ALL review comments (`gh api repos/InsanusMokrassar/WishlistApp/pulls/<PR#>/comments`), fix every comment on the SAME branch, push. Do NOT open a new issue or PR. Continue the issue work where it was left off.
