Model: GPT-5.6 Codex
Changed files: features/auth/README.md, features/deeplinks/README.md, features/email/README.md, features/email/server/src/commonMain/kotlin/models/EmailPasswordChangePayload.kt, features/email/server/src/commonMain/kotlin/services/EmailPasswordChangeService.kt, features/ui/users/README.md, features/ui/users/src/androidMain/kotlin/ui/PasswordChangeView.kt, features/ui/users/src/commonMain/kotlin/Plugin.kt, features/ui/users/src/commonMain/kotlin/ui/PasswordChangeViewModel.kt, features/ui/users/src/commonMain/kotlin/ui/UserEditViewModel.kt, features/ui/users/src/commonTest/kotlin/ui/PasswordChangeViewModelTest.kt, features/ui/users/src/jsMain/kotlin/ui/PasswordChangeView.kt, features/ui/users/src/jvmMain/kotlin/ui/PasswordChangeView.kt, agents/task/09.09.2026_09.40.05-b512784f-672e-4309-aa90-4551cb0c3361/012-coding.md

The Coding role used the available ML Codex model, as required by the Coding model priority. I began from the required clean `4a601567abb4cde287f0f6bdeb0cc8592f8fb84d` head on `fix/issue-78-email-authorized-password-change`, rebuilt ast-index before navigation and again after source edits, and preserved the existing lifecycle join fixes.

## Completed corrections

`Plugin` now registers `PasswordChangeViewConfig.Pending` and `Completed` as concrete polymorphic subclasses under both `Any` and `ViewConfig`; it no longer attempts to register the sealed base serializer as a subtype.

`PasswordChangeViewModel.onSubmitPasswordChange` now admits from raw synchronous state, claims loading before launching work, and uses `when` guards for loading/terminal, mismatch, and policy rejection. The added regression changes a valid derived state immediately to a mismatch and sends two submissions without a dispatcher turn; it proves no stale mismatch request and only one admitted request.

The owner request action now synchronously verifies the current matching owner id, SMTP-enabled capability, non-busy state, non-null email, and approved flag before creating a mutation. This prevents stale derived eligibility from issuing an email request.

Email payload persistence now has the stable `email.password_change.v1` serial name. Issuance maps only a normal SMTP failure to `DeliveryFailed`; minting, revalidation, and cleanup failures propagate, and SMTP failure with failed cleanup retains the initiating failure with its cleanup failure suppressed. Cancellation retains the existing exact-id cleanup behavior.

All password-change views render live mismatch and policy feedback. JVM and Android password fields are single-line password inputs with password keyboard options and guarded IME Done completion. README corrections document the concrete serializer boundary, raw admission guard, platform form semantics, byte-versus-character policy distinction, stable payload schema, cleanup semantics, and sanitized deeplink GET failure boundary without changing Operator Notes.

## Verification

- `./gradlew :wishlist.features.ui.users:jvmTest --tests '*PasswordChangeViewModelTest' --console=plain` — passed.
- `./gradlew :wishlist.features.email.server:jvmTest :wishlist.features.auth.server:jvmTest :wishlist.features.deeplinks.server:jvmTest :wishlist.features.auth.common:jvmTest :wishlist.features.auth.client:jvmTest --console=plain` — passed.
- `./gradlew :wishlist.features.ui.users:compileKotlinJs :wishlist.features.ui.users:compileDebugKotlinAndroid :wishlist.features.ui.users:compileKotlinJvm --console=plain` — passed.
- `ast-index rebuild` — passed after source edits.
- `git diff --check` — passed.

The first focused JVM invocation failed only because `KeyboardOptions` was imported from the wrong Compose package; the import was changed to the repository's existing `androidx.compose.foundation.text` convention, and the rerun passed.

## Remaining validation scope

The correction establishes the concrete production fixes and a direct raw-admission regression. Architecture 011's broader T01 and T05--T15 evidence matrix, including production DI serializer construction, browser navigation/DOM forms, JVM/Robolectric rendered form tests, and the detailed service-failure/graph/transport matrix, is not implemented in this bounded coding pass. Those missing tests remain an acceptance blocker for independent validation; compilation and the focused existing suites are not substitutes for them.

```text
ENTITY:
entity_id=issue_78_coding_012; type=coding_result; state=partial_evidence_ready

CONTEXT:
* task_id=09.09.2026_09.40.05-b512784f-672e-4309-aa90-4551cb0c3361; agent_id=issue78_coding_cycle2; memory_ref=[009-validating.md,010-planning.md,011-architecturing.md]
* base_head=4a601567abb4cde287f0f6bdeb0cc8592f8fb84d; branch=fix/issue-78-email-authorized-password-change; ast_index_rebuilt=true

ACTION:
1. action=fix; target=V78-01; params={registration=[Pending,Completed],bases=[Any,ViewConfig]}
2. action=fix_and_test; target=V78-02; params={admission=raw_synchronous_state,test=PasswordChangeViewModelTest.synchronousSubmissionGuardRejectsStaleMismatchAndDuplicateClick}
3. action=fix; target=V78-03; params={delivery_classification=SMTP_only,cleanup_failure=propagated_or_suppressed}
4. action=fix; target=V78-05; params={owner_admission=current_profile_and_raw_capability}
5. action=fix; target=V78-06; params={feedback=live,native=[password_keyboard,single_line,IME_Done]}
6. action=fix; target=[V78-07,V78-08]; params={serial_name=email.password_change.v1,readmes=[auth,email,deeplinks,ui_users]}

VERIFICATION:
* check=ui_users_jvm_viewmodel_tests; expected=pass
* check=auth_email_deeplinks_jvm_suites; expected=pass
* check=ui_users_js_android_jvm_compilation; expected=pass
* check=diff_whitespace; expected=pass

UNCERTAINTY:
* missing=[T01,T05,T06,T07,T08,T09,T10,T11,T12,T13,T14,T15]; ambiguity=none
* acceptance_status=not_ready_for_validation; requirement=implement_remaining_architecture_011_evidence

REPETITION OF RESULT:
* entity_id=issue_78_coding_012; stored_in=tracked_step_report; status=partial_evidence_ready

COMMUNICATION:
* sender=issue78_coding_cycle2; receiver=orchestrator; task_id=09.09.2026_09.40.05-b512784f-672e-4309-aa90-4551cb0c3361; message_id=012-coding-result; protocol=AML-HIP

PERSISTENCE:
* local_memory=false; shared_memory=true; index_keys=[task_id,issue_78_coding_012,V78-01,V78-02,V78-03]

VALIDATION:
* format_valid=true; no_pronouns=true; entities_explicit=true; high_density=true; causal_chain_present=true; ambiguity_detected=false
```
