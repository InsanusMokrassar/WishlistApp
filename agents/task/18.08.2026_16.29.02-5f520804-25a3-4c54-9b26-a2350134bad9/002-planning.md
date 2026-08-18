Model: OpenAI GPT-5.6 Terra (ML)
Changed files: agents/task/18.08.2026_16.29.02-5f520804-25a3-4c54-9b26-a2350134bad9/002-planning.md

OpenAI GPT-5.6 Terra (ML) is appropriate because the selected result contract crosses serialized API models, authorization security boundaries, role-transition races, credential storage, and multiplatform UI behavior. The work is bounded, but coherent planning across those established feature boundaries is required before implementation.

## Task understanding

Authorization is conditional on the direct `User` role. Password login, refresh-token exchange, access-token bearer authentication, and direct token-to-user lookup must all fail closed for any account without that role. This prevents a pending `NewUser` account from receiving or using credentials and also makes role removal take effect for already-issued bearer tokens.

Required-email registration remains successful after the account is stored, marked `NewUser`, given a password, and sent its verification invite. Its success result is now explicitly pending verification: it contains no credentials, must not be persisted as a session, and becomes eligible for ordinary login only after the email deeplink promotes the account to `User`.

## Operator question and answer

The prior planning step asked how a successful required-email registration should be represented without credentials. The operator chose an explicit pending-email-verification success result. The UI must close the registration form and present a check-email confirmation. No credentials may be returned or stored for that result.

## Investigation and decisions

`AuthFeature.register` currently returns nullable `AuthCredentials`, which makes HTTP 200 synonymous with an authenticated session and maps `null` to HTTP 400. The selected contract therefore requires a shared serialized result hierarchy rather than a nullable credential workaround. The recommended shape is `RegistrationResult.Authorized(credentials)` and `RegistrationResult.PendingEmailVerification`; only the former carries `AuthCredentials`.

Auth cannot reference `RolesRepo` or `UserRole` directly because `roles/server` already references `auth/server`. An Auth-owned `UserRoleAuthorization` interface should therefore expose only `ensureUserRole(RegisteredUser)` for immediate generic self-registration assignment and `hasUserRole(UserId)` for authorization checks. Roles will implement the interface using its existing transition mutex, `grantDefaultRoles`, and direct `UserRole` membership check. The existing roles creation-flow subscription remains idempotent, while the synchronous ensure operation closes the race between optional-email account creation and credential issuance.

The selected contract resolves the only open architecture question. No further operator questions remain.

## Final plan

### Shared registration contract

Add a `@Serializable` sealed `RegistrationResult` model in `features/auth/common`: `Authorized` wraps `AuthCredentials`; `PendingEmailVerification` is a credential-free data object. Change both `AuthFeature.register` overloads, the client and server feature implementations, and test fixtures to return `RegistrationResult?`. Preserve HTTP 400 for actual registration refusal, duplicate data, failed invite delivery, missing required infrastructure, and invalid passwords; return HTTP 200 with the selected result for either successful outcome.

Update `AuthRoutingsConfigurator` to serialize the result hierarchy and `KtorAuthFeature` to deserialize it. Update the client `AuthFeatureService` to save credentials only for `Authorized`; `PendingEmailVerification` must leave persistent credentials unset. Login and refresh retain their existing `AuthCredentials?` contract.

### Role-gated authorization and registration sequencing

Introduce the narrow Auth-owned `UserRoleAuthorization` dependency-inversion interface and bind the Roles implementation in `roles/server`. Its synchronous generic-registration operation must reuse the role transition mutex and existing default-role rule, then confirm direct `UserRole`; its read operation must check that same role. When no implementation is installed, Auth must fail closed rather than issue credentials.

Make `AuthFeatureService.issueCredentialsFor` conditional on `hasUserRole`, then apply the same gate to bearer authentication and token-to-user lookup so current sessions cannot bypass a later role removal. Login and refresh consequently return `null` without issuing replacement credentials when the account lacks `User`.

For optional-email registration, create the account, synchronously establish the generic `User` role through the bridge, then return `RegistrationResult.Authorized` only after credential issuance succeeds. For required-email registration, retain the existing reserve, pending-role, delivery, validation, password-store, compensation, cancellation, and lock-boundary behavior, but return `RegistrationResult.PendingEmailVerification` immediately after password storage and never call the credential issuer. Email verification remains responsible only for promotion; a post-promotion login then satisfies the authorization gate.

### Pending-verification UI

Change the Auth UI model facade to return the registration result rather than a Boolean. In `AuthViewModel.onRegister`, branch explicitly: an authorized result keeps the existing logged-in transition; a pending result clears sensitive form fields, closes the registration dialog, never calls `onUserLoggedIn`, and exposes a dismissible pending-email-verification confirmation state; `null` remains the existing registration error.

Add localized confirmation strings and render the same credential-free check-email confirmation across JS, JVM, and Android Auth views. The confirmation is separate from the registration dialog, so the registration form is closed before it appears and the user remains logged out.

### Documentation and verification

