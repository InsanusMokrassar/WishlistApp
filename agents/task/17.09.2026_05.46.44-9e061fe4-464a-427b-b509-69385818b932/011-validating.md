Model: OpenAI Sol (HL)
Changed files: agents/task/17.09.2026_05.46.44-9e061fe4-464a-427b-b509-69385818b932/011-validating.md

# Final validation report

## Model choice

OpenAI Sol is a high-level model and is the preferred available class for Validating under `agents/SHORTCUTS.md` and `agents/MODELS.md`. This review required distinguishing a correct production repair from an ineffective DOM regression assertion, then independently rechecking topology, prior finding closure, MVVM ownership, and password-change security boundaries. The explicit no-subagent instruction prevented low-level Markdown transcription, so the assigned Sol model wrote this report directly in normal prose. Caveman full was limited to internal working notes.

## Final result: FAIL

The production `FieldSet` restoration is correct and all findings predating `008-validating.md` remain closed. The new browser selector, however, does not prove that the restored outer grouping exists. Finding counts are zero Critical, zero High, one Medium, and zero Low. The Medium finding does not concern authentication, permissions, or data integrity.

Under the Orchestrator's Medium Findings Decision Rule, one non-sensitive Medium finding does not mandate a loop by count alone. The prompt for this validation explicitly requires a meaningful DOM regression assertion, so the recommendation is a final test-only PR #81 correction before any push. No repeat-problem escalation applies: the `008-validating.md` production-wrapper finding is closed, while the present finding is a new proof defect in the added assertion.

## Finding

### Medium — the new selector is satisfied by the email field's own `.fieldset`

`features/ui/users/src/jsTest/kotlin/ui/UserEditViewBrowserTest.kt:123` asserts `querySelector(".fieldset #settings-email")`. That selector requires only some `.fieldset` ancestor. Independently of the restored outer wrapper, `CalmTextField` always renders its own `Div` with the `.fieldset` class around its input at `features/common/client/src/jsMain/kotlin/ui/components/CalmForms.kt:60`–`81`, specifically the wrapper at line 70 and the input at lines 72–79. Therefore the assertion would also pass against the prior `60aa21a` DOM, where the outer email-section `FieldSet` was absent.

The test continues to prove approved-owner visibility, busy disabling, exact email forwarding, and delivery-failure feedback at `UserEditViewBrowserTest.kt:122`–`137`; the denied-state absence proof also remains at lines 74–79. Only the structural regression proof is ineffective. Replace the selector with one that distinguishes the outer group from `CalmTextField`'s inner wrapper, for example `.fieldset > .fieldset > #settings-email`, or explicitly assert two consecutive `.fieldset` ancestors of `#settings-email`.

## Production correction and previous-finding closure

The second correction itself is exact and narrow. `features/ui/users/src/jsMain/kotlin/ui/UserEditView.kt:152`–`280` now encloses the complete owner-email branch in an unlabeled `FieldSet`: the wrapper opens at line 153, closes at line 279 after the password-change result flow, and leaves the root-only fields outside. This matches master's grouping boundary. Commit `37f2a2fb5fe358c42e13576712d3586671a777d6` adds only those two braces in production and changes only the single browser assertion in test.

The removed subtitle stays removed on every platform. A fresh 847-file `ast-index` rebuild reports no definition or reference for `emailSectionTitle`, and the correction restores neither a label argument nor a localized resource. The earlier README finding also remains closed: `features/ui/users/README.md:63` and line 67 assign the reactive flows to `DefaultUsersModel` and describe `Plugin.kt` as the composition root. The Operator Notes block at lines 3–6 is byte-identical at `60aa21a` and `37f2a2f`.

## MVVM, security, and compatibility audit

The correction is byte-identical to `60aa21a` for the common model, Plugin, auth, email, deeplink, route, serializer, approval, credential, and persistence surfaces. `features/ui/users/src/commonMain/kotlin/ui/DefaultUsersModel.kt:47`–`58` remains the public named implementation with `PasswordChangeFeature` in its primary constructor. Lines 105–114 remain direct, exact, nullable one-call delegations with no catch or retry, preserving both ordinary exceptions and cancellation. `features/ui/users/src/commonMain/kotlin/Plugin.kt:54`–`67` retains the single interface binding with `passwordChangeFeature = get()` and `meState = meStateFlow`. A production scan finds no anonymous `object : *Model` implementation.

The transport boundary remains fail-closed and cancellation-safe. `features/auth/client/src/commonMain/kotlin/KtorPasswordChangeFeature.kt:58`–`68` posts completion with `AuthCircuitBreaker`, uses the trusted browser completion URL when supplied, bypasses the default saved server URL for that case, and performs no retry. Lines 70–80 rethrow `CancellationException` and map only other transport/decoding failures to null. `features/ui/users/src/commonMain/kotlin/ui/PasswordChangeViewModel.kt:145`–`188` retains policy checks, one busy slot, exact request construction, a post-suspension activity check, credential clearing only after confirmed success, and no automatic retry. No new security, authorization, cancellation, compatibility, or data-integrity regression was found.

## Topology, scope, and role-compliance audit

PR #81 head `37f2a2fb5fe358c42e13576712d3586671a777d6` has sole parent `60aa21a412c8cae7a1f44617a95db9126192aac0`; that commit has sole parent verified merge `f549be86c70c9d75bf2710f9509542678268433f`. The merge retains original PR head `ffd73fd5698b2c1f0c80b5948d38761787883b4b` first and master `a220b3d2f9f4224e09880022e3051c374bd69a6a` second. Master and the verified merge remain ancestors. The correction subject is normal prose and its required `Co-Authored-By: Claude <noreply@anthropic.com>` footer is present.

PR #82 remains exactly `06cf01ea39a411141359291cd918e8775a272ea3`, and PR #75 remains exactly `1cbab45e1579cd8168bd3c88e34d15278ac8397f`. Their remote-tracking branches still point to their original pre-merge heads, so none of the local targets was pushed. The PR #81 correction contains no current-task artifact, no unresolved index entry, no conflict marker, and no whitespace error. The detached test worktree remained clean. The coordination branch remained selected, and the root-owned task `PROMPT.md` remained untracked and unstaged.

Coding used the preferred ML model, changed only the two intended PR files, preserved the production/security scope, committed its report separately, and did not push or rewrite history. Verification used the preferred ML model, tested the exact detached head, committed only its report, and did not mutate the target. Both roles incorrectly characterized `.fieldset #settings-email` as proof of the outer wrapper; that shared miss is the sole role-compliance defect in the final correction cycle.

## Verification evidence and residual risks

Independent focused execution of `./gradlew --no-daemon --no-parallel :wishlist.features.ui.users:jsBrowserTest --tests '*UserEditViewBrowserTest*' --rerun-tasks --console=plain` passed with 234 executed tasks. Verification also records a passing 235-task complete users JS browser suite and a passing 4,608-task repository build. `git show --check`, `git diff --check`, clean-index checks, topology checks, scoped sensitive-file comparisons, and the fresh AST-index queries passed. These results support production correctness, but the focused pass cannot establish outer-wrapper preservation because the selector is non-discriminating.

Residual external limits remain live SMTP delivery, physical Android/IME behavior, graphical browser layout beyond JSDOM, and process-crash durability. None creates an additional finding.

## Push recommendation

Do not push yet. Return only PR #81's browser test to Coding and make the structural assertion distinguish the restored outer `FieldSet` from `CalmTextField`'s intrinsic inner `.fieldset`; leave production code, prior corrections, PR #82, and PR #75 unchanged. After that test-only correction passes the focused browser test and proportional verification, all three local PR heads can be pushed.
