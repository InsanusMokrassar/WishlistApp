Model: GPT-5 (ML verification)
Changed files: agents/task/10.09.2026_16.32.33-2f63e0e4-5fa6-4bb0-819c-3c99ea86af3d/008-verification.md

# Verification of issue #79

GPT-5 satisfies the Verification role's ML-first preference. Verification was validation-only: no production, test, feature documentation, build, prompt, prior report, or Operator Notes file was edited. The `verify-and-stop` discipline kept the work to acceptance proof and the required report, while Caveman mode was limited to internal working notes.

## Verification Result: PASS

### Build

The required starting revision was clean `cf43e92a21d6e59392c5b30787d0935b21a45589` on `feat/issue-79-user-email-change`. A process preflight found zero Java or Gradle processes for the worktree. Verification then ran exactly one foreground Gradle invocation, serially and with the standard cache: `set -o pipefail; ./gradlew --no-parallel build --rerun-tasks --console=plain 2>&1 | tee /tmp/issue79-008-build-output.txt`. The same foreground session was polled until termination; no second Gradle invocation was started.

The real shell and Gradle exit code was 0. The terminal markers were `BUILD SUCCESSFUL in 5m 11s` and `4530 actionable tasks: 4530 executed`. The complete 577,074-byte log is `/tmp/issue79-008-build-output.txt`, SHA-256 `c9597b16d335f5a8b29657208839ddf43c7ae92d4022ff7181b05540e1a2a43a`. The build executed JVM, JS browser, JS Node, Android debug, and Android release test tasks, so the zero-test-only `allTests` fallback was not run.

### Fresh test results

Only `TEST-*.xml` files whose modification time was at or after the exact build log's creation epoch `1789046180` were counted. Across the repository, 159 fresh suites contain 898 tests, 0 failures, 0 errors, and 0 skipped tests. Therefore 898 tests passed.

The issue-focused UI module produced 54 tests on each of JVM, JS Node, JS browser, Android debug unit, and Android release unit targets, for 270 tests total. Each target contains the same normalized 48-case `UserEditViewModelEmailTest` name set, SHA-256 `09b2a71b8d200c41a531e38d002aa24a975917367a3b819a52dc7949cbbe910c`, plus one `UsersModelTest` and five unchanged root-save tests. The email client produced six tests on each of the same five targets, for 30 tests. The email server produced 81 JVM tests. Users common produced 18 JVM tests and five tests on each JS Node, JS browser, Android debug, and Android release target, for 38 tests. All named target totals have zero failures, errors, and skips.

