Model: OpenAI GPT-5.6 Sol (HL)
Changed files: agents/task/18.08.2026_16.29.02-5f520804-25a3-4c54-9b26-a2350134bad9/003-architecturing.md

OpenAI GPT-5.6 Sol (HL) is appropriate because the change spans a security boundary, a serialized multiplatform contract, two independently synchronized account-state machines, and three UI renderers. The preferred architecture model is needed to keep the role check fail-closed without creating the existing Auth-to-Roles module cycle and to preserve the compensated required-email transaction while changing its success meaning.

## Architecture outcome

Required-email registration becomes a successful but unauthenticated operation. The server returns a typed `PendingEmailVerification` result after the password has been stored, without creating access or refresh tokens. Optional-email registration returns `Authorized` only after the new account has a direct `User` role and credentials have been issued. Every later authorization decision also checks the current direct `User` role, so role removal immediately makes an otherwise unexpired access or refresh token unusable.

The architecture introduces no Auth dependency on the Roles module. Auth owns a narrow server-side port, Roles implements and binds that port, and the implementation uses the role-transition mutex already shared by generic grant, pending marking, compensation, and verification promotion. Missing role infrastructure is a fail-closed condition.

All planned behavior can be covered automatically. Server and transport behavior has common/JVM tests, the shared UI transition has common tests, JS confirmation markup has a browser test, and JVM/Android confirmation composables have Compose UI tests. No external SMTP delivery, live database, pixel-perfect rendering, or other manually verified behavior is introduced by this task, so no operator test-policy decision is required before Coding.

## Shared registration contract

Create `features/auth/common/src/commonMain/kotlin/models/RegistrationResult.kt`. The declaration is a `@Serializable sealed interface RegistrationResult` with two nested variants: `@Serializable @SerialName("authorized") data class Authorized(val credentials: AuthCredentials)` and `@Serializable @SerialName("pendingEmailVerification") data object PendingEmailVerification`. Stable serial names prevent class renames from changing the protocol. Under the application `Json` configuration (`useArrayPolymorphism = true`), the HTTP bodies are `["authorized",{"credentials":{"token":"...","refreshToken":"..."}}]` and `["pendingEmailVerification",{}]`; the pending representation contains no credential-shaped field.

Change both `AuthFeature.register` overloads from `AuthCredentials?` to `RegistrationResult?`. The email-aware default overload continues delegating to the two-argument overload, preserving source compatibility for the method shape while intentionally changing the coordinated client/server return contract. `null` continues to mean refusal or failure; a non-null result is always an HTTP success.

`AuthRoutingsConfigurator` must name the value `result`, return HTTP 400 only for `null`, and serialize either result variant with HTTP 200. Login and refresh retain the existing `AuthCredentials?` and HTTP 401 contracts.

`KtorAuthFeature.register` must deserialize `RegistrationResult` for every successful response and return `null` for non-success status. `features/auth/client/AuthFeatureService.register` must persist `result.credentials` only for `RegistrationResult.Authorized`; it must return `PendingEmailVerification` unchanged without calling `AuthCredentialsStorage.save`. Login, refresh, logout, and `getMe` remain unchanged.

## Auth-owned role authorization boundary

Create `features/auth/server/src/commonMain/kotlin/UserRoleAuthorization.kt` with an Auth-owned interface containing `suspend fun ensureUserRole(user: RegisteredUser): Boolean` and `suspend fun hasUserRole(userId: UserId): Boolean`. `ensureUserRole` means that the generic role rule has been applied and a direct `User` role is present when the call returns. `hasUserRole` is a current direct-membership query, not a functionality-registry check and not an inherited-role check.

Create `features/roles/server/src/commonMain/kotlin/RolesUserRoleAuthorization.kt`. The class receives `RolesRepo`, implements the Auth-owned port, and delegates to new internal helpers in `RolesBootstrap.kt`. `ensureDefaultUserRole` acquires `roleTransitionMutex`, calls the existing `grantDefaultRolesWhileLocked`, and confirms `UserRole in rolesRepo.getDirectRoles(roleSubject(user.id))`. `hasDirectUserRole` acquires the same mutex and performs the same direct-role membership test for a `UserId`. The existing `grantDefaultRoles`, pending transition, role removal, and promotion functions retain their current behavior and lock ordering.

Register `single<UserRoleAuthorization> { RolesUserRoleAuthorization(get()) }` in `features/roles/server/Plugin.setupDI`. Resolve the port with `getOrNull<UserRoleAuthorization>()` in `features/auth/server/Plugin.setupDI` and pass the nullable dependency to `AuthFeatureService`. Resolution remains optional so server graphs without Roles still start, but all credential issuance and token authentication in such a graph fail closed.

