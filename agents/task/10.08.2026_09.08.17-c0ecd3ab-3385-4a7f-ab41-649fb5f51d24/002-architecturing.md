Model: gpt-5.6-sol
Changed files: agents/task/10.08.2026_09.08.17-c0ecd3ab-3385-4a7f-ab41-649fb5f51d24/002-architecturing.md

## Model rationale

The architecture preference list names `fable`, `opus`, and `sonnet`, but none is available in the current Codex session. The inherited `gpt-5.6-sol` model is the strongest available frontier agentic coding model and is appropriate for the required concurrency, compatibility, authorization, and compensation design. The report records the model actually used.

## Evidence and scope

The fetched remote branch still points to PR head `55c420b9319e304775fe14ea692a08f394ae220e`, based on `origin/master` at `fe375f02e2442e75a14b95011ce7dbaf38da82ee`. I read the complete planning step, the base-to-head product diff, and the Auth, Common, DeepLinks, Email, Roles, UI/Auth, Users, and Admin feature READMEs before tracing the source. No Operator Note blocks the fixes.

The coding scope is the eight accepted product findings below, the local PR review, focused documentation updates, and the master-only deploy correction. It does not include an application-wide `UserRole` gate, a verification-link resend flow, link consumption, a verification timestamp or database schema, password reset, or re-verification after an already approved user edits an email address. Those changes are separate product decisions and are not needed to correct the reviewed regressions.

## Verification-link exploit determination

The omitted candidate is exploitable and belongs in `local.review.74.md` as a High-severity finding. Required-email registration returns access and refresh credentials immediately after SMTP delivery, before the deeplink is opened. Bearer authentication accepts those credentials without checking `NewUserRole`. `PUT /email/myEmail` requires only that bearer identity, and both enabled and disabled email services update the caller's user row without a role check. The stored deeplink payload contains only `userId`, while the handler checks only that the user still exists before promotion.

A registrant can therefore submit address A, receive credentials, replace the stored value with unverified address B, and then open the link delivered to A. The handler promotes the account while B is stored. The bounded correction is to bind each new payload to the invited `Email` and reject promotion unless the current user row still contains the exact same address. Existing user-id-only payloads must decode safely but fail closed. This preserves the documented post-approval self-service email-edit route; requiring re-verification for later edits would be a broader policy change.

## Architecture

### 1. Required registration becomes a compensated two-phase operation

`AuthFeatureService.register(username, password, email)` must keep the global auth write lock only around repository and in-memory auth transitions. With required email enabled, preconditions first require a non-null email, a `RegistrationEmailSender`, and a registration-role lifecycle hook. The reservation phase acquires the lock, rejects an existing username, translates `DuplicateUserFieldException` to `null`, creates the provisional user, and marks the exact user id pending. It must not store the password or issue either token during this phase.

After releasing the lock, BCrypt hashing and invite delivery run without the global auth lock. A provisional account has no password and cannot log in while the sender is suspended, while unrelated login, authentication, refresh, and logout calls remain available. A successful delivery enters a second locked phase, verifies that the same provisional user id and submitted email still exist, stores the password hash, and issues credentials. The role remains `NewUser` until the bound deeplink is handled.

False delivery, an ordinary sender exception, a failed pending-role transition, or a missing provisional user triggers compensation. Compensation removes the provisional user before synchronously removing its direct roles, defensively removes any password/auth state, and returns `null`. Deleting the user before the synchronous role cleanup makes a delayed creation callback observe absence; the deletion-flow cleanup supplies a second idempotent guard. `CancellationException` must run compensation in `NonCancellable` context and then propagate, never become an ordinary registration failure. `EmailRegistrationInviteSender` applies the same rule to a link minted before cancellation: remove the link in `NonCancellable` context, then rethrow cancellation.

Optional-email registration has no external delivery phase and retains the existing locked create/password/token behavior, with the same duplicate-field translation. This avoids changing the established optional-registration behavior.

The auth server owns a small dependency-inversion contract named `RegistrationRoleLifecycle`, with `markPending(userId)` and `removeRoles(userId)` operations. `AuthFeatureService` depends only on this contract. The roles server provides and registers the implementation; this direction avoids an auth-to-roles module cycle and keeps role mutations in the Roles feature.

### 2. Role assignment is generic by default and explicitly pending for self-registration

