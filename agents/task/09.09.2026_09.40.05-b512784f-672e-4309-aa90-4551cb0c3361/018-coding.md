Model: GPT-5.6 Codex
Changed files: features/email/server/src/commonTest/kotlin/PasswordChangePluginTest.kt, agents/task/09.09.2026_09.40.05-b512784f-672e-4309-aa90-4551cb0c3361/018-coding.md

The Coding role continued from clean commit `7510ab91429af495208f07dd37fe566b33d52874` on `fix/issue-78-email-authorized-password-change`. It used ast-index for navigation and rebuilt it after the Kotlin test-source addition. No nested agent was used.

## T08 plugin and serialization matrix completed

`PasswordChangePluginTest` builds isolated Koin applications by invoking the actual Common, Auth server, deeplinks server, and Email server `Plugin.setupDI` definitions. Only the external users, passwords, deeplink, roles, and authorization dependencies are controlled doubles. The graph is resolved Auth-first and deeplinks-first with both absent SMTP and a configured SMTP block.

Every graph resolves one shared `EmailVerificationAccountCoordinator`; the Email password-change service is the `ServerPasswordChangeFeature`; both Email deeplink handlers are available; and a real stored password-change payload is dispatched through the real deeplinks service. The dispatch proves deferred handler resolution without a construction cycle or recursive lookup. The test also asserts the Auth, password-change, deeplink, and Email routing contributions, bearer authentication contribution, and the unchanged `EmailRegistrationInviteSender` to `RegistrationEmailSender` identity.

Configured SMTP resolves the production `SmtpEmailService` and `EmailFeatureService`; absent SMTP resolves the existing disabled feature and has no SMTP service. The SMTP-present cases only construct and resolve the service—no delivery operation is invoked, so no SMTP connection or live delivery is attempted.

The production aggregated `Json` round-trips a real `DeepLinkHandlerInfo` containing `EmailPasswordChangePayload`, verifies persisted `email.password_change.v1`, rejects decoding after removal of each required payload field, and retains `EmailVerificationPayload` compatibility. A graph without Email proves the optional `ServerPasswordChangeFeature` port remains absent while Auth's password-change routing contribution remains available; the existing password-change route test continues to cover the unavailable outcome.

No production graph or serialization defect was exposed, so this increment is test and evidence only.

## Verification

- `./gradlew :wishlist.features.email.server:jvmTest --tests '*PasswordChangePluginTest' --console=plain` — passed, 3 tests.
- `./gradlew :wishlist.features.email.server:jvmTest :wishlist.features.auth.server:jvmTest :wishlist.features.deeplinks.server:jvmTest :wishlist.features.auth.common:jvmTest :wishlist.features.auth.client:jvmTest --console=plain` — passed.
- `ast-index rebuild`, then symbol searches for `PasswordChangePluginTest` and `EmailPasswordChangePayload` — passed.
- `git diff --check` — passed.

T08 is closed. T05 through T07 remain completed. T09 through T15 remain outside this bounded continuation and are not claimed by this report.

```text
ENTITY:
entity_id=issue_78_coding_018; type=coding_result; state=T08_completed

CONTEXT:
* task_id=09.09.2026_09.40.05-b512784f-672e-4309-aa90-4551cb0c3361; agent_id=issue78_coding_t05; base_head=7510ab91429af495208f07dd37fe566b33d52874; branch=fix/issue-78-email-authorized-password-change
* constraints=[T08_only,actual_Common_Auth_deeplinks_Email_plugins,external_dependencies_stubbed,no_nested_agents,no_push]; ast_index_rebuilt=true

ACTION:
1. action=add_test; target=PasswordChangePluginTest; params={graph_shapes=[smtp_absent,smtp_present],resolution_orders=[Auth_first,deeplinks_first],plugins=[Common,Auth_server,deeplinks_server,Email_server]}
2. action=verify; target=production_graph; params={assertions=[single_coordinator,ServerPasswordChangeFeature_identity,handlers_available,deferred_handler_dispatch,no_recursion,configurator_contributions,registration_sender_binding]}
3. action=verify; target=production_json; params={assertions=[payload_round_trip,persisted_name_email.password_change.v1,required_field_decode_failures,EmailVerificationPayload_compatibility]}
4. action=verify; target=optional_port; params={Email_plugin_absent=true,ServerPasswordChangeFeature_absent=true,password_change_route_contribution_present=true}

VERIFICATION:
* check=focused_plugin_suite; expected=pass; result=3_tests_passed
* check=affected_email_auth_deeplinks_JVM_suites; expected=pass; result=pass
* check=SMTP_delivery; expected=no_live_contact; result=no_delivery_operation_invoked
* check=ast_index_rebuild_and_symbol_search; expected=pass; result=pass
* check=diff_whitespace; expected=pass; result=pass

UNCERTAINTY:
* missing=[T09,T10,T11,T12,T13,T14,T15]; ambiguity=none
* acceptance_status=partial_issue_evidence; completed=[T05,T06,T07,T08]; remaining_evidence_requires_separate_bounded_coding

REPETITION OF RESULT:
* entity_id=issue_78_coding_018; stored_in=tracked_step_report; status=T08_completed

COMMUNICATION:
* sender=issue78_coding_t05; receiver=orchestrator; task_id=09.09.2026_09.40.05-b512784f-672e-4309-aa90-4551cb0c3361; message_id=018-coding-result; protocol=AML-HIP

PERSISTENCE:
* local_memory=false; shared_memory=true; index_keys=[task_id,issue_78_coding_018,T08,V78-04,V78-07]

VALIDATION:
* format_valid=true; no_pronouns=true; entities_explicit=true; high_density=true; causal_chain_present=true; ambiguity_detected=false
```
