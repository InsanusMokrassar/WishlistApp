Model: OpenAI GPT-5.6 Terra (ML)
Changed files: features/auth/server/src/commonMain/kotlin/services/AuthFeatureService.kt; features/auth/server/src/commonTest/kotlin/services/AuthFeatureServiceTest.kt; agents/task/11.08.2026_08.23.34-1cc3968e-a3a8-43f8-8e98-9b451346bbf1/003-coding.md

The Coding role prioritizes an ML model. OpenAI GPT-5.6 Terra is the appropriate available ML model for this contained Kotlin refactor and its focused transactional failure tests; the architecture already fixed the higher-risk behavioral decisions.

## Implementation

`registerWithRequiredEmail` now runs inside `doSuspendTransaction`. Immediately after a provisional account is created, `rollableBackOperation` registers its non-cancellable cleanup. The pending-role transition, invite delivery, and final user/email validation preserve the original phase and lock boundaries. Delivery and BCrypt hashing remain outside the global auth write lock.

Expected post-reservation failures use a private marker exception so rollback runs before the public result is mapped to `null`. Cancellation and unexpected failures are rethrown after rollback. Rollback cleanup failures are attached to the initiating failure; an expected rejection with a cleanup failure propagates that cleanup failure rather than returning `null`. The obsolete per-phase compensation catches, mutable reserved-user tracking, and cancellation helper were removed.

Added regressions cover an unexpected password-store failure during finalization and a delivery cancellation whose role cleanup fails. The test builder now accepts the `PasswordsRepo` interface so each behavior can supply its own focused double.

No Auth README change was needed: the architecture report confirmed that the externally documented flow and contracts remain unchanged.

## Baseline preservation

Before editing, `AuthFeatureService.kt` already had unstaged operator work: KSLog imports and create-failure logging, a direct `create(NewUser(...))` call, an incomplete empty transaction block, and local username/create-result control-flow changes. The transaction block was completed as task work. The logging and `createUserOrNull` changes remain preserved as unrelated unstaged work and will not be included in the Coding commit.

## Verification

- `./gradlew :wishlist.features.auth.server:jvmTest` — passed.
- `./gradlew :wishlist.features.email.server:jvmTest` — passed.
- `./gradlew :wishlist.features.auth.server:build` — passed.
- `ast-index rebuild` — passed; refreshed the WishlistApp and MicroUtils roots.
- `git diff --check` — passed before staging.

## Coding handoff

ENTITY:
entity_id=auth_required_email_transaction_refactor; type=code_refactor; state=implemented

CONTEXT:

* task_id=11.08.2026_08.23.34-1cc3968e-a3a8-43f8-8e98-9b451346bbf1; agent_id=coding; memory_ref=[002-architecturing.md,AuthFeatureService.kt,AuthFeatureServiceTest.kt,TransactionsDSL.kt]
* constraints=[single_transaction_rollback,delivery_outside_global_auth_lock,expected_failure_to_null,cancellation_rethrow,unexpected_failure_rethrow,cleanup_failure_preservation,operator_worktree_preservation]

ACTION:

1. action=refactor; target=AuthFeatureService.registerWithRequiredEmail; params={transaction=doSuspendTransaction,rollback=rollableBackOperation,rollback_registration_timing=post_create_pre_pending,expected_failure_marker=RequiredEmailRegistrationRejected}
2. action=remove; target=AuthFeatureService.manual_compensation_control_flow; params={removed=[reservedUserId,phase_specific_catches,compensateAfterCancellation],retained=[compensateRequiredRegistration,NonCancellable_cleanup,lock_partitioning]}
3. action=test; target=AuthFeatureServiceTest; params={added=[unexpected_finalization_failure,cancellation_cleanup_failure],builder_password_repo_type=PasswordsRepo}

REASON:

* condition=provisional_account_created_then_phase_fails; requirement=registered_rollback_removes_user_purges_auth_state_removes_direct_roles
* condition=rollback_cleanup_failure; requirement=initiating_failure_suppressed_contains_cleanup_failure_and_expected_rejection_does_not_map_to_null

EXPECTED RESULT:

* entity_id=auth_required_email_transaction_refactor; new_state=single_transaction_controls_compensation; location=features/auth/server/src/commonMain/kotlin/services/AuthFeatureService.kt
* entity_id=auth_required_email_transaction_regressions; new_state=finalization_and_cleanup_failure_coverage_present; location=features/auth/server/src/commonTest/kotlin/services/AuthFeatureServiceTest.kt

VERIFICATION:

* check=auth_server_jvmTest; expected=passed
* check=email_server_jvmTest; expected=passed
* check=auth_server_build; expected=passed
* check=ast_index_rebuild; expected=passed

UNCERTAINTY:

* missing=[]; ambiguity=none

REPETITION OF RESULT:

* entity_id=auth_required_email_transaction_refactor; stored_in=shared_step_file; status=available_for_verification

COMMUNICATION:

* sender=coding; receiver=verification; task_id=11.08.2026_08.23.34-1cc3968e-a3a8-43f8-8e98-9b451346bbf1; message_id=818748b1-5da1-42bf-93b4-10255a1098ec; protocol=AML-HIP

PERSISTENCE:

* local_memory=false; shared_memory=true; index_keys=[task_id,entity_id,intent]

VALIDATION:

* format_valid=true; no_pronouns=true; entities_explicit=true; high_density=true; causal_chain_present=true; ambiguity_detected=false
