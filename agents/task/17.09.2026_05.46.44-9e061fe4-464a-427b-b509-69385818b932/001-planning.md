Model: OpenAI Sol
Changed files: agents/task/17.09.2026_05.46.44-9e061fe4-464a-427b-b509-69385818b932/001-planning.md

## Model choice

OpenAI Sol is a high-level model and is the preferred available class for Planning under `agents/SHORTCUTS.md` and `agents/MODELS.md`. This task requires cross-branch history analysis, three-way merge prediction, and preservation of a new repository-wide MVVM convention. The explicit no-subagent instruction prevents separate low-level Markdown transcription, so the assigned Planning model writes this report directly in normal prose.

## Task understanding

Update every pull request that was open at task start by merging current `master` into its head branch, resolving conflicts without losing either the pull request behavior or the default-model migration merged in PR #84, and verifying each updated branch independently. The fixed branch set is PR #82 (`feat/issue-79-user-email-change`), PR #81 (`fix/issue-78-email-authorized-password-change`), and PR #75 (`fix/issue-72-detekt-conventions`). All three target `master` in the same repository. The master reference and `origin/master` both point to `a220b3d`, the merge commit for PR #84.

The governing MVVM rule now requires each production UI Model interface to have a public `Default<InterfaceName>` implementation in a separate `commonMain` file. All outside-world dependencies must be primary-constructor `private val`s, and `Plugin.kt` must remain a composition root that binds the interface as a Koin `single`. Anonymous production Model implementations are forbidden. Interactors, test doubles, feature client implementations, and server services are not MVVM Models and are not subject to this extraction rule.

The branch set and base commit are fixed for this run. Do not discover and add later pull requests midway through implementation; doing so would make verification and acceptance non-deterministic. If a listed pull request closes before push, preserve the completed local branch update and report the external-state change to the orchestrator instead of redirecting work to another branch.

## Investigation result

Repository instructions, the task prompt, the current MVVM pattern, the PR #84 planning/architecture/validation record, and all feature READMEs touched by PR #81 were read. The relevant Operator Notes contain only the standard placeholder and introduce no extra constraint. `ast-index` was available; its default index was absent, so it was rebuilt with `XDG_CACHE_HOME=/tmp/wishlist-open-pr-sync-ast`. The rebuilt index contains 801 files and 49 modules. It identifies `DefaultUsersModel` as the production `UsersModel` implementation and `Plugin.kt` as its interface binding.

Read-only three-way merge analysis used each branch's actual merge base. PR #82 is based on `a2dc220` and predicts no content conflict. PR #81 is based on `4318025` and predicts conflicts in `features/ui/users/README.md`, `features/ui/users/build.gradle`, `features/ui/users/src/commonMain/kotlin/Plugin.kt`, and `features/ui/users/src/commonTest/kotlin/UsersModelTest.kt`. Files changed on both sides but without conflict, including the three platform `UserEditView.kt` files, still require semantic review. PR #75 is based on `474d49c` and predicts no content conflict; `gradle/libs.versions.toml` changes on both sides but merges automatically because the edits occupy compatible entries.

The only branch-specific violation of the new MVVM rule is in PR #81. That branch adds `PasswordChangeFeature` and its two delegations directly to an anonymous `object : UsersModel` inside `Plugin.kt`. Current master has already moved the former anonymous body into `DefaultUsersModel`. PR #81 must therefore add `PasswordChangeFeature` to the named class constructor and move `requestPasswordChangeEmail` and `completePasswordChange` into that class while retaining serializer and ViewModel registrations in `Plugin.kt`. PR #82 changes the existing `UsersModel` contract documentation and editor behavior but does not add another implementation. PR #75 changes build tooling only. Merging master supplies compliant default classes to both branches without further model extraction.

GitHub API refresh was unavailable from the sandbox, but the orchestrator supplied the verified open-PR set and the local heads match their `origin/*` references: PR #82 at `077fcd2`, PR #81 at `ffd73fd`, and PR #75 at `5c5d0d4`. No evidence creates a product or architecture ambiguity.

## Merge strategy

Process branches independently and serially. Before each merge, require a clean worktree, confirm the branch head still matches the recorded remote-tracking head, and record the pre-merge SHA for recovery. Checkout the pull request head, then merge commit `a220b3d` through the local `master` ref with a real merge commit; do not rebase, squash, force-push, or merge one pull-request branch into another. This preserves existing review history and gives each PR an explicit master-sync point.

Keep the orchestration task files on `chore/open-pr-mvvm-sync-20260917`; do not accidentally carry `PROMPT.md`, planning files, or reports into the pull-request branches. The untracked task `PROMPT.md` remains root-owned and must not be staged by Planning or by branch-update commits.

