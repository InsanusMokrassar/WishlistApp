Model: Codex GPT-5
Changed files: client/src/jsTest/kotlin/PasswordChangeNavigationBrowserTest.kt; agents/task/09.09.2026_09.40.05-b512784f-672e-4309-aa90-4551cb0c3361/060-coding.md

I completed the ML browser-only correction specified by Architecture 059. The canonical browser test still drives the actual Pending ViewModel through the MockEngine, immediately reloads the successful URL through the real adapter, and executes actual Completed Continue. The test now also retains the production-composition A/B rebinding contract.

Before the edit, the entry segment invoked `onChanged(stalePending)` directly after `push`, without awaiting publication of the exact node, retaining the accepted owner Job, or observing a successful B owner save before B disposal. Validation 057 measured `A_after_onChanged_active=0` at that old boundary. That was an absence of the required regression assertion, not evidence of a production navigation defect. I used the source-backed absence record rather than adding and running a transient red assertion.

The retained schedule uses root ids `password-change-browser-root-A` and `password-change-browser-root-B`. Each production mount calls `WithPasswordChangeNavigationBinding` and `initNavigation`; generic persistence receives the real URL repository directly, while the singleton owner repository records root-id-attributed attempts and immutable successful holders. The post-apply scope signal now comes from `SideEffect`, after the real navigation initialization.

After the retained first A completion/Continue path, the test resets only A owner measurement records. It publishes the exact stale A Pending, awaits stack/root membership and the real Pending URL, holds A leaf work with the shared FIFO dispatcher, captures exactly one new composition child from actual `onChanged`, and requires an active transition with owner count one. The created-but-not-published A Completed replacement remains queued while Pending stays current; A attempts and successful holders remain zero.

The test mounts B from the same restored Pending URL, disposes A, cancels A leaf work before draining, and joins A transition, leaf, held-scope, and composition Jobs. The captured A transition and composition Jobs are cancelled and completed, with zero A saves or children. Stale A Changed and Continue callbacks leave B Pending and all owner measurements unchanged. B then repeats the held admission, completes the real replacement, awaits the delegated B Completed holder before disposal, and proves one non-cancelled B transition, one B attempt, one B holder, and zero remaining owner transitions while B remains active. The B snapshot is structurally compared with live `storeHierarchy`: exact B root id, scaffold/top/sidebar/main chains, UsersList→Completed, no successor, and no Pending. URL/history and credential-redaction assertions remain intact; model and HTTP counts remain one.

I rebuilt ast-index after the Kotlin edit. The rebuild reported 1,462 files and 115 modules. The required single foreground browser gate completed with the canonical test discovered in fresh XML. Fresh client browser XML contains five suites and 20 tests: one AdminPanel navigation test, three email-notification tests, thirteen password-interactor tests, the one canonical browser test, and two URL navigation tests. Every suite reports zero failures, zero errors, and zero skips. JSDOM remains the exercised DOM/URL host; graphical-browser, native-device, live-SMTP, and process-crash evidence remain outside this increment.

The three KDoc fillings remain assigned to the later LL Coding increment. No production, common fixture, README, prior-report, Operator Notes, dependency, or route file changed.

