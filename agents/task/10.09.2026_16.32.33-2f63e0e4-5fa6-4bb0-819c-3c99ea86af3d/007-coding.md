Model: GPT-5 (HL lock diagnosis and aggregate verification)
Changed files: features/ui/users/src/commonTest/kotlin/ui/UserEditViewModelEmailTest.kt, agents/task/10.09.2026_16.32.33-2f63e0e4-5fa6-4bb0-819c-3c99ea86af3d/007-coding.md

# Coding Stage 5: lock diagnosis and final aggregate

Stage 5 found no issue-79 dependency, build, package, or lock declaration change against base `a2dc220`. A path-filtered diff for Gradle files, settings, version catalogs, package manifests, and lockfiles is empty. The failing store file is not repository data: `git check-ignore -v kotlin-js-store/yarn.lock` resolves `.gitignore:16:kotlin-js-store/`, and Git does not track the path. The initial ignored file was 143,176 bytes with mtime `2026-09-09 21:28:21.356898250 +0600` and SHA-256 `d1f557c3d35f8c12768ed3ab3444942956974e145d67e70946836e26a41e9a43`. A fresh `:kotlinStoreYarnLock --rerun-tasks` reproduced `Lock file was changed. Run the kotlinUpgradeYarnLock task to actualize lock file`; its generated `build/js/yarn.lock` was 2,882 lines with SHA-256 `6a4fc52389ddb0209d9ea4065b5e9bfba7754a1343dd213ac6c25a778770723d`, while the stale store was 3,108 lines and included `jsdom@26.1.0` plus its CSS, URL, and proxy transitives.

The cross-branch cause is directly attributable. Branch `fix/issue-78-email-authorized-password-change` adds `devNpm("jsdom", "26.1.0")` in both `client/build.gradle` and `features/ui/users/build.gradle`; issue #79 contains neither declaration nor another `jsdom` reference. After a zero-process preflight, `./gradlew --no-parallel clean --console=plain` succeeded in 14 seconds with 167 actionable tasks, 111 executed and 56 up-to-date. Git status was unchanged, so clean removed generated build outputs without removing tracked or user files, but correctly left the ignored store in place. The stale file was then moved recoverably, not deleted, to `/tmp/issue79-stale-yarn.lock`; source absence, matching backup metadata/hash, and unchanged Git status were verified before Gradle regenerated branch-current state. The regenerated ignored store is 132,521 bytes with mtime `2026-09-10 18:39:03.025013302 +0600` and SHA-256 `6a4fc52389ddb0209d9ea4065b5e9bfba7754a1343dd213ac6c25a778770723d`; `jsdom` and the identified transitives are absent. `kotlinUpgradeYarnLock` was never run, and no tracked lockfile was edited.

The first aggregate after quarantine exposed one real test-portability defect: the JS variant of `nodeDestroyCancelsEmailWork` timed out in a `yield` polling loop while waiting for dependency-owned superclass cancellation. The navigation dependency keeps its lifecycle `MutableSharedFlow` private and exposes only a final read-only `Flow`, so shared tests cannot prove subscriber readiness through `subscriptionCount`; dispatcher turns also cannot provide a deterministic cross-platform barrier. The smallest correction remains test-only and renames the case to `nodeDestroySuppressesNonCooperativeEmailContinuation`. The case now asserts the real node reached `NavigationNodeState.NEW`, releases the deliberately `NonCancellable` PUT, drives the injected `TestCoroutineScheduler` with `advanceUntilIdle`, and retains every acceptance assertion: no successor GET, no POST, no saved marker, no verification result, and no navigation success. This tests issue-79's raw-node-state, generation, token, and stale-continuation guards without asserting nondeterministic timing internal to the navigation dependency. Focused JVM and JS Node executions of the renamed case each passed; no production source changed in Stage 5.

The final command was `./gradlew --no-parallel --rerun-tasks --console=plain :wishlist.features.ui.users:jvmTest :wishlist.features.ui.users:jsNodeTest :wishlist.features.ui.users:testDebugUnitTest :wishlist.features.ui.users:compileKotlinJs :wishlist.features.ui.users:compileKotlinJvm :wishlist.features.ui.users:compileDebugKotlinAndroid :wishlist.features.email.client:jvmTest :wishlist.features.email.client:jsNodeTest :wishlist.features.email.server:jvmTest :wishlist.features.users.common:jvmTest`. It completed successfully in 1 minute 2 seconds with 556 actionable tasks, all 556 executed. Fresh XML reports UI users JVM 54 tests, JS Node 54 tests, and Android debug unit 54 tests; each platform has 48 `UserEditViewModelEmailTest` cases, one `UsersModelTest` case, and five `UserEditViewModelSaveTest` cases. Email client JVM and JS Node each report 6 tests. Email server JVM reports 81 tests. Users common JVM reports 18 tests. The aggregate test total is 273 with 0 failures, 0 errors, and 0 skipped.

