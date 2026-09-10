# Coding 063: Canonical Browser-Test Compile and Runtime Repair

Model: Codex GPT-5 (ML Coding role)
Changed files: client/src/jsTest/kotlin/PasswordChangeNavigationBrowserTest.kt; agents/task/09.09.2026_09.40.05-b512784f-672e-4309-aa90-4551cb0c3361/063-coding.md

## Outcome

The canonical browser test now compiles and passes against the repository's actual Navigation and coroutines APIs. The patch is confined to `client/src/jsTest/kotlin/PasswordChangeNavigationBrowserTest.kt`; no production source, README, dependency, timeout, or prior report changed. The exact four-task relevant aggregate passed from the repaired source with fresh XML for all four requested tasks.

Step 062 deterministically disproved the client-JS success evidence reported by step 060 and therefore disproved any client-JS success evidence carried forward into step 061. Step 061 did not itself complete the client browser task: its accurate direct result was failure at `:wishlist.client:compileTestKotlinJs`, and its “pre-existing” qualification applied only relative to the comment-only step-061 delta. The step-061 documentation proof remains valid: the two users-plugin override KDocs and the canonical browser method KDoc remain immediately attached with their exact text, and step 061's comment-and-whitespace-stripped token comparison proves that its two Kotlin changes contained comments only.

Architecture 059 remains the canonical ledger. Its eight immutable source records are 050 `8d1ff33e-cb57-40b6-a227-5670476cb6cd`, 051 `4b1a2d2c-245c-4cb3-9db6-8d3aa41165ad`, 052 `c9e090da-b143-4ea9-9dd2-087e4b8e8e7d`, 053 `e34e71f4-4e29-4b53-b677-d71b2455c7c5`, 054 `7a6e29e3-0f13-4f43-9e9e-15b26f0b2d8a`, 055 `97a06661-e64f-4cf1-b83c-2436f7f32c33`, 056 `1d262989-fa60-4871-8097-3285ae2d8302`, and 057 `0c917c89-cee8-408a-8259-253b4cf5a1f0`. The ledger retains all twelve finding records with no Coding closure or count change: nine resolved findings and open V78-05 High at 4/4, V78-07 Medium at 4/4, and V78-11 Medium at 3/3. Step 062 UUID `3d3d26b4-3c3f-411b-a815-e17c4a120f98` remains the deterministic failed-compilation evidence, while step 063 UUID `a9f03af7-5bef-4fdb-9148-13b40e5947f9` supplies the forward repair and fresh aggregate evidence.

## API Evidence and Narrow Repair

Ast-index was used before direct source inspection. The installed Navigation 0.7.7 declaration places `findNodeInSubTree` in `dev.inmo.navigation.core.extensions`, so the test import now names that package. The kotlinx.coroutines 1.11.0 source declares `children` as a `Job` member property, so the unavailable `kotlinx.coroutines.children` import was removed without replacing it. The actual `NavigationChain` constructor orders `parentNode`, `nodeFactory`, and `id`; both roots now use explicit `parentNode = null`, their exact node factory, and their exact `NavigationChainId`.

Fresh execution exposed three deterministic runtime assumptions after compilation was repaired. First, composition B restored the Pending node before the URL saver published the canonical path; the test now waits through the existing bounded `awaitBrowserPhase` until the pathname is canonical and then retains the exact pathname assertion. Second, full root-B hierarchy serialization required the common `EmptyConfig` serializer in the test Koin graph; the test now registers a randomly qualified polymorphic `SerializersModule` for `ViewConfig` and `EmptyConfig`, matching the serializer lookup used by the test helper without changing production DI. Third, `ScaffoldViewConfig`, `TopBarViewConfig`, and `SidebarViewConfig` are ordinary serializable classes with identity equality; the test now proves the saved structure through explicit scaffold field types, a `WishlistsListViewConfig` with null `userId`, exact top/sidebar/main chain IDs, and exact top/sidebar config types rather than comparing freshly constructed objects by identity.

