Model: OpenAI GPT-5 (HL)
Changed files: agents/task/03.09.2026_07.37.13-11a2ee6a-f254-4e30-9d7f-3af7c441f87a/007-planning.md

Model rationale: The follow-up requires precise reasoning about coroutine cancellation, a hidden database commit, transaction-DSL rollback enrollment, lock release, and driver-specific exception chains. The Planning role prefers an HL model, and the failure-ordering proof warrants that level.

# Follow-up planning report

## Task understanding

The operator now authorizes finding 8. The implementation must close only the required-email registration window in which a user insert has committed but `rollableBackOperation` has not yet enrolled provisional-user compensation. Parent cancellation must remain prompt outside the smallest consistency-critical region, must propagate after enrollment and cleanup, and must not change duplicate/null behavior or the established link, role, password, and account rollback order.

Finding 4 remains recommendation-only. No SQLite classifier source or test change belongs in this implementation cycle.

## Current mechanism and failure boundary

`registerWithRequiredEmail` runs inside `doSuspendTransaction`. Under the existing Auth write lock, the code checks the username, calls `createUserOrNull`, and only after that suspending call returns passes the created user through `rollableBackOperation`. The rollback action then owns `compensateRequiredRegistration`, which deletes the user, purges password/session state, and removes direct roles in `NonCancellable`.

The production write can commit before returning. MicroUtils `AbstractExposedWriteCRUDRepo.create` completes the Exposed transaction and then suspends in `onAfterCreate` and `_newObjectsFlow.emit`; the cache wrapper can suspend again while updating its cache. Cancellation in those post-commit operations makes `createUserOrNull` throw before Auth receives the created value, so `doSuspendTransaction` has no user rollback action to execute.

## Authorized implementation plan for finding 8

Keep `createUserOrNull` unchanged and modify only its required-email call site inside the existing Auth lock and transaction. Check the parent context with `currentCoroutineContext().ensureActive()` immediately before entering the protected region so an already-cancelled request never starts a durable insert. Then run both the nullable create and the existing `rollableBackOperation` enrollment inside one `withContext(NonCancellable)` block. A non-null created user must become the rollback action result before the block exits; a null result must leave the transaction without registering a meaningless action. Check the restored parent context with `currentCoroutineContext().ensureActive()` immediately after the block and before `markPending`.

The post-check is deliberate even though `withContext` may already observe cancellation while restoring the parent context. The explicit check documents and guarantees the service contract independently of undispatched context-switch details: cancellation received during the protected region is surfaced before the pending-role transition, BCrypt work, SMTP delivery, or password finalization. If cancellation is thrown while leaving `withContext`, enrollment has already happened. If cancellation is first visible at the explicit post-check, enrollment has also already happened.

The protected region remains bounded to user creation plus synchronous rollback enrollment. It stays inside the current Auth write lock, so no new interleaving is introduced. Parent cancellation cannot abort the region; therefore the underlying repository must eventually finish its post-commit notification/cache work. Once the region exits, cancellation unwinds the lock before `doSuspendTransaction` invokes rollback. `compensateRequiredRegistration` can then reacquire the Auth lock without self-deadlock and execute in `NonCancellable`.

### Null and duplicate behavior

`createUserOrNull` must continue translating only `DuplicateUserFieldException` to null. With an active parent and a correctly classified duplicate, the protected block returns null, registers no rollback action, and required-email registration returns null without pending-role or email-sender calls. If cancellation arrives while the repository is returning a null result, the post-check propagates cancellation rather than converting cancellation into a normal registration refusal. Unexpected exceptions continue to propagate.

SQLite failures not yet translated to `DuplicateUserFieldException` remain governed by finding 4; the finding 8 change cannot compensate an ordinary exception thrown after an insert whose committed identity never reaches Auth.

### Registration-mode scope

Although `createUserOrNull` is shared, the non-cancellable block must not be moved into that helper. Optional-email registration has no `doSuspendTransaction` provisional-user rollback action, so making only the shared create call non-cancellable would shift the same risk to the role/password phase without creating cleanup ownership. The authorized change therefore affects required-email registration only. Optional registration success, duplicate handling, authorization, password storage, and credential issuance remain byte-for-byte unchanged. A complete optional-registration cancellation transaction would be separate scope and needs a distinct rollback design.

### Rollback order

