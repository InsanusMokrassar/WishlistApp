Model: OpenAI GPT-5 (HL)
Changed files: agents/task/03.09.2026_07.37.13-11a2ee6a-f254-4e30-9d7f-3af7c441f87a/008-architecturing.md

Model rationale: The Architecture role prefers an HL model. The follow-up requires exact reasoning across coroutine cancellation delivery, a repository commit-before-return boundary, a lexical transaction DSL, lock release, and deterministic virtual-time testing.

# Follow-up architecturing report

## Scope and decision

Implement finding 8 only in the required-email registration path. Protect the smallest consistency-critical interval: the call that may commit a provisional user and the synchronous registration of that user's existing compensation action. Preserve cancellation propagation immediately after compensation ownership exists. Do not change the optional-email path, repository APIs, rollback ordering, or SQLite uniqueness classification.

Finding 4 remains recommendation-only. Coding must not edit `ExposedUsersRepo.isUniqueViolation()` or related SQLite tests in this cycle.

All planned behavior is coverable by a deterministic common test. No operator decision is needed before Coding.

## Confirmed transaction and coroutine semantics

`doSuspendTransaction` creates one ordinary `TransactionsDSL` object, invokes the transaction lambda with that object as its receiver, and retains its `rollbackActions` list until the transaction completes. `TransactionsDSL` is not a coroutine-context element. `rollableBackOperation` calls its action and, on successful return, synchronously appends a rollback lambda to that same object's `ArrayList`; the append has no suspension point.

`withContext(NonCancellable)` replaces the current `Job` element for the nested block but supplies no dispatcher, so the existing dispatcher is inherited. Capturing the transaction receiver before entering the nested block makes ownership explicit and avoids relying on nested implicit-receiver resolution. The protected block continues to mutate the same `TransactionsDSL` instance; no nested transaction is created and no rollback state is lost across the context switch. Execution remains sequential, so the non-thread-safe rollback list is not accessed concurrently.

The protected block must contain both `createUserOrNull` and the immediate `transaction.rollableBackOperation` call. Parent cancellation delivered while Exposed post-commit notification or cache synchronization is suspended cannot interrupt either operation. A non-null returned identity is therefore enrolled before parent cancellation can escape. The nullable refusal path exits without enrolling a no-op user rollback.

An explicit active check is required after the protected block. Because `NonCancellable` does not change the dispatcher, coroutine machinery need not provide the dispatcher-switch prompt-cancellation guarantee when restoring the parent context. The explicit check makes cancellation propagation independent of that implementation detail and prevents `markPending`, BCrypt, or delivery from starting after cancellation was deferred by the protected region.

The active check remains inside the existing Auth write-lock action, immediately before `markPending`. A thrown cancellation runs `withWriteLock`'s `finally` before `doSuspendTransaction` invokes compensation, allowing `compensateRequiredRegistration` to reacquire the Auth lock. Existing cancellation-at-finalization coverage already exercises the same lock-unwind-then-non-cancellable-compensation mechanism.

## Exact production change

Add `currentCoroutineContext` and `ensureActive` imports from `kotlinx.coroutines`. In `registerWithRequiredEmail`, capture the transaction receiver once and replace only the current provisional-create/enrollment fragment with this shape:

```kotlin
private suspend fun registerWithRequiredEmail(
    // Existing parameters remain unchanged.
): RegistrationResult? = doSuspendTransaction {
    val transaction = this
    val provisionalUser =
        locker.withWriteLock {
            val userByUsername = usersRepo.getUserByUsername(username)
            if (userByUsername != null) return@withWriteLock null

            currentCoroutineContext().ensureActive()
            val created = withContext(NonCancellable) {
                val createdUser = createUserOrNull(username, email)
                    ?: return@withContext null
                transaction.rollableBackOperation(
                    rollback = {
                        try {
                            compensateRequiredRegistration(actionResult.id, roleLifecycle)
                        } catch (cleanupError: Throwable) {
                            error.addSuppressed(cleanupError)
                        }
                    },
                    action = { createdUser },
                )
            } ?: return@withWriteLock null
            currentCoroutineContext().ensureActive()

            val pending = rollableBackOperation(
                { Unit }
            ) {
                roleLifecycle.markPending(created.id)
            }
            if (pending == false) throw RequiredEmailRegistrationRejected()
            created
        } ?: return@doSuspendTransaction null

    // Existing hashing, delivery, finalization, and result mapping remain unchanged.
}
```

