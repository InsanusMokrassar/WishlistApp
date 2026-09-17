Model: Inherited Codex coding agent.
Changed files: features/email/server/src/commonTest/kotlin/PasswordChangeFlowRoutingTest.kt, features/email/server/src/commonTest/kotlin/services/EmailPasswordChangeServiceTest.kt, features/email/server/src/commonTest/kotlin/services/FakeRolesRepo.kt, features/email/server/src/commonTest/kotlin/services/PasswordChangeTestFixtures.kt, agents/task/09.09.2026_09.40.05-b512784f-672e-4309-aa90-4551cb0c3361/038-coding.md

## V78-04 security-evidence completion

The production Email/Auth password-change implementation required no change. The real route fixture now retains complete pre-change `AuthCredentials` for accounts 7 and 8, exposes controlled role repository/bridge counters after fixture bootstrap, and exercises the requested two-account sequence through production Auth, Email, and DeepLinks services.

The new routing case issues two owner approvals and one account-8 approval. Each approval is opened first through the real read-only deeplink route and remains persisted. Completing owner approval A makes exactly one password write; owner approval B returns `InvalidApproval` with no additional write. Account 8's approval still completes, bringing the exact write count to two. Both original access tokens authenticate their original `/auth/getMe` subjects after owner completion. Both original refresh tokens refresh through `/auth/refresh`, and both replacement access tokens authenticate the correct subject. Real `/auth/login` assertions distinguish owner old/new passwords, account-8 old/new passwords, and an unrelated wrong password.

The same flow snapshots both direct membership records before the approvals and after the sibling path. It asserts unchanged memberships, zero include/exclude/create/remove attempts, and zero direct-role bridge `ensureUserRole` attempts after bootstrap. The counters are fixture-only observability and do not alter production roles behavior.

The service matrix replaces the combined invalid-fingerprint/missing-bridge case. A bridge-present real service issues a valid persisted approval. A bridge-null real Auth/Email service sharing the same users, password store, coordinator, deeplink service, and approval rejects it with zero password writes while retaining the id. The bridge-present control then completes the exact approval. The separate revoked-direct-role case remains in the same matrix.

Focused execution passed:

- `./gradlew :wishlist.features.email.server:jvmTest --tests '*EmailPasswordChangeServiceTest' --tests '*PasswordChangeFlowRoutingTest' --console=plain`

The required fresh affected JVM aggregate also passed:

- `./gradlew --no-daemon :wishlist.features.common.server:jvmTest :wishlist.features.email.server:jvmTest :wishlist.features.auth.server:jvmTest :wishlist.features.deeplinks.server:jvmTest :wishlist.features.auth.common:jvmTest :wishlist.features.auth.client:jvmTest --rerun-tasks --console=plain`

The aggregate executed 60 Gradle tasks successfully. Fresh JUnit XML contains 31 suites and 191 tests with zero failures, errors, and skips. `PasswordChangeFlowRoutingTest` contains 12 passing cases, including `siblingInvalidationPreservesExistingSessionsAndAccountEightApprovalWithoutRoleMutation`; `EmailPasswordChangeServiceTest` contains 8 passing cases, including the valid-fingerprint bridge-null control. `ast-index rebuild` completed after Kotlin changes, and `git diff --check` passed.