Named fresh coverage includes `approvedOwnerEmailCanBeReplacedAndVerified`, `pendingOwnerEmailCanBeReplacedAndVerified`, `disabledSmtpOwnerCanReplaceApprovedEmailWithoutSending`, `resendUsesSavedEmailAndPreservesReplacementDraft`, `anonymousNonOwnerAndRootOtherCannotAccessPrivateEmail`, `oldFinallyCannotClearNewBusy`, and `nodeDestroySuppressesNonCooperativeEmailContinuation` in the 48-case shared matrix. `setMyEmailPutsOnlyEmailAndReturnsStatusSuccess` covers exact JSON body shape and OK, No Content, Conflict/409, and server-error Boolean outcomes on both client platforms; `setMyEmailNetworkFailurePropagates` covers transport failure. The eight route cases include `setEmailUsesBearerCallerDespiteForgedAccountSelectors`, missing/invalid bearer rejection, malformed/invalid body rejection, Boolean status mapping, and duplicate-to-conflict mapping. The twelve repository cases include `emailApprovalTracksOnlyTheCurrentStoredAddress`, `bulkUpdatesPreserveOrResetCurrentAddressApproval`, `failedBulkUpdateLeavesRowsUnchanged`, `conditionalApprovalEmitsOnlySuccessfulUpdates`, and duplicate-email exception mapping. Related unchanged baselines are also green, including the 15-case email feature service, 8-case verification coordinator, 8-case verification deep-link handler, 4-case email deep-link integration, and all remaining email-server suites.

The source audit confirms owner-only private state requires authorization, caller identity, the bound user ID, and the live node target. Root editing another account does not bypass that boundary. Both pending and approved saved addresses can be replaced; SMTP `Disabled` permits storage while suppressing delivery. Enabled saving follows PUT, checked matching private GET, exact-recipient POST when pending, and final checked private GET. False, 409, and transport-uncertain persistence preserve the draft, use generic unconfirmed-storage feedback, never post automatically, and never publish false success. Resend reads the saved pending address rather than the draft. Owner generation, refresh version, mutation identity, cancellation checks, raw target checks, and matching-finally ownership suppress stale work. All three renderers keep the authoritative saved address/status separate from the editable draft and apply the owner/live-target guard; JVM and Android route Done IME to guarded `onSaveEmail`, while the existing JS Calm form intentionally has no implicit Enter submission. English/Russian strings and relevant KDocs match the architecture, and the merged PR #80 `Подтверждение email` subtitle remains absent.

The users README `## Operator Notes` section is byte-identical to `cc8b63934086a41fb4c5965ce30b68775a67b67e`; both extracted sections hash to `3b29bbd30c965e1eb672e0753bc4432b7f41b3b26dd469a1343e5283e716a46c`. `git diff --check`, dependency/lock scope, renderer, locale, KDoc, subtitle, and issue-source-scope audits pass. The final AST index was rebuilt after the shared Kotlin test edit and reports 785 files, 6,570 symbols, 26,866 references, 49 modules, one XML usage, and four resources. The prior Stage-4 count was audited before the edit; the final count reflects the removed `Job`/`yield` references and renamed test symbol.

No real users renderer/DOM/device host exists in the repository, so the aggregate proves shared business behavior and JS/JVM/Android compilation but does not claim DOM rendering, native widget interaction, Android device execution, Done IME execution, live SMTP, or live server behavior. The exact superclass lifecycle-cancellation timing remains dependency-owned; issue-79 stale-continuation suppression is covered after a real destruction transition. These are accepted evidence limits, not compiler or business-test blockers.

