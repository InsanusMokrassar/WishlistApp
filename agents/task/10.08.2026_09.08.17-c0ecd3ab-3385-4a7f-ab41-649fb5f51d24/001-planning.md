Model: gpt-5.6-sol
Changed files: agents/task/10.08.2026_09.08.17-c0ecd3ab-3385-4a7f-ab41-649fb5f51d24/001-planning.md

## Model rationale

The planning preference list names `fable`, `opus`, and `sonnet`, none of which is an available model in the current Codex session. The inherited active model is `gpt-5.6-sol`, the strongest available frontier agentic coding model, so the report records the actual model and uses it for the cross-module concurrency, compatibility, and data-integrity review.

## Scope and latest-version evidence

The checked-out PR head is `55c420b9319e304775fe14ea692a08f394ae220e`, and `origin/master` is `fe375f02e2442e75a14b95011ce7dbaf38da82ee`. GitHub reports the same head for open PR #74, with no review comments and a mergeable branch. The normal build check passed, while the Docker deploy check failed during Gradle configuration because the workflow rewrote the project version to `1.1.0-branch_issue-73-email-support-build72`, which is invalid for the configured Deb, RPM, EXE, and MSI packages.

I reviewed the complete base-to-head product diff, the issue #73 prompt and all ten prior stage reports, and the Auth, Email, DeepLinks, Roles, and UI/Auth feature READMEs. No Operator Notes constrain the remediation. The earlier internal validation fixed its reported role-promotion race and account/deeplink compensation gaps, but the review below found additional blocking behavior not covered by those reports.

## Review conclusion

PR #74 should not merge at the current head. `local.review.74.md` should recommend changes and contain the findings below, ordered by severity, with the cited source locations and concrete failure sequences. The fix cycle should remain bounded to registration orchestration, role lifecycle, compatibility, invite URL configuration, focused regression tests, documentation, and the failing CI workflow.

## High: required-email delivery holds the global auth lock across external I/O

`AuthFeatureService.register` enters `locker.withWriteLock` at line 127 and calls `RegistrationEmailSender.sendRegistrationEmail` at lines 132–137 without releasing the lock. The sender persists a deeplink and performs SMTP delivery at `EmailRegistrationInviteSender.kt:43-56`. The same lock protects login, refresh, logout, token authentication, password changes, and user purging. A slow or unavailable SMTP server therefore lets an unauthenticated registration request block every authentication operation for the duration of external I/O; repeated registration requests serialize behind the same lock and amplify the outage.

The fix should keep only repository state transitions under the auth lock. A required-email account can be created without a usable password/session, invite delivery can run after releasing the lock, and a second locked phase can either install the password and issue credentials after success or compensate the provisional user after failure. Cancellation must propagate rather than being converted into an ordinary delivery failure. Tests should suspend a fake sender and prove that an unrelated login or token operation completes while delivery is pending, while the provisional account remains unable to log in.

## High: failed registration cleanup can leave orphan role records

The roles plugin reacts asynchronously to every `UsersRepo.newObjectsFlow` event at `features/roles/server/src/jvmMain/kotlin/JVMPlugin.kt:56-58`. Required-email failure removes only the password and user at `AuthFeatureService.kt:138-141`. No corresponding role cleanup exists. If the callback runs before deletion, `NewUserRole` remains after the user disappears; if the callback runs after deletion, the callback can create the same orphan after compensation. Because role subjects are plain user-id strings without a user foreign key, repeated failed public registrations can grow unreachable role data indefinitely.

The role lifecycle fix must coordinate creation, rollback, and the asynchronous callback under the existing transition synchronization. The callback must not grant a role for an account that no longer exists, and rollback must remove every direct pending/default role even when callback execution already occurred. A deterministic test must cover both callback-before-delete and callback-after-delete orderings and assert that users, passwords, deeplinks, and roles are all absent after failed delivery.

## High: the global role callback strands non-registration users as pending

