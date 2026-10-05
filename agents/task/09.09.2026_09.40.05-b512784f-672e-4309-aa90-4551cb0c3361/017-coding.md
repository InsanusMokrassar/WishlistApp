Model: GPT-5.6 Codex
Changed files: features/email/server/src/commonTest/kotlin/services/PasswordChangeTestFixtures.kt, features/email/server/src/commonTest/kotlin/services/EmailPasswordChangeCommitTest.kt, agents/task/09.09.2026_09.40.05-b512784f-672e-4309-aa90-4551cb0c3361/017-coding.md

The Coding role used the available ML Codex model, as required by the Coding model priority. This continuation began clean at `ff98e11be52d6395d1cd82987953ab9962c3e745` on `fix/issue-78-email-authorized-password-change`. It used ast-index before navigation and rebuilt it after source edits. No nested agent was used.

## T07 commit matrix completed

`EmailPasswordChangeCommitTest` adds 13 deterministic real-service cases using the shared fixture. The tests retain real Email, Auth, coordinator, and deeplink services. `PasswordChangeDeepLinksRepo` now records delegated reads, allowing the test to suspend exactly at final reread without wall-clock waiting.

The matrix proves that two overlapping valid completions serialize under coordinator then Auth, produce exactly one `Changed` result, one password write, and one exact-id removal. It proves an approval changed or removed after the initial read fails at final reread with no completion consume/write. Independent tests hold the final reread while a coordinated email update, `AuthFeatureService.setPassword`, or `AuthFeatureService.purgeUser` begins; each competitor waits and then completes after the commit releases its locks.

All four persistence-uncertainty outcomes are covered. Removal failure before delegation leaves the approval and old password; removal failure after delegation consumes the approval without writing a password. Password failure before delegation consumes the approval while retaining the old credential; failure after delegation consumes the approval and may retain the new credential. The write-failure cases reject replay, and every failure case proves both coordinator and Auth locks can be reacquired by a subsequent issued approval.

The cancellation cases cover precommit, removal, and password-storage boundaries. Precommit cancellation leaves the approval and credential untouched. Cancellation during the non-cancellable removal or password-write region completes consume/write, propagates cancellation rather than inventing `Changed`, releases both locks, and rejects replay. All gates use entered/completed deferred signals and no sleeps.

No production defect was exposed by the matrix, so this increment changes only test support and tests.

## Verification

- `./gradlew :wishlist.features.email.server:jvmTest --tests '*EmailPasswordChangeCommitTest' --console=plain` — passed, 13 tests.
- `./gradlew :wishlist.features.email.server:jvmTest :wishlist.features.auth.server:jvmTest :wishlist.features.deeplinks.server:jvmTest :wishlist.features.auth.common:jvmTest :wishlist.features.auth.client:jvmTest --console=plain` — passed.
- `ast-index rebuild` followed by symbols for `EmailPasswordChangeCommitTest` and `PasswordChangeDeepLinksRepo` — passed.
- `git diff --check` — passed.

T07 is closed. T05 and T06 remain completed. T08--T15 remain outside this bounded continuation and are not claimed by this report.

```text
ENTITY:
entity_id=issue_78_coding_017; type=coding_result; state=T07_completed

CONTEXT:
* task_id=09.09.2026_09.40.05-b512784f-672e-4309-aa90-4551cb0c3361; agent_id=issue78_coding_t05; base_head=ff98e11be52d6395d1cd82987953ab9962c3e745; branch=fix/issue-78-email-authorized-password-change
* constraints=[T07_only,real_service_graph,coordinator_then_Auth,no_nested_agents,no_push]; ast_index_rebuilt=true

ACTION:
1. action=extend_fixture; target=PasswordChangeDeepLinksRepo; params={records=[get,set,unset],final_reread_gate=true,hooks_suspendable=true}
2. action=add_test; target=EmailPasswordChangeCommitTest; params={case_count=13,cases=[overlap,link_changed,link_removed,coordinated_email_update,Auth_setPassword,Auth_purgeUser,remove_before,remove_after,password_before,password_after,cancel_precommit,cancel_removal,cancel_password_write],gates=[entered,release],sleeps=zero}
3. action=verify; target=T07; params={assertions=[one_Changed,one_password_write,exact_removal,replay_rejection,lock_reacquisition,cancellation_propagation,competing_operation_progress]}

VERIFICATION:
* check=focused_commit_suite; expected=pass; result=13_tests_passed
* check=affected_email_auth_deeplinks_JVM_suites; expected=pass; result=pass
* check=ast_index_rebuild; expected=pass; result=pass
* check=diff_whitespace; expected=pass; result=pass

UNCERTAINTY:
* missing=[T08,T09,T10,T11,T12,T13,T14,T15]; ambiguity=none
* acceptance_status=partial_issue_evidence; completed=[T05,T06,T07]; remaining_evidence_requires_separate_bounded_coding

REPETITION OF RESULT:
* entity_id=issue_78_coding_017; stored_in=tracked_step_report; status=T07_completed

COMMUNICATION:
* sender=issue78_coding_t05; receiver=orchestrator; task_id=09.09.2026_09.40.05-b512784f-672e-4309-aa90-4551cb0c3361; message_id=017-coding-result; protocol=AML-HIP

PERSISTENCE:
* local_memory=false; shared_memory=true; index_keys=[task_id,issue_78_coding_017,T07,V78-04]

VALIDATION:
* format_valid=true; no_pronouns=true; entities_explicit=true; high_density=true; causal_chain_present=true; ambiguity_detected=false
```