The Roles-to-Auth dependency already exists, so placing the interface in Auth and the implementation in Roles adds no cycle. `features/auth/server` must not import `RolesRepo`, `UserRole`, or any Roles implementation type.

## Server authorization and registration sequencing

Add `private val userRoleAuthorization: UserRoleAuthorization? = null` to `AuthFeatureService`. Add a private `hasUserRole(userId)` helper that returns `false` for a missing port and otherwise delegates to the port. Change `issueCredentialsFor` to return `AuthCredentials?`; it checks `hasUserRole(id)` before mutating any token repository. This makes password login and refresh fail without issuing replacement credentials when direct `User` membership is absent.

`authenticate(token)` must retain expiry cleanup, then return the entry id only when `hasUserRole(entry.id)` succeeds. `getUser(token)` must retain token expiry and user projection behavior, but return `null` before the user lookup when the current direct role check fails. These checks are deliberately performed for every call rather than cached in token entries. Revoking `User` therefore rejects an already-issued bearer token and direct token-to-user lookup; restoring `User` may make a still-unexpired access token usable again, which follows the requested current-role policy and avoids silently deleting sessions during role administration.

The optional-email branch must require a non-null `UserRoleAuthorization` before creating an account. Inside the existing auth write lock, `registerWithoutRequiredEmail` keeps duplicate detection and user creation, calls `ensureUserRole(created)`, and proceeds to password hashing/storage only when the direct role is confirmed. It then calls the guarded credential issuer and wraps the non-null credentials in `RegistrationResult.Authorized`. A missing bridge creates no user; a false role confirmation or failed credential issue returns `null` and never returns an authenticated result. The existing user-creation and password-repository failure propagation remains unchanged.

The required-email branch keeps the current reservation, pending-role transition, invite delivery outside the auth lock, final user/email equality check, password storage, rollback, cancellation, and suppressed-cleanup-error behavior. The final locked phase returns `RegistrationResult.PendingEmailVerification` immediately after successful password storage and never calls `issueCredentialsFor`. Required registration does not require `UserRoleAuthorization` to create the pending account, because pending creation is intentionally unauthenticated; all later login and bearer paths still require the port and direct `User` membership.

Email verification remains the only pending-to-approved transition. `EmailVerificationAccountCoordinator.verifyInvitedEmailAndPromote` still validates the invited address and calls `promoteNewUserToUser` under the existing email-operation mutex. No Email production source change is required. A later ordinary login observes the promoted direct role through `UserRoleAuthorization` and can then issue credentials.

Constructor fixtures in `features/admin/server/src/commonTest/kotlin/UsersManagementFeatureTest.kt` and `features/email/server/src/commonTest/kotlin/services/RegistrationCompensationIntegrationTest.kt` must pass an appropriate fake or Roles implementation where authorization behavior is exercised. Admin password-management behavior does not require credential issuance and may use the default missing port.

## Pending-verification UI

Change `AuthModel.register` to return `RegistrationResult?`. The anonymous model in `features/ui/auth/Plugin.kt` returns the result from `ClientAuthFeature.register` directly instead of collapsing every non-null value to `true`.

Add `_pendingEmailVerificationState` and public read-only `pendingEmailVerificationState` to `AuthViewModel`, plus `onDismissPendingEmailVerification()`. `onRegister` handles three explicit branches. `Authorized` clears username, password, and email, closes the form, resets register mode, clears pending confirmation, and calls `interactor.onUserLoggedIn(node)`. `PendingEmailVerification` clears the same inputs, closes the form, resets register mode and error, sets the pending confirmation state, and does not call the interactor. `null` leaves the form open and sets the existing registration error. Dismissal only clears the confirmation state. Starting another login or registration form should also clear a stale confirmation before opening the form.

Add `pendingEmailVerificationTitle`, `pendingEmailVerificationMessage`, and `pendingEmailVerificationDismissButton` to `AuthStrings`, with English and Russian text. The message must tell the user to check the submitted email and verify the account before logging in; it must not imply that a session already exists.

Each platform `AuthView.onDraw` collects `pendingEmailVerificationState` and renders a confirmation separate from the credentials form. JS uses `CalmModal`, `ModalHeader`, `ModalBody`, and `ModalFooter`; JVM and Android use their existing `Dialog`/`Surface` conventions. Dismiss actions and outside dismissal call `onDismissPendingEmailVerification`. The form state is already false before confirmation state becomes true, so both dialogs cannot be visible simultaneously. Extract a small internal platform composable such as `PendingEmailVerificationDialog(visible, onDismiss)` where needed to make renderer tests independent of Koin and navigation setup.