When `requireEmailForRegistration` is true, `grantDefaultRoles` assigns `NewUserRole` to every non-root user observed by `newObjectsFlow` at `RolesBootstrap.kt:40-48`. The admin creation path at `features/admin/server/src/commonMain/kotlin/UsersManagementFeature.kt:47-50` creates a user without an email and only sets a password; the path never calls the invite sender. Such an account can never receive a verification link and therefore has no path to the documented `NewUser` to `User` transition.

The pending-role decision must be tied to required-email self-registration rather than to every repository insertion. Generic and administrator-created accounts must retain the existing approved-user bootstrap behavior, while a self-registration-specific role hook marks the provisional account pending before invite delivery. The implementation must preserve the already-fixed delayed-callback invariant. Tests should create an administrator-managed user with the email requirement enabled and assert `UserRole` without `NewUserRole`, then exercise a self-registration and assert the inverse until verification.

## Medium: duplicate registration email escapes as a server error

Registration now passes the submitted email to `writeUsersRepo.create` at `AuthFeatureService.kt:129`. `ExposedUsersRepo.create` explicitly throws `DuplicateUserFieldException` for either unique username or unique email at `features/users/common/src/jvmMain/kotlin/repo/ExposedUsersRepo.kt:148-153`, but neither the auth service nor the registration route catches the exception. The route only maps a `null` result to `400` at `AuthRoutingsConfigurator.kt:31-38`. Registering with an email already owned by another account therefore escapes the documented registration-failure contract as an internal server error.

The auth boundary should translate `DuplicateUserFieldException` to the existing failed-registration response without exposing which unique field collided. A service test should use a repository double that throws on duplicate email, and a route-level test should assert the selected client status when a Ktor test harness is available.

## Medium: the claimed `AuthFeature` compatibility still breaks legacy implementors

`AuthFeature.register` changed from a two-parameter abstract method to a three-parameter abstract method at `features/auth/common/src/commonMain/kotlin/AuthFeature.kt:19`. A default value preserves old call sites but does not preserve classes that override the former signature. The added `LegacyAuthFeature` fixture at `RegisterRequestTest.kt:13-25` already implements the new three-parameter method, so the test cannot detect the break it claims to cover.

The shared interface should retain the old two-parameter contract and add an email-aware overload with a default bridge, or use another source-compatible extension shape. Current client/server implementations can explicitly bridge the old method to the new method. The compatibility fixture must compile while implementing only the pre-PR method set.

## Medium: a new client disables registration against an older server

`KtorAuthFeature.getConfig` returns `AuthConfig()` on every non-success response or exception at `features/auth/client/src/commonMain/kotlin/KtorAuthFeature.kt:62-66`. An older server legitimately returns `404` for the new `/auth/config` route while still exposing `/auth/is_registration_available`; the new UI consequently hides registration even when the old server reports registration enabled. Retaining the legacy route and a default interface method does not provide cross-version compatibility while this transport override fails closed to `enableRegistration=false`.

The transport fallback should call `isRegistrationAvailable()` when `/auth/config` is unavailable and return `AuthConfig(enableRegistration = legacyResult, requireEmailForRegistration = false)`. A MockEngine or equivalent client test should cover a `404` config response followed by a successful legacy boolean response.

## Medium: generated production invite URLs conflate bind and public origins

The sender derives HTTP versus HTTPS from the WebSocket-specific `wss` flag and always appends the internal server bind port at `features/email/server/src/commonMain/kotlin/Plugin.kt:79-86` and `EmailRegistrationInviteSender.kt:72-77`. The production documentation explicitly recommends terminating TLS at a reverse proxy, where the public origin commonly uses port 443 while Ktor binds plain HTTP on 8196. The current configuration cannot express that topology and generates links such as `https://example.com:8196/...` or `http://example.com:8196/...` instead of the externally reachable origin. The README does not document `wss` as the invite scheme switch.

