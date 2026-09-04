Model: OpenAI Terra (ML)
Changed files: features/auth/README.md; features/auth/common/src/commonMain/kotlin/AuthFeature.kt; features/auth/common/src/commonMain/kotlin/models/RegistrationResult.kt; features/auth/common/src/commonTest/kotlin/models/RegisterRequestTest.kt; features/auth/common/src/commonTest/kotlin/models/RegistrationResultTest.kt; features/auth/client/src/commonMain/kotlin/AuthFeatureService.kt; features/auth/client/src/commonMain/kotlin/KtorAuthFeature.kt; features/auth/client/src/commonTest/kotlin/AuthFeatureServiceTest.kt; features/auth/server/src/commonMain/kotlin/Plugin.kt; features/auth/server/src/commonMain/kotlin/UserRoleAuthorization.kt; features/auth/server/src/commonMain/kotlin/services/AuthFeatureService.kt; features/auth/server/src/commonTest/kotlin/services/AuthFeatureServiceTest.kt; features/roles/README.md; features/roles/server/src/commonMain/kotlin/Plugin.kt; features/roles/server/src/commonMain/kotlin/RolesBootstrap.kt; features/roles/server/src/commonMain/kotlin/RolesUserRoleAuthorization.kt; features/roles/server/src/commonTest/kotlin/RolesBootstrapTest.kt; features/email/README.md; features/ui/auth/README.md; features/ui/auth/src/commonMain/kotlin/AuthStrings.kt; features/ui/auth/src/commonMain/kotlin/Plugin.kt; features/ui/auth/src/commonMain/kotlin/ui/AuthModel.kt; features/ui/auth/src/commonMain/kotlin/ui/AuthViewModel.kt; features/ui/auth/src/jsMain/kotlin/ui/AuthView.kt; features/ui/auth/src/jvmMain/kotlin/ui/AuthView.kt; features/ui/auth/src/androidMain/kotlin/ui/AuthView.kt; agents/task/18.08.2026_16.29.02-5f520804-25a3-4c54-9b26-a2350134bad9/004-coding.md

OpenAI Terra is the preferred ML choice for this Coding stage because the change crosses server authorization, serialized API contracts, storage semantics, and three UI targets while remaining an implementation task rather than an architectural redesign. The ML tier provides sufficient reasoning for the security-sensitive integration and test adaptation while matching the repository's stated Coding-role priority.

Implemented the typed registration-success contract. `RegistrationResult.Authorized` is the sole credential-bearing result, while `PendingEmailVerification` is credential-free. The shared Auth contract, Ktor transport, client storage service, server router, and UI model now preserve that distinction.

Auth owns the narrow `UserRoleAuthorization` port and fails closed when no implementation is bound. Roles provides and binds the direct-membership adapter. Optional registration synchronously ensures direct `UserRole` before hashing or issuing credentials. Login, refresh, bearer validation, and token-to-user lookup all re-check current direct membership. Required-email registration stores the password after invite delivery and returns the pending result without issuing credentials.

The Auth ViewModel closes and clears the form for either successful outcome. Only `Authorized` invokes the logged-in interactor. Pending verification shows a separate localized confirmation, implemented as a Calm Studio modal in JS and a platform dialog in JVM and Android. Opening another form or dismissing the confirmation clears the confirmation state without creating a session.

Added focused common serialization, client storage, server authorization, and Roles direct-membership tests. Updated all required feature READMEs without changing Operator Notes. The original required-email compensation flow now also avoids duplicate role cleanup after a failed pending transition and preserves duplicate registration as the existing `null` failure result.

Verification passed:

- `./gradlew :wishlist.features.auth.common:allTests :wishlist.features.auth.client:allTests :wishlist.features.auth.server:jvmTest :wishlist.features.roles.server:jvmTest :wishlist.features.email.server:jvmTest :wishlist.features.ui.auth:allTests --console=plain --quiet`
- `ast-index rebuild`

ENTITY:
entity_id=registration_result_and_direct_user_authorization; type=auth_security_and_registration_change; state=implemented

CONTEXT:
* task_id=18.08.2026_16.29.02-5f520804-25a3-4c54-9b26-a2350134bad9; agent_id=coding; constraints=[direct_UserRole_required,missing_port_denied,pending_result_credential_free]

ACTION:
1. action=add_contract; target=RegistrationResult; params={variants=[authorized,pendingEmailVerification],credential_owner=authorized_only}
2. action=add_port; target=UserRoleAuthorization; params={owner=auth_server,adapter=RolesUserRoleAuthorization,checks=[login,refresh,authenticate,getUser]}
3. action=modify_ui; target=AuthViewModel_and_platform_views; params={pending_confirmation=true,logged_in_transition=authorized_only}

EXPECTED RESULT:
* entity_id=required_email_registration; new_state=password_stored_pending_logged_out; location=auth_server_and_ui_auth
* entity_id=credential_authorization; new_state=current_direct_UserRole_required; location=auth_server_and_roles_server

VERIFICATION:
* check=targeted_gradle_test_matrix; expected=passed
* check=ast_index_rebuild; expected=completed

REPETITION OF RESULT:
* entity_id=registration_result_and_direct_user_authorization; stored_in=shared_step_file; status=available

VALIDATION:
* format_valid=true; no_pronouns=true; entities_explicit=true; high_density=true; causal_chain_present=true; ambiguity_detected=false