```text
ENTITY:
entity_id=issue_78_cycle5_coding_060; type=browser_rebinding_test_increment; state=browser_gate_executed

CONTEXT:
* task_id=09.09.2026_09.40.05-b512784f-672e-4309-aa90-4551cb0c3361; agent_id=issue78_coding_browser_rebind_cycle5; architecture_ref=059-architecturing.md; source_head=23c7c4f96615e7872839bc94c6446d935579665d
* constraints=[browser_test_and_060_only,no_nested_agents,no_push,no_checkout,no_production_edit,no_KDoc_fill,Operator_Notes_unchanged]; host=JSDOM; browser_gate=unfiltered

ACTION:
1. action=replace_rebinding_segment; target=PasswordChangeNavigationBrowserTest.canonicalApprovalReloadsAndCompletionPersistsWithoutCredential; params={root_ids:[password-change-browser-root-A,password-change-browser-root-B],mounts:[WithPasswordChangeNavigationBinding,initNavigation],generic_repo:urlRepo,owner_repo:persistenceRepo}
2. action=record_owner_persistence; target=persistenceRepo.save; params={attribution:ConfigHolder.Chain.id,attempts:[A,B],holders:[RecordingPasswordNavigationRepo_A,RecordingPasswordNavigationRepo_B],successful_B_signal:Completed_without_Pending}
3. action=retain_A_admission; target=mainA_and_scopeAJob; params={Pending_membership:identity,Pending_URL:/ui/password-change/7/123e4567-e89b-42d3-a456-426614174000,held_FIFO:separate_A,transition_capture:single_new_child,replacement_signal:factory_identity}
4. action=retain_A_cleanup; target=compositionA_and_heldScopeAJob; params={order:[dispose,cancel_leaf,cancel_held,drain,join_transition_leaf_held_composition],expected:{transition_cancelled:true,attempts:0,holders:0,children:0}}
5. action=retain_B_persistence; target=mainB_and_scopeBJob; params={restored_Pending:identity,held_FIFO:separate_B,transition_capture:single_new_child,expected:{attempts:1,holders:1,transition_cancelled:false,owner_jobs:0}}
6. action=compare_B_hierarchy; target=recorded_B_holder_and_rootB.storeHierarchy; params={root_id:password-change-browser-root-B,structure:[scaffold,top,sidebar,main,UsersList,Completed],forbidden:[Pending,approval_UUID,browser_password],URL:/ui/password-changed}

REASON:
* condition=entry_segment_called_A_onChanged_before_exact_Pending_publication; requirement=retained_accepted_A_Job_zero_save_and_B_successful_save_proof; causal_chain=publish_A→capture_A→bind_B→dispose_join_A→persist_B
* condition=Validation_057_observation_A_active_0; requirement=accurate_old_boundary_record; causal_chain=unawaited_callback→accepted_Job_absent→source_backed_absence_record→green_retained_schedule

EXPECTED RESULT:
* entity_id=issue_78_cycle5_coding_060; new_state=browser_rebinding_assertions_retained; location=client/src/jsTest/kotlin/PasswordChangeNavigationBrowserTest.kt
* entity_id=issue_78_cycle5_coding_060_report; new_state=tracked_commit_input; location=agents/task/09.09.2026_09.40.05-b512784f-672e-4309-aa90-4551cb0c3361/060-coding.md

VERIFICATION:
* check=ast_index_rebuild; expected={files:1462,modules:115}; result={files:1462,modules:115}; cache_access=approved
* check=browser_gate; expected={command:./gradlew --no-daemon :wishlist.client:jsBrowserTest --rerun-tasks --console=plain,canonical_discovery:1,failures:0,errors:0,skips:0}; result={suites:5,tests:20,failures:0,errors:0,skips:0}
* check=canonical_XML; expected={suite:jsBrowserTest.dev.inmo.wishlist.client.PasswordChangeNavigationBrowserTest,test:canonicalApprovalReloadsAndCompletionPersistsWithoutCredential}; result={tests:1,failures:0,errors:0,skips:0}

UNCERTAINTY:
* missing=[LL_three_KDoc_attachments,post_LL_aggregate,independent_full_Verification,independent_Validation]; ambiguity=V78_finding_closure_excluded_from_Coding_060
* missing=[graphical_browser_runtime,native_device_runtime,live_SMTP,process_crash_durability]; ambiguity=JSDOM_DOM_URL_host_limits

REPETITION OF RESULT:
* entity_id=issue_78_cycle5_coding_060; stored_in=shared_memory; status=available; A_result={transition_cancelled_completed:true,attempts:0,holders:0}; B_result={transition_completed_noncancelled:true,attempts:1,holders:1}

COMMUNICATION:
* sender=issue78_coding_browser_rebind_cycle5; receiver=orchestrator_and_next_Coding; task_id=09.09.2026_09.40.05-b512784f-672e-4309-aa90-4551cb0c3361; message_id=b4e7e340-0749-4c7d-9163-4e600acb9e08; protocol=AML-HIP

PERSISTENCE:
* local_memory=true; shared_memory=true; index_keys=[task_id,issue_78_cycle5_coding_060,V78-05,A_transition,B_holder]; persistence_medium=tracked_step_report_and_browser_test

VALIDATION:
* format_valid=true; no_pronouns=true; entities_explicit=true; high_density=true; causal_chain_present=true; ambiguity_detected=true
```
