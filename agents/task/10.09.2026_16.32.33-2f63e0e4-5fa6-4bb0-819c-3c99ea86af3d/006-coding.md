Model: GPT-5 (LL documentation and KDoc pass)
Changed files: features/ui/users/README.md, features/ui/users/src/androidMain/kotlin/ui/UserEditView.kt, features/ui/users/src/commonMain/kotlin/UsersListStrings.kt, features/ui/users/src/commonMain/kotlin/ui/UserEditViewConfig.kt, features/ui/users/src/commonMain/kotlin/ui/UserEditViewModel.kt, features/ui/users/src/commonMain/kotlin/ui/UsersModel.kt, features/ui/users/src/jsMain/kotlin/ui/UserEditView.kt, features/ui/users/src/jvmMain/kotlin/ui/UserEditView.kt, agents/task/10.09.2026_16.32.33-2f63e0e4-5fa6-4bb0-819c-3c99ea86af3d/006-coding.md

# Coding Stage 4: platform UI, copy, KDocs, README, and bounded renderer proof

Stage 4 updates the existing owner-email section on JS, JVM, and Android without adding a framework, dependency, navigation host, or issue-78 code. Loaded self-owner screens now keep the authoritative saved address and editable replacement draft visibly separate for missing, pending, and approved saved states. Approved and pending saved addresses remain replaceable. Storage eligibility follows authenticated self ownership, profile match, capability state, load state, and busy state; SMTP Disabled still permits storage. SMTP Enabled changes the save action to “Save and send verification” and permits resend only for the saved pending address. The shared save eligibility guards unchanged, invalid, and busy callbacks. JVM and Android use single-line email `OutlinedTextField` controls with email keyboard type and Done IME callbacks routed to the same guarded `onSaveEmail`.

The three renderers show saved confirmation, generic storage-confirmation failure, recoverable load failure with Refresh, delivery success/failure, replacement verification guidance, and operation interruption. Root editing another account and live-target retargeting hide private email controls and feedback through the existing owner/live-target guard. JS retains the existing Calm Studio components and has no implicit Enter behavior. The merged PR #80 removal of the `Подтверждение email` subtitle remains intact.

`UsersListStrings.kt` now contains Architecture-specified English/Russian copy and KDocs for `saveEmailButton`, `emailSaved`, `savedEmailLabel`, `emailReplacementNeedsVerification`, and `emailOperationInterrupted`; missing guidance is storage-oriented and SaveFailed wording remains honest about uncertain confirmation. Relevant `UserEditViewConfig.kt`, `UsersModel.kt`, and `UserEditViewModel.kt` KDocs describe replacement, SMTP-independent storage, delivery capability, and the modified public state. README Overview, Models, Architecture Notes, tests, and limitations document Boolean 409/transport ambiguity, draft preservation, owner/caller/live-target boundaries, no rollback after accepted PUT, unobservable same-owner credential epochs, and absent live SMTP/browser/device execution. `## Operator Notes` is byte-identical to commit `cc8b63934086a41fb4c5965ce30b68775a67b67e`.

The requested JVM renderer target was attempted with `./gradlew --no-parallel :wishlist.features.ui.users:jvmTest --tests '*UserEditEmailRenderTest'`. No `UserEditEmailRenderTest.kt` exists and the module has no existing users ViewModel/Compose host capable of rendering the actual panel and callbacks without a new harness. The exact bounded failure was `No tests found for given includes: [*UserEditEmailRenderTest](--tests filter)`. No conditional test file was retained or added. Fallback proof is the shared 48-test JVM email state suite plus successful JS/JVM/Android source compiles; no browser, Android device, native IME, SMTP, or live server execution is claimed.

`XDG_CACHE_HOME=/tmp/wishlist-issue79-ast-cache ast-index rebuild` completed after Kotlin edits: 785 files, 6,575 symbols, 26,873 references, 49 modules, 1 XML usage, and 4 resources. AST searches resolve `onSaveEmail`, `canSaveEmailState`, and `emailOperationInterrupted` across ViewModel, strings, tests, and all three renderers. `git diff --check` passes. EN/RU string audit, all-three-renderer audit, KDoc inventory, source-scope review, subtitle search, and Operator Notes byte comparison pass.

The focused command `./gradlew --no-parallel :wishlist.features.ui.users:jvmTest --tests '*UserEditViewModelEmailTest'` completed successfully with 48 tests, 0 skipped, 0 failures, and 0 errors; fresh XML timestamp is `2026-09-10T12:24:17.056Z`. The final three-platform compile command completed successfully with 279 actionable tasks, including the live-target guard adjustment. The mandated combined gate reached `compileKotlinJs`, `compileKotlinJvm`, and `compileDebugKotlinAndroid`, then failed at the pre-existing `:kotlinStoreYarnLock` guard with `Lock file was changed. Run the kotlinUpgradeYarnLock task to actualize lock file`; no lockfile was changed and the mutating lockfile task was not run. The renderer-target command failed only because no matching renderer test exists. No Java or Gradle process was present during preflight, and Gradle invocations were serial with `--no-parallel`.