The first active check avoids beginning a durable write for a request already known to be cancelled after the read-only username check. Cancellation racing after that check is allowed to enter the protected block; the write may occur, but compensation ownership is installed before cancellation propagates. The second active check either observes cancellation directly or is unreachable because restoration from `withContext` already surfaced it; both cases occur only after enrollment.

Do not move the protected region into `createUserOrNull`. That helper is also used by optional registration, which has no surrounding rollback transaction. Do not include `markPending` in the non-cancellable region: after user compensation is enrolled, ordinary cancellation is safe and should remain prompt. Do not include the username lookup, password hashing, SMTP delivery, or finalization.

### Preserved results and rollback order

- Active, unique registration still creates the user, marks the role pending, sends the invite, stores the password, and returns `PendingEmailVerification`.
- A classified duplicate still returns `null`, enrolls no user rollback, and invokes neither pending-role transition nor delivery.
- Cancellation during the protected create waits for repository return, enrolls provisional-user compensation, propagates `CancellationException`, and invokes only provisional cleanup because pending-role and delivery stages never started.
- Cancellation after later enrollment points keeps the existing reverse order: delivered-link rollback, pending placeholder, then user/password/session/direct-role cleanup.
- Optional-email registration remains unchanged.

## Deterministic regression fixture

Generalize the private `buildService` fixture parameter from `FakeUsersRepo` to `UsersRepo`; it is already passed as both the read and write dependency, so existing tests remain source-compatible. Add this common-test wrapper beside `FakeUsersRepo`:

```kotlin
private class GatedCreateUsersRepo(
    private val delegate: FakeUsersRepo = FakeUsersRepo(),
) : UsersRepo by delegate {
    val persisted = CompletableDeferred<Unit>()
    val release = CompletableDeferred<Unit>()
    val events = mutableListOf<String>()

    override suspend fun create(values: List<NewUser>): List<RegisteredUser> {
        val created = delegate.create(values)
        events += "persisted"
        persisted.complete(Unit)
        release.await()
        events += "repositoryReturned"
        return created
    }

    override suspend fun deleteById(ids: List<UserId>) {
        delegate.deleteById(ids)
        events += "userDeleted"
    }
}
```

Delegation preserves read, update, and flow behavior. The batch overrides are the actual `WriteCRUDRepo` dispatch points reached by the single-value `create(NewUser)` and `deleteById(UserId)` convenience extensions. Delegating creation before the gate models both production suspension sites: an Exposed row can already be committed while post-create flow notification is pending, and the cache wrapper can suspend after its backing repository returns. Delegating deletion before recording `userDeleted` proves that the persistent fake is empty when the cleanup event is emitted.

## Executable regression-test specification

Add `requiredEmailRegistrationCancellationAfterPersistedCreateCompensatesUser` to `AuthFeatureServiceTest`:

```kotlin
@Test
fun requiredEmailRegistrationCancellationAfterPersistedCreateCompensatesUser() = runTest {
    val usersRepo = GatedCreateUsersRepo()
    val passwordsRepo = FakePasswordsRepo()
    val sender = FakeRegistrationEmailSender(true)
    val lifecycle = FakeRegistrationRoleLifecycle()
    val username = Username("alice")
    val service = buildService(
        usersRepo = usersRepo,
        passwordsRepo = passwordsRepo,
        enableRegistration = true,
        requireEmailForRegistration = true,
        registrationEmailSender = sender,
        registrationRoleLifecycle = lifecycle,
    )
    val registration = async {
        service.register(username, plainPassword, Email("alice@example.com"))
    }

    usersRepo.persisted.await()
    assertNotNull(usersRepo.getUserByUsername(username))

    registration.cancel()
    testScheduler.runCurrent()
    assertFalse(registration.isCompleted)
    assertNotNull(usersRepo.getUserByUsername(username))

    usersRepo.release.complete(Unit)
    withTimeout(1_000) { registration.join() }
    assertFailsWith<CancellationException> { registration.await() }

    assertTrue(usersRepo.getAll().isEmpty())
    assertTrue(passwordsRepo.getAll().isEmpty())
    assertTrue(sender.users.isEmpty())
    assertEquals(0, lifecycle.markCalls)
    assertEquals(1, lifecycle.removeCalls)
    assertTrue(lifecycle.directRoleUserIds.isEmpty())
    assertEquals(
        listOf("persisted", "repositoryReturned", "userDeleted"),
        usersRepo.events,
    )
}
```

