Model: GPT-5 (HL)
Changed files: agents/task/03.09.2026_07.37.13-11a2ee6a-f254-4e30-9d7f-3af7c441f87a/001-planning.md

Model rationale: This planning task crosses SMTP security, serialized wire compatibility, database-driver error semantics, and cancellation-safe compensation across Auth, Email, DeepLinks, Users, and MicroUtils. The Planning role prefers an HL model, and the cross-module failure-ordering analysis requires that level.

# Planning report

## Task understanding

The implementation scope is limited to findings 1, 5, and 7. SMTP certificate trust must become an explicit unsafe opt-in whose omission remains safe, the port-587 sample must use STARTTLS without implicit SSL, and a successfully delivered verification link must participate in later Auth rollback without changing the existing `RegistrationEmailSender.sendRegistrationEmail(RegisteredUser): Boolean` contract. Focused regression coverage and affected feature documentation are part of those changes.

Finding 2 is accepted unchanged because the operator treats `server/sample.config.json` as an adaptable self-hosting template. Finding 6 is also accepted unchanged: the two-argument `AuthFeature.register` source/API break is permitted for now. Findings 3, 4, and 8 require evidence-backed explanations or recommendations only and must not be implemented in this cycle.

## Repository and feature constraints

The Email, Auth, DeepLinks, Users, and Roles feature READMEs contain no operator note that conflicts with the requested scope. Email configuration is decoded from the nested `config["email"]` element through the application `Json`, whose `ignoreUnknownKeys = true` and Kotlin-serialization defaults preserve older configuration files when a defaulted field is added. Required-email registration deliberately keeps SMTP delivery outside the global Auth write lock and relies on `doSuspendTransaction` rollback actions for provisional state.

## Evidence and conclusions

### Finding 1: SMTP trust must be default-safe

`SmtpConfig` currently defaults to port 587, STARTTLS enabled, and implicit SSL disabled. `SmtpEmailService.buildSession`, however, always writes `mail.smtp.ssl.trust` with the configured host. Angus interprets that property as an explicit trust override, so normal certificate-chain validation is bypassed for every configured SMTP host whenever TLS is used.

Add `unsafeSsl: Boolean = false` to `SmtpConfig`. The existing nested config reader needs no migration or special fallback: old files omit the field and decode to `false`; application JSON ignores unknown keys, so config files containing the new field also remain decodable by the preceding application configuration stack. Extract or expose a module-internal SMTP-properties builder so tests can assert that `mail.smtp.ssl.trust` is absent by default and equals the configured host only when `unsafeSsl` is explicitly true. `useTls` and `useSsl` retain their present meanings and values.

### Finding 2: sample SQLite path

No change is planned. This disposition accepts responsibility for adapting the relative database location in a concrete self-hosted deployment and does not claim that the current path is persistent in every container layout.

### Finding 3: registration wire compatibility

The problem is not limited to old devices. It is a mixed-version protocol break in both directions.

For a new client talking to a legacy server, `GET /auth/config` fails and `KtorAuthFeature.getConfig` correctly falls back to the legacy availability endpoint with optional-email semantics. Registration then succeeds server-side, but the legacy server returns a bare `AuthCredentials` JSON object while the new client unconditionally calls `response.body<RegistrationResult>()`. The application JSON uses array polymorphism for `RegistrationResult`, so decoding the bare object fails after the account has already been created. The client service never receives `RegistrationResult.Authorized`, never stores credentials, and the user sees a failed registration despite the reserved username.

For an old installed client talking to a new server, the old `RegisterRequest` remains decodable because the new `email` property defaults to null. When email is optional, the new server creates the account and returns polymorphic `RegistrationResult.Authorized`; the old client expects bare `AuthCredentials`, so decoding again fails after creation. When email is required, the legacy request has no address and is rejected before account creation. Thus old Android/JVM clients, or any still-running old web bundle, are affected after a server upgrade. Finding 6 accepts the Kotlin source/API break but does not remove this independent HTTP wire incompatibility. No compatibility decoder or route versioning is authorized in this cycle.

### Finding 4: SQLite uniqueness classification recommendation

`ExposedUsersRepo` catches `ExposedSQLException` from both `create` and `update`, then `isUniqueViolation` checks only PostgreSQL SQL state `23505`. Xerial SQLite reports constraint failures through `SQLiteException`; SQL state is normally absent, while SQLite constraint codes are used instead. A raw SQLite uniqueness failure therefore bypasses `DuplicateUserFieldException`, so Auth cannot map duplicate registration to its existing null/400 contract and email/admin routes cannot map duplicates to 409.

Recommended later fix: traverse the caught `SQLException`, its `nextException` chain, and its cause chain. Preserve PostgreSQL `23505`. For Xerial, classify `SQLiteException.resultCode` values `SQLITE_CONSTRAINT_UNIQUE` and `SQLITE_CONSTRAINT_PRIMARYKEY`; avoid treating every base `SQLITE_CONSTRAINT` value as a duplicate because NOT NULL, CHECK, and foreign-key failures share the base constraint family. Prove the driver-specific shape with a real in-memory SQLite `ExposedUsersRepo` test that duplicates username and non-null email separately, and retain unit coverage showing unrelated SQL states and non-unique SQLite constraint codes remain unclassified. No source change is planned until the operator authorizes finding 4.

