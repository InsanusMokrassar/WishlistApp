Model: Inherited Codex coding agent.
Changed files: features/email/server/src/commonTest/kotlin/PasswordChangeFlowRoutingTest.kt, agents/task/09.09.2026_09.40.05-b512784f-672e-4309-aa90-4551cb0c3361/036-coding.md

## V78-09 status-classification matrix

The production Email routing fixture now obtains Common JVMPlugin's actual `StatusPagesConfigurator.Element` contribution and installs the real Auth, password-change, and deeplink routes. The new matrix throws Ktor 3.5.2's installed typed exceptions through that contribution and verifies `400`, `404`, `415`, `415`, `413`, `504`, and sanitized `500` outcomes. A no-sanitizer bad-request test records the baseline `400` parity. Register, login, and refresh now each cover malformed JSON and missing required fields, while anonymous `getMe` remains `401` and an unknown Auth path remains `404`.

The fixture captures both `Ktor`/CallLogging and `io.ktor.server.Application` at DEBUG level, restores each direct level and appender after the assertion, and rejects request UUID, URL/query, body, email, password, Location, cause, and suppressed-cause markers in the formatted message, MDC values, and argument array. Every captured event must have no throwable proxy. The direct callback test obtains the actual Common-contributed `Throwable` callback, invokes it with a normal `CancellationException` and a `TimeoutCancellationException` produced by `withTimeout`, and proves identity rethrow, no callback response status, and no boundary log. No production change was required: the existing boundary preserved the complete installed classifier behavior.

Focused and affected suites passed:

- `./gradlew --no-daemon :wishlist.features.email.server:jvmTest --tests '*PasswordChangeFlowRoutingTest' --console=plain`
- `./gradlew --no-daemon :wishlist.features.common.server:jvmTest :wishlist.features.email.server:jvmTest :wishlist.features.auth.server:jvmTest :wishlist.features.deeplinks.server:jvmTest :wishlist.features.auth.common:jvmTest :wishlist.features.auth.client:jvmTest --rerun-tasks --console=plain`

Fresh XML reports show zero failures, errors, and skips for the selected `PasswordChangeFlowRoutingTest` (11 tests), Common server, Auth server/common/client, Email server, and DeepLinks server JVM suites. `ast-index rebuild` completed after the Kotlin test source change, and `git diff --check` passed.

