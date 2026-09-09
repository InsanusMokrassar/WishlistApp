Model: Codex GPT-5.6 coding agent.
Changed files: client/src/commonMain/kotlin/ClientPlugin.kt; client/src/commonTest/kotlin/PasswordChangeInteractorTest.kt; client/src/jsMain/kotlin/UrlNavigationConfigsRepo.kt; client/src/jsTest/kotlin/PasswordChangeNavigationBrowserTest.kt; client/src/jsTest/kotlin/PasswordChangeNavigationTest.kt; client/src/jvmTest/kotlin/PasswordChangeHtmlPolicyTest.kt; features/auth/README.md; features/auth/client/src/commonMain/kotlin/KtorPasswordChangeFeature.kt; features/auth/client/src/commonMain/kotlin/Plugin.kt; features/auth/client/src/commonMain/kotlin/configurators/DefaultUrlHttpClientConfigurator.kt; features/auth/client/src/jsMain/kotlin/JSPlugin.kt; features/auth/common/src/commonMain/kotlin/Constants.kt; features/auth/common/src/commonTest/kotlin/models/PasswordChangeContractTest.kt; features/auth/server/src/commonMain/kotlin/Plugin.kt; features/auth/server/src/commonMain/kotlin/configurators/PasswordChangeRoutingsConfigurator.kt; features/auth/server/src/commonTest/kotlin/configurators/PasswordChangeRoutingsConfiguratorTest.kt; features/common/README.md; features/common/server/src/jvmMain/kotlin/JVMPlugin.kt; features/deeplinks/server/src/commonMain/kotlin/configurators/DeepLinksRoutingConfigurator.kt; features/deeplinks/server/src/commonTest/kotlin/configurators/DeepLinksRoutingConfiguratorTest.kt; features/deeplinks/server/src/commonTest/kotlin/services/DeepLinksServiceTest.kt; features/email/server/src/commonMain/kotlin/Plugin.kt; features/email/server/src/commonMain/kotlin/services/EmailPasswordChangeService.kt; features/email/server/src/commonTest/kotlin/PasswordChangeFlowRoutingTest.kt; features/email/server/src/commonTest/kotlin/PasswordChangePluginTest.kt; features/email/server/src/commonTest/kotlin/services/EmailPasswordChangeCommitTest.kt; features/email/server/src/commonTest/kotlin/services/EmailPasswordChangeIssuanceTest.kt; features/email/server/src/commonTest/kotlin/services/EmailPasswordChangeServiceTest.kt; features/email/server/src/commonTest/kotlin/services/FakeRolesRepo.kt; features/ui/users/README.md; features/ui/users/src/androidMain/kotlin/AndroidPlugin.kt; features/ui/users/src/androidMain/kotlin/ui/PasswordChangeView.kt; features/ui/users/src/androidUnitTest/kotlin/ui/PasswordChangeViewTest.kt; features/ui/users/src/commonMain/kotlin/Plugin.kt; features/ui/users/src/commonMain/kotlin/ui/PasswordChangeViewModel.kt; features/ui/users/src/commonMain/kotlin/ui/UserEditViewModel.kt; features/ui/users/src/commonTest/kotlin/PasswordChangeSerializationTest.kt; features/ui/users/src/commonTest/kotlin/UsersModelTest.kt; features/ui/users/src/commonTest/kotlin/ui/PasswordChangeViewModelTest.kt; features/ui/users/src/commonTest/kotlin/ui/UserEditTestFixtures.kt; features/ui/users/src/commonTest/kotlin/ui/UserEditViewModelPasswordChangeTest.kt; features/ui/users/src/commonTest/kotlin/ui/UserEditViewModelSaveTest.kt; features/ui/users/src/jsMain/kotlin/JSPlugin.kt; features/ui/users/src/jsMain/kotlin/ui/PasswordChangeView.kt; features/ui/users/src/jsTest/kotlin/ui/BrowserViewTestFixture.kt; features/ui/users/src/jsTest/kotlin/ui/PasswordChangeViewBrowserTest.kt; features/ui/users/src/jsTest/kotlin/ui/UserEditViewBrowserTest.kt; features/ui/users/src/jvmMain/kotlin/JVMPlugin.kt; features/ui/users/src/jvmMain/kotlin/ui/PasswordChangeView.kt; features/ui/users/src/jvmTest/kotlin/ui/PasswordChangeViewTest.kt; agents/task/09.09.2026_09.40.05-b512784f-672e-4309-aa90-4551cb0c3361/040-coding.md.