`roles/server/RolesBootstrap.kt` keeps one process-local transition mutex, but the transition rules change. Generic user creation grants `UserRole` to a non-root user unless `NewUserRole` is already present; root receives `UserRole` and `SuperAdminRole`. The callback no longer reads the global auth email flag. Required self-registration alone calls `markPending`, which removes `UserRole` and adds `NewUserRole` under the same mutex. Verification removes `NewUserRole` and adds `UserRole` under that mutex.

These rules converge for either callback order. A generic callback that runs first may add `UserRole`, after which `markPending` replaces it. A callback that runs after `markPending` observes `NewUserRole` and preserves the pending state. A callback delayed until after verification observes the approved state and idempotently retains `UserRole`.

The live creation callback must check `UsersRepo.getById(user.id)` while holding the transition mutex before granting anything. `JVMPlugin` must also subscribe to `deletedObjectsIdsFlow` before the backfill and remove every direct role for the deleted subject under the same mutex. The combination covers both failure orders: creation callback before deletion is followed by deletion cleanup; creation callback after deletion sees no user and grants nothing. The synchronous lifecycle cleanup keeps the registration call's own compensation deterministic.

Backfill remains approved-user migration behavior. It grants existing users `UserRole`, preserves an already explicit `NewUserRole`, and grants root privileges. No auth configuration argument remains on generic grant or backfill functions. Administrator-created users consequently receive `UserRole` even when required-email self-registration is enabled.

### 3. Duplicate registration fields use the existing failure contract

The narrow `writeUsersRepo.create` call in `AuthFeatureService.register` catches `DuplicateUserFieldException` and returns `null`. The existing route already maps `null` to `400 Bad Request`, so no field name is disclosed and no new HTTP response shape is needed. Other repository failures still propagate; the catch must not become a broad exception swallow.

### 4. `AuthFeature` retains source compatibility

`AuthFeature` restores the pre-PR abstract two-argument `register(username, password)` method. A distinct three-argument overload, without a default parameter, has a default implementation that delegates to the legacy method. An implementation compiled from source against the old method set therefore still satisfies the interface. Current client wrapper, Ktor transport, and server service implement both methods; each two-argument implementation delegates to its three-argument implementation with `email = null`.

The default `getConfig()` bridge remains based on `isRegistrationAvailable()` and `requireEmailForRegistration = false`. A legacy implementation can therefore compile and behave as an optional-email server without implementing either new method.

### 5. New clients fall back to the legacy availability route

`KtorAuthFeature.getConfig()` first requests `/auth/config`. A non-success status, transport failure, or decode failure falls back to `isRegistrationAvailable()` and returns `AuthConfig(enableRegistration = legacyResult, requireEmailForRegistration = false)`. A successful config response is returned unchanged and must not make the legacy request. The existing `isRegistrationAvailable()` failure behavior remains fail-closed, so failure of both probes yields the default disabled configuration.

### 6. Verification payloads bind promotion to the invited address

`EmailVerificationPayload` adds `email: Email? = null`. New links always store the user's non-null registration email. The nullable default lets already persisted user-id-only payloads deserialize, but the handler rejects a null payload email, a missing user, or a current user email different from the invited value without changing roles. A matching user is promoted through the existing synchronized transition.

This is deliberately a handler-time equality check rather than an email subsystem redesign. It closes the reviewed sequential attack and causes a stale link to remain unhandled after a pending user changes the address. It does not change the documented ability of an already approved account to edit its own email later.

### 7. Invite URLs use one validated public HTTP origin

The common server `Config` adds `publicHttpOrigin: String`, defaulting to `http://{publicHost}:{port}` for configuration compatibility and local development. Production configuration sets the complete externally reachable origin, such as `https://wishlist.example` or `https://wishlist.example:9443`. The WebSocket `wss` flag no longer controls HTTP invite links, and the Ktor bind port is not appended to an explicit public origin.

`EmailRegistrationInviteSender` accepts only `publicHttpOrigin`, validates at construction that it is an absolute `http` or `https` origin with a host and without credentials, query, fragment, or non-root path, and resolves `/api/links/{id}` beneath it. Invalid configuration fails during startup rather than sending unusable links. Development config explicitly uses `http://127.0.0.1:8196`; the sample config shows an HTTPS reverse-proxy origin without the internal bind port.

### 8. Branch deploys cannot publish `latest`

`.github/workflows/docker_deploy.yml` changes its trigger from every push to pushes on `master` only. The branch-qualified version rewrite is removed; the Gradle memory configuration may remain under a clearly named setup step. `.github/workflows/build.yml` remains push-triggered for every branch, so PR branches still compile but never log in to the registry or execute the publish script. No packaging-version redesign or branch image tag is introduced.

