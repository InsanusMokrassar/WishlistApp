Model: OpenAI GPT-5.6 Terra (ML)
Changed files: agents/task/18.08.2026_16.29.02-5f520804-25a3-4c54-9b26-a2350134bad9/001-planning.md

OpenAI GPT-5.6 Terra (ML) is appropriate because the task is a bounded change to an established cross-feature authentication lifecycle: the model can trace the Auth, Roles, Email, and UI contracts precisely while the implementation remains small after the public registration-result contract is decided.

## Task understanding

The server must never authorize an account that lacks the approved `User` role. Required-email self-registration currently creates a `NewUser` account, sends an invite, installs the password, and immediately issues access and refresh credentials before email verification promotes the account to `User`. That final credential issuance violates both requested outcomes.

The authorization check must cover credential issuance from password login and token refresh, and bearer-token authentication must reject a previously issued token if its account no longer has `User`. The role check must be owned by the Roles feature behind an Auth-owned narrow dependency-inversion interface; making Auth depend directly on Roles would create a module cycle because Roles already depends on Auth for bearer-route utilities and the existing registration lifecycle contract.

The required-email path must retain the successfully delivered invite, stored password, provisional user, and `NewUser` role, but must not store or return credentials. Verification remains the only transition that promotes the account to `User`; a later normal login can then issue credentials.

## Investigation

`AuthFeatureService.issueCredentialsFor` is reached by `login`, `refresh`, optional-email `register`, and required-email `register`. `BearerAuthenticationConfigurator` separately delegates every bearer request to `AuthFeatureService.authenticate`, so blocking only the three credential-issuing paths would leave already-issued sessions usable after role removal. `RolesBootstrap` and `EmailVerificationAccountCoordinator` already provide the authoritative direct-role transitions: required registration produces only `NewUser`, while verification replaces it with `User` under the existing transition lock.

The present register API cannot represent a successful pending account. `AuthFeature.register` and `POST /api/auth/register` use nullable `AuthCredentials` as both the success payload and the only success signal; `null` produces HTTP 400. The Ktor client and `AuthFeatureService` persist any returned credentials, and the Auth UI treats `true` only as an authenticated registration that closes the dialog and opens the logged-in state. Returning `null` after a successful invite would therefore falsely report failure to the user and make retry behavior unsafe.

All relevant feature README files were read in full: Auth, Roles, Email, Users, and UI/Auth. No operator notes add constraints.

## QUESTIONS FOR OPERATOR

After a required-email registration has successfully created the pending account, stored its password, and delivered the verification invite, what successful client-visible result should `POST /api/auth/register` return? The recommended contract is a serialized registration outcome that distinguishes `authorized` (credentials present) from `pendingEmailVerification` (no credentials), with the UI closing the registration dialog and showing a verification-email confirmation. This preserves a truthful HTTP success response without authorizing the pending account. Please confirm that contract and the desired confirmation UX, or specify an alternative success response such as HTTP 204.

## Architecture handoff

ENTITY:
entity_id=user_role_authorization_and_pending_registration_contract; type=authentication_policy_change; state=blocked_pending_operator_response

CONTEXT:

* task_id=18.08.2026_16.29.02-5f520804-25a3-4c54-9b26-a2350134bad9; agent_id=planning; memory_ref=[PROMPT.md,features/auth/README.md,features/roles/README.md,features/email/README.md,features/users/README.md,features/ui/auth/README.md,AuthFeatureService.kt,AuthRoutingsConfigurator.kt,BearerAuthenticationConfigurator.kt,RolesBootstrap.kt,EmailVerificationAccountCoordinator.kt]
* constraints=[deny_credentials_without_UserRole,deny_bearer_tokens_without_UserRole,preserve_pending_NewUserRole_until_email_verification,avoid_auth_to_roles_module_cycle,do_not_report_pending_registration_as_HTTP_400]

ACTION:

1. action=await_operator_decision; target=required_email_registration_response; params={recommended_contract=serialized_authorized_or_pending_outcome,alternative_contract=HTTP_204_without_body,ui_confirmation=required_for_pending_success}

REASON:

* condition=required_email_registration_succeeds_without_credential_issuance; requirement=HTTP_and_client_contract_distinguish_pending_success_from_registration_failure
* condition=AuthFeature_register_returns_nullable_AuthCredentials; requirement=null_currently_maps_to_HTTP_400_and_client_error_state

EXPECTED RESULT:

* entity_id=required_email_registration_response; new_state=operator_selected_contract; location=operator_response_then_next_planning_step

VERIFICATION:

* check=operator_response_received; expected=unambiguous_pending_registration_HTTP_payload_and_UI_behavior

UNCERTAINTY:

* missing=required_email_registration_success_response_and_confirmation_behavior; ambiguity=AuthCredentials_absence_currently_means_failure_not_pending_success

REPETITION OF RESULT:

* entity_id=user_role_authorization_and_pending_registration_contract; stored_in=shared_step_file; status=blocked_pending_operator_response

COMMUNICATION:

* sender=planning; receiver=orchestrator; task_id=18.08.2026_16.29.02-5f520804-25a3-4c54-9b26-a2350134bad9; message_id=fc21a5ea-6433-469b-bd5a-9b609a5bf5bb; protocol=AML-HIP

PERSISTENCE:

* local_memory=true; shared_memory=true; index_keys=[task_id,user_role_authorization_and_pending_registration_contract,operator_response]

VALIDATION:

* format_valid=true; no_pronouns=true; entities_explicit=true; high_density=true; causal_chain_present=true; ambiguity_detected=true
