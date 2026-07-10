# PROMPT

## Source: GitHub issue #67 (verbatim)

**Title:** Add special model for UsersFeature

**Body:**
> 1. Method `UsersFeature.getAll` now returns list of `RegisteredUsers`. It is required to create special model for this method (like `UsersFeatureUser`) to be returned instead of `RegisteredUser`, this model must not contains email of user.
> 2. Write as a rule, that any data returned from features (excluding identificators, primitive types, libraries types like streams and other non-project types) must be covered by Feature-related model
> 3. In separate commit fix other features which do not follow rule from point 2

## Context (observed by the issue-executor before delegating to Root)

- Branch `fix/67-users-feature-model`, created off `master` (synced from `origin/master`, plus the same
  pre-existing unrelated unpushed local commit noted in the previous task, `feat(release): ...` — not
  touched by this task). No existing PR references issue #67.
- Both `UsersFeature` interfaces (`features/users/client/src/commonMain/kotlin/UsersFeature.kt` and
  `features/users/server/src/commonMain/kotlin/UsersFeature.kt`) declare `suspend fun getAll():
  List<RegisteredUser>`. `RegisteredUser` (`features/users/common/src/commonMain/kotlin/models/`) carries
  `id`, `username`, AND `email: Email?` (the nullable email field added by a prior task on this repo).
  **The server interface's own KDoc says "No auth required at the route layer."** — i.e. this is
  genuinely a public, unauthenticated endpoint currently leaking every registered user's email address
  to any caller. This is a real, not merely cosmetic, issue — Root/Planning should treat point 1 with
  that severity in mind, not just as a style/architecture cleanup.
- Point 1 is a concrete, scoped fix: introduce a new model (issue suggests the name `UsersFeatureUser`,
  Planning/Architecture should decide the exact final name and its exact field set — at minimum it must
  drop `email`, but should preserve whatever else `RegisteredUser` exposes today, e.g. `id`/`username`)
  and change `getAll()`'s return type on both the client and server `UsersFeature` interfaces (and every
  implementation: server `UsersFeatureService`/whatever implements it, client `KtorUsersFeature`, plus
  any DI wiring and all call sites — investigate before assuming the exact blast radius).
- Point 2 is a NEW, general, written-down coding rule for this repository: **any data a `*Feature`
  interface (the client/server public capability interfaces this repo's architecture is built around —
  see `agents/ARCHITECTURE.md`'s "Server Architecture"/"Client Architecture" sections) returns must be a
  type belonging to that feature's own model set — not a shared/repo-layer model reused verbatim —
  except identifiers, primitive types, and non-project library types (e.g. streams).** This rule needs
  to be written into the appropriate `agents/*.md` file (most likely `agents/CODING.md` or
  `agents/ARCHITECTURE.md`, alongside this repo's other model-layer conventions like the existing
  "CRUD Repository Pattern"/value-class-naming rules) so future feature work follows it automatically.
  Root/Planning must decide the exact wording and placement, and must decide precisely what counts as
  "a project type" vs. an allowed exception (e.g.: is `RegisteredUser` itself — a `commonMain` model
  shared across the `users` feature's own client/server/UI consumers — an allowed return type from
  `UsersFeature` methods that legitimately need the full user record, or does EVERY feature method need
  its own bespoke return model even when an existing project model already fits exactly? The email-leak
  case in point 1 argues for "a feature's *public-facing* interface must return exactly what it needs to
  expose, not whatever the underlying repo model happens to carry" — but point 2's literal wording is
  broader than just "public-facing"/auth-boundary methods. This ambiguity should be resolved by
  Planning, asking the operator if genuinely unclear rather than guessing, since it will directly decide
  the audit scope of point 3.)
- Point 3 requires auditing every OTHER `*Feature` interface in the repo against the new rule and fixing
  violations, in a separate commit from the rule-writing/point-1-fix commit. This repo has multiple
  feature modules beyond `users` — Planning must enumerate them (e.g. via `ast-index` or `grep -rn
  "interface.*Feature"` across `features/*/client` and `features/*/server`) and check each method's
  return type against the finalized rule from point 2. Do not assume the audit's scope/results — investigate
  and report findings concretely.

## Constraints (from `agents/*.md`, apply regardless of prompt content)

- Follow `agents/CODING.md`'s existing model-layer conventions (value-class property naming, KDoc
  requirements) for any new model.
- Per the issue's own point 3: the rule-establishing change (point 1's `UsersFeatureUser` fix + point 2's
  written rule) and the cross-feature audit fixes (point 3) MUST land as separate commits — do not squash
  them together even though they're part of the same PR/branch. This may mean two separate Coding-role
  passes within the same cycle (or across cycles) rather than one — Architecture should plan for this
  explicitly.
- Branch stays `fix/67-users-feature-model`. Do not touch `master`, do not open a PR yourself — the
  issue-executor (Root's caller) handles branch push, PR creation (`Closes #67`), and reviewer assignment
  after the full cycle completes.
- Run `ast-index rebuild` after `.kt` changes.
