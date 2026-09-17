Model: GPT-5.6 Codex
Changed files: features/email/server/src/commonMain/kotlin/services/EmailPasswordChangeService.kt, features/email/server/src/commonTest/kotlin/services/EmailPasswordChangeServiceTest.kt, features/email/server/src/commonTest/kotlin/services/PasswordChangeTestFixtures.kt, features/email/server/src/commonTest/kotlin/services/EmailPasswordChangeIssuanceTest.kt, agents/task/09.09.2026_09.40.05-b512784f-672e-4309-aa90-4551cb0c3361/016-coding.md

The Coding role used the available ML Codex model, as required by the Coding model priority. This continuation began clean at `96fd9dfb0c03a1da9aa5e8a4379721e45a89d97c` on `fix/issue-78-email-authorized-password-change`. It used ast-index before navigation and rebuilt it after source edits. No nested agent was used.

## T05 issuance matrix completed

`EmailPasswordChangeService.requestPasswordChangeEmail` now records one domain outcome and one primary failure before a single cleanup decision. Only ordinary `EmailsService.sendHtml` exceptions classify as `DeliveryFailed`; repository, coordinator, Auth, cancellation, and fatal failures remain exceptional. Every owned non-Sent id receives exactly one non-cancellable removal attempt. Successful delivery retains the approval. A failed SMTP response or ineligible post-send state exposes the cleanup failure when removal fails. SMTP, repository, and cancellation primaries retain the exact primary throwable and suppress the cleanup throwable when required.

`PasswordChangeTestFixtures.kt` extracts the prior fixture and adds controlled map-backed deeplink/password repositories plus a user facade. Hooks are suspendable before and after actual delegated reads, writes, and removals. The fixture still constructs real `EmailPasswordChangeService`, `EmailVerificationAccountCoordinator`, `AuthFeatureService`, and `DeepLinksService`; no fake service or production transaction layer was introduced.

`EmailPasswordChangeIssuanceTest` contains 13 deterministic real-service cases. The matrix covers mint-before-write failure, real DeepLinksService write-then-throw cleanup preservation, false SMTP removal and removal failure, ordinary SMTP exception with successful and failed cleanup, post-send Auth-read failure with successful and failed cleanup, changed account state after delivery, cancellation after mint enrollment, cancellation after SMTP acceptance, cancellation while SMTP is suspended, and Sent retention. Each cleanup case records one exact id attempt, checks no issuance password write, and preserves sibling and unrelated records. The mint write-then-throw case uses real `DeepLinksService` rather than a fake minting service.

The first focused invocation exposed an unresolved import left after fixture extraction; that import was restored. The next focused invocation exposed Kotlin coroutine stack-trace recovery copying cleanup exceptions across `withContext(NonCancellable)`. The bounded cleanup helper now captures the exception inside the non-cancellable region and rethrows outside it, preserving primary and suppressed throwable identity without widening the non-cancellable work.

## Verification

- `./gradlew :wishlist.features.email.server:jvmTest --tests '*EmailPasswordChangeIssuanceTest' --console=plain` — passed, 13 tests.
- `./gradlew :wishlist.features.email.server:jvmTest :wishlist.features.auth.server:jvmTest :wishlist.features.deeplinks.server:jvmTest :wishlist.features.auth.common:jvmTest :wishlist.features.auth.client:jvmTest --console=plain` — passed.
- `ast-index rebuild` followed by symbols for `EmailPasswordChangeIssuanceTest`, `PasswordChangeTestFixtures`, and `EmailPasswordChangeService` — passed.
- `git diff --check` — passed before the step report was added; no whitespace issue was present.

T05 is closed. T06 is already completed by Coding 014. T07--T11 and T12--T15 remain outside this bounded continuation and are not claimed by this report.

```text
ENTITY:
entity_id=issue_78_coding_016; type=coding_result; state=T05_completed

CONTEXT:
* task_id=09.09.2026_09.40.05-b512784f-672e-4309-aa90-4551cb0c3361; agent_id=issue78_coding_t05; base_head=96fd9dfb0c03a1da9aa5e8a4379721e45a89d97c; branch=fix/issue-78-email-authorized-password-change
* constraints=[T05_only,real_service_graph,single_cleanup_owner,no_nested_agents,no_push]; ast_index_rebuilt=true

ACTION:
1. action=refactor_and_fix; target=EmailPasswordChangeService.requestPasswordChangeEmail; params={outcome=single,cleanup_attempts=one,SMTP_exception=DeliveryFailed,repository_cancellation_fatal=propagated,suppression=primary_identity_preserved}
2. action=extract; target=PasswordChangeTestFixtures.kt; params={services=[EmailPasswordChangeService,EmailVerificationAccountCoordinator,AuthFeatureService,DeepLinksService],repositories=[deeplinks,passwords,users],hooks=[before_delegation,after_delegation],hooks_suspendable=true}
3. action=add_test; target=EmailPasswordChangeIssuanceTest; params={case_count=13,cases=[mint_before_write,mint_write_throw,false_SMTP,SMTP_exception,post_send_read_failure,state_change,cancellation_mint,cancellation_SMTP_acceptance,cancellation_SMTP_suspend,Sent_retention],assertions=[exact_id,one_removal,no_password_write,sibling_preservation,unrelated_preservation,primary_identity,suppressed_identity]}

VERIFICATION:
* check=focused_issuance_suite; expected=pass; result=13_tests_passed
* check=affected_email_auth_deeplinks_JVM_suites; expected=pass; result=pass
* check=ast_index_rebuild; expected=pass; result=pass
* check=diff_whitespace; expected=pass; result=pass

UNCERTAINTY:
* missing=[T07,T08,T09,T10,T11,T12,T13,T14,T15]; ambiguity=none
* acceptance_status=partial_issue_evidence; completed=[T05,T06]; remaining_evidence_requires_separate_bounded_coding

REPETITION OF RESULT:
* entity_id=issue_78_coding_016; stored_in=tracked_step_report; status=T05_completed

COMMUNICATION:
* sender=issue78_coding_t05; receiver=orchestrator; task_id=09.09.2026_09.40.05-b512784f-672e-4309-aa90-4551cb0c3361; message_id=016-coding-result; protocol=AML-HIP

PERSISTENCE:
* local_memory=false; shared_memory=true; index_keys=[task_id,issue_78_coding_016,T05,V78-03]

VALIDATION:
* format_valid=true; no_pronouns=true; entities_explicit=true; high_density=true; causal_chain_present=true; ambiguity_detected=false
```
