Model: GPT-5 (inherited session)
Changed files: features/email/client/src/commonTest/kotlin/KtorEmailFeatureTest.kt, features/email/server/src/commonTest/kotlin/configurators/EmailRoutingsConfiguratorTest.kt, agents/task/10.09.2026_16.32.33-2f63e0e4-5fa6-4bb0-819c-3c99ea86af3d/005-coding.md

# Coding Stage 3: HTTP boundary proof for owner email replacement

The inherited model was retained for Coding's ML/HL preference because this stage joins client serialization, Ktor route authentication, status mapping, and the existing repository invariant. The surgical-patch workflow kept the change at the two existing HTTP test fixtures. No production source, route contract, schema, dependency, feature README, platform view, shared Stage 2 file, prior report, or issue #78 file changed. No nested agent was used.

## Client transport proof

`setMyEmailPutsOnlyEmailAndReturnsStatusSuccess` exercises the production `KtorEmailFeature.setMyEmail` through the existing `MockEngine`. Each status variant requires exactly one PUT, the existing relative `/email/myEmail` path, `application/json`, and an object whose complete key set is exactly `email` with `replacement@example.com` as its value. The exact-key assertion excludes user IDs, selected-user IDs, caller IDs, root IDs, and every other account selector. HTTP 200 and 204 return `true`; HTTP 409 and 500 return `false`.

`setMyEmailNetworkFailurePropagates` makes the engine throw and requires the exception to escape the client call. No `/api` assertion was added to this relative transport fixture. The independently installed server route fixture proves `/api/email/myEmail`, matching the project's central global-prefix boundary without conflating that prefix with `KtorEmailFeature`'s relative request path.

The required command `./gradlew --no-parallel :wishlist.features.email.client:jvmTest --tests '*KtorEmailFeatureTest'` completed with `BUILD SUCCESSFUL in 18s`. Fresh XML timestamp `2026-09-10T12:03:45.158Z` records 6 tests, 0 skipped, 0 failures, and 0 errors. The class contains the two new PUT cases plus the four retained capability and verification tests.

## Server route proof

The route fixture now records every `(callerId, email)` pair and supports Boolean or thrown duplicate outcomes without becoming a generalized harness. Missing and invalid bearer credentials each produce 401 before any service call. Valid owner and other bearer tokens each determine the recorded target even when the JSON includes forged `callerId`, `userId`, `selectedUserId`, and `rootId` fields. The same valid email reaches the service, so selected, root, and other-account values cannot redirect the write.

Successful service mutation maps to 200, a false service outcome retains the existing 500 contract, and `DuplicateUserFieldException` maps to 409 after one caller-scoped invocation. Malformed JSON and an invalid serialized email each produce 400 before service invocation. The three pre-existing verification-route cases remain unchanged and green.

The first server invocation reached only test compilation and exposed a Kotlin generic-inference mismatch between expected non-null email pairs and the fixture's nullable-email call type. The smallest test-only correction gave the expected pair collections their exact nullable type; no endpoint defect or production correction was needed. The final required command `./gradlew --no-parallel :wishlist.features.email.server:jvmTest --tests '*EmailRoutingsConfiguratorTest'` completed with `BUILD SUCCESSFUL in 22s`. Fresh XML timestamp `2026-09-10T12:06:04.084Z` records 8 tests, 0 skipped, 0 failures, and 0 errors. The exact new passing names are `setEmailRejectsMissingAndInvalidBearerBeforeCallingFeature`, `setEmailUsesBearerCallerDespiteForgedAccountSelectors`, `setEmailMapsFeatureBooleanOutcomeToExistingStatuses`, `setEmailMapsDuplicateToConflict`, and `setEmailRejectsMalformedOrInvalidBodyBeforeCallingFeature`.

## Retained repository proof and limits

No users repository test or production source changed. The required command `./gradlew --no-parallel :wishlist.features.users.common:jvmTest --tests '*ExposedUsersRepoSqliteTest'` completed with `BUILD SUCCESSFUL in 17s`. Fresh XML timestamp `2026-09-10T12:06:47.858Z` records 12 tests, 0 skipped, 0 failures, and 0 errors. In particular, `emailApprovalTracksOnlyTheCurrentStoredAddress` proves same-address approval preservation, replacement approval reset, and stale-address approval rejection; `updateDuplicateEmailMapsToDuplicateUserFieldException` proves duplicate rollback; `bulkUpdatesPreserveOrResetCurrentAddressApproval`, `failedBulkUpdateLeavesRowsUnchanged`, and `conditionalApprovalEmitsOnlySuccessfulUpdates` retain the bulk, rollback, and conditional-event invariants identified by Architecture.

