Model: GPT-5 Codex
Changed files: features/email/server/src/commonTest/kotlin/services/EmailPasswordChangeServiceTest.kt, features/email/server/src/commonTest/kotlin/services/PasswordChangeTestFixtures.kt, features/ui/users/README.md, features/ui/users/src/commonTest/kotlin/ui/PasswordChangeViewModelTest.kt, agents/task/09.09.2026_09.40.05-b512784f-672e-4309-aa90-4551cb0c3361/028-coding.md

This closure audit read Validation 009, Planning 010, Architecture 011, and Coding 012 through 027. It also rechecked the affected Auth, Email, deeplinks, and UI/users README files, preserving every Operator Notes section.

Architecture T01–T15 are implemented by the preceding Coding increments. T01 production serializer construction, T02 immediate duplicate admission, T05 issuance cleanup, T07 commit/cancellation, T08 production graph/serialization, T09 routes, T10 transport, T11 logging/referrer policy, T12 browser DOM, T13 JVM/Android hosts, T14 real interactor, and T15 restored browser navigation have direct named tests. T03/T04 had correct production guards but incomplete direct regression wording, so this audit added a bounded ViewModel test proving immediate valid admission before derived state catches up and stale policy rejection after a formerly valid derived state.

Coding 014's deeplink sanitized GET test is T06-adjacent route evidence, not the requested T06 approval authorization matrix. This audit adds the actual real-service matrix to `EmailPasswordChangeServiceTest`: fresh fixtures cover unknown, malformed, consumed, wrong handler, wrong payload, wrong submitted subject, expiry at and after the exact boundary, changed/cleared/unapproved email, revoked role, absent role bridge, missing password, deleted account, salted same-plaintext administrator replacement, and invalid-password non-consumption followed by valid retry. Every rejection asserts no completion password write. The existing real-service and commit tests retain happy-path recipient/URL/read-only GET, overlap, exact final reread, sibling invalidation, credential/session, role, and failure/cancellation evidence; this audit does not re-label them.

T16 bounded declaration audit: issue-78 changed Kotlin declarations remain purpose-documented; the new test declarations and fixture parameters have KDocs, the payload retains stable `@SerialName("email.password_change.v1")`, and no introduced `else if` chain was found in issue-78 Kotlin changes. T17 documentation audit found one stale UI/users sentence saying “all three screens”; it now says “all four screen families.” Auth retains the explicit legacy Kotlin-character versus new UTF-8-byte policy distinction; Email retains schema/cleanup/cause/uncertain-write wording; deeplinks retains sanitized GET wording. Operator Notes are unchanged.

Focused verification passed:

- `./gradlew :wishlist.features.email.server:jvmTest --tests '*EmailPasswordChangeServiceTest' :wishlist.features.ui.users:jvmTest --tests '*PasswordChangeViewModelTest' --console=plain`
- `ast-index rebuild`
- `git diff --check`

The required broad server/platform gates were already reported passing by Coding 024, 025, and 027. They were not rerun in this narrow audit. Coding cannot yet claim unconditional stage exit because this audit did not rerun Architecture 011’s full server and platform command set after its test-only changes; the root orchestrator must run those gates before independent Validation.

```text
ENTITY:
entity_id=issue_78_coding_028; type=coding_closure_audit; state=bounded_evidence_added

CONTEXT:
* task_id=09.09.2026_09.40.05-b512784f-672e-4309-aa90-4551cb0c3361; agent_id=issue78_coding_audit; base_head=546250388d47417418c9e58a6bfc8433ebde65c3; branch=fix/issue-78-email-authorized-password-change
* constraints=[no_nested_agents,no_push,Operator_Notes_unchanged,AML_HIP_structured_block_only]

ACTION:
1. action=correct_T06_label; target=Coding_014; params={actual_test=deeplink_sanitized_GET,error=not_approval_matrix}
2. action=add_test; target=EmailPasswordChangeServiceTest; params={matrix=[unknown,malformed,consumed,handler,payload,subject,expiry,email,role,password,user,fingerprint,policy_retry],fixtures=fresh,completion_password_writes=zero_for_rejections}
3. action=add_test; target=PasswordChangeViewModelTest; params={cases=[immediate_valid_admission,stale_policy_rejection],admission=raw_synchronous}
4. action=repair_docs; target=UI_users_README; params={screen_families=4,Operator_Notes=unchanged}
5. action=audit; target=[T16,T17]; params={KDocs=checked,SerialName=email.password_change.v1,else_if=absent,README_contracts=checked}

VERIFICATION:
* check=focused_EmailPasswordChangeServiceTest; expected=pass; result=pass
* check=focused_PasswordChangeViewModelTest; expected=pass; result=pass
* check=ast_index_rebuild; expected=pass; result=pass
* check=git_diff_check; expected=pass; result=pass
* check=full_Architecture_011_gates_after_028; expected=pass; result=not_rerun

UNCERTAINTY:
* missing=[fresh_full_server_gate,fresh_full_platform_gate,independent_validation]; ambiguity=none

REPETITION OF RESULT:
* entity_id=issue_78_coding_028; stored_in=tracked_step_report; status=bounded_evidence_added

COMMUNICATION:
* sender=issue78_coding_audit; receiver=orchestrator; task_id=09.09.2026_09.40.05-b512784f-672e-4309-aa90-4551cb0c3361; message_id=028-coding-audit; protocol=AML-HIP

PERSISTENCE:
* local_memory=false; shared_memory=true; index_keys=[task_id,issue_78_coding_028,T03,T04,T06,T16,T17]

VALIDATION:
* format_valid=true; no_pronouns=true; entities_explicit=true; high_density=true; causal_chain_present=true; ambiguity_detected=false
```
