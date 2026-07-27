Model: gpt-5-codex
Changed files: agents/task/27.07.2026_18.30.32-ae6ccedb-bc93-47f6-9a4e-54c1e81d8e30/006-planning.md

## Model rationale

The planning role priority is `fable`, followed by `opus` and `sonnet`. The active harness model is `gpt-5-codex`, so this report records the actual model rather than claiming an unavailable override.

## Cycle 2 planning result

The validator’s High finding is reproducible from the committed implementation. `UsersRepo.newObjectsFlow.subscribeLoggingDropExceptions` launches the role callback asynchronously in `features/roles/server/src/jvmMain/kotlin/JVMPlugin.kt:55-57`; the callback can run after `EmailVerificationDeepLinkHandler` has already promoted the account in `features/email/server/src/commonMain/kotlin/services/EmailVerificationDeepLinkHandler.kt:32-36`. The existing `grantDefaultRoles` implementation always adds `NewUserRole` for a required-email non-root account, so a verified account can finish with both `UserRole` and `NewUserRole`.

The three Medium findings are also confirmed. Required-email registration persists the user and password before delivery and returns `null` without compensating cleanup at `features/auth/server/src/commonMain/kotlin/services/AuthFeatureService.kt:126-134`; the invite sender can leave a persisted deeplink when SMTP reports failure. Development configuration binds on `0.0.0.0` but advertises `publicHost: "0.0.0.0"` at `server/dev.config.json:2-4`. `AuthFeature.getConfig()` is abstract at `features/auth/common/src/commonMain/kotlin/AuthFeature.kt:20-25`, so downstream implementations written against the previous interface contract must add a method before compiling.

The Low findings are bounded documentation, handoff, and integration-test gaps. The runtime handler id is consistently `email.registration_verification`, but the AML-HIP action in `002-architecturing.md` says `email.verify`. The auth model table omits `getConfig()`, the roles README still describes two hardcoded roles and a universally granted `User`, and the focused test fixtures need complete strict-KDoc coverage. Existing unit tests do not cover route transport, application-level deeplink polymorphism, full deeplink dispatch, or the callback/promotion ordering.

No product file has been changed in this planning stage. The next Architecture and Coding stages should implement the following bounded changes.

## Exact remediation plan

### High — asynchronous role callback can re-add `NewUserRole`

Change `features/roles/server/src/commonMain/kotlin/RolesBootstrap.kt` so `grantDefaultRoles` and `promoteNewUserToUser` share one role-transition synchronization primitive. A `kotlinx.coroutines.sync.Mutex` around the complete read/modify/write sequence is sufficient for the single in-process `RolesRepo` used by the server. The required-email branch must check `rolesRepo.contains(subject, UserRole)` while holding the same lock before adding `NewUserRole`; a callback that runs after verification must observe the approved role and skip the pending-role grant. Promotion must hold the same lock while excluding `NewUserRole` and including `UserRole`. Root handling and optional-registration behavior remain unchanged, and backfill continues to use the same helper with the required-email flag disabled.

The role invariant is: a non-root account awaiting verification has `NewUserRole` and no `UserRole`; an approved account has `UserRole` and no `NewUserRole`; root has `UserRole` and `SuperAdminRole` and never receives `NewUserRole`. The synchronization must cover both the reactive callback and the verification handler, not only the unit-test helper.

Add a deterministic regression test in `features/roles/server/src/commonTest/kotlin/RolesBootstrapTest.kt`. Start a required-email flow collector on a controlled test dispatcher, create a non-root user, invoke `promoteNewUserToUser` before releasing the delayed callback, then advance the dispatcher. Assert the final direct role set is exactly `{UserRole}`. Retain the existing tests for pending assignment, root exemption, optional mode, backfill, and promotion idempotency.

Update the roles architecture documentation to describe the synchronized transition and the approved-role guard. The documentation must no longer call the two operations harmless merely because `includeDirect` is idempotent; idempotent individual writes do not prevent an ordering race.

### Medium — failed invite delivery leaves an unusable registration

Make failure compensation explicit across the auth and email boundaries.