## HTTP and dependency test support

Add a `ktor-server-test-host` version-catalog alias using the existing Ktor version and add the alias to `features/auth/server` test dependencies if the test host is not already available transitively. `AuthRoutingsConfiguratorTest` should install JSON content negotiation in `testApplication`, mount the configurator, and exercise the real `/auth/register` route. No production routing abstraction should be added solely for testing.

The Compose templates already provide `compose.uiTest` to JVM and Android tests, and the JS template already provides browser Mocha. UI renderer tests should use those existing target test facilities; no production-only testing hook is needed beyond an internal confirmation composable with explicit state and callback parameters.

## Test specifications

### Common Auth model and compatibility tests

Add `RegistrationResultTest`. `authorizedRoundTripsInApplicationFormat` encodes and decodes known tokens with `Json { useArrayPolymorphism = true }`, asserts the `authorized` serial name, and recovers identical credentials. `pendingRoundTripsWithoutCredentialFields` asserts the `pendingEmailVerification` serial name, round-trips the singleton, and verifies that the encoded body contains none of `credentials`, `token`, or `refreshToken`. Update `LegacyAuthFeature` and its compatibility test so both registration overloads use `RegistrationResult?` and the email-aware overload still delegates to the legacy method.

### Server Auth service tests

Extend `AuthFeatureServiceTest` with a configurable fake `UserRoleAuthorization` that records ensure/check calls and permits role changes during a test. `optionalRegistrationEnsuresUserRoleBeforeReturningAuthorized` supplies an allowed ensure result and expects `Authorized`, a stored password, and one ensure call for the created user. `optionalRegistrationWithoutRoleBridgeFailsBeforeCreatingUser` omits the port and expects `null`, no user, no password, and no token. `optionalRegistrationDeniedByRoleBridgeReturnsNoCredentials` returns false from ensure and verifies that no password or authenticated result is produced.

Rename the required-registration success test to `requiredEmailRegistrationReturnsPendingAfterInviteAndPasswordStorage`. The test expects `PendingEmailVerification`, one delivered invite, one pending-role transition, and a stored password. `requiredEmailRegistrationDoesNotInvokeCredentialAuthorization` uses a recording role authorization fake and expects zero `ensureUserRole` and `hasUserRole` calls during the required flow. Existing missing-email, invite failure, compensation, cancellation, duplicate, and lock-boundary tests retain their assertions but update the success result type.

Add authorization-path tests. `loginWithoutDirectUserRoleReturnsNull` uses a valid password and a denied role check. `loginWithDirectUserRoleReturnsCredentials` permits the role and expects credentials. `refreshAfterUserRoleRemovalReturnsNull` issues credentials while permitted, revokes the fake role, and expects the refresh token to be rejected without replacement credentials. `authenticateAfterUserRoleRemovalReturnsNull` and `getUserAfterUserRoleRemovalReturnsNull` issue a token, revoke the role, and verify both bearer and token-to-user paths fail. `missingRoleBridgeFailsClosedForLoginRefreshAuthenticateAndGetUser` verifies all four paths return `null`. Existing valid and expired `getUser` tests must use an allowed role fake so they continue testing expiry and projection rather than the new fail-closed default.

### Server routing tests

Add `AuthRoutingsConfiguratorTest` with a minimal fake `ServerAuthFeature`. `registerPendingReturnsHttp200WithCredentialFreeBody` returns the pending singleton and checks status 200 plus the decoded result and absence of credential field names. `registerAuthorizedReturnsHttp200WithCredentials` returns known credentials and checks the decoded authorized variant. `registerRefusalReturnsHttp400` returns `null` and checks the existing failure status. Existing login and refresh status behavior is unchanged and needs only a regression smoke assertion if the fixture mounts the full configurator.

### Client transport and storage tests

Extend `KtorAuthFeatureTest`. `registerDecodesAuthorizedResult` and `registerDecodesPendingEmailVerificationResult` use `MockEngine`, application JSON settings, and successful response bodies for both variants. `registerNonSuccessReturnsNull` returns HTTP 400. The mock must also assert the path and decode `RegisterRequest` so username, password, and optional email transport remain intact.