For a clean merge, review the resulting tree before committing the merge. For PR #81, resolve all four conflicts and finish the MVVM migration as part of the merge result, because accepting either entire side of `Plugin.kt` would respectively lose the password-change feature or restore a forbidden anonymous Model. After every merge, rebuild `ast-index` because Kotlin sources will change in the checked-out tree. Commit and verify one branch before moving to the next so failures and rollback stay branch-local. Push remains an orchestrator action after all required role gates; individual roles must not push.

## Branch plan: PR #82

Merge `master` into `feat/issue-79-user-email-change` at recorded head `077fcd2`. The three-way analysis predicts an automatic merge. Semantically review the merged users README and all email-editor source and tests because both the branch and master document or exercise `UsersModel`, even when Git reports no textual conflict.

Retain the branch's owner-email replacement behavior and its additions to `UsersModel` documentation. Retain master's `DefaultUsersModel.kt`, the short `single<UsersModel> { DefaultUsersModel(...) }` binding in `Plugin.kt`, and master's concrete-class and singleton assertions in `UsersModelTest`. Do not move editor state or presentation logic into the Model while reconciling files. The Model remains an outside-world facade; `UserEditViewModel` remains owner of email draft, capability, busy, error, and reconciliation state.

No new constructor dependency or Model method is introduced by this branch. The expected model work is therefore preservation proof: `DefaultUsersModel` must remain the only production `UsersModel` implementation, its constructor dependencies must remain unchanged, and `Plugin.kt` must not regain an anonymous object. Preserve PR #82's focused transport/routing tests and the JVM email-render test alongside PR #84's default-model tests.

Run at minimum the users UI, email client, and email server module builds that own the merged behavior, then the full `./gradlew build`. Exercise the branch's owner-email replacement and stale-owner/session tests, Ktor email client tests, email routing tests, `UsersModelTest`, and `UsersModelFileTest`. Confirm `git diff --check`, rebuild the AST index, and verify no production anonymous Model implementation appears under `features/ui`.

## Branch plan: PR #81

Merge `master` into `fix/issue-78-email-authorized-password-change` at recorded head `ffd73fd`. Resolve the four predicted conflicts by combining behavior rather than choosing a whole side:

1. In `features/ui/users/README.md`, preserve the password-change screens, routes, model methods, navigation lifecycle, and security notes from PR #81, while retaining master's explicit `DefaultUsersModel` implementation note. Update the documented constructor dependency list to include `PasswordChangeFeature`. Preserve the Operator Notes section byte-for-byte.
2. In `features/ui/users/build.gradle`, retain every source-set dependency required by the password-change browser/native tests and every test dependency added by PR #84 for named-model/file delegation coverage. Do not remove a dependency merely because only one side introduced it; verify actual source-set consumers after the merge.
3. In `features/ui/users/src/commonMain/kotlin/Plugin.kt`, retain PR #81's `PasswordChangeViewConfig` serializers and `PasswordChangeViewModel` factory. Delete the anonymous `object : UsersModel` body. Bind `UsersModel` to `DefaultUsersModel` using named constructor arguments and pass all master dependencies plus `passwordChangeFeature = get()`. Keep `meState = meStateFlow` explicit rather than resolving an unqualified state flow.
4. In `features/ui/users/src/commonTest/kotlin/UsersModelTest.kt`, preserve master's direct construction and `assertIs<DefaultUsersModel>`/singleton coverage while adding PR #81's recording `PasswordChangeFeature` double and exact-argument assertions for both password-change delegations. The test must prove the methods live on and delegate through `DefaultUsersModel`, not merely that Koin can resolve the interface.

Extend `features/ui/users/src/commonMain/kotlin/ui/DefaultUsersModel.kt` with a primary-constructor `private val passwordChangeFeature: PasswordChangeFeature`. Move PR #81's `requestPasswordChangeEmail(expectedEmail)` and `completePasswordChange(request)` bodies into that class without changing argument mapping, nullable return contracts, retry behavior, or exception behavior. Import the password-change DTO/result types in the interface and named implementation as needed. Keep Koin access out of the class.

Retain all other PR #81 work, including auth client/server/common contracts, email issuance and deep-link services, browser navigation ownership, serializers, platform views, and their security/cancellation tests. Review auto-merged `UserEditView.kt`, client navigation, auth/email/deeplink plugins, and related READMEs for semantic loss even when they have no conflict marker. The merge must not weaken approval binding, leak approval IDs or passwords, restore a logged-in requirement for token-authorized completion, or change the branch's credential invalidation rules.