The final source retains Architecture 059's root-A/root-B schedule and assertions. Root A publishes the exact Pending node, captures the accepted active owner Job before replacement publication, holds the A leaf queue with the replacement created but absent from the live subtree, and records zero A save attempts and holders. Root B binds and restores its exact Pending node before A disposal. A disposal drains the held dispatcher and joins the owner transition, leaf, held scope, and composition scope; every A job completes or cancels as required, no A child remains, and A still has zero attempts and holders. Stale A Changed and Continue callbacks leave both counters and B's current node unchanged. B then creates and publishes its exact replacement, records exactly one save attempt and one successful holder while B's composition scope remains active, and keeps A at zero.

The saved root-B holder is serialized identically to the live root-B holder and has exact root ID B, an `EmptyConfig` root node, the scaffold and top/sidebar/main chains, and main `UsersListViewConfig` followed by the terminal `PasswordChangeViewConfig.Completed`. The Completed node has no successor or subchains, no Pending remains, serialized state and browser state contain neither approval UUID nor plaintext password, the URL is exactly `/ui/password-changed` with empty search and null history state, and the production model and MockEngine transport each record exactly one request. The original completed-reload and Continue path also retains one total model request and one total transport request.

## Serial Verification

No Gradle, Gradle daemon, or Java process for the worktree was present before any Gradle invocation. After the three requested API corrections, `./gradlew --no-daemon :wishlist.client:compileTestKotlinJs --rerun-tasks --console=plain` passed in 37 seconds with 166 of 166 actionable tasks executed.

The first exact aggregate compiled the browser test and then failed the canonical case because the B Pending node preceded asynchronous URL publication: expected `/ui/password-change/7/<approval>` and observed `/ui`. After the bounded B-URL publication wait, the minimal compilation gate passed again in 37 seconds with 166 executed tasks. The next exact aggregate reached serialization and failed with `SerializationException: Serializer for subclass 'EmptyConfig' is not found in the polymorphic scope of 'ViewConfig'`. After test-only serializer registration, the minimal compilation gate passed in 36 seconds with 166 executed tasks. The next exact aggregate reached saved-structure inspection and failed at identity-based whole-object `ScaffoldViewConfig` equality with `Expected <[object Object]>, actual <[object Object]>`. The structural field/type assertions repaired that last direct failure. Every invocation ran serially in the foreground without overlap.

The final required command was `./gradlew --no-daemon :wishlist.client:jvmTest :wishlist.client:jsBrowserTest :wishlist.features.ui.users:jvmTest :wishlist.features.ui.users:jsBrowserTest --rerun-tasks --console=plain`. It passed in 1 minute 6 seconds with 480 of 480 actionable tasks executed. Only XML produced by that run was counted.

Fresh client JVM XML contains three suites and 16 tests: `AdminPanelNavigationTest` 1, `PasswordChangeHtmlPolicyTest` 2, and `PasswordChangeInteractorTest` 13. Fresh client JS XML contains five suites and 20 tests: `AdminPanelNavigationTest` 1, `EmailApprovalNotificationTest` 3, `PasswordChangeInteractorTest` 13, `PasswordChangeNavigationBrowserTest` 1, and `PasswordChangeNavigationTest` 2. Fresh UI/users JVM XML contains seven suites and 43 tests: `PasswordChangeSerializationTest` 1, `UsersModelTest` 1, `PasswordChangeViewModelTest` 8, `PasswordChangeViewTest` 2, `UserEditViewModelEmailTest` 18, `UserEditViewModelPasswordChangeTest` 8, and `UserEditViewModelSaveTest` 5. Fresh UI/users JS XML contains eight suites and 50 tests: `PasswordChangeSerializationTest` 1, `UsersModelTest` 1, `PasswordChangeViewBrowserTest` 2, `PasswordChangeViewModelTest` 8, `UserEditViewBrowserTest` 7, `UserEditViewModelEmailTest` 18, `UserEditViewModelPasswordChangeTest` 8, and `UserEditViewModelSaveTest` 5. The total is 23 suites and 129 tests, with zero failures, zero errors, and zero skips.