Add `features/auth/client/src/commonTest/kotlin/AuthFeatureServiceTest.kt` with fake transport and recording storage. `authorizedRegistrationPersistsCredentialsOnce` expects the wrapped credentials to be saved and the same result returned. `pendingRegistrationDoesNotWriteCredentials` starts with empty storage, returns pending, and expects zero saves plus an unauthorized storage state. `pendingRegistrationDoesNotOverwriteExistingStorage` starts with known credentials and expects unchanged storage and zero saves. `failedRegistrationDoesNotWriteStorage` returns `null` and expects zero saves.

### Roles bridge tests

Extend `RolesBootstrapTest` or add `RolesUserRoleAuthorizationTest`. `ensureUserRoleGrantsAndConfirmsDirectUserRole` starts with no roles and expects true plus exactly direct `UserRole`. `ensureUserRolePreservesPendingAndReturnsFalse` starts with direct `NewUserRole` and expects false with no direct `UserRole`. `hasUserRoleAcceptsDirectUserRoleOnly` covers direct User, NewUser-only, SuperAdmin-only, and no-role subjects. `ensureAndPendingTransitionShareSerializationBoundary` races ensure with `markPending` and asserts the final state is one of the two complete legal states, never simultaneous direct `UserRole` and `NewUserRole`. Add a Koin graph assertion that the Roles plugin binds one `UserRoleAuthorization` implementation.

### Email verification integration test

Add an Auth/Roles/Email success-flow integration test alongside `RegistrationCompensationIntegrationTest`. Configure real `AuthFeatureService`, `RolesRegistrationRoleLifecycle`, `RolesUserRoleAuthorization`, `EmailRegistrationInviteSender`, `EmailVerificationDeepLinkHandler`, and in-memory repositories. Registering with required email must return pending, store the password, leave exactly direct `NewUserRole`, and make login return `null`. Handling the minted link must leave exactly direct `UserRole`; a subsequent login with the stored password must return credentials. The existing SMTP-failure integration remains a compensated `null` result with empty user, password, deeplink, and role stores.

### UI model, ViewModel, and renderer tests

Add `AuthViewModelTest` with fake `AuthModel`, recording `AuthViewInteractor`, and the navigation test fixture used by the navigation library. `authorizedRegistrationClosesFormAndTransitionsToLoggedIn` expects cleared fields, closed form, hidden pending confirmation, and one interactor call. `pendingRegistrationClosesFormAndShowsConfirmationWithoutLoginTransition` expects cleared fields, closed registration form, visible confirmation, false logged-in state, and zero interactor calls. `failedRegistrationKeepsFormOpenAndShowsError` expects the current error behavior. `dismissPendingConfirmationHidesConfirmation` verifies the new dismissal method. `openingEitherFormClearsStalePendingConfirmation` verifies repeat-flow behavior.

Add a plugin-level model test or extract the anonymous facade into an internal factory so `AuthModel.register` is verified to preserve `Authorized`, `PendingEmailVerification`, and `null` rather than converting the result to a Boolean. Keep `AuthRegistrationValidationTest` unchanged except for any fixture imports.

Add target renderer tests around the extracted confirmation composable. The JS browser test renders visible state, asserts translated title/message/dismiss action in the DOM, clicks dismiss, and observes one callback. JVM and Android Compose UI tests assert the same visible text and callback behavior, then render `visible = false` and assert the confirmation is absent. These tests cover the new branches in each modified `onDraw`; platform compile tasks cover imports and integration with the full `AuthView`.

## README updates

The Coding stage must update only the agent-maintained sections and must not modify any `## Operator Notes` section.

`features/auth/README.md` must change the register route response to `RegistrationResult | 400`, document both serialized outcomes, describe `UserRoleAuthorization`, state that all credential issuance plus bearer and token lookup require current direct `User`, and explain that required-email success stores the password but emits no credentials. The previous statement that required registration installs and returns credentials after invite delivery must be replaced.

`features/roles/README.md` must document `RolesUserRoleAuthorization`, its Auth-owned interface, direct membership semantics, fail-closed consumer behavior, and reuse of `roleTransitionMutex` for synchronous optional-registration assignment and current authorization checks.

`features/email/README.md` must state that successful required-email registration remains logged out, verification only promotes `NewUser` to `User`, and normal login after promotion is the first credential-producing step. Existing invite, equality, mutex, and compensation descriptions remain valid.

`features/ui/auth/README.md` must change `AuthModel.register` from Boolean to `RegistrationResult?`, add `pendingEmailVerificationState` and dismissal behavior to the ViewModel model list, and describe the separate localized check-email confirmation on JS/JVM/Android. `features/users/README.md` requires no delta because identity models and repository ownership do not change.

## Expected implementation footprint