V78-07 is closed with a finite declaration audit. I rebuilt ast-index and used `ast-index changed --base master` plus outlines of the bounded issue-78 files. The audit covered the five PasswordChangeViewModel backing states and lifecycle handoff, PasswordChangeNavigationOwner binding/job fields and cleanup, KtorPasswordChangeFeature, PasswordChangeRoutingsConfigurator, EmailPasswordChangeService, Auth/Common/Email/UI-users/deeplink/URL declarations, all three PasswordChangeView implementations, all UI/users platform factories and plugin setup/start methods, and issue-created test classes, methods, helpers, fixtures, recordings, overrides, and fake repositories. Existing purpose-bearing KDocs were retained; missing class, function, property, override, constructor-parameter, and test declaration KDocs were added. Local function temporaries and unrelated pre-existing navigation overrides in modified legacy files were excluded from the bounded issue declaration set.

V78-08 is closed. UI/users, Common, and Auth READMEs now describe the final renderer split, root-owned transition lifetime, Pending field/HTTP ownership, exact-node stale guards, Completed/UsersList persistence, cancellation and durability limits, transport classification, and unchanged sessions/roles. Email and DeepLinks READMEs were accuracy-checked against final code and required no text changes. Operator Notes were compared byte-for-byte against `master`; all five sections matched SHA-256 `67bacb0f982e1f2417f3e2f20567deef209f85d4fa2412643362502edffac4a8`.

V78-11 was audited forward across structured blocks 032–040. Steps 012–030 were not edited. A programmatic check verified all eleven required AML-HIP headings and a parseable UUID in every block, including the new block below.

Validation used `git diff --check` (passed), `ast-index rebuild` (passed; 1,460 files, 115 modules), and the affected JVM Gradle aggregate (passed: `BUILD SUCCESSFUL`, 214 actionable tasks, 95 executed, 119 up-to-date). The aggregate produced passing JVM production/test compilation and tests. Focused non-JVM validation compiled UI/users JS and Android production/unit-test sources and ran UI/users JS Node/browser tests successfully; client JS Node tests passed 13 tests, while client JS browser tests passed 13 of 14 tests and one browser-only reload test timed out at 240 seconds in `client/build/test-results/jsBrowserTest/TEST-jsBrowserTest.dev.inmo.wishlist.client.PasswordChangeNavigationBrowserTest.xml`. No test or production behavior was changed by this documentation-only cycle; the timeout is recorded as an environment/test-run limitation.