Fresh named discovery includes the canonical `canonicalApprovalReloadsAndCompletionPersistsWithoutCredential[js, browser]` case. Both client targets discover all six V78-12 owner cases: `detachedPendingAtEntryStartsNoTransition`, `detachedWhileQueuedStopsWithoutLeafEmission`, `queuedNodeSupersessionNeverSavesCompleted`, `replacementSupersededBeforeSaveIsIgnored`, `newerDestinationAboveCompletedIsPreservedInSavedHierarchy`, and `oldBindingCleanupCannotClearNewBinding`. Both UI/users targets discover all eight `PasswordChangeViewModelTest` cases and all eight `UserEditViewModelPasswordChangeTest` cases. The canonical browser case exercises the actual ViewModel, exact POST to `https://wishlist.test/api/auth/completePasswordChange`, one model request, and one MockEngine request.

Fresh actual-lifecycle and transport discovery also includes client cases `actualSubmittingViewModelDestructionPrecedesCompletedPersistence`, `cancelledSubmittingViewModelCannotStartChangedHandoff`, `saveFailureDoesNotReplayAndLaterContinuePersistsUsersList`, `rootBindingDisposalCancelsPendingTransitionAndCleansOwnerJobs`, `unprocessedReplacementTimesOutAndCleansOwnerJobs`, `stalePendingCannotReplaceNewerDestination`, and `changedPendingReplacesCredentialRouteAndContinueAlwaysReachesUsersList`. The eight password-change ViewModel cases are `synchronousSubmissionGuardRejectsStaleMismatchAndDuplicateClick`, `immediateAdmissionAndLocalValidationDoNotTrustDerivedSubmitState`, `completedConfigIsCredentialFreeAndNonActionable`, `invalidApprovalPreventsDuplicateSubmission`, `immediateDoubleSubmitAdmitsOneRequest`, `matchingSubmissionUsesExactApprovalAndClearsSensitiveFields`, `invalidLocalInputNeverSubmitsApproval`, and `destructionClearsInMemoryPasswordInputs`. The eight owner password-email ViewModel cases are `stalePasswordChangeCompletionCannotPublishOrClearLaterMutation`, `unknownLoadingAndRefreshKeepPasswordRequestIneligible`, `busyPasswordChangeRequestAcceptsOnlyOneClick`, `staleTrueCannotAuthorizePasswordEmail`, `rawAdmissionRejectsUnauthorizedMissingUnapprovedMismatchedAndUnavailableStates`, `staleFalseDoesNotBlockValidRawAdmission`, `passwordChangeFeedbackIsDistinctAndEligibleRetryReconcilesProfile`, and `ownerAndRootOnSelfIssueOnlyDisplayedApprovedEmailWithoutOtherMutations`.

Fresh owner-DOM discovery includes the positive busy/delivery-failure case `approvedOwnerRequestIsVisibleDisabledWhileBusyAndReportsDeliveryFailure` and the six negative or stale-state cases `disabledSmtpHidesPasswordChangeRequest`, `missingEmailHidesPasswordChangeRequest`, `unapprovedEmailHidesPasswordChangeRequest`, `anotherUserHidesPrivatePasswordChangeControls`, `rootOnOtherUserHidesPrivatePasswordChangeControls`, and `heldRefreshRemovesCurrentPasswordChangeRequest`. Fresh form discovery includes JVM cases `completedFactoryDrawsCredentialFreeContentWithoutPasswordInputs` and `pendingFactoryDrawsProtectedFormAndAdmitsOneCorrectedImeSubmission`, plus browser cases `pendingFormUsesPasswordInputsAndNativeSubmitOnlyOnce` and `completedFormContainsNoPasswordInputs`.

Ast-index was rebuilt after each Kotlin repair. The final rebuild indexed 1,462 files, 115 modules, zero direct dependencies, zero transitive dependencies, one XML usage, and four resources with zero resource usages. All five Operator Notes sections remain byte-identical to local `master`; each boundary is 150 bytes with SHA-256 `67bacb0f982e1f2417f3e2f20567deef209f85d4fa2412643362502edffac4a8`. `git diff --check` and the final changed-file scope audit passed. Independent Verification and Validation retain authority over the three open findings.

