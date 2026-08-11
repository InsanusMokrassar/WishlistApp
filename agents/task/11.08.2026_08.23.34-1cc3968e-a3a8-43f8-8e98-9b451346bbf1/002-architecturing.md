Model: OpenAI GPT-5.6 Sol (HL)
Changed files: agents/task/11.08.2026_08.23.34-1cc3968e-a3a8-43f8-8e98-9b451346bbf1/002-architecturing.md

The Architecture role prefers an HL model. This small refactor still requires high-confidence reasoning about cancellation, transaction-result classification, rollback registration timing, and lock re-entry, so OpenAI GPT-5.6 Sol is the best available fit.

## Architectural decision

Keep the public `AuthFeatureService.register` contract, the three-phase required-email flow, and every existing dependency boundary unchanged. Refactor only the private orchestration in `registerWithRequiredEmail` so one MicroUtils transaction owns post-reservation compensation. No endpoint, DTO, repository interface, DI binding, or role-lifecycle contract changes are needed.

Add the `rollableBackOperation` import next to `doSuspendTransaction`. Add a private exception type inside `AuthFeatureService` whose sole purpose is to represent an expected required-email rejection after a provisional user has been created. The type must remain private and must never cross the service boundary: transaction-result mapping converts an unsuppressed instance back to `null` after rollback has completed.

The transaction block should preserve these boundaries. The first `locker.withWriteLock` checks the username, attempts the narrow user create, registers the successful created user with `rollableBackOperation`, and calls `roleLifecycle.markPending`. Registration of the rollback must happen immediately after creation and before the pending transition. A pre-existing username or `createUserOrNull` uniqueness result exits the transaction successfully with `null`, because no provisional user exists. A false pending result or an ordinary `Exception` from the pending collaborator throws the private rejection marker; cancellation and non-`Exception` failures are rethrown unchanged.

The BCrypt hash and `sender.sendRegistrationEmail` remain outside the global auth lock. A false sender result or an ordinary sender `Exception` throws the rejection marker. Cancellation and unexpected failures remain transaction failures. The final `locker.withWriteLock` re-reads the same user id and compares the stored email with the requested email before writing the password and issuing credentials. A missing user or changed email throws the rejection marker. Password-store, credential-issuance, repository, hashing, cancellation, and other unexpected failures propagate after rollback.

The rollback lambda calls `compensateRequiredRegistration` with the created user's id and the same lifecycle. Because `doSuspendTransaction` begins rollback only after the transaction block has failed, an exception thrown inside either locked phase has already unwound and released the write lock before compensation reacquires it. The existing `NonCancellable` compensation boundary remains the owner of user deletion, auth-state purge, and direct-role removal.

Do not allow rollback cleanup failures to disappear into `doSuspendTransaction`'s default no-op rollback-error callback. The rollback context exposes the initiating error; if compensation throws, attach the cleanup failure to that initiating error. Result mapping must propagate cancellation and unexpected initiating failures with the cleanup failure suppressed. If the initiating error is the private expected-rejection marker and cleanup failed, propagate the cleanup failure instead of returning `null`, because a failed compensation cannot satisfy the registration-refusal contract.

After result classification is in place, delete the mutable `reservedUserId`, the repeated phase-specific compensation catches, and `compensateAfterCancellation`. Retain `compensateRequiredRegistration` unchanged unless a minimal signature adjustment is required. Preserve all pre-existing operator edits outside the method, including the current `createUserOrNull` changes and logging additions.

## Failure classification

Before reservation, an existing username or `DuplicateUserFieldException` translated by `createUserOrNull` remains a successful `null` result and registers no rollback. After reservation, false or ordinary-exception results from the pending-role and delivery collaborators, plus final user/email validation failure, become internal expected-rejection failures that roll back and then map to `null`. Cancellation rolls back in the non-cancellable compensation context and is rethrown. BCrypt, repository, auth-state, and other unexpected failures roll back and are rethrown. A compensation failure is never silently converted to `null`.

## Test specification

The existing `requiredEmailRegistrationSendsInviteBeforeReturningCredentials` test remains the success specification: valid inputs with successful pending transition and delivery return credentials, persist the password only after delivery, retain the user, and retain the pending direct role.

The existing `requiredEmailRegistrationCompensatesFailedPendingTransition`, `requiredEmailRegistrationHidesCredentialsWhenInviteFails`, and `requiredEmailRegistrationCompensatesSenderException` tests remain the expected-failure specifications. False pending, false delivery, and an ordinary delivery exception must each return `null`; every post-reservation case must remove the provisional user, password/auth state, and direct roles; delivery must not run when the pending transition fails.

The existing `requiredEmailRegistrationCleansUpFailedInviteAndAllowsRetry` test remains the rollback completeness specification: a failed first attempt leaves no username, password, or role residue, and the same username and email can succeed on a later attempt.

The existing `requiredEmailDeliveryRunsOutsideGlobalAuthLock` test remains the lock-partition specification: while delivery is suspended, an unrelated login completes within the timeout, the provisional account has no password and cannot authenticate, and successful delivery permits final credential installation.

The existing `requiredEmailDeliveryCancellationPropagatesAfterCompensation` test remains the cancellation specification: cancellation during delivery must reach the caller only after user, password/auth state, and role cleanup has completed.

