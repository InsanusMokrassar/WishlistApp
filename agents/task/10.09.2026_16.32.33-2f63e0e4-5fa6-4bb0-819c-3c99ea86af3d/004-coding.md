Model: GPT-5 (inherited session)
Changed files: features/ui/users/src/commonMain/kotlin/ui/UserEditViewModel.kt, features/ui/users/src/commonTest/kotlin/ui/UserEditTestFixtures.kt, features/ui/users/src/commonTest/kotlin/ui/UserEditViewModelEmailTest.kt, features/ui/users/src/androidMain/kotlin/ui/UserEditView.kt, features/ui/users/src/jsMain/kotlin/ui/UserEditView.kt, features/ui/users/src/jvmMain/kotlin/ui/UserEditView.kt, agents/task/10.09.2026_16.32.33-2f63e0e4-5fa6-4bb0-819c-3c99ea86af3d/004-coding.md

# Coding Stage 2: Shared GREEN for owner email replacement

The inherited model was retained for Coding's ML/HL preference because the shared editor now coordinates authorization, live navigation targeting, capability policy, private-profile reconciliation, and cancellation across several suspension boundaries. The lean-build discipline kept the production change at `UserEditViewModel`, reused the existing user and navigation abstractions, and stopped before transport, backend, renderer-layout, copy, or documentation work allocated to later stages. No nested agent was used.

## Shared implementation

`OwnerSession` now carries the live `selectedUserId` from `node.configState`, while the ViewModel retains the original bound `userId` as the only valid private target. Imperative callbacks synchronize a raw caller, authorization, and current-node snapshot before admission. A matching private profile, confirmed Enabled or Disabled capability, clear load state, and idle mutation state are also required. Public state now separates owner visibility, storage mutation, valid changed-draft save, enabled-SMTP resend, acknowledged saved recipient, verification outcome, and generic interruption feedback.

Owner or target departure increments the generation, cancels current work, clears all address-bearing private state, and prevents leave/return revival. Mutation tokens bind caller, target, generation, operation identity, and lifecycle state; checks run before and after every suspend call. A delayed old `finally` can release only its exact active token. Manual refresh and resume coalesce while a mutation owns the state machine, and input callbacks cannot replace an admitted operation's captured draft.

Email storage is available under both Enabled and Disabled SMTP capability. Blank or malformed drafts produce `InvalidEmail` without dispatch. Parsed unchanged drafts are trim-normalized, clear dirtiness and address-bound feedback, and dispatch nothing. Enabled replacement executes acknowledged PUT B, a checked same-owner private GET containing exact B, POST with expectedEmail B only for a pending B, and a final checked private GET containing exact B before publishing saved success or a positive verification result. An already-approved first GET skips POST. Disabled replacement executes PUT B and one checked GET B with no POST.

False or thrown PUT maps to `SaveFailed`, preserves the raw draft, reconciles without an automatic POST, and never converts a lost response plus authoritative B into saved success. Missing, wrong-owner, mismatched-address, or failed reconciliation blocks successor requests and success. Negative delivery outcomes survive a failed final reconciliation, while positive outcomes require the final authoritative record. Resend is enabled-only, targets the authoritative saved pending address instead of a dirty draft, performs no PUT, checks the final private GET, and preserves the draft. The implementation deliberately adds no client rollback and no credential-epoch mechanism, preserving the architecture's documented limits.

The three platform view files received only the callback rename and the exhaustive `EmailChanged` error branch required for compilation. Visual policy, button wiring, interruption presentation, new copy, and renderer restructuring remain deferred to Stage 4.

## Deterministic acceptance evidence

The existing email test class now contains 48 deterministic common tests, 28 more than the Stage 1 suite. The matrix retains the three original RED replacements and covers successful/reset/order behavior; disabled SMTP approved, pending, and missing storage; dirty admin preservation; invalid, unchanged, and exact-case drafts; false, thrown, and lost PUT responses; primary-error preservation; exact first and final GET binding; all verification outcomes and POST exceptions; saved-versus-draft resend; refresh, resume, recovery, and coalescing; double submission; input while busy; anonymous, non-owner, root-other, and root-self policy; mismatched profiles; raw lagging guards; node retargeting at PUT, first GET, POST, and final GET boundaries; owner/target leave-return generations; lifecycle destruction; and old-finally isolation. Combined parameter variants retain distinct assertions and event traces. Fixtures use `CompletableDeferred`, `NonCancellable`, ordered events, and `MutableStateFlow` navigation config; no arbitrary delay or timeout was added.

Before each Gradle invocation, the Java/Gradle process preflight found no running process. The final required command was `./gradlew --no-parallel :wishlist.features.ui.users:jvmTest --tests '*UserEditViewModelEmailTest'`; it completed with `BUILD SUCCESSFUL in 28s`. Fresh XML at timestamp `2026-09-10T11:50:26.884Z` records 48 tests, 0 skipped, 0 failures, and 0 errors. The serial safety command `./gradlew --no-parallel :wishlist.features.ui.users:compileKotlinJs :wishlist.features.ui.users:compileDebugKotlinAndroid` completed with `BUILD SUCCESSFUL in 39s`, proving the mechanical JS and Android call-site updates compile. Reported Gradle deprecation, compile-SDK, and unrelated existing source warnings did not fail either gate.

