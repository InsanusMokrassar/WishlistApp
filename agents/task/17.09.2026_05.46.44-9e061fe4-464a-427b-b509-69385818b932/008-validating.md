Model: OpenAI Sol (HL)
Changed files: agents/task/17.09.2026_05.46.44-9e061fe4-464a-427b-b509-69385818b932/008-validating.md

# Validation report for PR #81 correction

## Model choice

OpenAI Sol is a high-level model and is the preferred available class for Validating under `agents/SHORTCUTS.md` and `agents/MODELS.md`. This correction review required reconstructing the intended master UI structure, distinguishing closure of earlier findings from a new overcorrection, and rechecking MVVM and password-change security boundaries. The explicit no-subagent instruction prevented low-level Markdown transcription, so the assigned Sol model wrote this report directly in normal prose. Caveman full was limited to internal working notes.

## Final result: FAIL

Both findings from `005-validating.md` are closed, but correction commit `60aa21a412c8cae7a1f44617a95db9126192aac0` removes more of master's web form structure than the finding requested. Finding counts are zero Critical, zero High, one Medium, and zero Low. The Medium finding does not involve auth, permissions, or data integrity.

Under the Orchestrator's Medium Findings Decision Rule, one non-sensitive Medium finding does not by itself mandate a Coding loop. The corrected branch nevertheless remains outside the exact master-preservation and narrow-scope acceptance conditions, so this validation recommends one more focused PR #81 correction before push.

## Finding

### Medium — the correction removes master's unlabeled email `FieldSet`, not only its subtitle

Master `a220b3d` retains `FieldSet { ... }` around the complete JS owner-email section at `features/ui/users/src/jsMain/kotlin/ui/UserEditView.kt:149`; master commit `dfe353a` removed only the label argument and localized subtitle. Correction `60aa21a` deletes both the opening wrapper and its closing brace, so the corrected tree enters the email `when` directly at `features/ui/users/src/jsMain/kotlin/ui/UserEditView.kt:152`–`153`. This is not a formatting-only abstraction: `FieldSet` emits a `.fieldset` container at `features/common/client/src/jsMain/kotlin/ui/components/CalmForms.kt:37`–`41`, and `.fieldset` supplies an 18-pixel bottom margin at `features/common/client/src/jsMain/kotlin/ui/CalmStudioStyleSheet.kt:523`–`526`.

The extra deletion changes grouping and layout beyond the prior finding, makes the corrected JS tree differ from master in an unrelated location, and leaves visibly over-indented orphaned content. Restore the unlabeled `FieldSet { ... }` wrapper exactly as master has it. Keep the `emailSectionTitle` resource absent, keep the label absent, and retain the improved browser assertions.

This is a new overcorrection, not the same unresolved finding from the previous validation cycle. Repeat-problem escalation therefore does not apply.

## Previous-finding closure

The previous Medium finding is closed: `emailSectionTitle` has no definition or reference, the JS editor no longer renders the stale subtitle, and Android/JVM remain subtitle-free. The previous Low finding is closed: `features/ui/users/README.md:67` now correctly attributes `meState.mapLatest { ... }.stateIn(...)` to `DefaultUsersModel`, consistent with the implementation and the composition-only Plugin.

The revised browser proof is substantive rather than assertion removal. `features/ui/users/src/jsTest/kotlin/ui/UserEditViewBrowserTest.kt:76`–`78` proves private state and `#settings-email` are absent when required; lines 122–136 prove the approved-owner email control and password-change action are present, the action disables while busy, the exact email reaches the model, and delivery failure is rendered. Only the `.fieldset` structure is untested, which is why the focused test remains green despite the new finding.

## Topology, scope, and role-compliance audit

PR #81 head `60aa21a412c8cae7a1f44617a95db9126192aac0` is an ordinary one-parent correction on verified merge `f549be86c70c9d75bf2710f9509542678268433f`. The merge still has original PR head `ffd73fd5698b2c1f0c80b5948d38761787883b4b` first and master `a220b3d2f9f4224e09880022e3051c374bd69a6a` second. Master remains an ancestor; no history was rewritten. The correction message is normal prose and has the required `Co-Authored-By: Claude <noreply@anthropic.com>` footer.

The correction changes exactly four users-feature files and contains no current-task artifact. `git show --check`, `git diff --check`, conflict-marker scanning, and the unmerged-index check pass. The users README Operator Notes block is byte-identical before and after the correction. PR #82 remains exactly `06cf01ea39a411141359291cd918e8775a272ea3`, and PR #75 remains exactly `1cbab45e1579cd8168bd3c88e34d15278ac8397f`. Remote-tracking branches remain unchanged, so no role pushed.

Coding used the preferred ML model, committed only `006-coding.md` on the coordination branch, and limited the PR branch correction to the reported locations. Verification used the preferred ML model, committed only `007-verification.md`, independently tested the corrected head, and did not mutate it. Both roles correctly closed the original label/resource and README findings, but both missed that the intended unlabeled wrapper was also removed. No other role restriction was violated; the root-owned `PROMPT.md` remains untracked and unstaged.

## MVVM, security, and verification evidence

A fresh `ast-index` rebuild indexed 847 files. `ast-index refs emailSectionTitle` returns no references, while `ast-index refs DefaultUsersModel` still shows one production definition, the explicit Plugin construction, and focused tests. The correction changes no Model, Plugin, Auth, Email, DeepLinks, route, serializer, approval, credential, or persistence file. `DefaultUsersModel` therefore retains its primary-constructor `private val passwordChangeFeature`, exact one-call nullable delegations, and exception/cancellation propagation. The production fallback scan again finds no anonymous `object : *Model` implementation.

The independent focused command `./gradlew --no-daemon --no-parallel :wishlist.features.ui.users:jsBrowserTest --tests '*UserEditViewBrowserTest*' --rerun-tasks --console=plain` passed with 234 executed tasks. Verification also records passing focused browser, full browser, JVM, and 4,608-task full-build gates. Those gates establish functional behavior and compilation but do not preserve the removed `.fieldset` DOM/layout wrapper.

Residual external limits remain live SMTP, physical Android/IME behavior, graphical browser review beyond JSDOM, and process-crash durability. None creates an additional finding.

## Push recommendation

Do not push yet. Return only PR #81 to Coding to restore the unlabeled `FieldSet` wrapper, then rerun its focused JS browser test and the proportional verification gate. Leave the closed subtitle resource, README correction, PR #82, and PR #75 unchanged.
