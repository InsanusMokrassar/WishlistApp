Model: gpt-5.6-terra
Changed files: features/ui/users/README.md, features/ui/users/src/androidMain/kotlin/ui/UserEditView.kt, features/ui/users/src/commonMain/kotlin/UsersListStrings.kt, features/ui/users/src/commonMain/kotlin/ui/UserEditViewModel.kt, features/ui/users/src/commonTest/kotlin/ui/UserEditViewModelEmailTest.kt, features/ui/users/src/jsMain/kotlin/ui/UserEditView.kt, features/ui/users/src/jvmMain/kotlin/ui/UserEditView.kt, features/ui/users/src/jvmTest/kotlin/ui/UserEditEmailRenderTest.kt, agents/task/10.09.2026_16.32.33-2f63e0e4-5fa6-4bb0-819c-3c99ea86af3d/012-coding.md

# Coding correction for issue #79

The six bounded issue-79 corrections are implemented on `feat/issue-79-user-email-change`. The selected model is gpt-5.6-terra: `agents/SHORTCUTS.md` gives Coding an ML-first priority, and `agents/MODELS.md` classifies Terra as ML. This task's explicit Coding assignment authorizes the report written by the same selected model; normal prose is retained for this report and the commit message.

`UserEditViewModel` now binds completed saved and verification publications to a private exact email-and-approval snapshot. A later checked address, missing-email, or approval-state change retires old feedback; unchanged snapshots retain it. Failed/unusable reads retire only completed success claims, preserving current local errors and negative delivery outcomes until a checked snapshot proves them obsolete. Save publication uses the checked Disabled first read or Enabled final read, while negative delivery fallback retains its captured matching profile.

Raw draft dirtiness now compares trimmed text with the saved email string without parsing. A nonblank invalid draft is dirty when no email is stored, produces `InvalidEmail` during ordinary typing, survives refresh and resume, and is considered directly by Back before the derived combined state can lag. Equivalent draft events preserve unrelated current failure feedback; material edits intentionally clear prior operation feedback.

All three platform renderers now collect their actual node `configState` and combine that collected target with the immediate raw node target and immutable bound user id before rendering any private email content. The JVM email panel was extracted into the production `OwnerEmailEditor(viewModel, node)` composable and is used by both `UserEditView.onDraw` and the new desktop renderer suite. The two real JVM fields use `settings-email-saved` and `settings-email` tags. Confirmed `Disabled` capability renders the existing unavailable-delivery copy once while retaining save-only storage; an `Unavailable` server result uses the same single copy. `emailVerificationUnavailable` KDoc now documents both sources.

Five new common regressions cover later approval feedback retirement, unchanged approval feedback retention, immediate invalid typing feedback, missing-email partial-draft refresh/resume preservation, and immediate Back confirmation. The real `UserEditEmailRenderTest` uses the already declared Compose v2 desktop raster host with separate composition, test-body, and ViewModel schedulers. It renders the actual production panel, verifies confirmed Disabled guidance, and proves a live retarget removes private semantics before the deliberately unadvanced ViewModel scheduler can invalidate owner state.

The shared red command was `./gradlew --no-parallel --rerun-tasks --console=plain :wishlist.features.ui.users:jvmTest --tests '*UserEditViewModelEmailTest'`; it compiled and executed 53 tests with the five new regressions failing: `laterPendingRefreshClearsAlreadyApprovedFeedback`, `approvalSnapshotChangesRetireCompletedFeedback`, `invalidDraftPublishesFeedbackDuringTyping`, `missingEmailPartialDraftSurvivesRefreshAndResume`, and `rawDraftDirtinessControlsBackConfirmation`. After the shared implementation, the exact command passed all 53 tests. The first actual desktop smoke command for `ownerEmailPanelRendersOnDesktop` passed. The first full `UserEditEmailRenderTest` command passed all three renderer tests, establishing the real host rather than repeating the prior nonexistent-filter claim.

For direct renderer negative evidence, I temporarily removed only the final JVM production-panel target predicate, rebuilt the AST index, and ran `liveRetargetHidesPrivateEmailBeforeOwnerCollectorRuns`. The real semantics assertion failed at `UserEditEmailRenderTest.kt:141` because private content remained rendered. I restored the exact predicate, rebuilt the index, and reran the full three-test renderer suite successfully. No temporary source state remains in the final diff.

The six-target platform gate passed with 521 actionable tasks: JS/JVM/Android users compilation, JS Node and Android users tests, and email-client JS Node tests. The required ten-target aggregate passed with 556 actionable tasks, including the new desktop suite in users JVM plus JS Node and Android common behavior. All Gradle commands used `--no-parallel`, `--rerun-tasks`, `--console=plain`, a zero Java/Gradle-process preflight, standard cache, pipefail, and separate logs under `/tmp/issue79-012-*`. The sandbox could not create the existing Gradle wrapper lock, so each required Gradle command used the approved existing-cache escalation; no cache, lockfile, dependency, or build configuration was changed.

`XDG_CACHE_HOME=/tmp/wishlist-issue79-ast-cache ast-index rebuild` ran after Kotlin changes and again for the final source tree. The final index contains 786 files, 49 modules, one XML usage, and four resources. AST searches confirm the three direct `configState` renderer collectors, the snapshot helpers, `OwnerEmailEditor` production/test references, and JVM Save/Done wiring. `git diff --check` passes. The affected README's first seven lines, which include the complete Operator Notes section, have SHA-256 `71220d017d4abfc545b86bf4201c820e10be52373482feffea69c4831436e3bc` in the final worktree and both required baselines `6a79d72624817f264acfd937b290bdfd6c75c5ad` and `cc8b63934086a41fb4c5965ce30b68775a67b67e`. The PR #80 `Подтверждение email` subtitle remains absent.