## Scope, index, and deferred work

The AST index began with 785 files, 6,313 symbols, 25,690 references, and 49 modules. The required post-Kotlin rebuild completed with 785 files, 6,520 symbols, 26,654 references, and 49 modules. AST lookup finds no definition or usage of the removed compatibility callback; source lookup finds `onSaveEmail` in the shared owner and all three platform call sites.

`git diff --check` passes. The source diff is limited to the shared ViewModel, its existing common fixture and email test class, and the three mechanical platform view call sites. Stage 1 report `003-coding.md` is unchanged. `features/ui/users/README.md` has no diff, so Operator Notes remain byte-identical. No server, route, transport, schema, API taxonomy, dependency, build, or issue-78 file changed.

Stage 3 retains direct HTTP client and server-route boundary proof. Stage 4 retains renderer behavior, visual structure, strings, README changes outside Operator Notes, and deferred configuration/model declaration documentation. Stage 5 retains aggregate gates and existing repository-invariant reruns. Browser execution, Android device execution, SMTP delivery, backend routing, rollback, and credential-epoch behavior are not claimed by this shared stage.

```text
ENTITY:
entity_id=issue_79_stage2_shared_green; type=owner_email_state_machine; state=implemented_and_verified

CONTEXT:
* task_id=10.09.2026_16.32.33-2f63e0e4-5fa6-4bb0-819c-3c99ea86af3d; agent_id=issue79_coding_shared_green_cycle1; memory_ref=[PROMPT.md,001-planning.md,002-architecturing.md,003-coding.md,004-coding.md]
* constraints=[shared_owner_only,live_target_binding,serial_Gradle,deterministic_barriers]; exclusions=[HTTP_boundaries,server_changes,visual_restructure,README_changes,issue_78]

ACTION:
1. action=implement_owner_session_state_machine; target=issue_79_stage2_shared_green; params={bound_target:userId,live_target:node.configState,capability:[Enabled,Disabled],serialization:token_identity}
2. action=implement_storage_and_delivery_order; target=issue_79_stage2_shared_green; params={enabled:[PUT_B,GET_B,conditional_POST_B,final_GET_B],disabled:[PUT_B,GET_B],success_binding:exact_recipient}
3. action=complete_common_acceptance_matrix; target=user_edit_view_model_email_test; params={tests:48,skipped:0,failures:0,errors:0}

REASON:
* condition=existing_email_or_disabled_SMTP; requirement=owner_storage_independent_from_delivery; causal_chain=valid_changed_draft→acknowledged_PUT→checked_private_GET
* condition=identity_or_target_departure; requirement=private_state_nonrevival; causal_chain=raw_mismatch→generation_invalidation→success_suppression

EXPECTED RESULT:
* entity_id=issue_79_stage2_shared_green; new_state=shared_acceptance_green; location=features/ui/users/src/commonMain/kotlin/ui/UserEditViewModel.kt; evidence=48_test_XML

VERIFICATION:
* check=focused_jvm_email_suite; expected=48_green; observed=48_tests_0_skipped_0_failures_0_errors
* check=platform_compile_safety; expected=JS_and_Android_success; observed=compileKotlinJs_and_compileDebugKotlinAndroid_success
* check=post_edit_AST_index; expected=callback_replacement_and_full_rebuild; observed=785_files_6520_symbols_26654_refs_49_modules

UNCERTAINTY:
* missing=client_rollback; ambiguity=lost_response_commit_state; resolution=authoritative_reconciliation_without_success_publication; owner=accepted_architecture_limit
* missing=credential_epoch; ambiguity=leave_return_between_observations; resolution=raw_snapshot_plus_generation_without_epoch_claim; owner=accepted_architecture_limit

REPETITION OF RESULT:
* entity_id=issue_79_stage2_shared_green; stored_in=shared_memory; status=available; result=shared_acceptance_green

COMMUNICATION:
* sender=issue79_coding_shared_green_cycle1; receiver=orchestrator; task_id=10.09.2026_16.32.33-2f63e0e4-5fa6-4bb0-819c-3c99ea86af3d; message_id=518dd6e7-dfb0-4262-ab63-8a637283bfc3; protocol=AML-HIP

PERSISTENCE:
* local_memory=true; shared_memory=true; index_keys=[task_id,issue_79_stage2_shared_green,owner_email_state_machine]; storage=task_step_file; auto_memory=false

VALIDATION:
* format_valid=true; no_pronouns=true; entities_explicit=true; high_density=true; causal_chain_present=true; ambiguity_detected=true; ambiguity_resolution=accepted_limits_explicit_and_deferred_stages_bounded
```
