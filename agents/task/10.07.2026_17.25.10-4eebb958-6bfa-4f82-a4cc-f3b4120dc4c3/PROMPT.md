# PROMPT

## Source: GitHub issue #68 (verbatim)

**Title:** Implement roles in project

**Body:**
> It is required to add support of roles in project. For roles library have been chosen `dev.inmo:kroles` library. What is required:
>
> 1. Add feature for roles
> 2. Include there kroles library
> 3. Add exposed and caching repos for kroles roles repo. By default must be used cache one, it must fully mirror exposed state
> 4. Add aggregator of features and required role for them
> 5. Add hardcoded role `SuperAdmin` (will be set to user `root`)
> 6. Add role `User` and add opportunity to set it automatically to all users (in server users feature?) + add small migration to add to all currently exists users `User` role
> 7. Create simple feature for roles with only one suspend fun to check is user superadmin or not (on server with UserId argument, on client - without)
> 8. All server places where it is required to check that user is root - replace to usage of user feature from 7-th point
> 9. All client places where it is required to check that user is root - replace with checks of user's superadmin status
> 10. Roles feature on client must have two realizations - ktor and cache. Last one must cache only answer for is superadmin user or not

## Context (observed by the issue-executor before delegating to Root)

- Branch `fix/68-roles`, created off `master` (synced from `origin/master`, plus the same pre-existing
  unrelated unpushed local commit noted in prior tasks on this repo, `feat(release): ...` — not touched
  by this task, and now also including the two just-merged-locally... actually NOT merged, `master` here
  is the same base both `fix/66-...` and `fix/67-...` branched from; those two PRs (#69, #70) are still
  open/unmerged on GitHub as of this task's creation — this branch does NOT include their changes). No
  existing PR references issue #68.
- **This is the largest and most architecturally significant of the three currently-open issues.** It
  introduces a brand-new external dependency (`dev.inmo:kroles` — Root/Planning must investigate this
  library's actual API from scratch; it is not yet a dependency anywhere in this repo, verified via
  `grep -rn "kroles" gradle/`), a new feature module, a DB-backed repo pattern (this repo has an
  established "CRUD Repository Pattern" convention in `agents/CODING.md` for Exposed+cache repos —
  follow it, adapting for whatever `kroles`' own repo/role-storage abstractions actually look like), a
  data migration for existing users, and a **repo-wide replacement of every root-privilege check** on
  both server and client.
- Quick, non-exhaustive reconnaissance of existing root-check call sites (Planning MUST do its own
  complete audit via `ast-index` — this list is a starting lead, not authoritative or complete):
  - Server-side root/admin checks found in: `features/ui/adminPanel/.../AdminPanelViewModel.kt`,
    `features/ui/users/.../Plugin.kt`, `features/files/server/.../FilesRoutingsConfigurator.kt`,
    `features/auth/server/.../JVMPlugin.kt` (this is where the root-user bootstrap lives, per
    `agents/CODING.md`'s "Root-user bootstrap" section — creates the `root` user on first startup;
    relevant to point 5's "hardcoded role SuperAdmin will be set to user root"),
    `features/email/server/.../EmailFeatureService.kt` (root-only `sendTestEmail`, from a recently
    completed task on this repo), `features/admin/server/.../AdminRoutingsConfigurator.kt`.
  - Client-side root/admin checks found in: `features/ui/users/**` (multiple files — `UserEditView.kt`
    on jvm/js/android, `UserEditViewModel.kt`, `UsersModel.kt`, `UserViewModel.kt`, `Plugin.kt`),
    `features/files/server/.../FilesRoutingsConfigurator.kt` (appears in both lists — verify why).
  - The most recently completed task on this branch's history (`agents/task/10.07.2026_11.49.17-...`,
    issue #66, PR #69) added a root-only sidebar item using `UsersModel.isCurrentUserRootFlow` — this is
    exactly the kind of client-side check point 9 asks to replace with a superadmin-status check.
- Point 6's exact migration mechanism ("small migration to add to all currently exists users `User`
  role") needs a concrete design — check whether this repo has any existing precedent for one-time data
  migrations beyond schema migrations (`initTable()`'s `createMissingTablesAndColumns` is schema-only,
  not a data-backfill mechanism) before inventing one.
- Given the size and number of genuinely open design questions this issue is likely to raise (kroles'
  actual API shape, exact DI/module boundaries for a new cross-cutting "roles" feature that every other
  feature's root-check depends on, the migration mechanism, whether `SuperAdmin`/`User` are the only two
  roles or whether the aggregator in point 4 needs to support more), Planning should not hesitate to ask
  the operator multiple concrete questions if genuinely blocked — per `agents/PLAN.md` step 4, do not
  guess on architectural decisions this consequential.

## Constraints (from `agents/*.md`, apply regardless of prompt content)

- Follow `agents/ARCHITECTURE.md`'s Feature adding rules (`./generate_feature.sh` for the new roles
  feature module) and CRUD Repository Pattern (Exposed + cache repo, `agents/CODING.md`) as closely as
  `kroles`' own abstractions allow.
- Follow the newly-established "Feature Interface Return Model Rule" (`agents/CODING.md`, added by a
  just-completed task on this repo, issue #67) for the new roles feature's own interfaces — do not
  reintroduce the pattern that rule was written to prevent.
- Branch stays `fix/68-roles`. Do not touch `master`, do not open a PR yourself — the issue-executor
  (Root's caller) handles branch push, PR creation (`Closes #68`), and reviewer assignment after the
  full cycle completes.
- Run `ast-index rebuild` after `.kt` changes.
- Given the scope, this task may legitimately need more than one Planning round and a very detailed
  Architecture pass before Coding begins — do not rush to Coding with an underspecified plan for a
  change this size and this security-sensitive (it replaces every privilege check in the app).
