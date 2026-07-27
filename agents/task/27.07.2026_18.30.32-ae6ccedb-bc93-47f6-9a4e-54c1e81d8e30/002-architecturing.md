Model: gpt-5-codex
Changed files: agents/task/27.07.2026_18.30.32-ae6ccedb-bc93-47f6-9a4e-54c1e81d8e30/002-architecturing.md

## Architecture fallback note

The delegated architecture workers did not return a report within repeated bounded waits. The main session completed the architecture pass from `001-planning.md`, repository source inspection, and existing feature constraints. The fallback report preserves the required architecture-stage handoff and records the implementation contract below.

## Model rationale

The repository guidance gives architecture priority to `fable`, then `opus`, then `sonnet`. The active model is `gpt-5-codex`; the report records the actual model rather than claiming an unavailable override.

## Decisions

The implementation will extend the existing auth contract without removing the current boolean registration probe. A new common `AuthConfig` DTO and `getConfig()` method will expose both `enableRegistration` and `requireEmailForRegistration`; `GET /auth/config` will serve the DTO. `RegisterRequest` and `register` will accept an optional validated `Email`. Existing callers remain source-compatible through default arguments where practical.

Email delivery will be decoupled from auth through an auth-server `RegistrationEmailSender` interface. The email server feature will bind an implementation that creates an `EmailVerificationPayload` deeplink, builds an absolute URL from the common server `publicHost`/port configuration, and sends the message through the existing `EmailsService`. A disabled implementation returns failure when SMTP or deeplink infrastructure is unavailable. Required-email registration fails when the invite cannot be sent; the implementation must avoid returning credentials for an account that cannot receive its approval link.

The email feature will own a `DeepLinkHandler` identified by a stable `DeepLinkHandlerId`. The handler will decode `EmailVerificationPayload`, verify the user still exists, remove `NewUserRole`, and grant `UserRole`. Repeated opens are safe because role inclusion/exclusion are idempotent. The handler will register its polymorphic serializer through the email server plugin. The deeplink core remains unchanged.

The roles feature will add `NewUserRole`. New-user reactive role assignment will receive the auth configuration: `root` always receives `UserRole` and `SuperAdminRole`; non-root users receive `NewUserRole` while email-required registration is enabled and `UserRole` otherwise. Existing-user backfill will continue granting `UserRole` and will never downgrade existing users to `NewUserRole`; this prevents enabling the option from locking out existing accounts. Approval promotion is the only path that changes a pending user from `NewUserRole` to `UserRole`.

The local development Compose file will add a Mailpit SMTP/UI service. `server/dev.config.json` will enable the deeplinks plugin, point SMTP at Mailpit, set a usable local `publicHost`, and enable required-email registration. `server/sample.config.json` will document the new auth flag and retain production SMTP placeholders. Production deployment continues to require an operator-supplied SMTP configuration.

## File-level architecture

### Auth common/client/server

- Add `features/auth/common/src/commonMain/kotlin/models/AuthConfig.kt` with serializable `enableRegistration` and `requireEmailForRegistration` fields.
- Extend `AuthFeature` with `getConfig(): AuthConfig`; retain `isRegistrationAvailable()` for existing consumers.
- Add `email: Email? = null` to `RegisterRequest`; add the direct `email.common` dependency to `auth/common`.
- Extend `AuthFeatureService` with `requireEmailForRegistration`, `RegistrationEmailSender?`, and registration validation. Reject missing email when required, persist supplied email, invoke the sender for required registrations, and return no credentials when invite delivery fails. Preserve password and duplicate-username behavior.
- Add `RegistrationEmailSender` to `auth/server`; the interface accepts the newly persisted `RegisteredUser` and returns delivery success.
- Move the wire-facing `AuthConfig` contract to `auth/common`; update `AuthRoutingsConfigurator`, `KtorAuthFeature`, and client service delegation for `GET /auth/config` and the extended registration body.
- Add `requireEmailForRegistration` to `auth/server.Config`, wire it through `auth/server/Plugin`, and keep the default `false` for backward compatibility.

### Email/deeplinks

- Add `EmailVerificationPayload(userId: UserId)` to the email server module as the polymorphic deeplink value.
- Add `EmailRegistrationInviteSender` and `EmailVerificationDeepLinkHandler` under email server services.
- Register the sender, handler, payload serializer, and required `DeepLinksService`/`RolesRepo` bindings in `features/email/server/Plugin.kt`.
- Build invite URLs as `http(s)://{publicHost}:{port}/api/links/{deeplinkId}` using the common server config. Keep link construction in a pure helper for unit tests.
- Add `deeplinks.common`/`deeplinks.server` dependencies to email server. Add `deeplinks.server.JVMPlugin` to dev configuration; sample configuration already lists it.

