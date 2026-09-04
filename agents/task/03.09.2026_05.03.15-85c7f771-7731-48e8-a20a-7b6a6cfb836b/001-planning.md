Model: OpenAI GPT-5 (high-level)
Changed files: agents/task/03.09.2026_05.03.15-85c7f771-7731-48e8-a20a-7b6a6cfb836b/001-planning.md

# Planning Report

## Model rationale

The Planning role prefers a high-level model. The requested refactor crosses Compose function types, `StateFlow` state, and effect-key behavior, so OpenAI GPT-5 was used to define the compatibility boundary and verification plan.

## Task understanding

Change the nullable toast state in `Toaster.kt` from a rendered `String` to a nullable `@Composable () -> String` provider. Keep `show(text: String)` source-compatible and store `{ text }` in `_message`. Update `ToastHost` to invoke the provider during composition while retaining the existing null sentinel, CSS visibility behavior, success icon, 2600 ms delay, and `clear()` behavior.

The change is limited to `features/common/client/src/jsMain/kotlin/ui/components/Toaster.kt`. No caller edits, new overloads, dependencies, styling changes, or notification behavior changes are planned.

## Investigation

The Common feature README has no operator-specific constraints. `ast-index` located the `Toaster` definition and confirmed one function-reference caller in `ClientJSPlugin.kt`, five direct string callers across booking and wishlist views, and the single `ToastHost()` mount in `ScaffoldView.kt`. The current implementation stores `StateFlow<String?>`, renders `Text(message ?: "")`, keys auto-dismiss on the collected message, delays for 2600 ms, and clears the state to `null`.

A fresh `{ text }` wrapper has reference identity, so repeated calls with the same string can emit a new state value instead of being conflated by `StateFlow` string equality. The requested wrapper assignment explicitly entails that result and aligns with the existing KDoc promise that a subsequent call restarts the timer.

## Open questions

No unclear architecture decisions, requirements, or constraints remain. No operator questions are required because the prompt specifies the new state type, the compatibility wrapper, and the narrow behavior-preservation boundary.

## Final plan and architecture handoff

```text
ENTITY:
entity_id=toaster_message_state; type=Kotlin_StateFlow; state=StateFlow<String?>

CONTEXT:
* task_id=03.09.2026_05.03.15-85c7f771-7731-48e8-a20a-7b6a6cfb836b; agent_id=planning; memory_ref=[PROMPT.md,features/common/README.md,features/common/client/src/jsMain/kotlin/ui/components/Toaster.kt]
* constraints=[source_scope=Toaster.kt,public_api=show(String),null_sentinel=preserved,dismiss_delay_ms=2600,new_dependencies=false]; verification_scope=[common_client_js_compile,web_client_js_compile]

ACTION:
1. action=change_state_type; target=toaster_message_state; params={private_type=MutableStateFlow<(@Composable () -> String)?>,public_type=StateFlow<(@Composable () -> String)?>,empty_state=null}
2. action=preserve_string_entrypoint; target=Toaster.show(String); params={signature_unchanged=true,assignment=_message.value={ text },caller_changes=none}
3. action=render_composable_provider; target=ToastHost; params={collection=collectAsState(),render=Text(message?.invoke() ?: ""),visibility_condition=message!=null}
4. action=preserve_auto_dismiss; target=ToastHost; params={effect_key=message,delay_ms=2600,terminal_action=Toaster.clear()}
5. action=verify_same_proof_before_and_after; target=wishlist_web_client; params={command=./gradlew :wishlist.features.common.client:compileKotlinJs :wishlist.client:compileKotlinJs,expected_exit_code=0,ast_index_rebuild_after_source_change=true}

REASON:
* condition=toast_state_requires_composition_time_string_provider → action=store_composable_lambda_and_invoke_during_composition → result=provider_capability_added; requirement=existing_String_callers_remain_source_compatible

EXPECTED RESULT:
* entity_id=toaster_message_state; new_state=StateFlow<(@Composable () -> String)?>; location=features/common/client/src/jsMain/kotlin/ui/components/Toaster.kt
* entity_id=Toaster.show(String); new_state=signature_preserved_and_wrapper_lambda_stored; location=features/common/client/src/jsMain/kotlin/ui/components/Toaster.kt
* entity_id=ToastHost; new_state=provider_invoked_and_existing_visibility_plus_dismissal_preserved; location=features/common/client/src/jsMain/kotlin/ui/components/Toaster.kt

VERIFICATION:
* check=pre_change_and_post_change_JS_compilation; expected=exit_code_0_for_common_client_and_web_client
* check=ast_index_reference_review; expected=existing_call_sites_unchanged_and_single_host_mount_preserved

UNCERTAINTY:
* missing=none; ambiguity=none; operator_questions=none

REPETITION OF RESULT:
* entity_id=toaster_message_state; stored_in=shared_memory; status=available

COMMUNICATION:
* sender=planning; receiver=architecture; task_id=03.09.2026_05.03.15-85c7f771-7731-48e8-a20a-7b6a6cfb836b; message_id=2740e8dd-2844-44fd-8ee4-be01db26d9b5; protocol=AML-HIP

PERSISTENCE:
* local_memory=true; shared_memory=true; index_keys=[task_id,entity_id,intent]

VALIDATION:
* format_valid=true; no_pronouns=true; entities_explicit=true; high_density=true; causal_chain_present=true; ambiguity_detected=false
```
