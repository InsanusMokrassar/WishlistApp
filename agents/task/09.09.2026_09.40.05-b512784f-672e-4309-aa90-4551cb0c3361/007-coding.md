Model: GPT-5

Changed files:

- `agents/task/09.09.2026_09.40.05-b512784f-672e-4309-aa90-4551cb0c3361/007-coding.md`
- `features/ui/users/src/commonTest/kotlin/ui/UserEditViewModelSaveTest.kt`

GPT-5 remained the inherited model and matches the repository's ML-first model choice for this narrow Coding return. I used investigate-first to require fresh, ranked evidence before touching the code, and surgical-patch to keep the correction at the one responsible test lifecycle boundary. No production source, feature behavior, README, or Operator Notes changed.

## Investigation and cause

The verified symptom was an aggregate-only Kotlin/JS failure. A fresh branch run of `:wishlist.features.ui.users:jsTest --rerun-tasks` failed after 237 tasks, not on an assertion, but in Gradle's `SerializableTestResultStore` output handling. The failure followed the expected `JobCancellationException` log from `UserEditViewModelSaveTest.cancellationClearsLoadingWithoutProfileFailure`. The result-store errors were `NullPointerException` in `SerializableTestResultStore$Writer.output`, `IllegalStateException` for an output range with no start, and `ArrayIndexOutOfBoundsException`.

The ranked hypotheses were a product regression, an ordinary deterministic test failure, and an aggregate result-output ordering defect. The product hypothesis was rejected: the named cancellation test and the complete Node and browser suites each passed fresh before the edit. The test itself is unchanged from `master`, and a fresh aggregate `jsTest --rerun-tasks` in a detached master worktree passed under the same build settings. That left the branch-specific timing interaction as the credible mechanism.

`UserEditViewModel` uses an inherited navigation `SupervisorJob` scope plus a test-injected `StandardTestDispatcher` work scope. The old test cancelled the inherited scope and only called `runCurrent()`, which drains the injected test dispatcher but does not await final completion of the inherited lifecycle job and its children. The issue #78 branch adds the eager `canRequestPasswordChangeEmailState.stateIn(workScope, ...)` child. That additional lifecycle work changes the cancellation/output ordering enough for the existing `launchLoggingDropExceptions` cancellation log to reach the JS aggregate result collector after the test's logical completion. The identical cancellation log is expected; its unbounded timing was the defect in the test seam.

The mechanism was proven before editing in a disposable current-branch worktree. Capturing the lifecycle `Job`, cancelling it, and joining it made two independent fresh aggregate `jsTest --rerun-tasks` runs pass, with 236 and 237 executed tasks respectively. This controlled change distinguished the lifecycle boundary from a generic, non-reproducible Gradle race.

## Fix

The cancellation test now retrieves the ViewModel scope `Job`, cancels the scope, and awaits `lifecycleJob.join()` before checking that loading is cleared and no profile failure was published. The join is the precise completion boundary for the cancellation already under test. It preserves the expected cancellation log, verifies the same production cleanup behavior on every target, and prevents late test output from escaping the test result lifetime. No issue #78 authorization, password-email, route, transport, UI, or security behavior was changed.

## Fresh verification

- `./gradlew :wishlist.features.ui.users:jsNodeTest --tests '*UserEditViewModelSaveTest.cancellationClearsLoadingWithoutProfileFailure' --rerun-tasks --console=plain --warning-mode=none` passed. Its fresh XML recorded the named Node test with zero failures and zero errors.
- `./gradlew :wishlist.features.ui.users:jsBrowserTest --tests '*UserEditViewModelSaveTest.cancellationClearsLoadingWithoutProfileFailure' --rerun-tasks --console=plain --warning-mode=none` passed. Its fresh XML recorded the named browser test with zero failures and zero errors.
- `./gradlew :wishlist.features.ui.users:jsTest --rerun-tasks --console=plain --warning-mode=none` passed in 32 seconds with 237 executed tasks. The fresh Node and browser `UserEditViewModelSaveTest` XML reports each contain five tests with zero failures and zero errors.
- `./gradlew :wishlist.features.ui.users:jvmTest --rerun-tasks --console=plain --warning-mode=none` passed in 36 seconds with 92 executed tasks. The fresh JVM `UserEditViewModelSaveTest` report contains five tests with zero failures and zero errors.
- `./gradlew :wishlist.features.ui.users:testDebugUnitTest --rerun-tasks --console=plain --warning-mode=none` passed in 37 seconds with 243 executed tasks. Its fresh Android Debug report contains five tests with zero failures and zero errors.
- `./gradlew build --rerun-tasks --console=plain --warning-mode=none` passed in 3 minutes 27 seconds with 4,530 executed tasks. This is the same repository-wide gate that failed in 006.

`ast-index rebuild` completed after the Kotlin test-source edit, indexing 1,444 files and 115 modules. `git diff --check` passed. Existing non-failing Gradle configuration and unrelated Kotlin warnings remain. No further correction beyond the permitted narrow fix cycle is required.
