Model: gpt-5.6-sol
Changed files: agents/task/10.08.2026_09.08.17-c0ecd3ab-3385-4a7f-ab41-649fb5f51d24/008-planning.md

## Model rationale

The Planning preference list names `fable`, `opus`, and `sonnet`, none of which is available in the current Codex session. The inherited `gpt-5.6-sol` model is the strongest available frontier agentic coding model and is appropriate for the narrow cross-repository concurrency analysis. The report records the model actually used.

## Task understanding and confirmed failure

Step `007-validating.md` correctly proves that the first address-binding fix remains non-atomic. `EmailVerificationDeepLinkHandler.tryHandle` reads address A and compares A with the payload at `EmailVerificationDeepLinkHandler.kt:33-36`, then calls the separately synchronized role promotion at line 37. Both enabled and disabled email services reach `updateStoredEmail`, which independently reads and updates `UsersRepo` at `UpdateStoredEmail.kt:25-27`. A credentialed pending account can therefore store B after the handler accepts A but before `UserRole` is granted.

The cycle-two scope is only that TOCTOU. The previous fixes for registration compensation, role cleanup, source compatibility, legacy client fallback, public origin, SMTP cancellation, and deployment safety remain accepted. No resend flow, link consumption, re-verification policy, schema change, authentication redesign, or application-wide role gate belongs in this cycle.

## Smallest correct product contract

The recommended correction is a single email-server transition coordinator shared by self-service email mutation and verification. A policy restriction on pending-user edits would require a new role-query contract for both `EmailFeatureService` and `DisabledEmailFeature`, would contradict the current route statement that any authenticated user may edit an address, and would leave typo correction without a resend path. Serialization preserves the existing policy without creating a new product decision.

The email server already depends on `features/users/common` and `features/roles/server`; the current plugin already resolves `UsersRepo` and `RolesRepo` to construct the email services and verification handler. A new `EmailVerificationAccountCoordinator` can therefore own the existing repositories plus one process-local `Mutex` without adding a Gradle dependency or crossing a new module boundary. The plugin must register exactly one coordinator singleton and inject the same instance into `EmailFeatureService`, `DisabledEmailFeature`, and `EmailVerificationDeepLinkHandler`.

The coordinator exposes two operations. The email mutation operation holds the coordinator mutex across the current user read and `UsersRepo.update`. The verification operation holds the same mutex across payload validation, current-user read, invited-email equality comparison, and `promoteNewUserToUser`. The existing Roles transition mutex remains nested inside verification and continues to protect role ordering; no Roles function acquires the new email coordinator, so the design introduces no reverse lock order. The user-repository operation completes before verification asks for the Roles mutex, avoiding a simultaneous Users lock while waiting for Roles.

The observable contract is linearizable. When an update to B acquires the coordinator first, B is stored before verification evaluates the payload; verification returns `false`, `NewUserRole` remains, and `UserRole` is absent. When verification acquires the coordinator first, promotion completes while A is still the stored address; the update to B waits, then may succeed after approval as the already-documented post-approval self-service edit. A final `UserRole` plus B is valid only in that second ordering because A was current at the promotion linearization point. The handler must continue failing closed for a missing user, null legacy payload email, cleared email, and nonmatching email.

## Concrete implementation plan

Add the coordinator under `features/email/server/src/commonMain/kotlin/services/`. Move the logic currently split between `updateStoredEmail` and `EmailVerificationDeepLinkHandler.tryHandle` behind coordinator methods. Keep `updateStoredEmail` only as a thin shared delegate if retaining the helper minimizes constructor churn; the helper must accept the coordinator rather than a raw `UsersRepo`. `EmailFeatureService.setMyEmail` and `DisabledEmailFeature.setMyEmail` must both use the coordinated mutation path so SMTP configuration cannot change concurrency behavior. The handler should delegate the complete validation-and-promotion operation to the coordinator rather than reading `UsersRepo` before delegation.

Update `features/email/server/src/commonMain/kotlin/Plugin.kt` to register one coordinator and supply the same Koin singleton to both possible `EmailFeature` realizations and the deeplink handler. Existing `UsersRepo`, `RolesRepo`, and `RolesFeature` registrations are sufficient. No Auth, Roles, Users, DeepLinks, configuration, or database production file should change unless compilation exposes a constructor call that directly follows from this injection change.