Every Gradle invocation ran serially after a process preflight found no Java or Gradle process. These are in-process MockEngine, Ktor test-application, and SQLite tests. No live HTTP server, SMTP service, browser, Android device, or external database was executed or claimed.

## Index and scope audit

The initial AST index contained 785 files, 6,520 symbols, 26,654 references, and 49 modules. The mandatory rebuild after the final Kotlin test edit completed with 785 files, 6,548 symbols, 26,804 references, and 49 modules. AST searches resolve both new client and server boundary test names at their source locations.

`git diff --check` passes. The source diff contains only the two allocated email test files and this report. Both feature READMEs have no diff from Stage 3's starting HEAD, so their Operator Notes remain byte-identical. Production-change count is zero, and no blocker remains for the later renderer/documentation and aggregate-verification stages.

```text
ENTITY:
entity_id=issue_79_stage3_http_boundary; type=transport_and_route_regression_suite; state=implemented_and_verified

CONTEXT:
* task_id=10.09.2026_16.32.33-2f63e0e4-5fa6-4bb0-819c-3c99ea86af3d; agent_id=issue79_coding_http_cycle1; memory_ref=[PROMPT.md,001-planning.md,002-architecturing.md,003-coding.md,004-coding.md,005-coding.md]
* constraints=[client_relative_path,server_global_prefix,bearer_self_ownership,Boolean_status_contract,serial_Gradle]; exclusions=[production_source,repository_edits,SMTP_execution,live_server,issue_78]

ACTION:
1. action=add_client_PUT_proof; target=issue_79_stage3_http_boundary; params={method:PUT,path:/email/myEmail,keys:[email],statuses:[200_true,204_true,409_false,500_false],network_failure:propagates}
2. action=add_server_route_proof; target=issue_79_stage3_http_boundary; params={path:/api/email/myEmail,caller_source:bearer,forged_selectors:ignored,statuses:[200,400,401,409,500]}
3. action=rerun_repository_invariants; target=exposed_users_repo_sqlite_suite; params={tests:12,approval_reset:green,duplicate_rollback:green,source_edits:0}

REASON:
* condition=missing_direct_setMyEmail_transport_tests; requirement=email_only_replacement_payload_and_status_truth; causal_chain=MockEngine_capture→exact_request_assertion→Boolean_result_assertion
* condition=missing_PUT_route_tests; requirement=bearer_owned_write_and_conflict_mapping; causal_chain=test_application_request→authenticated_principal→recorded_service_call_and_HTTP_status

EXPECTED RESULT:
* entity_id=issue_79_stage3_http_boundary; new_state=HTTP_acceptance_green; location=agents/task/10.09.2026_16.32.33-2f63e0e4-5fa6-4bb0-819c-3c99ea86af3d/005-coding.md

VERIFICATION:
* check=client_focused_XML; expected=6_tests_0_skipped_0_failures_0_errors; observed=6_tests_0_skipped_0_failures_0_errors
* check=server_focused_XML; expected=8_tests_0_skipped_0_failures_0_errors; observed=8_tests_0_skipped_0_failures_0_errors
* check=repository_focused_XML; expected=12_tests_0_skipped_0_failures_0_errors; observed=12_tests_0_skipped_0_failures_0_errors
* check=post_edit_AST_index; expected=full_rebuild_and_new_symbols; observed=785_files_6548_symbols_26804_refs_49_modules

UNCERTAINTY:
* missing=live_server_and_SMTP_execution; ambiguity=none; accepted_scope=in_process_HTTP_and_repository_proof
* missing=duplicate_specific_client_taxonomy; ambiguity=Boolean_false_cause; accepted_scope=HTTP_409_to_false_without_contract_expansion

REPETITION OF RESULT:
* entity_id=issue_79_stage3_http_boundary; stored_in=shared_memory; status=available; result=HTTP_acceptance_green

COMMUNICATION:
* sender=issue79_coding_http_cycle1; receiver=orchestrator; task_id=10.09.2026_16.32.33-2f63e0e4-5fa6-4bb0-819c-3c99ea86af3d; message_id=e8324a99-b210-4d52-950b-c3622362a090; protocol=AML-HIP

PERSISTENCE:
* local_memory=true; shared_memory=true; index_keys=[task_id,issue_79_stage3_http_boundary,owner_email_replacement]; storage=task_step_file; auto_memory=false

VALIDATION:
* format_valid=true; no_pronouns=true; entities_explicit=true; high_density=true; causal_chain_present=true; ambiguity_detected=true; ambiguity_resolution=accepted_limits_explicit_and_no_blocker
```
