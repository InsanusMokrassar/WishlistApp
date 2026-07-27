Model: gpt-5-codex
Changed files: features/auth/README.md; features/auth/common/src/commonMain/kotlin/AuthFeature.kt; features/auth/common/src/commonTest/kotlin/models/RegisterRequestTest.kt; features/auth/server/src/commonMain/kotlin/services/AuthFeatureService.kt; features/auth/server/src/commonTest/kotlin/services/AuthFeatureServiceTest.kt; features/deeplinks/README.md; features/deeplinks/server/src/commonMain/kotlin/services/DeepLinksService.kt; features/email/README.md; features/email/server/src/commonMain/kotlin/services/EmailRegistrationInviteSender.kt; features/email/server/src/commonTest/kotlin/services/EmailDeepLinkIntegrationTest.kt; features/email/server/src/commonTest/kotlin/services/EmailRegistrationInviteSenderTest.kt; features/roles/README.md; features/roles/server/src/commonMain/kotlin/RolesBootstrap.kt; features/roles/server/src/commonTest/kotlin/RolesBootstrapTest.kt; features/roles/server/src/jvmMain/kotlin/JVMPlugin.kt; server/dev.config.json; agents/task/27.07.2026_18.30.32-ae6ccedb-bc93-47f6-9a4e-54c1e81d8e30/008-coding.md

## Cycle-two implementation

Implemented the bounded remediations approved in `006-planning.md` and `007-architecturing.md`. Role transitions now use one process-local `Mutex` for the complete default-grant and verification-promotion operations. Required-email default granting checks for an existing approved `UserRole` before adding `NewUserRole`, so a delayed flow callback cannot undo verification.

Required-email registration now fails closed and compensates inside the existing authentication write lock. A false or throwing invite sender removes the stored password and newly created user, allowing the same username to retry. `DeepLinksService` now exposes the server-side removal operation used by the invite sender to remove a minted link after SMTP returns false or throws; successful sends retain the link.

`AuthFeature.getConfig()` now has a compatibility default derived from `isRegistrationAvailable()` with optional email verification. The development server continues listening on `0.0.0.0` while publishing links through `127.0.0.1`. The canonical handler assertion records `EmailVerification.handlerId.string == "email.registration_verification"`; the historical mismatch in `002-architecturing.md` was left untouched.

Added deterministic regression and integration coverage for delayed role callbacks, failed-then-successful registration retry, SMTP false and exception cleanup, legacy authentication configuration, application polymorphic JSON round-tripping, and full verification deeplink dispatch to the approved role state. Existing Ktor route/client test infrastructure does not provide a test host or MockEngine dependency, so route transport coverage was not added. Live Mailpit delivery, Docker startup, and interactive platform rendering remain manual checks.

Updated affected README and KDoc statements for role state, failed invite cleanup, deeplink cleanup, handler configuration, and compatibility behavior. Existing `Operator Notes` sections were preserved, and no prior task report was edited.

## Verification

`ast-index rebuild` completed successfully after the Kotlin changes. The focused JVM suite passed for auth common, auth server, roles server, email server, deeplinks server, and UI auth. The repository-wide `./gradlew build` passed with 4266 actionable tasks. `git diff --check` passed. JSON host assertions passed, and `docker compose -f server/docker-compose.yml config` passed with the repository's existing obsolete `version` warning. No fix cycle was required because the focused and full build runs completed without errors.

## AML-HIP

ENTITY:
entity_id=issue73_cycle2_coding; type=coding_report; state=completed
entity_id=role_transition_remediation; type=concurrency_fix; state=implemented
entity_id=invite_cleanup_remediation; type=compensation_fix; state=implemented
entity_id=integration_regression_suite; type=test_coverage; state=implemented
entity_id=compatibility_documentation_remediation; type=compatibility_docs; state=implemented

CONTEXT:

* task_id=27.07.2026_18.30.32-ae6ccedb-bc93-47f6-9a4e-54c1e81d8e30; agent_id=coding-008; memory_ref=[005-validating.md,006-planning.md,007-architecturing.md,003-coding.md,004-verification.md,agents/ALL.md,agents/CODING.md,agents/PROTOCOL.md,agents/GIT.md]
* constraints=[cycle_two_remediations_only,prior_reports_immutable,operator_notes_preserved,ast_index_rebuild_required,no_push,commit_current_step_report]

ACTION:

1. action=synchronize; target=role_transition_remediation; params={mutex=roleTransitionMutex,scope=[grantDefaultRoles,promoteNewUserToUser],guard=approved_UserRole_before_NewUserRole}
2. action=compensate; target=invite_cleanup_remediation; params={auth_cleanup=[password_unset,user_delete],deeplink_cleanup=removeDeepLink,failures=[smtp_false,smtp_exception],retry=same_username_success}
3. action=compatibilize; target=compatibility_documentation_remediation; params={default_getConfig=registration_availability,default_required_email=false,publicHost=127.0.0.1,canonical_handler_id=email.registration_verification}
4. action=regress; target=integration_regression_suite; params={tests=[role_ordering,registration_retry,smtp_cleanup,legacy_auth,polymorphic_json,deeplink_dispatch]}
5. action=verify; target=issue73_cycle2_coding; params={ast_index=rebuild,focused_tests=passed,full_build=passed,compose_config=passed,git_diff_check=passed}

REASON:

* condition=async_role_callback_after_approval → action=shared_mutex_plus_approved_role_guard → result=NewUserRole_absent_after_approval
* condition=required_invite_false_or_exception → action=remove_password_user_and_minted_deeplink → result=same_username_retry_available
* condition=legacy_AuthFeature_without_getConfig → action=default_getConfig_implementation → result=source_compatibility_preserved

EXPECTED RESULT:

* entity_id=role_transition_remediation; new_state=approved_non_root_exactly_UserRole; location=features/roles/server
* entity_id=invite_cleanup_remediation; new_state=failed_registration_state_absent; location=features/auth/server+features/deeplinks/server+features/email/server
* entity_id=integration_regression_suite; new_state=cycle_two_paths_covered; location=features/*/commonTest
* entity_id=compatibility_documentation_remediation; new_state=configuration_and_docs_aligned; location=features/*+server/dev.config.json

VERIFICATION:

* check=ast_index_rebuild; expected=success
* check=focused_jvm_tests; expected=success
* check=repository_gradle_build; expected=success
* check=dev_config_hosts; expected=host_0.0.0.0_and_publicHost_127.0.0.1
* check=compose_config; expected=success_with_existing_version_warning
* check=prior_report_scope; expected=unchanged

UNCERTAINTY:

* missing=live_Mailpit_delivery; ambiguity=external_SMTP_runtime_unverified
* missing=interactive_platform_rendering; ambiguity=browser_JVM_Android_manual_validation_unavailable
* missing=Ktor_route_test_host; ambiguity=existing_test_dependencies_do_not_support_transport_harness

REPETITION OF RESULT:

* entity_id=issue73_cycle2_coding; stored_in=shared_memory; status=completed
* entity_id=role_transition_remediation; stored_in=shared_memory; status=implemented
* entity_id=invite_cleanup_remediation; stored_in=shared_memory; status=implemented

COMMUNICATION:

* sender=coding-008; receiver=issue73_workflow; task_id=27.07.2026_18.30.32-ae6ccedb-bc93-47f6-9a4e-54c1e81d8e30; message_id=cycle2-coding-complete; protocol=AML-HIP

PERSISTENCE:

* local_memory=true; shared_memory=true; index_keys=[task_id,entity_id,role_race,cleanup,compatibility,test,commit]

VALIDATION:

* format_valid=true
* no_pronouns=true
* entities_explicit=true
* high_density=true
* causal_chain_present=true
* ambiguity_detected=true