```text
ENTITY:
entity_id=issue_78_coding_040; type=coding_documentation_step; state=implemented_and_validated

CONTEXT:

* task_id=09.09.2026_09.40.05-b512784f-672e-4309-aa90-4551cb0c3361; agent_id=issue78_coding_docs_cycle3; memory_ref=[031-validation,032-planning,033-architecturing,034-039-coding]
* constraints=[steps_012_to_030_immutable,scope=issue_78_declarations_and_feature_READMEs,behavior_unchanged,stable_serial_names,push=false]

ACTION:

1. action=audit_declarations; target=V78-07; params={base=master,head=a6033aa297b279e5d939e9f6b28b5a6037075c0a,method=[git_diff,ast_index_changed,ast_index_outline],bounded_inventory=architecture_033_lines_101_plus}
2. action=add_purpose_kdocs; target=issue_78_kotlin_declarations; params={classes=platform_views_plugins_fixtures,functions=routes_services_tests,properties=backing_states_bindings_jobs,params=constructors_and_overrides}
3. action=update_readmes; target=V78-08; params={files=[features/ui/users/README.md,features/common/README.md,features/auth/README.md],accuracy_checked=[features/email/README.md,features/deeplinks/README.md],operator_notes=byte_equal}
4. action=audit_structured_reports; target=V78-11; params={range=032..040,required_headings=11,uuid_format=parseable_uuid,prior_steps=immutable}
5. action=compile_and_test; target=affected_source_sets; params={jvm=passed,ui_users_js_node=passed,ui_users_js_browser=passed,android_compile=passed,client_js_node=passed,client_js_browser=one_timeout}

REASON:

* condition=missing_purpose_kdocs_or_stale_feature_contracts; requirement=architecture_033_authority_and_validation_031_acceptance
* condition=Pending_replacement_outlives_child_viewmodel; action=root_owned_binding_and_save_documentation; result=cleanup_and_durability_semantics_explicit

EXPECTED RESULT:

* entity_id=V78-07; new_state=closed; location=issue_78_kotlin_diff_audit_and_source_kdocs
* entity_id=V78-08; new_state=closed; location=three_updated_feature_readmes_plus_two_unchanged_accuracy_checks
* entity_id=V78-11; new_state=forward_audited; location=structured_blocks_032_through_040

VERIFICATION:

* check=git_diff_check; expected=pass; result=pass
* check=ast_index_rebuild; expected=1460_indexed_files_and_115_modules; result=pass
* check=operator_notes_hash_equality; expected=current_hash_equals_master_hash_for_five_readmes; result=pass
* check=aml_headings_and_uuid; expected=11_headings_and_parseable_uuid_per_block_032_through_040; result=pass
* check=affected_gradle_validation; expected=JVM_success_and_nonJVM_compile_success; result=JVM_success_nonJVM_compile_success_client_browser_timeout

UNCERTAINTY:

* missing=successful_client_browser_reload_case; ambiguity=240_second_browser_timeout_under_client_jsBrowserTest
* missing=native_device_runtime_execution; ambiguity=Android_validation_limited_to_production_and_unit_test_Kotlin_compilation
* excluded=unrelated_legacy_local_declarations_and_preexisting_navigation_overrides; basis=bounded_issue_78_diff_inventory

REPETITION OF RESULT:

* entity_id=issue_78_coding_040; stored_in=tracked_step_report_and_shared_worktree; status=available_with_commit_pending
* entity_id=V78-07_V78-08_V78-11; stored_in=step_040_audit; status=results_recorded_and_honest

COMMUNICATION:

* sender=issue78_coding_docs_cycle3; receiver=root; task_id=09.09.2026_09.40.05-b512784f-672e-4309-aa90-4551cb0c3361; message_id=a63aff60-398b-4452-b4a5-6d75b3cd2ffd; protocol=AML-HIP; handoff=commit_sha_and_validation_summary

PERSISTENCE:

* local_memory=true; shared_memory=true; index_keys=[task_id=09.09.2026_09.40.05-b512784f-672e-4309-aa90-4551cb0c3361,entity_id=issue_78_coding_040,intent=KDoc_README_AML_audit]
* persistence_scope=working_tree_and_step_report; push=false; operator_notes_mutation=false

VALIDATION:

* format_valid=true; no_pronouns=true; entities_explicit=true; high_density=true; causal_chain_present=true; ambiguity_detected=true
* check=required_sections; expected=[ENTITY,CONTEXT,ACTION,REASON,EXPECTED RESULT,VERIFICATION,UNCERTAINTY,REPETITION OF RESULT,COMMUNICATION,PERSISTENCE,VALIDATION]; result=pass
* check=message_id; expected=parseable_UUID; result=pass; value=a63aff60-398b-4452-b4a5-6d75b3cd2ffd
* check=behavior_and_api; expected=unchanged; result=pass
```
