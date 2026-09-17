Model: Codex GPT-5.6 coding agent.
Changed files: features/ui/users/src/commonTest/kotlin/ui/UserEditViewModelPasswordChangeTest.kt, features/ui/users/src/jsTest/kotlin/ui/UserEditViewBrowserTest.kt, agents/task/09.09.2026_09.40.05-b512784f-672e-4309-aa90-4551cb0c3361/039-coding.md

## V78-05 owner eligibility and rendered visibility evidence

V78-05 is complete at the requested test boundary. The ViewModel matrix now asserts the observable eligibility StateFlow for eligible ordinary owner and root-on-self flows, and every requested reject state: anonymous, another account, root editing another account, absent/unapproved/mismatched private email, disabled/failed capability, failed profile, held cold/loading/refresh states, and an active mutation. It covers `Unavailable` alongside Sent, Ineligible, DeliveryFailed, and null transport results while preserving the zero verification, administrator-password, and username side-effect assertions.

The previous ambiguous stale-derived case is split into deterministic `StandardTestDispatcher` schedules. A stale `true` value cannot bypass raw authorization, and a stale `false` value cannot block a raw-valid request emitted at the profile/loading transition. The obsolete-completion case now starts a separate later held mutation and proves the released old operation cannot publish a result, trigger a private-profile read, or clear the newer busy slot.

The actual Compose HTML browser suite keeps the positive/busy/result case and adds independent disabled-SMTP, missing-email, unapproved-email, non-owner, root-on-other, and held-refresh DOM cases. Each negative case examines the current DOM; non-owner/root-on-other cases also prove the private address and owner-email feedback are absent. The held refresh uses the rendered Refresh event and proves the approved request button is absent after profile clearing, without inspecting a detached historical button. No production change was needed because all new assertions passed against the existing raw owner gate and form branch.

Focused JVM XML reports 8 `UserEditViewModelPasswordChangeTest` cases with zero failures/skips. The unfiltered JS Node report has 41 cases with zero failures/skips and no `UserEditViewBrowserTest` report, matching the exact build filter. The unfiltered JS browser report has 7 `UserEditViewBrowserTest` DOM cases with zero failures/skips. `ast-index rebuild` completed after the Kotlin changes; `git diff --check` passed.

