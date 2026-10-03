Model: Inherited Codex coding agent.
Changed files: client/src/commonMain/kotlin/ClientPlugin.kt, client/src/commonMain/kotlin/PasswordChangeNavigationOwner.kt, client/src/commonTest/kotlin/PasswordChangeInteractorTest.kt, features/common/server/src/jvmMain/kotlin/utils/CallLoggingFormat.kt, features/email/server/src/commonTest/kotlin/PasswordChangeFlowRoutingTest.kt, features/ui/users/src/commonMain/kotlin/ui/PasswordChangeViewModel.kt, agents/task/09.09.2026_09.40.05-b512784f-672e-4309-aa90-4551cb0c3361/034-coding.md

## High-finding correction increment

Validation 031 supplied the initial red evidence for both High findings: its compiled Ktor diagnostic observed the global boundary change a typed bad request from 400 to 500, and its production navigation diagnostic observed 16 caller cancellations before an explicit save in 30 iterations. The prior reports remain unchanged.

For V78-09, the boundary now rethrows cancellation unchanged, uses Ktor 3.5.2's top-level `defaultExceptionStatusCode`, returns only the resulting status, and logs only method/status at DEBUG for classified client errors or ERROR for server errors. The Email flow fixture now obtains Common's actual StatusPages contribution and installs actual Auth routes. Its new malformed-body regression covers register, login, and refresh and confirms 400 plus no body sentinel disclosure; an unknown Auth path remains 404.

For V78-10, `PasswordChangeNavigationOwner` is registered once and bound from the Client root composition through `DisposableEffect` using the exact `rememberCoroutineScope` supplied to `initNavigation`. The owner launches only root-scope, exact-current-node, deduplicated replacement work, waits at most five seconds for the particular replacement, then synchronously saves the current root hierarchy. Binding disposal cancels its outstanding work without clearing a newer binding. The Pending ViewModel remains the HTTP/plaintext owner and checks cancellation between the model result and navigation handoff. The focused lifecycle regression uses an actual Pending `PasswordChangeViewModel`, a held model completion, a real navigation chain and production interactor; it verifies destroyed ViewModel lifecycle, cleared plaintext, Completed persistence, and UUID/plaintext absence from saved hierarchy.

Focused commands passed:

- `./gradlew --no-daemon :wishlist.features.email.server:jvmTest --tests '*PasswordChangeFlowRoutingTest' --console=plain`
- `./gradlew --no-daemon :wishlist.client:jvmTest --tests '*PasswordChangeInteractorTest' --console=plain`
- `./gradlew --no-daemon :wishlist.features.ui.users:jvmTest --tests '*PasswordChangeViewModelTest' --console=plain`

`git diff --check` passed. `ast-index rebuild` completed after the source changes. The browser lifecycle adapter, typed Ktor parity/cancellation capture, supersession/timeout/save-failure matrix, V78-04/V78-05 evidence, declaration audit, and README changes remain for subsequent coding increments; no High finding is represented as fully closed by this report alone.

```text
ENTITY:
entity_id=issue_78_coding_034; type=high_finding_correction; state=implemented_with_focused_evidence

CONTEXT:
* task_id=09.09.2026_09.40.05-b512784f-672e-4309-aa90-4551cb0c3361; agent_id=issue78_coding_high_cycle3; base_head=79156119b3b9a20db83657e689f3e274dda37920; branch=fix/issue-78-email-authorized-password-change
* constraints=[no_nested_agents,no_push,no_checkout,immutable_prior_reports,unchanged_Operator_Notes,source_index_rebuilt]

ACTION:
1. action=replace; target=V78-09_global_boundary; params={classifier=defaultExceptionStatusCode,cancellation=rethrow_identity,response=status_only,logging=[DEBUG_4xx,ERROR_5xx,no_throwable]}
2. action=replace; target=V78-10_navigation_interactor; params={owner=PasswordChangeNavigationOwner,binding=DisposableEffect_root_scope,transition=exact_node,dedup=node_identity,timeout_ms=5000,save=current_root_hierarchy_synchronous}
3. action=add_regression; target=PasswordChangeFlowRoutingTest; params={production_StatusPages=true,production_Auth_routes=true,malformed=[register,login,refresh],expected=400,sentinel_disclosure=false,unknown_path=404}
4. action=add_regression; target=PasswordChangeInteractorTest; params={actual_PasswordChangeViewModel=true,Pending_node=true,held_completion=true,destruction=true,Completed_save=true,plaintext_clear=true}

REASON:
* condition=typed_Ktor_client_failure_mapped_to_500; requirement=preserve_Ktor_status_contract_without_sensitive_detail; causal_chain=typed_classifier→status_only_response→sanitized_log
* condition=Pending_lifecycle_cancels_caller_owned_observer; requirement=Completed_persistence_after_confirmed_success; causal_chain=root_scope_owner→replacement_observation→synchronous_current_hierarchy_save

EXPECTED RESULT:
* entity_id=V78-09_global_boundary; new_state=implementation_present; location=features/common/server/src/jvmMain/kotlin/utils/CallLoggingFormat.kt
* entity_id=V78-10_navigation_owner; new_state=implementation_present; location=client/src/commonMain/kotlin/PasswordChangeNavigationOwner.kt
* entity_id=issue_78_coding_034; new_state=next_coding_increment_required; location=agents/task/09.09.2026_09.40.05-b512784f-672e-4309-aa90-4551cb0c3361/034-coding.md

VERIFICATION:
* check=PasswordChangeFlowRoutingTest; expected=all_selected_tests_pass; result=passed; command=./gradlew_no-daemon_:wishlist.features.email.server:jvmTest
* check=PasswordChangeInteractorTest; expected=all_selected_tests_pass; result=passed; command=./gradlew_no-daemon_:wishlist.client:jvmTest
* check=PasswordChangeViewModelTest; expected=all_selected_tests_pass; result=passed; command=./gradlew_no-daemon_:wishlist.features.ui.users:jvmTest
* check=git_diff_check; expected=no_whitespace_errors; result=passed
* check=ast_index_rebuild; expected=source_index_current; result=passed

UNCERTAINTY:
* missing=typed_Ktor_404_413_415_504_parity_and_cancellation_log_capture; ambiguity=V78-09_independent_validation_pending
* missing=browser_adapter_reload_and_root_disposal_timeout_save_failure_supersession_matrix; ambiguity=V78-10_independent_validation_pending
* missing=[V78-04_security_matrix,V78-05_owner_visibility,V78-07_declaration_audit,V78-08_READMEs,V78-11_forward_format_audit]; ambiguity=subsequent_increment_scope

REPETITION OF RESULT:
* entity_id=issue_78_coding_034; stored_in=shared_memory; status=available; high_implementation=[V78-09,V78-10]; independent_closure=false

COMMUNICATION:
* sender=issue78_coding_high_cycle3; receiver=root; task_id=09.09.2026_09.40.05-b512784f-672e-4309-aa90-4551cb0c3361; message_id=20f5ba69-8e68-45a8-9161-3614960ba2d8; protocol=AML-HIP

PERSISTENCE:
* local_memory=false; shared_memory=true; index_keys=[task_id,issue_78_coding_034,V78-09,V78-10]; persistence_medium=tracked_step_report_only

VALIDATION:
* format_valid=true; no_pronouns=true; entities_explicit=true; high_density=true; causal_chain_present=true; ambiguity_detected=false
```