The existing `duplicateEmailRegistrationUsesExistingFailureContract`, missing-email, and missing-infrastructure tests remain the pre-reservation specifications: each returns `null` without delivery or role mutation, and an existing account is untouched.

Add a focused regression test named `requiredEmailRegistrationPropagatesUnexpectedFinalizationFailureAfterCompensation`. Supply a `PasswordsRepo` test double whose `set` throws a known exception after pending transition and delivery succeed. Assert that the same exception reaches the caller, the provisional user is deleted, no password or session residue remains, and direct roles are removed. Generalize the test builder's password repository parameter from the concrete fake to `PasswordsRepo` if needed.

Add a focused regression test named `requiredEmailRegistrationCancellationRetainsCleanupFailure`. Supply a sender that throws a known `CancellationException` and a lifecycle whose `removeRoles` throws a known cleanup exception. Assert that registration throws the same cancellation, the cleanup exception is present in its suppressed failures, and deletion plus auth purge ran before the role cleanup failed. This directly protects the result-mapping behavior that replaces `compensateAfterCancellation`.

All planned behavior is automatable with common tests; no operator decision about untestable functionality is required. Run `./gradlew :wishlist.features.auth.server:jvmTest` and `./gradlew :wishlist.features.email.server:jvmTest`. Rebuild the AST index after Kotlin changes, and verify the final diff contains only the intended service, test additions, any justified Auth README delta, and the Coding step report.

## README updates

No Auth README change is required. The existing required-email Architecture Notes already describe the externally relevant compensated phases, lock partition, failure mapping, cleanup, cancellation propagation, and retry invariant. The transaction DSL and private marker are implementation mechanisms rather than new public architecture.

## Architecture handoff

ENTITY:
entity_id=auth_required_email_transaction_architecture; type=service_orchestration_refactor; state=specified

CONTEXT:

* task_id=11.08.2026_08.23.34-1cc3968e-a3a8-43f8-8e98-9b451346bbf1; agent_id=architecturing; memory_ref=[PROMPT.md,001-planning.md,features/auth/README.md,AuthFeatureService.kt,TransactionsDSL.kt,AuthFeatureServiceTest.kt,RegistrationCompensationIntegrationTest.kt]
* constraints=[single_transaction_rollback,rollback_registration_immediately_after_create,delivery_outside_auth_lock,non_cancellable_compensation,expected_failure_returns_null,cancellation_propagates,unexpected_failure_propagates,rollback_failure_not_silenced,pre_existing_source_edits_preserved]

ACTION:

1. action=refactor; target=AuthFeatureService.registerWithRequiredEmail; params={transaction=doSuspendTransaction,rollback=rollableBackOperation,expected_failure_marker=private_exception,result_mapping=[success_value,expected_rejection_to_null,cancellation_rethrow,unexpected_rethrow]}
2. action=remove; target=AuthFeatureService.manual_compensation_control_flow; params={remove=[reservedUserId,phase_specific_compensation_catches,compensateAfterCancellation],retain=[compensateRequiredRegistration,lock_partitioning,createUserOrNull_contract]}
3. action=test; target=AuthFeatureServiceTest; params={retain=[success,failed_pending,failed_delivery,sender_exception,retry,lock_release,cancellation,duplicate],add=[unexpected_finalization_failure,cancellation_cleanup_failure]}

REASON:

* condition=successful_provisional_user_creation_followed_by_failure; requirement=exactly_one_registered_compensation_path_after_lock_unwind
* condition=expected_post_reservation_refusal_represented_as_nullable_success; requirement=internal_failure_marker_triggers_rollback_before_public_null_mapping
* condition=transaction_rollback_action_throws; requirement=cleanup_failure_preserved_and_never_silently_mapped_to_null

EXPECTED RESULT:

* entity_id=auth_required_email_transaction_architecture; new_state=single_transaction_controls_required_email_compensation; location=features/auth/server/src/commonMain/kotlin/services/AuthFeatureService.kt
* entity_id=auth_required_email_transaction_tests; new_state=behavior_and_failure_mapping_covered; location=features/auth/server/src/commonTest/kotlin/services/AuthFeatureServiceTest.kt

VERIFICATION:

* check=auth_server_jvm_tests; expected=all_tests_pass_including_transaction_regressions
* check=email_server_jvm_tests; expected=registration_compensation_integration_passes
* check=ast_index_rebuild; expected=updated_symbols_and_references_after_kotlin_changes
* check=git_diff_scope; expected=[AuthFeatureService.kt,AuthFeatureServiceTest.kt,coding_step_report]

UNCERTAINTY:

* missing=[]; ambiguity=none

REPETITION OF RESULT:

* entity_id=auth_required_email_transaction_architecture; stored_in=shared_step_file; status=available_for_coding

COMMUNICATION:

* sender=architecturing; receiver=coding; task_id=11.08.2026_08.23.34-1cc3968e-a3a8-43f8-8e98-9b451346bbf1; message_id=71af203f-aab1-4c39-a078-c1757bed3523; protocol=AML-HIP

PERSISTENCE:

* local_memory=false; shared_memory=true; index_keys=[task_id,entity_id,intent]

VALIDATION:

* format_valid=true; no_pronouns=true; entities_explicit=true; high_density=true; causal_chain_present=true; ambiguity_detected=false