The pre-release `isCompleted == false` assertion proves parent cancellation cannot escape the protected region. `withTimeout { registration.join() }` is the deadlock guard; placing `assertFailsWith<CancellationException>` outside that timeout prevents a `TimeoutCancellationException` from falsely satisfying the expected registration-cancellation assertion. The final counters prove no pending or delivery work occurred and that defensive role cleanup ran exactly once.

Retain and run the existing required-email success, duplicate-email refusal, delivery cancellation, link rollback, and optional-registration tests. Together with the new regression, those tests cover the modified function's success, nullable result, cancellation, later-stage rollback, and untouched optional-mode cases.

## README updates

Coding should add one Auth Architecture Notes bullet, without modifying Operator Notes: required-email account creation and immediate user-compensation enrollment form a bounded non-cancellable consistency region bracketed by parent-context active checks; cancellation received during repository return is propagated before pending-role transition after enrollment completes. State that the repository must eventually return, that optional registration is unchanged, and that process termination or any post-commit repository exception that withholds the created identity still requires repository-owned compensation or a durable commit receipt.

## Finding 4 recommendation retained without implementation

A later, separately authorized patch should traverse the caught `ExposedSQLException`, nested causes, and every `SQLException.nextException`, with cycle protection. Keep PostgreSQL SQL state `23505`. For Xerial, accept only `SQLiteException.resultCode` values `SQLITE_CONSTRAINT_UNIQUE` and `SQLITE_CONSTRAINT_PRIMARYKEY`; do not classify base error code 19 because that family also contains NOT NULL, CHECK, and foreign-key failures. Prove the real wrapper shape with in-memory SQLite duplicate-username and duplicate-non-null-email tests, plus negative non-unique constraint tests. No part of that recommendation belongs in Coding step 009.

## Limitations

The non-cancellable region is structurally small but cannot be safely time-limited after a possible commit: without a returned user identity, Auth cannot compensate. A repository that never returns can therefore keep the registration request in a cancelling state and retain the Auth write lock. Process death, JVM termination, an independently thrown cancellation, or another exception after an internal commit but before the created identity is returned remains outside this repair. A durable outbox/registration record, repository-owned compensation, or a commit receipt would be required for those cases. Optional-registration cancellation compensation is also outside the authorized scope.

## Coding handoff

ENTITY:
entity_id=required_email_create_enrollment; type=cancellation_consistency_boundary; state=architecture_complete

CONTEXT:

* task_id=03.09.2026_07.37.13-11a2ee6a-f254-4e30-9d7f-3af7c441f87a; agent_id=architecturing; memory_ref=[PROMPT.md,007-planning.md,TransactionsDSL.kt,AuthFeatureService.kt,AuthFeatureServiceTest.kt,features/auth/README.md]
* constraints=[finding_8_only,required_email_scope,parent_cancellation_propagation,existing_public_API,existing_reverse_rollback_order]; exclusions=[finding_4_implementation,optional_registration_redesign,repository_API_change]

ACTION:

1. action=capture_transaction_receiver; target=required_email_create_enrollment; params={source=doSuspendTransaction_receiver,name=transaction,lifetime=single_registration}
2. action=reject_known_cancellation; target=required_email_create_enrollment; params={operation=currentCoroutineContext.ensureActive,position=after_username_lookup_before_write,write_started=false}
3. action=protect_commit_to_enrollment_interval; target=required_email_create_enrollment; params={context=NonCancellable,operations=[createUserOrNull,transaction.rollableBackOperation],dispatcher=existing,rollback_list=existing}
4. action=preserve_nullable_refusal; target=required_email_create_enrollment; params={null_result=no_rollback_action,non_null_result=user_compensation_enrolled,duplicate_contract=null}
5. action=surface_deferred_cancellation; target=required_email_create_enrollment; params={operation=currentCoroutineContext.ensureActive,position=before_markPending,lock_release=finally_before_rollback}
6. action=generalize_test_factory; target=AuthFeatureServiceTest.buildService; params={parameter_type=UsersRepo,read_dependency=same_instance,write_dependency=same_instance}
7. action=add_gated_repository; target=GatedCreateUsersRepo; params={delegate=FakeUsersRepo,gate_after=persistence,gate_before=repository_return,events=[persisted,repositoryReturned,userDeleted]}
8. action=add_regression; target=requiredEmailRegistrationCancellationAfterPersistedCreateCompensatesUser; params={cancel_position=after_persist_before_return,completion_bound_ms=1000,sender_calls=0,pending_calls=0,role_cleanup_calls=1}
9. action=document_boundary; target=features/auth/README.md; params={section=Architecture_Notes,operator_notes_change=false,limitations=[repository_must_return,identity_withholding_exception,process_termination,optional_scope]}
10. action=preserve_sqlite_classifier; target=ExposedUsersRepo.isUniqueViolation; params={source_change=false,test_change=false,recommendation_only=true}

REASON:

* condition=committed_user_insert_plus_cancellable_repository_return; requirement=provisional_user_rollback_enrollment_before_parent_cancellation_escape
* condition=TransactionsDSL_object_outside_coroutine_context; requirement=explicit_receiver_capture_plus_same_object_enrollment_across_job_replacement
* condition=operator_withheld_finding_4_implementation; requirement=SQLite_classifier_source_and_tests_unchanged

EXPECTED RESULT:

* entity_id=required_email_create_enrollment; new_state=parent_cancellation_safe_after_committed_insert; location=AuthFeatureService.registerWithRequiredEmail
* entity_id=required_email_cancellation_regression; new_state=deterministic_and_deadlock_bounded; location=AuthFeatureServiceTest.requiredEmailRegistrationCancellationAfterPersistedCreateCompensatesUser
* entity_id=sqlite_unique_classifier; new_state=recommendation_only; location=008-architecturing.md

VERIFICATION:

* check=cancel_after_persist_before_repository_return; expected=registration_incomplete_until_release_plus_compensation_after_release
* check=post_release_completion; expected=CancellationException_plus_empty_user_store_plus_empty_password_store
* check=pre_pending_side_effects; expected=sender_calls_0_plus_pending_calls_0_plus_role_cleanup_calls_1
* check=event_order; expected=[persisted,repositoryReturned,userDeleted]
* check=required_email_success_and_duplicate_tests; expected=unchanged_results_plus_unchanged_side_effects
* check=optional_registration_tests; expected=unchanged_source_path_plus_unchanged_results
* check=focused_build; expected=wishlist.features.auth.server_jvmTest_pass
* check=source_index; expected=ast-index_rebuild_success_after_Coding_changes

UNCERTAINTY:

* missing=repository_commit_receipt; ambiguity=post_commit_exception_without_returned_identity_remains_uncompensated
* missing=durable_recovery_record; ambiguity=process_termination_prevents_in_process_rollback
* missing=optional_registration_transaction_owner; ambiguity=optional_mode_cancellation_compensation_outside_scope

REPETITION OF RESULT:

* entity_id=required_email_create_enrollment; stored_in=shared_memory; status=available_for_coding

COMMUNICATION:

* sender=architecturing; receiver=coding; task_id=03.09.2026_07.37.13-11a2ee6a-f254-4e30-9d7f-3af7c441f87a; message_id=1f2b5796-fcdc-4519-9de1-c5f41a8a0308; protocol=AML-HIP

PERSISTENCE:

* local_memory=false; shared_memory=true; index_keys=[task_id,required_email_create_enrollment,required_email_cancellation_regression,sqlite_unique_classifier]

VALIDATION:

* format_valid=true; no_pronouns=true; entities_explicit=true; high_density=true
* causal_chain_present=true; ambiguity_detected=false; validation_scope=coding_handoff
