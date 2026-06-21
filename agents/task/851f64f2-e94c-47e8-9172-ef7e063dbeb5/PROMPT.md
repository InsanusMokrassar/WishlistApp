# Task: GitHub Issue #54

## Title
Make breadcrumbs clickable

## Body
On click on breadcrumbs it must navigate onto the point in chain it reflects

## Context
A prior PR #57 (branch issue/54-clickable-breadcrumbs) attempted this and was CLOSED by the operator WITHOUT comments (silent rejection). This is a FRESH max-effort implementation. The prior approach is NOT assumed correct; design it properly. Prior branch may be inspected for reference only (`git show origin/issue/54-clickable-breadcrumbs`), not blindly copied.

## Git constraints
- Working on branch `fix/54-clickable-breadcrumbs` (already checked out). Do NOT create/switch/commit/push/stash branches or touch git history. Only modify working-tree files (code + task step files).

## Definition of Done
- Breadcrumbs are clickable and navigate to the reflected point in the navigation chain.
- Affected module(s) BUILD successfully (gradle build → BUILD SUCCESSFUL).
- Validation role confirms correctness.