Constructor-based tests for `EmailFeatureService`, `DisabledEmailFeature`, `EmailVerificationDeepLinkHandler`, and the deeplink integration must be adapted to share one coordinator where mutation and verification interact. Existing sequential matching, mismatch, cleared, legacy, missing-user, repeated-open, duplicate-email-to-409, and SMTP-disabled storage tests must remain.

## Deterministic concurrency regression test

Add a deterministic test in the email server test source set using one in-memory `FakeUsersRepo`, one pending `FakeRolesRepo` variant with a promotion barrier, one shared coordinator, the real handler, and the real coordinated self-service mutation path. The blocking roles fake should signal when `excludeDirect(NewUserRole)` is reached and suspend until explicitly released. Reaching that barrier proves that the handler already read A and passed the equality comparison while still holding the coordinator mutex.

Start verification for payload A and await the promotion barrier. Launch an authenticated-style update to B through `DisabledEmailFeature.setMyEmail` or the shared helper, then run the test scheduler. Before releasing promotion, assert that the update job is incomplete and the repository still stores A. Release promotion and record the stored email when `UserRole` is included; the recorded value must be A. Await successful handling, then allow the waiting update to finish. The final state may be `UserRole` plus B, but the observed event order must be `promotion_with_A` followed by `update_to_B`.

Add the inverse ordering in the same test class: complete the coordinated update to B before starting verification for payload A. Handling must return `false`; the final user stores B, direct roles contain exactly `NewUserRole`, and `UserRole` is absent. Together the two orderings prove that B cannot be stored between a successful comparison and promotion: B either precedes the atomic verification and blocks approval, or follows completed approval as an ordinary approved-user edit.

The test must not rely on timing, sleeps, or an unconstrained dispatcher. Use `CompletableDeferred` barriers and the coroutine test scheduler. The promotion barrier belongs in the fake repository or another boundary double, not in production code.

## Review and documentation correction

Keep `local.review.74.md` local and ignored. Preserve the original PR-head evidence, severity, and Request Changes decision. Replace the address-binding finding's current serial-only “fixed” statement with the shared-coordinator result and deterministic interleaving coverage after implementation. The review must explain both legal orderings so a final approved account storing B is not misclassified when B was written after promotion. Update the focused-verification summary only after the new test and full build pass.

Update the Email README model and architecture text that currently says the equality check alone prevents A from approving B. The corrected text must state that email mutation and equality-plus-promotion share one coordinator; an update linearized before verification makes the link stale, while an update linearized after promotion remains allowed by the established self-service policy. The route contract remains unchanged, including SMTP-independent storage and duplicate-email `409` handling. No Operator Notes content may change.

## Process debt

Commit `eb928ba8d6eb0130b7017b36b8898235c77ef519` contains literal `\n\n` text and no trailer recognized by `git interpret-trailers`. Step `007-validating.md` already records the Low process debt. History must remain unchanged; the current Planning commit and every later cycle commit must use a separately parsed `Co-Authored-By: Claude <noreply@anthropic.com>` trailer.

## Verification handoff

After implementation, rebuild `ast-index` because Kotlin sources change. Run the Email server focused tests, including both forced orderings, then the full `./gradlew build`. Run `git diff --check`, recount Gradle XML results, and confirm `local.review.74.md` remains ignored and untracked while its local status text and the Email README are accurate. The next Validator must inspect the coordinator instance wiring and rerun the deterministic interleaving test rather than relying only on serial mismatch coverage.

## QUESTIONS FOR OPERATOR

None. A shared coordinator preserves every existing route and post-approval email-edit policy, fits current module dependencies, and resolves the High finding without a new product choice.

## AML-HIP

ENTITY:
entity_id=pr74_cycle_2_plan; type=planning_report; state=ready_for_architecture
entity_id=email_verification_atomicity; type=high_finding; state=remediation_defined
entity_id=email_account_transition_coordinator; type=concurrency_boundary; state=planned
entity_id=planning_commit_trailer_debt; type=low_process_debt; state=documented_no_history_rewrite

CONTEXT:

* task_id=10.08.2026_09.08.17-c0ecd3ab-3385-4a7f-ab41-649fb5f51d24; agent_id=planning-008; memory_ref=[002-architecturing.md,007-validating.md,email_README,roles_README,users_README,current_source]
* constraints=[edit_only_008-planning.md,no_product_changes,no_push,scope=email_update_verification_TOCTOU,post_approval_email_edit_preserved]

