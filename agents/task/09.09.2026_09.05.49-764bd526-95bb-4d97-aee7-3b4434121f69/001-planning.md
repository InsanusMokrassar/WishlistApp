Model: GPT-5
Changed files: `agents/task/09.09.2026_09.05.49-764bd526-95bb-4d97-aee7-3b4434121f69/001-planning.md`

# Planning report for GitHub issue #77

## Model choice

The Planning role prefers a high-level model. GPT-5 was selected because this task requires distinguishing a deliberately narrow presentation change from the adjacent owner-email state machine and producing a precise cross-platform handoff without broadening scope.

## Task understanding

GitHub issue #77 requires removing the localized email-confirmation section subtitle from the profile editor on JS, JVM, and Android. The email address field, approval and loading states, save, resend, refresh, validation, error feedback, visibility rules, and every unrelated profile control must retain current behavior. The other two requests in the source prompt, password recovery and changing an existing email, are separate issues and are outside this branch's scope.

The issue is open, contains no comments, and gives complete acceptance criteria. No operator questions remain.

## Investigation

AST-index navigation found the shared `UsersListStrings.emailSectionTitle` declaration at `features/ui/users/src/commonMain/kotlin/UsersListStrings.kt:123`. The only usages are the JS `FieldSet` label at `features/ui/users/src/jsMain/kotlin/ui/UserEditView.kt:149`, the JVM Material `Text` heading at `features/ui/users/src/jvmMain/kotlin/ui/UserEditView.kt:162`, and the Android Material3 `Text` heading at `features/ui/users/src/androidMain/kotlin/ui/UserEditView.kt:163`. No other source or test references the resource.

The JS `FieldSet` component accepts a nullable label and always retains its `.fieldset` container, so removing only the label argument preserves the current grouping and layout while suppressing the rendered heading. JVM and Android render the heading as standalone `Text` calls, which can be removed without restructuring their surrounding owner-email branches. The common ViewModel, model, email clients, navigation, and server APIs do not participate in subtitle rendering and require no changes.

The users UI module has common ViewModel tests but no repository-local render or DOM test harness for these platform views. Existing tests do not assert the subtitle. The feature README documents owner-email visibility and behavior but does not promise or name the subtitle, so no documentation correction is needed for issue #77.

## Acceptance and non-goals

Acceptance requires that neither `Email verification` nor `Подтверждение email` is rendered as an owner-email section heading on any of the three profile editors. The owner-email block must still appear under the same `canManageOwnEmail` gate, and every existing branch and callback inside that block must remain byte-for-byte equivalent apart from the JS wrapper invocation needed to omit its label. The now-unused shared string resource should be removed so the obsolete copy cannot be rendered accidentally.

No new email mutation, password recovery, deeplink handling, authorization rule, server endpoint, dependency, layout redesign, or test infrastructure belongs in this change. Existing email behavior tests should not be rewritten merely because presentation copy disappears. Documentation should remain unchanged unless Architecture finds a direct subtitle claim missed by Planning.

## Risks

The main regression risk is removing the entire JS `FieldSet` while trying to remove its label, which would also alter spacing and grouping. Retaining `FieldSet` with its default null label avoids that change. The other risk is touching the owner-email conditional body or ViewModel while deleting nearby UI code; the implementation should limit edits to the three heading expressions and the orphaned string declaration. Compilation across all three targets is necessary because the platform view implementations use different Compose APIs.

## Final plan for Architecture

Architecture should specify a four-file production patch. In JS, change the email block from a labeled `FieldSet` to an unlabeled `FieldSet` and retain the complete content lambda. In JVM and Android, delete only the standalone email-section `Text` heading. In common strings, delete only `emailSectionTitle` and its KDoc after confirming that the three UI removals leave zero references. No ViewModel, model, email feature, Gradle dependency, test source, or README change should be included.

Verification should run the existing users UI tests and compile every affected target with `./gradlew :wishlist.features.ui.users:jvmTest :wishlist.features.ui.users:jsNodeTest :wishlist.features.ui.users:compileDebugKotlinAndroid :wishlist.features.ui.users:compileReleaseKotlinAndroid --no-daemon --no-parallel --max-workers=1 --console=plain`. After source edits, rebuild the AST index and confirm that `emailSectionTitle`, `Email verification`, and `Подтверждение email` have no remaining production matches. A final diff inspection should prove that all owner-email state branches and callbacks are unchanged.