```text
ENTITY:
entity_id=issue_78_coding_039; type=V78_05_owner_presentation_evidence; state=implemented_and_verified

CONTEXT:
* task_id=09.09.2026_09.40.05-b512784f-672e-4309-aa90-4551cb0c3361; agent_id=issue78_coding_owner_cycle3; base_head=926810db9c289c524e869b4b8dabaafb46de89a2; branch=fix/issue-78-email-authorized-password-change
* constraints=[V78-05_only,no_nested_agents,no_push,no_checkout,prior_reports_immutable,Operator_Notes_unchanged,production_change_only_on_exposed_discrepancy]

ACTION:
1. action=extend_ViewModel_matrix; target=UserEditViewModelPasswordChangeTest; params={eligibility=[owner_true,root_self_true,anonymous_false,other_false,root_other_false,null_false,unapproved_false,mismatch_false,disabled_false,probe_failed_false,profile_failed_false,busy_false],outcomes=[Sent,Ineligible,Unavailable,DeliveryFailed,null],side_effects=[verification_zero,admin_password_zero,username_zero]}
2. action=add_dispatcher_schedules; target=UserEditViewModelPasswordChangeTest; params={dispatcher=StandardTestDispatcher,stale_true=[derived_true,raw_authorization_false,request_zero],stale_false=[derived_false,raw_enabled_profile_loading_false,request_exact_owner_email],gates=[cold_Unknown,probe_Loading,profile_Loading,refresh_cleared_profile]}
3. action=replace_obsolete_completion_case; target=UserEditViewModelPasswordChangeTest; params={old_mutation=NonCancellable_held,later_mutation=held,old_release_assertions=[result_null,profile_reads_unchanged,busy_true],later_result=DeliveryFailed,busy_final=false,cleanup=[deferred_release,cancelAndJoin]}
4. action=extend_Compose_HTML_DOM; target=UserEditViewBrowserTest; params={positive=[visible,busy,DeliveryFailed],negative=[SMTP_disabled,email_missing,email_unapproved,other_user,root_other,held_refresh],private_DOM_hidden=[other_user,root_other],current_DOM_button_absent=true,real_event=Refresh_email_status}
5. action=run; target=ui_users_gates; params={JVM=UserEditViewModelPasswordChangeTest,JS_Node=unfiltered,JS_Browser=unfiltered,source_discrepancy=false,ast_index=rebuild_complete}

REASON:
* condition=V78-05_missing_observable_owner_eligibility_and_DOM_evidence; requirement=raw_admission_and_rendered_visibility_proof; causal_chain=deterministic_fixture_schedules_and_real_DOM_events→StateFlow_and_current_node_assertions→focused_regression_evidence
* condition=derived_StateFlow_can_lag_raw_owner_inputs; requirement=raw_authorization_neither_bypassed_nor_blocked; causal_chain=stale_true_raw_rejection_and_stale_false_raw_admission→exact_email_request_assertion
* condition=obsolete_mutation_can_resume_after_identity_transition; requirement=later_mutation_ownership_preserved; causal_chain=old_release_during_later_busy→zero_old_publication_read_or_busy_clear→later_normal_completion

EXPECTED RESULT:
* entity_id=V78-05; new_state=focused_owner_visibility_and_concurrency_evidence_complete; location=features/ui/users/src/commonTest/kotlin/ui/UserEditViewModelPasswordChangeTest.kt
* entity_id=V78-05_DOM; new_state=real_Compose_HTML_negative_visibility_evidence_complete; location=features/ui/users/src/jsTest/kotlin/ui/UserEditViewBrowserTest.kt
* entity_id=issue_78_coding_039; new_state=independent_validation_pending; location=agents/task/09.09.2026_09.40.05-b512784f-672e-4309-aa90-4551cb0c3361/039-coding.md

VERIFICATION:
* check=focused_JVM_command; expected=UserEditViewModelPasswordChangeTest_pass; result=passed; report={tests:8,failures:0,skipped:0}; command=./gradlew_:wishlist.features.ui.users:jvmTest_--tests_*UserEditViewModelPasswordChangeTest
* check=unfiltered_JS_Node_command; expected=all_Node_cases_pass_and_DOM_class_excluded; result=passed; report={tests:41,failures:0,skipped:0,UserEditViewBrowserTest_report:absent}; command=./gradlew_:wishlist.features.ui.users:jsNodeTest_:wishlist.features.ui.users:jsBrowserTest
* check=unfiltered_JS_Browser_command; expected=UserEditViewBrowserTest_DOM_cases_pass; result=passed; report={tests:7,failures:0,skipped:0}; command=./gradlew_:wishlist.features.ui.users:jsNodeTest_:wishlist.features.ui.users:jsBrowserTest
* check=source_production_discrepancy; expected=direct_assertion_failure_required; result=absent; production_files_changed=false
* check=source_index; expected=post_Kotlin_rebuild; result=passed; indexed_files=1460
* check=diff_whitespace; expected=no_errors; result=passed

UNCERTAINTY:
* missing=independent_validation_result; ambiguity=V78-05_closure_requires_independent_review
* missing=graphical_browser_execution; ambiguity=JSDOM_Compose_HTML_cases_prove_DOM_behavior_not_native_browser_keyboard_or_rendering
* missing=live_SMTP_delivery; ambiguity=fixture_outcome_matrix_exercises_client_presentation_and_admission_only

REPETITION OF RESULT:
* entity_id=issue_78_coding_039; stored_in=shared_memory; status=available; V78-05_focused_evidence=complete; production_source_changed=false

COMMUNICATION:
* sender=issue78_coding_owner_cycle3; receiver=root; task_id=09.09.2026_09.40.05-b512784f-672e-4309-aa90-4551cb0c3361; message_id=8588de83-fa9b-44c8-8a84-b75de2b40a81; protocol=AML-HIP; V78-05=focused_evidence_complete

PERSISTENCE:
* local_memory=false; shared_memory=true; index_keys=[task_id,issue_78_coding_039,V78-05,UserEditViewModelPasswordChangeTest,UserEditViewBrowserTest]; persistence_medium=tracked_step_report_only; personal_auto_memory=false

VALIDATION:
* format_valid=true; no_pronouns=true; entities_explicit=true; high_density=true; causal_chain_present=true; ambiguity_detected=false
```
