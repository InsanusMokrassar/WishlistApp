Model: Inherited Codex coding agent.
Changed files: client/src/commonTest/kotlin/PasswordChangeInteractorTest.kt, agents/task/09.09.2026_09.40.05-b512784f-672e-4309-aa90-4551cb0c3361/035-coding.md

## Follow-up lifecycle evidence

This focused follow-up extends the committed root-owner regression with an explicit stale-node check and persistence after Continue. The existing production interactor test now verifies that the completed-page Continue operation leaves a nonempty users-list destination and invokes a persisted hierarchy save. The new stale-node case places another destination above Pending before calling `onChanged`; it verifies neither replacement nor save occurs.

`./gradlew --no-daemon :wishlist.client:jvmTest --tests '*PasswordChangeInteractorTest' --console=plain` passed with all three selected tests. No source behavior change was needed for this evidence increment.

```text
ENTITY:
entity_id=issue_78_coding_035; type=navigation_lifecycle_evidence; state=partial_evidence_added

CONTEXT:
* task_id=09.09.2026_09.40.05-b512784f-672e-4309-aa90-4551cb0c3361; agent_id=issue78_coding_high_cycle3; base_head=d0dd1880e31e89651f84fc809be872695ad5aaf5; branch=fix/issue-78-email-authorized-password-change
* constraints=[no_nested_agents,no_push,no_checkout,immutable_prior_reports,unchanged_Operator_Notes]

ACTION:
1. action=extend_regression; target=PasswordChangeInteractorTest; params={Continue=UsersList_persisted,stale_Pending=zero_replace_zero_save}
2. action=run; target=:wishlist.client:jvmTest; params={test_filter=PasswordChangeInteractorTest,result=passed,test_count=3}

REASON:
* condition=queued_Pending_callback_after_newer_destination; requirement=exact_node_stale_guard; causal_chain=non_last_node→owner_rejection→zero_navigation_and_save
* condition=Completed_exit_uses_replacement; requirement=nonempty_durable_destination; causal_chain=Continue→UsersList_replacement→hierarchy_save

EXPECTED RESULT:
* entity_id=PasswordChangeInteractorTest; new_state=stale_and_Continue_evidence_present; location=client/src/commonTest/kotlin/PasswordChangeInteractorTest.kt

VERIFICATION:
* check=focused_client_JVM; expected=three_selected_tests_pass; result=passed
* check=git_diff_check; expected=no_whitespace_errors; result=pending_final_command

UNCERTAINTY:
* missing=[typed_Ktor_status_log_cancellation_matrix,root_disposal_timeout_save_failure_supersession_matrix,production_composable_browser_VM_submission]; ambiguity=High_closure_requires_remaining_architecture_033_evidence

REPETITION OF RESULT:
* entity_id=issue_78_coding_035; stored_in=shared_memory; status=available; High_closure=false

COMMUNICATION:
* sender=issue78_coding_high_cycle3; receiver=root; task_id=09.09.2026_09.40.05-b512784f-672e-4309-aa90-4551cb0c3361; message_id=8c09a330-f94f-4b29-b759-1a29c6d41a22; protocol=AML-HIP

PERSISTENCE:
* local_memory=false; shared_memory=true; index_keys=[task_id,issue_78_coding_035,V78-10]; persistence_medium=tracked_step_report_only

VALIDATION:
* format_valid=true; no_pronouns=true; entities_explicit=true; high_density=true; causal_chain_present=true; ambiguity_detected=false
```
