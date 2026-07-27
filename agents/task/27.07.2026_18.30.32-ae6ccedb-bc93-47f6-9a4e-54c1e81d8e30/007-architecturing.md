Model: gpt-5-codex
Changed files: agents/task/27.07.2026_18.30.32-ae6ccedb-bc93-47f6-9a4e-54c1e81d8e30/007-architecturing.md

## Architecture fallback note

The delegated cycle-two architecture worker did not return a step report after bounded waits. The main session converted the accepted `006-planning.md` remediation plan into this architecture handoff. No product files were changed during the architecture stage.

## Model rationale

The repository architecture priority is `fable`, then `opus`, then `sonnet`; the active model is `gpt-5-codex`, which is recorded as the actual model.

## Concrete design

The role transition must be serialized across both the asynchronous default-role callback and deeplink approval. `features/roles/server` will own one process-local `Mutex` and expose transition helpers that lock the complete check/modify sequence. The required-email default grant checks for an existing `UserRole` while holding the lock and skips `NewUserRole` when approval already completed. Promotion uses the same lock, removes `NewUserRole`, and adds `UserRole`. Root and optional-registration behavior remain unchanged. This lock is process-local and matches the current in-process role repository; PostgreSQL-level multi-instance coordination is outside this issue's existing deployment model.

Required-email registration will compensate after invite failure: remove the stored password and just-created user before returning `null`. The invite sender will remember the created deeplink and remove it when SMTP returns `false` or throws. Successful sends retain the deeplink. No deletion HTTP route is added. Tests will use fake repositories and a sender that fails once then succeeds to prove same-username retryability.

The development config will retain `host: 0.0.0.0` and change only `publicHost` to `127.0.0.1`. The existing URL builder then produces a clickable local URL matching the README and Mailpit workflow.

`AuthFeature.getConfig()` will receive a default implementation derived from `isRegistrationAvailable()` with `requireEmailForRegistration=false`. Current server/client implementations retain explicit overrides. This preserves source compatibility for legacy implementations while exposing the new policy to current clients.

Integration coverage will be added at deterministic test layers available in the repository: auth common legacy-interface compatibility; email sender failed-link cleanup; role delayed-callback ordering; application JSON polymorphic payload registration; and, where the existing Ktor test dependencies permit, auth route/client transport and full deeplink dispatch. Live Mailpit, Docker startup, and interactive platform rendering remain manual checks and are explicitly documented as such.

The canonical handler id is `EmailVerification.handlerId` with literal `email.registration_verification`; the historical mismatch in `002-architecturing.md` is immutable and will be called out in the coding report. Auth and roles README tables/architecture notes will be updated, and all new test fixture declarations will receive KDocs without changing Operator Notes.

## Planned file changes

- `features/roles/server/src/commonMain/kotlin/RolesBootstrap.kt`: shared mutex, approved-role guard, synchronized promotion helper.
- `features/roles/server/src/commonTest/kotlin/RolesBootstrapTest.kt`: delayed callback regression and exact final role set.
- `features/auth/server/src/commonMain/kotlin/services/AuthFeatureService.kt`: password/user compensation; import `deleteById`.
- `features/auth/server/src/commonTest/kotlin/services/AuthFeatureServiceTest.kt`: failed-then-successful sender retry and cleanup assertions.
- `features/deeplinks/server/src/commonMain/kotlin/services/DeepLinksService.kt`: internal `removeDeepLink` operation backed by repo removal.
- `features/email/server/src/commonMain/kotlin/services/EmailRegistrationInviteSender.kt`: remove minted link on send failure/exception.
- `features/email/server/src/commonTest/kotlin/services/EmailRegistrationInviteSenderTest.kt`: failed-send link cleanup.
- `features/auth/common/src/commonMain/kotlin/AuthFeature.kt`: default `getConfig()` implementation.
- `features/auth/common/src/commonTest/kotlin/models/RegisterRequestTest.kt`: legacy implementation compatibility test.
- `server/dev.config.json`: public host correction.
- `features/email/server` tests: canonical handler-id and application polymorphism assertions where feasible.
- `features/auth/README.md`, `features/roles/README.md`, and affected test files: documentation/KDoc corrections.

## Test specifications

### Role transition

- Controlled dispatcher starts the repository flow collector.
- New non-root user creation schedules default pending-role work.
- Approval promotion runs before delayed callback release.
- Dispatcher advances callback.
- Final roles equal exactly `{UserRole}`; `NewUserRole` is absent.
- Root required-email transition equals `{UserRole, SuperAdminRole}`; optional transition equals `{UserRole}`.
- Repeated promotion and backfill remain idempotent.

### Registration and invite cleanup

- Required registration with sender failure leaves no user and no password.
- Same username retry with sender success creates one user and returns credentials.
- Missing sender fails closed with no credentials and cleanup.
- Successful sender retains exactly one deeplink.
- SMTP false result and thrown exception both remove the minted deeplink.

### Compatibility and configuration