Cancellation before pending-role enrollment leaves only the created-user rollback registered; cleanup deletes the user, purges any auth state defensively, and removes any direct role that a repository subscriber may have assigned. Cancellation after later stages preserves the established reverse order: delivered-link handle first when present, pending-operation placeholder second, and provisional account/password/session/direct-role cleanup last. The new bracket changes no delivery or finalization ordering.

## Deterministic regression design

Add a gated `UsersRepo` test double around the existing in-memory fake. Its overridden batch `create` delegates first so the user is observably persisted, completes a `persisted` signal, waits on a test-controlled `release` deferred, records repository return, and only then returns the created list. Reads and deletes delegate to the underlying fake; deletion records a cleanup event. Generalize the test service factory parameter from concrete `FakeUsersRepo` to `UsersRepo` so the wrapper can be injected as both read and write repository without affecting existing fixtures.

The regression starts required-email registration asynchronously, awaits `persisted`, confirms the provisional user exists, cancels the registration job, advances the scheduler, and confirms the job is not completed while the protected repository call remains gated. The test then completes `release`. The repository returns under `NonCancellable`, Auth enrolls the exact created-user rollback, the restored parent cancellation is observed, the Auth lock unwinds, and transaction compensation deletes the user. A bounded `withTimeout` around the final await proves no lock reacquisition deadlock. Assertions must prove `CancellationException` propagation, an empty user and password store, zero sender calls, zero pending-role transition calls, one role-cleanup call, and event order `persisted`, `repositoryReturned`, `userDeleted`. Successful existing required-email tests and duplicate-email tests protect unchanged non-cancelled and null paths.

## Finding 4 recommendation retained without implementation

The later classifier should traverse the outer `SQLException`, every `nextException`, and nested causes with cycle protection. PostgreSQL SQL state `23505` remains a positive match. Xerial matching should require `SQLiteException.resultCode` equal to `SQLITE_CONSTRAINT_UNIQUE` or `SQLITE_CONSTRAINT_PRIMARYKEY`; base `SQLITE_CONSTRAINT` error code 19 alone is too broad because NOT NULL, CHECK, and foreign-key violations share that family.

Focused later tests should cover direct UNIQUE and PRIMARYKEY Xerial exceptions, each code wrapped as a cause, each code reachable through `nextException`, PostgreSQL `23505`, and negative Xerial NOTNULL/CHECK plus unrelated SQL states. Real in-memory SQLite `ExposedUsersRepo` tests should separately duplicate username and non-null email to prove the actual Exposed/Xerial wrapper shape. Finding 4 remains unmodified until separately authorized.

## Explicit limitations

The finding 8 bracket repairs parent coroutine cancellation after a successful commit when the repository eventually returns. It cannot recover from process death, JVM termination, or a non-cancellation exception thrown after an internal commit but before the repository exposes the created identity. Those cases require a stronger repository contract, such as a durable transaction/outbox record, a commit receipt available on post-commit failure, or repository-owned compensation. The change also does not add cancellation compensation to optional registration.

## Questions for operator

No operator question remains. The follow-up decision authorizes the narrow required-email repair, keeps finding 4 recommendation-only, and does not authorize a broader optional-registration transaction redesign.

## Final implementation handoff

ENTITY:
entity_id=required_email_create_enrollment; type=cancellation_consistency_boundary; state=ready_for_architecture

CONTEXT:

* task_id=03.09.2026_07.37.13-11a2ee6a-f254-4e30-9d7f-3af7c441f87a; agent_id=planning; memory_ref=[PROMPT.md,006-orchestrator.md,AuthFeatureService.kt,AuthFeatureServiceTest.kt,features/auth/README.md]
* constraints=[implement_finding_8,do_not_implement_finding_4,required_email_scope,existing_Auth_lock,existing_doSuspendTransaction]; exclusions=[optional_registration_redesign,repository_API_change,process_death_recovery]

ACTION:

1. action=reject_preexisting_cancellation; target=registerWithRequiredEmail_create_phase; params={operation=currentCoroutineContext.ensureActive,position=before_NonCancellable_create,committed_write=false}
2. action=protect_create_and_enrollment; target=AuthFeatureService.kt; params={context=NonCancellable,scope=createUserOrNull_plus_rollableBackOperation,lock=existing_Auth_write_lock,transaction=existing_doSuspendTransaction}
3. action=preserve_nullable_create; target=createUserOrNull_result; params={non_null=enroll_existing_compensation,null=return_registration_null,duplicate_exception=translated_to_null,unexpected_exception=propagated}
4. action=surface_deferred_cancellation; target=registerWithRequiredEmail_post_create_phase; params={operation=currentCoroutineContext.ensureActive,position=before_markPending,rollback_action=already_enrolled}
5. action=preserve_rollback_order; target=doSuspendTransaction_actions; params={early_cancellation=[provisional_account_cleanup],later_cancellation=[delivered_link_cleanup,pending_placeholder,provisional_account_cleanup],cleanup_context=NonCancellable}
6. action=add_gated_repository_fixture; target=AuthFeatureServiceTest.kt; params={delegate=FakeUsersRepo,signals=[persisted,release,repositoryReturned,userDeleted],interfaces=[UsersRepo],factory_parameter=UsersRepo}
7. action=prove_cancellation_sequence; target=required_email_post_commit_cancellation_test; params={sequence=[persist,cancel,assert_job_pending,release,enroll,propagate,cleanup],timeout=bounded,deadlock_expected=false,sender_calls=0,pending_calls=0,role_cleanup_calls=1}
8. action=retain_existing_contract_tests; target=AuthFeatureServiceTest.kt; params={cases=[required_email_success,duplicate_email_null,delivery_compensation,finalization_compensation],expected=unchanged}
9. action=document_consistency_boundary; target=features/auth/README.md; params={topics=[precheck,NonCancellable_bracket,postcheck,required_only_scope,repository_return_requirement,limitations]}
10. action=verify_and_stop; target=repository; params={commands=[./gradlew_:wishlist.features.auth.server:jvmTest,./gradlew_build,ast-index_rebuild],stop_condition=finding_8_regression_and_existing_suite_pass}

REASON:

* condition=database_commit_before_cancellable_repository_return; requirement=rollback_enrollment_before_parent_cancellation_propagation
* condition=shared_create_helper_without_optional_rollback_owner; requirement=required_email_callsite_bracketing_without_optional_behavior_change
* condition=SQLite_classifier_not_authorized; requirement=finding_4_source_and_tests_unchanged

EXPECTED RESULT:

* entity_id=required_email_create_enrollment; new_state=cancellation_safe_after_committed_insert; location=AuthFeatureService_required_email_create_phase
* entity_id=required_email_cancellation_regression; new_state=deterministic_and_deadlock_bounded; location=AuthFeatureServiceTest_gated_repository_case
* entity_id=sqlite_unique_classifier; new_state=recommendation_only; location=007-planning.md

VERIFICATION:

* check=cancel_before_protected_region; expected=no_user_insert_and_CancellationException_propagated
* check=cancel_after_persist_before_repository_return; expected=job_pending_until_release_then_user_cleanup_then_CancellationException
* check=duplicate_create_with_active_parent; expected=null_result_without_sender_or_pending_transition
* check=required_email_success; expected=pending_result_and_existing_delivery_flow
* check=optional_registration_suite; expected=no_behavior_or_source_path_change
* check=post_release_completion_timeout; expected=no_Auth_lock_deadlock

UNCERTAINTY:

* missing=repository_commit_receipt; ambiguity=non_cancellation_post_commit_exception_cannot_be_compensated_by_Auth
* missing=durable_recovery_record; ambiguity=process_death_cannot_execute_in_process_rollback
* missing=optional_registration_transaction_design; ambiguity=optional_mode_cancellation_cleanup_outside_authorized_scope

REPETITION OF RESULT:

* entity_id=required_email_create_enrollment; stored_in=shared_memory; status=available_for_architecture

COMMUNICATION:

* sender=planning; receiver=architecture; task_id=03.09.2026_07.37.13-11a2ee6a-f254-4e30-9d7f-3af7c441f87a; message_id=86075fe6-c979-4333-b99d-b7e0f9986fed; protocol=AML-HIP

PERSISTENCE:

* local_memory=false; shared_memory=true; index_keys=[task_id,required_email_create_enrollment,required_email_cancellation_regression,sqlite_unique_classifier]

VALIDATION:

* format_valid=true; no_pronouns=true; entities_explicit=true; high_density=true
* causal_chain_present=true; ambiguity_detected=false; validation_scope=final_implementation_handoff