## Concrete implementation map

Auth changes belong in `features/auth/common/src/commonMain/kotlin/AuthFeature.kt`, the two current client implementations, `features/auth/server/src/commonMain/kotlin/RegistrationRoleLifecycle.kt`, `Plugin.kt`, and `services/AuthFeatureService.kt`. Auth tests belong in the existing common/server tests plus a new client transport test; `features/auth/client/build.gradle` and `gradle/libs.versions.toml` may add Ktor MockEngine as a test-only dependency.

Role changes belong in `features/roles/server/src/commonMain/kotlin/RolesBootstrap.kt`, a small roles-owned `RegistrationRoleLifecycle` implementation, `Plugin.kt`, and `jvmMain/JVMPlugin.kt`, with focused additions to `RolesBootstrapTest.kt`. No Roles repository or schema change is required.

Email changes belong in `EmailVerificationPayload.kt`, `EmailRegistrationInviteSender.kt`, `EmailVerificationDeepLinkHandler.kt`, and their existing tests. The sender wiring in `features/email/server/src/commonMain/kotlin/Plugin.kt` passes the common public origin and the synchronized role transition dependency. The common server config model and the two checked-in server JSON configs carry the new origin. No DeepLinks core change beyond the already introduced removal API is required.

The coding agent also writes `local.review.74.md`, updates the workflow, and applies the README deltas below. Product code outside these named seams should remain untouched unless compilation proves a direct overload or constructor call site requires adaptation.

## Test specifications

### Auth service and compatibility tests

Extend `AuthFeatureServiceTest` with a sender controlled by `CompletableDeferred`. Start required registration and wait until delivery is suspended. While suspension is active, an unrelated seeded user's valid login must complete within a timeout, and login for the provisional username/password must complete with `null`. After delivery returns true, registration must return credentials, store one password, retain the user, and record one pending-role transition.

Exercise required delivery returning false and throwing an ordinary exception. Both inputs must return `null`; users, passwords, access/refresh credentials, and direct roles for the provisional id must be absent. A same-username retry after false delivery must be able to succeed. Exercise a sender cancellation after delivery starts: the registration job must finish with `CancellationException`, while user/password/role state is compensated rather than reported as a normal `null` result.

Use a write-repository double that throws `DuplicateUserFieldException` from `create` for a distinct username and duplicate email. Registration must return `null`, must not call the sender or lifecycle, and must create no password or token. Keep the existing route assertion that `null` maps to `400`; no new status branch is introduced. Also test that required mode with a missing sender or missing role lifecycle fails before creating a user.

Replace the misleading compatibility fixture in `RegisterRequestTest` with a class that implements only the pre-PR two-argument register method plus the other old abstract methods. The fixture must compile, `getConfig()` must return the legacy availability with a false email requirement, and a call to the new three-argument overload must delegate to the legacy method. Current Ktor, client-service, and server implementations must each have a two-argument delegation test or be exercised through the shared interface.

### Client fallback tests

Add a MockEngine-backed `KtorAuthFeatureTest`. For a `404` `/auth/config` response followed by `true` from `/auth/is_registration_available`, `getConfig()` must return `AuthConfig(true, false)` and the recorded request order must be config then legacy. Repeat with a config transport/decode failure and a false legacy response; the result must be disabled optional-email config. For a successful `AuthConfig(true, true)` response, return both flags and assert that no legacy request occurs. If both probes fail, assert `AuthConfig()`.

### Role lifecycle and cleanup tests

Extend `RolesBootstrapTest` with an administrator-style generic user creation while required-email registration exists elsewhere in the application. The generic callback must yield exactly `UserRole`, never `NewUserRole`. Test both pending orderings explicitly: generic grant followed by `markPending`, and `markPending` followed by generic grant; both must yield exactly `NewUserRole`. Promotion followed by a delayed generic callback must yield exactly `UserRole`.

Test orphan prevention in both deterministic orders. In callback-before-delete order, grant defaults, remove the user, run deletion cleanup, and assert no direct roles. In delete-before-callback order, remove the user first, invoke the existence-checking callback, and assert no direct roles were created. A deletion-flow integration test using the in-memory users repository must also prove that deleting a generic or pending user eventually removes every direct role.

The existing backfill test must run without an auth-policy parameter and keep pre-existing non-root users approved. Root tests must retain `UserRole` plus `SuperAdminRole`; role removal must be idempotent for an unknown or already-clean id.

### Email verification and compensation tests