The execution establishes JVM desktop widget/semantics evidence but does not claim a browser DOM renderer, Android device renderer/IME, live SMTP, deployed server, server revision, credential epoch, rollback of an accepted PUT, or wholly unobserved equal email-and-approval round trip. The required full repository build remains the next Verification-stage gate. V79-01 through V79-06 remain Open until independent Validation reviews the final candidate; Coding does not independently close them.

## Structured coding handoff

```aml-hip
ENTITY:
entity_id=issue_79_coding_cycle_2; type=bounded_correction; state=implemented_and_tested

CONTEXT:
task_id=10.09.2026_16.32.33-2f63e0e4-5fa6-4bb0-819c-3c99ea86af3d; agent_id=issue79_coding_cycle2; memory_ref=[009,010,011,012]
constraints=[six_findings_only,no_push,serial_Gradle,Operator_Notes_preserved,final_AST_rebuild]; branch=feat/issue-79-user-email-change
exclusions=[API,schema,server_production,dependencies,build_configuration,lockfiles,navigation_framework,issue_78,PR_81]

ACTION:
action=implement_live_renderer_guard; target=V79-01; params={platforms:[JS,JVM,Android],predicate:[canManageOwnEmail,collected_config_userId,raw_config_userId,bound_userId],JVM_panel:OwnerEmailEditor}
action=implement_snapshot_feedback; target=V79-02; params={snapshot:[email,emailApproved],bindings:[emailSavedSnapshot,emailVerificationSnapshot],changed_snapshot:retire,unusable_read:clear_success_preserve_failure}
action=implement_typing_validation; target=V79-03; params={nonblank_invalid:InvalidEmail_immediate,valid_blank:clear_validation,redundant_event:preserve_operation_failure}
action=implement_raw_draft_protection; target=V79-04; params={comparison:trimmed_raw_to_saved_string,refresh_resume:preserve_partial,Back:direct_raw_dirty_check}
action=implement_desktop_proof; target=V79-05; params={test_class:UserEditEmailRenderTest,host:runDesktopComposeUiTest,production_panel:OwnerEmailEditor,schedulers:[composition,test_body,ViewModel]}
action=implement_disabled_guidance; target=V79-06; params={predicate:EmailCapabilityState.Disabled,copy:emailVerificationUnavailable,duplicate_copy:false,save_storage:true,verification_POST:false}

VERIFICATION:
check=shared_behavioral_red; expected=5_new_failures; observed={tests:53,failures:5,log:/tmp/issue79-012-shared-red.log}
check=shared_behavioral_green; expected=53_pass; observed={tests:53,failures:0,log:/tmp/issue79-012-shared-green.log}
check=desktop_smoke; expected=ownerEmailPanelRendersOnDesktop_pass; observed={tests:1,failures:0,log:/tmp/issue79-012-renderer-smoke.log}
check=renderer_controlled_red; expected=live_retarget_private_semantics_present_without_predicate; observed={tests:1,failures:1,assertion:UserEditEmailRenderTest.kt:141,log:/tmp/issue79-012-renderer-live-target-red.log}
check=renderer_final_green; expected=3_pass; observed={tests:3,failures:0,log:/tmp/issue79-012-renderer-final-green.log}
check=platform_gate; expected=6_targets_pass; observed={exit_code:0,actionable_tasks:521,log:/tmp/issue79-012-platform-gate.log}
check=aggregate_gate; expected=10_targets_pass; observed={exit_code:0,actionable_tasks:556,log:/tmp/issue79-012-aggregate.log}
check=integrity; expected=[diff_check,Operator_Notes,AST_scope]; observed={diff_check:pass,operator_notes_sha256:71220d017d4abfc545b86bf4201c820e10be52373482feffea69c4831436e3bc,AST_files:786}

EXPECTED RESULT:
entity_id=issue_79_coding_cycle_2; new_state=ready_for_Verification; location=agents/task/10.09.2026_16.32.33-2f63e0e4-5fa6-4bb0-819c-3c99ea86af3d/012-coding.md
finding_id=V79-01; state=Open_pending_independent_Validation; implementation=complete
finding_id=V79-02; state=Open_pending_independent_Validation; implementation=complete
finding_id=V79-03; state=Open_pending_independent_Validation; implementation=complete
finding_id=V79-04; state=Open_pending_independent_Validation; implementation=complete
finding_id=V79-05; state=Open_pending_independent_Validation; implementation=complete
finding_id=V79-06; state=Open_pending_independent_Validation; implementation=complete

UNCERTAINTY:
missing=[browser_DOM,Android_device_IME,live_SMTP,deployed_server,server_revision,credential_epoch]; ambiguity=platform_and_external_execution_limits; accepted_scope=[JVM_desktop_semantics,common_state,JS_Android_compile_and_unit_execution]

REPETITION OF RESULT:
entity_id=issue_79_coding_cycle_2; stored_in=shared_memory; status=available; result=implementation_and_Coding_gates_pass

COMMUNICATION:
sender=issue79_coding_cycle2; receiver=Orchestrator_and_Verification; task_id=10.09.2026_16.32.33-2f63e0e4-5fa6-4bb0-819c-3c99ea86af3d; message_id=2c3cf02a-56a1-4052-a4cb-6e2f3f0b4a0c; protocol=AML-HIP; next_role=Verification

PERSISTENCE:
local_memory=true; shared_memory=true; index_keys=[task_id,issue_79_coding_cycle_2,V79_01,V79_02,V79_03,V79_04,V79_05,V79_06]; storage=012-coding.md; auto_memory=false

VALIDATION:
format_valid=true; no_pronouns=true; entities_explicit=true; high_density=true; causal_chain_present=true; ambiguity_detected=true; ambiguity_resolution=independent_Verification_and_Validation
```