The 48 discovered shared owner-email cases are `alreadyApprovedRefreshedReplacementDoesNotSend`, `alreadyApprovedResultWithFinalPendingProfileCannotClaimApproval`, `anonymousNonOwnerAndRootOtherCannotAccessPrivateEmail`, `approvedOwnerEmailCanBeReplacedAndVerified`, `blankAndInvalidReplacementNeverDispatch`, `changedMissingWrongOwnerOrFailedFinalReadSuppressesSuccess`, `defaultUiDispatcherSuppressesLateRefreshAfterIdentityInvalidation`, `disabledSmtpOwnerCanReplaceApprovedEmailWithoutSending`, `disabledSmtpPendingAndMissingEmailCanBeSavedWithoutSending`, `doubleSubmitAdmitsOnlyOneMutation`, `emailChangeWhileBusyDoesNotReplaceSubmittedDraft`, `emailSavePreservesDirtyAdminFields`, `failedEmailSaveDoesNotRequestVerification`, `failedPostReconciliationPreservesDeliveryFailureAndBlocksMutation`, `failedReconciliationPreservesPrimaryErrorAndBlocksMutation`, `falseReplacementPreservesDraftAndSaveFailure`, `identityLossDuringPutPreventsPostAndRefresh`, `initialProbeFailureExposesRetryAndRecovers`, `lateCancelledRefreshCannotPublish`, `logoutDuringPostSuppressesOldResult`, `logoutExitsWithoutDirtyConfirmation`, `mismatchedInitialPrivateProfileFailsClosed`, `missingOwnerEmailIsSavedThenVerified`, `nodeDestroySuppressesNonCooperativeEmailContinuation`, `oldFinallyCannotClearNewBusy`, `ownerLeaveAndReturnDoesNotReviveOperation`, `pendingOwnerEmailCanBeReplacedAndVerified`, `pendingOwnerEmailCanRetryVerificationWithoutSavingAgain`, `postThrowAfterConfirmedSaveReportsDeliveryFailure`, `privateReadFailureExposesRetryAndPreservesDraft`, `queuedReplacementRejectsNodeRetarget`, `queuedSaveRejectsChangedOwner`, `rawOwnerAndTargetChecksRejectLaggingEligibility`, `refreshAndResumePreserveReplacementDraft`, `refreshDuringMutationIsCoalesced`, `replacementThrowAfterCommitDoesNotReportSuccess`, `replacementThrowBeforeCommitPreservesDraft`, `replacementUsesExactEmailEquality`, `resendUsesSavedEmailAndPreservesReplacementDraft`, `retargetDuringPostSuppressesFinalPublication`, `retargetDuringPrivateRefreshPreventsPostAndPublication`, `retargetDuringPutPreventsSuccessorRequests`, `sameOwnerRootHasTheSameEmailControls`, `successfulPutWithMissingMismatchedOrFailedProfileNeverSends`, `targetLeaveAndReturnDoesNotReviveOldMutation`, `unchangedNormalizedEmailNeverDispatches`, `verificationResultsPublishOnlyAfterFinalMatchingRefresh`, and `verificationWaitsForMatchingPrivateRefresh`.

These fresh names directly cover approved and pending replacement, enabled and disabled SMTP, PUT/private-GET/exact-recipient-POST/final-private-GET ordering, saved-versus-draft state, false and thrown persistence results, draft retention, delivery separation, double submission, raw caller/authorization/live-config checks, retargeting at every suspension boundary, root-other privacy, old-finally isolation, logout, and non-cooperative continuation suppression after real node destruction.

Fresh `KtorEmailFeatureTest` XML contains `setMyEmailPutsOnlyEmailAndReturnsStatusSuccess` and `setMyEmailNetworkFailurePropagates`; source assertions require one PUT, relative `/email/myEmail`, JSON content type, the exact sole `email` key and replacement value, true for 200/204, false for 409/500, and propagated network failure. Fresh `EmailRoutingsConfiguratorTest` XML contains `setEmailRejectsMissingAndInvalidBearerBeforeCallingFeature`, `setEmailUsesBearerCallerDespiteForgedAccountSelectors`, `setEmailMapsFeatureBooleanOutcomeToExistingStatuses`, `setEmailMapsDuplicateToConflict`, and `setEmailRejectsMalformedOrInvalidBodyBeforeCallingFeature`, plus the three bearer/status verification-route cases. Fresh `ExposedUsersRepoSqliteTest` XML contains all 12 repository cases, including `emailApprovalTracksOnlyTheCurrentStoredAddress`, `bulkUpdatesPreserveOrResetCurrentAddressApproval`, `failedBulkUpdateLeavesRowsUnchanged`, `conditionalApprovalEmitsOnlySuccessfulUpdates`, and `updateDuplicateEmailMapsToDuplicateUserFieldException`.

### Independent source acceptance audit

The shared ViewModel accepts a valid draft that differs exactly from a matching private profile's saved email, regardless of whether SMTP delivery is enabled or disabled. A disabled capability still permits PUT plus checked private GET and never permits a verification POST. An enabled save performs acknowledged PUT, a checked same-owner private GET containing the exact submitted address, optional exact-recipient POST only while pending, and a final checked private GET before publishing storage or positive delivery feedback. A false result, 409-derived false, or ordinary transport exception records `SaveFailed`, preserves the draft, reconciles without automatic POST, and cannot publish storage success. Resend captures only the authoritative saved pending address, never the replacement draft.