1. In `AuthFeatureService.register`, when required-email delivery returns `false` or the sender is absent, remove the password with `passwordsRepo.unset(created.id)` and remove the just-created user with `writeUsersRepo.deleteById(created.id)` before returning `null`. Keep the operation inside the existing auth write lock. A failed registration must not reserve the username and a subsequent attempt with the same username must be able to succeed.
2. Add a removal operation to `DeepLinksService` backed by the existing `DeepLinksRepo` key-value removal API. In `EmailRegistrationInviteSender`, retain the newly minted id, treat a false or thrown email-send result as delivery failure, and remove the deeplink before returning `false`. Successful delivery keeps the deeplink for verification. Missing SMTP or deeplink infrastructure must still fail without creating a link.
3. Keep the existing fail-closed HTTP behavior: failed delivery returns the existing registration failure response and never returns credentials. Do not expose a new public deeplink-deletion route.

Extend `AuthFeatureServiceTest` with a sender that fails once and succeeds once. After the first failed registration, assert no user and no password remain; retry the same username and valid email, then assert credentials and the persisted user exist. Extend `EmailRegistrationInviteSenderTest` with a false-send case and assert the fake deeplink repository is empty after failure; retain the successful-send assertion that checks the handler id and payload.

### Medium — development invite URL advertises a wildcard address

Change only `server/dev.config.json` for this finding: retain `host: "0.0.0.0"` as the server bind address and set `publicHost` to `"127.0.0.1"`, matching the documented local URL and Mailpit workflow. Add a configuration assertion or JSON-focused test that the development public host is client-reachable and differs from the wildcard bind address. Keep production `publicHost` operator-configurable.

### Medium — abstract `AuthFeature.getConfig()` breaks downstream implementations

Give `AuthFeature.getConfig()` a default implementation in `features/auth/common/src/commonMain/kotlin/AuthFeature.kt` that derives `enableRegistration` from the already-required `isRegistrationAvailable()` method and defaults `requireEmailForRegistration` to `false`. Keep the server and client implementations’ explicit overrides so configured email policy still reaches current UI and transport code. Preserve the existing `register(..., email: Email? = null)` source compatibility.

Add an auth-common compatibility test containing a legacy-style `AuthFeature` implementation that implements the pre-issue methods but omits `getConfig()`. Call the default method and assert `AuthConfig(enableRegistration = expected, requireEmailForRegistration = false)`. Keep the existing serialization tests for omitted and explicit policy fields.

### Low — missing cross-feature integration coverage

Add focused integration tests at the smallest available existing test layer; no live PostgreSQL, SMTP, or interactive platform runtime is required for deterministic tests.

- Add an auth-server route test around `AuthRoutingsConfigurator` that exercises `GET /api/auth/config` and `POST /api/auth/register`, verifies the `AuthConfig` wire response, and verifies that the optional `email` request field reaches the server feature. If no route-test harness exists, add the minimal Ktor test-host dependency to the auth-server test source set and install only the JSON/content-negotiation components required by the configurator.
- Add an auth-client transport test using Ktor `MockEngine` that verifies the config path, registration path, serialized email field, and legacy availability path. The test must assert HTTP-only behavior rather than service-side storage behavior.
- Add an email/deeplinks serialization test using the application `Json` serializer aggregation, `DeepLinkHandlerInfo`, and `EmailVerificationPayload`; assert polymorphic encode/decode succeeds with the registered module.
- Add a deeplinks-server dispatch test that mints a link through `DeepLinksService.createDeepLink`, opens the link through `DeepLinksService.handle`, and asserts `HandleResult.Handled` plus the exact approved role set. Keep direct handler tests for wrong payload and missing user.
- Add the controlled-dispatch role regression described under the High finding. This test is the required cross-feature ordering coverage that the previous unit-only split allowed to remain invisible.
- Keep live Mailpit delivery, Docker startup, and JS/JVM/Android interactive rendering as manual smoke checks. Continue validating JSON and Compose structure in verification.

### Low — handler-id mismatch in the architecture handoff

Use `EmailVerification.handlerId` as the single source of truth and use the canonical literal `email.registration_verification` in every new code, test, and documentation reference. Add an explicit test assertion for the canonical literal in the email verification tests. The historical `002-architecturing.md` report is an immutable prior-stage record under the step-file rules; the next coding report must record the correction rather than editing that report. No runtime identifier change is required because the committed source and feature READMEs already use the canonical id.

### Low — stale documentation and strict KDoc deviations

Update `features/auth/README.md` so the `AuthFeature` model row includes `getConfig(): AuthConfig`. Update `features/roles/README.md` so the overview and subscribe-then-backfill explanation describe `SuperAdmin`, `User`, and pending `NewUser`, including the conditional grant and synchronized promotion invariant.

Complete strict KDoc coverage for every newly added or modified test helper and class-level fixture property in the focused suites, including the fake deeplink repository and the user/deeplink fixture values in auth, email, and roles tests. KDocs must describe fixture purpose and identity rather than merely repeat names. Do not alter any `## Operator Notes` section.