```text
ENTITY:
entity_id=issue_79_stage5_aggregate; type=lock_diagnosis_and_acceptance_aggregate; state=verified_green_with_explicit_host_limits

CONTEXT:
* task_id=10.09.2026_16.32.33-2f63e0e4-5fa6-4bb0-819c-3c99ea86af3d; agent_id=issue79_coding_aggregate_cycle1; memory_ref=[PROMPT.md,001-planning.md,002-architecturing.md,003-coding.md,004-coding.md,005-coding.md,006-coding.md,007-coding.md]
* constraints=[serial_Gradle,tracked_lock_immutability,issue_79_scope,Operator_Notes_byte_preservation,AML_HIP]; exclusions=[kotlinUpgradeYarnLock,new_renderer_harness,live_SMTP,issue_78_dependencies]

ACTION:
1. action=diagnose_lock_mismatch; target=issue_79_stage5_aggregate; params={base:a2dc220,stale_sha256:d1f557c3d35f8c12768ed3ab3444942956974e145d67e70946836e26a41e9a43,current_sha256:6a4fc52389ddb0209d9ea4065b5e9bfba7754a1343dd213ac6c25a778770723d,root_cause:ignored_issue_78_generated_state}
2. action=quarantine_and_regenerate_ignored_store; target=issue_79_stage5_aggregate; params={backup:/tmp/issue79-stale-yarn.lock,clean_used=true,tracked_status_unchanged=true,upgrade_task_run=false}
3. action=correct_portable_shared_test; target=issue_79_stage5_aggregate; params={production_edits:0,test_case:nodeDestroySuppressesNonCooperativeEmailContinuation,raw_state:NEW,noncooperative_continuation_suppressed=true}
4. action=run_final_aggregate; target=issue_79_stage5_aggregate; params={requested_targets:10,actionable_tasks:556,test_cases:273,failures:0,errors:0,skipped:0}
5. action=audit_source_and_docs; target=issue_79_stage5_aggregate; params={privacy_and_sequence_pass=true,renderer_and_locale_pass=true,KDoc_and_README_pass=true,dependency_diff_empty=true}

REASON:
* condition=ignored_cross_branch_store_contained_issue_78_jsdom; requirement=preserve_tracked_locks_and_regenerate_branch_current_state; causal_chain=dependency_diff_audit→recoverable_quarantine→store_guard_success
* condition=dependency_lifecycle_flow_private_and_final; requirement=shared_test_must_avoid_unprovable_cancellation_timing; causal_chain=assert_raw_NEW→release_noncooperative_PUT→drive_test_scheduler→prove_zero_stale_effects
* condition=issue_79_acceptance_requires_cross_platform_business_evidence; requirement=all_requested_test_and_compile_targets_green; causal_chain=serial_rerun_tasks→fresh_XML_inspection→aggregate_acceptance

EXPECTED RESULT:
* entity_id=issue_79_stage5_aggregate; new_state=issue_79_implementation_ready_for_independent_validation; location=task_report_and_committed_test_correction

VERIFICATION:
* check=final_Gradle_aggregate; expected=10_targets_green_and_0_failures; observed=556_tasks_executed_and_273_tests_green
* check=fresh_UI_XML; expected=48_email_cases_per_platform_plus_6_baselines; observed=54_JVM_plus_54_JS_Node_plus_54_Android_all_green
* check=transport_route_repository_XML; expected=client_409_body_plus_route_caller_plus_repository_reset_duplicate_coverage; observed=12_client_plus_81_server_plus_18_users_common_all_green
* check=regenerated_yarn_store; expected=branch_current_without_jsdom; observed=sha256_6a4fc52389ddb0209d9ea4065b5e9bfba7754a1343dd213ac6c25a778770723d_and_jsdom_absent
* check=ast_and_static_audits; expected=current_index_and_all_scope_checks_green; observed=785_files_6570_symbols_26866_refs_49_modules_and_all_audits_green

UNCERTAINTY:
* missing=real_renderer_DOM_device_host; ambiguity=widget_and_IME_execution_not_observed; accepted_scope=shared_business_tests_plus_three_platform_compilation
* missing=deterministic_dependency_lifecycle_subscription_barrier; ambiguity=superclass_cancellation_timing_not_cross_platform_observable; accepted_scope=raw_NEW_and_stale_continuation_suppression
* missing=live_SMTP_and_live_server_execution; ambiguity=external_delivery_and_deployment_not_observed; accepted_scope=mock_transport_and_service_integration_suites

REPETITION OF RESULT:
* entity_id=issue_79_stage5_aggregate; stored_in=shared_memory; status=available; result=aggregate_green_lock_state_regenerated_and_issue_79_acceptance_ready

COMMUNICATION:
* sender=issue79_coding_aggregate_cycle1; receiver=orchestrator; task_id=10.09.2026_16.32.33-2f63e0e4-5fa6-4bb0-819c-3c99ea86af3d; message_id=d380b133-8799-48aa-a93b-2ae834c8aa50; protocol=AML-HIP

PERSISTENCE:
* local_memory=true; shared_memory=true; index_keys=[task_id,issue_79_stage5_aggregate,lock_root_cause,aggregate_counts,accepted_limits]; storage=007-coding.md_and_git_commit

VALIDATION:
* format_valid=true; no_pronouns=true; entities_explicit=true; high_density=true; causal_chain_present=true; ambiguity_detected=true; ambiguity_resolution=renderer_host_and_dependency_lifecycle_limits_explicit
```