### Finding 5: port 587 transport mode

`server/sample.config.json` currently combines `port: 587`, `useTls: true`, and `useSsl: true`. Port 587 expects a plaintext SMTP greeting followed by STARTTLS; implicit TLS is normally used on port 465. Change only the sample SMTP setting to `useSsl: false`, retain `useTls: true`, and show `unsafeSsl: false` explicitly so the secure default is discoverable. The Email README already documents the correct 587 pairing and must be extended with the unsafe trust opt-in warning.

### Finding 6: two-argument registration API

No change is planned. The operator accepts the source/API break from `AuthCredentials?` to `RegistrationResult?` for the two-argument method in the current development phase.

### Finding 7: delivered-link compensation without breaking the existing sender API

`EmailRegistrationInviteSender` owns the minted `DeepLinkId`, cleans it when SMTP returns false or throws, and returns only Boolean after successful delivery. `AuthFeatureService` installs provisional-user rollback before delivery, but a later stored-email check, password write, or cancellation can fail after the Boolean success. Auth then removes user, password, and roles while no component still exposes the exact link ID to the transaction, leaving a dead persistent deeplink.

Preserve `RegistrationEmailSender` unchanged and add an optional additive capability in Auth server, for example `CompensableRegistrationEmailSender`, plus a request-local delivery handle whose suspending `rollback()` removes the exact delivered artifact. The capability exposes a separate send method returning the handle on success and null on ordinary delivery failure. `EmailRegistrationInviteSender` implements the capability and keeps the existing Boolean method as a bridge that delegates to the compensable send path and discards the handle for legacy callers.

Inside required-email Auth registration, detect the optional capability at runtime. Enroll the returned handle as a `rollableBackOperation` immediately after successful delivery; legacy Boolean-only senders retain current behavior. On a later failure, invoke handle rollback in `NonCancellable`, attach cleanup failures as suppressed exceptions, and then allow the already-registered provisional-user compensation to remove user, password, and roles. Reverse rollback order removes the delivered link before account state. On success, no rollback runs and the link remains usable. A request-local handle avoids a shared `UserId -> DeepLinkId` map, so concurrent registrations cannot overwrite cleanup ownership and no success-path bookkeeping leaks memory.

Cancellation during SMTP remains governed by the sender's existing non-cancellable cleanup. Cancellation after SMTP success is governed by the new Auth rollback handle. The design cannot repair a process crash between SMTP acceptance and final Auth completion because in-process rollback never executes; complete crash safety would require durable provisional-registration/outbox state or deeplink expiry and is outside this task.

### Finding 8: cancellation before create compensation recommendation

The production create path commits before returning. `AbstractExposedWriteCRUDRepo.create` completes its database transaction, then suspends in `onAfterCreate` and `_newObjectsFlow.emit`; `WriteCRUDCacheRepo.create` can subsequently suspend while updating its cache. `AuthFeatureService` currently calls `createUserOrNull` first and invokes `rollableBackOperation` only after that call returns. Cancellation in any post-commit suspension therefore reaches `doSuspendTransaction` before the user rollback exists.

Recommended later fix: within the existing Auth write lock, wrap both `createUserOrNull` and the immediately following `rollableBackOperation` enrollment in one bounded `withContext(NonCancellable)` block. If the database insert commits, all post-create notification/cache work is allowed to finish and the rollback action is registered before parent cancellation is observed; `doSuspendTransaction` can then compensate the account. A regression test should use a write repository that persists a user, signals the test, waits on a controllable post-create gate, and then returns. Cancel registration after persistence, release the gate, and assert that cancellation propagates only after user, password, and role cleanup. This is the narrow application-level repair; a broader MicroUtils change would affect all repositories and is not authorized. Non-cancellation exceptions raised after a hidden commit remain a repository-contract concern unless the repository exposes a commit receipt or makes post-commit notification non-throwing.

## Questions for operator

No further operator questions remain. The prompt explicitly resolves the sample database path and source/API compatibility decisions, and the additive compensation capability satisfies the condition attached to finding 7 without changing the existing sender contract.

## Final implementation handoff

ENTITY:
entity_id=review_remediation_1_5_7; type=implementation_plan; state=ready_for_architecture

CONTEXT:

* task_id=03.09.2026_07.37.13-11a2ee6a-f254-4e30-9d7f-3af7c441f87a; agent_id=planning; memory_ref=[PROMPT.md,features/email/README.md,features/auth/README.md,features/deeplinks/README.md,features/users/README.md,features/roles/README.md]
* constraints=[implement_findings_1_5_7,explain_findings_3_4_8,leave_findings_2_6,retain_RegistrationEmailSender_signature]; scope=[email_config,smtp_properties,sample_smtp,registration_compensation,focused_tests,feature_docs]

ACTION:

1. action=add_safe_smtp_option; target=features/email/server/src/commonMain/kotlin/EmailConfig.kt; params={field=unsafeSsl,type=Boolean,default=false,kdoc=explicit_certificate_trust_bypass}
2. action=conditionalize_smtp_trust; target=features/email/server/src/commonMain/kotlin/services/SmtpEmailService.kt; params={property=mail.smtp.ssl.trust,condition=unsafeSsl_true,default_result=property_absent,test_seam=internal_properties_builder}
3. action=correct_submission_sample; target=server/sample.config.json; params={port=587,useTls=true,useSsl=false,unsafeSsl=false,database_path=unchanged}
4. action=add_compensation_capability; target=features/auth/server/src/commonMain/kotlin/RegistrationEmailSender.kt; params={base_signature=unchanged,new_capability=CompensableRegistrationEmailSender,success_value=request_local_rollback_handle,failure_value=null}
5. action=produce_exact_link_handle; target=features/email/server/src/commonMain/kotlin/services/EmailRegistrationInviteSender.kt; params={handle_target=DeepLinkId,legacy_boolean_bridge=preserved,false_cleanup=preserved,smtp_cancellation_cleanup=preserved}
6. action=enroll_delivery_rollback; target=features/auth/server/src/commonMain/kotlin/services/AuthFeatureService.kt; params={capability_detection=runtime,registration=rollableBackOperation,cleanup_context=NonCancellable,cleanup_error=suppressed,legacy_sender_behavior=unchanged}
7. action=add_regression_coverage; target=[EmailConfigTest.kt,SmtpEmailService_properties_test,EmailRegistrationInviteSenderTest.kt,AuthFeatureServiceTest.kt,RegistrationCompensationIntegrationTest.kt]; params={cases=[unsafe_default_absent,unsafe_opt_in_trust,port_587_sample_mode,finalization_failure_removes_link,post_delivery_cancellation_removes_link,success_retains_link,legacy_sender_unchanged]}
8. action=update_feature_documentation; target=[features/email/README.md,features/auth/README.md,features/deeplinks/README.md]; params={topics=[unsafeSsl_warning,STARTTLS_sample,delivery_handle_ownership,rollback_order,crash_limit]}
9. action=verify_without_scope_expansion; target=repository_build; params={commands=[focused_email_tests,focused_auth_tests,full_./gradlew_build,ast-index_rebuild],stop_condition=all_selected_acceptance_conditions_pass}

REASON:

* condition=unconditional_smtp_trust_or_unowned_delivered_link; requirement=default_certificate_validation_and_exact_artifact_compensation
* condition=port_587_with_implicit_ssl; requirement=STARTTLS_enabled_and_implicit_SSL_disabled
* condition=mixed_version_or_sqlite_or_create_cancellation_findings_outside_implementation_scope; requirement=evidence_recorded_without_source_mutation

EXPECTED RESULT:

* entity_id=review_remediation_1_5_7; new_state=implemented_with_regressions; location=selected_source_config_test_and_feature_README_files
* entity_id=smtp_certificate_policy; new_state=safe_by_default_with_explicit_unsafe_opt_in; location=SmtpConfig_and_SMTP_session_properties
* entity_id=verification_link_ownership; new_state=transaction_compensable_after_delivery; location=Auth_Email_additive_capability_boundary

VERIFICATION:

* check=omitted_unsafeSsl_decode_and_session_property; expected=unsafeSsl_false_and_mail.smtp.ssl.trust_absent
* check=explicit_unsafeSsl_session_property; expected=mail.smtp.ssl.trust_equals_configured_host
* check=sample_port_587_flags; expected=useTls_true_and_useSsl_false
* check=finalization_failure_or_cancellation_after_delivery; expected=link_user_password_roles_removed_and_original_failure_or_cancellation_propagated
* check=successful_required_email_registration; expected=verification_link_retained_and_pending_result_returned
* check=legacy_RegistrationEmailSender; expected=existing_Boolean_contract_and_behavior_preserved

UNCERTAINTY:

* missing=durable_crash_recovery_design; ambiguity=process_crash_after_SMTP_acceptance_remains_out_of_scope
* missing=operator_authorization_for_findings_4_8; ambiguity=SQLite_classifier_and_create_cancellation_recommendations_remain_unimplemented

REPETITION OF RESULT:

* entity_id=review_remediation_1_5_7; stored_in=shared_memory; status=available_for_architecture

COMMUNICATION:

* sender=planning; receiver=architecture; task_id=03.09.2026_07.37.13-11a2ee6a-f254-4e30-9d7f-3af7c441f87a; message_id=c0f891de-6dc4-4789-866a-2c7fcd8411a9; protocol=AML-HIP

PERSISTENCE:

* local_memory=true; shared_memory=true; index_keys=[task_id,review_remediation_1_5_7,smtp_certificate_policy,verification_link_ownership]

VALIDATION:

* format_valid=true; no_pronouns=true; entities_explicit=true; high_density=true
* causal_chain_present=true; ambiguity_detected=false; validation_scope=final_implementation_handoff