```text
ENTITY:
entity_id=issue_78_coding_036; type=V78_09_status_matrix; state=implemented_and_verified

CONTEXT:
* task_id=09.09.2026_09.40.05-b512784f-672e-4309-aa90-4551cb0c3361; agent_id=issue78_coding_status_matrix; base_head=08b6c79e312b25683a9a7bada3bf6b286651ee0f; branch=fix/issue-78-email-authorized-password-change
* constraints=[V78-09_only,no_nested_agents,no_push,no_checkout,prior_steps_immutable,Operator_Notes_unchanged,Common_JVMPlugin_contribution,Auth_routes]

ACTION:
1. action=extend_fixture; target=PasswordChangeFlowRoutingTest; params={production_contribution=CommonServerJVMPlugin_StatusPagesConfigurator_Element,production_routes=[Auth,PasswordChange,DeepLinks],typed_cases=[BadRequest_400,NotFound_404,UnsupportedMediaType_415,CannotTransformContentToType_415,PayloadTooLarge_413,TimeoutException_504,IllegalStateException_500]}
2. action=extend_Auth_contract; target=PasswordChangeFlowRoutingTest; params={register=[malformed_400,missing_field_400],login=[malformed_400,missing_field_400],refresh=[malformed_400,missing_field_400],explicit=[getMe_401,unknown_path_404]}
3. action=add_log_capture; target=KtorLogCapture; params={loggers=[Ktor,io.ktor.server.Application],levels=DEBUG,restoration=[direct_levels,test_appenders],assertions=[method_final_status,formatted_message,MDC,argument_array,null_throwableProxy]}
4. action=add_cancellation_proof; target=Common_StatusPages_Throwable_callback; params={inputs=[CancellationException,withTimeout_TimeoutCancellationException],expected=[identity_rethrow,no_callback_response_status,no_boundary_log]}
5. action=run; target=focused_and_affected_JVM_suites; params={focused=PasswordChangeFlowRoutingTest,aggregate=[Common_server,Auth_server,Email_server,DeepLinks_server,Auth_common,Auth_client],rerun_tasks=true,result=passed}

REASON:
* condition=global_Throwable_boundary_requires_status_contract_preservation; requirement=V78-09; causal_chain=installed_Ktor_typed_exception→defaultExceptionStatusCode→status_only_response_and_redacted_log
* condition=legacy_Auth_receive_failures_use_Ktor_body_classification; requirement=malformed_and_missing_field_400_parity; causal_chain=actual_Auth_routes→production_StatusPages_contribution→HTTP_400
* condition=cancellation_is_control_flow; requirement=identity_rethrow_without_boundary_side_effect; causal_chain=actual_contributed_callback→CancellationException_or_TimeoutCancellationException→rethrow_same_instance

EXPECTED RESULT:
* entity_id=V78-09; new_state=focused_implementation_and_regression_evidence_complete; location=features/email/server/src/commonTest/kotlin/PasswordChangeFlowRoutingTest.kt
* entity_id=PasswordChangeFlowRoutingTest; new_state=eleven_JVM_tests_passing; location=features/email/server/build/test-results/jvmTest/TEST-dev.inmo.wishlist.features.email.server.PasswordChangeFlowRoutingTest.xml

VERIFICATION:
* check=focused_command; expected=PasswordChangeFlowRoutingTest_pass; result=passed; test_count=11
* check=typed_status_matrix; expected=[400,404,415,415,413,504,500]; result=passed
* check=Auth_malformed_missing_matrix; expected=register_login_refresh_400; result=passed
* check=explicit_status_contract; expected=[getMe_401,unknown_Auth_path_404,unsanitized_BadRequest_400]; result=passed
* check=log_redaction; expected=[method_final_status_only,no_sentinel_in_formatted_message_MDC_argument_array,null_throwableProxy]; result=passed
* check=cancellation_callback; expected=[same_instance_rethrow,no_callback_response_status,no_boundary_log]; result=passed
* check=affected_JVM_aggregate; expected=[Common_server,Auth_server,Email_server,DeepLinks_server,Auth_common,Auth_client]; result=passed
* check=source_index; expected=ast_index_rebuild_after_Kotlin_change; result=passed
* check=diff_whitespace; expected=git_diff_check_clean; result=passed

UNCERTAINTY:
* missing=independent_Validation_result; ambiguity=V78-09_closure_requires_independent_review
* missing=V78-04_V78-05_V78-07_V78-08_V78-10_V78-11_completion_evidence; ambiguity=outside_V78-09_scope

REPETITION OF RESULT:
* entity_id=issue_78_coding_036; stored_in=shared_memory; status=available; V78-09_status=focused_evidence_complete; production_source_change=false

COMMUNICATION:
* sender=issue78_coding_status_matrix; receiver=root; task_id=09.09.2026_09.40.05-b512784f-672e-4309-aa90-4551cb0c3361; message_id=6fa55006-2e23-4b1f-b631-8f11e6244c54; protocol=AML-HIP; V78-09=focused_evidence_complete

PERSISTENCE:
* local_memory=false; shared_memory=true; index_keys=[task_id,issue_78_coding_036,V78-09,PasswordChangeFlowRoutingTest]; persistence_medium=tracked_step_report_only; personal_auto_memory=false

VALIDATION:
* format_valid=true; no_pronouns=true; entities_explicit=true; high_density=true; causal_chain_present=true; ambiguity_detected=false
```