### Roles

- Add `NewUserRole = BaseRole("NewUser")` in `roles/common`.
- Extend `grantDefaultRoles` with a `requireEmailForRegistration` parameter, preserving a default `false` for current unit fixtures.
- Extend `backfillDefaultRoles` with the same parameter but keep existing users on `UserRole`; update `roles/server/JVMPlugin` to read auth `Config` and pass the flag to reactive and backfill paths.
- Add tests for pending non-root assignment, root exemption, unchanged backfill, promotion removing `NewUserRole`, and idempotent promotion.

### UI

- Extend `AuthModel` and its plugin implementation with auth config and `register(..., email)` support.
- Add email state and required-email state to `AuthViewModel`; include email validity in registration submit gating only when registration requires email. Parse via `Email.parse` rather than constructing an unchecked value.
- Render the email field only in registration mode across JS/JVM/Android views. Add a localized email placeholder. Login behavior stays unchanged.
- Platform-specific rendering is not fully covered by repository unit-test infrastructure; cover the common ViewModel validation/gating if a test fixture exists, and document manual JS/JVM/Android smoke verification.

### Configuration/docs

- Update `server/docker-compose.yml` with a Mailpit service exposing SMTP `1025` and web UI `8025`.
- Update `server/dev.config.json`, `server/sample.config.json`, root README, and feature READMEs for the auth flag, SMTP requirement, deeplink URL behavior, Mailpit usage, and `NewUser` transition.
- Never alter any `## Operator Notes` section.

## Contracts and failure behavior

- `GET /api/auth/config` → `AuthConfig` without authentication.
- `POST /api/auth/register` accepts `{username,password,email?}`. Missing/invalid required email, disabled registration, duplicate fields, unavailable delivery, and invalid password result in the existing `400` registration failure response.
- Optional email remains storable when the requirement is disabled; the invite sender is not called in that mode.
- Approval link handling remains `GET /api/links/{deeplink_uuid}` and returns the existing `200`/`404` mapping.
- SMTP delivery uses the existing `EmailsService`; the live Mailpit/SMTP server is external integration and cannot be deterministically unit-tested here. Use a fake sender/service for all branching and message/link assertions, then perform manual Compose smoke verification.

## Test specifications for coding

### Auth tests

- `AuthFeatureService.getConfig` returns both configured flags.
- Registration disabled returns `null` and does not touch users, passwords, or sender.
- Required-email registration rejects `null` email and accepts a valid email.
- Optional-email registration accepts absent and present email values and stores the supplied value.
- Required-email registration invokes the sender with the created user and returns credentials only when sender success is `true`.
- Required-email registration returns `null` when sender failure is reported; no successful credentials are exposed.
- `RegisterRequest` serializes/deserializes the optional email field and remains compatible when the field is omitted.
- `GET /auth/config` route and client transport return the DTO; existing registration-availability behavior remains stable.

### Email/deeplink tests

- URL builder produces the configured scheme, host, port, API prefix, and generated deeplink id.
- Invite sender returns `false` when SMTP/deeplink dependencies are absent.
- Invite sender creates a deeplink with the expected handler id and `EmailVerificationPayload`, sends recipient/subject/body containing the absolute link, and returns the fake transport result.
- Handler returns `false` for wrong payload type and missing user.
- Handler promotes an existing user by excluding `NewUserRole` and including `UserRole`.
- Repeated handler calls leave exactly the approved role state and do not fail.
- Polymorphic payload registration can encode/decode through the application `Json` module.

### Roles tests

- Non-root new user receives `NewUserRole` when the flag is true and not `UserRole`.
- Root receives `UserRole` and `SuperAdminRole` regardless of the flag.
- Flag false preserves existing `UserRole` behavior.
- Backfill grants `UserRole` to existing non-root users even when the flag is true and does not add `NewUserRole`.
- Repeated backfill and repeated promotion are idempotent.

### UI/config tests

- ViewModel submit state allows login with username/password independently of email policy.
- Required-email registration is disabled for blank or syntactically invalid email and enabled for valid email.
- Optional-email registration permits a blank email and rejects malformed nonblank email before transport.
- Config JSON decodes with the new flag omitted (`false`) and explicitly enabled (`true`).
- Compose YAML validates structurally; Mailpit exposes the documented ports. Actual SMTP delivery remains manual integration coverage.