- Legacy `AuthFeature` implementing previous methods compiles without `getConfig()` and receives `AuthConfig(true, false)` from the default.
- Current server override returns configured required-email flag.
- `server/dev.config.json` parses with `host=0.0.0.0` and `publicHost=127.0.0.1`.
- URL builder emits `http://127.0.0.1:8196/api/links/{id}`.

### Integration and documentation

- Application `Json` encodes/decodes `DeepLinkHandlerInfo` containing `EmailVerificationPayload` through the registered polymorphic module.
- `DeepLinksService.createDeepLink` followed by `handle` returns `Handled` and leaves exactly approved roles.
- Auth route/client tests, if supported by existing Ktor test infrastructure, verify `/auth/config`, registration email JSON, and legacy availability endpoint.
- README model/role tables include new contracts; test fixtures contain valid KDocs; historical step reports remain untouched.

## Manual-only checks

Live SMTP acceptance through Mailpit, Docker Compose startup, and interactive JS/JVM/Android rendering are external/platform checks. Verification must still run full Gradle build, JSON parsing, Compose config validation, and record the manual limitation.

## AML-HIP

ENTITY:
entity_id=issue73_cycle2_architecture; type=architecture_handoff; state=ready_for_coding
entity_id=role_transition_lock; type=concurrency_control; state=defined
entity_id=failed_invite_compensation; type=cleanup_flow; state=defined
entity_id=auth_config_default; type=compatibility_contract; state=defined
entity_id=integration_coverage; type=test_scope; state=defined

CONTEXT:

* task_id=27.07.2026_18.30.32-ae6ccedb-bc93-47f6-9a4e-54c1e81d8e30; agent_id=architecturing-007-fallback; memory_ref=[006-planning.md,005-validating.md,003-coding.md,004-verification.md,agents/ARCHITECTURE.md,agents/GIT.md]
* constraints=[edit_only_current_step_file,no_operator_notes_changes,shared_role_lock_required,failed_registration_retryable,manual_external_SMTP_check]

ACTION:

1. action=synchronize; target=role_transition_lock; params={scope=[grantDefaultRoles,promoteNewUserToUser],guard=UserRole_presence_before_NewUserRole,mutex=process_local}
2. action=compensate; target=failed_invite_compensation; params={auth=[passwordsRepo.unset,writeUsersRepo.deleteById],deeplink=DeepLinksService.removeDeepLink,success_condition=same_username_retry}
3. action=compatibilize; target=auth_config_default; params={method=getConfig,source=isRegistrationAvailable,required_email_default=false}
4. action=integrate; target=integration_coverage; params={tests=[role_ordering,invite_cleanup,legacy_auth,polymorphic_payload,deeplink_dispatch,route_transport_if_supported]}
5. action=normalize; target=handler_id; params={source=EmailVerification.handlerId,literal=email.registration_verification,historical_report_immutable=true}

REASON:

* condition=approval_before_async_default_grant → action=shared_mutex_plus_UserRole_guard → result=approved_account_forbids_NewUserRole
* condition=invite_delivery_failure → action=remove_password_user_and_minted_deeplink → result=retryable_registration
* condition=legacy_AuthFeature_implementation → action=default_getConfig → result=source_compatibility_preserved

EXPECTED RESULT:

* entity_id=role_transition_lock; new_state=verified_role_set_exactly_UserRole; location=features/roles/server
* entity_id=failed_invite_compensation; new_state=user_password_deeplink_absent_after_failure; location=features/auth/server+features/deeplinks/server+features/email/server
* entity_id=auth_config_default; new_state=legacy_implementations_compile; location=features/auth/common
* entity_id=integration_coverage; new_state=ordering_cleanup_compatibility_runtime_paths_tested; location=commonTest_sources

VERIFICATION:

* check=High_role_race_regression; expected=final_roles_UserRole_only
* check=failed_registration_retry; expected=first_attempt_cleanup_and_second_attempt_success
* check=dev_public_host; expected=127.0.0.1
* check=legacy_auth_default; expected=requireEmailForRegistration_false
* check=product_scope; expected=coding_stage_only_changes_product_and_current_step

UNCERTAINTY:

* missing=live_Mailpit_runtime; ambiguity=external_SMTP_requires_manual_smoke
* missing=interactive_platform_UI_harness; ambiguity=visual_rendering_requires_manual_smoke
* missing=historical_report_edit_authority; ambiguity=002_architecture_mismatch_corrected_in_current_handoff_only

REPETITION OF RESULT:

* entity_id=issue73_cycle2_architecture; stored_in=shared_memory; status=ready_for_coding
* entity_id=role_transition_lock; stored_in=shared_memory; status=required

COMMUNICATION:

* sender=architecturing-007-fallback; receiver=coding-008; task_id=27.07.2026_18.30.32-ae6ccedb-bc93-47f6-9a4e-54c1e81d8e30; message_id=8f7b0d0a-4f1d-4aa4-a1ab-31d4fc2ec5e8; protocol=AML-HIP

PERSISTENCE:

* local_memory=true; shared_memory=true; index_keys=[task_id,entity_id,severity,fix,test,manual_check]

VALIDATION:

* format_valid=true
* no_pronouns=true
* entities_explicit=true
* high_density=true
* causal_chain_present=true
* ambiguity_detected=true