The server configuration should expose one validated public HTTP origin, or separate public scheme/host/port fields, and the URL builder should resolve `/api/links/{id}` against that origin without blindly appending the bind port. Development should remain `http://127.0.0.1:8196`; production tests should cover a TLS origin on the default external port and a non-default external port. Configuration and feature documentation must describe the new contract.

## Blocking CI check

GitHub run `30301982966` fails before deployment because `.github/workflows/docker_deploy.yml:17-21` writes a branch-qualified Gradle version that Compose native packaging rejects. The same workflow runs on every branch push and, if configuration succeeds, invokes a script that pushes the shared Docker `latest` tag. The safe bounded fix is to restrict registry deployment to `master` and leave branch validation to the existing build workflow; alternatively, a branch job must never log in or push and must use packaging-compatible numeric versions. The fix must be verified by reproducing the Gradle configuration command locally with the branch version logic and by checking the workflow condition.

## QUESTIONS FOR OPERATOR

None. Repository contracts and observable failure paths determine the remediation without a product decision.

## Architecture and coding handoff

Architecture should define a two-phase required-registration transaction and a registration-specific role lifecycle hook so external delivery occurs outside the auth lock, provisional accounts cannot authenticate, failed accounts leave no user-owned state, and generic user creation remains approved. Architecture should also define the source-compatible auth overload and public-origin configuration shape. Coding should implement those contracts, write `local.review.74.md` with the findings above, add the focused concurrency/data-integrity/compatibility/configuration tests, rebuild `ast-index` after Kotlin changes, run focused module tests and the full Gradle build, validate JSON and Compose configuration, and verify that the branch no longer has a failing deploy check without publishing branch code as `latest`.

## AML-HIP

ENTITY:
entity_id=pr74_review_plan; type=planning_report; state=ready_for_architecture
entity_id=auth_lock_external_io; type=high_finding; state=unresolved
entity_id=failed_registration_role_orphan; type=high_finding; state=unresolved
entity_id=admin_user_pending_dead_end; type=high_finding; state=unresolved
entity_id=duplicate_email_server_error; type=medium_finding; state=unresolved
entity_id=auth_register_source_compatibility; type=medium_finding; state=unresolved
entity_id=legacy_server_config_fallback; type=medium_finding; state=unresolved
entity_id=public_invite_origin; type=medium_finding; state=unresolved
entity_id=docker_deploy_check; type=ci_finding; state=failing

CONTEXT:

* task_id=10.08.2026_09.08.17-c0ecd3ab-3385-4a7f-ab41-649fb5f51d24; agent_id=planning-001; memory_ref=[PROMPT.md,PR74,issue73_reports,feature_READMEs,GitHub_run_30301982966]
* constraints=[edit_only_001-planning.md,no_product_changes,no_push,review_head=55c420b9319e304775fe14ea692a08f394ae220e,review_base=fe375f02e2442e75a14b95011ce7dbaf38da82ee]

ACTION:

1. action=separate_external_io; target=auth_lock_external_io; params={phases=[reserve,deliver,finalize_or_rollback],login_before_success=false,cancellation=propagate}
2. action=coordinate_cleanup; target=failed_registration_role_orphan; params={states_removed=[user,password,deeplink,direct_roles],orderings_tested=[callback_before_delete,callback_after_delete]}
3. action=scope_pending_role; target=admin_user_pending_dead_end; params={pending_source=required_email_self_registration,generic_creation_role=UserRole,admin_creation_role=UserRole}
4. action=translate_exception; target=duplicate_email_server_error; params={exception=DuplicateUserFieldException,response=existing_registration_failure,field_disclosure=false}
5. action=restore_overload; target=auth_register_source_compatibility; params={legacy_signature=register_username_password,new_signature=email_aware_overload,test_fixture=pre_PR_method_set}
6. action=fallback; target=legacy_server_config_fallback; params={new_route_failure=legacy_boolean_probe,email_policy=false,test=client_transport}
7. action=model_public_origin; target=public_invite_origin; params={development=http://127.0.0.1:8196,production_proxy_origin=configurable,bind_port_reuse=false}
8. action=gate_deployment; target=docker_deploy_check; params={registry_push_branch=master,feature_branch_push=false,existing_build_workflow=retained}
9. action=write_review; target=local.review.74.md; params={recommendation=request_changes,findings=7,ci_blocker=1,line_anchors=true}

REASON:

* condition=unauthenticated_registration_holds_global_auth_lock_during_SMTP → action=two_phase_registration → result=auth_operations_remain_available; requirement=external_io_outside_global_lock
* condition=failed_user_deletion_uncoordinated_with_role_callback → action=synchronized_role_cleanup → result=no_orphan_role_subject; requirement=complete_compensation
* condition=global_required_email_policy_applies_to_admin_creation → action=registration_specific_pending_marker → result=admin_account_receives_UserRole; requirement=every_pending_account_has_verification_path
* condition=duplicate_email_throws_DuplicateUserFieldException → action=boundary_translation → result=controlled_registration_failure; requirement=no_expected_conflict_as_500
* condition=default_parameter_changes_abstract_override_signature → action=legacy_overload_preservation → result=legacy_implementor_compiles; requirement=claimed_source_compatibility
* condition=new_config_route_returns_404_on_old_server → action=legacy_probe_fallback → result=registration_visibility_preserved; requirement=cross_version_client_compatibility
* condition=reverse_proxy_public_origin_differs_from_bind_origin → action=explicit_public_origin → result=clickable_verification_link; requirement=production_delivery_correctness
* condition=branch_version_invalid_and_branch_deploy_pushes_latest → action=master_only_registry_deploy → result=green_safe_branch_checks; requirement=release_integrity

EXPECTED RESULT:

* entity_id=pr74_review_plan; new_state=implemented_and_verified; location=local.review.74.md+product_fix_commit
* entity_id=required_registration; new_state=nonblocking_atomic_compensated_flow; location=auth+roles+email_modules
* entity_id=compatibility_contracts; new_state=legacy_implementors_and_servers_supported; location=auth_common+auth_client
* entity_id=deployment_pipeline; new_state=master_only_registry_publish; location=.github/workflows/docker_deploy.yml

VERIFICATION:

* check=auth_concurrency; expected=login_completes_while_invite_sender_suspended
* check=failed_registration_state; expected={users=0,passwords=0,deeplinks=0,roles=0}
* check=admin_creation_with_required_email; expected={roles=[UserRole],invite_count=0}
* check=duplicate_email_registration; expected=controlled_client_error
* check=legacy_auth_implementation; expected=compiles_with_two_parameter_register_only
* check=old_server_client_config; expected={enableRegistration=legacy_probe_value,requireEmailForRegistration=false}
* check=proxy_invite_url; expected=public_origin_plus_api_deeplink_path
* check=branch_deployment; expected={docker_push=false,build_check=true}

UNCERTAINTY:

* missing=live_SMTP_and_reverse_proxy_environment; ambiguity=external_smoke_test_required_after_automated_tests

REPETITION OF RESULT:

* entity_id=pr74_review_plan; stored_in=shared_memory; status=ready_for_architecture
* entity_id=blocking_findings; stored_in=shared_memory; status=seven_product_findings_plus_one_CI_finding

COMMUNICATION:

* sender=planning-001; receiver=architecture-next; task_id=10.08.2026_09.08.17-c0ecd3ab-3385-4a7f-ab41-649fb5f51d24; message_id=5f7ff5ca-f275-4659-a2c0-bf51893d1e71; protocol=AML-HIP

PERSISTENCE:

* local_memory=true; shared_memory=true; index_keys=[task_id,entity_id,severity,fix,test,PR]

VALIDATION:

* format_valid=true
* no_pronouns=true
* entities_explicit=true
* high_density=true
* causal_chain_present=true
* ambiguity_detected=true
