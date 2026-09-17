Model: OpenAI Sol
Changed files: agents/task/17.09.2026_05.46.44-9e061fe4-464a-427b-b509-69385818b932/002-architecturing.md

# Architecture report for open pull request MVVM synchronization

## Model choice

OpenAI Sol is a high-level model and is the preferred available class for Architecturing under `agents/SHORTCUTS.md` and `agents/MODELS.md`. This task needs exact three-way conflict design across three independent branches, preservation of a security-sensitive password-change flow, and a reversible Git execution plan. The explicit no-subagent instruction also means the assigned Sol model performs the investigation and writes this report directly. Caveman full was limited to internal working notes; this report and its commit use normal prose.

## Decision

Use three temporary linked worktrees and one real merge commit per pull-request branch. Keep the main worktree on `chore/open-pr-mvvm-sync-20260917`, where the orchestration reports remain. PR #82 and PR #75 are merge-and-preservation updates: neither branch requires a branch-specific Model edit after receiving master. PR #81 requires one narrow MVVM repair in the merge result: extend master's named `DefaultUsersModel` with `PasswordChangeFeature`, move the two password-change delegations out of the branch's anonymous `UsersModel`, and retain the branch's serializers, ViewModel factory, contracts, and tests.

Planning identified the correct fixed branch set, heads, merge base behavior, four PR #81 conflict paths, and sole model violation. No Planning conclusion needs reversal. Architecture adds the missing exact constructor, imports, conflict resolutions, test cases, worktree isolation, and commit/gate sequence. One wording refinement is important for PR #75: files arriving from master are new code on that branch, not “pre-existing” findings. Baseline changes are allowed only when a fresh aggregate Detekt run proves an accepted KDoc finding in the merged tree; they are not inferred from history.

No source, test, feature README, branch pointer, or main-worktree checkout is changed by this role.

## Research and adaptation

Current primary-source research was performed on 17 September 2026.