Update sender tests to assert that the stored `EmailVerificationPayload` contains both the exact user id and recipient email. Add cancellation coverage with a suspending emails service: cancel after link creation, assert cancellation propagates, and assert the deeplink repository is empty after non-cancellable cleanup. Existing false/ordinary-exception cleanup tests remain.

Update handler tests with a pending user whose stored email equals the payload email; the handler must return true and leave exactly `UserRole`. For a different current email, a cleared email, a missing payload email decoded from the old shape, and a missing user, the handler must return false and leave `NewUserRole` unchanged. Repeated opens of a matching link remain idempotent. The polymorphic serialization test must round-trip the new email and must decode the prior user-id-only JSON to `email = null` without promoting it.

Add or extend the cross-feature registration integration test using real auth orchestration, the roles lifecycle, in-memory user/password/roles/deeplink repositories, and an SMTP false result. After registration returns, all four stores must be empty. Run the same assertion when the role creation callback is forced before deletion and after deletion.

### Public-origin tests

Replace the URL-builder tests with origin inputs. `http://127.0.0.1:8196` must produce the development URL; `https://wishlist.example` must not gain `:8196` or `:443`; `https://wishlist.example:9443` must preserve the external port. A trailing slash must normalize to one separator. Constructor or validator tests must reject blank origins, `ftp`, missing hosts, credentials, queries, fragments, and non-root paths.

Add a common server config serialization test proving that omission of `publicHttpOrigin` derives the compatibility development value from `publicHost` and `port`, while an explicit HTTPS origin survives decoding unchanged. Validate both checked-in server JSON configs after editing.

### Workflow and build tests

Run a deterministic YAML/action assertion that the Docker workflow resolves to a push trigger whose only branch is `master`, contains registry login and deploy only inside that job, and contains no branch-qualified version rewrite. Assert separately that the Build workflow remains enabled for every push. Run `actionlint` when available, reproduce Gradle configuration without the invalid branch suffix, then execute focused Auth, Roles, Email, and Common tests followed by `./gradlew build`. Rebuild `ast-index` after Kotlin changes and run `git diff --check`.

All planned behavior has an automated unit, integration, transport, serialization, configuration, or static-workflow check. Live SMTP delivery and an actual reverse proxy remain deployment smoke tests, but neither is required to establish the corrected contracts because SMTP and public-origin inputs are stubbed deterministically.

## README updates

The Auth Architecture Notes must describe the two-phase provisional account, deferred password/token installation, cancellation propagation, duplicate-field failure mapping, role-lifecycle hook, restored overload compatibility, and legacy config fallback. The route table remains `400` for registration conflicts.

The Roles Architecture Notes must replace the global email-policy callback description with generic approved assignment, explicit registration pending transitions, existence-checked creation handling, and deletion-flow role cleanup. The notes must state the two callback-order invariants.

The Email Models and Architecture Notes must document the email-bound payload, mismatch rejection, cancellation cleanup, and `publicHttpOrigin` URL construction. The Common config documentation and root README configuration table must define `publicHttpOrigin`, its compatibility default, and reverse-proxy examples. No Operator Notes section may change.

## Review handoff

`local.review.74.md` should request changes and retain the seven findings from planning, add the High-severity verification-link/address-binding finding proved above, and list the Docker workflow as a blocking CI/release-safety issue. The review should use PR-head line anchors, concrete failure sequences, and the bounded remediations in this report.

## AML-HIP handoff

ENTITY:
entity_id=pr74_architecture; type=architecture_report; state=ready_for_coding
entity_id=required_registration_flow; type=concurrency_contract; state=specified
entity_id=registration_role_lifecycle; type=role_contract; state=specified
entity_id=verification_address_binding; type=security_contract; state=specified
entity_id=compatibility_contracts; type=client_api_contract; state=specified
entity_id=public_http_origin; type=configuration_contract; state=specified
entity_id=master_only_deploy; type=workflow_contract; state=specified

CONTEXT:

* task_id=10.08.2026_09.08.17-c0ecd3ab-3385-4a7f-ab41-649fb5f51d24; agent_id=architecturing-002; memory_ref=[001-planning.md,PR74_head_55c420b,feature_READMEs,product_diff]
* constraints=[edit_only_002-architecturing.md,no_product_changes,no_push,minimal_scope,eight_product_fixes,one_CI_fix]

ACTION:

