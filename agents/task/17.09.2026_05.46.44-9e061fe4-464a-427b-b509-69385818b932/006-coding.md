Model: OpenAI Terra (ML)
Changed files: agents/task/17.09.2026_05.46.44-9e061fe4-464a-427b-b509-69385818b932/006-coding.md

# Coding correction report — PR #81 validation findings closed

## Model choice

OpenAI Terra is the Medium Level (ML) choice preferred for Coding. The correction required narrow source and browser-test changes against a validated merge, exact preservation of password-change behavior, AST-backed model checks, and a complete Gradle build. Caveman full was used only for internal notes; this report and the correction commit use normal prose.

## Finding closure

Only PR #81 was changed. Its prior verified merge head was `f549be86c70c9d75bf2710f9509542678268433f`; the new local head is the ordinary follow-up commit `60aa21a412c8cae7a1f44617a95db9126192aac0`. The correction commit has `f549be86c70c9d75bf2710f9509542678268433f` as its sole parent, retains `a220b3d2f9f4224e09880022e3051c374bd69a6a` as an ancestor through the original merge, and does not amend, rebase, reset, or otherwise rewrite that merge.

The JavaScript `UserEditView` no longer restores master’s removed email-section `FieldSet` label. The now-unused `UsersListStrings.emailSectionTitle` resource was removed. `UserEditViewBrowserTest` now proves private-section absence through the stable `#settings-email` control and approved-owner state through that control plus the password-change action, while retaining the action’s visibility, busy-disabled, exact-request, and delivery-failure assertions. The users README now correctly says `meState.mapLatest { ... }.stateIn(...)` is built in `DefaultUsersModel`, not `Plugin.kt`. The `## Operator Notes` block hash remains `71220d017d4abfc545b86bf4201c820e10be52373482feffea69c4831436e3bc` before and after the README edit.

No password-change Model, Koin binding, transport, approval, security, or cancellation behavior changed. `DefaultUsersModel` remains the named production implementation and `Plugin.kt` remains its composition root.

## Verification

All correction gates passed:

- `./gradlew --no-daemon --no-parallel :wishlist.features.ui.users:jsBrowserTest --tests '*UserEditViewBrowserTest*' --rerun-tasks --console=plain` — PASS, 234 tasks, 1m12s.
- `./gradlew --no-daemon --no-parallel :wishlist.features.ui.users:jsBrowserTest --rerun-tasks --console=plain` — PASS, 235 tasks, 25s.
- `./gradlew --no-daemon --no-parallel :wishlist.features.ui.users:jvmTest --rerun-tasks --console=plain` — PASS, 92 tasks, 25s.
- `./gradlew --no-parallel build --console=plain` — PASS, 4,608 actionable tasks: 4,342 executed and 266 up-to-date, 5m15s.

`git diff --check`, staged-diff checking, `git show --check`, and `git diff --check HEAD^ HEAD` passed. The correction commit changes exactly four allowed users-feature files and contains no current-task artifacts. `ast-index rebuild` indexed 847 files after the Kotlin source change. `ast-index refs emailSectionTitle` reports no references; `ast-index refs DefaultUsersModel` reports its one production definition, the explicit Plugin construction, and focused tests; `ast-index refs UsersModel` shows the named implementation/binding plus permitted consumers and test doubles. The host lacks `ast-grep`, so the fallback production-source scan found no anonymous `object : *Model` implementation under `features/ui`.

## Branch topology and unchanged targets

PR #81 now consists of its verified two-parent merge `f549be86c70c9d75bf2710f9509542678268433f` followed by correction `60aa21a412c8cae7a1f44617a95db9126192aac0`. The original merge still has first parent `ffd73fd5698b2c1f0c80b5948d38761787883b4b` and second parent `a220b3d2f9f4224e09880022e3051c374bd69a6a`.

PR #82 remains unchanged at `06cf01ea39a411141359291cd918e8775a272ea3`. PR #75 remains unchanged at `1cbab45e1579cd8168bd3c88e34d15278ac8397f`. No branch was pushed. The clean temporary PR #81 worktree was isolated under `/tmp/wishlist-pr81-correction.mpPQLt/pr81`; its ignored `local.properties` symlink was never staged.

## Compatibility and rollback

The correction removes only a presentation subtitle and its dead localized string, retargets browser proof to stable controls, and aligns documentation with the implemented ownership. Public APIs, persisted data, routes, serializers, DI bindings, and security behavior remain unchanged. Before push, rollback is an ordinary `git revert 60aa21a`; after push, use the same normal revert rather than rewriting the branch. No blocker remains.
