Model: gpt-5-codex
Changed files: README.md; features/auth/README.md; features/auth/client/src/commonMain/kotlin/AuthFeatureService.kt; features/auth/client/src/commonMain/kotlin/KtorAuthFeature.kt; features/auth/common/build.gradle; features/auth/common/src/commonMain/kotlin/AuthFeature.kt; features/auth/common/src/commonMain/kotlin/Constants.kt; features/auth/common/src/commonMain/kotlin/models/AuthConfig.kt; features/auth/common/src/commonMain/kotlin/models/RegisterRequest.kt; features/auth/common/src/commonTest/kotlin/models/RegisterRequestTest.kt; features/auth/server/src/commonMain/kotlin/Config.kt; features/auth/server/src/commonMain/kotlin/Plugin.kt; features/auth/server/src/commonMain/kotlin/RegistrationEmailSender.kt; features/auth/server/src/commonMain/kotlin/configurators/AuthRoutingsConfigurator.kt; features/auth/server/src/commonMain/kotlin/models/AuthConfig.kt (deleted); features/auth/server/src/commonMain/kotlin/services/AuthFeatureService.kt; features/auth/server/src/commonTest/kotlin/services/AuthFeatureServiceTest.kt; features/deeplinks/README.md; features/email/README.md; features/email/server/build.gradle; features/email/server/src/commonMain/kotlin/Plugin.kt; features/email/server/src/commonMain/kotlin/models/EmailVerification.kt; features/email/server/src/commonMain/kotlin/models/EmailVerificationPayload.kt; features/email/server/src/commonMain/kotlin/services/EmailRegistrationInviteSender.kt; features/email/server/src/commonMain/kotlin/services/EmailVerificationDeepLinkHandler.kt; features/email/server/src/commonTest/kotlin/services/EmailRegistrationInviteSenderTest.kt; features/email/server/src/commonTest/kotlin/services/EmailVerificationDeepLinkHandlerTest.kt; features/email/server/src/commonTest/kotlin/services/FakeRolesRepo.kt; features/roles/README.md; features/roles/common/src/commonMain/kotlin/models/RoleConstants.kt; features/roles/server/src/commonMain/kotlin/RolesBootstrap.kt; features/roles/server/src/commonTest/kotlin/RolesBootstrapTest.kt; features/roles/server/src/jvmMain/kotlin/JVMPlugin.kt; features/ui/auth/README.md; features/ui/auth/src/androidMain/kotlin/ui/AuthView.kt; features/ui/auth/src/commonMain/kotlin/AuthStrings.kt; features/ui/auth/src/commonMain/kotlin/Plugin.kt; features/ui/auth/src/commonMain/kotlin/ui/AuthModel.kt; features/ui/auth/src/commonMain/kotlin/ui/AuthViewModel.kt; features/ui/auth/src/commonTest/kotlin/ui/AuthRegistrationValidationTest.kt; features/ui/auth/src/jsMain/kotlin/ui/AuthView.kt; features/ui/auth/src/jvmMain/kotlin/ui/AuthView.kt; server/dev.config.json; server/docker-compose.yml; server/sample.config.json; agents/task/27.07.2026_18.30.32-ae6ccedb-bc93-47f6-9a4e-54c1e81d8e30/003-coding.md

The coding pass extends the auth wire contract with a common `AuthConfig`, a public config route, optional registration email data, and a server-side registration email sender hook. Required-email registration now validates email presence, persists the address, and returns credentials only after invite delivery succeeds. Existing optional-email registration behavior remains available when the new flag is false.

The email server now owns verification invite delivery and the `email.registration_verification` deeplink handler. Verification payloads are registered polymorphically, invite URLs use the configured public host, port, and HTTP scheme, and repeated verification opens promote an existing account safely from `NewUser` to `User`.

Role bootstrap now assigns `NewUser` to new non-root registrations only when required-email registration is enabled. Root remains approved and privileged, and existing-user backfill continues to grant `User` without downgrading accounts. JS, JVM, and Android registration forms expose and validate the email field according to the shared auth configuration.

Local development now includes Mailpit on SMTP port 1025 with its web UI on port 8025. Development configuration enables deeplinks, Mailpit SMTP, and required-email registration; the sample configuration documents the new flag while leaving the production choice disabled by default. Auth, email, roles, deeplinks, UI auth, and root documentation were updated without changing any Operator Notes section.

Verification completed:

- `ast-index rebuild` completed successfully after Kotlin changes.
- The first relevant Gradle test/compile attempt found two errors: the UI submit-state flow used an unsupported six-flow `combine` shape, and the email plugin attempted Koin JSON resolution during `setupDI`.
- The single fix cycle replaced the UI flow with nested typed combines and decoded server configuration with a local unknown-key-tolerant `Json` instance.
- The second relevant command completed successfully: `./gradlew :wishlist.features.auth.common:jvmTest :wishlist.features.auth.server:jvmTest :wishlist.features.roles.server:jvmTest :wishlist.features.email.server:jvmTest :wishlist.features.ui.auth:jvmTest`.
- `git diff --check`, `jq empty server/dev.config.json`, and `jq empty server/sample.config.json` completed successfully.
- Mailpit live delivery and Compose runtime startup were not exercised; external SMTP integration remains a manual check.

ENTITY:
entity_id=issue73; type=coding_implementation; state=completed
entity_id=auth_registration_contract; type=wire_api; state=extended_with_email_and_config
entity_id=registration_email_sender; type=server_hook; state=implemented
entity_id=email_verification_deeplink; type=handler_flow; state=implemented_idempotently
entity_id=new_user_role; type=role_transition; state=implemented_with_safe_backfill
entity_id=registration_ui; type=multiplatform_ui; state=implemented_js_jvm_android
entity_id=mailpit_local_environment; type=development_service; state=implemented

CONTEXT:

* task_id=27.07.2026_18.30.32-ae6ccedb-bc93-47f6-9a4e-54c1e81d8e30; agent_id=coding-003; memory_ref=[PROMPT.md,001-planning.md,002-architecturing.md,agents/CODING.md,agents/patterns/server-routes.md,agents/patterns/bearer-auth.md,agents/patterns/auth-ui.md,agents/patterns/mvvm.md,agents/patterns/full-stack-feature.md]
* constraints=[product_files_and_003-coding_only,operator_notes_unchanged,no_push,normal_prose_report,AML-HIP_structured_blocks]

ACTION:

1. action=extend; target=auth_registration_contract; params={route=GET_/api/auth/config,request_field=email_nullable,config_field=requireEmailForRegistration,default=false}
2. action=connect; target=registration_email_sender; params={caller=AuthFeatureService,implementation=EmailRegistrationInviteSender,success_requirement=invite_delivery_true}
3. action=mint; target=email_verification_deeplink; params={handler_id=email.registration_verification,payload=EmailVerificationPayload(userId),route=GET_/api/links/{deeplink_uuid}}
4. action=promote; target=new_user_role; params={transition=NewUser→User,operation=exclude_NewUser_then_include_User,idempotent=true}
5. action=provision; target=mailpit_local_environment; params={smtp_port=1025,http_port=8025,compose_file=server/docker-compose.yml}
6. action=verify; target=product_changes; params={ast_index=rebuild,gradle_tests=passed,json_validation=passed,diff_check=passed}

REASON:

* condition=requireEmailForRegistration=false; requirement=legacy_registration_accepts_absent_or_present_optional_email_and_skips_invite_sender
* condition=requireEmailForRegistration=true; requirement=valid_email_and_successful_invite_delivery_before_credentials_return
* condition=verification_deeplink_opened_and_user_exists; requirement=NewUser_removed_and_User_granted
* condition=existing_user_backfill; requirement=User_granted_without_NewUser_downgrade

EXPECTED RESULT:

* entity_id=issue73; new_state=product_implementation_committed; location=git_commit
* entity_id=auth_registration_contract; new_state=client_server_wire_compatibility; location=features/auth/common+features/auth/client+features/auth/server
* entity_id=email_verification_deeplink; new_state=invite_creation_and_role_promotion; location=features/email/server+features/deeplinks
* entity_id=registration_ui; new_state=email_input_rendered_and_validated_on_three_platforms; location=features/ui/auth

VERIFICATION:

* check=ast_index_rebuild; expected=success
* check=relevant_gradle_tests; expected=success
* check=server_dev_config_json; expected=valid_json
* check=server_sample_config_json; expected=valid_json
* check=operator_notes_changed; expected=false

UNCERTAINTY:

* missing=live_mailpit_runtime_execution; ambiguity=external_smtp_delivery_requires_manual_smoke_test
* missing=full_android_and_js_distribution_build; ambiguity=targeted_jvm_multiplatform_ui_test_passed

REPETITION OF RESULT:

* entity_id=issue73; stored_in=shared_memory; status=available
* entity_id=product_changes; stored_in=git_index; status=ready_for_commit

COMMUNICATION:

* sender=coding-003; receiver=orchestrator; task_id=27.07.2026_18.30.32-ae6ccedb-bc93-47f6-9a4e-54c1e81d8e30; message_id=4c1d8021-0e8d-4c8d-9f58-1f8df8cc7a2e; protocol=AML-HIP

PERSISTENCE:

* local_memory=true; shared_memory=true; index_keys=[task_id,entity_id,intent,verification]

VALIDATION:

* format_valid=true
* no_pronouns=true
* entities_explicit=true
* high_density=true
* causal_chain_present=true
* ambiguity_detected=true
