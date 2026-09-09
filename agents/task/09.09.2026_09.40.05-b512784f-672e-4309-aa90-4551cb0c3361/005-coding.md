Model: GPT-5

Changed files:

- `agents/task/09.09.2026_09.40.05-b512784f-672e-4309-aa90-4551cb0c3361/005-coding.md`
- `features/ui/users/src/commonTest/kotlin/ui/PasswordChangeViewModelTest.kt`

GPT-5 remained the available model for this narrow coding return. No production code or README changed: the confirmed defect was test synchronization, not password-change behavior.

## Investigation and cause

I applied the investigate-first procedure before editing. The persisted XML in 004 was not treated as fresh evidence. Forced reruns of the exact destruction test with `--rerun-tasks` reproduced the failure on both `jsNodeTest` and `jsBrowserTest`: each ran 235 tasks and failed with the fields still equal to `new-password`. The same test passed on JVM and Android.

The ranked hypotheses were: first, an unawaited ViewModel lifecycle completion; second, a StateFlow/password-clearing implementation defect; and third, stale Gradle output. The forced JS reruns disproved the stale-output explanation. Existing successful-submission coverage and the corrected destruction test both clear the same StateFlows, disproving a mutation defect. The first hypothesis was confirmed by the lifecycle implementation: the inherited navigation ViewModel creates a `Dispatchers.Default + SupervisorJob` scope and owns a navigation-destruction collector. `PasswordChangeViewModel` clears the two fields from a normal `Job.invokeOnCompletion` handler. Such a handler runs when the supervisor job has completed, after its child jobs finish.

The old test cancelled that scope and called `advanceUntilIdle()`. Its injected `StandardTestDispatcher` drives the password page work scope, but cannot await the inherited default-dispatcher lifecycle collector or the supervisor job's final completion. JVM and Android happened to complete that asynchronous termination before the assertion; Node and browser did not. This explains the platform split without changing the intended production lifecycle.

## Fix

Following surgical-patch, the destruction test now obtains the ViewModel scope's `Job`, cancels the scope, and awaits `lifecycleJob.join()` before asserting that both plaintext fields are empty. `join()` observes the same completion boundary on every target, so the test verifies the existing `invokeOnCompletion` security cleanup instead of relying on an unrelated scheduler drain. The change is limited to the one test and retains all issue #78 routes, authorization, transport, UI, and cleanup behavior.

## Fresh verification

The exact test was forced fresh before and after the patch on both JS targets:

- `./gradlew :wishlist.features.ui.users:jsNodeTest --tests '*PasswordChangeViewModelTest.destructionClearsInMemoryPasswordInputs' --rerun-tasks --console=plain --warning-mode=none` failed before the patch and passed afterward; the passing rerun executed 235 tasks.
- `./gradlew :wishlist.features.ui.users:jsBrowserTest --tests '*PasswordChangeViewModelTest.destructionClearsInMemoryPasswordInputs' --rerun-tasks --console=plain --warning-mode=none` failed before the patch and passed afterward; the passing rerun executed 235 tasks.
- `./gradlew :wishlist.features.ui.users:jvmTest --tests '*PasswordChangeViewModelTest.destructionClearsInMemoryPasswordInputs' --rerun-tasks --console=plain --warning-mode=none` passed with one focused test and zero failures.
- `./gradlew :wishlist.features.ui.users:testDebugUnitTest --rerun-tasks --console=plain --warning-mode=none` passed with 243 executed tasks. Its fresh XML contains all five `PasswordChangeViewModelTest` cases, including destruction cleanup, with zero failures. `commonTest` resource-generation tasks reporting `NO-SOURCE` do not prevent the compiled shared test from executing on Android Debug.

The fresh all-target UI/users matrix also passed:

`./gradlew :wishlist.features.ui.users:jsNodeTest :wishlist.features.ui.users:jsBrowserTest :wishlist.features.ui.users:jvmTest :wishlist.features.ui.users:testDebugUnitTest --rerun-tasks --console=plain --warning-mode=none`

It completed successfully with 533 executed tasks. Fresh result XML reports `PasswordChangeViewModelTest` as Node 5/5, browser 5/5, JVM 5/5, and Android Debug 5/5.

The broader fresh issue-relevant JVM regression suite passed with 214 executed tasks:

`./gradlew :wishlist.features.auth.common:jvmTest :wishlist.features.auth.server:jvmTest :wishlist.features.deeplinks.server:jvmTest :wishlist.features.email.server:jvmTest :wishlist.features.auth.client:jvmTest :wishlist.features.common.server:jvmTest :wishlist.features.ui.users:jvmTest :wishlist.client:jvmTest --rerun-tasks --console=plain --warning-mode=none`

`ast-index rebuild` completed after the Kotlin test source edit, indexing 1,444 files and 115 modules. Existing Gradle warnings about Android compile SDK compatibility, JS npm configuration timing, and unrelated Kotlin warnings remained non-failing. No further correction beyond this permitted narrow fix is required.