Update the Auth, Roles, Email, and UI/Auth feature READMEs to describe the serialized registration outcomes, the `User`-role authorization gate, required-email no-session behavior, and the confirmation UI. Add or update shared-model serialization, server service, client transport/storage, Roles bridge, Email verification integration, and UI view-model tests. Cover approved and denied login, refresh, bearer authentication, and token lookup; optional registration's immediate role assignment; required registration's pending result, stored password, absent credentials, denied pre-verification login, and successful post-verification login; HTTP result mapping; no credential persistence for pending; and UI closure/confirmation without a logged-in transition. Rebuild `ast-index` after Kotlin changes and run the narrow Auth, Roles, Email, and UI/Auth test tasks plus the relevant aggregate build task.

## Architecture handoff

ENTITY:
entity_id=user_role_authorization_and_pending_registration_contract; type=authentication_policy_change; state=planned

CONTEXT:

* task_id=18.08.2026_16.29.02-5f520804-25a3-4c54-9b26-a2350134bad9; agent_id=planning; memory_ref=[PROMPT.md,001-planning.md,features/auth/README.md,features/roles/README.md,features/email/README.md,features/users/README.md,features/ui/auth/README.md,AuthFeature.kt,AuthFeatureService.kt,AuthRoutingsConfigurator.kt,BearerAuthenticationConfigurator.kt,RolesBootstrap.kt,EmailVerificationAccountCoordinator.kt]
* constraints=[operator_selected_pending_email_verification_success,PendingEmailVerification_contains_no_AuthCredentials,PendingEmailVerification_does_not_persist_session,UserRole_required_for_login_refresh_bearer_and_getUser,Auth_to_Roles_dependency_inversion,registration_dialog_closed_before_confirmation]

ACTION:

1. action=add_model; target=auth_common_RegistrationResult; params={variants=[Authorized(AuthCredentials),PendingEmailVerification],serialization=kotlinx_polymorphic,register_return_type=RegistrationResult_nullable}
2. action=add_contract; target=auth_server_UserRoleAuthorization; params={operations=[ensureUserRole(RegisteredUser),hasUserRole(UserId)],missing_binding=fail_closed,implementation=roles_server_existing_transition_mutex}
3. action=modify; target=AuthFeatureService; params={credential_issuer=requires_hasUserRole,guarded_paths=[login,refresh,authenticate,getUser],optional_registration=ensure_UserRole_then_Authorized,required_email_registration=password_store_then_PendingEmailVerification_without_credentials}
4. action=modify; target=auth_client_and_ui_auth; params={storage=Authorized_only,PendingEmailVerification_storage=none,form=close_and_clear,interactor_onUserLoggedIn=Authorized_only,confirmation=multiplatform_dismissible_check_email}
5. action=verify; target=auth_roles_email_ui_auth_test_suites; params={coverage=[model_serialization,HTTP_mapping,role_gate,role_removal,optional_registration,required_pending_registration,post_verification_login,credential_storage,UI_confirmation],ast_index=rebuild_after_Kotlin_changes}

REASON:

* condition=required_email_registration_succeeds_before_UserRole_promotion; requirement=HTTP_200_pending_result_without_credentials_or_session_storage
* condition=account_lacks_UserRole; requirement=login_refresh_bearer_authentication_and_getUser_return_denied
* condition=generic_registration_creates_account_before_async_role_flow_callback; requirement=synchronous_role_assignment_precedes_credential_issuance
* condition=roles_server_depends_on_auth_server; requirement=Auth_owned_interface_prevents_cyclic_module_dependency

EXPECTED RESULT:

* entity_id=auth_registration_API; new_state=authorized_or_pending_serialized_success; location=features/auth/common_and_server
* entity_id=auth_session_policy; new_state=UserRole_gated; location=features/auth/server
* entity_id=required_email_UI; new_state=logged_out_check_email_confirmation; location=features/ui/auth

VERIFICATION:

* check=PendingEmailVerification_serialized_response; expected=HTTP_200_without_token_or_refreshToken_fields
* check=credential_storage_after_PendingEmailVerification; expected=unchanged_or_empty_storage
* check=pre_verification_authorization_attempts; expected=[login_null,refresh_null,bearer_rejected,getUser_null]
* check=post_verification_login; expected=AuthCredentials_issued_after_UserRole_promotion
* check=UI_pending_submission; expected=[registration_dialog_closed,confirmation_visible,logged_in_false]
* check=ast_index_rebuild_after_Kotlin_changes; expected=successful_index_refresh

UNCERTAINTY:

* missing=[]; ambiguity=none

REPETITION OF RESULT:

* entity_id=user_role_authorization_and_pending_registration_contract; stored_in=shared_step_file; status=available_for_architecture

COMMUNICATION:

* sender=planning; receiver=architecture; task_id=18.08.2026_16.29.02-5f520804-25a3-4c54-9b26-a2350134bad9; message_id=cdac7113-d68c-4cc9-9684-f06ca4639c87; protocol=AML-HIP

PERSISTENCE:

* local_memory=true; shared_memory=true; index_keys=[task_id,user_role_authorization_and_pending_registration_contract,RegistrationResult,UserRoleAuthorization]

VALIDATION:

* format_valid=true; no_pronouns=true; entities_explicit=true; high_density=true; causal_chain_present=true; ambiguity_detected=false