## README updates

Add architecture notes to `features/auth/README.md`, `features/email/README.md`, `features/deeplinks/README.md`, and `features/roles/README.md` describing the new registration config, registration sender hook, verification payload/handler, role transition, existing-user backfill rule, and SMTP/deeplink dependency. Update root README's configuration and local startup sections with `requireEmailForRegistration`, Mailpit ports, and the approval-link flow.

## AML-HIP

ENTITY:
entity_id=issue73; type=architecture_handoff; state=ready_for_coding
entity_id=auth_config; type=wire_contract; state=extended
entity_id=registration_sender; type=auth_server_extension; state=defined
entity_id=email_verification_payload; type=deeplink_payload; state=defined
entity_id=new_user_role; type=role_transition; state=defined
entity_id=local_mailpit; type=deployment_service; state=defined

CONTEXT:

* task_id=27.07.2026_18.30.32-ae6ccedb-bc93-47f6-9a4e-54c1e81d8e30; agent_id=architecturing-002-fallback; memory_ref=[001-planning.md,repository_source_inspection,features/auth/README.md,features/email/README.md,features/deeplinks/README.md,features/roles/README.md]
* constraints=[preserve_existing_auth_probe,default_requireEmailForRegistration=false,do_not_modify_operator_notes,SMTP_external_integration_manual_verification]

ACTION:

1. action=extend; target=auth_config; params={fields=[enableRegistration:Boolean,requireEmailForRegistration:Boolean],route=GET_/api/auth/config,default_requireEmailForRegistration=false}
2. action=connect; target=registration_sender; params={caller=AuthFeatureService,implementation=EmailRegistrationInviteSender,condition=requireEmailForRegistration=true}
3. action=mint; target=email_verification_payload; params={service=DeepLinksService,handler_id=email.verify,payload=UserId}
4. action=promote; target=new_user_role; params={input=opened_verification_deeplink,transition=NewUser→User,repeat_safe=true}
5. action=provision; target=local_mailpit; params={smtp_port=1025,http_port=8025,compose_file=server/docker-compose.yml}

REASON:

* condition=required_email_registration=true; requirement=registration_requires_valid_email_and_successful_invite_delivery
* condition=verification_link_opened; requirement=remove_NewUserRole_and_grant_UserRole
* condition=existing_user_before_flag_enablement; requirement=backfill_must_not_downgrade_or_pending_lock_existing_account
* condition=SMTP_or_deeplink_dependency_unavailable; requirement=required_registration_must_not_return_successful_credentials

EXPECTED RESULT:

* entity_id=auth_config; new_state=serializable_common_contract_and_server_route; location=features/auth/common+features/auth/server
* entity_id=registration_sender; new_state=DI_hook_with_email_implementation; location=features/auth/server+features/email/server
* entity_id=email_verification_payload; new_state=persisted_polymorphic_deeplink; location=features/email/server+features/deeplinks
* entity_id=new_user_role; new_state=pending_to_approved_transition; location=features/roles
* entity_id=local_mailpit; new_state=compose_enabled_SMTP_test_service; location=server/docker-compose.yml

VERIFICATION:

* check=architecture_report_present; expected=true
* check=test_specification_exists_for_each_planned_symbol_route_config_path; expected=true
* check=untestable_external_or_platform_behavior_explicitly_flagged; expected=true
* check=operator_note_modification; expected=false

UNCERTAINTY:

* missing=live_SMTP_server_in_unit_environment; ambiguity=external_delivery_success_requires_manual_Mailpit_smoke_test
* missing=platform_UI_rendering_test_harness; ambiguity=common_ViewModel_tests_plus_manual_JS_JVM_Android_checks

REPETITION OF RESULT:

* entity_id=issue73; stored_in=shared_memory; status=available
* entity_id=new_user_role; stored_in=shared_memory; status=ready_for_coding

COMMUNICATION:

* sender=architecturing-002-fallback; receiver=coding-003; task_id=27.07.2026_18.30.32-ae6ccedb-bc93-47f6-9a4e-54c1e81d8e30; message_id=7b9b3d7c-4c0d-4af7-9b2b-3e8e56d1e66a; protocol=AML-HIP

PERSISTENCE:

* local_memory=true; shared_memory=true; index_keys=[task_id,entity_id,route,config,role]

VALIDATION:

* format_valid=true
* no_pronouns=true
* entities_explicit=true
* high_density=true
* causal_chain_present=true
* ambiguity_detected=true