```text
ENTITY:
entity_id=issue_79_stage4_platform_ui_docs; type=platform_email_editor_documentation; state=implemented_with_bounded_renderer_proof

CONTEXT:
* task_id=10.09.2026_16.32.33-2f63e0e4-5fa6-4bb0-819c-3c99ea86af3d; agent_id=issue79_coding_ui_docs_cycle1; memory_ref=[PROMPT.md,001-planning.md,002-architecturing.md,003-coding.md,004-coding.md,005-coding.md,006-coding.md]
* constraints=[three_platform_views,existing_components,SMTP_independent_storage,AML_HIP,serial_Gradle,Operator_Notes_byte_preservation]; exclusions=[new_harness,new_dependency,live_device_execution,issue_78,renderer_claim_without_host]

ACTION:
1. action=update_platform_email_sections; target=issue_79_stage4_platform_ui_docs; params={renderers:[JS,JVM,Android],saved_draft_separation=true,pending_and_approved_replacement=true,disabled_SMTP_storage=true,done_IME_guard=true,live_target_guard=true}
2. action=update_strings_and_KDocs; target=issue_79_stage4_platform_ui_docs; params={locales:[EN,RU],new_documented_strings:5,storage_guidance=true,delivery_capability_docs=true}
3. action=update_users_README; target=issue_79_stage4_platform_ui_docs; params={sections:[Overview,Models,Architecture_Notes,tests,limitations],operator_notes_compare=cc8b63934086a41fb4c5965ce30b68775a67b67e}
4. action=attempt_bounded_JVM_renderer_target; target=issue_79_stage4_platform_ui_docs; params={command:jvmTest_UserEditEmailRenderTest,host_result=no_matching_test,exact_failure=No_tests_found_for_given_includes}

REASON:
* condition=authoritative_saved_email_and_replacement_draft_require_distinct_UI_state; requirement=saved_status_must_not_describe_unsaved_draft; causal_chain=saved_profile_plus_draft_input→separate_controls_and_status_text→safe_replacement_feedback
* condition=SMTP_delivery_capability_differs_from_storage_capability; requirement=Disabled_SMTP_must_allow_Save_email; causal_chain=capability_state→shared_storage_eligibility→save_label_and_optional_delivery
* condition=private_state_must_follow_owner_and_live_target_boundaries; requirement=root_other_and_retargeted_views_must_hide_private_feedback; causal_chain=caller_and_live_config_guard→private_controls_visibility→no_cross_account_state_exposure

EXPECTED RESULT:
* entity_id=issue_79_stage4_platform_ui_docs; new_state=platform_UI_and_documentation_complete_with_bounded_renderer_limit; location=features/ui/users_and_task_report

VERIFICATION:
* check=focused_jvm_email_state_XML; expected=48_tests_0_skipped_0_failures_0_errors; observed=48_tests_0_skipped_0_failures_0_errors; timestamp=2026-09-10T12:24:17.056Z
* check=platform_compile_tasks; expected=JS_JVM_Android_success; observed=compileKotlinJs_compileKotlinJvm_compileDebugKotlinAndroid_success
* check=mandated_combined_gate; expected=all_requested_tasks_success; observed=blocked_at_kotlinStoreYarnLock_existing_lock_drift; mutation_task=not_run
* check=renderer_target; expected=honest_host_result; observed=no_matching_test_for_UserEditEmailRenderTest; fallback=shared_48_test_state_evidence
* check=ast_index; expected=785_files_6575_symbols_26873_refs_49_modules; observed=785_files_6575_symbols_26873_refs_49_modules
* check=source_and_documentation_audits; expected=diff_check_renderer_audit_locale_audit_KDoc_audit_operator_notes_audit_scope_audit_pass; observed=all_pass

UNCERTAINTY:
* missing=existing_real_renderer_host; ambiguity=renderer_execution_unavailable_without_new_harness; accepted_scope=bounded_shared_state_and_platform_compile_proof
* missing=successful_combined_JS_test_execution; ambiguity=kotlinStoreYarnLock_lock_drift; accepted_scope=exact_failure_recorded_without_lockfile_mutation
* missing=live_SMTP_browser_device_IME_execution; ambiguity=none; accepted_scope=no_live_execution_claim

REPETITION OF RESULT:
* entity_id=issue_79_stage4_platform_ui_docs; stored_in=shared_memory; status=available; result=platform_UI_copy_KDocs_README_complete_with_renderer_limit_and_lock_guard

COMMUNICATION:
* sender=issue79_coding_ui_docs_cycle1; receiver=orchestrator; task_id=10.09.2026_16.32.33-2f63e0e4-5fa6-4bb0-819c-3c99ea86af3d; message_id=8fb9e020-bb28-4aa9-a9ce-2ca19f73a4e4; protocol=AML-HIP

PERSISTENCE:
* local_memory=true; shared_memory=true; index_keys=[task_id,issue_79_stage4_platform_ui_docs,owner_email_UI,renderer_limit,lock_guard]; storage=task_step_file

VALIDATION:
* format_valid=true; no_pronouns=true; entities_explicit=true; high_density=true; causal_chain_present=true; ambiguity_detected=true; ambiguity_resolution=renderer_host_and_lock_drift_limits_explicit
```