## Proposed implementation and verification sequence

Architecture should first confirm the shared role lock and the cleanup ownership boundary, then produce test stubs for the delayed callback, retryable registration, failed-link deletion, interface default, route/client transport, serializer aggregation, and full deeplink dispatch. Coding should implement the product and documentation changes in the listed feature files, run `ast-index rebuild` after Kotlin changes, and run focused module tests plus the relevant multiplatform builds. Verification should run the full build, parse test totals, validate both JSON files and `docker compose -f server/docker-compose.yml config`, and check that only the intended product files and the current step report are committed.

Acceptance requires all of the following:

- A verification link opened before the asynchronous role callback completes leaves exactly `UserRole` after the callback completes.
- A failed required-email invite leaves no user, password, or deeplink and permits same-username retry.
- A successful invite retains one deeplink with handler id `email.registration_verification` and the expected `EmailVerificationPayload`.
- Development invite URLs use `http://127.0.0.1:8196/api/links/{id}` while server binding remains `0.0.0.0`.
- A legacy `AuthFeature` implementation without `getConfig()` still compiles and receives the default optional-email policy.
- Route, client transport, application serialization, and full deeplink dispatch tests pass in addition to the existing focused suites.
- Auth and roles READMEs and focused test KDocs are current; no Operator Notes content changes.

## QUESTIONS FOR OPERATOR

None. Validator findings, repository APIs, current configuration, and the requested issue behavior determine the remediation without an additional product decision.

## Architecture handoff

The next Architecture role should treat the cleanup and role-transition synchronization as correctness requirements, not optional hardening. The preferred boundary is: auth removes the persisted account/password after sender failure, while the email sender removes the deeplink it minted after transport failure. The preferred race fix is one shared coroutine mutex plus a `UserRole` presence guard around the complete role transition. The public development host is `127.0.0.1`; the bind host remains `0.0.0.0`; the public `AuthFeature` API receives a default `getConfig()` implementation.

## AML-HIP

ENTITY:
entity_id=issue73_cycle2; type=planning_report; state=ready_for_architecture
entity_id=role_transition_race; type=High_finding; state=remediation_defined
entity_id=failed_invite_cleanup; type=Medium_finding; state=remediation_defined
entity_id=development_invite_host; type=Medium_finding; state=remediation_defined
entity_id=auth_interface_compatibility; type=Medium_finding; state=remediation_defined
entity_id=integration_test_coverage; type=Low_finding; state=remediation_defined
entity_id=handler_id_handoff; type=Low_finding; state=normalization_defined
entity_id=documentation_kdoc_deviations; type=Low_finding; state=remediation_defined

CONTEXT:

* task_id=27.07.2026_18.30.32-ae6ccedb-bc93-47f6-9a4e-54c1e81d8e30; agent_id=planning-006; memory_ref=[PROMPT.md,001-planning.md,002-architecturing.md,003-coding.md,004-verification.md,005-validating.md,agents/ALL.md,agents/PLAN.md,agents/GIT.md,features/auth/README.md,features/email/README.md,features/deeplinks/README.md,features/roles/README.md,features/ui/auth/README.md]
* constraints=[edit_only_current_step_file,no_product_changes,no_push,commit_only_current_step_file,normal_prose_narrative,AML_HIP_structured_blocks,operator_notes_immutable]
* validator_findings=[High_role_callback_race,Medium_failed_invite_cleanup,Medium_dev_publicHost,Medium_auth_getConfig_compatibility,Low_integration_coverage,Low_handler_id_mismatch,Low_docs_KDocs]

ACTION:

1. action=synchronize; target=role_transition_race; params={implementation=shared_Mutex,guard=UserRole_presence_before_NewUserRole_grant,scope=[grantDefaultRoles,promoteNewUserToUser],test=delayed_callback_after_promotion}
2. action=compensate; target=failed_invite_cleanup; params={auth_cleanup=[passwordsRepo.unset,writeUsersRepo.deleteById],email_cleanup=DeepLinksService.removeDeepLink,success_condition=same_username_retryable}
3. action=configure; target=development_invite_host; params={bind_host=0.0.0.0,public_host=127.0.0.1,invite_url=http://127.0.0.1:8196/api/links/{id}}
4. action=compatibilize; target=auth_interface_compatibility; params={getConfig=default_method,enableRegistration_source=isRegistrationAvailable,requireEmailForRegistration_default=false}
5. action=integrate; target=integration_test_coverage; params={tests=[auth_route,auth_client_transport,application_polymorphism,deeplink_dispatch,role_ordering]}
6. action=normalize; target=handler_id_handoff; params={canonical_constant=EmailVerification.handlerId,canonical_literal=email.registration_verification,historical_report=immutable}
7. action=document; target=documentation_kdoc_deviations; params={readmes=[features/auth/README.md,features/roles/README.md],kdocs=[test_helpers,class_fixture_properties],operator_notes=unchanged}

REASON:

* condition=async_role_callback_after_verification → action=approved_role_guard_plus_shared_mutex → result=UserRole_only
* condition=invite_delivery_false_or_exception → action=deeplink_removal_plus_password_removal_plus_user_removal → result=retryable_registration_state
* condition=publicHost=0.0.0.0 → action=development_public_host_correction → result=client_reachable_invite_url
* condition=abstract_getConfig_method → action=default_interface_implementation → result=legacy_AuthFeature_source_compatibility
* condition=route_serialization_dispatch_coverage_absent → action=integration_test_addition → result=runtime_flow_coverage
* condition=handler_id_handoff_mismatch → action=canonical_id_normalization → result=stage_handoff_consistency
* condition=stale_readmes_or_incomplete_KDocs → action=documentation_and_KDoc_update → result=process_compliance

EXPECTED RESULT:

* entity_id=role_transition_race; new_state=verified_accounts_cannot_receive_NewUserRole_after_promotion; location=features/roles/server
* entity_id=failed_invite_cleanup; new_state=user_password_deeplink_removed_on_failed_delivery; location=features/auth/server+features/email/server+features/deeplinks/server
* entity_id=development_invite_host; new_state=127.0.0.1_public_host; location=server/dev.config.json
* entity_id=auth_interface_compatibility; new_state=legacy_implementations_compile_without_getConfig_override; location=features/auth/common
* entity_id=integration_test_coverage; new_state=route_transport_serialization_dispatch_ordering_covered; location=feature_commonTest_sources
* entity_id=handler_id_handoff; new_state=email.registration_verification_canonical; location=source_tests_documentation
* entity_id=documentation_kdoc_deviations; new_state=auth_roles_docs_and_test_KDocs_current; location=features/auth/README.md+features/roles/README.md+focused_test_sources

VERIFICATION:

* check=delayed_role_callback_final_set; expected={roles=[UserRole],forbidden=[NewUserRole]}
* check=failed_registration_cleanup; expected={user_count=0,password_count=0,deeplink_count=0,retry=true}
* check=successful_invite_persistence; expected={handler_id=email.registration_verification,payload=EmailVerificationPayload(userId),delivery=true}
* check=development_config; expected={host=0.0.0.0,publicHost=127.0.0.1,json_valid=true}
* check=legacy_auth_implementation; expected={getConfig_override=false,enableRegistration=delegated,requireEmailForRegistration=false}
* check=integration_suites; expected={route=true,client_transport=true,polymorphism=true,deeplink_dispatch=true,ordering=true}
* check=repository_scope; expected={product_files_changed_by_planning=false,current_step_file_changed=true}

UNCERTAINTY:

* missing=live_Mailpit_delivery; ambiguity=external_SMTP_acceptance_requires_manual_smoke_test
* missing=interactive_JS_JVM_Android_rendering; ambiguity=platform_visual_behavior_requires_manual_smoke_test
* missing=historical_002_report_rewrite_authority; ambiguity=step_file_immutability_requires_correction_note_in_new_report

REPETITION OF RESULT:

* entity_id=issue73_cycle2; stored_in=shared_memory; status=ready_for_architecture
* entity_id=role_transition_race; stored_in=shared_memory; status=High_finding_remediation_required
* entity_id=failed_invite_cleanup; stored_in=shared_memory; status=Medium_finding_remediation_required

COMMUNICATION:

* sender=planning-006; receiver=architecture-007; task_id=27.07.2026_18.30.32-ae6ccedb-bc93-47f6-9a4e-54c1e81d8e30; message_id=8f9b3a7c-9f6a-4e40-8a5e-1d6f3b2c4a70; protocol=AML-HIP

PERSISTENCE:

* local_memory=true; shared_memory=true; index_keys=[task_id,entity_id,severity,fix,test,acceptance]

VALIDATION:

* format_valid=true
* no_pronouns=true
* entities_explicit=true
* high_density=true
* causal_chain_present=true
* ambiguity_detected=true