ACTION:

1. action=create_singleton_coordinator; target=email_account_transition_coordinator; params={module=email_server,dependencies=[UsersRepo,RolesRepo,Mutex],instances=1}
2. action=serialize_email_mutation; target=email_verification_atomicity; params={critical_section=[read_current_user,UsersRepo_update],callers=[EmailFeatureService,DisabledEmailFeature]}
3. action=serialize_verification; target=email_verification_atomicity; params={critical_section=[payload_validation,user_read,email_compare,role_promotion],legacy_null_email=reject,mismatch=reject}
4. action=force_interleaving_test; target=email_verification_atomicity; params={sequence=[read_A,launch_update_B,promotion_barrier],expected_order=[promotion_with_A,update_to_B]}
5. action=test_inverse_order; target=email_verification_atomicity; params={sequence=[update_B_complete,verify_A],expected={handled=false,roles=[NewUserRole],stored_email=B}}
6. action=correct_status; target=local.review.74.md; params={original_head_evidence=preserve,fix_status=atomic_coordinator_tested,tracking=ignored_local}
7. action=correct_documentation; target=email_README; params={equality_only_claim=remove,linearization_orderings=document,operator_notes=unchanged}
8. action=retain_process_debt; target=planning_commit_trailer_debt; params={commit=eb928ba8d6eb0130b7017b36b8898235c77ef519,history_rewrite=false,new_trailers=parsed}

REASON:

* condition=email_compare_and_role_promotion_separated_from_email_update → action=shared_email_coordinator_mutex → result=no_update_between_successful_compare_and_promotion; requirement=stored_email_matches_invited_email_at_promotion
* condition=update_B_linearizes_before_verification → action=handler_reads_B_and_rejects_A → result=pending_account_with_B; requirement=uninvited_B_not_approved
* condition=verification_linearizes_before_update_B → action=promotion_completes_with_A_then_update_B_runs → result=approved_account_edit_after_verification; requirement=existing_post_approval_policy_preserved
* condition=email_server_already_depends_on_users_and_roles_servers → action=email_owned_coordinator → result=no_new_module_dependency_or_schema; requirement=minimal_change
* condition=pending_edit_restriction_requires_new_role_policy_and_resend_decision → action=choose_serialization → result=no_operator_product_choice; requirement=bounded_cycle_scope

EXPECTED RESULT:

* entity_id=email_verification_atomicity; new_state=linearizable_and_deterministically_tested; location=email_server_services
* entity_id=email_account_transition_coordinator; new_state=shared_by_email_feature_and_deeplink_handler; location=email_server_Koin_graph
* entity_id=review_and_docs; new_state=accurate_atomic_fix_status; location=local.review.74.md+features/email/README.md

VERIFICATION:

* check=forced_read_A_update_B_attempt_promote; expected={update_blocked_before_promotion=true,email_at_UserRole_grant=A,event_order=[promotion_with_A,update_to_B]}
* check=update_B_before_verification; expected={handled=false,stored_email=B,direct_roles=[NewUserRole]}
* check=legacy_and_serial_handler_cases; expected={matching=true,mismatch=false,cleared=false,legacy_null=false,missing_user=false}
* check=SMTP_disabled_email_storage; expected=coordinated_update_path
* check=full_gradle_build; expected={exit_code=0,failures=0,errors=0}
* check=review_locality; expected={ignored=true,tracked=false,status_text=atomic_fix_complete}
* check=current_commit_trailer; expected={parsed_Co_Authored_By=1,history_rewritten=false}

UNCERTAINTY:

* missing=none; ambiguity=none

REPETITION OF RESULT:

* entity_id=pr74_cycle_2_plan; stored_in=008-planning.md; status=ready_for_architecture
* entity_id=email_verification_atomicity; stored_in=008-planning.md; status=High_remediation_required

COMMUNICATION:

* sender=planning-008; receiver=architecture-next; task_id=10.08.2026_09.08.17-c0ecd3ab-3385-4a7f-ab41-649fb5f51d24; message_id=87c49aa6-39f2-47f2-87b5-da6318c461ef; protocol=AML-HIP

PERSISTENCE:

* local_memory=true; shared_memory=true; index_keys=[task_id,entity_id,atomicity,ordering,test,review_status]

VALIDATION:

* format_valid=true; no_pronouns=true; entities_explicit=true; high_density=true; causal_chain_present=true; ambiguity_detected=false