1. action=implement_two_phase_registration; target=required_registration_flow; params={locked_phases=[reserve,finalize,compensate],external_phase=deliver,password_before_delivery=false,cancellation=propagate_after_non_cancellable_cleanup}
2. action=implement_registration_specific_roles; target=registration_role_lifecycle; params={generic_nonroot=UserRole,required_self_registration=NewUserRole,deleted_user_roles=remove_all_direct,callback_user_check=true}
3. action=bind_invited_email; target=verification_address_binding; params={payload=[userId,email],legacy_email=null,mismatch=unhandled,promotion=matching_current_email_only,severity=high}
4. action=restore_compatibility; target=compatibility_contracts; params={legacy_register=abstract_two_argument,new_register=default_three_argument,config_fallback=legacy_availability,email_policy_default=false}
5. action=configure_public_origin; target=public_http_origin; params={field=publicHttpOrigin,schemes=[http,https],bind_port_append=false,invalid_origin=startup_failure}
6. action=gate_registry_deploy; target=master_only_deploy; params={docker_push_branches=[master],build_branches=all_pushes,branch_version_suffix=removed}
7. action=write_review; target=local.review.74.md; params={recommendation=request_changes,product_findings=8,CI_blockers=1,address_binding_severity=high}

REASON:

* condition=SMTP_delivery_inside_global_auth_lock → action=reserve_deliver_finalize → result=unrelated_auth_progress; requirement=provisional_login_denied
* condition=global_policy_callback_plus_async_delete → action=explicit_pending_hook_plus_creation_existence_check_plus_deletion_cleanup → result=no_admin_dead_end_and_no_orphan_roles; requirement=callback_order_independence
* condition=userId_only_link_plus_credentialed_NewUser_email_update → action=payload_email_equality_check → result=stale_link_rejected; requirement=promotion_bound_to_invited_address
* condition=abstract_signature_replacement_plus_missing_old_server_route → action=legacy_overload_plus_transport_fallback → result=source_and_cross_version_compatibility; requirement=optional_email_default
* condition=bind_origin_differs_from_public_origin → action=validated_publicHttpOrigin → result=externally_reachable_invite_URL; requirement=reverse_proxy_support
* condition=branch_deploy_pushes_shared_latest → action=master_only_trigger → result=feature_branch_registry_safety; requirement=branch_build_retained

EXPECTED RESULT:

* entity_id=required_registration_flow; new_state=nonblocking_compensated_pending_account; location=auth_server_plus_roles_server_plus_email_server
* entity_id=verification_address_binding; new_state=invited_email_required_for_promotion; location=email_verification_payload_and_handler
* entity_id=compatibility_contracts; new_state=legacy_implementor_and_server_supported; location=auth_common_and_auth_client
* entity_id=master_only_deploy; new_state=registry_publish_restricted; location=.github/workflows/docker_deploy.yml

VERIFICATION:

* check=registration_sender_suspended; expected={unrelated_login=complete,provisional_login=null,password_absent=true}
* check=failed_registration_all_orders; expected={users=0,passwords=0,deeplinks=0,direct_roles=0,cancellation=propagated}
* check=generic_admin_creation; expected={roles=[UserRole],NewUserRole=false}
* check=stale_email_link; expected={handled=false,roles=[NewUserRole]}
* check=legacy_auth_implementation; expected={two_argument_only_compiles=true,default_config=[enabled_legacy,false]}
* check=old_server_config_route; expected={fallback_route=is_registration_available,requireEmailForRegistration=false}
* check=proxy_invite_origin; expected={default_TLS_port_omitted=true,custom_external_port_preserved=true,bind_port_leak=false}
* check=workflow_scope; expected={docker_deploy_branches=[master],build_branches=all_pushes,branch_registry_login=false}

UNCERTAINTY:

* missing=none; ambiguity=none

REPETITION OF RESULT:

* entity_id=pr74_architecture; stored_in=shared_memory; status=ready_for_coding
* entity_id=pr74_fix_scope; stored_in=shared_memory; status=eight_product_fixes_plus_one_CI_fix

COMMUNICATION:

* sender=architecturing-002; receiver=coding-next; task_id=10.08.2026_09.08.17-c0ecd3ab-3385-4a7f-ab41-649fb5f51d24; message_id=dc24784c-020b-49c1-9494-acde571f75ae; protocol=AML-HIP

PERSISTENCE:

* local_memory=true; shared_memory=true; index_keys=[task_id,entity_id,architecture,test_contract,review_finding]

VALIDATION:

* format_valid=true; no_pronouns=true; entities_explicit=true; high_density=true; causal_chain_present=true; ambiguity_detected=false
