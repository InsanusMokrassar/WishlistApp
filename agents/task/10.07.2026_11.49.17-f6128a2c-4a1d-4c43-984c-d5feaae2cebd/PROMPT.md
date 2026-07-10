# PROMPT

## Source: GitHub issue #66 (verbatim)

**Title:** In left panel add item for admin panel

**Body:**
> If user is root, left panel must include item to open admin panel view in main part

## Context (observed by the issue-executor before delegating to Root)

- This task is being executed per `agents/ISSUES_EXECUTION.md`: branch `fix/66-admin-panel-left-panel-item`
  created off `master` (synced from `origin/master`, plus one pre-existing unpushed local commit on
  `master` unrelated to this work — `feat(release): ...`, not touched by this task).
- No existing PR references issue #66 (confirmed via `gh pr list --state all`).
- Repo already has an admin panel UI feature (`features/ui/adminPanel`, per a prior session's read-only
  navigation-diagram task which found "admin subtree: 8 Admin*ViewConfig, wired internally, ZERO inbound
  edges → unreachable in-app" — i.e. the admin panel screens exist and are internally wired, but nothing
  in the live client UI currently navigates to them). This issue is exactly about closing that gap: adding
  a reachable entry point.
- "Left panel" refers to the web client's `Sidebar`/left-nav chain (per the same navigation-diagram
  finding: "left chain (Sidebar, web) → every click resetToSingleNode on main"). Root must independently
  verify this — the diagram finding is from a different, unrelated prior session and should be treated as
  a lead to investigate, not settled fact.
- "If user is root" — root must determine how root/superadmin status is currently checked in this
  codebase (there is no roles system yet; issue #68, separately open, proposes adding one via `kroles`,
  but this task should NOT wait for or depend on #68 — use whatever root-check mechanism currently exists,
  e.g. comparing `Username.string` against a hardcoded `"root"` literal, matching the pattern already used
  server-side in `EmailFeatureService.sendTestEmail`/`rootUsername`).

## Constraints (from `agents/*.md`, apply regardless of prompt content)

- Follow `agents/ARCHITECTURE.md`'s UI Feature Adding / Navigation / MVVM rules and `agents/CODING.md`'s
  Design System Rule (Calm Studio — no raw CSS, no Bootstrap, components from
  `features/common/client/.../ui/components/`) for any web client UI changes.
- This issue only asks for a **web ("left panel")** entry point — do not assume JVM/Android need parallel
  changes unless investigation shows sidebar-equivalent chrome exists there too (per the nav-diagram
  finding, the left/Sidebar chain was flagged "web-only").
- Branch stays `fix/66-admin-panel-left-panel-item`. Do not touch `master`, do not open a PR yourself —
  the issue-executor (Root's caller) handles branch push, PR creation (`Closes #66`), and reviewer
  assignment after your full Planning→Architecture→Coding→Verification→Validating cycle completes.
- Run `ast-index rebuild` after `.kt` changes.
