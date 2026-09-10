Model: Inherited Codex coding agent.
Changed files: client/src/commonMain/kotlin/PasswordChangeNavigationOwner.kt, client/src/commonTest/kotlin/PasswordChangeInteractorTest.kt, client/src/jsTest/kotlin/PasswordChangeNavigationBrowserTest.kt, agents/task/09.09.2026_09.40.05-b512784f-672e-4309-aa90-4551cb0c3361/037-coding.md

## V78-10 lifecycle completion matrix

`PasswordChangeNavigationOwner` now starts an UNDISTPATCHED root-owned stack observer before queuing its exact-node replacement. The observer stops after either the exact replacement arrives or the accepted Pending node disappears, and the transition waits no more than five seconds. A timeout, supersession, root disposal, or repository exception does not create a second request or write an obsolete hierarchy. The transition always removes its tracked owner job after observer cleanup.

The common test matrix now exercises the actual users password ViewModel, ClientPlugin interactor and concrete owner. It proves Pending destruction and cleared fields before root-owned persistence; a Completed ViewModel admits no submit; Continue persists UsersList; cancellation-resistant completion does not hand off after Pending destruction; stale nodes make no replacement/save; binding disposal clears owner work; a stopped queue times out at five virtual seconds; and a first synchronous save failure leaves Completed usable for a later persisted Continue. Fresh JVM and Node XML report seven tests with zero failures, errors, and skips.

The JSDOM test restores the real URL scaffold, binds the production owner to the live root, submits an actual Pending ViewModel through a held result, and verifies the credential-free URL, null history state, no Pending config/plaintext, adapter reload to Completed, a Completed ViewModel with zero additional HTTP, and UsersList on Continue. Every replaced JSDOM global constructor is restored. The test is JSDOM evidence only, not a graphical-browser claim. The JS browser runner publishes HTML rather than XML; its focused test passed, and the all-tests report records the browser lifecycle case at zero failures/errors/skips.

Focused commands passed:

- `./gradlew --no-daemon :wishlist.client:jvmTest --tests '*PasswordChangeInteractorTest' --console=plain`
- `./gradlew --no-daemon :wishlist.client:jsBrowserTest --tests '*PasswordChangeNavigationBrowserTest' --console=plain`
- `./gradlew --no-daemon :wishlist.client:jsNodeTest :wishlist.client:jsBrowserTest --console=plain`

`ast-index rebuild` completed after Kotlin source/test edits and `git diff --check` passed. The navigation-specific V78-10 direct evidence is complete; independent validation remains responsible for closure.

```text
ENTITY:
entity_id=issue_78_coding_037; type=V78_10_navigation_matrix; state=implemented_and_focused_verified

CONTEXT:
* task_id=09.09.2026_09.40.05-b512784f-672e-4309-aa90-4551cb0c3361; agent_id=issue78_coding_navigation_matrix; base_head=38779cbec986dddccef596755a404d77aab40e6c; branch=fix/issue-78-email-authorized-password-change
* constraints=[V78-10_only,no_nested_agents,no_push,no_checkout,prior_steps_immutable,Operator_Notes_unchanged,JSDOM_only]

ACTION:
1. action=replace; target=PasswordChangeNavigationOwner; params={observer=UNDISPATCHED_root_job,observation=[replacement_present,Pending_absent,root_detached],timeout_ms=5000,cleanup=cancel_and_join,save=current_root_hierarchy_synchronous}
2. action=extend_regression; target=PasswordChangeInteractorTest; params={cases=[actual_VM_destruction,Completed_zero_submit,Continue_UsersList,cancellation_resistant_result,stale_Pending,root_disposal,timeout,first_save_failure],dispatcher=deterministic_virtual_time,owner_jobs=asserted}
3. action=extend_browser_regression; target=PasswordChangeNavigationBrowserTest; params={adapter=WishlistsAppUrlNavigationConfigsRepo,scaffold=production_restored,model=actual_Pending_and_Completed_ViewModels,assertions=[password_changed_URL,null_history_state,plaintext_absent,reload_Completed,Continue_UsersList,globals_restored]}
4. action=run; target=client_navigation_gates; params={JVM=PasswordChangeInteractorTest_7_pass,JS_Node=PasswordChangeInteractorTest_7_pass,JS_Browser=PasswordChangeNavigationBrowserTest_pass,report_sources=[JUnit_XML,Gradle_HTML]}

REASON:
* condition=Navigation_0.7.7_generic_saver_omits_replacement_diff; requirement=V78-10_durable_credential_free_transition; causal_chain=root_owned_observer→exact_replacement_confirmation→current_hierarchy_save
* condition=Pending_ViewModel_lifecycle_cancellation_and_adapter_failure; requirement=no_stale_handoff_or_HTTP_replay; causal_chain=ensureActive_and_exact_node_guard→zero_stale_transition→usable_Completed_Continue

EXPECTED RESULT:
* entity_id=PasswordChangeNavigationOwner; new_state=finite_root_owned_replacement_owner; location=client/src/commonMain/kotlin/PasswordChangeNavigationOwner.kt
* entity_id=PasswordChangeInteractorTest; new_state=seven_platform_shared_lifecycle_cases; location=client/src/commonTest/kotlin/PasswordChangeInteractorTest.kt
* entity_id=PasswordChangeNavigationBrowserTest; new_state=JSDOM_adapter_VM_lifecycle_evidence; location=client/src/jsTest/kotlin/PasswordChangeNavigationBrowserTest.kt

VERIFICATION:
* check=focused_client_JVM_XML; expected={tests=7,failures=0,errors=0,skipped=0}; result=passed
* check=unfiltered_client_JS_Node_XML; expected={PasswordChangeInteractorTest_tests=7,failures=0,errors=0,skipped=0}; result=passed
* check=focused_client_JS_Browser_HTML; expected={PasswordChangeNavigationBrowserTest_tests=1,failures=0,errors=0,skipped=0}; result=passed
* check=unfiltered_client_JS_Browser_HTML; expected=browser_lifecycle_case_zero_failures; result=passed
* check=source_index; expected=ast_index_rebuild_after_Kotlin_changes; result=passed
* check=diff_whitespace; expected=git_diff_check_clean; result=passed

UNCERTAINTY:
* missing=independent_validation_result; ambiguity=V78-10_closure_requires_independent_review
* missing=graphical_browser_execution; ambiguity=JSDOM_adapter_and_DOM_behavior_only
* missing=process_termination_or_adapter_repository_failure_durability; ambiguity=outside_root_lifetime_guarantee

REPETITION OF RESULT:
* entity_id=issue_78_coding_037; stored_in=shared_memory; status=available; V78-10_focused_evidence=complete; independent_closure=false

COMMUNICATION:
* sender=issue78_coding_navigation_matrix; receiver=root; task_id=09.09.2026_09.40.05-b512784f-672e-4309-aa90-4551cb0c3361; message_id=3d9a1393-e7b3-4eb7-b44c-0d1df54b8592; protocol=AML-HIP

PERSISTENCE:
* local_memory=false; shared_memory=true; index_keys=[task_id,issue_78_coding_037,V78-10,PasswordChangeNavigationOwner,PasswordChangeInteractorTest,PasswordChangeNavigationBrowserTest]; persistence_medium=tracked_step_report_only; personal_auto_memory=false

VALIDATION:
* format_valid=true; no_pronouns=true; entities_explicit=true; high_density=true; causal_chain_present=true; ambiguity_detected=false
```
