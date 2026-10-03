Model: GPT-6 (inherited session)
Changed files: features/ui/users/src/commonTest/kotlin/ui/UserEditTestFixtures.kt, features/ui/users/src/commonTest/kotlin/ui/UserEditViewModelEmailTest.kt, agents/task/10.09.2026_16.32.33-2f63e0e4-5fa6-4bb0-819c-3c99ea86af3d/003-coding.md

# Coding Stage 1: Behavioral RED for owner email replacement

The inherited high-capability model was retained for Coding's ML/HL preference because the regression boundary includes owner-private state, SMTP capability behavior, and asynchronous ViewModel tests. The surgical-patch discipline kept this stage limited to the smallest shared behavioral proof and the existing fixture. No nested agent was used.

## Test changes

`approvedOwnerEmailCanBeReplacedAndVerified` and `pendingOwnerEmailCanBeReplacedAndVerified` now start with an existing approved or pending address, enter `replacement@example.com`, invoke the current public save callback, and require exactly one replacement PUT. Their later assertions also require the exact verification recipient, an authoritative refreshed replacement profile with `emailApproved=false`, and no generic profile-save navigation.

`disabledSmtpOwnerCanReplaceApprovedEmailWithoutSending` replaces the obsolete expectation that disabled SMTP hides the owner section. The regression requires the approved address to be replaced and refreshed while making no verification request. The test fails first on the missing PUT, so the failure is caused by current product behavior rather than unavailable future state or a harness problem.

The existing `UserEditTestUsersModel.saveEmailHandler` now mirrors the repository invariant narrowly: an unchanged non-null address preserves existing approval, while a changed or cleared address resets approval. No generalized fixture, production source, renderer, string, README, build file, or backend file changed.

## Deterministic RED evidence

The serial focused command was `./gradlew --no-parallel :wishlist.features.ui.users:jvmTest --tests '*UserEditViewModelEmailTest'`. A process preflight found no running Java/Gradle process. Gradle used the standard project cache under the approved execution boundary.

Compilation completed successfully through `compileKotlinJvm`, `compileTestKotlinJvm`, and `jvmTestClasses`. The test task then failed as expected: 20 tests completed, 3 failed, 0 skipped, and 0 errors. All 17 unchanged pre-existing cases in the same class passed.

The exact expected failures were `approvedOwnerEmailCanBeReplacedAndVerified[jvm]`, `pendingOwnerEmailCanBeReplacedAndVerified[jvm]`, and `disabledSmtpOwnerCanReplaceApprovedEmailWithoutSending[jvm]`. Each reported `java.lang.AssertionError: expected:<[Email(string=replacement@example.com)]> but was:<[]>`. The common message proves that the existing-address and disabled-SMTP paths dispatch no replacement PUT. The command exited 1 with `BUILD FAILED in 35s`; the intentionally red assertion task is the only reported failure.

## Deferred Architecture cases

Stage 2 must add the public saved-versus-draft state and mutable navigation-config fixture before expressing the remaining shared acceptance matrix honestly. Deferred shared cases cover GET-before-POST ordering and the exact refreshed recipient, final reconciliation, disabled-SMTP pending and missing variants, normalized unchanged/blank/invalid no-dispatch behavior, double submit, replacement-specific false and thrown PUT draft retention, refresh/resume/coalescing, saved-address resend, load mismatches, root-self and root-other policy, caller/logout/current-user changes, live `ComposeNode.configState` retarget before and during each suspension, lifecycle cancellation, stale publication, and old-finally isolation.

Architecture Stages 3 through 5 retain direct Ktor 409 and route-caller coverage, JVM renderer proof, conditional JS/Android host proof, strings and documentation, aggregate compilation/tests, and repository invariant reruns. Adding those cases now would require unavailable Stage 2 public state or would violate the first RED-stage allocation.

## Scope and index audit

The initial AST index contained 785 files, 6,299 symbols, 25,640 references, and 49 modules. The mandatory rebuild after Kotlin test edits completed with 785 files and 49 modules; the refreshed statistics contain 6,313 symbols and 25,690 references. AST queries resolve both new regression names at their source locations.

`git diff --check` passed. The source scope contains only the two allocated common-test files, plus this report. `features/ui/users/README.md` has no diff, so the Operator Notes section remains byte-identical. No production behavior was changed in this RED stage.

```text
ENTITY:
entity_id=issue_79_stage1_red_tests; type=behavioral_regression_suite; state=deterministic_red

CONTEXT:
task_id=10.09.2026_16.32.33-2f63e0e4-5fa6-4bb0-819c-3c99ea86af3d; agent_id=issue79_coding_red_cycle1; memory_ref=[PROMPT.md,001-planning.md,002-architecturing.md,003-coding.md]
constraints=[common_tests_only,existing_public_callback,serial_Gradle,production_changes_zero]; exclusions=[renderers,strings,README,build_files,PR_81,nested_agents]

ACTION:
1. action=add_behavioral_regressions; target=issue_79_stage1_red_tests; params={tests:[approved_replacement,pending_replacement,disabled_SMTP_replacement],expected_dispatch:PUT_replacement}
2. action=align_existing_fake_invariant; target=user_edit_test_users_model; params={unchanged_nonnull:preserve_approval,replacement_or_clear:reset_approval}

REASON:
condition=existing_nonnull_email_guard; requirement=approved_and_pending_owner_replacement; causal_chain=guard→zero_PUT→deterministic_RED
condition=disabled_SMTP_storage_gate; requirement=storage_independent_from_delivery; causal_chain=disabled_capability→zero_PUT→deterministic_RED

EXPECTED RESULT:
entity_id=issue_79_stage1_red_tests; new_state=behavioral_failure_proven; location=agents/task/10.09.2026_16.32.33-2f63e0e4-5fa6-4bb0-819c-3c99ea86af3d/003-coding.md

VERIFICATION:
check=focused_jvm_suite; expected=three_behavioral_failures; observed=20_tests_3_failures_17_passes
check=Kotlin_test_compilation; expected=successful_compilation; observed=compileKotlinJvm_and_compileTestKotlinJvm_success

UNCERTAINTY:
missing=stage2_public_state_and_mutable_config_fixture; ambiguity=remaining_shared_matrix_deferred; owner=next_coding_stage
missing=platform_renderer_and_transport_execution; ambiguity=architecture_stages_3_to_5_deferred; owner=later_coding_and_verification

REPETITION OF RESULT:
entity_id=issue_79_stage1_red_tests; stored_in=shared_memory; status=available; result=behavioral_failure_proven

COMMUNICATION:
sender=issue79_coding_red_cycle1; receiver=orchestrator; task_id=10.09.2026_16.32.33-2f63e0e4-5fa6-4bb0-819c-3c99ea86af3d; message_id=aead0f84-df6a-4b5a-ba82-9ded70f02980; protocol=AML-HIP

PERSISTENCE:
local_memory=true; shared_memory=true; index_keys=[task_id,issue_79_stage1_red_tests,owner_email_replacement]; storage=task_step_file; auto_memory=false

VALIDATION:
format_valid=true; no_pronouns=true; entities_explicit=true; high_density=true; causal_chain_present=true; ambiguity_detected=true; ambiguity_resolution=defer_unexpressible_cases_to_allocated_stages
```