Build and test the users UI, auth client/common/server, email server, deeplinks server, common server, and top-level client modules, including JS browser tests and JVM/Android view tests already present on the branch. Run the full `./gradlew build` after focused gates. Explicitly run `UsersModelTest` and confirm both named-class identity and exact password-change delegation. Rebuild `ast-index`, verify `DefaultUsersModel` is the sole production implementation, verify `Plugin.kt` contains only composition wiring for that Model, and run `git diff --check`.

## Branch plan: PR #75

Merge `master` into `fix/issue-72-detekt-conventions` at recorded head `5c5d0d4`. Preserve the Detekt version, plugin classpath aliases, custom `NoElseIf` rule module, root convention, verification-before-build workflow, and project-scoped baselines. Retain master's dependency catalog updates when reviewing the automatically merged `gradle/libs.versions.toml`.

This branch adds no MVVM Model behavior. The model requirement is satisfied by the eight `Default*Model` files and Plugin bindings arriving from master. Do not edit those implementations merely to regenerate lint data. Run the merged Detekt gate first to determine whether PR #84's new Kotlin files create unbaselined KDoc findings. If the gate passes, leave baselines unchanged. If it reports only legitimate pre-existing KDoc findings introduced by the master merge, regenerate only the affected project-scoped baseline files, review every new entry, and require zero `NoElseIf` baseline entries. Never hide a new `NoElseIf` violation or unrelated rule failure in a baseline.

Run `./gradlew :wishlist.detekt-rules:test`, `./gradlew detekt`, and `./gradlew build` in that order. Confirm the aggregate Detekt task reaches every Kotlin-bearing project, the custom rules JAR is loaded without a self-dependency, PR #84's Default model files are scanned, and no production anonymous Model remains. Run `git diff --check` and rebuild `ast-index` after the merge.

## Compatibility and rollback

All planned Model work is source-level composition refactoring. It adds no database migration, wire-format change, endpoint change, or persisted-state conversion beyond the already reviewed pull-request features. PR #82's email contracts and PR #81's password-change DTOs, routes, and approval lifecycle must remain byte-compatible with their branch heads. Koin continues to expose the same `UsersModel` interface as one lazy singleton. Existing consumers therefore need no compatibility adapter, dual registration, or deprecated anonymous implementation.

Record each pre-merge branch SHA before mutation. If a branch fails acceptance and cannot be corrected within the stated scope, abort an uncommitted merge. After a committed but unpushed merge, return the branch to its recorded head only through an orchestrator-approved recoverable action. After push, use a normal revert of the branch's merge/update commits rather than rewriting published history; a merge revert must use the pull-request branch as mainline. PR #75 baseline changes are independently reviewable and revertible because they carry no runtime state.

## Acceptance criteria

The task is ready for Architecture and Coding when the following result is implemented on all three pull-request branches:

- Each branch contains `a220b3d` as a merged ancestor through a real merge commit and retains its original feature or quality-gate behavior.
- No pull-request branch contains a production anonymous `object : *Model` implementation under `features/ui`.
- Every production UI Model uses a separate public `Default<InterfaceName>.kt` implementation with outside-world dependencies supplied as constructor `private val`s, and every Plugin binds the interface to the named class as a Koin `single`.
- PR #82 preserves owner-email replacement behavior and the master `DefaultUsersModel` implementation without unnecessary Model changes.
- PR #81's `DefaultUsersModel` receives `PasswordChangeFeature` and owns both password-change delegations; `Plugin.kt` retains only composition wiring plus PR #81's serializers/factories; tests prove concrete-class identity, singleton scope, and exact delegation.
- PR #75 retains its four-rule Detekt policy, scans the new master files, has no `NoElseIf` baseline entries, and changes baselines only when the merged gate proves a legitimate need.
- Relevant focused tests pass on each branch, each branch passes `./gradlew build`, PR #75 additionally passes its custom-rule and aggregate Detekt gates, `git diff --check` passes, and the rebuilt AST index confirms the Model inventory.
- Operator Notes remain unchanged, the task's `PROMPT.md` remains untracked by role commits, no pull-request branch contains orchestration-only task artifacts, and no role pushes or rewrites remote history.

## Operator questions and blockers

No operator question is required. The open-PR set, branch heads, target master commit, MVVM convention, conflict locations, migration ownership, and verification boundaries are all determined by repository and Git evidence. GitHub API access was unavailable during Planning, but the orchestrator's verified PR set and matching local remote-tracking refs are sufficient for this bounded plan.

## Architecture handoff

Architecture should specify the exact PR #81 `DefaultUsersModel` constructor and merged Koin binding, the conflict-resolution invariants for the four users-feature files, the branch-isolated commit sequence, and the focused test matrix above. Architecture should treat PR #82 and PR #75 as merge-and-preservation work, not opportunities for unrelated refactoring. The stop condition is three independently verified, master-synchronized pull-request branches with the new named default-model rule preserved everywhere.
