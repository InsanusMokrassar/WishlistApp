Model: GPT-6
Changed files: `agents/task/09.09.2026_09.05.49-764bd526-95bb-4d97-aee7-3b4434121f69/002-architecturing.md`

# Architecture report for GitHub issue #77

## Model choice

The Architecture role prefers a high-level model. GPT-6 performed the source investigation and architectural assessment because the task requires a precise cross-platform deletion while preserving the existing owner-email state machine. The Orchestrator instructed this role to write the report directly after stopping a documentation helper before any file was changed, applying the workflow restriction against nested agents.

## Decision and evidence

Issue #77 requires four production-file changes confined to the users UI feature. No API, model, ViewModel, dependency, navigation, or server change is required. The password recovery and existing-email replacement requests remain outside this issue.

The latest Planning report, source prompt, feature README including Operator Notes, and applicable architecture instructions were read. AST-index confirms one `emailSectionTitle` declaration and exactly three usages. The existing cache was accessed with the already authorized sandbox escalation after a sandboxed lookup could not access the index; no search fallback was needed.

The JS owner-email block at `features/ui/users/src/jsMain/kotlin/ui/UserEditView.kt:149` calls `FieldSet(label = UsersListStrings.emailSectionTitle.translation())`. Replace that invocation with `FieldSet` and retain the complete content lambda. The shared component at `features/common/client/src/jsMain/kotlin/ui/components/CalmForms.kt:37` has a nullable label defaulting to null, always renders its `.fieldset` `Div`, and emits a `Label` only when the label is non-null. Retaining the wrapper therefore removes the subtitle while preserving the grouping container. This component is a `Div`, not an HTML `fieldset` or `legend`.

At `features/ui/users/src/jvmMain/kotlin/ui/UserEditView.kt:162`, delete only the single `Text` call rendering `emailSectionTitle`. At `features/ui/users/src/androidMain/kotlin/ui/UserEditView.kt:163`, delete only the four-line `Text` call rendering the same property with Android resources and `titleSmall` typography. Preserve the surrounding `if (canManageOwnEmail)` and every expression inside the remaining branch.

At `features/ui/users/src/commonMain/kotlin/UsersListStrings.kt:122`, delete the heading KDoc and `emailSectionTitle` property, including both `Email verification` and `Подтверждение email`. Keep `emailLabel` and every other localized resource unchanged. No import becomes unused solely because of these deletions: the views continue to render other text and use their existing material themes.

The intended behavior is that the heading disappears in both supported languages on JS, JVM, and Android. Email labels, existing input values, hints, validation, permission and visibility gates, enabled states, read-only states, save, resend, refresh, loading, and result feedback retain their current expressions and handlers. The layout naturally loses the deleted heading; no compensating spacer or layout redesign is requested.

## Research and adaptation

Current primary-source research was performed on 9 September 2026. The [W3C WAI guidance on grouping controls](https://www.w3.org/WAI/tutorials/forms/grouping/) explains that individual control labels should remain self-explanatory even when a group heading is not announced. Applied here, the independent `Email address` label remains on every email field; JS retains the `settings-email` identifier and its label association. The removed wrapper label has no `forId` and provides no input association. Introducing semantic groups, replacement headings, or hidden copy would broaden the requested deletion and is unnecessary for preserving the existing labels.

The [Kotlin Multiplatform testing tutorial](https://kotlinlang.org/docs/multiplatform/multiplatform-run-tests.html) describes common tests executed through platform test configurations. Applied to the existing repository, run the users feature's JVM and JS Node tests and compile Android debug and release variants. Repository templates already configure JVM JUnit, JS Node tests, and Android library variants. No new test target, dependency, framework, or toolchain migration is needed.

## Test specifications

No function, endpoint, or class is added. The three platform `UserEditView` rendering implementations and the removed common resource are covered by a finite source-change contract, target compilation, and existing behavior tests. Implement no new persistent tests or UI test infrastructure for this deletion.

First, use commit `edc2cf94741045101eb405351c9cc52f36009d9f` as the before-change baseline. For each of the four production paths, load the baseline content and apply exactly the replacement or deletion described above. Require each original heading expression or resource block to occur exactly once before transformation. Assert that the resulting expected content equals the complete edited file byte for byte. Apart from step reports, the changed paths must be exactly those four files. This automated source check detects accidental removal of the JS wrapper, altered handlers, modified state gates, collateral formatting, and unrelated changes. It is a task verification assertion, not a new maintained test that mirrors the implementation.

The source contract must preserve the email branch for both values of `canManageOwnEmail`. Within the visible branch, verify unchanged code for loading; missing private profile with and without a load failure; missing email with editable input and save action; pending email with read-only input and resend; approved email with read-only input and no resend; busy or mutation-disabled controls; invalid-email, save, and load errors; refresh availability; and every verification-result message. For each platform and locale, the expected new outcome is absence of the obsolete heading expression and resource. The exact-content comparison proves that every remaining expression for these states is unchanged; it does not claim that screenshots or platform rendering tests have been executed.

After source editing, run `ast-index rebuild`. Require zero definitions and zero usages of `emailSectionTitle`. Search production source for both obsolete localized literals and require zero matches, excluding historical task reports. AST-index should be used for indexed source search; a literal scan may supplement the index if the index does not retain string contents. Confirm that `emailLabel`, JS `settings-email`, and the independent label expressions remain unchanged through the exact-content assertion.

Run `./gradlew :wishlist.features.ui.users:jvmTest :wishlist.features.ui.users:jsNodeTest :wishlist.features.ui.users:compileDebugKotlinAndroid :wishlist.features.ui.users:compileReleaseKotlinAndroid --no-daemon --no-parallel --max-workers=1 --console=plain` and require successful completion. JVM and JS test tasks compile the respective production implementations; Android compilation validates both configured build variants against the shared resource deletion.

The existing `UserEditViewModelEmailTest` suite must remain unchanged and pass on JVM and JS. Its cases cover saving a missing owner email before verification, failed or uncertain saves without verification requests, pending-email retry without another save, identity loss and stale-operation suppression, private-profile and capability failures, and disabled capability rejecting mutations. The existing `UserEditViewModelSaveTest` suite must also pass unchanged, preserving adjacent administrator save behavior. These tests establish the current state-machine behavior while the source contract proves that the views continue to consume and invoke the same state and handlers.

Finish with `git diff --check` and inspection of the complete production diff. No new functionality requires an external service, device-only behavior, or unavailable rendering harness. Automated static assertions, compilation, and existing tests cover this narrow deletion; there is no untestable-functionality blocker and no operator question before Coding. Architecture has inspected sources and specified these checks, but has not executed the proposed post-change tests.

## README updates

No README change is required. The users feature README documents owner-email visibility, state, and controls, but does not promise the removed subtitle. The existing architecture description remains accurate after the four deletions. Keep the README and its Operator Notes unchanged.

## Handoff

Coding can proceed with the exact four-file patch and the verification specifications above. Preserve the current branch scope for issue #77. Completion requires removed heading calls and localized property, unchanged remaining production content, a rebuilt AST index, successful target checks, and a reviewed diff.