- [Koin definition documentation](https://insert-koin.io/docs/reference/koin-core/definitions/) defines `single` as one shared container-managed instance and distinguishes it from `factory`. Retain this repository's explicit `single<UsersModel> { DefaultUsersModel(...) }` interface binding, and test both concrete identity and repeated-resolution identity. The newer compiler-plugin DSL is not adopted because changing DI style is unrelated and the repository consistently uses runtime module DSL.
- [Kotlin class documentation](https://kotlinlang.org/docs/classes.html) describes primary-constructor `val` parameters as stored properties usable by member functions, while [Kotlin visibility documentation](https://kotlinlang.org/docs/visibility-modifiers.html) defines `private` member visibility. Apply the repository's stricter MVVM rule directly: every outside-world dependency, including `PasswordChangeFeature`, is a primary-constructor `private val`; the named class does not access Koin.
- [Git worktree documentation](https://git-scm.com/docs/git-worktree) states that linked worktrees share the repository while keeping worktree-specific `HEAD` and index state. Use one linked worktree per PR so merge state, generated build output, and branch commits cannot displace the coordination branch or stage its task files.
- [Git merge documentation](https://git-scm.com/docs/git-merge) documents `--no-ff` merge commits and warns that aborting around pre-existing changes may not reconstruct state. Require clean worktrees, record each original head, use `--no-ff --no-commit`, and use `git merge --abort` only before the merge commit. This preserves review history without rebase or force-push.

These practices are adapted to existing repository conventions rather than used to introduce a new framework, DI syntax, branch policy, or source layout.

## Branch-isolated execution design

The Coding role should first verify that the main worktree is still on `chore/open-pr-mvvm-sync-20260917`, that its only permitted untracked entry is the root-owned current-task `PROMPT.md`, that local `master` and `origin/master` still equal `a220b3d`, and that the three local branch heads still equal the recorded remote-tracking heads: `077fcd2`, `ffd73fd`, and `5c5d0d4`. A changed remote head or a dirty target branch is a stop condition, not authority to overwrite work.

Create one temporary parent with `mktemp -d /tmp/wishlist-open-pr-sync.XXXXXX`, then add existing branches at child paths `pr82`, `pr81`, and `pr75` with `git worktree add`. Do not switch the main worktree. Process the linked worktrees serially to avoid shared Gradle-daemon and machine-resource contention. Each linked worktree gets a distinct temporary AST cache, such as `<temporary-parent>/cache-pr81`, through `XDG_CACHE_HOME`; run `ast-index rebuild` after the merge changes Kotlin sources and again after any subsequent Kotlin correction. Markdown-only changes on the coordination branch do not require a rebuild.

Within each worktree, record `git rev-parse HEAD`, require a clean status, and run `git merge --no-ff --no-commit a220b3d`. Resolve and stage the complete merge result, run `git diff --cached --check`, rebuild and inspect the AST index, run the branch's focused gates, then run the full gate before committing. The final merge commit must have the original PR head as first parent and `a220b3d` as second parent. Its normal-prose message must describe the branch result and end with `Co-Authored-By: Claude <noreply@anthropic.com>`. Do not create a preliminary merge commit followed by a fix commit: conflict resolution and required MVVM adaptation belong in the same reviewable merge commit.

After a successful commit, require `git merge-base --is-ancestor a220b3d HEAD`, `git rev-parse HEAD^2` equal to `a220b3d`, `git show --check --oneline HEAD` to pass, and no current-task `agents/task/17.09.2026_05.46.44-9e061fe4-464a-427b-b509-69385818b932` path in the PR commit. Build outputs remain ignored and local to the linked worktree. Remove linked worktrees with `git worktree remove` only after recording their committed heads; never delete a worktree containing uncommitted changes. Pushing remains the Orchestrator's responsibility.

## PR #82: `feat/issue-79-user-email-change`

Merge `a220b3d` into recorded head `077fcd2`. No textual conflict is predicted and no new Model method or dependency is required. The resulting production model must be master's public `DefaultUsersModel` in its separate commonMain file, with the same nine constructor dependencies, while `Plugin.kt` retains the named-argument `single<UsersModel>` binding. No anonymous production `UsersModel` may reappear.

Semantically review the auto-merged users README, `UsersModel` contract, `UserEditViewModel`, all three `UserEditView` implementations, and their tests. Preserve PR #82's independent saved-email/replacement-draft state, storage while SMTP is disabled, exact owner/live-target privacy gates, retarget cancellation, ambiguous PUT behavior, later reconciliation, and existing render coverage. Preserve master's concrete model identity, singleton, every existing delegation, and file-service tests. Do not move editor state into the Model and do not alter the model constructor merely because the branch changed email-editor behavior.

The README merge must retain PR #82's owner-email replacement architecture and master's `DefaultUsersModel` implementation note. Preserve `## Operator Notes` byte-for-byte. No additional README design text is needed beyond combining those already-approved statements.

### PR #82 test specification and gates

Existing tests are the regression specification; no new test duplicate is required for unchanged Model behavior.

- `UsersModelTest` must resolve `UsersModel` as `DefaultUsersModel`, resolve the same instance twice, and keep all users/auth/email/admin/files/roles delegations and reactive flows green.
- `UsersModelFileTest` must keep upload/download behavior through the named class.
- `UserEditViewModelEmailTest` must retain the saved-versus-draft matrix, malformed input, replacement/approval reset, disabled-SMTP storage, exact resend recipient, stale owner/target suppression, cancellation, refresh, and uncertain storage outcomes.
- `UserEditEmailRenderTest` must retain the production desktop private-semantics and immediate live-retarget privacy checks.
- `KtorEmailFeatureTest` and `EmailRoutingsConfiguratorTest` must retain exact PUT/POST payloads, bearer ownership, and HTTP 409 handling.

Run the branch's four documented focused commands exactly:

```text
./gradlew --no-parallel :wishlist.features.ui.users:jvmTest --tests '*UserEditViewModelEmailTest'
./gradlew --no-parallel :wishlist.features.ui.users:jvmTest --tests '*UserEditEmailRenderTest'
./gradlew --no-parallel :wishlist.features.email.client:jvmTest --tests '*KtorEmailFeatureTest'
./gradlew --no-parallel :wishlist.features.email.server:jvmTest --tests '*EmailRoutingsConfiguratorTest'
```

Then run `./gradlew --no-parallel :wishlist.features.ui.users:build :wishlist.features.email.client:build :wishlist.features.email.server:build`, followed by `./gradlew --no-parallel build`. Rebuild the AST index and require `DefaultUsersModel` to be the only production `UsersModel` implementation and `Plugin.kt` to contain no anonymous Model body.

## PR #81: `fix/issue-78-email-authorized-password-change`

Merge `a220b3d` into recorded head `ffd73fd`. Resolve the four predicted conflicts by combining both branches. Auto-merged files still require semantic review, especially `UsersModel.kt`, the platform views, client navigation ownership, Auth/Email/DeepLinks plugins, and their security and lifecycle tests.

### Exact named-model implementation

Keep master's file `features/ui/users/src/commonMain/kotlin/ui/DefaultUsersModel.kt`. Add these imports to that file and nowhere merely for delegation convenience:

```kotlin
import dev.inmo.wishlist.features.auth.client.PasswordChangeFeature
import dev.inmo.wishlist.features.auth.common.models.CompletePasswordChangeRequest
import dev.inmo.wishlist.features.auth.common.models.PasswordChangeEmailRequestResult
import dev.inmo.wishlist.features.auth.common.models.PasswordChangeResult
```

The complete constructor shape must be master's existing dependency list plus one private password-change dependency in this order:

```kotlin
class DefaultUsersModel(
    private val feature: UsersFeature,
    private val authFeature: ClientAuthFeature,
    private val emailFeature: EmailFeature,
    private val passwordChangeFeature: PasswordChangeFeature,
    private val meState: StateFlow<AuthFeatureUser?>,
    private val adminFeature: AdminFeature,
    private val filesService: FilesClientService,
    private val scope: CoroutineScope,
    private val credentialsStorage: AuthCredentialsStorage,
    private val rolesFeature: RolesFeature,
) : UsersModel
```

Add `@param passwordChangeFeature` to the class KDoc. Keep every master property and method body unchanged except for adding the two direct delegations:

```kotlin
override suspend fun requestPasswordChangeEmail(
    expectedEmail: Email,
): PasswordChangeEmailRequestResult? =
    passwordChangeFeature.requestPasswordChangeEmail(expectedEmail)

override suspend fun completePasswordChange(
    request: CompletePasswordChangeRequest,
): PasswordChangeResult? = passwordChangeFeature.completePasswordChange(request)
```

These methods must pass the original object/value unchanged, perform exactly one feature call, preserve nullable results, and propagate exceptions and cancellation. They must not infer a user from `meState`, substitute an email, catch an uncertain result, retry, or resolve a dependency from Koin.

### Exact PR #81 conflict resolutions

For `features/ui/users/src/commonMain/kotlin/Plugin.kt`, start from master's composition-only version. Retain PR #81's four polymorphic registrations for `PasswordChangeViewConfig.Pending` and `.Completed` under both `Any` and `ViewConfig`, and retain `factory { PasswordChangeViewModel(...) }`. Import `PasswordChangeViewConfig` and `PasswordChangeViewModel`; import no DTO, flow operator, `MPPFile`, admin constant, file constant, or feature type used only by the deleted anonymous body. Remove PR #81's `@OptIn(ExperimentalCoroutinesApi::class)` from `setupDI`, because flow construction remains in the named class. The binding must be exactly:

```kotlin
single<UsersModel> {
    DefaultUsersModel(
        feature = get(),
        authFeature = get(),
        emailFeature = get(),
        passwordChangeFeature = get(),
        meState = meStateFlow,
        adminFeature = get(),
        filesService = get(),
        scope = get(),
        credentialsStorage = get(),
        rolesFeature = get(),
    )
}
```

The explicit `meState = meStateFlow` access is required; do not replace it with an unqualified `get<StateFlow<...>>()`. `Plugin.kt` remains responsible only for serializers, factories, and DI construction.

For `features/ui/users/src/commonTest/kotlin/UsersModelTest.kt`, use master's comprehensive `runTest`/`backgroundScope` test structure, concrete-class assertion, repeated-resolution assertion, reactive state checks, and every existing delegation assertion. Do not restore PR #81's smaller standalone `startModelKoin` test or its manually managed `SupervisorJob`. Add a recording `PasswordChangeFeature`, bind that exact double in Koin, pass a harmless recording double from the `modelForFileTests` helper, and add exact value assertions for both new methods. Compare the complete `CompletePasswordChangeRequest`, including user id, approval id, and password, rather than checking only its user id.

For `features/ui/users/build.gradle`, retain the union of both branches: `unitTests.returnDefaultValues = true`, PR #81's `unitTests.includeAndroidResources = true`, master's `jvmTest` MockEngine dependency, PR #81's `jsTest` `jsdom` dependency, PR #81's Android unit-test Robolectric and Compose UI test dependencies, and both JS Node exclusions for browser-only view tests. Do not duplicate source-set blocks or drop a dependency based only on which side introduced it.

For `features/ui/users/README.md`, preserve PR #81's four screen families, password-change contracts, navigation ownership, security/cancellation rules, and platform coverage. Add master's named-model architecture statement and document the complete constructor dependency set with `PasswordChangeFeature` included. Change statements saying reactive model flows are built in `Plugin` to say they are built in `DefaultUsersModel`. Preserve `## Operator Notes` byte-for-byte.

`UsersModel.kt` should merge automatically. Confirm that it retains PR #81's three Auth DTO imports and both nullable method declarations while keeping all master KDoc and other members. Do not move transport DTOs or behavior into the plugin.

### PR #81 test specification and gates

For the modified class and binding, implement these automated cases:

- Resolve `UsersModel`; assert `DefaultUsersModel`; resolve again and assert the same object. Omit the `PasswordChangeFeature` binding in a separate graph only if an existing Koin graph-validation pattern supports it; otherwise successful construction with the exact double is sufficient and avoids testing Koin internals.
- Call `requestPasswordChangeEmail(expectedEmail)` with a non-default address. Assert one call with that exact `Email` and the same `PasswordChangeEmailRequestResult` instance/value returned by the double.
- Call `completePasswordChange(request)` with distinct user id, approval id, and password. Assert one call with the exact request and the same `PasswordChangeResult` returned by the double.
- Configure each recording method to return `null` and assert the Model returns `null` without a second call. Configure one call to throw a sentinel exception or cancellation and assert propagation with call count one. This proves nullable and no-retry behavior without retesting transport logic.
- Retain every master's `UsersModelTest` assertion for authorization state, public/private profile calls, email verification, admin mutations, roles, and files. Retain `UsersModelFileTest` through the updated helper.
- Retain serializer round trips for Pending and Completed; ViewModel tests for password policy, mismatch, busy state, terminal invalid approval, success, cancellation, and no automatic retry; owner editor tests for approved-owner visibility and negative/stale cases; browser/JVM/Android view tests; Ktor exact-request and no-retry tests; Auth route and Email/DeepLinks issuance, validation, consumption, cleanup, lock-order, security-header, and credential-invalidation tests.

Run `./gradlew --no-daemon :wishlist.features.ui.users:jvmTest --tests '*UsersModelTest*' --rerun-tasks --console=plain` first. Then run the retained cross-platform gate `./gradlew --no-daemon :wishlist.client:jvmTest :wishlist.client:jsBrowserTest :wishlist.features.ui.users:jvmTest :wishlist.features.ui.users:jsBrowserTest --rerun-tasks --console=plain`. Run the owning feature JVM suites with this serial command:

```text
./gradlew --no-daemon --no-parallel \
  :wishlist.features.auth.client:jvmTest \
  :wishlist.features.auth.common:jvmTest \
  :wishlist.features.auth.server:jvmTest \
  :wishlist.features.email.server:jvmTest \
  :wishlist.features.deeplinks.server:jvmTest \
  :wishlist.features.common.server:jvmTest \
  --rerun-tasks --console=plain
```

Finally run `./gradlew --no-parallel build`.

After the final Kotlin edit, rebuild the branch AST index. `ast-index refs UsersModel` must show the production definition and the named implementation/binding plus test doubles only; `ast-index refs DefaultUsersModel` must show the class, binding, and tests; a Kotlin structural search for `object : $MODEL { $$$BODY }` must find no production UI Model implementation. Review the result rather than treating raw match count as proof, because test doubles are permitted.

## PR #75: `fix/issue-72-detekt-conventions`

Merge `a220b3d` into recorded head `5c5d0d4`. Preserve the branch's Detekt plugin/version catalog wiring, `NoElseIf` rule and service provider, recursive Kotlin source configuration, aggregate `detekt`/`detektBaseline` tasks, producer self-dependency exclusion, four active rules, project-scoped baselines, and verification-before-build policy. Preserve master's unrelated version-catalog and settings additions in the automatic `gradle/libs.versions.toml` and `settings.gradle` merge.

This branch adds no Model API or implementation. Master supplies the named default implementations and bindings. Do not edit Model sources merely to make lint output change. Rebuild the AST index after the merge and verify the production model inventory before running quality gates.

Run `./gradlew --no-parallel :wishlist.detekt-rules:test --rerun-tasks`, then the blocking `./gradlew --no-parallel detekt`. If Detekt passes, do not touch a baseline. If Detekt reports accepted KDoc findings caused by master files, run only the owning project's `detektBaseline` task, review the XML delta against the reported findings, and rerun aggregate Detekt. Never baseline `NoElseIf`, an unexpected rule, or a newly introduced defect that should be fixed in source. Any required source correction outside the fixed merge/MVVM scope is a stop condition for Orchestrator review; this architecture does not authorize broad lint cleanup. Only after Detekt passes, run `./gradlew --no-parallel build`.

Acceptance for the quality branch requires the custom rule tests, aggregate Detekt, and full build to pass; every Kotlin-bearing project must participate except intentional `NO-SOURCE` projects; the rules producer must not depend on its own JAR; baseline XML must contain zero `NoElseIf` and zero manual-suppression entries; and AST inspection must show all master Default Model files scanned with no production anonymous UI Model.

## README updates

Only merged branch READMEs change; this Architecturing role does not edit them.

- PR #82: combine the branch's owner-email replacement notes with master's existing `DefaultUsersModel` implementation note. Do not invent a new behavior or change Operator Notes.
- PR #81: add the named `DefaultUsersModel` statement, list `PasswordChangeFeature` among its private constructor dependencies, and move reactive-flow ownership wording from `Plugin` to `DefaultUsersModel`. Retain all password approval, navigation, transport, and security notes. Do not change Operator Notes.
- PR #75: no feature README delta is designed. Tooling documentation arrives from the branch/master merge only.

## Compatibility and rollback

The PR #81 change is source-level composition only. `UsersModel`, Auth password-change DTOs, HTTP paths, serializers, nullable outcomes, security headers, approval storage, navigation configs, and password policy remain unchanged. Koin still exposes one lazy `UsersModel` singleton, so existing consumers require no adapter. PR #82 and PR #75 introduce no extra runtime contract beyond their already-reviewed branch changes and master.

Each original branch SHA is the recovery anchor. Before a merge commit, use `git merge --abort` in that clean linked worktree. After a local unpushed merge commit, do not reset or rewrite without Orchestrator approval; the recorded first-parent SHA permits an exact recovery decision. After push, rollback uses a normal revert of the merge commit with the PR branch as mainline, followed by ordinary corrective commits if needed. Detekt baseline changes, if proven necessary, remain in the single PR #75 merge commit and are therefore independently inspectable in that merge diff.

## Stop conditions and acceptance

Stop the affected branch without committing if its recorded head no longer matches the remote-tracking head, master is not `a220b3d`, a target worktree is dirty, a merge introduces an unplanned security or wire-contract conflict, Operator Notes differ, a required test is not automatable, Detekt would require hiding `NoElseIf` or an unrelated defect, any focused/full gate fails after in-scope correction, or the current task folder appears in a PR commit. Do not substitute another open PR, rebase, squash, force-push, or merge one PR branch into another.

Completion requires three independent merge commits with `a220b3d` as second parent; all original PR behavior retained; PR #81 using the exact named-model constructor, direct delegations, and Koin binding above; PR #82 retaining its email replacement/privacy behavior; PR #75 retaining its quality policy with clean Detekt; all focused and full gates green; final AST indexes current; `git diff` checks clean; the coordination branch unchanged except for role reports; `PROMPT.md` untracked and unstaged; and no push by an individual role.

No functionality in this plan is inherently untestable at the required abstraction boundary. Live SMTP, physical Android IME behavior, graphical browser behavior outside the existing JSDOM coverage, and process-crash durability remain the branches' documented external limits; the merge introduces no new claim in those areas. No operator question or blocker remains.