```text
ENTITY:
entity_id=issue_78_cycle6_coding_063; type=canonical_browser_test_compile_runtime_repair; state=green_relevant_aggregate

CONTEXT:
* task_id=09.09.2026_09.40.05-b512784f-672e-4309-aa90-4551cb0c3361; agent_id=issue78_coding_compile_repair_cycle6; memory_ref=[059-architecturing.md,060-coding.md,061-coding.md,062-coding.md]; entry_head=eb621d99335d2a5973a380421adf064e93e63fa9
* constraints=[single_test_source,no_production_edit,no_README_edit,no_prior_report_edit,no_push,serial_foreground_Gradle,root_A_root_B_behavior_preserved,three_KDocs_preserved]; scope=client_JS_canonical_browser_test
* source_records={059:8cf3bd86-5a19-4e95-a8b4-dde1f1400526,060:b4e7e340-0749-4c7d-9163-4e600acb9e08,061:4402081d-c399-4801-8640-9006cbbd3fd7,062:3d3d26b4-3c3f-411b-a815-e17c4a120f98,063:a9f03af7-5bef-4fdb-9148-13b40e5947f9}; canonical_ledger={source_records_050_057:8,finding_records:12,open:3,resolved:9,count_changes:0}

ACTION:
1. action=repair_compile_API_usage; target=PasswordChangeNavigationBrowserTest.kt; params={findNodeInSubTree_package:dev.inmo.navigation.core.extensions,Job_children:member_property,children_import:removed,root_A_constructor:[parentNode_null,nodeFactory_nodesFactoryA,id_rootAId],root_B_constructor:[parentNode_null,nodeFactory_nodesFactoryB,id_rootBId]}
2. action=repair_URL_publication_race; target=composition_B_pending_restore; params={wait=awaitBrowserPhase,dispatcher=Dispatchers_Default,condition=exact_canonical_path,exact_assertion=retained,bound=existing_5000_ms}
3. action=repair_test_serialization_graph; target=canonical_test_Koin_module; params={module=SerializersModule,base=ViewConfig,subclass=EmptyConfig,qualifier=random,production_DI_changes:0}
4. action=repair_saved_structure_assertion; target=root_B_scaffold_hierarchy; params={identity_equality:removed,scaffold_fields:[TopBarViewConfig,SidebarViewConfig,WishlistsListViewConfig_userId_null],chain_ids:[TopNavigationChainId,LeftNavigationChainId,MainNavigationChainId],terminal=UsersList_to_Completed}
5. action=run_serial_gates; target=[client_compileTestKotlinJs,relevant_four_task_aggregate]; params={minimal_passes:3,intermediate_aggregate_failures:[B_URL_publication,EmptyConfig_serializer,ScaffoldViewConfig_identity_equality],final_aggregate_exit:0,final_tasks:480,overlap:false}
6. action=inspect_fresh_XML; target=[client_jvmTest,client_jsBrowserTest,UI_users_jvmTest,UI_users_jsBrowserTest]; params={suites:[3,5,7,8],tests:[16,20,43,50],total_suites:23,total_tests:129,failures:0,errors:0,skips:0,canonical_discovery:1}
7. action=preserve_documentation_and_ledger; target=[three_KDocs,Architecture_059,Operator_Notes]; params={KDocs:3,source_records_050_057:8,finding_records:12,Operator_Notes_sections:5,Operator_Notes_bytes_each:150,Operator_Notes_sha256:67bacb0f982e1f2417f3e2f20567deef209f85d4fa2412643362502edffac4a8}
8. action=correct_forward_evidence; target=[060-coding.md,061-coding.md,062-coding.md,063-coding.md]; params={060_client_JS_success:disproved_by_062,061_carried_JS_success:disproved_by_062,061_direct_compile_failure:retained,061_comment_only_proof:retained,062_compile_failure:retained,063_repair_and_fresh_aggregate:green}

REASON:
* condition=committed_canonical_test_failed_actual_API_compilation; requirement=repository_API_compatible_browser_test; causal_chain=ast_index_lookup→direct_declaration_confirmation→three_compile_corrections→compile_gate_PASS
* condition=fresh_runtime_exposed_three_test_assumptions; requirement=deterministic_canonical_browser_proof; causal_chain=B_URL_wait→EmptyConfig_serializer_registration→structural_scaffold_assertions→aggregate_PASS
* condition=Architecture_059_requires_exact_A_B_lifecycle_and_secret_free_hierarchy; requirement=assertion_preservation; causal_chain=root_A_accepted_Job_capture→root_B_bind_before_A_disposal→A_join_and_zero_save→B_exact_save→saved_live_hierarchy_equality

EXPECTED RESULT:
* entity_id=issue_78_cycle6_coding_063; new_state=canonical_browser_test_green_with_fresh_aggregate; location=agents/task/09.09.2026_09.40.05-b512784f-672e-4309-aa90-4551cb0c3361/063-coding.md
* entity_id=canonical_browser_test; new_state=actual_API_compatible_and_runtime_green; location=client/src/jsTest/kotlin/PasswordChangeNavigationBrowserTest.kt; production_delta=0

VERIFICATION:
* check=minimal_compile_gate; expected={exit:0,task:client_compileTestKotlinJs}; result={passes:3,latest_tasks:166,latest_executed:166}; command=gradlew_no_daemon_compileTestKotlinJs_rerun_tasks_plain
* check=final_relevant_aggregate; expected={exit:0,fresh_XML:true}; result={exit:0,tasks:480,suites:23,tests:129,failures:0,errors:0,skips:0}; duration=66_seconds
* check=canonical_root_A_root_B_source; expected={A_attempts:0,A_holders:0,A_jobs_joined:true,stale_A_callbacks_noop:true,B_attempts:1,B_holders:1,B_scope_active_during_save:true}; result=matched
* check=saved_live_hierarchy; expected={root_id:B,root_config:EmptyConfig,chains:[top,sidebar,main],main:[UsersList,Completed],Pending:false,successor:false,UUID:false,plaintext:false}; result=matched
* check=named_discovery; expected={canonical:1,V78_12_owner_cases:6,owner_DOM_cases:7,password_forms:4}; result={canonical:1,V78_12_owner_cases:6,owner_DOM_cases:7,password_forms:4}
* check=ast_index_and_scope; expected={files:1462,modules:115,changed_source_files:1,production_files:0}; result={files:1462,modules:115,changed_source_files:1,production_files:0,git_diff_check:PASS}
* check=Operator_Notes; expected={sections:5,bytes_each:150,master_equal:true}; result={sections:5,bytes_each:150,master_equal:true,sha256:67bacb0f982e1f2417f3e2f20567deef209f85d4fa2412643362502edffac4a8}

UNCERTAINTY:
* missing=[independent_Verification,independent_Validation,graphical_browser_runtime,native_device_runtime,live_SMTP,process_crash_durability]; ambiguity=JSDOM_and_in_process_hosts_exclude_external_runtime_guarantees
* missing=independent_finding_closure; ambiguity=Coding_role_cannot_change_V78_05_V78_07_V78_11_lifecycle

REPETITION OF RESULT:
* entity_id=issue_78_cycle6_coding_063; stored_in=shared_memory; status=available; aggregate={exit:0,suites:23,tests:129,failures:0,errors:0,skips:0}; next_stage=independent_Verification

COMMUNICATION:
* sender=issue78_coding_compile_repair_cycle6; receiver=orchestrator_and_next_Verification; task_id=09.09.2026_09.40.05-b512784f-672e-4309-aa90-4551cb0c3361; message_id=a9f03af7-5bef-4fdb-9148-13b40e5947f9; protocol=AML-HIP; verdict=PASS

PERSISTENCE:
* local_memory=true; shared_memory=true; index_keys=[task_id,issue_78_cycle6_coding_063,canonical_browser_test,root_A,root_B,fresh_aggregate]; persistence_medium=tracked_step_report_and_test_source; personal_auto_memory=disabled

VALIDATION:
* format_valid=true; no_pronouns=true; entities_explicit=true; high_density=true; causal_chain_present=true; ambiguity_detected=true
```