Private admission and every continuation are guarded by authorization, current caller, bound user ID, live node target, owner generation, mutation identity, refresh version, lifecycle/cancellation state, and checked private-profile identity. Session or target invalidation clears address-bearing private state, prevents successor calls and stale publication, and keeps old `finally` completion from clearing a newer mutation's busy state. Root status does not participate in the private owner predicate, so root editing another account receives neither private email state nor controls.

The JS, JVM, and Android renderers all show an authoritative disabled saved-address field and approval status separately from an editable draft field for missing, pending, and approved states. Each renderer uses shared save and resend eligibility, enabled-versus-disabled labels, saved-address-only resend visibility, explicit error/success text, and the owner/live-target visibility gate. JVM and Android Done IME callbacks invoke the same guarded `onSaveEmail`; JS intentionally has no implicit Enter submission. English and Russian resources cover every new label and message. KDocs and the users README accurately state replacement, storage/delivery separation, exact-address confirmation, uncertainty, privacy, lifecycle, and host limits. The removed PR #80 subtitle `Подтверждение email` remains absent.

The production client sends only `SetEmailRequest(email)` and returns HTTP status success honestly. The route obtains the target only from the bearer caller and maps duplicate storage to 409. Enabled and disabled server implementations both delegate storage to the same coordinator. `ExposedUsersRepo` preserves approval only for the unchanged current non-null address, resets approval for replacement or clear, and conditionally approves only the exact current address. The UI model delegates fresh private reads and email operations without retargeting arguments.

### Integrity and scope

The ignored `kotlin-js-store/yarn.lock` is 132,521 bytes and 2,882 lines with SHA-256 `6a4fc52389ddb0209d9ea4065b5e9bfba7754a1343dd213ac6c25a778770723d`. It matches the exact current `build/js/yarn.lock` byte-for-byte and contains no `jsdom` stanza. The issue-79 diff against base `a2dc2202535795620f5891e1b7389feeaa457a6e` changes no Gradle file, settings file, version catalog, package manifest, or lockfile. The quarantined `/tmp/issue79-stale-yarn.lock` was not used as repository evidence.

The branch merge base is `a2dc2202535795620f5891e1b7389feeaa457a6e`, and the pre-report branch scope is exactly the task prompt, seven prior reports, ten issue-79 source/test/documentation files, and no dependency, schema, endpoint, or issue-78 production change. `git diff --check a2dc220..HEAD` passes. The pre-report worktree remained clean after the full build. The AST index is current after the latest Kotlin commit and reports 785 files, 6,570 symbols, 26,866 references, 49 modules, one XML usage, and four resources; searches resolve `onSaveEmail`, the three renderer call sites, transport tests, route tests, and repository ownership symbols.

The current users README Operator Notes section is byte-identical to commit `cc8b63934086a41fb4c5965ce30b68775a67b67e`; both compared byte streams have SHA-256 `67bacb0f982e1f2417f3e2f20567deef209f85d4fa2412643362502edffac4a8`.

### Evidence limits and routing

JS browser and JS Node executions ran common ViewModel/client tests; neither execution is DOM renderer interaction proof. JVM and Android renderer sources compiled, and Android shared tests ran under debug and release unit-test tasks; compilation and unit execution do not prove desktop semantics, native widgets, Android device behavior, or Done IME interaction. No live SMTP service, deployed HTTP server, physical device, or external database was exercised. The navigation dependency keeps the exact lifecycle-flow subscriber timing private; the accepted test instead proves stale continuation suppression after an observed real node transition to `NEW`. HTTP cancellation cannot roll back a PUT already accepted by the server, same-owner credential epochs are not observable, and cross-client updates retain the existing server-serialized behavior. These bounded limitations do not contradict issue #79 acceptance and are not presented as live integration proof.

The full build and all fresh tests pass, source acceptance and integrity audits pass, and no Verification blocker remains. Route the task to independent Validating.

