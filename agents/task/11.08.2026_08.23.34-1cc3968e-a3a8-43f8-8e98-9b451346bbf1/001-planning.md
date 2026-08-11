Model: OpenAI GPT-5.6 Sol (HL)
Changed files: agents/task/11.08.2026_08.23.34-1cc3968e-a3a8-43f8-8e98-9b451346bbf1/001-planning.md

The Planning role prefers an HL model, and this refactor needs careful reasoning about transactional rollback, cancellation, exception mapping, and lock boundaries even though the source change is small. OpenAI GPT-5.6 Sol satisfies that priority and risk profile.

## Task understanding

Rewrite only the required-email registration workflow in `AuthFeatureService` around MicroUtils' `doSuspendTransaction` and `rollableBackOperation`. The refactor should replace the repeated phase-specific compensation catches with one rollback registration while preserving the public behavior documented by the Auth feature: duplicate or expected registration failures return `null`, a provisional account never receives credentials before successful delivery, email delivery runs outside the global auth write lock, every failure after account reservation removes the user, auth state, and direct roles, cancellation performs compensation and propagates, and unexpected repository or hashing failures still propagate.

The working tree already contains operator work in `AuthFeatureService.kt`, including an empty `doSuspendTransaction` block and changes outside the target method. Coding must preserve those pre-existing edits and avoid staging or reverting unrelated hunks.

## Investigation

The Auth feature README defines required-email registration as a compensated two-phase flow. `registerWithRequiredEmail` currently reserves a user and marks the role state under the lock, hashes and delivers outside the lock, then validates the same stored email and installs the password and tokens under the lock. Three separate catch/cleanup regions and a cancellation-specific helper implement rollback today.

MicroUtils' `doSuspendTransaction` returns `Result<T>`, runs registered `rollableBackOperation` rollback actions in reverse order for every thrown `Throwable`, and leaves successful nullable results untouched. Therefore, rollback must be registered immediately after successful provisional-user creation, expected post-reservation failures must become an internal transaction failure rather than a successful `null`, and the final `Result` mapping must distinguish the expected failure marker from cancellation and unexpected errors.

The existing Auth service tests already cover successful required-email registration, failed delivery, retry after cleanup, failed pending transition, ordinary sender exceptions, delivery outside the auth lock, cancellation propagation after cleanup, and duplicate-email behavior. The Email integration test additionally verifies cleanup across user, password, deep-link, and role stores. No new behavior is requested, so source-test edits are unnecessary unless implementation reveals a previously unrepresented regression.

## Open questions and answers

No unclear architecture decisions, requirements, or constraints remain. The prompt, feature README, current implementation, MicroUtils transaction implementation, and existing tests resolve the transaction semantics and validation scope, so no operator questions are required.

## Final plan

### Transaction boundary and rollback

Import `rollableBackOperation` alongside `doSuspendTransaction`, make `registerWithRequiredEmail` return the mapped transaction result, and keep the current lock partitioning. Inside the first locked phase, preserve the duplicate checks and nullable create result. Once creation succeeds, register a rollback operation whose compensation calls `compensateRequiredRegistration` for the created user, then perform the pending-role transition.

### Failure semantics

Represent expected post-reservation failures with a private required-registration failure marker so `doSuspendTransaction` triggers rollback before the marker is converted back to the existing `null` result. Treat a false or ordinary-exception pending transition, a false or ordinary-exception delivery, and a missing or changed user during final validation as expected failures. Let `CancellationException`, BCrypt failures, repository failures, and other unexpected throwables leave the transaction as failures and rethrow after rollback. Preserve cleanup-error attachment for cancellation and avoid silently converting unexpected failures into registration refusal.

### Simplification and verification

Remove the phase-specific compensation catches and delete `compensateAfterCancellation` plus obsolete imports if the transaction result and rollback callback fully replace the helper. Retain `compensateRequiredRegistration` and its non-cancellable locked cleanup. Do not alter optional-email registration or the unrelated pre-existing source edits. Rebuild the AST index after the Kotlin edit, then run the Auth server test suite and the Email server integration test task, or the narrowest equivalent Gradle tasks exposed by the project, to confirm success, expected failure, retry, lock-release, cross-feature cleanup, and cancellation behavior.

## Architecture handoff

ENTITY:
entity_id=auth_required_email_transaction_refactor; type=code_refactor; state=planned

CONTEXT:

* task_id=11.08.2026_08.23.34-1cc3968e-a3a8-43f8-8e98-9b451346bbf1; agent_id=planning; memory_ref=[PROMPT.md,features/auth/README.md,AuthFeatureService.kt,TransactionsDSL.kt,AuthFeatureServiceTest.kt,RegistrationCompensationIntegrationTest.kt]
* constraints=[behavior_preservation,pre_existing_worktree_changes_preservation,email_delivery_outside_global_auth_lock,post_reservation_rollback,cancellation_propagation,unexpected_failure_propagation]

ACTION:

1. action=refactor; target=AuthFeatureService.registerWithRequiredEmail; params={transaction_api=doSuspendTransaction,rollback_api=rollableBackOperation,expected_failure_contract=null,cancellation_contract=rollback_then_rethrow,unexpected_failure_contract=rollback_then_rethrow}
2. action=simplify; target=AuthFeatureService.compensation_control_flow; params={remove=phase_specific_compensation_catches,retain=compensateRequiredRegistration,delete_if_unused=compensateAfterCancellation,preserve=lock_partitioning}
3. action=verify; target=wishlist.features.auth.server; params={ast_index=rebuild,tests=[auth_server_suite,email_registration_compensation_integration],coverage=[success,expected_failure,retry,lock_release,cancellation,cross_feature_cleanup]}

REASON:

* condition=provisional_user_created_and_later_phase_failed; requirement=single_registered_rollback_deletes_user_purges_auth_state_removes_direct_roles
* condition=transaction_failure_classified; requirement=expected_failure_returns_null_and_cancellation_or_unexpected_failure_rethrows

EXPECTED RESULT:

* entity_id=auth_required_email_transaction_refactor; new_state=transaction_dsl_controls_compensation; location=features/auth/server/src/commonMain/kotlin/services/AuthFeatureService.kt
* entity_id=required_email_registration_contract; new_state=behavior_preserved_with_reduced_manual_exception_control_flow; location=features/auth/server/src/commonTest/kotlin/services/AuthFeatureServiceTest.kt

VERIFICATION:

* check=ast_index_rebuild_after_kotlin_change; expected=successful_index_refresh
* check=auth_and_email_registration_tests; expected=all_targeted_tests_pass
* check=git_diff_scope; expected=[AuthFeatureService.kt,coding_step_report]

UNCERTAINTY:

* missing=[]; ambiguity=none

REPETITION OF RESULT:

* entity_id=auth_required_email_transaction_refactor; stored_in=shared_step_file; status=available_for_architecture

COMMUNICATION:

* sender=planning; receiver=architecture; task_id=11.08.2026_08.23.34-1cc3968e-a3a8-43f8-8e98-9b451346bbf1; message_id=cb293ccc-6588-4b70-9ea2-66dde28afe2b; protocol=AML-HIP

PERSISTENCE:

* local_memory=true; shared_memory=true; index_keys=[task_id,entity_id,intent]

VALIDATION:

* format_valid=true; no_pronouns=true; entities_explicit=true; high_density=true; causal_chain_present=true; ambiguity_detected=false