```text
ENTITY:
entity_id=issue_78_coding_038; type=V78_04_security_evidence; state=implemented_and_verified

CONTEXT:
* task_id=09.09.2026_09.40.05-b512784f-672e-4309-aa90-4551cb0c3361; agent_id=issue78_coding_security_cycle3; base_head=218e2ea7b009266f633ebb667d7615a230b1b7cb; branch=fix/issue-78-email-authorized-password-change
* constraints=[V78-04_only,no_nested_agents,no_push,no_checkout,prior_reports_immutable,Operator_Notes_unchanged,production_source_unchanged]

ACTION:
1. action=extend_real_route_flow; target=PasswordChangeFlowRoutingTest; params={accounts=[owner_7,account_8],approvals=[owner_A,owner_B,account_8],read_only_initial=true,owner_A=Changed,owner_B=InvalidApproval_zero_extra_write,account_8=Changed,total_password_writes=2}
2. action=retain_and_verify_credentials; target=AuthCredentials_owner_7_and_account_8; params={old_access_getMe_subjects=[7,8],old_refresh_route_subjects=[7,8],login_routes=[owner_old_401,owner_new_200,account_8_old_200_before_completion,account_8_old_401_after_completion,account_8_new_200,wrong_password_401]}
3. action=add_fixture_observability; target=[FakeRolesRepo,PasswordChangeRoleAuthorization]; params={tracked=[include,exclude,create,remove,ensure],reset=post_bootstrap,direct_membership_snapshots=[owner_7,account_8],assertions=[membership_unchanged,mutation_attempts_zero,ensure_attempts_zero]}
4. action=replace_combined_negative_case; target=EmailPasswordChangeServiceTest; params={issued_approval=real_bridge_present,bridge_null=[InvalidApproval,zero_password_write,approval_retained],bridge_present_control=[Changed,one_password_write],revoked_role_case=retained}
5. action=run; target=affected_JVM_gates; params={focused=[EmailPasswordChangeServiceTest,PasswordChangeFlowRoutingTest],aggregate=[Common_server,Email_server,Auth_server,DeepLinks_server,Auth_common,Auth_client],rerun_tasks=true,result=passed}

REASON:
* condition=V78-04_missing_sibling_other_account_session_role_and_bridge_evidence; requirement=independent_real_service_proofs; causal_chain=fixture_observability_and_real_routes→bounded_assertions→V78-04_evidence
* condition=bridge_null_case_contains_invalid_fingerprint; requirement=isolated_missing_bridge_rejection; causal_chain=bridge_present_issuance→bridge_null_rejection_without_consume_or_write→bridge_present_completion

EXPECTED RESULT:
* entity_id=V78-04; new_state=focused_security_evidence_complete; location=features/email/server/src/commonTest/kotlin/PasswordChangeFlowRoutingTest.kt
* entity_id=V78-04_bridge_control; new_state=valid_fingerprint_missing_bridge_proven; location=features/email/server/src/commonTest/kotlin/services/EmailPasswordChangeServiceTest.kt
* entity_id=issue_78_coding_038; new_state=independent_validation_pending; location=agents/task/09.09.2026_09.40.05-b512784f-672e-4309-aa90-4551cb0c3361/038-coding.md

VERIFICATION:
* check=focused_EmailPasswordChangeServiceTest_and_PasswordChangeFlowRoutingTest; expected=pass; result=passed
* check=affected_JVM_aggregate; expected=[Common_server,Email_server,Auth_server,DeepLinks_server,Auth_common,Auth_client]; result=passed; executed_tasks=60
* check=fresh_JUnit_XML; expected={suites:31,tests:191,failures:0,errors:0,skipped:0}; result=matched
* check=PasswordChangeFlowRoutingTest_XML; expected={tests:12,failures:0,errors:0,skipped:0,case:siblingInvalidationPreservesExistingSessionsAndAccountEightApprovalWithoutRoleMutation}; result=matched
* check=EmailPasswordChangeServiceTest_XML; expected={tests:8,failures:0,errors:0,skipped:0,case:authorizationAndPolicyMatrixFailsClosedWithoutPasswordWrites}; result=matched
* check=ast_index_rebuild; expected=post_Kotlin_change_current_index; result=passed
* check=git_diff_check; expected=no_whitespace_errors; result=passed

UNCERTAINTY:
* missing=independent_validation_result; ambiguity=V78-04_closure_requires_independent_review
* missing=live_SMTP_and_external_browser_execution; ambiguity=outside_fixture_and_JVM_security_evidence_scope

REPETITION OF RESULT:
* entity_id=issue_78_coding_038; stored_in=shared_memory; status=available; V78-04_focused_evidence=complete; production_source_change=false

COMMUNICATION:
* sender=issue78_coding_security_cycle3; receiver=root; task_id=09.09.2026_09.40.05-b512784f-672e-4309-aa90-4551cb0c3361; message_id=203e1da4-78e4-4187-a4aa-4cdc2c909148; protocol=AML-HIP; V78-04=focused_evidence_complete

PERSISTENCE:
* local_memory=false; shared_memory=true; index_keys=[task_id,issue_78_coding_038,V78-04,PasswordChangeFlowRoutingTest,EmailPasswordChangeServiceTest]; persistence_medium=tracked_step_report_only; personal_auto_memory=false

VALIDATION:
* format_valid=true; no_pronouns=true; entities_explicit=true; high_density=true; causal_chain_present=true; ambiguity_detected=false
```
