Model: OpenAI GPT-5.6 Terra (ML)
Changed files: features/auth/server/src/commonMain/kotlin/services/AuthFeatureService.kt, features/auth/server/src/commonTest/kotlin/services/AuthFeatureServiceTest.kt, features/auth/README.md, agents/task/03.09.2026_07.37.13-11a2ee6a-f254-4e30-9d7f-3af7c441f87a/009-coding.md

Model rationale: The Coding role prefers an ML model. Terra was sufficient for the bounded coroutine-consistency change, deterministic test fixture, documentation update, and focused verification required by the architecture.

# Follow-up coding report

## Implementation

Required-email registration now checks the parent coroutine before durable creation, executes account creation and immediate transaction rollback enrollment within one bounded `NonCancellable` region, and checks the restored parent context before interpreting a nullable result or beginning the pending-role transition. The transaction DSL receiver is captured explicitly, so the rollback action is installed on the original registration transaction before deferred cancellation can propagate.

The nullable result ordering preserves an active duplicate registration as `null` while ensuring a cancellation racing with a nullable repository return still propagates. Optional-email registration, public APIs, later rollback ordering, invite handling, and SQLite exception classification were not changed.

## Regression coverage

A `GatedCreateUsersRepo` fixture now persists the provisional user, exposes the commit-equivalent point, and delays returning the created identity until the test releases the gate. The regression first failed against the previous service at the assertion that registration must remain incomplete after cancellation, directly reproducing the orphan window. With the production change, the test proves registration remains cancelling until repository return, compensation deletes the user without a lock deadlock, cancellation is propagated, pending and delivery work never starts, password state remains empty, role cleanup runs once, and the recorded sequence is `persisted`, `repositoryReturned`, `userDeleted`.

The Auth feature README now documents the bounded consistency region, required repository-return guarantee, unchanged optional path, and remaining process-death or identity-withholding limitations without changing Operator Notes.

## Verification

The focused new JVM regression passed. The complete Auth server JVM test task passed. The Auth server module build, including `allTests`, passed. `git diff --check` passed, and the mandatory post-source-change `ast-index rebuild` completed successfully. Gradle emitted only the repository's existing deprecation warnings.

## Verification handoff

ENTITY:
entity_id=required_email_create_enrollment; type=cancellation_consistency_boundary; state=implemented_and_coding_verified

CONTEXT:

* task_id=03.09.2026_07.37.13-11a2ee6a-f254-4e30-9d7f-3af7c441f87a; agent_id=coding; memory_ref=[PROMPT.md,007-planning.md,008-architecturing.md,AuthFeatureService.kt,AuthFeatureServiceTest.kt,features/auth/README.md]
* constraints=[finding_8_only,required_email_scope,existing_public_API,existing_reverse_rollback_order]; exclusions=[finding_4_implementation,optional_registration_redesign,repository_API_change]

ACTION:

1. action=guard_pre_create_cancellation; target=AuthFeatureService.registerWithRequiredEmail; params={operation=currentCoroutineContext.ensureActive,position=before_durable_create,write_started=false}
2. action=protect_create_to_enrollment; target=required_email_create_enrollment; params={context=NonCancellable,operations=[createUserOrNull,transaction.rollableBackOperation],transaction_receiver=captured_original}
3. action=guard_post_enrollment_cancellation; target=AuthFeatureService.registerWithRequiredEmail; params={operation=currentCoroutineContext.ensureActive,position=before_nullable_return_and_markPending,rollback_owner=installed_for_non_null_create}
4. action=add_deterministic_regression; target=AuthFeatureServiceTest.requiredEmailRegistrationCancellationAfterPersistedCreateCompensatesUser; params={gate=[persisted,release],events=[persisted,repositoryReturned,userDeleted],timeout_ms=1000}
5. action=document_consistency_contract; target=features/auth/README.md; params={section=Architecture_Notes,operator_notes_modified=false,optional_registration_changed=false}
6. action=preserve_recommendation_only_scope; target=ExposedUsersRepo.isUniqueViolation; params={source_change=false,test_change=false,finding_4_implemented=false}

REASON:

* condition=user_insert_commit_before_cancellable_repository_return; requirement=user_compensation_enrollment_before_parent_cancellation_escape
* condition=nullable_duplicate_result_plus_concurrent_cancellation; requirement=post_region_active_check_before_nullable_registration_return

EXPECTED RESULT:

* entity_id=required_email_create_enrollment; new_state=parent_cancellation_safe_after_committed_insert; location=AuthFeatureService.registerWithRequiredEmail
* entity_id=required_email_cancellation_regression; new_state=deterministic_and_deadlock_bounded; location=AuthFeatureServiceTest.requiredEmailRegistrationCancellationAfterPersistedCreateCompensatesUser
* entity_id=sqlite_unique_classifier; new_state=recommendation_only_and_source_unchanged; location=ExposedUsersRepo.isUniqueViolation

VERIFICATION:

* check=baseline_regression_without_production_fix; expected=FAILED_at_pre_release_registration_completion_assertion
* check=focused_regression_with_production_fix; expected=BUILD_SUCCESSFUL
* check=./gradlew_:wishlist.features.auth.server:jvmTest; expected=BUILD_SUCCESSFUL
* check=./gradlew_:wishlist.features.auth.server:build; expected=BUILD_SUCCESSFUL_plus_allTests_passed
* check=git_diff_check; expected=no_whitespace_errors
* check=ast-index_rebuild_after_source_change; expected=1391_files_indexed_plus_success

UNCERTAINTY:

* missing=repository_commit_receipt; ambiguity=post_commit_exception_without_returned_identity_remains_uncompensated
* missing=durable_recovery_record; ambiguity=process_termination_prevents_in_process_rollback
* missing=optional_registration_transaction_owner; ambiguity=optional_mode_cancellation_compensation_outside_authorized_scope

REPETITION OF RESULT:

* entity_id=required_email_create_enrollment; stored_in=shared_memory; status=available_for_verification

COMMUNICATION:

* sender=coding; receiver=verification; task_id=03.09.2026_07.37.13-11a2ee6a-f254-4e30-9d7f-3af7c441f87a; message_id=44cc8fc9-bfe5-4b1b-916c-dd9a69bf3a63; protocol=AML-HIP

PERSISTENCE:

* local_memory=false; shared_memory=true; index_keys=[task_id,required_email_create_enrollment,required_email_cancellation_regression,sqlite_unique_classifier]

VALIDATION:

* format_valid=true; no_pronouns=true; entities_explicit=true; high_density=true
* causal_chain_present=true; ambiguity_detected=false; validation_scope=verification_handoff
