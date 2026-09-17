Model: Codex GPT-5
Changed files: client/src/commonTest/kotlin/PasswordChangeInteractorTest.kt, client/src/commonTest/kotlin/PasswordChangeNavigationTestFixtures.kt, agents/task/09.09.2026_09.40.05-b512784f-672e-4309-aa90-4551cb0c3361/050-coding.md

## Outcome

Added only the first V78-12 red-regression increment. The two new common tests build a real root → ancestor → leaf hierarchy with a Pending password page, replace the live ancestor, and make exact object-identity assertions. The first test proves immutable ancestry remains intact after detachment while the current owner still accepts a stale callback. The second uses the bounded FIFO/TestDispatcher leaf queue, prevents leaf stack delivery, and proves the current leaf-only observer does not complete when the outer ancestor is replaced.

The bounded fixture adds a held dispatcher with deterministic delegated delays and a synchronous recording navigation repository. Both test scopes, the explicit leaf-start Job, root scopes, and chain Jobs are cancelled and joined in `finally` blocks. No production code, README, dependency, browser, lifecycle, security, or documentation work was changed.

## Red evidence

The focused command was `./gradlew --no-daemon :wishlist.client:jvmTest --tests '*PasswordChangeInteractorTest' --console=plain`. It compiled the final test source and discovered nine JVM cases. Seven pre-existing cases passed; no errors, skips, or unrelated failures occurred. The expected two red failures were:

- `detachedPendingAtEntryStartsNoTransition`: `activeTransitionCount` expected `0`, observed `1` after stale `onChanged` and `onContinue` on the detached leaf. The test also asserts zero Completed factory calls, zero saves, and preservation of the live outer replacement.
- `detachedWhileQueuedStopsWithoutLeafEmission`: expected all root transition children completed after the outer replacement without advancing the five-second timer; observed `false`. The later assertions require zero owner jobs/saves, unchanged leaf stack, and the exact outer replacement.

Discovered passing cases were `actualSubmittingViewModelDestructionPrecedesCompletedPersistence`, `cancelledSubmittingViewModelCannotStartChangedHandoff`, `changedPendingReplacesCredentialRouteAndContinueAlwaysReachesUsersList`, `rootBindingDisposalCancelsPendingTransitionAndCleansOwnerJobs`, `saveFailureDoesNotReplayAndLaterContinuePersistsUsersList`, `stalePendingCannotReplaceNewerDestination`, and `unprocessedReplacementTimesOutAndCleansOwnerJobs`.

`ast-index --walk-up rebuild` completed after Kotlin changes and indexed 1,461 files. `git diff --check` passed. The next bounded production change is the Architecture 049 owner fix: current root subtree identity membership at admission and during observation, with root-subtree wakeups and prompt detached-transition cleanup. V78-12 remains open; this report does not close a finding.

```text
ENTITY:
entity_id=issue_78_v78_12_red_increment_050; type=common_test_regression; state=red_committed

CONTEXT:
* task_id=09.09.2026_09.40.05-b512784f-672e-4309-aa90-4551cb0c3361; agent_id=issue78_coding_red_reachability; memory_ref=[049-architecturing.md:issue_78_cycle4_regenerated_handoff,047-validating.md:V78-12]; entry_head=19fb80cb8a47f8f1ee8135055f7c11b68c88f5a4
* constraints=[commonTest_only,no_production_change,no_push,no_checkout,no_nested_agents,serial_Gradle,identity_assertions,FIFO_TestDispatcher,final_cleanup]

ACTION:
1. action=add_regression; target=PasswordChangeInteractorTest.detachedPendingAtEntryStartsNoTransition; params={hierarchy=root_ancestor_leaf_Pending,detachment=live_root_replace_ancestor,stale_calls=[onChanged,onContinue],assertions=[immutable_ancestry,Completed_factory_calls_0,owner_jobs_0,saves_0,outer_replacement_identity]}
2. action=add_regression; target=PasswordChangeInteractorTest.detachedWhileQueuedStopsWithoutLeafEmission; params={leaf_queue=HeldNavigationDispatcher,outer_replacement=live_root_replace_ancestor,timer_advance_ms=0,assertions=[root_transition_completion,owner_jobs_0,saves_0,outer_replacement_identity,leaf_stack_identity]}
3. action=add_fixture; target=PasswordChangeNavigationTestFixtures; params={types=[HeldNavigationDispatcher,RecordingPasswordNavigationRepo],cleanup=[leaf_start_cancel_join,held_scope_cancel_join,root_scope_cancel_join,root_chain_cancel_join]}

REASON:
* condition=ancestor_replacement_preserves_detached_leaf_parent_links; requirement=current_root_identity_membership; causal_chain=stale_callback_admission_and_leaf_only_observer→accepted_job_or_timeout_wait→V78_12_red_evidence

EXPECTED RESULT:
* entity_id=PasswordChangeNavigationOwner; new_state=live_root_subtree_membership_admission_and_observation; location=client/src/commonMain/kotlin/PasswordChangeNavigationOwner.kt; prerequisite=next_Coding_increment

VERIFICATION:
* check=focused_gate; expected={tests:9,failures:2,errors:0,skips:0,unrelated_failures:0}; result={tests:9,failures:2,errors:0,skips:0,existing_passes:7}; command=./gradlew_--no-daemon_:wishlist.client:jvmTest_--tests_*PasswordChangeInteractorTest_--console=plain
* check=detachedPendingAtEntryStartsNoTransition; expected=activeTransitionCount_0; observed=activeTransitionCount_1; result=red
* check=detachedWhileQueuedStopsWithoutLeafEmission; expected=rootScopeJob_children_completed_true; observed=rootScopeJob_children_completed_false; result=red
* check=ast_index_rebuild; expected=Kotlin_source_index_current; result={files:1461,status=completed}
* check=git_diff_check; expected=no_whitespace_errors; result=passed

UNCERTAINTY:
* missing=production_live_membership_and_root_subtree_observer; ambiguity=V78_12_fix_behavior_unimplemented
* missing=[browser_production_binding,actual_VM_transport,security_fixture_corrections,LL_KDocs,aggregate_gates]; ambiguity=Architecture_049_sequential_increment_scope

REPETITION OF RESULT:
* entity_id=issue_78_v78_12_red_increment_050; stored_in=shared_memory; status=available; finding=V78-12; finding_state=open; focused_gate_state=expected_red

COMMUNICATION:
* sender=issue78_coding_red_reachability; receiver=orchestrator_and_next_Coding; task_id=09.09.2026_09.40.05-b512784f-672e-4309-aa90-4551cb0c3361; message_id=8d1ff33e-cb57-40b6-a227-5670476cb6cd; protocol=AML-HIP; canonical_ledger=049-architecturing.md:issue_78_cycle4_regenerated_handoff

PERSISTENCE:
* local_memory=true; shared_memory=true; index_keys=[task_id,issue_78_v78_12_red_increment_050,V78-12,detachedPendingAtEntryStartsNoTransition,detachedWhileQueuedStopsWithoutLeafEmission]; persistence_medium=tracked_step_report_only; personal_auto_memory=disabled

VALIDATION:
* format_valid=true; no_pronouns=true; entities_explicit=true; high_density=true; causal_chain_present=true; ambiguity_detected=true
```
