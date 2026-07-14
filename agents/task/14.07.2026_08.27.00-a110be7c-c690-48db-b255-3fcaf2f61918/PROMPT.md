# Task Prompt (operator, verbatim)

Use Opus, xhigh effort

1. Rebase all current PR's on master, if ANY problems will be faced - try use merge master to PR's branch instead
2. For #69 PR - extract all changes related to release of application (gradle configs and others) into separated PR, base new PR on master (not on the branch of 69 PR)

# Orchestrator notes

- Task type: git operations over open PRs #69 (`fix/66-admin-panel-left-panel-item`), #70 (`fix/67-users-feature-model`), #71 (`fix/68-roles`), all based on `master`.
- Operator model directive: "Use Opus, xhigh effort" overrides `agents/SHORTCUTS.md` role model priorities for this task — all role subagents run on `opus`. The Agent tool exposes no per-agent reasoning-effort parameter; effort inherits the session setting. Recorded here as the argued model choice for all stages.
- Orchestrator recon (to be independently verified by Planning):
  - `master` == `origin/master` == 497af56. All three PR branches fork from 0744a6f (`feat(release): ...`), which is already an ancestor of `origin/master`.
  - Commits on master missing from all three branches: 497af56 (agents framework rework), 761d052 (review task records). Both touch only `agents/**` and `.claude/settings.json`.
  - File overlap master-vs-branch: `fix/66` none, `fix/68` none, `fix/67` conflicts on `agents/CODING.md` (branch commit d91ab80 adds "Feature Interface Return Model Rule" to the old 1000+ line CODING.md; master's 497af56 reworked CODING.md to 269 lines + `agents/patterns/*`; the rule text is absent from master's new docs).
  - PR #69 GitHub diff shows release files (`.github/workflows/release.yml`, `CHANGELOG.md`, `changelog_info_retriever`, `client/android/build.gradle`, `client/build.gradle`, `gradle.properties`) only because GitHub's merge-base for the PR is stale: it still counts 0744a6f as a PR commit. Local three-dot diff `origin/master...origin/fix/66-admin-panel-left-panel-item` contains NO release files; release files are byte-identical between branch and master. The release changes are ALREADY on master (commit 0744a6f) — prompt item 2 contradicts repository reality; a new "release changes" PR based on master would have an empty diff. Requires operator clarification before executing item 2.
- Stage deviation: Architecturing stage is skipped for this task — pure git operations, no new code structures and no test stubs to write (severity: Low, recorded per CLAUDE.md violation-handling rule).