Production changes are confined to Auth common/server/client, Roles server, and UI/Auth plus the four feature READMEs described above. Email production code and Users production code remain unchanged. Tests span Auth common/server/client, Roles server, Email server integration, and UI/Auth target source sets. The route test may require the version-catalog and Auth server test-dependency additions described above.

After Kotlin changes, Coding must run `ast-index rebuild`. Verification should run `:wishlist.features.auth.common:allTests`, `:wishlist.features.auth.client:allTests`, `:wishlist.features.auth.server:jvmTest`, `:wishlist.features.roles.server:jvmTest`, `:wishlist.features.email.server:jvmTest`, and `:wishlist.features.ui.auth:allTests`, followed by the relevant aggregate `build`. If Android test execution is unavailable locally, the Android test source must still compile in the aggregate build; JVM and JS renderer tests remain executable locally.

## Architecture handoff

ENTITY:
entity_id=user_role_authorization_and_pending_registration_architecture; type=security_and_registration_architecture; state=ready_for_coding

CONTEXT:

* task_id=18.08.2026_16.29.02-5f520804-25a3-4c54-9b26-a2350134bad9; agent_id=architecture; memory_ref=[PROMPT.md,001-planning.md,002-planning.md,features/auth/README.md,features/roles/README.md,features/email/README.md,features/users/README.md,features/ui/auth/README.md]
* constraints=[direct_UserRole_required,Auth_to_Roles_dependency_cycle_forbidden,required_email_result_credential_free,required_email_password_persisted,registration_form_closed_before_confirmation,all_planned_behavior_automated]

ACTION:

1. action=add_contract; target=RegistrationResult; params={variants=[authorized,pendingEmailVerification],wire_format=array_polymorphism,credential_owner=authorized_only,null_semantics=registration_failure}
2. action=add_port; target=UserRoleAuthorization; params={owner=auth_server,implementation=roles_server,operations=[ensureUserRole,hasUserRole],membership=direct_UserRole,missing_binding=deny}
3. action=modify_service; target=AuthFeatureService; params={guarded_paths=[login,refresh,authenticate,getUser,issueCredentialsFor],optional_registration=ensure_then_authorized,required_registration=password_then_pending_without_tokens}
4. action=modify_client; target=auth_client_and_ui_auth; params={transport=RegistrationResult,storage=authorized_only,pending_ui=separate_dismissible_confirmation,logged_in_transition=authorized_only}
5. action=verify; target=automated_test_matrix; params={suites=[auth_common,auth_client,auth_server,roles_server,email_server,ui_auth_js,ui_auth_jvm,ui_auth_android],index_action=ast-index_rebuild}

REASON:

* condition=account_without_direct_UserRole; requirement=credential_issuance_and_bearer_resolution_denied_at_current_role_state
* condition=required_email_account_holds_NewUserRole; requirement=successful_registration_response_without_credentials_or_session
* condition=roles_server_already_depends_on_auth_server; requirement=Auth_owned_port_with_Roles_owned_implementation

EXPECTED RESULT:

* entity_id=auth_registration_contract; new_state=typed_authorized_or_pending_success; location=features/auth/common_and_transports
* entity_id=auth_authorization_policy; new_state=current_direct_UserRole_gate; location=features/auth/server_with_roles_server_adapter
* entity_id=pending_verification_presentation; new_state=logged_out_localized_confirmation; location=features/ui/auth_all_platforms

VERIFICATION:

* check=required_registration_success; expected=[PendingEmailVerification,password_stored,no_token,no_refresh_token,direct_NewUserRole]
* check=authorization_without_direct_UserRole; expected=[login_null,refresh_null,authenticate_null,getUser_null]
* check=verification_then_login; expected=[direct_UserRole,AuthCredentials]
* check=pending_UI_transition; expected=[form_closed,fields_cleared,confirmation_visible,interactor_calls_zero]

UNCERTAINTY:

* missing=[]; ambiguity=none

REPETITION OF RESULT:

* entity_id=user_role_authorization_and_pending_registration_architecture; stored_in=shared_step_file; status=available_for_coding

COMMUNICATION:

* sender=architecture; receiver=coding; task_id=18.08.2026_16.29.02-5f520804-25a3-4c54-9b26-a2350134bad9; message_id=6b5fb5e1-c245-4863-99c4-63a600bf71f7; protocol=AML-HIP

PERSISTENCE:

* local_memory=true; shared_memory=true; index_keys=[task_id,RegistrationResult,UserRoleAuthorization,pending_email_verification,authorization_gate]

VALIDATION:

* format_valid=true; no_pronouns=true; entities_explicit=true; high_density=true; causal_chain_present=true; ambiguity_detected=false
