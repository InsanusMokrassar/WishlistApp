# PROMPT

## Source operator prompt (verbatim)

> solve unresolved commentaries for this PR

## Context (observed by Root before handoff to Planning)

- Branch `fix/44-email` is linked to PR #63 (`feat(email): base email feature (#44)`,
  https://github.com/InsanusMokrassar/WishlistApp/pull/63), OPEN.
- Per `agents/ISSUES_EXECUTION.md`'s rule: "If operator left a review on the issue's PR → treat it as
  continuation of the same issue: read ALL review comments, fix every comment on the SAME branch, push.
  Do NOT open a new issue or PR." This task follows that rule — same branch, no new PR.
- Fetched all PR #63 review threads via `gh api graphql` (`reviewThreads` with `isResolved`/`isOutdated`).
  Three threads total:
  1. `features/email/common/.../models/Email.kt` — **already resolved** (rename `raw`→`string`, done in
     commit `b7c6644`, predates this session).
  2. `features/email/client/.../EmailFeature.kt` — **already resolved** (split client/server `EmailFeature`,
     done in commit `a9d85bf`, predates this session).
  3. `features/users/common/src/jvmMain/kotlin/repo/ExposedUsersRepo.kt` line 42 — **UNRESOLVED**, posted
     2026-07-10T09:22:12Z by the repo owner (`InsanusMokrassar`), body verbatim: *"Emails for users must
     be unique"*. Targets `private val emailColumn = text("email").nullable()` — currently has no
     `.uniqueIndex()` (unlike the adjacent `usernameColumn = text("username").uniqueIndex()` two lines
     above it in the same file).

So there is exactly **one** unresolved review comment to address. No other open threads exist on this PR
(confirmed via the GraphQL query — `reviewThreads(first: 100)` returned all 3 nodes, no pagination needed).

## Scope for Planning to investigate

The literal, minimal fix is adding `.uniqueIndex()` to `emailColumn` in `ExposedUsersRepo.kt`. Planning
must additionally investigate and decide (or flag as an open question if genuinely ambiguous) the
following, since a DB-level unique constraint changes failure behavior for existing call sites:

- `EmailFeatureService.setMyEmail` / `DisabledEmailFeature.setMyEmail` (both delegate to the shared
  `updateStoredEmail` helper introduced in a prior task on this branch) call
  `usersRepo.update(callerId, NewUser(...))` and treat a non-null result as success (`true`), null as
  failure (`false`). Read `AbstractExposedCRUDRepo`'s `update`/insert implementation (from
  `dev.inmo.micro_utils.repos.exposed`, likely a dependency jar — check via `ast-index` or the Gradle
  cache/sources if available) to determine: does a unique-constraint violation on `emailColumn` surface
  as a thrown exception (e.g. `ExposedSQLException`/`SQLException`) that propagates uncaught out of
  `update()`, or does the CRUD repo layer already catch DB exceptions and translate them to a `null`
  return? This determines whether any new exception handling is needed at all.
  - Check whether Ktor `StatusPages` (`ApplicationStatusPagesConfigurator`/similar, per
    `agents/ARCHITECTURE.md`'s "Server Configurator Extension Points" table) is installed anywhere in
    this repo and whether an uncaught exception from a route handler already gets translated to *some*
    HTTP response today (even a generic unhandled 500), vs. crashing the request pipeline in a way that
    differs from the existing `HttpStatusCode.InternalServerError` fallback already used in
    `EmailRoutingsConfigurator`'s `put(myEmailPathPart)` handler.
  - `PUT /email/myEmail` currently responds `200 OK` on success or `500 Internal Server Error` on any
    `setMyEmail() == false`. Decide: is a generic 500 (indistinguishable from "user not found" or any
    other failure) an acceptable response when the actual cause is "email address already taken by
    another user", or does this warrant a more specific signal (e.g. `409 Conflict`) so API consumers
    can tell the two failure modes apart? This is a genuine design decision beyond the literal review
    comment (which only asked for uniqueness at the DB level) — if `agents/CODING.md`/`agents/ARCHITECTURE.md`
    contain a relevant precedent or convention for this repo (check other unique-constrained fields,
    e.g. `usernameColumn`, for how duplicate-username handling is done today, if at all), follow it; if
    there is no precedent and multiple reasonable designs exist, ask the operator rather than guess.

## Constraints (from `agents/*.md`, apply regardless of prompt content)

- Follow `agents/CODING.md`: KDocs on every new/changed declaration, `when` over `else if`, run the
  module build task after coding, update `features/email/README.md` and/or `features/users/README.md`
  (whichever documents the affected behavior — check both) to match. Never touch `## Operator Notes`.
- Stay on branch `fix/44-email`. Do not create a new branch, do not touch `master`, do not open a new PR.
- After the fix is committed (and pushed, since the operator explicitly asked to solve/resolve the PR
  comment — resolving requires the fix to exist on the remote branch the PR tracks), reply to and resolve
  the GitHub review thread. Root will handle the actual `gh api graphql` mutation to mark the thread
  resolved (thread id `PRRT_kwDOSnS5tM6P1Vhi`, comment id for a reply is via
  `gh api repos/InsanusMokrassar/WishlistApp/pulls/63/comments/3557799588/replies` or the GraphQL
  `addPullRequestReviewThreadReply`/`resolveReviewThread` mutations) — this does not need to be done by
  any of the Planning/Architecture/Coding/Verification/Validating subagents, it is a post-cycle
  Orchestrator action once Validating passes.