```text
ENTITY:
entity_id=issue_79_verification_cycle1; type=full_repository_verification; state=pass_ready_for_validating

CONTEXT:
* task_id=10.09.2026_16.32.33-2f63e0e4-5fa6-4bb0-819c-3c99ea86af3d; agent_id=issue79_verification_cycle1; memory_ref=[PROMPT.md,001-planning.md,002-architecturing.md,003-coding.md,004-coding.md,005-coding.md,006-coding.md,007-coding.md,008-verification.md]
* constraints=[validation_only,single_foreground_Gradle,serial_execution,fresh_XML_only,Operator_Notes_byte_preservation]; exclusions=[production_edits,test_edits,documentation_edits,dependency_edits,live_integration_claims]

ACTION:
1. action=run_full_repository_build; target=issue_79_verification_cycle1; params={command:gradlew_no_parallel_build_rerun_tasks_console_plain,exit_code:0,actionable_tasks:4530,terminal:BUILD_SUCCESSFUL}
2. action=aggregate_fresh_XML; target=issue_79_verification_cycle1; params={suites:159,tests:898,failures:0,errors:0,skipped:0}
3. action=audit_acceptance_and_integrity; target=issue_79_verification_cycle1; params={source_acceptance:pass,lock_graph:pass,scope:pass,AST_currency:pass,Operator_Notes:pass}

REASON:
* condition=issue_79_requires_existing_owner_email_replacement_without_false_success; requirement=full_build_plus_cross_target_business_and_boundary_proof; causal_chain=single_full_build→fresh_XML_and_source_audit→verification_PASS
* condition=private_email_work_crosses_identity_target_and_suspension_boundaries; requirement=raw_guards_plus_generation_token_and_cancellation_evidence; causal_chain=named_race_tests_plus_source_checks→stale_successor_and_publication_suppression→owner_privacy_preserved

EXPECTED RESULT:
* entity_id=issue_79_verification_cycle1; new_state=verification_pass_committed_for_independent_validation; location=agents/task/10.09.2026_16.32.33-2f63e0e4-5fa6-4bb0-819c-3c99ea86af3d/008-verification.md

VERIFICATION:
* check=prescribed_full_build; expected=exit_0_and_executed_tests; observed=exit_0_BUILD_SUCCESSFUL_4530_tasks_and_898_tests
* check=issue_79_target_matrix; expected=48_shared_cases_on_executed_JVM_JS_Android_targets; observed=48_identical_normalized_names_on_JVM_JS_Node_JS_browser_Android_debug_Android_release
* check=transport_route_repository_acceptance; expected=exact_body_status_bearer_caller_409_approval_reset_stale_approval_duplicate_rollback; observed=all_named_fresh_XML_cases_green
* check=static_integrity; expected=clean_scope_current_AST_no_issue78_jsdom_unchanged_Operator_Notes; observed=all_checks_pass

UNCERTAINTY:
* missing=DOM_desktop_semantics_Android_device_and_IME_execution; ambiguity=platform_compilation_and_common_test_execution_do_not_prove_native_rendering; accepted_scope=shared_business_tests_plus_platform_compilation
* missing=live_SMTP_deployed_server_and_external_database_execution; ambiguity=mock_and_in_process_results_do_not_prove_external_services; accepted_scope=transport_route_service_coordinator_and_SQLite_automation
* missing=dependency_private_lifecycle_subscription_timing_and_same_owner_credential_epoch; ambiguity=exact_internal_timing_and_epoch_unobservable; accepted_scope=observed_NEW_transition_raw_session_guards_and_stale_continuation_suppression

REPETITION OF RESULT:
* entity_id=issue_79_verification_cycle1; stored_in=shared_memory; status=available; result=PASS_ready_for_validating

COMMUNICATION:
* sender=issue79_verification_cycle1; receiver=orchestrator_and_validating; task_id=10.09.2026_16.32.33-2f63e0e4-5fa6-4bb0-819c-3c99ea86af3d; message_id=7bbed3eb-cd17-4791-81fe-dc59049fd1df; protocol=AML-HIP

PERSISTENCE:
* local_memory=true; shared_memory=true; index_keys=[task_id,issue_79_verification_cycle1,build_result,fresh_XML,acceptance_audit]; storage=008-verification.md

VALIDATION:
* format_valid=true; no_pronouns=true; entities_explicit=true; high_density=true; causal_chain_present=true; ambiguity_detected=true; ambiguity_resolution=host_live_service_and_dependency_private_limits_explicit
```
